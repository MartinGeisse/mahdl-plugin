/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen_new;

import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ModuleCannotGenerateCodeException extends UserMessageException {

	public ModuleCannotGenerateCodeException(@NotNull String message) {
		super(message);
	}

}
