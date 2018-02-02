/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class ModuleInstance extends Named {

	private final ImplementationItem_ModuleInstance moduleInstanceElement;
	private final Module moduleElement;
	private final ImmutableMap<String, PortDefinitionGroup> portNameToPortDefinitionGroup;
	private final ImmutableMap<String, ProcessedDataType> portNameToProcessedDataType;
	private ImmutableMap<String, ProcessedExpression> portNameToProcessedExpression;

	public ModuleInstance(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement,
						  @NotNull Module moduleElement,
						  @NotNull ImmutableMap<String, PortDefinitionGroup> portNameToPortDefinitionGroup,
						  @NotNull ImmutableMap<String, ProcessedDataType> portNameToProcessedDataType) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
		this.moduleElement = moduleElement;
		this.portNameToPortDefinitionGroup = portNameToPortDefinitionGroup;
		this.portNameToProcessedDataType = portNameToProcessedDataType;
	}

	@NotNull
	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

	public Module getModuleElement() {
		return moduleElement;
	}

	public ImmutableMap<String, PortDefinitionGroup> getPortNameToPortDefinitionGroup() {
		return portNameToPortDefinitionGroup;
	}

	public ImmutableMap<String, ProcessedDataType> getPortNameToProcessedDataType() {
		return portNameToProcessedDataType;
	}

	public ImmutableMap<String, ProcessedExpression> getPortNameToProcessedExpression() {
		return portNameToProcessedExpression;
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
		Map<String, ProcessedExpression> portNameToProcessedExpression = new HashMap<>();
		for (PortConnection connection : moduleInstanceElement.getPortConnections().getAll()) {
			String portName = connection.getPortName().getIdentifier().getText();
			ProcessedExpression processedExpression = expressionProcessor.process(connection.getExpression());
			PortDefinitionGroup group = portNameToPortDefinitionGroup.get(portName);
			if (group == null) {
				expressionProcessor.getErrorHandler().onError(connection.getPortName().getIdentifier(), "unknown port: '" + portName + "'");
				continue;
			}
			ProcessedDataType portType = portNameToProcessedDataType.get(portName);
			if (portType == null) {
				expressionProcessor.getErrorHandler().onError(connection.getPortName().getIdentifier(), "could not determine data type for port '" + portName + "'");
				continue;
			}
			if (group.getDirection() instanceof PortDirection_In) {
				processedExpression = expressionProcessor.convertImplicitly(processedExpression, portType);
			} else {
				if (!(processedExpression.getDataType() instanceof ProcessedDataType.Unknown)) {
					if (!(portType instanceof ProcessedDataType.Unknown)) {
						if (!processedExpression.getDataType().equals(portType)) {
							expressionProcessor.getErrorHandler().onError(connection.getExpression(), "cannot connect port of type " +
								portType + " to expression of type " + processedExpression.getDataType());
							continue;
						}
					}
				}
			}
			portNameToProcessedExpression.put(portName, processedExpression);
		}
		this.portNameToProcessedExpression = ImmutableMap.copyOf(portNameToProcessedExpression);
	}

}
