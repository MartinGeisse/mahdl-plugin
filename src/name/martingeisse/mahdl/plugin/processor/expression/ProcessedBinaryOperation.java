package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression_BinaryConcat;
import name.martingeisse.mahdl.plugin.input.psi.Expression_BinaryShiftLeft;
import name.martingeisse.mahdl.plugin.input.psi.Expression_BinaryShiftRight;
import name.martingeisse.mahdl.plugin.processor.constant.BinaryOperatorUtil;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public final class ProcessedBinaryOperation extends ProcessedExpression {

	private final ProcessedExpression leftOperand;
	private final ProcessedExpression rightOperand;
	private final ProcessedBinaryOperator operator;

	public ProcessedBinaryOperation(PsiElement errorSource,
									ProcessedExpression leftOperand,
									ProcessedExpression rightOperand,
									ProcessedBinaryOperator operator) throws TypeErrorException {
		super(errorSource, operator.checkTypes(leftOperand.getDataType(), rightOperand.getDataType()));
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
		this.operator = operator;
	}

	public ProcessedExpression getLeftOperand() {
		return leftOperand;
	}

	public ProcessedExpression getRightOperand() {
		return rightOperand;
	}

	public ProcessedBinaryOperator getOperator() {
		return operator;
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {

		// determine operand values
		ConstantValue leftOperandValue = leftOperand.evaluateFormallyConstant(context);
		ConstantValue rightOperandValue = rightOperand.evaluateFormallyConstant(context);
		if (leftOperandValue instanceof ConstantValue.Unknown) {
			return leftOperandValue;
		}
		if (rightOperandValue instanceof ConstantValue.Unknown) {
			return rightOperandValue;
		}

		// Only logical operators can handle bit values, and only if both operands are bits.
		if ((leftOperandValue instanceof ConstantValue.Bit) != (rightOperandValue instanceof ConstantValue.Bit)) {
			return context.evaluationInconsistency(this, "only one operand has bit type");
		}
		if (leftOperandValue instanceof ConstantValue.Bit) {
			boolean leftBoolean = ((ConstantValue.Bit) leftOperandValue).isSet();
			boolean rightBoolean = ((ConstantValue.Bit) rightOperandValue).isSet();
			try {
				return new ConstantValue.Bit(BinaryOperatorUtil.evaluateLogicalOperator(expression, leftBoolean, rightBoolean));
			} catch (BinaryOperatorUtil.OperatorException e) {
				return error(expression, e.getMessage());
			}
		}

		// Concatenation can handle various types. Logical operators can handle bit values. All other operators can
		// only handle vectors and integers.
		if (expression instanceof Expression_BinaryConcat) {
			return evaluateConcatenation((Expression_BinaryConcat) expression, leftOperandValue, rightOperandValue);
		}
		if (!(leftOperandValue instanceof ConstantValue.Vector) && !(leftOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, leftOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as left operand here");
		}
		if (!(rightOperandValue instanceof ConstantValue.Vector) && !(rightOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, rightOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as right operand here");
		}

		//
		// Note about the type rules for TAIVOs (shift operators): For an expression that is known to be constant,
		// the special rules for TAIVOs disappear. So we don't have to watch out for them here -- either the expression
		// is constant, or we bail out anyway.
		//

		// perform the corresponding integer operation
		BigInteger leftInteger = leftOperandValue.convertToInteger();
		BigInteger rightInteger = rightOperandValue.convertToInteger();
		ConstantValue integerResultValue;
		BigInteger resultInteger;
		try {
			integerResultValue = BinaryOperatorUtil.evaluateIntegerVectorOperator(expression, leftInteger, rightInteger);
			resultInteger = integerResultValue.convertToInteger();
			if (resultInteger == null) {
				return error(expression, "got result value of wrong type for binary operator: " + integerResultValue.getDataTypeFamily());
			}
		} catch (BinaryOperatorUtil.OperatorException e) {
			return error(expression, e.getMessage());
		}

		// shifting can handle vectors of different size
		if (expression instanceof Expression_BinaryShiftLeft || expression instanceof Expression_BinaryShiftRight) {
			if (leftOperandValue instanceof ConstantValue.Vector) {
				int size = ((ConstantValue.Vector) leftOperandValue).getSize();
				return new ConstantValue.Vector(size, resultInteger);
			} else {
				return integerResultValue;
			}
		}

		// all other operators want the operands to be of the same size
		int resultSize;
		if (leftOperandValue instanceof ConstantValue.Vector) {
			resultSize = ((ConstantValue.Vector) leftOperandValue).getSize();
			if (rightOperandValue instanceof ConstantValue.Vector) {
				int rightSize = ((ConstantValue.Vector) rightOperandValue).getSize();
				if (rightSize != resultSize) {
					return error(expression, "vectors of different sizes (" + resultSize + " and " + rightSize + ") cannot be used with this operator");
				}
			}
		} else {
			if (rightOperandValue instanceof ConstantValue.Vector) {
				resultSize = ((ConstantValue.Vector) rightOperandValue).getSize();
			} else {
				return integerResultValue;
			}
		}
		return new ConstantValue.Vector(resultSize, resultInteger);
	}

	private ConstantValue evaluateConcatenation(Expression_BinaryConcat expression, ConstantValue leftOperandValue, ConstantValue rightOperandValue) {

		// string concatenation
		if (leftOperandValue instanceof ConstantValue.Text || rightOperandValue instanceof ConstantValue.Text) {
			return new ConstantValue.Text(leftOperandValue.convertToString() + rightOperandValue.convertToString());
		}

		// bit / vector concatenation
		if (leftOperandValue instanceof ConstantValue.Bit) {
			ConstantValue.Bit leftBit = (ConstantValue.Bit) leftOperandValue;

			if (rightOperandValue instanceof ConstantValue.Bit) {
				ConstantValue.Bit rightBit = (ConstantValue.Bit) rightOperandValue;

				BitSet resultBits = new BitSet();
				resultBits.set(1, leftBit.isSet());
				resultBits.set(0, rightBit.isSet());
				return new ConstantValue.Vector(2, resultBits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet resultBits = rightVector.getBits();
				resultBits.set(rightVector.getSize(), leftBit.isSet());
				return new ConstantValue.Vector(rightVector.getSize() + 1, resultBits);

			}

		} else if (leftOperandValue instanceof ConstantValue.Vector) {
			ConstantValue.Vector leftVector = (ConstantValue.Vector) leftOperandValue;

			if (rightOperandValue instanceof ConstantValue.Bit) {
				ConstantValue.Bit rightBit = (ConstantValue.Bit) rightOperandValue;

				BitSet resultBits = new BitSet();
				if (rightBit.isSet()) {
					resultBits.set(0);
				}
				BitSet leftBits = leftVector.getBits();
				for (int i = 0; i < leftVector.getSize(); i++) {
					resultBits.set(1 + i, leftBits.get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + 1, resultBits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet resultBits = rightVector.getBits();
				BitSet leftBits = leftVector.getBits();
				for (int i = 0; i < leftVector.getSize(); i++) {
					resultBits.set(rightVector.getSize() + i, leftBits.get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + rightVector.getSize(), resultBits);

			}

		}

		return error(expression, "concatenation operator cannot be used on typed " +
			leftOperandValue.getDataTypeFamily().getDisplayString() + " and " +
			rightOperandValue.getDataTypeFamily().getDisplayString());
	}


}
