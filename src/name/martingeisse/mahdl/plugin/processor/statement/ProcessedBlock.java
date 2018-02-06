package name.martingeisse.mahdl.plugin.processor.statement;

import com.google.common.collect.ImmutableList;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedBlock extends ProcessedStatement {

	private final ImmutableList<ProcessedStatement> statements;

	public ProcessedBlock(ImmutableList<ProcessedStatement> statements) {
		this.statements = statements;
	}

	public ImmutableList<ProcessedStatement> getStatements() {
		return statements;
	}

}
