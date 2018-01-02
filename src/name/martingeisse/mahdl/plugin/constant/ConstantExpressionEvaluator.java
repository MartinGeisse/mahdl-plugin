package name.martingeisse.mahdl.plugin.constant;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.math.BigInteger;
import java.util.Map;

/**
 *
 */
public class ConstantExpressionEvaluator {

	private static final BigInteger MAX_INDEX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

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

			Literal literal = ((Expression_Literal) expression).getLiteral();
			if (literal instanceof Literal_Vector) {
				String text = ((Literal_Vector) literal).getValue().getText();
				return parseVector(text);
			} else if (literal instanceof Literal_Integer) {
				String text = ((Literal_Integer) literal).getValue().getText();
				return new ConstantValue.Integer(new BigInteger(text));
			} else if (literal instanceof Literal_Text) {
				String rawText = ((Literal_Text) literal).getValue().getText();
				return parseText(rawText);
			} else {
				return nonConstant(expression, annotationHolder);
			}

		} else if (expression instanceof Expression_Signal) {

			String name = ((Expression_Signal) expression).getSignalName().getText();
			ConstantValue value = definedConstants.get(name);
			if (value == null) {
				return error(expression, "undefined constant: '" + name + "'", annotationHolder);
			}
			return value;

		} else if (expression instanceof Expression_InstancePort) {

			return nonConstant(expression, annotationHolder);

		} else if (expression instanceof Expression_IndexSelection) {

			Expression_IndexSelection indexSelection = (Expression_IndexSelection)expression;
			ConstantValue containerValue = evaluate(indexSelection.getContainer(), annotationHolder);
			int containerSize;
			String containerTypeText;
			if (containerValue == null) {
				containerSize = -1;
				containerTypeText = "(unknown)";
			} else if (containerValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vectorValue = (ConstantValue.Vector)containerValue;
				containerSize = vectorValue.getSize();
				containerTypeText = vectorValue.getDataTypeDisplayString();
			} else if (containerValue instanceof ConstantValue.Memory) {
				ConstantValue.Memory memoryType = (ConstantValue.Memory)containerValue;
				containerSize = memoryType.getFirstSize();
				containerTypeText = memoryType.getDataTypeDisplayString();
			} else {
				return error(expression, "cannot index-select from an expression of type " +
					containerValue.getDataTypeFamilyDisplayString(), annotationHolder);
			}
			int intIndexValue = handleIndexValue(containerSize, containerTypeText, indexSelection.getIndex(), annotationHolder);
			if (intIndexValue < 0) {
				return null;
			}
			// TODO

		} else if (expression instanceof Expression_RangeSelection) {

			Expression_RangeSelection rangeSelection = (Expression_RangeSelection)expression;
			ConstantValue containerValue = evaluate(rangeSelection.getContainer(), annotationHolder);
			ConstantValue getFromIndexValue = evaluate(rangeSelection.getFrom(), annotationHolder);
			ConstantValue getToIndexValue = evaluate(rangeSelection.getTo(), annotationHolder);
			if (containerValue == null || getFromIndexValue == null || getToIndexValue == null) {
				return null;
			}
			if (containerValue instanceof ConstantValue.Vector) {
				// TODO
			} else {
				return error(expression, "cannot range-select from an expression of type " +
					containerValue.getDataTypeFamilyDisplayString(), annotationHolder);
			}

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

			return evaluate(((Expression_Parenthesized) expression).getExpression(), annotationHolder);

		} else {
			return nonConstant(expression, annotationHolder);
		}
	}

	private static ConstantValue error(PsiElement element, String message, AnnotationHolder annotationHolder) {
		if (annotationHolder != null) {
			annotationHolder.createErrorAnnotation(element, message);
		}
		return null;
	}

	private static ConstantValue nonConstant(PsiElement element, AnnotationHolder annotationHolder) {
		return error(element, "expression must be constant", annotationHolder);
	}

	private static ConstantValue.Vector parseVector(String text) {
		// TODO
	}

	private static ConstantValue.Text parseText(String rawText) {
		// TODO remote quotation marks; translate escape sequences

	}

	private int handleIndexValue(int containerSize, String containerType, Expression indexExpression, AnnotationHolder annotationHolder) {
		ConstantValue indexValue = evaluate(indexExpression, annotationHolder);
		if (indexValue == null) {
			return -1;
		}
		BigInteger numericIndexValue = indexValue.convertToInteger();
		if (numericIndexValue == null) {
			error(indexExpression, "value of type  " + indexValue.getDataTypeFamilyDisplayString() + " cannot be converted to integer", annotationHolder);
			return -1;
		}
		if (numericIndexValue.compareTo(BigInteger.ZERO) < 0) {
			error(indexExpression, "index is negative: " + numericIndexValue, annotationHolder);
			return -1;
		}
		if (numericIndexValue.compareTo(MAX_INDEX_VALUE) > 0) {
			error(indexExpression, "index too large: " + numericIndexValue, annotationHolder);
			return -1;
		}
		if (containerSize < 0) {
			// could not determine container type -- stop here
			return -1;
		}
		int intValue = numericIndexValue.intValue();
		if (intValue >= containerSize) {
			error(indexExpression, "index too large for type " + containerType + ": " + numericIndexValue, annotationHolder);
			return -1;
		}
		return intValue;
	}

}
