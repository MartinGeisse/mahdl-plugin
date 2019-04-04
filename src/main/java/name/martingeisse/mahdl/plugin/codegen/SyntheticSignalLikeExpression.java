/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class SyntheticSignalLikeExpression extends ProcessedExpression {

	private final String name;

	public SyntheticSignalLikeExpression(@NotNull PsiElement errorSource, @NotNull ProcessedDataType dataType, String name) {
		super(errorSource, dataType);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return context.notConstant(this);
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
