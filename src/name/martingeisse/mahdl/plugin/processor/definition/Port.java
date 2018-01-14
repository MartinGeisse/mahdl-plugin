/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.PortDirection;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class Port extends SignalLike {

	private final PortDirection directionElement;

	public Port(@NotNull PsiElement nameElement,
				@NotNull PortDirection directionElement,
				@NotNull DataType dataTypeElement,
				@NotNull ProcessedDataType processedDataType) {
		super(nameElement, dataTypeElement, processedDataType, null);
		this.directionElement = directionElement;
	}

	@NotNull
	public PortDirection getDirectionElement() {
		return directionElement;
	}

}
