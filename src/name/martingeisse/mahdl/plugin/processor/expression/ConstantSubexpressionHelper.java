package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.ModuleProcessor;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import org.jetbrains.annotations.NotNull;

/**
 * Helper object to try to evaluate a sub-expression as an (anonymous) constant. If the expression is not constant,
 * the error message is suppressed and the caller can type-check the expression as a run-time expression. All other
 * errors are reported normally and should cause the caller to skip the normal type check to avoid double-reporting.
 *
 * (Another problem would occur if a broken constant sub-expression got type-checked by the run-time type checker:
 * If such an expression deals with types that are only allowed in constant sub-expressions, such as integer, those
 * types would get reported as not run-time compatible, despite the fact that the sub-expression wasn't meant to be
 * used at run-time in the first place).
 */
public final class ConstantSubexpressionHelper {

	private final ConstantExpressionEvaluator exceptionThrowingConstantExpressionEvaluator;

	public ConstantSubexpressionHelper() {
		this.exceptionThrowingConstantExpressionEvaluator = new ConstantExpressionEvaluator((errorSource, message) -> {
			throw new ModuleProcessor.ConstantEvaluationException();
		}, module);
	}

	private ConstantValue tryEvaluateConstantExpression(@NotNull Expression expression) {
		try {
			return exceptionThrowingConstantExpressionEvaluator.evaluate(expression);
		} catch (ConstantEvaluationException e) {
			return ConstantValue.Unknown.INSTANCE;
		}
	}

	private static class ConstantEvaluationException extends RuntimeException {
	}

}
