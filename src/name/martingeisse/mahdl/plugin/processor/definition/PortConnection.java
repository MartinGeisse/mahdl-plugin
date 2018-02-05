/**
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

/**
 *
 */
public final class PortConnection {

	private final InstancePort port;
	private final PsiElement portNameElement;
	private final ExtendedExpression expressionElement;
	private ProcessedExpression processedExpression;

	public PortConnection(InstancePort port, PsiElement portNameElement, ExtendedExpression expressionElement) {
		this.port = port;
		this.portNameElement = portNameElement;
		this.expressionElement = expressionElement;
	}

	public InstancePort getPort() {
		return port;
	}

	public PsiElement getPortNameElement() {
		return portNameElement;
	}

	public ExtendedExpression getExpressionElement() {
		return expressionElement;
	}

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
