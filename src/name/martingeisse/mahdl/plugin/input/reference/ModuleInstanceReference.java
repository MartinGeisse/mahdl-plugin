/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ModuleInstanceReference extends LocalReference {

	private final InstanceReferenceName instanceReferenceName;

	public ModuleInstanceReference(@NotNull InstanceReferenceName instanceReferenceName) {
		this.instanceReferenceName = instanceReferenceName;
	}

	@Override
	@NotNull
	public LeafPsiElement getElement() {
		return instanceReferenceName.getIdentifier();
	}

	@Override
	protected boolean isElementTargetable(@Nullable PsiElement potentialTarget) {
		return (potentialTarget instanceof PortDefinition || potentialTarget instanceof SignalLikeDefinition || potentialTarget instanceof ImplementationItem_ModuleInstance);
	}

	@NotNull
	@Override
	public Object[] getVariants() {
		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data
		return VariantsUtil.getExpressionVariants(instanceReferenceName);
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
