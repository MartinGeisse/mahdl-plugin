package name.martingeisse.mahdl.plugin.processor.statement;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedAssignment extends ProcessedStatement {

	private final ProcessedExpression leftHandSide;
	private final ProcessedExpression rightHandSide;

	public ProcessedAssignment(ProcessedExpression leftHandSide, ProcessedExpression rightHandSide) {
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
	}

	public ProcessedExpression getLeftHandSide() {
		return leftHandSide;
	}

	public ProcessedExpression getRightHandSide() {
		return rightHandSide;
	}

}
