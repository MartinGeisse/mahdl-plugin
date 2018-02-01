/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class Constant extends SignalLike {

	private final ConstantValue value;

	public Constant(@NotNull PsiElement nameElement,
					@NotNull DataType dataTypeElement,
					@NotNull ProcessedDataType processedDataType,
					@Nullable ExtendedExpression initializer, // can be null in case of errors in the code
					@NotNull ConstantValue value) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
		this.value = value;
	}

	@NotNull
	public ConstantValue getValue() {
		return value;
	}

	@Override
	public void processInitializer(ExpressionProcessor expressionProcessor) {
		// initializers for constants are processed during evaluation, so we don't repeat this here
	}

}
