/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.psi;

import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface UnaryOperation {

	@NotNull
	Expression getOperand();

}
