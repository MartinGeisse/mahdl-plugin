package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ProcessedDataType;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class ModuleAnalyzer {

	private final Module module;
	private final Map<String, Named> definitions;

	public ModuleAnalyzer(Module module) {
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
	 *
	 * Returns this.
	 */
	public final ModuleAnalyzer registerConstants(Map<String, ConstantValue> constantValues) {
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition typedImplementationItem = (ImplementationItem_SignalLikeDefinition) implementationItem;
				if (typedImplementationItem.getKind() instanceof SignalLikeKind_Constant) {
					DataType dataType = typedImplementationItem.getDataType();
					ProcessedDataType processedDataType = processDataType(dataType);
					for (DeclaredSignalLike declaredSignalLike : typedImplementationItem.getSignalNames().getAll()) {
						PsiElement nameElement;
						Expression initializer;
						if (declaredSignalLike instanceof DeclaredSignalLike_WithoutInitializer) {
							DeclaredSignalLike_WithoutInitializer typedDeclaredSignal = (DeclaredSignalLike_WithoutInitializer) declaredSignalLike;
							nameElement = typedDeclaredSignal.getIdentifier();
							initializer = null;
						} else if (declaredSignalLike instanceof DeclaredSignalLike_WithInitializer) {
							DeclaredSignalLike_WithInitializer typedDeclaredSignal = (DeclaredSignalLike_WithInitializer) declaredSignalLike;
							nameElement = typedDeclaredSignal.getIdentifier();
							initializer = typedDeclaredSignal.getInitializer();
						} else {
							continue;
						}
						String name = nameElement.getText();
						ConstantValue value = constantValues.get(name);
						if (value == null) {
							onError(declaredSignalLike, "value for this constant is missing in the constant value map");
							value = ConstantValue.Unknown.INSTANCE;
						}
						add(new Constant(nameElement, dataType, processedDataType, initializer, value));
					}
				}
			}
		}
		return this;
	}

	/**
	 * Returns all named definitions (ports and implementation items) from the specified module, mapped by name.
	 *
	 * Returns this.
	 */
	public final ModuleAnalyzer analyzeNonConstants() {

		// ports
		for (PortDefinition portDefinition : module.getPorts().getAll()) {
			for (LeafPsiElement nameElement : portDefinition.getIdentifiers().getAll()) {
				DataType dataType = portDefinition.getDataType();
				Port port = new Port(nameElement, portDefinition.getDirection(), dataType, processDataType(dataType));
				add(port);
			}
		}

		// implementation items
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition typedImplementationItem = (ImplementationItem_SignalLikeDefinition) implementationItem;
				if (typedImplementationItem.getKind() instanceof SignalLikeKind_Constant) {
					continue;
				}
				for (DeclaredSignalLike declaredSignalLike : typedImplementationItem.getSignalNames().getAll()) {
					LeafPsiElement nameElement;
					Expression initializer;
					if (declaredSignalLike instanceof DeclaredSignalLike_WithoutInitializer) {
						DeclaredSignalLike_WithoutInitializer typedDeclaredSignal = (DeclaredSignalLike_WithoutInitializer) declaredSignalLike;
						nameElement = typedDeclaredSignal.getIdentifier();
						initializer = null;
					} else if (declaredSignalLike instanceof DeclaredSignalLike_WithInitializer) {
						DeclaredSignalLike_WithInitializer typedDeclaredSignal = (DeclaredSignalLike_WithInitializer) declaredSignalLike;
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

		return this;
	}

	private void add(Named element) {
		if (definitions.put(element.getName(), element) != null) {
			onError(element.getNameElement(), "redefinition of '" + element.getName() + "'");
		}
	}

	protected abstract void onError(PsiElement errorSource, String message);
	protected abstract ProcessedDataType processDataType(DataType dataType);

	private SignalLike convertSignalLike(ImplementationItem_SignalLikeDefinition signalLikeDefinition,
												LeafPsiElement nameElement,
												Expression initializer) {
		SignalLikeKind kind = signalLikeDefinition.getKind();
		DataType dataType = signalLikeDefinition.getDataType();
		ProcessedDataType processedDataType = processDataType(dataType);
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
