/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * This object is used instead of a {@link ModuleInstance} if the module definition cannot be resolved.
 * It isn't supported in most code that wants to deal with a module instance, but helps to improve error
 * messages in some cases.
 */
public final class ModuleInstanceWithMissingDefinition extends Named {

	@NotNull
	private final ImplementationItem_ModuleInstance moduleInstanceElement;

	public ModuleInstanceWithMissingDefinition(@NotNull ImplementationItem_ModuleInstance moduleInstanceElement) {
		super(moduleInstanceElement.getInstanceName());
		this.moduleInstanceElement = moduleInstanceElement;
	}

	@NotNull
	public ImplementationItem_ModuleInstance getModuleInstanceElement() {
		return moduleInstanceElement;
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
	}

}
