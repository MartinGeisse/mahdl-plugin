/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ModuleInstance extends Named {

	private final ImplementationItem_ModuleInstance moduleInstanceElement;

	public ModuleInstance(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
	}

	@NotNull
	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

}
