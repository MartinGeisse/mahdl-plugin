/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.definition.Constant;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;

/**
 *
 */
public final class SignalLikeReference extends ProcessedExpression {

	private final SignalLike definition;

	public SignalLikeReference(PsiElement errorSource, SignalLike definition) {
		super(errorSource, definition.getProcessedDataType());
		this.definition = definition;
	}

	public SignalLike getDefinition() {
		return definition;
	}

	@Override
	public ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		String name = definition.getName();
		if (definition instanceof Constant) {
			return ((Constant) definition).getValue();
		} else {
			return context.notConstant(this);
		}
	}

}
