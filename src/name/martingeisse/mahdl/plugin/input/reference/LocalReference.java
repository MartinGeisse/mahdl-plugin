/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for local references to signal-likes and module instances. We use a base class that can resolve all of
 * those because they live in the same namespace. Even if a reference targets an object of the wrong kind, we still
 * resolve to that object to make life easier for users who have made a mistake.
 */
public abstract class LocalReference implements PsiReference {

	@Override
	@NotNull
	public abstract LeafPsiElement getElement();

	// note: may only return true for PsiNamedElement objects!
	protected abstract boolean isElementTargetable(@NotNull PsiElement potentialTarget);

	@Override
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	@Override
	public PsiElement resolve() {

		LeafPsiElement referenceElement = getElement();
		Module module = PsiUtil.getAncestor(referenceElement, Module.class);
		if (module == null) {
			return null;
		}
		String identifier = referenceElement.getText();

		// ports
		for (PortDefinitionGroup group : module.getPortDefinitionGroups().getAll()) {
			if (group instanceof PortDefinitionGroup_Valid) {
				for (PortDefinition definition : ((PortDefinitionGroup_Valid) group).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null && definitionName.equals(identifier)) {
						return definition;
					}
				}
			}
		}

		// local definitions
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				for (SignalLikeDefinition definition : ((ImplementationItem_SignalLikeDefinitionGroup) implementationItem).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null && definitionName.equals(identifier)) {
						return definition;
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				String instanceName = implementationItem.getName();
				if (instanceName != null && instanceName.equals(identifier)) {
					return ((ImplementationItem_ModuleInstance) implementationItem).getInstanceName();
				}
			}
		}

		return null;
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return getElement().getText();
	}

	@Override
	public PsiElement handleElementRename(@Nullable String newName) throws IncorrectOperationException {
		if (newName == null) {
			throw new IncorrectOperationException("new name is null");
		}
		return PsiUtil.setText(getElement(), newName);
	}

	@Override
	@NotNull
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (isElementTargetable(psiElement)) {
			String newName = ((PsiNamedElement) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(getElement(), newName);
			}
		}
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(@Nullable PsiElement psiElement) {
		if (psiElement != null && isElementTargetable(psiElement)) {
			String elementName = ((PsiNamedElement) psiElement).getName();
			if (elementName != null) {
				String thisName = getCanonicalText();
				if (elementName.equals(thisName)) {
					PsiElement resolved = resolve();
					return (resolved != null && resolved.equals(psiElement));
				}
			}
		}
		return false;
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
