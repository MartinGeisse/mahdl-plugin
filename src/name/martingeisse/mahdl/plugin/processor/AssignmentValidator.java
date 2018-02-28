/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.definition.PortDirection;
import name.martingeisse.mahdl.plugin.processor.expression.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This object detects assignments to invalid targets such as constants or operator expressions (except assignments to
 * concatenation, which is allowed). It also detects multiple or missing assignments to signals and registers. It is NOT
 * concerned with type safety and assumes that the {@link ExpressionProcessor} has detected any type errors already.
 */
public final class AssignmentValidator {

	private final ErrorHandler errorHandler;
	private final Set<String> previouslyAssignedSignals = new HashSet<>();
	private final Set<String> newlyAssignedSignals = new HashSet<>();

	public AssignmentValidator(@NotNull ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void finishSection() {
		previouslyAssignedSignals.addAll(newlyAssignedSignals);
		newlyAssignedSignals.clear();
	}

	public void checkMissingAssignments(@NotNull Collection<Named> definitions) {
		for (Named definition : definitions) {
			if (definition instanceof ModulePort) {
				ModulePort port = (ModulePort) definition;
				if (port.getDirectionElement() instanceof PortDirection_Out) {
					if (port.getInitializer() == null && !previouslyAssignedSignals.contains(port.getName())) {
						errorHandler.onError(port.getNameElement(), "missing assignment for port '" + port.getName() + "'");
					}
				}
			} else if (definition instanceof Signal) {
				Signal signal = (Signal) definition;
				if (signal.getInitializer() == null && !previouslyAssignedSignals.contains(signal.getName())) {
					errorHandler.onError(signal.getNameElement(), "missing assignment for signal '" + signal.getName() + "'");
				}
			} else if (definition instanceof ModuleInstance) {
				ModuleInstance moduleInstance = (ModuleInstance) definition;
				String instanceName = moduleInstance.getName();
				ImplementationItem_ModuleInstance moduleInstanceElement = moduleInstance.getModuleInstanceElement();
				PsiElement untypedResolvedModule = moduleInstanceElement.getModuleName().getReference().resolve();
				if (untypedResolvedModule instanceof Module) {
					Module resolvedModule = (Module) untypedResolvedModule;
					for (PortDefinitionGroup untypedPortDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
						if (untypedPortDefinitionGroup instanceof PortDefinitionGroup_Valid) {
							PortDefinitionGroup_Valid portDefinitionGroup = (PortDefinitionGroup_Valid)untypedPortDefinitionGroup;
							if (portDefinitionGroup.getDirection() instanceof PortDirection_In) {
								for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
									String prefixedPortName = instanceName + '.' + portDefinition.getName();
									if (!previouslyAssignedSignals.contains(prefixedPortName)) {
										errorHandler.onError(moduleInstanceElement, "missing assignment for port '" + portDefinition.getName() + "' in instance '" + instanceName + "'");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void validateAssignmentTo(@NotNull ProcessedExpression destination, TriggerKind triggerKind) {
		// noinspection ConstantConditions
		if (destination == null) {
			throw new IllegalArgumentException("destination cannot be null");
		}
		if (destination instanceof ProcessedConstantValue) {

			errorHandler.onError(destination.getErrorSource(), "cannot assign to a constant");

		} else if (destination instanceof SignalLikeReference) {

			SignalLike signalLike = ((SignalLikeReference) destination).getDefinition();
			PsiElement errorSource = destination.getErrorSource();
			if (signalLike instanceof ModulePort) {
				PortDirection direction = ((ModulePort) signalLike).getDirection();
				if (direction != PortDirection.OUT) {
					errorHandler.onError(errorSource, "input port " + signalLike.getName() + " cannot be assigned to");
				} else if (triggerKind != TriggerKind.CONTINUOUS) {
					errorHandler.onError(errorSource, "assignment to module port must be continuous");
				}
			} else if (signalLike instanceof Signal) {
				if (triggerKind != TriggerKind.CONTINUOUS) {
					errorHandler.onError(errorSource, "assignment to signal must be continuous");
				}
			} else if (signalLike instanceof Register) {
				if (triggerKind != TriggerKind.CLOCKED) {
					errorHandler.onError(errorSource, "assignment to register must be clocked");
				}
			} else if (signalLike instanceof Constant) {
				errorHandler.onError(errorSource, "cannot assign to constant");
			}
			considerAssignedTo(signalLike, destination.getErrorSource());

		} else if (destination instanceof ProcessedIndexSelection) {

			validateAssignmentTo(((ProcessedIndexSelection) destination).getContainer(), triggerKind);

		} else if (destination instanceof ProcessedRangeSelection) {

			validateAssignmentTo(((ProcessedRangeSelection) destination).getContainer(), triggerKind);

		} else if (destination instanceof ProcessedBinaryOperation) {

			ProcessedBinaryOperation binaryOperation = (ProcessedBinaryOperation) destination;
			if (binaryOperation.getOperator() == ProcessedBinaryOperator.VECTOR_CONCAT) {
				validateAssignmentTo(binaryOperation.getLeftOperand(), triggerKind);
				validateAssignmentTo(binaryOperation.getRightOperand(), triggerKind);
			} else {
				errorHandler.onError(destination.getErrorSource(), "expression cannot be assigned to");
			}

		} else if (destination instanceof InstancePortReference) {

			InstancePortReference instancePortReference = (InstancePortReference) destination;
			if (triggerKind != TriggerKind.CONTINUOUS) {
				errorHandler.onError(destination.getErrorSource(), "assignment to instance port must be continuous");
			}
			validateAssignmentToInstancePort(instancePortReference.getModuleInstance(), instancePortReference.getPort(),
				instancePortReference.getErrorSource());

		} else if (!(destination instanceof UnknownExpression)) {

			errorHandler.onError(destination.getErrorSource(), "expression cannot be assigned to");

		}
	}

	/**
	 * Remembers the specified signal-like as "assigned to" in the current "section" (definition or do-block).
	 * <p>
	 * Does not check whether the signal-like can be assigned to in this context -- e.g. it will not treat an assignment
	 * to a constant as an error. It just checks whether this assignment is in conflict with another assignment.
	 * <p>
	 * Therefore, this method should not be called for the initializer of a register, because that initializer is not
	 * in conflict with an assignment to the register, but calling this function would assume it to be.
	 */
	public void considerAssignedTo(@NotNull SignalLike signalLike, @NotNull PsiElement errorSource) {
		considerAssignedTo(signalLike.getName(), errorSource);
	}

	public void validateAssignmentToInstancePort(@NotNull ModuleInstance moduleInstance, @NotNull InstancePort port, @NotNull PsiElement errorSource) {
		if (port.getDirection() == PortDirection.OUT) {
			errorHandler.onError(errorSource, "cannot assign to output port");
		} else {
			considerAssignedTo(moduleInstance.getName() + '.' + port.getName(), errorSource);
		}
	}

	private void considerAssignedTo(@NotNull String signalName, @NotNull PsiElement errorSource) {
		if (previouslyAssignedSignals.contains(signalName)) {
			errorHandler.onError(errorSource, "'" + signalName + "' was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
	}

	public enum TriggerKind {
		CONTINUOUS, CLOCKED
	}

}
