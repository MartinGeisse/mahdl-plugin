/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class AsciizFunction extends StringEncodingFunction {

	@NotNull
	@Override
	public String getName() {
		return "asciiz";
	}

	@Override
	protected void encode(String text, OutputStream out) throws IOException {
		out.write(text.getBytes(StandardCharsets.US_ASCII));
		out.write(0);
	}

}
