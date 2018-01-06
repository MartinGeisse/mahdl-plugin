package name.martingeisse.mahdl.plugin.processor.constant;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class ConstantExpressionEvaluator {

	private static final Pattern VECTOR_PATTERN = Pattern.compile("([0-9]+)([bodh])([0-9]+)");

	private static final BigInteger MAX_INDEX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final Module module;
	private final Map<String, ConstantValue> definedConstants = new HashMap<>();

	public ConstantExpressionEvaluator(Module module) {
		this.module = module;
	}

	public Map<String, ConstantValue> getDefinedConstants() {
		return definedConstants;
	}

	public void processConstantDefinitions() {
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition signalLike = (ImplementationItem_SignalLikeDefinition)implementationItem;
				if (signalLike.getKind() instanceof SignalLikeKind_Constant) {
					// for constants, the data type must be valid based on the constants defined above
					ProcessedDataType processedDataType = processDataType(signalLike.getDataType());
					for (DeclaredSignalLike declaredSignalLike : signalLike.getSignalNames().getAll()) {
						if (declaredSignalLike instanceof DeclaredSignalLike_WithoutInitializer) {

							DeclaredSignalLike_WithoutInitializer typedDeclaredSignalLike = (DeclaredSignalLike_WithoutInitializer)declaredSignalLike;
							onError(declaredSignalLike, "constant must have an initializer");
							definedConstants.put(typedDeclaredSignalLike.getIdentifier().getText(), ConstantValue.Unknown.INSTANCE);

						} else if (declaredSignalLike instanceof DeclaredSignalLike_WithInitializer) {

							DeclaredSignalLike_WithInitializer typedDeclaredSignalLike = (DeclaredSignalLike_WithInitializer)declaredSignalLike;
							ConstantValue value = evaluate(typedDeclaredSignalLike.getInitializer());
							definedConstants.put(typedDeclaredSignalLike.getIdentifier().getText(), processedDataType.convertValueImplicitly(value));

						} else {

							onError(declaredSignalLike, "unknown PSI node");

						}
					}
				}
			}
		}

	}

	/**
	 * Returns null for non-constant expressions or errors.
	 */
	public final ConstantValue evaluate(Expression expression) {
		if (expression instanceof Expression_Literal) {

			Literal literal = ((Expression_Literal) expression).getLiteral();
			if (literal instanceof Literal_Vector) {
				String text = ((Literal_Vector) literal).getValue().getText();
				return parseVector(text);
			} else if (literal instanceof Literal_Integer) {
				String text = ((Literal_Integer) literal).getValue().getText();
				return new ConstantValue.Integer(new BigInteger(text));
			} else if (literal instanceof Literal_Text) {
				String rawText = ((Literal_Text) literal).getValue().getText();
				return parseText(rawText);
			} else {
				return nonConstant(expression);
			}

		} else if (expression instanceof Expression_Signal) {

			String name = ((Expression_Signal) expression).getSignalName().getText();
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
			if (containerValue == null) {
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
			if (containerValue == null || intIndexValue < 0) {
				return null;
			}
			return containerValue.selectIndex(intIndexValue);

		} else if (expression instanceof Expression_RangeSelection) {

			Expression_RangeSelection rangeSelection = (Expression_RangeSelection) expression;
			ConstantValue containerValue = evaluate(rangeSelection.getContainer());
			int containerSize;
			String containerTypeText;
			if (containerValue == null) {
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
			if (containerValue == null || intFromIndexValue < 0 || intToIndexValue < 0) {
				return null;
			}
			return containerValue.selectRange(intFromIndexValue, intToIndexValue);

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
			Boolean booleanCondition = conditionValue.convertToBoolean();
			return booleanCondition == null ? null : booleanCondition ? thenValue : elseValue;

		} else if (expression instanceof Expression_FunctionCall) {

			// TODO
			return error(expression, "not yet implemented");

		} else if (expression instanceof Expression_Parenthesized) {

			return evaluate(((Expression_Parenthesized) expression).getExpression());

		} else {
			return nonConstant(expression);
		}
	}

	protected abstract void onError(PsiElement errorSource, String message);
	protected abstract ProcessedDataType processDataType(DataType dataType);

	private ConstantValue error(PsiElement element, String message) {
		onError(element, message);
		return null;
	}

	private ConstantValue nonConstant(PsiElement element) {
		return error(element, "expression must be constant");
	}

	private ConstantValue.Vector parseVector(String text) {
		Matcher matcher = VECTOR_PATTERN.matcher(text);
		if (!matcher.matches()) {
			return null;
		}
		int size = Integer.parseInt(matcher.group(1));
		char radixCode = matcher.group(2).charAt(0);
		String digits = matcher.group(3);

		int radix = radixCode == 'b' ? 2 : radixCode == 'o' ? 8 : radixCode == 'd' ? 10 : radixCode == 'h' ? 16 : 0;
		if (radix == 0) {
			return null;
		}
		final BigInteger integerValue = new BigInteger(digits, radix);
		if (integerValue.bitLength() > size) {
			return null;
		}
		return new ConstantValue.Vector(size, IntegerBitUtil.convertToBitSet(integerValue, size));
	}

	private ConstantValue.Text parseText(String rawText) {
		if (rawText.charAt(0) != '"' || rawText.charAt(rawText.length() - 1) != '"') {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		boolean escape = false;
		for (int i = 1; i < rawText.length() - 1; i++) {
			char c = rawText.charAt(i);
			if (escape) {
				// escapes not supported (yet), and it's not clear whether we need them
				return null;
			} else if (c == '\\') {
				escape = true;
			} else {
				builder.append(c);
			}
		}
		if (escape) {
			// unfinished escape sequence
			return null; // TODO we need better error output for such cases, e.g. use an exception (and then never return null)
		}
		return new ConstantValue.Text(builder.toString());
	}

	private int handleIndexValue(int containerSize, String containerType, Expression indexExpression) {
		ConstantValue indexValue = evaluate(indexExpression);
		if (indexValue == null) {
			return -1;
		}
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
			error(indexExpression, "index too large for type " + containerType + ": " + numericIndexValue);
			return -1;
		}
		return intValue;
	}

}
