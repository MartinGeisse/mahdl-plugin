/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.google.common.collect.ImmutableList;
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
public abstract class LoadMemoryFileFunction extends FixedSignatureFunction {

	public LoadMemoryFileFunction(ImmutableList<ProcessedDataType> argumentTypes) {
		super(ImmutableList.of(
			ProcessedDataType.Text.INSTANCE,
			ProcessedDataType.Integer.INSTANCE,
			ProcessedDataType.Integer.INSTANCE
		));
	}

	@NotNull
	@Override
	protected ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments, ErrorHandler errorHandler) {
		ProcessedExpression.FormallyConstantEvaluationContext context = new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler);
		int firstSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		int secondSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		return new ProcessedDataType.Memory(firstSize, secondSize);
	}

	@NotNull
	@Override
	public ConstantValue applyToConstantValues(@NotNull PsiElement errorSource, @NotNull List<ConstantValue> arguments, @NotNull ProcessedExpression.FormallyConstantEvaluationContext context) {
		String text = arguments.get(0).convertToString();
		int firstSize = arguments.get(1).convertToInteger().intValueExact();
		int secondSize = arguments.get(2).convertToInteger().intValueExact();

		/*
		TODO: at which time should the file be loaded? How does it get located?
		 */

		throw new UnsupportedOperationException();
	}

}
