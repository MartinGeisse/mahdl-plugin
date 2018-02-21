package name.martingeisse.mahdl.plugin.ise_build;

import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;

import java.io.PrintWriter;
import java.util.Map;

/**
 * TODO some nets need two lines in the UCF
 */
public class UcfGenerator extends TextFileGenerator {

	private final BuildContext buildContext;

	public UcfGenerator(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	protected void generate(PrintWriter out) {
		for (Map.Entry<String, String> entry : buildContext.getConfiguration().getPrefixed("fpga.ucf.").entrySet()) {
			String netName = entry.getKey();
			String constraints = entry.getValue();
			out.println("NET \"" + netName + "\" " + constraints + ";");
		}
	}

}
