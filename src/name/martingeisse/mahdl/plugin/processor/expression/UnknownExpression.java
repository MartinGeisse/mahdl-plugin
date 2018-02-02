/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 * This expression is generated in case of errors.
 */
public final class UnknownExpression extends ProcessedExpression {

	public UnknownExpression(PsiElement errorSource) {
		super(errorSource, ProcessedDataType.Unknown.INSTANCE);
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return ConstantValue.Unknown.INSTANCE;
	}

}
