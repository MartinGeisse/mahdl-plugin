/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public class ProcessedFunctionCall extends ProcessedExpression {

	private final StandardFunction function;
	private final ImmutableList<ProcessedExpression> arguments;

	public ProcessedFunctionCall(PsiElement errorSource,
								 StandardFunction function,
								 ImmutableList<ProcessedExpression> arguments) throws TypeErrorException {
		super(errorSource, function.checkType(arguments));
		this.function = function;
		this.arguments = arguments;
	}

	public StandardFunction getFunction() {
		return function;
	}

	public ImmutableList<ProcessedExpression> getArguments() {
		return arguments;
	}

}
