/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.constant;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.FunctionParameterException;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression_old.ExpressionTypeChecker;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.LiteralParser;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;

/**
 * TODO don't repeat type checks which the {@link ExpressionTypeChecker} should do. Instead, type-check all expressions
 * including constants first, then rely on type safety and only report a generic error if something goes wrong.
 */
public final class ConstantExpressionEvaluator {

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

	@NotNull
	private ConstantValue evaluateRangeSelectionFixed(@NotNull Expression_RangeSelectionFixed rangeSelection) {
		return evaluateRangeSelectionHelper(rangeSelection.getContainer(), rangeSelection.getFrom(), rangeSelection.getTo(), 0);
	}

	private ConstantValue evaluateRangeSelectionHelper(Expression container, Expression from, Expression other, int direction) {
		ConstantValue containerValue = evaluate(container);
		int containerSize = handleContainerValue(container, containerValue, false, "range-select");
		String containerTypeText = containerValue.getDataType().toString();
		int intFrom = handleIndexValue(containerSize, containerTypeText, from);
		int intTo = (direction == 0) ? handleIndexValue(containerSize, containerTypeText, other) : null;
		if (containerSize < 0 || intFrom < 0 || intTo < 0) {
			return ConstantValue.Unknown.INSTANCE;
		} else {
			// all error cases should be handled above and should have reported an error already, so we don't have to do that here
			return containerValue.selectRange(intFrom, intTo);
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

		// Only logical operators can handle bit values, and only if both operands are bits.
		if ((leftOperandValue instanceof ConstantValue.Bit) != (rightOperandValue instanceof ConstantValue.Bit)) {
			return error(expression, "this operator cannot be used for " + leftOperandValue.getDataTypeFamily() +
				" and " + rightOperandValue.getDataTypeFamily() + " operands");
		}
		if (leftOperandValue instanceof ConstantValue.Bit) {
			boolean leftBoolean = ((ConstantValue.Bit) leftOperandValue).isSet();
			boolean rightBoolean = ((ConstantValue.Bit) rightOperandValue).isSet();
			try {
				return new ConstantValue.Bit(BinaryOperatorUtil.evaluateLogicalOperator(expression, leftBoolean, rightBoolean));
			} catch (BinaryOperatorUtil.OperatorException e) {
				return error(expression, e.getMessage());
			}
		}

		// Concatenation can handle various types. Logical operators can handle bit values. All other operators can
		// only handle vectors and integers.
		if (expression instanceof Expression_BinaryConcat) {
			return evaluateConcatenation((Expression_BinaryConcat) expression, leftOperandValue, rightOperandValue);
		}
		if (!(leftOperandValue instanceof ConstantValue.Vector) && !(leftOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, leftOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as left operand here");
		}
		if (!(rightOperandValue instanceof ConstantValue.Vector) && !(rightOperandValue instanceof ConstantValue.Integer)) {
			return error(expression, rightOperandValue.getDataTypeFamily().getDisplayString() + " type not allowed as right operand here");
		}

		//
		// Note about the type rules for TAIVOs (shift operators): For an expression that is known to be constant,
		// the special rules for TAIVOs disappear. So we don't have to watch out for them here -- either the expression
		// is constant, or we bail out anyway.
		//

		// perform the corresponding integer operation
		BigInteger leftInteger = leftOperandValue.convertToInteger();
		BigInteger rightInteger = rightOperandValue.convertToInteger();
		ConstantValue integerResultValue;
		BigInteger resultInteger;
		try {
			integerResultValue = BinaryOperatorUtil.evaluateIntegerVectorOperator(expression, leftInteger, rightInteger);
			resultInteger = integerResultValue.convertToInteger();
			if (resultInteger == null) {
				return error(expression, "got result value of wrong type for binary operator: " + integerResultValue.getDataTypeFamily());
			}
		} catch (BinaryOperatorUtil.OperatorException e) {
			return error(expression, e.getMessage());
		}

		// shifting can handle vectors of different size
		if (expression instanceof Expression_BinaryShiftLeft || expression instanceof Expression_BinaryShiftRight) {
			if (leftOperandValue instanceof ConstantValue.Vector) {
				int size = ((ConstantValue.Vector) leftOperandValue).getSize();
				return new ConstantValue.Vector(size, resultInteger);
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
		return new ConstantValue.Vector(resultSize, resultInteger);
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

				BitSet resultBits = new BitSet();
				resultBits.set(1, leftBit.isSet());
				resultBits.set(0, rightBit.isSet());
				return new ConstantValue.Vector(2, resultBits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet resultBits = rightVector.getBits();
				resultBits.set(rightVector.getSize(), leftBit.isSet());
				return new ConstantValue.Vector(rightVector.getSize() + 1, resultBits);

			}

		} else if (leftOperandValue instanceof ConstantValue.Vector) {
			ConstantValue.Vector leftVector = (ConstantValue.Vector) leftOperandValue;

			if (rightOperandValue instanceof ConstantValue.Bit) {
				ConstantValue.Bit rightBit = (ConstantValue.Bit) rightOperandValue;

				BitSet resultBits = new BitSet();
				if (rightBit.isSet()) {
					resultBits.set(0);
				}
				BitSet leftBits = leftVector.getBits();
				for (int i = 0; i < leftVector.getSize(); i++) {
					resultBits.set(1 + i, leftBits.get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + 1, resultBits);

			} else if (rightOperandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector rightVector = (ConstantValue.Vector) rightOperandValue;

				BitSet resultBits = rightVector.getBits();
				BitSet leftBits = leftVector.getBits();
				for (int i = 0; i < leftVector.getSize(); i++) {
					resultBits.set(rightVector.getSize() + i, leftBits.get(i));
				}
				return new ConstantValue.Vector(leftVector.getSize() + rightVector.getSize(), resultBits);

			}

		}

		return error(expression, "concatenation operator cannot be used on typed " +
			leftOperandValue.getDataTypeFamily().getDisplayString() + " and " +
			rightOperandValue.getDataTypeFamily().getDisplayString());
	}

	@NotNull
	private ConstantValue evaluateConditional(@NotNull Expression_Conditional conditional) {

		//
		// I'm not sure yet whether to allow type errors in the not-taken branch. The use-cases
		// (soft-commenting-out; generating only constants during code generation; ...) aren't clear to me.
		// For now, let's be strict about it so no ugly code creeps in.
		//

		ConstantValue conditionValue = evaluate(conditional.getCondition());
		ConstantValue thenValue = evaluate(conditional.getThenBranch());
		ConstantValue elseValue = evaluate(conditional.getElseBranch());
		boolean error = false;

		// handle condition
		boolean conditionBoolean;
		if (conditionValue instanceof ConstantValue.Unknown) {
			error = true;
			conditionBoolean = false;
		} else {
			Boolean conditionBooleanOrNull = conditionValue.convertToBoolean();
			if (conditionBooleanOrNull == null) {
				error(conditional.getCondition(), "cannot use value of type " + conditionValue.getDataTypeFamily().getDisplayString() + " as selector");
				error = true;
				conditionBoolean = false;
			} else {
				conditionBoolean = conditionBooleanOrNull;
			}
		}

		// handle branches
		if (!thenValue.getDataType().equals(elseValue.getDataType())) {
			if (thenValue instanceof ConstantValue.Integer && elseValue instanceof ConstantValue.Vector) {
				int size = ((ConstantValue.Vector) elseValue).getSize();
				thenValue = new ConstantValue.Vector(size, ((ConstantValue.Integer) thenValue).getValue());
			} else if (thenValue instanceof ConstantValue.Vector && elseValue instanceof ConstantValue.Integer) {
				int size = ((ConstantValue.Vector) thenValue).getSize();
				elseValue = new ConstantValue.Vector(size, ((ConstantValue.Integer) elseValue).getValue());
			} else {
				error(conditional, "incompatible types in then/else branches: " + thenValue.getDataType() + " vs. " + elseValue.getDataType());
				error = true;
			}
		}

		return error ? ConstantValue.Unknown.INSTANCE : conditionBoolean ? thenValue : elseValue;
	}


}
