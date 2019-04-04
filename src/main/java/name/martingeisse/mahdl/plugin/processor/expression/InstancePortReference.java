/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.definition.InstancePort;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class InstancePortReference extends ProcessedExpression {

	@NotNull
	private final ModuleInstance moduleInstance;

	@NotNull
	private final InstancePort port;

	public InstancePortReference(@NotNull PsiElement errorSource,
								 @NotNull ModuleInstance moduleInstance,
								 @NotNull InstancePort port) {
		super(errorSource, port.getDataType());
		this.moduleInstance = moduleInstance;
		this.port = port;
	}

	@NotNull
	public ModuleInstance getModuleInstance() {
		return moduleInstance;
	}

	@NotNull
	public InstancePort getPort() {
		return port;
	}

	@Override
	@NotNull
	protected ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {
		return context.notConstant(this);
	}

	@NotNull
	@Override
	protected ProcessedExpression performFolding(@NotNull ErrorHandler errorHandler) {
		return this;
	}

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		return this;
	}

}
