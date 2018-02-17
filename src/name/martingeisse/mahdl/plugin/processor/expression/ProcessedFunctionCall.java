/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.BuiltinFunction;
import name.martingeisse.mahdl.plugin.functions.FunctionParameterException;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProcessedFunctionCall extends ProcessedExpression {

	private final BuiltinFunction function;
	private final ImmutableList<ProcessedExpression> arguments;

	public ProcessedFunctionCall(PsiElement errorSource,
								 ProcessedDataType returnType,
								 BuiltinFunction function,
								 ImmutableList<ProcessedExpression> arguments) {
		super(errorSource, returnType);
		this.function = function;
		this.arguments = arguments;
	}

	public BuiltinFunction getFunction() {
		return function;
	}

	public ImmutableList<ProcessedExpression> getArguments() {
		return arguments;
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		boolean error = false;
		List<ConstantValue> argumentValues = new ArrayList<>();
		for (ProcessedExpression argument : arguments) {
			ConstantValue argumentValue = argument.evaluateFormallyConstant(context);
			argumentValues.add(argumentValue);
			if (argumentValue instanceof ConstantValue.Unknown) {
				error = true;
			}
		}
		if (error) {
			return ConstantValue.Unknown.INSTANCE;
		}
		try {
			return function.applyToConstantValues(argumentValues);
		} catch (FunctionParameterException e) {
			return context.error(this, e.getMessage());
		}
	}

}
