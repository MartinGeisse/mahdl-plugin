/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input;

/**
 *
 */
public class ReferenceResolutionException extends Exception {

	public ReferenceResolutionException() {
	}

	public ReferenceResolutionException(String message) {
		super(message);
	}

	public ReferenceResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReferenceResolutionException(Throwable cause) {
		super(cause);
	}

}
