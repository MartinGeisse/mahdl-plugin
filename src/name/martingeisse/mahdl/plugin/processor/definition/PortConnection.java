/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class PortConnection {

	private final String portName;
	private final PortDirection portDirection;
	private final ProcessedDataType portType;
	private final ExtendedExpression expressionElement;
	private ProcessedExpression processedExpression;

	public PortConnection(String portName, PortDirection portDirection, ProcessedDataType portType, ExtendedExpression expressionElement) {
		this.portName = portName;
		this.portDirection = portDirection;
		this.portType = portType;
		this.expressionElement = expressionElement;
	}

	public String getPortName() {
		return portName;
	}

	public PortDirection getPortDirection() {
		return portDirection;
	}

	public ProcessedDataType getPortType() {
		return portType;
	}

	public ExtendedExpression getExpressionElement() {
		return expressionElement;
	}

	public ProcessedExpression getProcessedExpression() {
		return processedExpression;
	}

	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
		processedExpression = expressionProcessor.process(expressionElement);
		if (portDirection == PortDirection.IN) {
			processedExpression = expressionProcessor.convertImplicitly(processedExpression, portType);
		} else {
			if (!(processedExpression.getDataType() instanceof ProcessedDataType.Unknown)) {
				if (!(portType instanceof ProcessedDataType.Unknown)) {
					if (!processedExpression.getDataType().equals(portType)) {
						expressionProcessor.getErrorHandler().onError(expressionElement, "cannot connect port of type " +
							portType + " to expression of type " + processedExpression.getDataType());
					}
				}
			}
		}
	}

}
