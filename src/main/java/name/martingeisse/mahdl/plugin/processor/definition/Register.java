/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class Register extends SignalLike {

	@Nullable
	private ConstantValue initializerValue;

	public Register(@NotNull PsiElement nameElement,
					@NotNull DataType dataTypeElement,
					@NotNull ProcessedDataType processedDataType,
					@Nullable ExtendedExpression initializer) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
		super.processExpressions(expressionProcessor);
		if (getProcessedInitializer() != null) {
			// if there is an initializer then it must be formally constant
			initializerValue = getProcessedInitializer().evaluateFormallyConstant(
				new ProcessedExpression.FormallyConstantEvaluationContext(expressionProcessor.getErrorHandler()));
		}
	}

	@Nullable
	public ConstantValue getInitializerValue() {
		return initializerValue;
	}

}
