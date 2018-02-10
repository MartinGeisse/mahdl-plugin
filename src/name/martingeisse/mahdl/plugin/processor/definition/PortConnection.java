/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class PortConnection {

	@NotNull
	private final InstancePort port;

	@NotNull
	private final PsiElement portNameElement;

	@NotNull
	private final ExtendedExpression expressionElement;

	@Nullable
	private ProcessedExpression processedExpression;

	public PortConnection(@NotNull InstancePort port, @NotNull PsiElement portNameElement, @NotNull ExtendedExpression expressionElement) {
		this.port = port;
		this.portNameElement = portNameElement;
		this.expressionElement = expressionElement;
	}

	@NotNull
	public InstancePort getPort() {
		return port;
	}

	@NotNull
	public PsiElement getPortNameElement() {
		return portNameElement;
	}

	@NotNull
	public ExtendedExpression getExpressionElement() {
		return expressionElement;
	}

	@Nullable
	public ProcessedExpression getProcessedExpression() {
		return processedExpression;
	}

	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
		processedExpression = expressionProcessor.process(expressionElement);
		if (port.getDirection() == PortDirection.IN) {
			processedExpression = expressionProcessor.convertImplicitly(processedExpression, port.getDataType());
		} else {
			if (!(processedExpression.getDataType() instanceof ProcessedDataType.Unknown)) {
				if (!(port.getDataType() instanceof ProcessedDataType.Unknown)) {
					if (!processedExpression.getDataType().equals(port.getDataType())) {
						expressionProcessor.getErrorHandler().onError(expressionElement, "cannot connect port of type " +
							port.getDataType() + " to expression of type " + processedExpression.getDataType());
					}
				}
			}
		}
	}

}
