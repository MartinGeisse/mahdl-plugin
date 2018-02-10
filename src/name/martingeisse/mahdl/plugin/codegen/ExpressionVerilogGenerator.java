/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ExpressionVerilogGenerator {

	/*
		Important note about nesting: The reason we do this is NOT because of syntactic restrictions (that's part of
		the reason in some edge cases though). But the most important reason is this: We want to coerce the result of
		operations such as addition to the result vector size. So whenever addition is used as a sub-expression of some
		larger expression, we introduce a helper signal of that size to coerce the result to that size. If we don't do
		that, the carry bit tends to appear in places where we don't expect it, and which violates MaHDL semantics.
		There is no useful information on when and how this happens and how to prevent it without helper signals --
		it's part of the huge mess that is Verilog semantics.

		So don't try to tweak nesting just because you wrongly think it is about syntax.
	 */
	public static final int NESTING_TOPLEVEL = 0;
	public static final int NESTING_INSIDE_SWITCH = 1; // TODO switch expressions cannot be generated as such anyway,
		// so this nesting level is useless. Handle switch expressions, then remove this level (use toplevel instead)
	public static final int NESTING_INSIDE_OPERATION = 2;
	public static final int NESTING_INSIDE_SELECTION = 3;
	public static final int NESTING_IDENTIFIER_ONLY = 4;

	private static final ImmutableMap<ProcessedUnaryOperator, String> UNARY_OPERATOR_SYMBOLS;

	static {
		ImmutableMap.Builder builder = ImmutableMap.builder();
		builder.put(ProcessedUnaryOperator.NOT, "~");
		builder.put(ProcessedUnaryOperator.PLUS, "");
		builder.put(ProcessedUnaryOperator.MINUS, "-");
		UNARY_OPERATOR_SYMBOLS = builder.build();
	}

	private static final ImmutableMap<ProcessedBinaryOperator, String> BINARY_OPERATOR_SYMBOLS;

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
		BINARY_OPERATOR_SYMBOLS = builder.build();
	}

	private static final ImmutableMap<Class<? extends ProcessedExpression>, Integer> EXTRACTION_NEEDED_NESTING_LEVELS;

	static {
		ImmutableMap.Builder builder = ImmutableMap.builder();

		// switch...

		// operations
		builder.put(ProcessedUnaryOperation.class, NESTING_INSIDE_OPERATION);
		builder.put(ProcessedBinaryOperation.class, NESTING_INSIDE_OPERATION);
		builder.put(ProcessedConditional.class, NESTING_INSIDE_OPERATION);
		builder.put(ProcessedFunctionCall.class, NESTING_INSIDE_OPERATION);

		// selection
		builder.put(ProcessedIndexSelection.class, NESTING_INSIDE_SELECTION);
		builder.put(ProcessedRangeSelection.class, NESTING_INSIDE_SELECTION);

		// primary
		builder.put(InstancePortReference.class, NESTING_IDENTIFIER_ONLY);

		// TODO TypeConversion.BitToVector

		EXTRACTION_NEEDED_NESTING_LEVELS = builder.build();
	}

	private final Extractor extractor;

	public ExpressionVerilogGenerator(Extractor extractor) {
		this.extractor = extractor;
	}

	/**
	 * Returns the literal for an expression that must be a formally constant integer.
	 */
	public String convertIntegerConstantToString(ProcessedExpression expression) {
		ConstantValue value = fold(expression);
		if (value instanceof ConstantValue.Integer) {
			return ((ConstantValue.Integer) value).getValue().toString();
		} else {
			throw new ModuleCannotGenerateCodeException("expected integer constant, found: " + value);
		}
	}

	/**
	 * Generates the code for the specified expression to the builder, writing helper signals to the output as needed.
	 */
	public void generate(ProcessedExpression expression, StringBuilder builder) {
		generate(expression, builder, NESTING_TOPLEVEL);
	}

	/**
	 * Generates the code for the specified expression to the builder, writing helper signals to the output as needed.
	 * Any expressions that conflict with the specified current nesting level will be extracted.
	 */
	public void generate(ProcessedExpression expression, StringBuilder builder, int nesting) {

		// constant folding
		ConstantValue value = fold(expression);
		if (!(value instanceof ConstantValue.Unknown)) {
			if (value instanceof ConstantValue.Bit) {
				boolean set = ((ConstantValue.Bit) value).isSet();
				builder.append(set ? "1" : "0");
			} else if (value instanceof ConstantValue.Vector) {
				ConstantValue.Vector vector = (ConstantValue.Vector)value;
				builder.append(vector.getSize()).append("'h").append(vector.getHexLiteral());
			} else if (value instanceof ConstantValue.Memory) {
				extract(expression, builder);
			} else if (value instanceof ConstantValue.Integer) {
				builder.append(((ConstantValue.Integer) value).getValue());
			} else {
				throw new ModuleCannotGenerateCodeException("invalid run-time constant: " + value);
			}
			return;
		}

		// check for allowed expression nesting and extract if violated (evades strange behavior imposed by Verilog)
		if (nesting != NESTING_TOPLEVEL) {
			Integer extractionNeededNestingLevel = EXTRACTION_NEEDED_NESTING_LEVELS.get(expression.getClass());
			if (extractionNeededNestingLevel == null || nesting >= extractionNeededNestingLevel) {
				extract(expression, builder);
				return;
			}
		}

		// handle normal cases
		if (expression instanceof UnknownExpression) {
			throw new ModuleHasErrorsException();
		} else if (expression instanceof ProcessedConstantValue) {

			throw new ModuleCannotGenerateCodeException("constant did not react to folding");

		} else if (expression instanceof SignalLikeReference) {

			builder.append(((SignalLikeReference) expression).getDefinition().getName());

		} else if (expression instanceof InstancePortReference) {

			InstancePortReference instancePortReference = (InstancePortReference)expression;
			builder.append(instancePortReference.getModuleInstance().getName());
			builder.append('.').append(instancePortReference.getPortName());

		} else if (expression instanceof ProcessedIndexSelection) {

			ProcessedIndexSelection selection = (ProcessedIndexSelection)expression;
			generate(selection.getContainer(), builder, NESTING_INSIDE_SELECTION);
			builder.append('[');
			generate(selection.getIndex(), builder, NESTING_INSIDE_SELECTION);
			builder.append(']');

		} else if (expression instanceof ProcessedRangeSelection) {

			ProcessedRangeSelection selection = (ProcessedRangeSelection)expression;
			generate(selection.getContainer(), builder, NESTING_INSIDE_SELECTION);
			builder.append('[').append(selection.getFromIndex()).append(':').append(selection.getToIndex()).append(']');

		} else if (expression instanceof ProcessedUnaryOperation) {

			ProcessedUnaryOperation operation = (ProcessedUnaryOperation) expression;
			String symbol = UNARY_OPERATOR_SYMBOLS.get(operation.getOperator());
			if (symbol == null) {
				throw new ModuleCannotGenerateCodeException("cannot handle unary operator " + operation.getOperator());
			}
			builder.append(symbol);
			generate(operation.getOperand(), builder, NESTING_INSIDE_OPERATION);

		} else if (expression instanceof ProcessedBinaryOperation) {

			ProcessedBinaryOperation operation = (ProcessedBinaryOperation) expression;
			ProcessedBinaryOperator operator = operation.getOperator();
			if (operator == ProcessedBinaryOperator.VECTOR_CONCAT) {
				builder.append('{');
				generate(operation.getLeftOperand(), builder, NESTING_INSIDE_OPERATION);
				builder.append(", ");
				generate(operation.getRightOperand(), builder, NESTING_INSIDE_OPERATION);
				builder.append('}');
			} else {
				String symbol = BINARY_OPERATOR_SYMBOLS.get(operator);
				if (symbol == null) {
					throw new ModuleCannotGenerateCodeException("cannot handle binary operator " + operator);
				}
				generate(operation.getLeftOperand(), builder, NESTING_INSIDE_OPERATION);
				builder.append(symbol);
				generate(operation.getRightOperand(), builder, NESTING_INSIDE_OPERATION);
			}

		} else if (expression instanceof ProcessedConditional) {

			ProcessedConditional conditional = (ProcessedConditional) expression;
			generate(conditional.getCondition(), builder, NESTING_INSIDE_OPERATION);
			builder.append(" ? ");
			generate(conditional.getThenBranch(), builder, NESTING_INSIDE_OPERATION);
			builder.append(" : ");
			generate(conditional.getElseBranch(), builder, NESTING_INSIDE_OPERATION);

		} else if (expression instanceof ProcessedFunctionCall) {

			ProcessedFunctionCall call = (ProcessedFunctionCall) expression;
			StandardFunction function = call.getFunction();

			// for now, all functions are for formally constant expressions only
			throw new ModuleCannotGenerateCodeException("function cannot be called for formally non-constant expressions: " + function.getNameInCode());

		} else if (expression instanceof TypeConversion) {

			if (expression instanceof TypeConversion.BitToVector) {
				// TODO is this implicit in all cases in Verilog? What about nested expressions? If in doubt,
				// extract this expression!
			} else {
				throw new ModuleCannotGenerateCodeException("invalid run-time type conversion: " + expression);
			}

		} else {
			throw new ModuleCannotGenerateCodeException("unknown expression type: " + expression);
		}
	}

	//
	// constant folding
	//

	private ConstantValue fold(ProcessedExpression expression) {
		ErrorHandler errorHandler = (errorSource, message) -> {
			throw new ModuleHasErrorsException(message);
		};
		ProcessedExpression.FormallyConstantEvaluationContext context = new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler) {

			@Override
			public ConstantValue.Unknown notConstant(PsiElement errorSource) {
				throw new NotConstantException();
			}

			@Override
			public ConstantValue.Unknown notConstant(ProcessedExpression errorSource) {
				throw new NotConstantException();

			}
		};
		try {
			return expression.evaluateFormallyConstant(context);
		} catch (NotConstantException e) {
			return ConstantValue.Unknown.INSTANCE;
		}
	}

	private static class NotConstantException extends RuntimeException {
	}

	//
	// extraction
	//

	private void extract(ProcessedExpression expression, StringBuilder builder) {
		if (expression instanceof SignalLikeReference) {
			builder.append(((SignalLikeReference) expression).getDefinition().getName());
		} else {
			builder.append(extractor.extract(expression));
		}
	}

	public interface Extractor {
		String extract(ProcessedExpression expression);
	}

}
