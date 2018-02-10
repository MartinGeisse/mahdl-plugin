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

	@NotNull
	private final ImplementationItem_ModuleInstance moduleInstanceElement;

	@NotNull
	private final Module moduleElement;

	@NotNull
	private final ImmutableMap<String, InstancePort> ports;

	@NotNull
	private final ImmutableMap<String, PortConnection> portConnections;

	public ModuleInstance(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement,
						  @NotNull Module moduleElement,
						  @NotNull ImmutableMap<String, InstancePort> ports,
						  @NotNull ImmutableMap<String, PortConnection> portConnections) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
		this.moduleElement = moduleElement;
		this.ports = ports;
		this.portConnections = portConnections;
	}

	@NotNull
	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

	@NotNull
	public Module getModuleElement() {
		return moduleElement;
	}

	@NotNull
	public ImmutableMap<String, InstancePort> getPorts() {
		return ports;
	}

	@NotNull
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
