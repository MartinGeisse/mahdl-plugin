package name.martingeisse.mahdl.plugin.ise_build;

import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;

import java.io.PrintWriter;

/**
 *
 */
public class UploadScriptGenerator extends TextFileGenerator {

	private final BuildContext buildContext;

	public UploadScriptGenerator(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	protected void generate(PrintWriter out) {
		out.println("ssh martin@ise ./auto-ise/upload.sh");
	}

}
