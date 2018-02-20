package name.martingeisse.mahdl.plugin.ise_build;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public abstract class TextFileGenerator {

	private final Charset charset;

	public TextFileGenerator() {
		this(StandardCharsets.US_ASCII);
	}

	public TextFileGenerator(Charset charset) {
		this.charset = charset;
	}

	public final void generate(OutputStream out) throws IOException {
		try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out, charset)) {
			generate(outputStreamWriter);
		}
	}

	public final void generate(Writer out) throws IOException {
		try (PrintWriter printWriter = new PrintWriter(out)) {
			generate(printWriter);
		}
	}

	protected abstract void generate(PrintWriter out);

}
