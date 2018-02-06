package name.martingeisse.mahdl.plugin.processor.statement;

import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedIf extends ProcessedStatement {

	private final ProcessedExpression condition;
	private final ProcessedStatement thenBranch;
	private final ProcessedStatement elseBranch;

	public ProcessedIf(ProcessedExpression condition, ProcessedStatement thenBranch, ProcessedStatement elseBranch) {
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

	public ProcessedExpression getCondition() {
		return condition;
	}

	public ProcessedStatement getThenBranch() {
		return thenBranch;
	}

	public ProcessedStatement getElseBranch() {
		return elseBranch;
	}

}
