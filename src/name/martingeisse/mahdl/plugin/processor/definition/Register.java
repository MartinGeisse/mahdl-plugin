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
public final class Register extends SignalLike {

	public Register(PsiElement nameElement, DataType dataTypeElement, ProcessedDataType processedDataType, Expression initializer) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
	}
	
}
