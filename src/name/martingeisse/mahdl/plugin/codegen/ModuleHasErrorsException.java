/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.util.UserMessageException;

/**
 *
 */
public class ModuleHasErrorsException extends UserMessageException {

	public ModuleHasErrorsException(String message) {
		super("module has errors: " + message);
	}

}
