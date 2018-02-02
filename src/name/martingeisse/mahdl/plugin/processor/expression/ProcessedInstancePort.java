/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public class ProcessedInstancePort extends ProcessedExpression {

	private final ModuleInstance moduleInstance;
	private final String portName;

	public ProcessedInstancePort(PsiElement errorSource, ProcessedDataType dataType, ModuleInstance moduleInstance, String portName) {
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

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return context.notConstant(this);
	}

}
