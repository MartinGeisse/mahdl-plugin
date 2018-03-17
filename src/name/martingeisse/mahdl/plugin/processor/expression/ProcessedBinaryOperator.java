/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

/**
 * These operators mostly correspond to those from the language grammar, but they are much more restricted with
 * respect to types: For example, for TSIVOs, only integer/integer and vector/vector are supported; mixed operands
 * should be shielded by inserting type conversions.
 * <p>
 * The concatenation operators for texts and for vectors have been split up here. They use the same PSI nodes because
 * they use the same textual symbol, but they are split up in processing because most code pieces treat them very
 * differently.
 */
public enum ProcessedBinaryOperator {

	// logical operators
	AND((x, y) -> x & y, BigInteger::and, null, false),
	OR((x, y) -> x | y, BigInteger::or, null, false),
	XOR((x, y) -> x ^ y, BigInteger::xor, null, false),

	// concatenation operators (handled specially during evaluation)
	TEXT_CONCAT(null, null, null, false),
	VECTOR_CONCAT(null, null, null, false),

	// arithmetic operators
	PLUS(null, BigInteger::add, null, false),
	MINUS(null, BigInteger::subtract, null, false),
	TIMES(null, BigInteger::multiply, null, false),
	DIVIDED_BY(null, BigInteger::divide, null, true),
	REMAINDER(null, BigInteger::remainder, null, true),

	// shift operators
	SHIFT_LEFT(null, null, null, false) {
		@Override
		public ConstantValue evaluateIntegerVectorOperator(BigInteger leftOperand, BigInteger rightOperand) throws OperandValueException {
			int rightInt;
			try {
				rightInt = rightOperand.intValueExact();
			} catch (ArithmeticException e) {
				throw new OperandValueException("shift amount too large: " + rightOperand);
			}
			return new ConstantValue.Integer(leftOperand.shiftLeft(rightInt));
		}
	},
	SHIFT_RIGHT(null, null, null, false) {
		@Override
		public ConstantValue evaluateIntegerVectorOperator(BigInteger leftOperand, BigInteger rightOperand) throws OperandValueException {
			int rightInt;
			try {
				rightInt = rightOperand.intValueExact();
			} catch (ArithmeticException e) {
				throw new OperandValueException("shift amount too large: " + rightOperand);
			}
			return new ConstantValue.Integer(leftOperand.shiftRight(rightInt));
		}
	},

	// equality and comparison operators
	EQUAL((x, y) -> x == y, null, null, false) {
		@Override
		public ConstantValue evaluateIntegerVectorOperator(BigInteger leftOperand, BigInteger rightOperand) {
			return new ConstantValue.Bit(leftOperand.equals(rightOperand));
		}
	},
	NOT_EQUAL((x, y) -> x != y, null, null, false) {
		@Override
		public ConstantValue evaluateIntegerVectorOperator(BigInteger leftOperand, BigInteger rightOperand) {
			return new ConstantValue.Bit(!leftOperand.equals(rightOperand));
		}
	},
	LESS_THAN(null, null, x -> x < 0, false),
	LESS_THAN_OR_EQUAL(null, null, x -> x <= 0, false),
	GREATER_THAN(null, null, x -> x > 0, false),
	GREATER_THAN_OR_EQUAL(null, null, x -> x >= 0, false);

	private final LogicalOperation logicalOperation;
	private final BinaryOperator<BigInteger> bigIntegerEquivalentOperator;
	private final CompareResultPredicate compareResultPredicate;
	private final boolean zeroCheckRightOperand;

	private interface LogicalOperation {
		boolean evaluate(boolean leftOperand, boolean rightOperand);
	}

	private interface CompareResultPredicate {
		boolean test(int compareResult);
	}

	ProcessedBinaryOperator(LogicalOperation logicalOperation, BinaryOperator<BigInteger> bigIntegerEquivalentOperator, CompareResultPredicate compareResultPredicate, boolean zeroCheckRightOperand) {
		this.logicalOperation = logicalOperation;
		this.bigIntegerEquivalentOperator = bigIntegerEquivalentOperator;
		this.compareResultPredicate = compareResultPredicate;
		this.zeroCheckRightOperand = zeroCheckRightOperand;
	}

