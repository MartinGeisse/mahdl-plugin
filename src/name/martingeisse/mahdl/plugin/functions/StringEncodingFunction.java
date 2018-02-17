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
public abstract class StringEncodingFunction extends FixedSignatureFunction {

	public StringEncodingFunction() {
		super(ImmutableList.of(
			ProcessedDataType.Text.INSTANCE,
			ProcessedDataType.Integer.INSTANCE
		));
	}

	@NotNull
	@Override
	protected ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments) throws FunctionParameterException {
		return null;
	}

	@NotNull
	@Override
	public ConstantValue applyToConstantValues(@NotNull List<ConstantValue> arguments) throws FunctionParameterException {
		return null;
	}

}
