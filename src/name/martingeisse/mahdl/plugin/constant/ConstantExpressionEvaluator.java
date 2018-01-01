package name.martingeisse.mahdl.plugin.constant;

import com.intellij.lang.annotation.AnnotationHolder;
import name.martingeisse.mahdl.plugin.input.psi.Expression;

import java.util.Map;

/**
 *
 */
public class ConstantExpressionEvaluator {

	private final Map<String, ConstantValue> definedConstants;

	public ConstantExpressionEvaluator(Map<String, ConstantValue> definedConstants) {
		this.definedConstants = definedConstants;
	}

	/**
	 * Returns null for non-constant expressions or errors.
	 */
	public ConstantValue evaluate(Expression expression) {
		return evaluate(expression, null);
	}

	/**
	 * Returns null for non-constant expressions or errors. Those are also reported to the specified annotation holder,
	 * if any.
	 */
	public ConstantValue evaluate(Expression expression, AnnotationHolder annotationHolder) {
		// TODO
	}

}
