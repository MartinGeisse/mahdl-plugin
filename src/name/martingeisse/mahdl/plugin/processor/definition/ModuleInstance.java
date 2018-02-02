/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
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
	private ImmutableMap<String, ProcessedExpression> portNameToProcessedExpression;

	public ModuleInstance(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement,
						  @NotNull Module moduleElement,
						  @NotNull ImmutableMap<String, PortDefinitionGroup> portNameToPortDefinitionGroup) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
		this.moduleElement = moduleElement;
		this.portNameToPortDefinitionGroup = portNameToPortDefinitionGroup;
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
				expressionProcessor.error("unknown port: '" + portName + "'");
				continue;
			}
			if (group.getDirection() instanceof PortDirection_In) {
				// TODO type conversion!
			} else {
				// TODO type check!
			}
			portNameToProcessedExpression.put(portName, processedExpression);
		}
		this.portNameToProcessedExpression = ImmutableMap.copyOf(portNameToProcessedExpression);
	}

}
