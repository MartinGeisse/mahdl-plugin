/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
			if (!aCase.getSelectorValue().getDataType().equals(selector.getDataType())) {
				throw new TypeErrorException();
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
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		ConstantValue selectorValue = selector.evaluateFormallyConstant(context);
		if (selectorValue instanceof ConstantValue.Unknown) {
			return selectorValue;
		}
		for (Case aCase : cases) {
			if (selectorValue.equals(aCase.getSelectorValue())) {
				return aCase.getResultValue().evaluateFormallyConstant(context);
			}
		}
		if (defaultBranch == null) {
			return context.error(this, "constant selector does not match any match value and no default case exists");
		}
		return defaultBranch.evaluateFormallyConstant(context);
	}

	public static final class Case {

		@NotNull
		private final ConstantValue.Vector selectorValue;

		@NotNull
		private final ProcessedExpression resultValue;

		public Case(@NotNull ConstantValue.Vector selectorValue, @NotNull ProcessedExpression resultValue) {
			this.selectorValue = selectorValue;
			this.resultValue = resultValue;
		}

		@NotNull
		public ConstantValue.Vector getSelectorValue() {
			return selectorValue;
		}

		@NotNull
		public ProcessedExpression getResultValue() {
			return resultValue;
		}

	}

}
