/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 * This expression is generated in case of errors.
 */
public final class UnknownExpression extends ProcessedExpression {

	public UnknownExpression(@NotNull PsiElement errorSource) {
		super(errorSource, ProcessedDataType.Unknown.INSTANCE);
	}

	@Override
	@NotNull
	protected ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {
		return ConstantValue.Unknown.INSTANCE;
	}

	@NotNull
	@Override
	protected ProcessedExpression performFolding(@NotNull ErrorHandler errorHandler) {
		return this;
	}

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		return this;
	}

}
