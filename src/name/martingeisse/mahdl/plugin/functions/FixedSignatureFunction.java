/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.google.common.collect.ImmutableList;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public abstract class FixedSignatureFunction implements BuiltinFunction {

	private final ImmutableList<ProcessedDataType> argumentTypes;

	public FixedSignatureFunction(ImmutableList<ProcessedDataType> argumentTypes) {
		this.argumentTypes = argumentTypes;
	}

	public ImmutableList<ProcessedDataType> getArgumentTypes() {
		return argumentTypes;
	}

	public String getSignatureText() {
		StringBuilder builder = new StringBuilder(getName());
		builder.append('(');
		boolean first = true;
		for (ProcessedDataType expectedType : argumentTypes) {
			if (first) {
				first = false;
			} else {
				builder.append(", ");
			}
			builder.append(expectedType);
		}
		builder.append(')');
		return builder.toString();
	}

	@NotNull
	@Override
	public ProcessedDataType checkType(@NotNull List<ProcessedExpression> arguments) throws FunctionParameterException {
		if (arguments.size() != argumentTypes.size()) {
			throw new FunctionParameterException(getSignatureText() + " cannot be invoked with " + arguments.size() + " arguments");
		}
		for (int i = 0; i < argumentTypes.size(); i++) {
			if (!arguments.get(i).getDataType().equals(argumentTypes.get(i))) {
				throw new FunctionParameterException("argument #" + i + " has type " + arguments.get(i).getDataType() +
					", expected " + argumentTypes.get(i), i);
			}
		}
		return internalCheckType(arguments);
	}

	@NotNull
	protected abstract ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments) throws FunctionParameterException;

}
