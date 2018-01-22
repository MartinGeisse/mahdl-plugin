/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

/**
 * This exception is internally used to detect the construction of processed expressions with wrong types.
 */
public class TypeErrorException extends Exception {

	public TypeErrorException() {
	}

}
