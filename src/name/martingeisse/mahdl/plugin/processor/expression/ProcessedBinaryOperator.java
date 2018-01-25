package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 * These operators mostly correspond to those from the language grammar, but they are much more restricted with
 * respect to types: For example, for TSIVOs, only integer/integer and vector/vector are supported; mixed operands
 * should be shielded by inserting type conversions.
 *
 * The concatenation operators for texts and for vectors have been split up here. They use the same PSI nodes because
 * they use the same textual symbol, but they are split up in processing because most code pieces treat them very
 * differently.
 */
public enum ProcessedBinaryOperator {

	AND,
	OR,
	XOR,
	TEXT_CONCAT,
	VECTOR_CONCAT,

	PLUS,
	MINUS,
	TIMES,
	DIVIDED_BY,
	REMAINDER,

	SHIFT_LEFT,
	SHIFT_RIGHT,

	EQUAL,
	NOT_EQUAL,
	LESS_THAN,
	LESS_THAN_OR_EQUAL,
	GREATER_THAN,
	GREATER_THAN_OR_EQUAL;

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

}
