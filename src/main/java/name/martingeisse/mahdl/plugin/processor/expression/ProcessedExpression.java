/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.codegen.ExpressionVerilogGenerator;
import name.martingeisse.mahdl.plugin.codegen.ModuleHasErrorsException;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public abstract class ProcessedExpression {

	@NotNull
	private final PsiElement errorSource;

	@NotNull
	private final ProcessedDataType dataType;

	public ProcessedExpression(@NotNull PsiElement errorSource, @NotNull ProcessedDataType dataType) {
		this.errorSource = errorSource;
		this.dataType = dataType;
	}

	@NotNull
	public final PsiElement getErrorSource() {
		return errorSource;
	}

	@NotNull
	public final ProcessedDataType getDataType() {
		return dataType;
	}

	/**
	 * If this expression can be used as a bit literal, returns the corresponding expression that *is* a bit literal.
	 * Otherwise returns null.
	 * <p>
	 * The only expressions that can be used as a bit literal (except bit literals themselves) are the integer literals
	 * 0 and 1. (Any computed integer that is 0 or 1, even if formally constant, cannot be used as a bit literal).
	 * Before turning an integer literal into a bit literal, make sure you need a bit and not an integer!
	 */
	@Nullable
	public ProcessedExpression recognizeBitLiteral() {
		return null;
	}

	public final ConstantValue evaluateFormallyConstant(FormallyConstantEvaluationContext context) {
		ConstantValue value = evaluateFormallyConstantInternal(context);
		if (value == null) {
			return context.evaluationInconsistency(this, "evaluating this expression as formally constant returned null");
		} else if (value instanceof ConstantValue.Unknown) {
			return value;
		} else if (!value.getDataType().equals(dataType)) {
			return context.evaluationInconsistency(this, "evaluating this expression as formally constant returned a value of type " +
				value.getDataType() + ", but the expression type was " + dataType);
		} else {
			return value;
		}
	}

	@NotNull
	protected abstract ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context);

	@NotNull
	protected ProcessedExpression performFolding(@NotNull ErrorHandler errorHandler) {
		ProcessedExpression.FormallyConstantEvaluationContext context = new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler) {

			@Override
			@NotNull
			public ConstantValue.Unknown notConstant(@NotNull PsiElement errorSource) {
				throw new NotConstantException();
			}

			@Override
			@NotNull
			public ConstantValue.Unknown notConstant(@NotNull ProcessedExpression errorSource) {
				throw new NotConstantException();

			}
		};
		ConstantValue value;
		try {
			value = evaluateFormallyConstant(context);
		} catch (NotConstantException e) {
			return performSubFolding(errorHandler);
		}
		return new ProcessedConstantValue(errorSource, value);
	}

	private static class NotConstantException extends RuntimeException {
	}

	@NotNull
	protected abstract ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler);

	public static class FormallyConstantEvaluationContext {

		private final ErrorHandler errorHandler;

		public FormallyConstantEvaluationContext(@NotNull ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
		}

		@NotNull
		public ErrorHandler getErrorHandler() {
			return errorHandler;
		}

		@NotNull
		public ConstantValue.Unknown error(@NotNull PsiElement errorSource, @NotNull String message) {
			errorHandler.onError(errorSource, message);
			return ConstantValue.Unknown.INSTANCE;
		}

		@NotNull
		public ConstantValue.Unknown error(@NotNull ProcessedExpression errorSource, @NotNull String message) {
			return error(errorSource.getErrorSource(), message);
		}

		@NotNull
		public ConstantValue.Unknown notConstant(@NotNull PsiElement errorSource) {
			return error(errorSource, "expected a formally constant expression");
		}

		@NotNull
		public ConstantValue.Unknown notConstant(@NotNull ProcessedExpression errorSource) {
			return notConstant(errorSource.getErrorSource());
		}

		@NotNull
		public ConstantValue.Unknown evaluationInconsistency(@NotNull PsiElement errorSource, @NotNull String message) {
			return error(errorSource, "internal error: detected an inconsistency between static type check and constant evaluation" +
				(message == null ? "" : (": " + message)));
		}

		@NotNull
		public ConstantValue.Unknown evaluationInconsistency(@NotNull ProcessedExpression errorSource, @NotNull String message) {
			return evaluationInconsistency(errorSource.getErrorSource(), message);
		}

		@NotNull
		public ConstantValue.Unknown evaluationInconsistency(@NotNull PsiElement errorSource) {
			return evaluationInconsistency(errorSource, null);
		}

		@NotNull
		public ConstantValue.Unknown evaluationInconsistency(@NotNull ProcessedExpression errorSource) {
			return evaluationInconsistency(errorSource.getErrorSource());
		}

	}

}
