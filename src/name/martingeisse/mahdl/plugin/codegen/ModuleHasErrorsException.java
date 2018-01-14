/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ModuleHasErrorsException extends UserMessageException {

	public ModuleHasErrorsException(@NotNull String message) {
		super("module has errors: " + message);
	}

}
