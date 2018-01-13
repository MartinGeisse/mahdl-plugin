/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.util;

/**
 * This exception type carries a message that is intended for the user, not the MaPaG developer, and so gets
 * its stack trace suppressed in the output.
 */
public class UserMessageException extends RuntimeException {

	public UserMessageException(String message) {
		super(message);
	}

}
