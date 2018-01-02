package name.martingeisse.mahdl.plugin.definition;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class ModuleAnalyzer {

	// prevent instantiation
	private ModuleAnalyzer() {
	}

	/**
	 * Returns all named definitions (ports and implementation items) from the specified module.
	 */
	public static ImmutableMap<String, Named> analyze(Module module) {
		return analyze(module, null);
	}

	/**
	 * Returns all named definitions (ports and implementation items) from the specified module, mapped by name.
	 * <p>
	 * If an annotation holder is given, name collisions will be annotated. The reason this is supported here is that
	 * with name collisions, the returned map cannot contain all definitions, and the annotator could not generate
	 * annotations for name collisions without duplicating the functionality from here.
	 */
	public static ImmutableMap<String, Named> analyze(Module module, AnnotationHolder annotationHolder) {
		Map<String, Named> result = new HashMap<>();

		// ports
		for (PortDefinition portDefinition : module.getPorts().getAll()) {
			for (LeafPsiElement nameElement : portDefinition.getIdentifiers().getAll()) {
				Port port = new Port(nameElement, portDefinition.getDirection(), portDefinition.getDataType());
				add(result, port, annotationHolder);
			}
		}

		// implementation items
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition typedImplementationItem = (ImplementationItem_SignalLikeDefinition) implementationItem;
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
						add(result, signalLike, annotationHolder);
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				ModuleInstance moduleInstance = new ModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
				add(result, moduleInstance, annotationHolder);
			}
		}

		return ImmutableMap.copyOf(result);
	}

	private static void add(Map<String, Named> map, Named element, AnnotationHolder annotationHolder) {
		if (map.put(element.getName(), element) != null) {
			if (annotationHolder != null) {
				annotationHolder.createErrorAnnotation(element.getNameElement(),
					"redefinition of '" + element.getName() + "'");
			}
		}
	}

	private static SignalLike convertSignalLike(ImplementationItem_SignalLikeDefinition signalLikeDefinition,
												LeafPsiElement nameElement,
												Expression initializer) {
		SignalLikeKind kind = signalLikeDefinition.getKind();
		if (kind instanceof SignalLikeKind_Constant) {
			return new Constant(nameElement, signalLikeDefinition.getDataType(), initializer);
		} else if (kind instanceof SignalLikeKind_Signal) {
			return new Signal(nameElement, signalLikeDefinition.getDataType(), initializer);
		} else if (kind instanceof SignalLikeKind_Register) {
			return new Register(nameElement, signalLikeDefinition.getDataType(), initializer);
		} else {
			return null;
		}
	}

}
