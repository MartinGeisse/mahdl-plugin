/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedBlock extends ProcessedStatement {

	private final ImmutableList<ProcessedStatement> statements;

	public ProcessedBlock(PsiElement errorSource, ImmutableList<ProcessedStatement> statements) {
		super(errorSource);
		this.statements = statements;
	}

	public ImmutableList<ProcessedStatement> getStatements() {
		return statements;
	}

}
