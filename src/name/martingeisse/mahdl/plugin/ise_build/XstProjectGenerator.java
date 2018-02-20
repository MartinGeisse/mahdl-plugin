package name.martingeisse.mahdl.plugin.ise_build;

import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.input.psi.Module;

import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 */
public class XstProjectGenerator extends TextFileGenerator {

	private final DesignVerilogGenerator designGenerator;

	public XstProjectGenerator(DesignVerilogGenerator designGenerator) {
		this.designGenerator = designGenerator;
	}

	@Override
	protected void generate(PrintWriter out) {
		for (Module module : designGenerator.getGeneratedModules()) {
			out.println("verilog work " + module.getName() + ".v");
		}
	}

}
