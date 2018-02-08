/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.processor.expression.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ExpressionVerilogGenerator {

	private static final ImmutableMap<ProcessedUnaryOperator, String> unaryOperatorSymbols;
	static {
		ImmutableMap.Builder builder = ImmutableMap.builder();
		builder.put(ProcessedUnaryOperator.NOT, "~");
		builder.put(ProcessedUnaryOperator.PLUS, "");
		builder.put(ProcessedUnaryOperator.MINUS, "-");
		unaryOperatorSymbols = builder.build();
	}

	private static final ImmutableMap<ProcessedBinaryOperator, String> binaryOperatorSymbols;
	static {
		ImmutableMap.Builder builder = ImmutableMap.builder();
		builder.put(ProcessedBinaryOperator.AND, "&");
		builder.put(ProcessedBinaryOperator.OR, "|");
		builder.put(ProcessedBinaryOperator.XOR, "^");
		builder.put(ProcessedBinaryOperator.PLUS, "+");
		builder.put(ProcessedBinaryOperator.MINUS, "-");
		builder.put(ProcessedBinaryOperator.TIMES, "*");
		builder.put(ProcessedBinaryOperator.DIVIDED_BY, "/");
		builder.put(ProcessedBinaryOperator.REMAINDER, "%");
		builder.put(ProcessedBinaryOperator.SHIFT_LEFT, "<<");
		builder.put(ProcessedBinaryOperator.SHIFT_RIGHT, ">>");
		builder.put(ProcessedBinaryOperator.EQUAL, "==");
		builder.put(ProcessedBinaryOperator.NOT_EQUAL, "!=");
		builder.put(ProcessedBinaryOperator.LESS_THAN, "<");
		builder.put(ProcessedBinaryOperator.LESS_THAN_OR_EQUAL, "<=");
		builder.put(ProcessedBinaryOperator.GREATER_THAN, ">");
		builder.put(ProcessedBinaryOperator.GREATER_THAN_OR_EQUAL, ">=");
		binaryOperatorSymbols = builder.build();
	}

	private final Extractor extractor;

	public ExpressionVerilogGenerator(Extractor extractor) {
		this.extractor = extractor;
	}

	public void generate(ProcessedExpression expression, StringBuilder builder) {

		ProcessedDataType dataType = expression.getDataType();
		ProcessedDataType.Family typeFamily = dataType.getFamily();

		if (typeFamily != ProcessedDataType.Family.BIT && typeFamily != ProcessedDataType.Family.VECTOR && typeFamily != ProcessedDataType.Family.MEMORY) {
			throw new ModuleCannotGenerateCodeException("found wrong expression type: " + dataType);
		}

		if (expression instanceof UnknownExpression) {
			throw new ModuleHasErrorsException();
		} else if (expression instanceof ProcessedConstantValue) {
			generateNested(expression, builder);
		} else if (expression instanceof SignalLikeReference) {
			generateNested(expression, builder);
		} else if (expression instanceof InstancePortReference) {
			// TODO
		} else if (expression instanceof ProcessedIndexSelection) {
			generateNested(expression, builder);
		} else if (expression instanceof ProcessedRangeSelection) {
			generateNested(expression, builder);
		} else if (expression instanceof ProcessedUnaryOperation) {

			ProcessedUnaryOperation operation = (ProcessedUnaryOperation)expression;
			String symbol = unaryOperatorSymbols.get(operation.getOperator());
			if (symbol == null) {
				throw new ModuleCannotGenerateCodeException("cannot handle unary operator " + operation.getOperator());
			}
			builder.append(symbol);
			generateNested(operation.getOperand(), builder);

		} else if (expression instanceof ProcessedBinaryOperation) {

			ProcessedBinaryOperation operation = (ProcessedBinaryOperation)expression;
			ProcessedBinaryOperator operator = operation.getOperator();
			if (operator == ProcessedBinaryOperator.VECTOR_CONCAT) {
				builder.append('{');
				generateNested(operation.getLeftOperand(), builder);
				builder.append(", ");
				generateNested(operation.getRightOperand(), builder);
				builder.append('}');
			} else {
				String symbol = binaryOperatorSymbols.get(operator);
				if (symbol == null) {
					throw new ModuleCannotGenerateCodeException("cannot handle binary operator " + operator);
				}
				generateNested(operation.getLeftOperand(), builder);
				builder.append(symbol);
				generateNested(operation.getRightOperand(), builder);
			}

		} else if (expression instanceof ProcessedConditional) {

			ProcessedConditional conditional = (ProcessedConditional)expression;
			generateNested(conditional.getCondition(), builder);
			builder.append(" ? ");
			generateNested(conditional.getThenBranch(), builder);
			builder.append(" : ");
			generateNested(conditional.getElseBranch(), builder);

		} else if (expression instanceof ProcessedFunctionCall) {

			ProcessedFunctionCall call = (ProcessedFunctionCall)expression;
			StandardFunction function = call.getFunction();

			// for now, all functions are for formally constant expressions only
			throw new ModuleCannotGenerateCodeException("function cannot be called for formally non-constant expressions: " + function.getNameInCode());

		} else if (expression instanceof TypeConversion) {
			// TODO
		} else {
			throw new ModuleCannotGenerateCodeException("unknown expression type: " + expression);
		}
	}

	private void generateNested(ProcessedExpression expression, StringBuilder builder) {
		if (expression instanceof UnknownExpression) {
			throw new ModuleHasErrorsException();
		} else if (expression instanceof ProcessedConstantValue) {
			// TODO
		} else if (expression instanceof SignalLikeReference) {
			// TODO
		} else if (expression instanceof InstancePortReference) {
			// TODO
		} else if (expression instanceof ProcessedIndexSelection) {
			// TODO
		} else if (expression instanceof ProcessedRangeSelection) {
			// TODO
		} else if (expression instanceof ProcessedFunctionCall) {
			// TODO
		} else if (expression instanceof TypeConversion) {
			// TODO
		} else {
			extract(expression, builder);
		}
	}

	private void extract(ProcessedExpression expression, StringBuilder builder) {
		builder.append(extractor.extract(expression));
	}

	public interface Extractor {
		String extract(ProcessedExpression expression);
	}

}
