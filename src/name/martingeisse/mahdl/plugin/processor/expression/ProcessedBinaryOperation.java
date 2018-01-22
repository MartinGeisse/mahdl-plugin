package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedBinaryOperation extends ProcessedExpression {

	private final ProcessedExpression leftOperand;
	private final ProcessedExpression rightOperand;
	private final ProcessedBinaryOperator operator;

	public ProcessedBinaryOperation(ProcessedExpression leftOperand, ProcessedExpression rightOperand, ProcessedBinaryOperator operator) throws TypeErrorException {
		super(operator.checkTypes(leftOperand.getDataType(), rightOperand.getDataType()));
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

}
