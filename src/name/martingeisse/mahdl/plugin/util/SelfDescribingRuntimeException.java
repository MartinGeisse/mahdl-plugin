package name.martingeisse.mahdl.plugin.util;

import java.io.PrintWriter;

/**
 *
 */
public abstract class SelfDescribingRuntimeException extends RuntimeException {

	public SelfDescribingRuntimeException() {
	}

	public SelfDescribingRuntimeException(String message) {
		super(message);
	}

	public SelfDescribingRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public SelfDescribingRuntimeException(Throwable cause) {
		super(cause);
	}

	public abstract void describe(PrintWriter out);

}
