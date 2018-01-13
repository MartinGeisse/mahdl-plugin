/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.psi;

/**
 *
 */
public interface BinaryOperation {

	Expression getLeftOperand();

	Expression getRightOperand();

}
