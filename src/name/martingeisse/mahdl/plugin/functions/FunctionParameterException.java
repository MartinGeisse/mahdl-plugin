/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

/**
 * This class signals that a function was applied to the wrong number of parameters, or parameters of wrong type,
 * or parameters that should have been constant but aren't, or (in the case of constant-valued parameters) parameters
 * with the wrong value.
 * <p>
 * This exception should NOT be thrown if a function is used in away that is allowed but pointless.
 */
public class FunctionParameterException extends Exception {

	public FunctionParameterException(String message) {
		super(message);
	}

}
