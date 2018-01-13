/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.type;

import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;

import java.math.BigInteger;

/**
 * Implementation class for DataTypeProcessor.
 */
public final class DataTypeProcessorImpl implements DataTypeProcessor {

	private static final BigInteger MAX_SIZE_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final ErrorHandler errorHandler;
	private final ConstantExpressionEvaluator constantExpressionEvaluator;

	public DataTypeProcessorImpl(ErrorHandler errorHandler, ConstantExpressionEvaluator constantExpressionEvaluator) {
		this.errorHandler = errorHandler;
		this.constantExpressionEvaluator = constantExpressionEvaluator;
	}

	public ProcessedDataType processDataType(DataType dataType) {
		if (dataType instanceof DataType_Bit) {
			return ProcessedDataType.Bit.INSTANCE;
		} else if (dataType instanceof DataType_Vector) {
			DataType_Vector vector = (DataType_Vector) dataType;
			int size = processConstantSizeExpression(vector.getSize());
			return size < 0 ? ProcessedDataType.Unknown.INSTANCE : new ProcessedDataType.Vector(size);
		} else if (dataType instanceof DataType_Memory) {
			DataType_Memory memory = (DataType_Memory) dataType;
			int firstSize = processConstantSizeExpression(memory.getFirstSize());
			int secondSize = processConstantSizeExpression(memory.getSecondSize());
			return (firstSize < 0 || secondSize < 0) ? ProcessedDataType.Unknown.INSTANCE : new ProcessedDataType.Memory(firstSize, secondSize);
		} else if (dataType instanceof DataType_Integer) {
			return ProcessedDataType.Integer.INSTANCE;
		} else if (dataType instanceof DataType_Text) {
			return ProcessedDataType.Text.INSTANCE;
		} else {
			errorHandler.onError(dataType, "unknown data type");
			return ProcessedDataType.Unknown.INSTANCE;
		}
	}

	private int processConstantSizeExpression(Expression expression) {
		ConstantValue value = constantExpressionEvaluator.evaluate(expression);
		if (value.getDataTypeFamily() == ProcessedDataType.Family.UNKNOWN) {
			return -1;
		}
		BigInteger integerValue = value.convertToInteger();
		if (integerValue == null) {
			errorHandler.onError(expression, "cannot convert " + value + " to integer");
			return -1;
		}
		if (integerValue.compareTo(MAX_SIZE_VALUE) > 0) {
			errorHandler.onError(expression, "size too large: " + integerValue);
			return -1;
		}
		int intValue = integerValue.intValue();
		if (intValue < 0) {
			errorHandler.onError(expression, "size cannot be negative: " + integerValue);
			return -1;
		}
		return intValue;
	}


}
