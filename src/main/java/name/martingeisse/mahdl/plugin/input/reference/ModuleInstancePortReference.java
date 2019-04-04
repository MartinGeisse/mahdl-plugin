/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ModuleInstancePortReference implements PsiReference {

	private final InstancePortName instancePortName;

	public ModuleInstancePortReference(@NotNull InstancePortName instancePortName) {
		this.instancePortName = instancePortName;
	}

	@Override
	@NotNull
	public PsiElement getElement() {
		return instancePortName;
	}

	@Override
	@NotNull
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	private PsiElement resolveModule() {
		Expression_InstancePort expression = PsiUtil.getAncestor(instancePortName, Expression_InstancePort.class);
		if (expression == null) {
			return null;
		}
		PsiElement someElementInsideInstanceDefinition = expression.getInstanceName().getReference().resolve();
		if (someElementInsideInstanceDefinition == null) {
			return null;
		}
		ImplementationItem_ModuleInstanceDefinitionGroup moduleInstanceDefinitionGroup = PsiUtil.getAncestor(someElementInsideInstanceDefinition, ImplementationItem_ModuleInstanceDefinitionGroup.class);
		if (moduleInstanceDefinitionGroup == null) {
			// at least resolve to inside the instance
			return someElementInsideInstanceDefinition;
		}
		PsiElement moduleNameDefiningElement = moduleInstanceDefinitionGroup.getModuleName().getReference().resolve();
		if (moduleNameDefiningElement == null) {
			// the module name is unknown
			return moduleInstanceDefinitionGroup.getModuleName();
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
		Module targetModule = (Module) resolvedModule;
		String referencePortName = getCanonicalText();
		for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
			if (portDefinitionGroup instanceof PortDefinitionGroup_Valid) {
				for (PortDefinition portDefinition : ((PortDefinitionGroup_Valid) portDefinitionGroup).getDefinitions().getAll()) {
					String definitionPortName = portDefinition.getName();
					if (referencePortName.equals(definitionPortName)) {
						return portDefinition;
					}
				}
			}
		}
		// we found a module, but that module doesn't have a matching port. At least resolve to the module.
		return targetModule;
	}

	// Works similar to resolve(), but won't return anything other than a PortDefinition. That is, any failure case
	// doesn't resolve the reference "as good as we can" but just returns null.
	@Nullable
	public PortDefinition resolvePortDefinitionOnly() {
		PsiElement element = resolve();
		return (element instanceof PortDefinition ? (PortDefinition) element : null);
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return instancePortName.getIdentifier().getText();
	}

	@Override
	@NotNull
	public PsiElement handleElementRename(@Nullable String newName) throws IncorrectOperationException {
		if (newName == null) {
			throw new IncorrectOperationException("new name is null");
		}
		return PsiUtil.setText(instancePortName.getIdentifier(), newName);
	}

	@Override
	@NotNull
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
	public boolean isReferenceTo(@Nullable PsiElement psiElement) {
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
		List<String> portNames = new ArrayList<>();
		PsiElement resolvedModule = resolveModule();
		if (resolvedModule instanceof Module) {
			Module targetModule = (Module) resolvedModule;
			for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
				if (portDefinitionGroup instanceof PortDefinitionGroup_Valid) {
					for (PortDefinition portDefinition : ((PortDefinitionGroup_Valid) portDefinitionGroup).getDefinitions().getAll()) {
						String definitionPortName = portDefinition.getName();
						if (definitionPortName != null) {
							portNames.add(definitionPortName);
						}
					}
				}
			}
		}
		return portNames.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
