package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;

import java.math.BigInteger;

/**
 *
 */
public final class ProcessedUnaryOperation extends ProcessedExpression {

	private final ProcessedExpression operand;
	private final ProcessedUnaryOperator operator;

	public ProcessedUnaryOperation(PsiElement errorSource,
								   ProcessedExpression operand,
								   ProcessedUnaryOperator operator) throws TypeErrorException {
		super(errorSource, operator.checkType(operator.checkType(operand.getDataType())));
		this.operand = operand;
		this.operator = operator;
	}

	public ProcessedExpression getOperand() {
		return operand;
	}

	public ProcessedUnaryOperator getOperator() {
		return operator;
	}

	@Override
	public ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {

		// determine operand value
		ConstantValue operandValue = operand.evaluateFormallyConstant(context);
		if (operandValue instanceof ConstantValue.Unknown) {
			return operandValue;
		}

		// Only unary NOT can handle bit values. All other operators require a vector or integer.
		if (operator == ProcessedUnaryOperator.NOT && operandValue instanceof ConstantValue.Bit) {
			boolean bitOperandValue = ((ConstantValue.Bit) operandValue).isSet();
			return new ConstantValue.Bit(!bitOperandValue);
		}
		if (!(operandValue instanceof ConstantValue.Vector) && !(operandValue instanceof ConstantValue.Integer)) {
			return context.evaluationInconsistency(this, "found unary operation " + operator + " " + operandValue);
		}

		// shortcut for unary plus, which doesn't actually do anything
		if (operator == ProcessedUnaryOperator.PLUS) {
			return operandValue;
		}

		// perform the corresponding integer operation
		BigInteger integerOperand = operandValue.convertToInteger();
		if (integerOperand == null) {
			return context.evaluationInconsistency(this, "could not convert operand to integer");
		}
		BigInteger integerResult;
		if (operator == ProcessedUnaryOperator.NOT) {
			integerResult = integerOperand.not();
		} else if (operator == ProcessedUnaryOperator.MINUS) {
			integerResult = integerOperand.negate();
		} else {
			return context.evaluationInconsistency(this, "unknown operator");
		}

		// if the operand was a vector, turn the result into a vector of the same size, otherwise return as integer
		if (operandValue instanceof ConstantValue.Vector) {
			int size = ((ConstantValue.Vector) operandValue).getSize();
			return new ConstantValue.Vector(size, integerResult);
		} else {
			return new ConstantValue.Integer(integerResult);
		}

	}

}
