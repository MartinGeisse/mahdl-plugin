/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 *
 */
public final class ProcessedConstantValue extends ProcessedExpression {

	private final ConstantValue value;

	public ProcessedConstantValue(@NotNull PsiElement errorSource, @NotNull ConstantValue value) {
		super(errorSource, value.getDataType());
		this.value = value;
	}

	@NotNull
	public ConstantValue getValue() {
		return value;
	}

	@NotNull
	@Override
	public ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return value;
	}

	@Nullable
	@Override
	public ProcessedExpression recognizeBitLiteral() {
		if (value instanceof ConstantValue.Bit) {
			return this;
		}
		if (value instanceof ConstantValue.Integer) {
			BigInteger integerValue = ((ConstantValue.Integer) value).getValue();
			if (integerValue.equals(BigInteger.ZERO)) {
				return new ProcessedConstantValue(getErrorSource(), new ConstantValue.Bit(false));
			} else if (integerValue.equals(BigInteger.ONE)) {
				return new ProcessedConstantValue(getErrorSource(), new ConstantValue.Bit(true));
			}
		}
		return null;
	}

}
