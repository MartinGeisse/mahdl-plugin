/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.definition.Constant;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class SignalLikeReference extends ProcessedExpression {

	private final SignalLike definition;

	public SignalLikeReference(@NotNull PsiElement errorSource, @NotNull SignalLike definition) {
		super(errorSource, definition.getProcessedDataType());
		this.definition = definition;
	}

	@NotNull
	public SignalLike getDefinition() {
		return definition;
	}

	@Override
	@NotNull
	public ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {
		if (definition instanceof Constant) {
			ConstantValue value = ((Constant) definition).getValue();
			if (value == null) {
				throw new RuntimeException("defined constant has no value: " + definition.getName());
			}
			return value;
		} else {
			return context.notConstant(this);
		}
	}

}
