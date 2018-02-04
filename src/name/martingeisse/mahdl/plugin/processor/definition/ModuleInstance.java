/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ModuleInstance extends Named {

	private final ImplementationItem_ModuleInstance moduleInstanceElement;
	private final Module moduleElement;
	private final ImmutableMap<String, PortConnection> portConnections;

	public ModuleInstance(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement,
						  @NotNull Module moduleElement,
						  @NotNull ImmutableMap<String, PortConnection> portConnections) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
		this.moduleElement = moduleElement;
		this.portConnections = portConnections;
	}

	@NotNull
	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

	public Module getModuleElement() {
		return moduleElement;
	}

	public ImmutableMap<String, PortConnection> getPortConnections() {
		return portConnections;
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
		for (PortConnection connection : portConnections.values()) {
			connection.processExpressions(expressionProcessor);
		}
	}

}
