/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ModuleInstancePortReference implements PsiReference {

	private final InstancePortName instancePortName;

	public ModuleInstancePortReference(InstancePortName instancePortName) {
		this.instancePortName = instancePortName;
	}

	@Override
	public PsiElement getElement() {
		return instancePortName;
	}

	@Override
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	private PsiElement resolveModule() {
		PsiElement parent = instancePortName.getParent();
		if (!(parent instanceof Expression_InstancePort)) {
			// port name element is lost in a PSI soup and not even part of a real port reference
			return null;
		}
		PsiElement instanceNameDefiningElement = ((Expression_InstancePort) parent).getInstanceName().getReference().resolve();
		if (instanceNameDefiningElement == null) {
			// the instance name before the dot is unknown, so the port name is meaningless
			return null;
		}
		ImplementationItem_ModuleInstance moduleInstanceElement = PsiUtil.getAncestor(instanceNameDefiningElement, ImplementationItem_ModuleInstance.class);
		if (moduleInstanceElement == null) {
			// the element which defines the instance name was found but is lost in a PSI soup -- at least resolve to the name-defining element.
			return instanceNameDefiningElement;
		}
		PsiElement moduleNameDefiningElement = moduleInstanceElement.getModuleName().getReference().resolve();
		if (moduleNameDefiningElement == null) {
			// the module name is unknown
			return moduleInstanceElement.getModuleName();
		}
		Module module = PsiUtil.getAncestor(moduleNameDefiningElement, Module.class);
		if (module == null) {
			// the module name defining element is lost in a PSI soup
			return moduleNameDefiningElement;
		}
		return module;
	}

	@Nullable
	@Override
	public PsiElement resolve() {
		PsiElement resolvedModule = resolveModule();
		if (!(resolvedModule instanceof Module)) {
			return resolvedModule;
		}
		Module targetModule = (Module)resolvedModule;
		String referencePortName = getCanonicalText();
		for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
			for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
				String definitionPortName = portDefinition.getName();
				if (referencePortName.equals(definitionPortName)) {
					return portDefinition;
				}
			}
		}
		// we found a module, but that module doesn't have a matching port. At least resolve to the module. TODO test if this works!
		return targetModule;
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return instancePortName.getIdentifier().getText();
	}

	@Override
	public PsiElement handleElementRename(String newName) throws IncorrectOperationException {
		return PsiUtil.setText(instancePortName.getIdentifier(), newName);
	}

	@Override
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (psiElement instanceof PortDefinition) {
			String newName = ((PsiNamedElement) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(instancePortName.getIdentifier(), newName);
			}
		}
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(PsiElement psiElement) {
		if (psiElement instanceof PortDefinition) {
			String candidatePortName = ((PortDefinition) psiElement).getName();
			if (candidatePortName != null && candidatePortName.equals(getCanonicalText())) {
				PsiElement resolved = resolve();
				return (resolved != null && resolved.equals(psiElement));
			}
		}
		return false;
	}

	@NotNull
	@Override
	public Object[] getVariants() {

		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data

		List<Object> variants = new ArrayList<>();
		PsiElement resolvedModule = resolveModule();
		if (resolvedModule instanceof Module) {
			Module targetModule = (Module)resolvedModule;
			for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
				for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
					String definitionPortName = portDefinition.getName();
					if (definitionPortName != null) {
						variants.add(definitionPortName);
					}
				}
			}
		}
		return variants.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
