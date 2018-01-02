package name.martingeisse.mahdl.plugin.definition;

import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;

/**
 *
 */
public final class ModuleInstance extends Named {

	private final ImplementationItem_ModuleInstance moduleInstanceElement;

	public ModuleInstance(ImplementationItem_ModuleInstance moduleInstanceElement) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
	}

	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

}
