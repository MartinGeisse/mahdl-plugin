package name.martingeisse.mahdl.plugin.processor.constant;

import name.martingeisse.mahdl.plugin.input.psi.*;

import java.math.BigInteger;

/**
 * Helps evaluate binary expressions that operate on two integer values.
 */
class BinaryOperatorUtil {

	/**
	 * Evaluates a constant IVO -- see the language documentation for details on IVOs.
	 */
	static ConstantValue evaluateIntegerVectorOperator(BinaryOperation expression, BigInteger leftOperand, BigInteger rightOperand) throws OperatorException {
		if (expression instanceof Expression_BinaryConcat) {
			throw new OperatorException("concatenation should not be passed to this method");
		} else if (expression instanceof Expression_BinaryAnd) {
			return new ConstantValue.Integer(leftOperand.and(rightOperand));
		} else if (expression instanceof Expression_BinaryOr) {
			return new ConstantValue.Integer(leftOperand.or(rightOperand));
		} else if (expression instanceof Expression_BinaryXor) {
			return new ConstantValue.Integer(leftOperand.xor(rightOperand));
		} else if (expression instanceof Expression_BinaryPlus) {
			return new ConstantValue.Integer(leftOperand.add(rightOperand));
		} else if (expression instanceof Expression_BinaryMinus) {
			return new ConstantValue.Integer(leftOperand.subtract(rightOperand));
		} else if (expression instanceof Expression_BinaryTimes) {
			return new ConstantValue.Integer(leftOperand.multiply(rightOperand));
		} else if (expression instanceof Expression_BinaryDividedBy) {
			if (rightOperand.equals(BigInteger.ZERO)) {
				throw new OperatorException("division by zero");
			}
			return new ConstantValue.Integer(leftOperand.divide(rightOperand));
		} else if (expression instanceof Expression_BinaryRemainder) {
			if (rightOperand.equals(BigInteger.ZERO)) {
				throw new OperatorException("remainder from division by zero");
			}
			return new ConstantValue.Integer(leftOperand.remainder(rightOperand));
		} else if (expression instanceof Expression_BinaryShiftLeft) {
			int rightInt;
			try {
				rightInt = rightOperand.intValueExact();
			} catch (ArithmeticException e) {
				throw new OperatorException("shift amount too large: " + rightOperand);
			}
			return new ConstantValue.Integer(leftOperand.shiftLeft(rightInt));
		} else if (expression instanceof Expression_BinaryShiftRight) {
			int rightInt;
			try {
				rightInt = rightOperand.intValueExact();
			} catch (ArithmeticException e) {
				throw new OperatorException("shift amount too large: " + rightOperand);
			}
			return new ConstantValue.Integer(leftOperand.shiftRight(rightInt));
		} else if (expression instanceof Expression_BinaryEqual) {
			return new ConstantValue.Bit(leftOperand.equals(rightOperand));
		} else if (expression instanceof Expression_BinaryNotEqual) {
			return new ConstantValue.Bit(!leftOperand.equals(rightOperand));
		} else if (expression instanceof Expression_BinaryLessThan) {
			return new ConstantValue.Bit(leftOperand.compareTo(rightOperand) < 0);
		} else if (expression instanceof Expression_BinaryLessThanOrEqual) {
			return new ConstantValue.Bit(leftOperand.compareTo(rightOperand) <= 0);
		} else if (expression instanceof Expression_BinaryGreaterThan) {
			return new ConstantValue.Bit(leftOperand.compareTo(rightOperand) > 0);
		} else if (expression instanceof Expression_BinaryGreaterThanOrEqual) {
			return new ConstantValue.Bit(leftOperand.compareTo(rightOperand) >= 0);
		} else {
			throw new OperatorException("unknown operator");
		}
	}

	static boolean evaluateLogicalOperator(BinaryOperation expression, boolean leftOperand, boolean rightOperand) throws OperatorException {
		if (expression instanceof Expression_BinaryAnd) {
			return leftOperand & rightOperand;
		} else if (expression instanceof Expression_BinaryOr) {
			return leftOperand | rightOperand;
		} else if (expression instanceof Expression_BinaryXor) {
			return leftOperand ^ rightOperand;
		} else if (expression instanceof Expression_BinaryEqual) {
			return leftOperand == rightOperand;
		} else if (expression instanceof Expression_BinaryNotEqual) {
			return leftOperand != rightOperand;
		} else {
			throw new OperatorException("unknown operator");
		}
	}

	public static class OperatorException extends Exception {
		public OperatorException(String message) {
			super(message);
		}
	}

}
