/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.Expression_UnaryMinus;
import name.martingeisse.mahdl.plugin.input.psi.Expression_UnaryNot;
import name.martingeisse.mahdl.plugin.input.psi.Expression_UnaryPlus;
import name.martingeisse.mahdl.plugin.input.psi.UnaryOperation;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public enum ProcessedUnaryOperator {

	NOT(ProcessedDataType.Family.BIT, ProcessedDataType.Family.VECTOR, ProcessedDataType.Family.INTEGER),
	PLUS(ProcessedDataType.Family.VECTOR, ProcessedDataType.Family.INTEGER),
	MINUS(ProcessedDataType.Family.VECTOR, ProcessedDataType.Family.INTEGER);

	private final ProcessedDataType.Family[] acceptedOperandFamilies;

	ProcessedUnaryOperator(ProcessedDataType.Family... acceptedOperandFamilies) {
		this.acceptedOperandFamilies = acceptedOperandFamilies;
	}

	public static ProcessedUnaryOperator from(UnaryOperation operation) {
		if (operation instanceof Expression_UnaryNot) {
			return NOT;
		} else if (operation instanceof Expression_UnaryPlus) {
			return PLUS;
		} else if (operation instanceof Expression_UnaryMinus) {
			return MINUS;
		} else {
			throw new IllegalArgumentException("unknown unary operation: " + operation);
		}
	}

	@NotNull
	public ProcessedDataType checkType(@NotNull ProcessedDataType operandType) throws TypeErrorException {
		if (operandType instanceof ProcessedDataType.Unknown) {
			return operandType;
		}
		if (!ArrayUtils.contains(acceptedOperandFamilies, operandType)) {
			throw new TypeErrorException();
		}
		return operandType;
	}

}
