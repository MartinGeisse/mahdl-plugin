/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ProcessedConditional extends ProcessedExpression {

	private final ProcessedExpression condition;
	private final ProcessedExpression thenBranch;
	private final ProcessedExpression elseBranch;

	public ProcessedConditional(@NotNull PsiElement errorSource,
								@NotNull ProcessedExpression condition,
								@NotNull ProcessedExpression thenBranch,
								@NotNull ProcessedExpression elseBranch) throws TypeErrorException {
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

	@NotNull
	public ProcessedExpression getCondition() {
		return condition;
	}

	@NotNull
	public ProcessedExpression getThenBranch() {
		return thenBranch;
	}

	@NotNull
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

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		ProcessedExpression condition = this.condition.performFolding(errorHandler);
		ProcessedExpression thenBranch = this.thenBranch.performFolding(errorHandler);
		ProcessedExpression elseBranch = this.elseBranch.performFolding(errorHandler);
		if (condition != this.condition || thenBranch != this.thenBranch || elseBranch != this.elseBranch) {
			try {
				return new ProcessedConditional(getErrorSource(), condition, thenBranch, elseBranch);
			} catch (TypeErrorException e) {
				errorHandler.onError(getErrorSource(), "internal type error during folding of conditional expression");
				return this;
			}
		} else {
			return this;
		}
	}

}
