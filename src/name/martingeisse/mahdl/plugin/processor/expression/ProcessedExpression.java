/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
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

	/**
	 * If this expression can be used as a bit literal, returns the corresponding expression that *is* a bit literal.
	 * Otherwise returns null.
	 *
	 * The only expressions that can be used as a bit literal (except bit literals themselves) are the integer literals
	 * 0 and 1. (Any computed integer that is 0 or 1, even if formally constant, cannot be used as a bit literal).
	 * Before turning an integer literal into a bit literal, make sure you need a bit and not an integer!
	 */
	@Nullable
	public ProcessedExpression recognizeBitLiteral() {
		return null;
	}

	protected abstract ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context);

	public static class FormallyConstantEvaluationContext {

		private final ErrorHandler errorHandler;

		public FormallyConstantEvaluationContext(ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
		}

		public ErrorHandler getErrorHandler() {
			return errorHandler;
		}

		public ConstantValue.Unknown error(PsiElement errorSource, String message) {
			errorHandler.onError(errorSource, message);
			return ConstantValue.Unknown.INSTANCE;
		}

		public ConstantValue.Unknown error(ProcessedExpression errorSource, String message) {
			return error(errorSource.getErrorSource(), message);
		}

		public ConstantValue.Unknown notConstant(PsiElement errorSource) {
			return error(errorSource, "expected a formally constant expression");
		}

		public ConstantValue.Unknown notConstant(ProcessedExpression errorSource) {
			return notConstant(errorSource.getErrorSource());
		}

		public ConstantValue.Unknown evaluationInconsistency(PsiElement errorSource, String message) {
			return error(errorSource, "internal error: detected an inconsistency between static type check and constant evaluation" +
				(message == null ? "" : (": " + message)));
		}

		public ConstantValue.Unknown evaluationInconsistency(ProcessedExpression errorSource, String message) {
			return evaluationInconsistency(errorSource.getErrorSource(), message);
		}

		public ConstantValue.Unknown evaluationInconsistency(PsiElement errorSource) {
			return evaluationInconsistency(errorSource, null);
		}

		public ConstantValue.Unknown evaluationInconsistency(ProcessedExpression errorSource) {
			return evaluationInconsistency(errorSource.getErrorSource());
		}

	}

}
