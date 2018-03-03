/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.input.psi.ModuleInstanceDefinition;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ModuleInstance extends Named {

	@NotNull
	private final ModuleInstanceDefinition moduleInstanceDefinitionElement;

	@NotNull
	private final Module moduleElement;

	@NotNull
	private final ImmutableMap<String, InstancePort> ports;

	public ModuleInstance(@NotNull ModuleInstanceDefinition moduleInstanceDefinitionElement,
						  @NotNull Module moduleElement,
						  @NotNull ImmutableMap<String, InstancePort> ports) {
		super(moduleInstanceDefinitionElement.getIdentifier());
		this.moduleInstanceDefinitionElement = moduleInstanceDefinitionElement;
		this.moduleElement = moduleElement;
		this.ports = ports;
	}

	@NotNull
	public ModuleInstanceDefinition getModuleInstanceDefinitionElement() {
		return moduleInstanceDefinitionElement;
	}

	@NotNull
	public Module getModuleElement() {
		return moduleElement;
	}

	@NotNull
	public ImmutableMap<String, InstancePort> getPorts() {
		return ports;
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
	}

}
