package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public abstract class ProcessedExpression {

	private final PsiElement errorSource;
	private final ProcessedDataType dataType;

	public ProcessedExpression(PsiElement errorSource, ProcessedDataType dataType) {
		this.errorSource = errorSource;
		this.dataType = dataType;
	}

	public PsiElement getErrorSource() {
		return errorSource;
	}

	public ProcessedDataType getDataType() {
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

	protected abstract ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context);

	public static final class FormallyConstantEvaluationContext {

		private final ImmutableMap<String, ConstantValue> definedConstants;
		private final ErrorHandler errorHandler;

		public FormallyConstantEvaluationContext(ImmutableMap<String, ConstantValue> definedConstants, ErrorHandler errorHandler) {
			this.definedConstants = definedConstants;
			this.errorHandler = errorHandler;
		}

		public ImmutableMap<String, ConstantValue> getDefinedConstants() {
			return definedConstants;
		}

		public ErrorHandler getErrorHandler() {
			return errorHandler;
		}

		public final ConstantValue.Unknown error(PsiElement errorSource, String message) {
			errorHandler.onError(errorSource, message);
			return ConstantValue.Unknown.INSTANCE;
		}

		public final ConstantValue.Unknown error(ProcessedExpression errorSource, String message) {
			return error(errorSource.getErrorSource(), message);
		}

		public final ConstantValue.Unknown notConstant(PsiElement errorSource) {
			return error(errorSource, "expected a formally constant expression");
		}

		public final ConstantValue.Unknown notConstant(ProcessedExpression errorSource) {
			return notConstant(errorSource.getErrorSource());
		}

		public final ConstantValue.Unknown evaluationInconsistency(PsiElement errorSource, String message) {
			return error(errorSource, "internal error: detected an inconsistency between static type check and constant evaluation" +
				(message == null ? "" : (": " + message)));
		}

		public final ConstantValue.Unknown evaluationInconsistency(ProcessedExpression errorSource, String message) {
			return evaluationInconsistency(errorSource.getErrorSource(), message);
		}

		public final ConstantValue.Unknown evaluationInconsistency(PsiElement errorSource) {
			return evaluationInconsistency(errorSource, null);
		}

		public final ConstantValue.Unknown evaluationInconsistency(ProcessedExpression errorSource) {
			return evaluationInconsistency(errorSource.getErrorSource());
		}

	}

}
