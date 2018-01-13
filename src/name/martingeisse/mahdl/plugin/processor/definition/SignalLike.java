/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public abstract class SignalLike extends Named {

	private final DataType dataTypeElement;
	private final ProcessedDataType processedDataType;
	private final Expression initializer;

	public SignalLike(PsiElement nameElement, DataType dataTypeElement, ProcessedDataType processedDataType, Expression initializer) {
		super(nameElement);
		this.dataTypeElement = dataTypeElement;
		this.processedDataType = processedDataType;
		this.initializer = initializer;
	}

	public DataType getDataTypeElement() {
		return dataTypeElement;
	}

	public ProcessedDataType getProcessedDataType() {
		return processedDataType;
	}

	public Expression getInitializer() {
		return initializer;
	}

}
