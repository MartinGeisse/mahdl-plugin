package name.martingeisse.mahdl.plugin.ise_build;

import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.codegen.ModuleNamingStrategy;

import java.io.PrintWriter;

/**
 *
 */
public class XstScriptGenerator extends TextFileGenerator {

	private final BuildContext buildContext;

	public XstScriptGenerator(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	protected void generate(PrintWriter out) {
		out.println("set -tmpdir build/xst_temp");
		out.println("run");
		out.println("-ifn src/build.prj");
		out.println("-ofmt NGC");
		out.println("-ofn build/synthesized.ngc");
		out.println("-top " + ModuleNamingStrategy.getVerilogNameForMahdlName(buildContext.getToplevelModule().getName()));
		out.println("-p " + buildContext.getConfiguration().getRequired("fpga.part"));
		out.println("-opt_level 1");
		out.println("-opt_mode SPEED");
	}

}
