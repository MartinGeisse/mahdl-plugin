package name.martingeisse.mahdl.plugin.processor.statement;

import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_DoBlock;
import name.martingeisse.mahdl.plugin.input.psi.Statement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;

/**
 *
 */
public final class StatementProcessor {

	private final ErrorHandler errorHandler;

	public StatementProcessor(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public ProcessedDoBlock process(ImplementationItem_DoBlock doBlock) {

	}

	public ProcessedStatement process(Statement statement) {

	}

}
