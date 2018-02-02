/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	// returns the list of new definitions and also adds them to the definition map
	@NotNull
	public List<Named> process(ImplementationItem implementationItem) {
		List<Named> result = new ArrayList<>();
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

				// build the definition
				Named definition;
				if (kind instanceof SignalLikeKind_Constant) {
					Constant constant = new Constant(nameElement, dataType, processedDataType, initializer);
					ConstantValue value;
					if (initializer == null) {
						errorHandler.onError(signalLikeDefinition, "constant must have an initializer");
						value = ConstantValue.Unknown.INSTANCE;
					} else {
						constant.processInitializer(expressionProcessor);
						constant.evaluate(new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler));

						// TODO --- convert value
						// TODO value = processedDataType.convertConstantValueImplicitly(rawValue);
						// TODO if ((value instanceof ConstantValue.Unknown) && !(rawValue instanceof ConstantValue.Unknown)) {
						// TODO 	errorHandler.onError(initializer, "cannot convert value of type " + rawValue.getDataType() + " to type " + processedDataType);
						// TODO }

					}
					definition = constant;
				} else if (kind instanceof SignalLikeKind_Signal) {
					definition = new Signal(nameElement, dataType, processedDataType, initializer);
				} else if (kind instanceof SignalLikeKind_Register) {
					definition = new Register(nameElement, dataType, processedDataType, initializer);
				} else {
					continue;
				}

				// add the definition both to the current result and the definition map
				add(definition);
				result.add(definition);

			}
		} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
			ModuleInstance moduleInstance = new ModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
			add(moduleInstance);
			result.add(moduleInstance);
		}
		return result;
	}

	private void add(@NotNull Named element) {
		if (definitions.put(element.getName(), element) != null) {
			errorHandler.onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

}
