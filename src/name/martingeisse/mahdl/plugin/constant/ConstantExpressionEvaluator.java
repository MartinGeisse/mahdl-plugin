package name.martingeisse.mahdl.plugin.constant;

import com.intellij.lang.annotation.AnnotationHolder;
import name.martingeisse.mahdl.plugin.input.psi.*;

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
		if (expression instanceof Expression_Literal) {
			// TODO
		} else if (expression instanceof Expression_Signal) {
			// TODO
		} else if (expression instanceof Expression_InstancePort) {
			// TODO
		} else if (expression instanceof Expression_IndexSelection) {
			// TODO
		} else if (expression instanceof Expression_RangeSelection) {
			// TODO
		} else if (expression instanceof UnaryOperation) {
			// TODO
		} else if (expression instanceof BinaryOperation) {
			// TODO
		} else if (expression instanceof Expression_Mux) {
			// TODO
		} else if (expression instanceof Expression_FunctionCall) {
			// TODO
		} else if (expression instanceof Expression_Parenthesized) {
			// TODO
		} else {
			// TODO annotate
			return null;
		}
	}

}
