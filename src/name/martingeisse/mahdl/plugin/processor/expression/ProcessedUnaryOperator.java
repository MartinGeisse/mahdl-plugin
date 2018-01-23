package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public enum ProcessedUnaryOperator {

	NOT,
	PLUS,
	MINUS;

	@NotNull
	public ProcessedDataType checkType(@NotNull ProcessedDataType operandType) throws TypeErrorException {
		if (operandType instanceof ProcessedDataType.Unknown) {
			return operandType;
		}
		if (this == NOT) {
			if (operandType instanceof ProcessedDataType.Bit || operandType instanceof ProcessedDataType.Vector) {
				return operandType;
			}
		} else {
			if (operandType instanceof ProcessedDataType.Integer || operandType instanceof ProcessedDataType.Vector) {
				return operandType;
			}
		}
		throw new TypeErrorException();
	}

}
