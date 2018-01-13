/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.constant;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
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

	public ConstantExpressionEvaluator(ErrorHandler errorHandler, Module module) {
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
				ImplementationItem_SignalLikeDefinitionGroup signalLike = (ImplementationItem_SignalLikeDefinitionGroup)implementationItem;
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
							ConstantValue value = evaluate(typedDeclaredSignalLike.getInitializer());
							definedConstants.put(typedDeclaredSignalLike.getIdentifier().getText(), processedDataType.convertConstantValueImplicitly(value));

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

			Literal literal = ((Expression_Literal) expression).getLiteral();
			if (literal instanceof Literal_Vector) {
				return parseVector(((Literal_Vector) literal).getValue());
			} else if (literal instanceof Literal_Integer) {
				String text = ((Literal_Integer) literal).getValue().getText();
				return new ConstantValue.Integer(new BigInteger(text));
			} else if (literal instanceof Literal_Text) {
				return parseText(((Literal_Text) literal).getValue());
			} else {
				return nonConstant(expression);
			}

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

			Expression_IndexSelection indexSelection = (Expression_IndexSelection) expression;
			ConstantValue containerValue = evaluate(indexSelection.getContainer());
			int containerSize;
			String containerTypeText;
			if (containerValue instanceof ConstantValue.Unknown) {
				containerSize = -1;
				containerTypeText = "(unknown)";
			} else if (containerValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vectorValue = (ConstantValue.Vector) containerValue;
				containerSize = vectorValue.getSize();
				containerTypeText = vectorValue.getDataType().toString();
			} else if (containerValue instanceof ConstantValue.Memory) {
				ConstantValue.Memory memoryType = (ConstantValue.Memory) containerValue;
				containerSize = memoryType.getFirstSize();
				containerTypeText = memoryType.getDataType().toString();
			} else {
				return error(expression, "cannot index-select from an expression of type " +
					containerValue.getDataTypeFamily().getDisplayString());
			}
			int intIndexValue = handleIndexValue(containerSize, containerTypeText, indexSelection.getIndex());
			return containerValue.selectIndex(intIndexValue); // TODO this can return null!

		} else if (expression instanceof Expression_RangeSelection) {

			Expression_RangeSelection rangeSelection = (Expression_RangeSelection) expression;
			ConstantValue containerValue = evaluate(rangeSelection.getContainer());
			int containerSize;
			String containerTypeText;
			if (containerValue instanceof ConstantValue.Unknown) {
				containerSize = -1;
				containerTypeText = "(unknown)";
			} else if (containerValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vectorValue = (ConstantValue.Vector) containerValue;
				containerSize = vectorValue.getSize();
				containerTypeText = vectorValue.getDataType().toString();
			} else {
				return error(expression, "cannot range-select from an expression of type " +
					containerValue.getDataTypeFamily().toString());
			}
			int intFromIndexValue = handleIndexValue(containerSize, containerTypeText, rangeSelection.getFrom());
			int intToIndexValue = handleIndexValue(containerSize, containerTypeText, rangeSelection.getTo());
			return containerValue.selectRange(intFromIndexValue, intToIndexValue); // TODO this can return null!

		} else if (expression instanceof UnaryOperation) {

			// TODO
			return error(expression, "not yet implemented");

		} else if (expression instanceof BinaryOperation) {

			// TODO
			return error(expression, "not yet implemented");

		} else if (expression instanceof Expression_Mux) {

			// I'm not sure yet whether to allow type errors in the not-taken branch. The use-cases
			// (soft-commenting-out; generating only constants during code generation; ...) aren't clear to me.
			// For now, let's be strict about it so no ugly code creeps in.
			Expression_Mux mux = (Expression_Mux) expression;
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

		} else if (expression instanceof Expression_FunctionCall) {
			Expression_FunctionCall functionCall = (Expression_FunctionCall)expression;
			String functionName = functionCall.getFunctionName().getText();
			StandardFunction standardFunction = StandardFunction.getFromNameInCode(functionName);
			if (standardFunction == null) {
				return error(functionCall.getFunctionName(), "unknown function");
			}

			// TODO
			return error(expression, "not yet implemented");

		} else if (expression instanceof Expression_Parenthesized) {

			return evaluate(((Expression_Parenthesized) expression).getExpression());

		} else {
			return nonConstant(expression);
		}
	}

	@NotNull
	private ConstantValue error(PsiElement element, String message) {
		errorHandler.onError(element, message);
		return ConstantValue.Unknown.INSTANCE;
	}

	@NotNull
	private ConstantValue nonConstant(PsiElement element) {
		return error(element, "expression must be constant");
	}

	@NotNull
	private ConstantValue parseVector(LeafPsiElement textElement) {
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
	private ConstantValue parseText(LeafPsiElement textElement) {
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

	private int handleIndexValue(int containerSize, String containerType, Expression indexExpression) {
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

}
