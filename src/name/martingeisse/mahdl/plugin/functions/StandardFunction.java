/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.TypeErrorException;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *
 */
public enum StandardFunction {

	ASCII("ascii") {

		@Override
		public ProcessedDataType checkType(List<ProcessedExpression> arguments) throws TypeErrorException {
			if (arguments.size() != 1 || arguments.get(0).getDataType().getFamily() != ProcessedDataType.Family.TEXT) {
				throw new TypeErrorException();
			}
			// TODO we need constant evaluation here!
			throw new UnsupportedOperationException("not yet implemented");
		}

		@Override
		@NotNull
		public ConstantValue applyToConstantValues(@NotNull ConstantValue... values) throws FunctionParameterException {
			// TODO we need constant evaluation here!
			throw new UnsupportedOperationException("not yet implemented");
		}
	},

	ASCIIZ("asciiz") {

		@Override
		public ProcessedDataType checkType(List<ProcessedExpression> arguments) throws TypeErrorException {
			if (arguments.size() != 1 || arguments.get(0).getDataType().getFamily() != ProcessedDataType.Family.TEXT) {
				throw new TypeErrorException();
			}
			// TODO we need constant evaluation here!
			throw new UnsupportedOperationException("not yet implemented");
		}

		@Override
		@NotNull
		public ConstantValue applyToConstantValues(@NotNull ConstantValue... values) throws FunctionParameterException {
			throw new UnsupportedOperationException("not yet implemented");
		}
	},

	LOAD_BINARY_FILE("loadBinaryFile") {

		@Override
		public ProcessedDataType checkType(List<ProcessedExpression> arguments) throws TypeErrorException {
			// TODO this function needs first/second size arguments -- or we need something like "auto-convertable types",
			// those that can insert a type conversion by themselves automatically (but how should that work?)
			throw new UnsupportedOperationException("not yet implemented");
		}

		@Override
		@NotNull
		public ConstantValue applyToConstantValues(@NotNull ConstantValue... values) throws FunctionParameterException {
			throw new UnsupportedOperationException("not yet implemented");
		}

	},

	;

	private final String nameInCode;

	StandardFunction(@NotNull String nameInCode) {
		this.nameInCode = nameInCode;
	}

	@NotNull
	public String getNameInCode() {
		return nameInCode;
	}

	public abstract ProcessedDataType checkType(List<ProcessedExpression> arguments) throws TypeErrorException;

	@NotNull
	public abstract ConstantValue applyToConstantValues(@NotNull ConstantValue... values) throws FunctionParameterException;

	@Nullable
	public static StandardFunction getFromNameInCode(@NotNull String nameInCode) {
		for (StandardFunction standardFunction : values()) {
			if (standardFunction.nameInCode.equals(nameInCode)) {
				return standardFunction;
			}
		}
		return null;
	}

}
