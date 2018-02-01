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
	private final Map<String, Named> definitions;

	public DefinitionProcessor(@NotNull ErrorHandler errorHandler, @NotNull DataTypeProcessor dataTypeProcessor) {
		this.errorHandler = errorHandler;
		this.dataTypeProcessor = dataTypeProcessor;
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

			// TODO merge these two branches
			if (signalLike.getKind() instanceof SignalLikeKind_Constant) {
				DataType dataType = signalLike.getDataType();
				ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
				for (SignalLikeDefinition signalLikeDefinition : signalLike.getDefinitions().getAll()) {
					PsiElement nameElement;
					ExtendedExpression initializer;
					ConstantValue value;
					if (signalLikeDefinition instanceof SignalLikeDefinition_WithoutInitializer) {

						SignalLikeDefinition_WithoutInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithoutInitializer) signalLikeDefinition;
						errorHandler.onError(signalLikeDefinition, "constant must have an initializer");
						nameElement = typedDeclaredSignalLike.getIdentifier();
						initializer = null;
						value = ConstantValue.Unknown.INSTANCE;

					} else if (signalLikeDefinition instanceof SignalLikeDefinition_WithInitializer) {

						SignalLikeDefinition_WithInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithInitializer) signalLikeDefinition;
						nameElement = typedDeclaredSignalLike.getIdentifier();
						initializer = typedDeclaredSignalLike.getInitializer();
						ConstantValue rawValue = formallyConstantExpressionEvaluator.evaluate(typedDeclaredSignalLike.getInitializer());
						value = processedDataType.convertConstantValueImplicitly(rawValue);
						if ((value instanceof ConstantValue.Unknown) && !(rawValue instanceof ConstantValue.Unknown)) {
							errorHandler.onError(typedDeclaredSignalLike.getInitializer(), "cannot convert value of type " + rawValue.getDataType() + " to type " + processedDataType);
						}

					} else {

						errorHandler.onError(signalLikeDefinition, "unknown PSI node");
						continue;

					}
					Constant constant = new Constant(nameElement, dataType, processedDataType, initializer, value);
					add(constant);
					result.add(constant);
				}
			} else {
				for (SignalLikeDefinition signalLikeDefinition : typedImplementationItem.getDefinitions().getAll()) {
					LeafPsiElement nameElement;
					ExtendedExpression initializer;
					if (signalLikeDefinition instanceof SignalLikeDefinition_WithoutInitializer) {
						SignalLikeDefinition_WithoutInitializer typedDeclaredSignal = (SignalLikeDefinition_WithoutInitializer) signalLikeDefinition;
						nameElement = typedDeclaredSignal.getIdentifier();
						initializer = null;
					} else if (signalLikeDefinition instanceof SignalLikeDefinition_WithInitializer) {
						SignalLikeDefinition_WithInitializer typedDeclaredSignal = (SignalLikeDefinition_WithInitializer) signalLikeDefinition;
						nameElement = typedDeclaredSignal.getIdentifier();
						initializer = typedDeclaredSignal.getInitializer();
					} else {
						continue;
					}
					SignalLike signalLike = convertSignalLike(typedImplementationItem, nameElement, initializer);
					if (signalLike != null) {
						add(signalLike);
						result.add(signalLike);
					}
				}
			}
		} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
			ModuleInstance moduleInstance = new ModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
			add(moduleInstance);
			result.add(moduleInstance);
		}
		return result;
	}

	@Nullable
	private SignalLike convertSignalLike(@NotNull ImplementationItem_SignalLikeDefinitionGroup signalLikeDefinitionGroup,
										 @NotNull LeafPsiElement nameElement,
										 @Nullable ExtendedExpression initializer) {
		SignalLikeKind kind = signalLikeDefinitionGroup.getKind();
		DataType dataType = signalLikeDefinitionGroup.getDataType();
		ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
		if (kind instanceof SignalLikeKind_Constant) {
			throw new IllegalArgumentException("this method should not be called for constants");
		} else if (kind instanceof SignalLikeKind_Signal) {
			return new Signal(nameElement, dataType, processedDataType, initializer);
		} else if (kind instanceof SignalLikeKind_Register) {
			return new Register(nameElement, dataType, processedDataType, initializer);
		} else {
			return null;
		}
	}

	private void add(@NotNull Named element) {
		if (definitions.put(element.getName(), element) != null) {
			errorHandler.onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

}
