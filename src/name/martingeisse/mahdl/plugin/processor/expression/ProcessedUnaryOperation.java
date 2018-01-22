package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedUnaryOperation extends ProcessedExpression {

	private final ProcessedExpression operand;
	private final ProcessedUnaryOperator operator;

	public ProcessedUnaryOperation(ProcessedExpression operand, ProcessedUnaryOperator operator) throws TypeErrorException {
		super(operator.checkType(operator.checkType(operand.getDataType())));
		this.operand = operand;
		this.operator = operator;
	}

	public ProcessedExpression getOperand() {
		return operand;
	}

	public ProcessedUnaryOperator getOperator() {
		return operator;
	}

}
