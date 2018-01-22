package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedUnaryOperation extends ProcessedExpression {

	private final ProcessedExpression operand;
	private final ProcessedUnaryOperator operator;

	public ProcessedUnaryOperation(ProcessedDataType dataType, ProcessedExpression operand, ProcessedUnaryOperator operator) {
		super(dataType);
		this.operand = operand;
		this.operator = operator;
	}

}
