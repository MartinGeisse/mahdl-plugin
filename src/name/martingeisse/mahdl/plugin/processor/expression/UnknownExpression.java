/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 * This expression is generated in case of errors.
 */
public final class UnknownExpression extends ProcessedExpression {

	public static final UnknownExpression INSTANCE = new UnknownExpression();

	public UnknownExpression() {
		super(ProcessedDataType.Unknown.INSTANCE);
	}

}
