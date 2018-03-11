/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/**
 *
 */
public abstract class HeadBodyReader {

	public void readFrom(Reader reader) throws IOException, FormatException {
		LineNumberReader lineNumberReader = new LineNumberReader(reader);
		while (true) {
			String line = lineNumberReader.readLine();
			if (line == null) {
				throw new FormatException("missing file body");
			}
			if (line.isEmpty()) {
				break;
			}
			int colonIndex = line.indexOf(':');
			if (colonIndex == -1) {
				throw new FormatException("invalid head property line: " + line);
			}
			String key = line.substring(0, colonIndex).trim();
			String value = line.substring(colonIndex + 1).trim();
			onHeadProperty(key, value);
		}
		int bodyStartLineIndex = lineNumberReader.getLineNumber();
		while (true) {
			String line = lineNumberReader.readLine();
			if (line == null) {
				break;
			}
			onBodyLine(lineNumberReader.getLineNumber(), lineNumberReader.getLineNumber() - bodyStartLineIndex, line);
		}
	}

	protected abstract void onHeadProperty(String key, String value) throws FormatException;

	protected abstract void onBodyLine(int totalLineIndex, int bodyLineIndex, String line) throws FormatException;

	public static class FormatException extends Exception {

		public FormatException() {
		}

		public FormatException(String message) {
			super(message);
		}

		public FormatException(String message, Throwable cause) {
			super(message, cause);
		}

		public FormatException(Throwable cause) {
			super(cause);
		}

	}
}