	// NOTE: cannot detect concatenation operator because text/vector concatenation are detected by type, which is unknown here
	public static ProcessedBinaryOperator from(BinaryOperation operation) {
		if (operation instanceof Expression_BinaryAnd) {
			return AND;
		} else if (operation instanceof Expression_BinaryOr) {
			return OR;
		} else if (operation instanceof Expression_BinaryXor) {
			return XOR;
		} else if (operation instanceof Expression_BinaryConcat) {
			throw new IllegalArgumentException("concatenation should not be passed to this method");
		} else if (operation instanceof Expression_BinaryPlus) {
			return PLUS;
		} else if (operation instanceof Expression_BinaryMinus) {
			return MINUS;
		} else if (operation instanceof Expression_BinaryTimes) {
			return TIMES;
		} else if (operation instanceof Expression_BinaryDividedBy) {
			return DIVIDED_BY;
		} else if (operation instanceof Expression_BinaryRemainder) {
			return REMAINDER;
		} else if (operation instanceof Expression_BinaryShiftLeft) {
			return SHIFT_LEFT;
		} else if (operation instanceof Expression_BinaryShiftRight) {
			return SHIFT_RIGHT;
		} else if (operation instanceof Expression_BinaryEqual) {
			return EQUAL;
		} else if (operation instanceof Expression_BinaryNotEqual) {
			return NOT_EQUAL;
		} else if (operation instanceof Expression_BinaryLessThan) {
			return LESS_THAN;
		} else if (operation instanceof Expression_BinaryLessThanOrEqual) {
			return LESS_THAN_OR_EQUAL;
		} else if (operation instanceof Expression_BinaryGreaterThan) {
			return GREATER_THAN;
		} else if (operation instanceof Expression_BinaryGreaterThanOrEqual) {
			return GREATER_THAN_OR_EQUAL;
		} else {
			throw new IllegalArgumentException("unknown unary operation: " + operation);
		}
	}

	@NotNull
	public ProcessedDataType checkTypes(ProcessedDataType leftType, ProcessedDataType rightType) throws TypeErrorException {
		if (leftType instanceof ProcessedDataType.Unknown || rightType instanceof ProcessedDataType.Unknown) {

			// propagate unknown without raising further errors
			return ProcessedDataType.Unknown.INSTANCE;

		} else if (this == SHIFT_LEFT || this == SHIFT_RIGHT) {

			// the result type is the left type. The possible const-ness requirement for the right operand is not checked here.
			if ((leftType instanceof ProcessedDataType.Integer || leftType instanceof ProcessedDataType.Vector) &&
				(rightType instanceof ProcessedDataType.Integer || rightType instanceof ProcessedDataType.Vector)) {
				return leftType;
			}

		} else if (this == TEXT_CONCAT) {

			if (leftType instanceof ProcessedDataType.Text && rightType instanceof ProcessedDataType.Text) {
				return leftType;
			}

		} else if (this == VECTOR_CONCAT) {

			if (leftType instanceof ProcessedDataType.Vector && rightType instanceof ProcessedDataType.Vector) {
				int leftSize = ((ProcessedDataType.Vector) leftType).getSize();
				int rightSize = ((ProcessedDataType.Vector) rightType).getSize();
				return new ProcessedDataType.Vector(leftSize + rightSize);
			}

		} else {

			// logical operators work for bit/integer/vector, the others for integer/vector only
			if (this == AND || this == OR || this == XOR) {
				if (leftType instanceof ProcessedDataType.Bit && rightType instanceof ProcessedDataType.Bit) {
					return leftType;
				}
			}

			// predicate operators return a bit
			if (this == EQUAL || this == NOT_EQUAL || this == LESS_THAN || this == LESS_THAN_OR_EQUAL || this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL) {
				if (leftType.equals(rightType)) {
					return ProcessedDataType.Bit.INSTANCE;
				} else {
					throw new TypeErrorException();
				}
			}

			// the others return their operand type
			if (leftType instanceof ProcessedDataType.Integer && rightType instanceof ProcessedDataType.Integer) {
				return leftType;
			}
			if (leftType instanceof ProcessedDataType.Vector && rightType instanceof ProcessedDataType.Vector) {
				if (((ProcessedDataType.Vector) leftType).getSize() == ((ProcessedDataType.Vector) rightType).getSize()) {
					return leftType;
				}
			}

		}
		throw new TypeErrorException();
	}

	public boolean evaluateLogicalOperator(boolean leftOperand, boolean rightOperand) throws OperatorInconsistencyException {
		if (logicalOperation == null) {
			throw new OperatorInconsistencyException("evaluateLogicalOperator() not supported for this operator");
		}
		return logicalOperation.evaluate(leftOperand, rightOperand);
	}

	@NotNull
	public ConstantValue evaluateIntegerVectorOperator(BigInteger leftOperand, BigInteger rightOperand) throws OperatorInconsistencyException, OperandValueException {
		if (zeroCheckRightOperand && rightOperand.equals(BigInteger.ZERO)) {
			throw new OperandValueException("right operand is zero");
		}
		if (bigIntegerEquivalentOperator != null) {
			return new ConstantValue.Integer(bigIntegerEquivalentOperator.apply(leftOperand, rightOperand));
		} else if (compareResultPredicate != null) {
			return new ConstantValue.Bit(compareResultPredicate.test(leftOperand.compareTo(rightOperand)));
		} else {
			throw new OperatorInconsistencyException("evaluateIntegerVectorOperator() not supported for this operator");
		}
	}

	public static class OperandValueException extends Exception {
		public OperandValueException(String message) {
			super(message);
		}
	}

	public static class OperatorInconsistencyException extends Exception {
		public OperatorInconsistencyException(String message) {
			super(message);
		}
	}

}
