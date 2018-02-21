package name.martingeisse.mahdl.plugin.ise_build;

import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;

import java.io.PrintWriter;

/**
 * TODO set owner's x bit on the file
 */
public class BuildScriptGenerator extends TextFileGenerator {

	private final BuildContext buildContext;

	public BuildScriptGenerator(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	protected void generate(PrintWriter out) {
		out.println("ssh martin@ise ./auto-ise/clean.sh");
		out.println("scp -r . martin@ise:./auto-ise/src");
		out.println("ssh martin@ise ./auto-ise/build.sh environment.sh");
	}

}
