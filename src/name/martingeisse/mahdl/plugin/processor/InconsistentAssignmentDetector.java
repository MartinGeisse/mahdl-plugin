/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.definition.Port;
import name.martingeisse.mahdl.plugin.processor.definition.Signal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This object detects multiple or missing assignments to signals. It is not concerned with type safety.
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

	public void handleAssignment(@NotNull Statement_Assignment assignment) {
		handleAssignedTo(assignment.getLeftSide());
	}

	public void handleAssignedTo(@Nullable Expression destination) {
		if (destination instanceof Expression_Identifier) {
			LeafPsiElement signalNameElement = ((Expression_Identifier) destination).getIdentifier();
			handleAssignedToSignalLike(signalNameElement.getText(), signalNameElement);
		} else if (destination instanceof Expression_IndexSelection) {
			Expression container = ((Expression_IndexSelection) destination).getContainer();
			handleAssignedTo(container);
		} else if (destination instanceof Expression_RangeSelection) {
			Expression container = ((Expression_RangeSelection) destination).getContainer();
			handleAssignedTo(container);
		} else if (destination instanceof Expression_Parenthesized) {
			Expression inner = ((Expression_Parenthesized) destination).getExpression();
			handleAssignedTo(inner);
		} else if (destination instanceof Expression_BinaryConcat) {
			Expression_BinaryConcat typed = (Expression_BinaryConcat) destination;
			handleAssignedTo(typed.getLeftOperand());
			handleAssignedTo(typed.getRightOperand());
		} else if (destination instanceof Expression_InstancePort) {
			Expression_InstancePort typed = (Expression_InstancePort) destination;
			handleAssignedToInstancePort(typed.getInstanceName().getText(), typed.getPortName().getText(), typed);
		}
	}

	public void handleAssignedToSignalLike(@NotNull String signalLikeName, @NotNull PsiElement errorSource) {
		handleAssignedToInternal(signalLikeName, errorSource);
	}

	public void handleAssignedToInstancePort(@NotNull String instanceName, @NotNull String portName, @NotNull PsiElement errorSource) {
		handleAssignedToInternal(instanceName + '.' + portName, errorSource);
	}

	private void handleAssignedToInternal(@NotNull String signalName, @NotNull PsiElement errorSource) {
		if (previouslyAssignedSignals.contains(signalName)) {
			errorHandler.onError(errorSource, "signal " + signalName + " was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
	}

}
