package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

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

		// handle string concatenation
		if (operator == ProcessedBinaryOperator.TEXT_CONCAT) {
			return new ConstantValue.Text(leftOperandValue.convertToString() + rightOperandValue.convertToString());
		}

		// handle vector concatenation (for concat, bits have been converted to vectors of size 1 by now)
		if (operator == ProcessedBinaryOperator.VECTOR_CONCAT) {
			ConstantValue.Vector leftVector = (ConstantValue.Vector) leftOperandValue;
			ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;
			BitSet resultBits = rightVector.getBits();
			BitSet leftBits = leftVector.getBits();
			for (int i = 0; i < leftVector.getSize(); i++) {
				resultBits.set(rightVector.getSize() + i, leftBits.get(i));
			}
			return new ConstantValue.Vector(leftVector.getSize() + rightVector.getSize(), resultBits);
		}

		// with concatenation handled, only logical operators can handle bit values, and only if both operands are bits
		if ((leftOperandValue instanceof ConstantValue.Bit) != (rightOperandValue instanceof ConstantValue.Bit)) {
			return context.evaluationInconsistency(this, "only one operand has bit type");
		}
		if (leftOperandValue instanceof ConstantValue.Bit) {
			boolean leftBoolean = ((ConstantValue.Bit) leftOperandValue).isSet();
			boolean rightBoolean = ((ConstantValue.Bit) rightOperandValue).isSet();
			try {
				return new ConstantValue.Bit(operator.evaluateLogicalOperator(leftBoolean, rightBoolean));
			} catch (ProcessedBinaryOperator.OperatorInconsistencyException e) {
				return context.evaluationInconsistency(this, e.getMessage());
			}
		}

		// all other operands are IVOs
		if (!(leftOperandValue instanceof ConstantValue.Vector) && !(leftOperandValue instanceof ConstantValue.Integer)) {
			return context.evaluationInconsistency(this, "wrong left operand for IVO: " + leftOperandValue);
		}
		if (!(rightOperandValue instanceof ConstantValue.Vector) && !(rightOperandValue instanceof ConstantValue.Integer)) {
			return context.evaluationInconsistency(this, "wrong right operand for IVO: " + rightOperandValue);
		}

		// perform the corresponding integer operation and convert the result to the type of the expression
		BigInteger leftInteger = leftOperandValue.convertToInteger();
		BigInteger rightInteger = rightOperandValue.convertToInteger();
		ConstantValue integerResultValue;
		BigInteger resultInteger;
		try {
			integerResultValue = operator.evaluateIntegerVectorOperator(leftInteger, rightInteger);
			resultInteger = integerResultValue.convertToInteger();
			if (resultInteger == null) {
				return context.evaluationInconsistency(this, "got result value of wrong type for binary operator: " + integerResultValue.getDataTypeFamily());
			}
		} catch (ProcessedBinaryOperator.OperatorInconsistencyException e) {
			return context.evaluationInconsistency(this, e.getMessage());
		} catch (ProcessedBinaryOperator.OperandValueException e) {
			return context.error(this, e.getMessage());
		}
		if (getDataType() instanceof ProcessedDataType.Integer) {
			return integerResultValue;
		} else if (getDataType() instanceof ProcessedDataType.Vector) {
			return new ConstantValue.Vector(((ProcessedDataType.Vector) getDataType()).getSize(), resultInteger);
		} else {
			return context.evaluationInconsistency(this, "unexpected result type for IVO: " + getDataType());
		}

	}

}
