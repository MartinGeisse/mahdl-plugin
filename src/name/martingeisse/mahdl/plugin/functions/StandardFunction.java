/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public enum StandardFunction {

	ASCII("ascii") {
		@Override
		@NotNull
		public ConstantValue applyToConstantValues(ConstantValue... values) throws FunctionParameterException {
			throw new UnsupportedOperationException("not yet implemented");
		}
	},

	ASCIIZ("asciiz") {
		@Override
		@NotNull
		public ConstantValue applyToConstantValues(ConstantValue... values) throws FunctionParameterException {
			throw new UnsupportedOperationException("not yet implemented");
		}
	},

	LOAD_BINARY_FILE("loadBinaryFile") {
		@Override
		@NotNull
		public ConstantValue applyToConstantValues(ConstantValue... values) throws FunctionParameterException {
			throw new UnsupportedOperationException("not yet implemented");
		}
	},

	;

	private final String nameInCode;

	StandardFunction(String nameInCode) {
		this.nameInCode = nameInCode;
	}

	public String getNameInCode() {
		return nameInCode;
	}

	@NotNull
	public abstract ConstantValue applyToConstantValues(ConstantValue... values) throws FunctionParameterException;

	public static StandardFunction getFromNameInCode(String nameInCode) {
		for (StandardFunction standardFunction : values()) {
			if (standardFunction.nameInCode.equals(nameInCode)) {
				return standardFunction;
			}
		}
		return null;
	}

}
