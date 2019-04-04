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
public final class ProcessedIf extends ProcessedStatement {

	private final ProcessedExpression condition;
	private final ProcessedStatement thenBranch;
	private final ProcessedStatement elseBranch;

	public ProcessedIf(PsiElement errorSource, ProcessedExpression condition, ProcessedStatement thenBranch, ProcessedStatement elseBranch) {
		super(errorSource);
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

	public ProcessedExpression getCondition() {
		return condition;
	}

	public ProcessedStatement getThenBranch() {
		return thenBranch;
	}

	public ProcessedStatement getElseBranch() {
		return elseBranch;
	}

}
