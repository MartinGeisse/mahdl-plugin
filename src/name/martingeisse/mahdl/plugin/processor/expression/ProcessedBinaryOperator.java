package name.martingeisse.mahdl.plugin.processor.expression;

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
