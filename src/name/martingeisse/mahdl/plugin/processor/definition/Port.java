/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.PortDirection_In;
import name.martingeisse.mahdl.plugin.input.psi.PortDirection_Out;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class Port extends SignalLike {

	private final name.martingeisse.mahdl.plugin.input.psi.PortDirection directionElement;
	private final PortDirection direction;

	public Port(@NotNull PsiElement nameElement,
				@NotNull name.martingeisse.mahdl.plugin.input.psi.PortDirection directionElement,
				@NotNull DataType dataTypeElement,
				@NotNull ProcessedDataType processedDataType) {
		super(nameElement, dataTypeElement, processedDataType, null);
		this.directionElement = directionElement;
		if (directionElement instanceof PortDirection_In) {
			direction = PortDirection.IN;
		} else if (directionElement instanceof PortDirection_Out) {
			direction = PortDirection.OUT;
		} else {
			throw new IllegalArgumentException("invalid port direction element");
		}
	}

	@NotNull
	public name.martingeisse.mahdl.plugin.input.psi.PortDirection getDirectionElement() {
		return directionElement;
	}

	public PortDirection getDirection() {
		return direction;
	}
	
}
