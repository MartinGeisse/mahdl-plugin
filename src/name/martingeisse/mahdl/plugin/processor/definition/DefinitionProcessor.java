/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
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
				Port port = new Port(portDefinition, portDefinitionGroup.getDirection(), dataType, dataTypeProcessor.processDataType(dataType));
				add(port);
			}
		}
	}

	/**
	 * Processes the specified implementation item to obtain named definitions and adds them to the definition map
	 * stored in this processor.
	 *
	 * Any definition that is a constant gets its initializer processed and evaluated. Constants used in the initializer
	 * must be present in the definition map. If the current implementation item contains multiple constants, then their
	 * initializers may use constants that appear earlier in the current implementation item.
	 *
	 * Any signal-like definitions that are not constants do not get their initializer processed yet -- that must be
	 * done later by the caller. This allows the current item's initializers to use definitions that only appear in
	 * later implementation items.
	 *
	 * Usage note: This method must first be called for all constants in the order they appear in the module, then for
	 * all non-constants (in any order). The former ensures that each constant is available for all constants appearing
	 * later. The latter ensures that the type specifiers for non-constants can use constants defined later (TODO is
	 * this is a good idea? probably -- we don't want to enforce that all constants are defined at the top; we only
	 * demand the mutual ordering of constants because it doesn't work otherwise)
	 */
	public void process(ImplementationItem implementationItem) {
		if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
			ImplementationItem_SignalLikeDefinitionGroup signalLike = (ImplementationItem_SignalLikeDefinitionGroup) implementationItem;
			SignalLikeKind kind = signalLike.getKind();
			DataType dataType = signalLike.getDataType();
			ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
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
				} else if (kind instanceof SignalLikeKind_Signal) {
					add(new Signal(nameElement, dataType, processedDataType, initializer));
				} else if (kind instanceof SignalLikeKind_Register) {
					add(new Register(nameElement, dataType, processedDataType, initializer));
				}

			}
		} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
			ImplementationItem_ModuleInstance moduleInstanceElement = (ImplementationItem_ModuleInstance)implementationItem;

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

			// build a map of the ports from the module definition
			Map<String, PortDefinitionGroup> portNameToDefinitionGroup = new HashMap<>();
			for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
				for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
					String portName = portDefinition.getName();
					if (portName != null) {
						portNameToDefinitionGroup.put(portName, portDefinitionGroup);
					}
				}
			}

			add(new ModuleInstance(moduleInstanceElement, resolvedModule, ImmutableMap.copyOf(portNameToDefinitionGroup)));
		}
	}

	private void add(@NotNull Named element) {
		if (definitions.put(element.getName(), element) != null) {
			errorHandler.onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

}
