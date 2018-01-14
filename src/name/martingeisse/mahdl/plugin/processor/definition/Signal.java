/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class Signal extends SignalLike {

	public Signal(@NotNull PsiElement nameElement,
				  @NotNull DataType dataTypeElement,
				  @NotNull ProcessedDataType processedDataType,
				  @Nullable Expression initializer) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
	}

}
