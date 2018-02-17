/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public interface BuiltinFunction {

	@NotNull
	String getName();

	@NotNull
	ProcessedDataType checkType(@NotNull PsiElement errorSource,
								@NotNull List<ProcessedExpression> arguments,
								@NotNull ErrorHandler errorHandler);

	@NotNull
	ConstantValue applyToConstantValues(@NotNull PsiElement errorSource,
										@NotNull List<ConstantValue> arguments,
										@NotNull ProcessedExpression.FormallyConstantEvaluationContext context);

}
