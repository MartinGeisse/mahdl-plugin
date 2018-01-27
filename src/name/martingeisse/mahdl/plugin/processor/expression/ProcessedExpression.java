package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.util.Map;

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

	public abstract ConstantValue evaluateFormallyConstant(FormallyConstantEvaluationContext context);

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
			errorHandler.onError(errorSource, "expected a formally constant expression");
			return ConstantValue.Unknown.INSTANCE;
		}

		public final ConstantValue.Unknown notConstant(ProcessedExpression errorSource) {
			return notConstant(errorSource.getErrorSource());
		}

	}

}
