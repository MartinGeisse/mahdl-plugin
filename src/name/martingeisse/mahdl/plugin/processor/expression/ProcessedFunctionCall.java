/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.FunctionParameterException;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;

import java.util.ArrayList;
import java.util.List;

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
			return function.applyToConstantValues(argumentValues.toArray(new ConstantValue[argumentValues.size()]));
		} catch (FunctionParameterException e) {
			return context.error(this, e.getMessage());
		}
	}

}
