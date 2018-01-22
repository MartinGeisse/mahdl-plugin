package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedBinaryOperation extends ProcessedExpression {

	private final ProcessedExpression leftOperand;
	private final ProcessedExpression rightOperand;
	private final ProcessedUnaryOperator operator;

	public ProcessedBinaryOperation(ProcessedDataType dataType, ProcessedExpression leftOperand, ProcessedExpression rightOperand, ProcessedUnaryOperator operator) {
		super(dataType);
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
		this.operator = operator;
	}

}
