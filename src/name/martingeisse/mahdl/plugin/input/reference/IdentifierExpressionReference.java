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
public class IdentifierExpressionReference extends LocalReference {

	private final Expression_Identifier expression;

	public IdentifierExpressionReference(@NotNull Expression_Identifier expression) {
		this.expression = expression;
	}

	@Override
	@NotNull
	public LeafPsiElement getElement() {
		return expression.getIdentifier();
	}

	@Override
	protected boolean isElementTargetable(@Nullable PsiElement potentialTarget) {
		return (potentialTarget instanceof PortDefinition || potentialTarget instanceof SignalLikeDefinition || potentialTarget instanceof ImplementationItem_ModuleInstance);
	}

	@NotNull
	@Override
	public Object[] getVariants() {
		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data
		return VariantsUtil.getExpressionVariants(expression);
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
