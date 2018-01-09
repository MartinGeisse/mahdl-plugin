package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.definition.Port;
import name.martingeisse.mahdl.plugin.processor.definition.Signal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public abstract class InconsistentAssignmentDetector {

	private final Set<String> previouslyAssignedSignals = new HashSet<>();
	private final Set<String> newlyAssignedSignals = new HashSet<>();

	public void finishSection() {
		previouslyAssignedSignals.addAll(newlyAssignedSignals);
		newlyAssignedSignals.clear();
	}

	public void checkMissingAssignments(Collection<Named> definitions) {
		for (Named definition : definitions) {
			if (definition instanceof Port) {
				Port port = (Port) definition;
				if (port.getDirectionElement() instanceof PortDirection_Output) {
					if (port.getInitializer() == null && !previouslyAssignedSignals.contains(port.getName())) {
						onError(port.getNameElement(), "missing assignment for port '" + port.getName() + "'");
					}
				}
			} else if (definition instanceof Signal) {
				Signal signal = (Signal) definition;
				if (signal.getInitializer() == null && !previouslyAssignedSignals.contains(signal.getName())) {
					onError(signal.getNameElement(), "missing assignment for signal '" + signal.getName() + "'");
				}
			} else if (definition instanceof ModuleInstance) {
				ModuleInstance moduleInstance = (ModuleInstance) definition;
				String instanceName = moduleInstance.getName();
				ImplementationItem_ModuleInstance moduleInstanceElement = moduleInstance.getModuleInstanceElement();
				PsiElement untypedResolvedModule = moduleInstanceElement.getModuleName().getReference().resolve();
				if (untypedResolvedModule instanceof Module) {
					Module resolvedModule = (Module) untypedResolvedModule;
					for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
						if (portDefinitionGroup.getDirection() instanceof PortDirection_Output) {
							for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
								String prefixedPortName = instanceName + '.' + portDefinition.getName();
								if (!previouslyAssignedSignals.contains(prefixedPortName)) {
									onError(portDefinition.getIdentifier(), "missing assignment for port '" + portDefinition.getName() + "' in instance '" + instanceName + "'");
								}
							}
						}
					}
				}
			}
		}
	}

	public void handleAssignment(Statement_Assignment assignment) {
		handleAssignedTo(assignment.getLeftSide());
	}

	public void handleAssignedTo(Expression destination) {
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

	public void handleAssignedToSignalLike(String signalLikeName, PsiElement errorSource) {
		handleAssignedToInternal(signalLikeName, errorSource);
	}

	public void handleAssignedToInstancePort(String instanceName, String portName, PsiElement errorSource) {
		handleAssignedToInternal(instanceName + '.' + portName, errorSource);
	}

	private void handleAssignedToInternal(String signalName, PsiElement errorSource) {
		if (previouslyAssignedSignals.contains(signalName)) {
			onError(errorSource, "signal " + signalName + " was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
	}

	protected abstract void onError(PsiElement errorSource, String message);

}
