package name.martingeisse.mahdl.plugin.ise_build;

import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.codegen.ModuleNamingStrategy;
import name.martingeisse.mahdl.plugin.input.psi.Module;

import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public class XstProjectGenerator extends TextFileGenerator {

	private final BuildContext buildContext;

	public XstProjectGenerator(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	protected void generate(PrintWriter out) {
		for (Module module : buildContext.getGeneratedModules()) {
			out.println("verilog work " + ModuleNamingStrategy.getVerilogNameForMahdlName(module.getName()) + ".v");
		}
	}

}
