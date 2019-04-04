/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.Statement;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedDoBlock {

	private final ProcessedExpression clock;
	private final ProcessedStatement body;

	public ProcessedDoBlock(ProcessedExpression clock, ProcessedStatement body) {
		this.clock = clock;
		this.body = body;
	}

	public ProcessedExpression getClock() {
		return clock;
	}

	public ProcessedStatement getBody() {
		return body;
	}

}
