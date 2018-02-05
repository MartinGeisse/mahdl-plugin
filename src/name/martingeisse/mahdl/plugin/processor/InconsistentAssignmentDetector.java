/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.definition.PortConnection;
import name.martingeisse.mahdl.plugin.processor.definition.PortDirection;
import name.martingeisse.mahdl.plugin.processor.expression.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This object detects multiple or missing assignments to signals and registers. It is not concerned with type safety,
 * nor does it check whether a signal or register may be assigned to at all.
 */
public final class InconsistentAssignmentDetector {

	private final ErrorHandler errorHandler;
	private final Set<String> previouslyAssignedSignals = new HashSet<>();
	private final Set<String> newlyAssignedSignals = new HashSet<>();

	public InconsistentAssignmentDetector(@NotNull ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void finishSection() {
		previouslyAssignedSignals.addAll(newlyAssignedSignals);
		newlyAssignedSignals.clear();
	}

	public void checkMissingAssignments(@NotNull Collection<Named> definitions) {
		for (Named definition : definitions) {
			if (definition instanceof Port) {
				Port port = (Port) definition;
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
					for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
						if (portDefinitionGroup.getDirection() instanceof PortDirection_Out) {
							for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
								String prefixedPortName = instanceName + '.' + portDefinition.getName();
								if (!previouslyAssignedSignals.contains(prefixedPortName)) {
									errorHandler.onError(portDefinition.getIdentifier(), "missing assignment for port '" + portDefinition.getName() + "' in instance '" + instanceName + "'");
								}
							}
						}
					}
				}
			}
		}
	}

	public void handleAssignedTo(@Nullable ProcessedExpression destination) {
		if (destination instanceof ProcessedConstantValue) {
			// TODO this probably generated redundant error messages since checkLValue() also does the same
			errorHandler.onError(destination.getErrorSource(), "cannot assign to a constant");
		} else if (destination instanceof SignalLikeReference) {
			SignalLike signalLike = ((SignalLikeReference) destination).getDefinition();
			handleAssignedToSignalLike(signalLike, destination.getErrorSource());
		} else if (destination instanceof ProcessedIndexSelection) {
			handleAssignedTo(((ProcessedIndexSelection) destination).getContainer());
		} else if (destination instanceof ProcessedRangeSelection) {
			handleAssignedTo(((ProcessedRangeSelection) destination).getContainer());
		} else if (destination instanceof ProcessedBinaryOperation) {
			ProcessedBinaryOperation binaryOperation = (ProcessedBinaryOperation)destination;
			if (binaryOperation.getOperator() == ProcessedBinaryOperator.VECTOR_CONCAT) {
				handleAssignedTo(binaryOperation.getLeftOperand());
				handleAssignedTo(binaryOperation.getRightOperand());
			} else {
				// TODO this probably generated redundant error messages since checkLValue() also does the same
				errorHandler.onError(destination.getErrorSource(), "expression cannot be assigned to");
			}
		} else if (destination instanceof InstancePortReference) {
			InstancePortReference instancePortReference = (InstancePortReference) destination;
			handleAssignedToInstancePort(instancePortReference.getModuleInstance(), instancePortReference.getPort(),
				instancePortReference.getErrorSource());
		} else if (destination != null && !(destination instanceof UnknownExpression)) {
			// TODO this probably generated redundant error messages since checkLValue() also does the same
			errorHandler.onError(destination.getErrorSource(), "expression cannot be assigned to");
		}
	}

	public void handleAssignedToSignalLike(@NotNull SignalLike signalLike, @NotNull PsiElement errorSource) {
		handleAssignedToInternal(signalLike.getName(), errorSource);
	}

	public void handleAssignedToInstancePort(@NotNull ModuleInstance moduleInstance, @NotNull InstancePort port, @NotNull PsiElement errorSource) {
		if (port.getDirection() == PortDirection.OUT) {
			errorHandler.onError(errorSource, "cannot assign to output port");
		} else {
			handleAssignedToInternal(moduleInstance.getName() + '.' + port.getName(), errorSource);
		}
	}

	private void handleAssignedToInternal(@NotNull String signalName, @NotNull PsiElement errorSource) {
		if (previouslyAssignedSignals.contains(signalName)) {
			errorHandler.onError(errorSource, "signal " + signalName + " was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
	}

}
