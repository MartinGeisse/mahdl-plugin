/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedConditional extends ProcessedExpression {

	private final ProcessedExpression condition;
	private final ProcessedExpression thenBranch;
	private final ProcessedExpression elseBranch;

	public ProcessedConditional(PsiElement errorSource,
								ProcessedExpression condition,
								ProcessedExpression thenBranch,
								ProcessedExpression elseBranch) throws TypeErrorException {
		super(errorSource, thenBranch.getDataType());

		if (!(condition.getDataType() instanceof ProcessedDataType.Bit)) {
			throw new TypeErrorException();
		}
		if (!thenBranch.getDataType().equals(elseBranch.getDataType())) {
			throw new TypeErrorException();
		}

		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

	public ProcessedExpression getCondition() {
		return condition;
	}

	public ProcessedExpression getThenBranch() {
		return thenBranch;
	}

	public ProcessedExpression getElseBranch() {
		return elseBranch;
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		// evaluate both branches to detect errors even in the not-taken branch
		Boolean conditionBoolean = condition.evaluateFormallyConstant(context).convertToBoolean();
		ConstantValue thenValue = thenBranch.evaluateFormallyConstant(context);
		ConstantValue elseValue = elseBranch.evaluateFormallyConstant(context);
		if (conditionBoolean == null) {
			return context.evaluationInconsistency(this, "cannot convert condition to boolean");
		} else if (conditionBoolean) {
			return thenValue;
		} else {
			return elseValue;
		}
	}

}
