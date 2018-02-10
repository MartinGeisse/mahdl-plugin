/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.TypeErrorException;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO detect missing break statement. Group cases with shared statements?
 */
public final class ProcessedSwitchStatement extends ProcessedStatement {

	@NotNull
	private final ProcessedExpression selector;

	@NotNull
	private final ImmutableList<Case> cases;

	@Nullable
	private final ProcessedStatement defaultBranch;

	public ProcessedSwitchStatement(@NotNull PsiElement errorSource,
									@NotNull ProcessedExpression selector,
									@NotNull ImmutableList<Case> cases,
									@Nullable ProcessedStatement defaultBranch) throws TypeErrorException {
		super(errorSource);

		for (Case aCase : cases) {
			if (!aCase.getSelectorValue().getDataType().equals(selector.getDataType())) {
				throw new TypeErrorException();
			}
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
	public ProcessedStatement getDefaultBranch() {
		return defaultBranch;
	}

	public static final class Case {

		@NotNull
		private final ConstantValue.Vector selectorValue;

		@NotNull
		private final ProcessedStatement branch;

		public Case(@NotNull ConstantValue.Vector selectorValue, @NotNull ProcessedStatement branch) {
			this.selectorValue = selectorValue;
			this.branch = branch;
		}

		@NotNull
		public ConstantValue.Vector getSelectorValue() {
			return selectorValue;
		}

		@NotNull
		public ProcessedStatement getBranch() {
			return branch;
		}

	}

}
