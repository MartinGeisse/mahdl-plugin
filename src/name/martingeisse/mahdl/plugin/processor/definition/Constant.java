/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;

/**
 *
 */
public final class Constant extends SignalLike {

	private final ConstantValue value;

	public Constant(PsiElement nameElement, DataType dataTypeElement, ProcessedDataType processedDataType, Expression initializer, ConstantValue value) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
		this.value = value;
	}

	public ConstantValue getValue() {
		return value;
	}

}
