/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 *
 */
public final class ProcessedUnaryOperation extends ProcessedExpression {

	private final ProcessedExpression operand;
	private final ProcessedUnaryOperator operator;

	public ProcessedUnaryOperation(@NotNull PsiElement errorSource,
								   @NotNull ProcessedExpression operand,
								   @NotNull ProcessedUnaryOperator operator) throws TypeErrorException {
		super(errorSource, operator.checkType(operator.checkType(operand.getDataType())));
		this.operand = operand;
		this.operator = operator;
	}

	@NotNull
	public ProcessedExpression getOperand() {
		return operand;
	}

	@NotNull
	public ProcessedUnaryOperator getOperator() {
		return operator;
	}

	@Override
	@NotNull
	public ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {

		// determine operand value
		ConstantValue operandValue = operand.evaluateFormallyConstant(context);
		if (operandValue instanceof ConstantValue.Unknown) {
			return operandValue;
		}

		// Only unary NOT can handle bit values. All other operators require a vector or integer.
		if (operator == ProcessedUnaryOperator.NOT && operandValue instanceof ConstantValue.Bit) {
			boolean bitOperandValue = ((ConstantValue.Bit) operandValue).isSet();
			return new ConstantValue.Bit(!bitOperandValue);
		}
		if (!(operandValue instanceof ConstantValue.Vector) && !(operandValue instanceof ConstantValue.Integer)) {
			return context.evaluationInconsistency(this, "found unary operation " + operator + " " + operandValue);
		}

		// shortcut for unary plus, which doesn't actually do anything
		if (operator == ProcessedUnaryOperator.PLUS) {
			return operandValue;
		}

		// perform the corresponding integer operation
		BigInteger integerOperand = operandValue.convertToInteger();
		if (integerOperand == null) {
			return context.evaluationInconsistency(this, "could not convert operand to integer");
		}
		BigInteger integerResult;
		if (operator == ProcessedUnaryOperator.NOT) {
			integerResult = integerOperand.not();
		} else if (operator == ProcessedUnaryOperator.MINUS) {
			integerResult = integerOperand.negate();
		} else {
			return context.evaluationInconsistency(this, "unknown operator");
		}

		// if the operand was a vector, turn the result into a vector of the same size, otherwise return as integer
		if (operandValue instanceof ConstantValue.Vector) {
			int size = ((ConstantValue.Vector) operandValue).getSize();
			return new ConstantValue.Vector(size, integerResult, true);
		} else {
			return new ConstantValue.Integer(integerResult);
		}

	}

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		ProcessedExpression operand = this.operand.performFolding(errorHandler);
		if (operand != this.operand) {
			try {
				return new ProcessedUnaryOperation(getErrorSource(), operand, operator);
			} catch (TypeErrorException e) {
				errorHandler.onError(getErrorSource(), "internal type error during folding of unary operation");
				return this;
			}
		} else {
			return this;
		}
	}

}
