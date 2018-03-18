/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedAssignment;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedStatement;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedSwitchStatement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class ProcessedSwitchExpression extends ProcessedExpression {

	@NotNull
	private final ProcessedExpression selector;

	@NotNull
	private final ImmutableList<Case> cases;

	@Nullable
	private final ProcessedExpression defaultBranch;

	public ProcessedSwitchExpression(@NotNull PsiElement errorSource,
									 @NotNull ProcessedDataType dataType,
									 @NotNull ProcessedExpression selector,
									 @NotNull ImmutableList<Case> cases,
									 @Nullable ProcessedExpression defaultBranch) throws TypeErrorException {
		super(errorSource, dataType);

		for (Case aCase : cases) {
			for (ConstantValue caseSelectorValue : aCase.getSelectorValues()) {
				if (!caseSelectorValue.getDataType().equals(selector.getDataType())) {
					throw new TypeErrorException();
				}
			}
			if (!aCase.getResultValue().getDataType().equals(dataType)) {
				throw new TypeErrorException();
			}
		}
		if (defaultBranch != null && !defaultBranch.getDataType().equals(dataType)) {
			throw new TypeErrorException();
		}

		this.selector = selector;
		this.cases = cases;
		this.defaultBranch = defaultBranch;
	}

	@NotNull
	public ProcessedExpression getSelector() {
		return selector;
	}

	@NotNull
	public ImmutableList<Case> getCases() {
		return cases;
	}

	@Nullable
	public ProcessedExpression getDefaultBranch() {
		return defaultBranch;
	}

	@Override
	@NotNull
	protected ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {
		ConstantValue selectorValue = selector.evaluateFormallyConstant(context);
		if (selectorValue instanceof ConstantValue.Unknown) {
			return selectorValue;
		}
		for (Case aCase : cases) {
			for (ConstantValue caseSelectorValue : aCase.getSelectorValues()) {
				if (selectorValue.equals(caseSelectorValue)) {
					return aCase.getResultValue().evaluateFormallyConstant(context);
				}
			}
		}
		if (defaultBranch == null) {
			return context.error(this, "constant selector does not match any match value and no default case exists");
		}
		return defaultBranch.evaluateFormallyConstant(context);
	}

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		ProcessedExpression selector = this.selector.performFolding(errorHandler);
		ProcessedExpression defaultBranch = (this.defaultBranch == null ? null : this.defaultBranch.performFolding(errorHandler));
		boolean folded = (selector != this.selector || defaultBranch != this.defaultBranch);
		List<Case> cases = new ArrayList<>();
		for (Case aCase : this.cases) {
			Case foldedCase = aCase.performFolding(errorHandler);
			cases.add(foldedCase);
			if (foldedCase != aCase) {
				folded = true;
			}
		}
		try {
			return folded ? new ProcessedSwitchExpression(getErrorSource(), getDataType(), selector, ImmutableList.copyOf(cases), defaultBranch) : this;
		} catch (TypeErrorException e) {
			errorHandler.onError(getErrorSource(), "internal type error during folding of switch expression");
			return this;
		}
	}

	public static final class Case {

		@NotNull
		private final ImmutableList<ConstantValue.Vector> selectorValues;

		@NotNull
		private final ProcessedExpression resultValue;

		public Case(@NotNull ImmutableList<ConstantValue.Vector> selectorValues, @NotNull ProcessedExpression resultValue) {
			this.selectorValues = selectorValues;
			this.resultValue = resultValue;
		}

		@NotNull
		public ImmutableList<ConstantValue.Vector> getSelectorValues() {
			return selectorValues;
		}

		@NotNull
		public ProcessedExpression getResultValue() {
			return resultValue;
		}

		public Case performFolding(ErrorHandler errorHandler) {
			ProcessedExpression resultValue = this.resultValue.performFolding(errorHandler);
			return (resultValue == this.resultValue ? this : new Case(selectorValues, resultValue));
		}

	}

	@NotNull
	public ProcessedSwitchStatement convertToStatement(@NotNull ProcessedExpression destination) {
		try {
			List<ProcessedSwitchStatement.Case> statementCases = new ArrayList<>();
			for (ProcessedSwitchExpression.Case expressionCase : cases) {
				ProcessedStatement branch = new ProcessedAssignment(getErrorSource(), destination, expressionCase.getResultValue());
				statementCases.add(new ProcessedSwitchStatement.Case(expressionCase.getSelectorValues(), branch));
			}
			ProcessedStatement defaultBranch = new ProcessedAssignment(getErrorSource(), destination, this.defaultBranch);
			return new ProcessedSwitchStatement(getErrorSource(), selector, ImmutableList.copyOf(statementCases), defaultBranch);
		} catch (TypeErrorException e) {
			throw new RuntimeException(e);
		}
	}

}
