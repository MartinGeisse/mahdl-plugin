/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class Nop extends ProcessedStatement {

	public Nop(PsiElement errorSource) {
		super(errorSource);
	}

}
