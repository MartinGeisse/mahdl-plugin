/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.util.UserMessageException;

/**
 *
 */
public class ModuleCannotGenerateCodeException extends UserMessageException {

	public ModuleCannotGenerateCodeException(String message) {
		super(message);
	}

}
