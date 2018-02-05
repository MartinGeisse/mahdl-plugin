/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class DefinitionProcessor {

	private final ErrorHandler errorHandler;
	private final DataTypeProcessor dataTypeProcessor;
	private final ExpressionProcessor expressionProcessor;
	private final Map<String, Named> definitions;

	public DefinitionProcessor(@NotNull ErrorHandler errorHandler,
							   @NotNull DataTypeProcessor dataTypeProcessor,
							   @NotNull ExpressionProcessor expressionProcessor) {
		this.errorHandler = errorHandler;
		this.dataTypeProcessor = dataTypeProcessor;
		this.expressionProcessor = expressionProcessor;
		this.definitions = new HashMap<>();
	}

	@NotNull
	public Map<String, Named> getDefinitions() {
		return definitions;
	}

	public void processPorts(ListNode<PortDefinitionGroup> psiPortList) {
		for (PortDefinitionGroup portDefinitionGroup : psiPortList.getAll()) {
			for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
				DataType dataType = portDefinitionGroup.getDataType();
				ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
				ProcessedDataType.Family family = processedDataType.getFamily();
				if (family != ProcessedDataType.Family.UNKNOWN && family != ProcessedDataType.Family.BIT && family != ProcessedDataType.Family.VECTOR) {
					errorHandler.onError(dataType, family.getDisplayString() + " type not allowed for ports");
					processedDataType = ProcessedDataType.Unknown.INSTANCE;
				}
				add(new Port(portDefinition, portDefinitionGroup.getDirection(), dataType, processedDataType));
			}
		}
	}

	/**
	 * Processes the specified implementation item to obtain named definitions and adds them to the definition map
	 * stored in this processor.
	 * <p>
	 * Any definition that is a constant gets its initializer processed and evaluated. Constants used in the initializer
	 * must be present in the definition map. If the current implementation item contains multiple constants, then their
	 * initializers may use constants that appear earlier in the current implementation item.
	 * <p>
	 * Any signal-like definitions that are not constants do not get their initializer processed yet -- that must be
	 * done later by the caller. This allows the current item's initializers to use definitions that only appear in
	 * later implementation items.
	 * <p>
	 * Usage note: This method must first be called for all constants in the order they appear in the module, then for
	 * all non-constants (in any order). The former ensures that each constant is available for all constants appearing
	 * later. The latter ensures that the type specifiers for non-constants can use constants defined later.
	 */
	public void process(ImplementationItem implementationItem) {
		if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
			ImplementationItem_SignalLikeDefinitionGroup signalLike = (ImplementationItem_SignalLikeDefinitionGroup) implementationItem;
			SignalLikeKind kind = signalLike.getKind();
			DataType dataType = signalLike.getDataType();
			ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
			ProcessedDataType.Family dataTypeFamily = processedDataType.getFamily();
			for (SignalLikeDefinition signalLikeDefinition : signalLike.getDefinitions().getAll()) {

				// extract name element and initializer
				PsiElement nameElement;
				ExtendedExpression initializer;
				if (signalLikeDefinition instanceof SignalLikeDefinition_WithoutInitializer) {
					SignalLikeDefinition_WithoutInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithoutInitializer) signalLikeDefinition;
					nameElement = typedDeclaredSignalLike.getIdentifier();
					initializer = null;
				} else if (signalLikeDefinition instanceof SignalLikeDefinition_WithInitializer) {
					SignalLikeDefinition_WithInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithInitializer) signalLikeDefinition;
					nameElement = typedDeclaredSignalLike.getIdentifier();
					initializer = typedDeclaredSignalLike.getInitializer();
				} else {
					errorHandler.onError(signalLikeDefinition, "unknown PSI node");
					continue;
				}

				// add the definition
				if (kind instanceof SignalLikeKind_Constant) {
					Constant constant = new Constant(nameElement, dataType, processedDataType, initializer);
					if (initializer == null) {
						errorHandler.onError(signalLikeDefinition, "constant must have an initializer");
					} else {
						constant.processExpressions(expressionProcessor);
						constant.evaluate(new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler));
					}
					add(constant);
				} else if (kind instanceof SignalLikeKind_Signal || kind instanceof SignalLikeKind_Register) {
					if (dataTypeFamily != ProcessedDataType.Family.UNKNOWN &&
						dataTypeFamily != ProcessedDataType.Family.BIT &&
						dataTypeFamily != ProcessedDataType.Family.VECTOR &&
						dataTypeFamily != ProcessedDataType.Family.MEMORY) {
						errorHandler.onError(dataType, dataTypeFamily.getDisplayString() + " type not allowed for signals and registers");
						processedDataType = ProcessedDataType.Unknown.INSTANCE;
					}
					if (kind instanceof SignalLikeKind_Signal) {
						add(new Signal(nameElement, dataType, processedDataType, initializer));
					} else {
						add(new Register(nameElement, dataType, processedDataType, initializer));
					}
				}

			}
		} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
			ImplementationItem_ModuleInstance moduleInstanceElement = (ImplementationItem_ModuleInstance) implementationItem;

			// resolve the module definition
			Module resolvedModule;
			{
				PsiElement untypedResolvedModule = moduleInstanceElement.getModuleName().getReference().resolve();
				if (untypedResolvedModule instanceof Module) {
					resolvedModule = (Module) untypedResolvedModule;
				} else {
					errorHandler.onError(moduleInstanceElement.getModuleName(), "unknown module: '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText() + "'");
					return;
				}
			}

			// build a map of the port definitions from the module definition
			Map<String, InstancePort> ports = new HashMap<>();
			for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
				for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
					String portName = portDefinition.getName();
					if (portName != null) {
						PortDirection direction;
						if (portDefinitionGroup.getDirection() instanceof PortDirection_In) {
							direction = PortDirection.IN;
						} else if (portDefinitionGroup.getDirection() instanceof PortDirection_Out) {
							direction = PortDirection.OUT;
						} else {
							errorHandler.onError(portDefinitionGroup.getDirection(), "unknown direction");
							continue;
						}
						// TODO what happens if this detects an undefined type in the port? We can't place annotations in another file, right?
						ProcessedDataType processedDataType = dataTypeProcessor.processDataType(portDefinitionGroup.getDataType());
						ports.put(portName, new InstancePort(portName, direction, processedDataType));
					}
				}
			}

			// build port connections
			Map<String, PortConnection> portConnections = new HashMap<>();
			for (name.martingeisse.mahdl.plugin.input.psi.PortConnection rawPortConnection : moduleInstanceElement.getPortConnections().getAll()) {
				LeafPsiElement portNameElement = rawPortConnection.getPortName().getIdentifier();
				String portName = portNameElement.getText();
				InstancePort port = ports.get(portName);
				if (port == null) {
					expressionProcessor.getErrorHandler().onError(rawPortConnection.getPortName().getIdentifier(), "unknown port: '" + portName + "'");
					continue;
				}
				portConnections.put(portName, new PortConnection(port, portNameElement, rawPortConnection.getExpression()));
			}

			add(new ModuleInstance(moduleInstanceElement, resolvedModule, ImmutableMap.copyOf(ports), ImmutableMap.copyOf(portConnections)));
		}
	}

	private void add(@NotNull Named element) {
		if (definitions.put(element.getName(), element) != null) {
			errorHandler.onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

}
