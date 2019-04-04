/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.input.psi.ModuleInstanceDefinition;
import name.martingeisse.mahdl.plugin.input.psi.QualifiedModuleName;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * This object is used instead of a {@link ModuleInstance} if the module definition cannot be resolved.
 * It isn't supported in most code that wants to deal with a module instance, but helps to improve error
 * messages in some cases.
 */
public final class ModuleInstanceWithMissingDefinition extends Named {

	@NotNull
	private final QualifiedModuleName moduleNameElement;

	@NotNull
	private final ModuleInstanceDefinition moduleInstanceDefinitionElement;

	public ModuleInstanceWithMissingDefinition(@NotNull QualifiedModuleName moduleNameElement, @NotNull ModuleInstanceDefinition moduleInstanceDefinitionElement) {
		super(moduleInstanceDefinitionElement.getIdentifier());
		this.moduleNameElement = moduleNameElement;
		this.moduleInstanceDefinitionElement = moduleInstanceDefinitionElement;
	}

	@NotNull
	public QualifiedModuleName getModuleNameElement() {
		return moduleNameElement;
	}

	@NotNull
	public ModuleInstanceDefinition getModuleInstanceDefinitionElement() {
		return moduleInstanceDefinitionElement;
	}

	@Override
	public void processExpressions(@NotNull ExpressionProcessor expressionProcessor) {
	}

}
