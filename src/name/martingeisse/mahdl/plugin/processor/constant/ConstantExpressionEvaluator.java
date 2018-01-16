/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.constant;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.functions.FunctionParameterException;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public final class ConstantExpressionEvaluator {

	private static final Pattern VECTOR_PATTERN = Pattern.compile("([0-9]+)([bodh])([0-9]+)");

	private static final BigInteger MAX_INDEX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final ErrorHandler errorHandler;
	private final Module module;
	private final Map<String, ConstantValue> definedConstants = new HashMap<>();

	public ConstantExpressionEvaluator(@NotNull ErrorHandler errorHandler, @NotNull Module module) {
		this.errorHandler = errorHandler;
		this.module = module;
	}

	@NotNull
	public Map<String, ConstantValue> getDefinedConstants() {
		return definedConstants;
	}

	/**
	 * Processes the constant definitions from the module and stores their values. Each constant has an initializer
	 * that is evaluated and converted to the data type of the constant. Subsequent constants can use the value of
	 * previous constants to define their data type. Thus, this constant evaluator and the specified data type
	 * processor must work in lockstep to define constants and data types. Sepcifically, for each data type being
	 * processed, the data type processor must take the up-to-date constant values returned by getDefinedConstants()
	 * at the time the data type is being processed into account.
	 */
	public void processConstantDefinitions(@NotNull DataTypeProcessor dataTypeProcessor) {
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				ImplementationItem_SignalLikeDefinitionGroup signalLike = (ImplementationItem_SignalLikeDefinitionGroup) implementationItem;
				if (signalLike.getKind() instanceof SignalLikeKind_Constant) {
					// for constants, the data type must be valid based on the constants defined above
					ProcessedDataType processedDataType = dataTypeProcessor.processDataType(signalLike.getDataType());
					for (SignalLikeDefinition signalLikeDefinition : signalLike.getDefinitions().getAll()) {
						if (signalLikeDefinition instanceof SignalLikeDefinition_WithoutInitializer) {

							SignalLikeDefinition_WithoutInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithoutInitializer) signalLikeDefinition;
							errorHandler.onError(signalLikeDefinition, "constant must have an initializer");
							definedConstants.put(typedDeclaredSignalLike.getIdentifier().getText(), ConstantValue.Unknown.INSTANCE);

						} else if (signalLikeDefinition instanceof SignalLikeDefinition_WithInitializer) {

							SignalLikeDefinition_WithInitializer typedDeclaredSignalLike = (SignalLikeDefinition_WithInitializer) signalLikeDefinition;
							ConstantValue rawValue = evaluate(typedDeclaredSignalLike.getInitializer());
							ConstantValue convertedValue = processedDataType.convertConstantValueImplicitly(rawValue);
							if ((convertedValue instanceof ConstantValue.Unknown) && !(rawValue instanceof ConstantValue.Unknown)) {
								errorHandler.onError(typedDeclaredSignalLike.getInitializer(), "cannot convert value of type " + rawValue.getDataType() + " to type " + processedDataType);
							}
							definedConstants.put(typedDeclaredSignalLike.getIdentifier().getText(), convertedValue);

						} else {

							errorHandler.onError(signalLikeDefinition, "unknown PSI node");

						}
					}
				}
			}
		}

	}

	/**
	 * Returns a ConstantValue.Unknown for non-constant expressions or errors and also reports those to the error reporter.
	 */
	@NotNull
	public final ConstantValue evaluate(@NotNull Expression expression) {
		if (expression instanceof Expression_Literal) {

			return evaluateLiteral((Expression_Literal) expression);

		} else if (expression instanceof Expression_Identifier) {

			String name = ((Expression_Identifier) expression).getIdentifier().getText();
			ConstantValue value = definedConstants.get(name);
			if (value == null) {
				return error(expression, "undefined constant: '" + name + "'");
			}
			return value;

		} else if (expression instanceof Expression_InstancePort) {

			return nonConstant(expression);

		} else if (expression instanceof Expression_IndexSelection) {

			return evaluateIndexSelection((Expression_IndexSelection) expression);

		} else if (expression instanceof Expression_RangeSelection) {

			return evaluateRangeSelection((Expression_RangeSelection) expression);

		} else if (expression instanceof UnaryOperation) {

			return evaluateUnaryOperation((UnaryOperation) expression);

		} else if (expression instanceof BinaryOperation) {

			return evaluateBinaryOperation((BinaryOperation) expression);

		} else if (expression instanceof Expression_Mux) {

			return evaluateMux((Expression_Mux) expression);

		} else if (expression instanceof Expression_FunctionCall) {

			return evaluateFunctionCall((Expression_FunctionCall) expression);

		} else if (expression instanceof Expression_Parenthesized) {

			return evaluate(((Expression_Parenthesized) expression).getExpression());

		} else {

			return nonConstant(expression);

		}
	}

	@NotNull
	private ConstantValue error(@NotNull PsiElement element, @NotNull String message) {
		errorHandler.onError(element, message);
		return ConstantValue.Unknown.INSTANCE;
	}

	@NotNull
	private ConstantValue nonConstant(@NotNull PsiElement element) {
		return error(element, "expression must be constant");
	}

	@NotNull
	private ConstantValue evaluateLiteral(@NotNull Expression_Literal literalExpression) {
		Literal literal = literalExpression.getLiteral();
		if (literal instanceof Literal_Vector) {
			return parseVector(((Literal_Vector) literal).getValue());
		} else if (literal instanceof Literal_Integer) {
			String text = ((Literal_Integer) literal).getValue().getText();
			return new ConstantValue.Integer(new BigInteger(text));
		} else if (literal instanceof Literal_Text) {
			return parseText(((Literal_Text) literal).getValue());
		} else {
			return nonConstant(literal);
		}
	}

	@NotNull
	private ConstantValue parseVector(@NotNull LeafPsiElement textElement) {
		String text = textElement.getText();
		Matcher matcher = VECTOR_PATTERN.matcher(text);
		if (!matcher.matches()) {
			return error(textElement, "malformed vector");
		}
		int size = Integer.parseInt(matcher.group(1));
		char radixCode = matcher.group(2).charAt(0);
		String digits = matcher.group(3);

		int radix = radixCode == 'b' ? 2 : radixCode == 'o' ? 8 : radixCode == 'd' ? 10 : radixCode == 'h' ? 16 : 0;
		if (radix == 0) {
			return error(textElement, "unknown radix '" + radixCode);
		}
		final BigInteger integerValue = new BigInteger(digits, radix);
		if (integerValue.bitLength() > size) {
			return error(textElement, "vector literal contains a value larger than its sepcified size");
		}
		return new ConstantValue.Vector(size, IntegerBitUtil.convertToBitSet(integerValue, size));
	}

	@NotNull
	private ConstantValue parseText(@NotNull LeafPsiElement textElement) {
		String rawText = textElement.getText();
		if (rawText.charAt(0) != '"' || rawText.charAt(rawText.length() - 1) != '"') {
			return error(textElement, "missing quotation marks");
		}
		StringBuilder builder = new StringBuilder();
		boolean escape = false;
		for (int i = 1; i < rawText.length() - 1; i++) {
			char c = rawText.charAt(i);
			if (escape) {
				// escapes are not supported (yet), and it's not clear whether we need them
				return error(textElement, "text escape sequences are not supported yet");
			} else if (c == '\\') {
				escape = true;
			} else {
				builder.append(c);
			}
		}
		if (escape) {
			return error(textElement, "unterminated escape sequence");
		}
		return new ConstantValue.Text(builder.toString());
	}

	@NotNull
	private ConstantValue evaluateIndexSelection(@NotNull Expression_IndexSelection indexSelection) {
		ConstantValue containerValue = evaluate(indexSelection.getContainer());
		int containerSize;
		if (containerValue instanceof ConstantValue.Unknown) {
			containerSize = -1;
		} else if (containerValue instanceof ConstantValue.Vector) {
			containerSize = ((ConstantValue.Vector) containerValue).getSize();
		} else if (containerValue instanceof ConstantValue.Memory) {
			containerSize = ((ConstantValue.Memory) containerValue).getFirstSize();
		} else {
			error(indexSelection, "cannot index-select from an expression of type " + containerValue.getDataTypeFamily().getDisplayString());
			containerSize = -1;
		}
		int intIndexValue = handleIndexValue(containerSize, containerValue.getDataType().toString(), indexSelection.getIndex());
		if (containerSize < 0 || intIndexValue < 0) {
			return ConstantValue.Unknown.INSTANCE;
		} else {
			// all error cases should be handled above and should have reported an error already, so we don't have to do that here
			return containerValue.selectIndex(intIndexValue);
		}
	}

	@NotNull
	private ConstantValue evaluateRangeSelection(@NotNull Expression_RangeSelection rangeSelection) {
		ConstantValue containerValue = evaluate(rangeSelection.getContainer());
		int containerSize;
		if (containerValue instanceof ConstantValue.Unknown) {
			containerSize = -1;
		} else if (containerValue instanceof ConstantValue.Vector) {
			containerSize = ((ConstantValue.Vector) containerValue).getSize();
		} else {
			error(rangeSelection, "cannot range-select from an expression of type " + containerValue.getDataTypeFamily().toString());
			containerSize = -1;
		}
		String containerTypeText = containerValue.getDataType().toString();
		int intFromIndexValue = handleIndexValue(containerSize, containerTypeText, rangeSelection.getFrom());
		int intToIndexValue = handleIndexValue(containerSize, containerTypeText, rangeSelection.getTo());
		if (containerSize < 0 || intFromIndexValue < 0 || intToIndexValue < 0) {
			return ConstantValue.Unknown.INSTANCE;
		} else {
			// all error cases should be handled above and should have reported an error already, so we don't have to do that here
			return containerValue.selectRange(intFromIndexValue, intToIndexValue);
		}
	}

	private int handleIndexValue(int containerSize, @NotNull String containerType, @NotNull Expression indexExpression) {
		ConstantValue indexValue = evaluate(indexExpression);
		BigInteger numericIndexValue = indexValue.convertToInteger();
		if (numericIndexValue == null) {
			error(indexExpression, "value of type  " + indexValue.getDataTypeFamily().getDisplayString() + " cannot be converted to integer");
			return -1;
		}
		if (numericIndexValue.compareTo(BigInteger.ZERO) < 0) {
			error(indexExpression, "index is negative: " + numericIndexValue);
			return -1;
		}
		if (numericIndexValue.compareTo(MAX_INDEX_VALUE) > 0) {
			error(indexExpression, "index too large: " + numericIndexValue);
			return -1;
		}
		if (containerSize < 0) {
			// could not determine container type -- stop here
			return -1;
		}
		int intValue = numericIndexValue.intValue();
		if (intValue >= containerSize) {
			error(indexExpression, "index " + numericIndexValue + " is out of bounds for type " + containerType);
			return -1;
		}
		return intValue;
	}

	private ConstantValue evaluateUnaryOperation(UnaryOperation expression) {
		if (expression.getOperand() == null) {
			return ConstantValue.Unknown.INSTANCE;
		}
		ConstantValue operandValue = evaluate(expression.getOperand());
		if (operandValue instanceof ConstantValue.Unknown) {
			return operandValue;
		}
		if (expression instanceof Expression_UnaryNot) {
			if (operandValue instanceof ConstantValue.Bit) {
				boolean bitOperandValue = ((ConstantValue.Bit) operandValue).isSet();
				return new ConstantValue.Bit(!bitOperandValue);
			} else if (operandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vector = (ConstantValue.Vector) operandValue;
				BitSet invertedBits = (BitSet) vector.getBits().clone();
				invertedBits.flip(0, vector.getSize() - 1);
				return new ConstantValue.Vector(vector.getSize(), invertedBits);
			} else if (operandValue instanceof ConstantValue.Integer) {
				// if -n == ~n + 1, then ~n == -n - 1
				BigInteger integerOperandValue = ((ConstantValue.Integer) operandValue).getValue();
				BigInteger invertedIntegerValue = integerOperandValue.negate().subtract(BigInteger.ONE);
				return new ConstantValue.Integer(invertedIntegerValue);
			} else {
				return error(expression, "cannot bitwise-negate a value of type " + operandValue.getDataType());
			}
		} else if (expression instanceof Expression_UnaryPlus) {
			if (operandValue instanceof ConstantValue.Vector || operandValue instanceof ConstantValue.Integer) {
				return operandValue;
			} else {
				return error(expression, "cannot apply a unary plus to a value of type " + operandValue.getDataType());
			}
		} else if (expression instanceof Expression_UnaryMinus) {
			if (operandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vector = (ConstantValue.Vector) operandValue;
				int size = vector.getSize();
				BigInteger integerValue = IntegerBitUtil.convertToInteger(vector.getBits(), size);
				return new ConstantValue.Vector(size, IntegerBitUtil.convertToBitSet(integerValue.negate(), size));
			} else if (operandValue instanceof ConstantValue.Integer) {
				BigInteger integerOperandValue = ((ConstantValue.Integer) operandValue).getValue();
				return new ConstantValue.Integer(integerOperandValue.negate());
			} else {
				return error(expression, "cannot apply a unary minus to a value of type " + operandValue.getDataType());
			}
		} else {
			return error(expression, "unknown unary operator");
		}
	}

	private ConstantValue evaluateBinaryOperation(BinaryOperation expression) {

		// determine operand values
		if (expression.getLeftOperand() == null || expression.getRightOperand() == null) {
			return ConstantValue.Unknown.INSTANCE;
		}
		ConstantValue leftOperandValue = evaluate(expression.getLeftOperand());
		ConstantValue rightOperandValue = evaluate(expression.getRightOperand());
		if (leftOperandValue instanceof ConstantValue.Unknown) {
			return leftOperandValue;
		}
		if (rightOperandValue instanceof ConstantValue.Unknown) {
			return rightOperandValue;
		}

		// Concatenation can handle various types. All other operators can only handle vectors and integers.
		if (expression instanceof Expression_BinaryConcat) {
			return evaluateConcatenation((Expression_BinaryConcat) expression, leftOperandValue, rightOperandValue);
		}
		if (!(leftOperandValue instanceof ConstantValue.Vector) && !(leftOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, leftOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as left operand here");
		}
		if (!(rightOperandValue instanceof ConstantValue.Vector) && !(rightOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, rightOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as right operand here");
		}

		// perform the corresponding integer operation
		BigInteger leftInteger = leftOperandValue.convertToInteger();
		BigInteger rightInteger = rightOperandValue.convertToInteger();
		ConstantValue integerResultValue;
		BigInteger resultInteger;
		try {
			integerResultValue = BinaryOperatorUtil.evaluateIntegerVectorOperator(expression, leftInteger, rightInteger);
			resultInteger = integerResultValue.convertToInteger();
			if (resultInteger == null) {
				return error(expression, "got result value of wrong type for shift operator: " + integerResultValue.getDataTypeFamily());
			}
		} catch (BinaryOperatorUtil.OperatorException e) {
			return error(expression, e.getMessage());
		}

		// shifting can handle vectors of different size
		if (expression instanceof Expression_BinaryShiftLeft || expression instanceof Expression_BinaryShiftRight) {
			if (leftOperandValue instanceof ConstantValue.Vector) {
				int size = ((ConstantValue.Vector) leftOperandValue).getSize();
				return new ConstantValue.Vector(size, IntegerBitUtil.convertToBitSet(resultInteger, size));
			} else {
				return integerResultValue;
			}
		}

		// all other operators want the operands to be of the same size
		int resultSize;
		if (leftOperandValue instanceof ConstantValue.Vector) {
			resultSize = ((ConstantValue.Vector) leftOperandValue).getSize();
			if (rightOperandValue instanceof ConstantValue.Vector) {
				int rightSize = ((ConstantValue.Vector) rightOperandValue).getSize();
				if (rightSize != resultSize) {
					return error(expression, "vectors of different sizes (" + resultSize + " and " + rightSize + ") cannot be used with this operator");
				}
			}
		} else {
			if (rightOperandValue instanceof ConstantValue.Vector) {
				resultSize = ((ConstantValue.Vector) rightOperandValue).getSize();
			} else {
				return integerResultValue;
			}
		}
		return new ConstantValue.Vector(resultSize, IntegerBitUtil.convertToBitSet(resultInteger, resultSize));
	}

	private ConstantValue evaluateConcatenation(Expression_BinaryConcat expression, ConstantValue leftOperandValue, ConstantValue rightOperandValue) {

		// string concatenation
		if (leftOperandValue instanceof ConstantValue.Text || rightOperandValue instanceof ConstantValue.Text) {
			return new ConstantValue.Text(leftOperandValue.convertToString() + rightOperandValue.convertToString());
		}

		// bit / vector concatenation
		if (leftOperandValue instanceof ConstantValue.Bit) {
			ConstantValue.Bit leftBit = (ConstantValue.Bit) leftOperandValue;

			if (rightOperandValue instanceof ConstantValue.Bit) {
				ConstantValue.Bit rightBit = (ConstantValue.Bit) rightOperandValue;

				BitSet bits = new BitSet();
				bits.set(1, leftBit.isSet());
				bits.set(0, rightBit.isSet());
				return new ConstantValue.Vector(2, bits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet bits = (BitSet) rightVector.getBits().clone();
				bits.set(rightVector.getSize(), leftBit.isSet());
				return new ConstantValue.Vector(rightVector.getSize() + 1, bits);

			}

		} else if (leftOperandValue instanceof ConstantValue.Vector) {
			ConstantValue.Vector leftVector = (ConstantValue.Vector) leftOperandValue;

			if (rightOperandValue instanceof ConstantValue.Bit) {
				ConstantValue.Bit rightBit = (ConstantValue.Bit) rightOperandValue;

				BitSet bits = new BitSet();
				if (rightBit.isSet()) {
					bits.set(0);
				}
				for (int i = 0; i < leftVector.getSize(); i++) {
					bits.set(1 + i, leftVector.getBits().get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + 1, bits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet bits = (BitSet) rightVector.getBits().clone();
				for (int i = 0; i < leftVector.getSize(); i++) {
					bits.set(rightVector.getSize() + i, leftVector.getBits().get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + rightVector.getSize(), bits);

			}

		}

		return error(expression, "concatenation operator cannot be used on typed " +
			leftOperandValue.getDataTypeFamily().getDisplayString() + " and " +
			rightOperandValue.getDataTypeFamily().getDisplayString());
	}

	@NotNull
	private ConstantValue evaluateMux(@NotNull Expression_Mux mux) {
		// I'm not sure yet whether to allow type errors in the not-taken branch. The use-cases
		// (soft-commenting-out; generating only constants during code generation; ...) aren't clear to me.
		// For now, let's be strict about it so no ugly code creeps in.
		ConstantValue conditionValue = evaluate(mux.getCondition());
		ConstantValue thenValue = evaluate(mux.getThenBranch());
		ConstantValue elseValue = evaluate(mux.getElseBranch());
		if (conditionValue instanceof ConstantValue.Unknown) {
			// if the condition has errors, don't also complain that it's not a boolean
			return conditionValue;
		}
		Boolean booleanCondition = conditionValue.convertToBoolean();
		if (booleanCondition == null) {
			return error(mux.getCondition(), "cannot use value of type " + conditionValue.getDataTypeFamily().getDisplayString() + " as selector");
		}
		return booleanCondition ? thenValue : elseValue;
	}

	@NotNull
	private ConstantValue evaluateFunctionCall(@NotNull Expression_FunctionCall functionCall) {
		String functionName = functionCall.getFunctionName().getText();
		StandardFunction standardFunction = StandardFunction.getFromNameInCode(functionName);
		boolean error = false;
		if (standardFunction == null) {
			error(functionCall.getFunctionName(), "unknown function");
			error = true;
		}
		List<ConstantValue> arguments = new ArrayList<>();
		for (Expression argumentExpression : functionCall.getArguments().getAll()) {
			ConstantValue argument = evaluate(argumentExpression);
			arguments.add(argument);
			if (argument instanceof ConstantValue.Unknown) {
				error = true;
			}
		}
		if (error) {
			return ConstantValue.Unknown.INSTANCE;
		}
		try {
			return standardFunction.applyToConstantValues(arguments.toArray(new ConstantValue[arguments.size()]));
		} catch (FunctionParameterException e) {
			return error(functionCall, e.getMessage());
		}
	}

}
