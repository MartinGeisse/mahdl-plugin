/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public interface BuiltinFunction {

	@NotNull
	String getName();

	@NotNull
	ProcessedDataType checkType(@NotNull List<ProcessedExpression> arguments,
								ErrorHandler errorHandler)
		throws FunctionParameterException;

	@NotNull
	ConstantValue applyToConstantValues(@NotNull List<ConstantValue> arguments,
										ProcessedExpression.FormallyConstantEvaluationContext context)
		throws FunctionParameterException;

}
