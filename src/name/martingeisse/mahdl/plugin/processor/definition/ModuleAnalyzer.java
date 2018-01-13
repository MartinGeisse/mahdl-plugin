/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class ModuleAnalyzer {

	private final ErrorHandler errorHandler;
	private final DataTypeProcessor dataTypeProcessor;
	private final Module module;
	private final Map<String, Named> definitions;

	public ModuleAnalyzer(ErrorHandler errorHandler, DataTypeProcessor dataTypeProcessor, Module module) {
		this.errorHandler = errorHandler;
		this.dataTypeProcessor = dataTypeProcessor;
		this.module = module;
		this.definitions = new HashMap<>();
	}

	public Map<String, Named> getDefinitions() {
		return definitions;
	}


	/**
	 * Registers all constants. This must be done before the actual analysis, and after the constants have been
	 * evaluated, because all constants -- even those defined later -- are needed in evaluated form for this analyzer
	 * to work.
	 */
	public final void registerConstants(Map<String, ConstantValue> constantValues) {
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				ImplementationItem_SignalLikeDefinitionGroup typedImplementationItem = (ImplementationItem_SignalLikeDefinitionGroup) implementationItem;
				if (typedImplementationItem.getKind() instanceof SignalLikeKind_Constant) {
					DataType dataType = typedImplementationItem.getDataType();
					ProcessedDataType processedDataType = dataTypeProcessor.processDataType(dataType);
					for (SignalLikeDefinition signalLikeDefinition : typedImplementationItem.getDefinitions().getAll()) {
						PsiElement nameElement;
						Expression initializer;
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
						String name = nameElement.getText();
						ConstantValue value = constantValues.get(name);
						if (value == null) {
							errorHandler.onError(signalLikeDefinition, "value for this constant is missing in the constant value map");
							value = ConstantValue.Unknown.INSTANCE;
						}
						add(new Constant(nameElement, dataType, processedDataType, initializer, value));
					}
				}
			}
		}
	}

	/**
	 * Returns all named definitions (ports and implementation items) from the specified module, mapped by name.
	 */
	public final void analyzeNonConstants() {

		// ports
		for (PortDefinitionGroup portDefinitionGroup : module.getPortDefinitionGroups().getAll()) {
			for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
				DataType dataType = portDefinitionGroup.getDataType();
				Port port = new Port(portDefinition, portDefinitionGroup.getDirection(), dataType, dataTypeProcessor.processDataType(dataType));
				add(port);
			}
		}

		// implementation items
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				ImplementationItem_SignalLikeDefinitionGroup typedImplementationItem = (ImplementationItem_SignalLikeDefinitionGroup) implementationItem;
				if (typedImplementationItem.getKind() instanceof SignalLikeKind_Constant) {
					continue;
				}
				for (SignalLikeDefinition signalLikeDefinition : typedImplementationItem.getDefinitions().getAll()) {
					LeafPsiElement nameElement;
					Expression initializer;
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
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				ModuleInstance moduleInstance = new ModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
				add(moduleInstance);
			}
		}

	}

	private void add(Named element) {
		if (definitions.put(element.getName(), element) != null) {
			errorHandler.onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

	private SignalLike convertSignalLike(ImplementationItem_SignalLikeDefinitionGroup signalLikeDefinitionGroup,
												LeafPsiElement nameElement,
												Expression initializer) {
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

}
