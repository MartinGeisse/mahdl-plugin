package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.ErrorHandler;

/**
 *
 */
public class ExpressionProcessor {

	private final ErrorHandler errorHandler;
	private final LocalDefinitionResolver localDefinitionResolver;

	public ExpressionProcessor(ErrorHandler errorHandler, LocalDefinitionResolver localDefinitionResolver) {
		this.errorHandler = errorHandler;
		this.localDefinitionResolver = localDefinitionResolver;
	}



}
