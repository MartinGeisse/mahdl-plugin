/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.definition.InstancePort;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public class InstancePortReference extends ProcessedExpression {

	private final ModuleInstance moduleInstance;
	private final String portName;

	public InstancePortReference(PsiElement errorSource, ProcessedDataType dataType, ModuleInstance moduleInstance, String portName) {
		super(errorSource, dataType);
		this.moduleInstance = moduleInstance;
		this.portName = portName;
	}

	public ModuleInstance getModuleInstance() {
		return moduleInstance;
	}

	public String getPortName() {
		return portName;
	}

	public InstancePort getPort() {
		return moduleInstance.getPorts().get(portName);
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return context.notConstant(this);
	}

}
