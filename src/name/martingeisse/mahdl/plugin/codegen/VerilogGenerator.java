package name.martingeisse.mahdl.plugin.codegen;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VerilogGenerator {

	private final Module toplevelModule;
	private final Writer out;

	public VerilogGenerator(Module toplevelModule, Writer out) {
		this.toplevelModule = toplevelModule;
		this.out = out;
	}

	public void run() throws IOException {
		generateModule(toplevelModule);
	}

	private void generateModule(Module module) throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("moduleName", module.getModuleName().getText());

		List<Port> ports = new ArrayList<>();
		for (PortDefinition portDefinition : module.getPorts().getAll()) {
			for (LeafPsiElement identifierElement : portDefinition.getIdentifiers().getAll()) {
				Port port = new Port();
				port.comma = (ports.isEmpty() ? "" : ", ");
				if (portDefinition.getDirection() instanceof PortDirection_Input) {
					port.direction = "input";
				} else if (portDefinition.getDirection() instanceof PortDirection_Output) {
					port.direction = "output";
				} else if (portDefinition.getDirection() instanceof PortDirection_Inout) {
					port.direction = "inout";
				} else {
					throw new RuntimeException("unknown port direction: " + portDefinition.getDirection());
				}
				if (portDefinition.getDataType() instanceof DataType_Bit) {
					port.dataType = "";
				} else if (portDefinition.getDataType() instanceof DataType_Vector) {
					port.dataType = vectorTypeRangeToString((DataType_Vector) portDefinition.getDataType());
				} else {
					throw new RuntimeException("unexpected data type for port: " + portDefinition.getDataType());
				}
				port.name = identifierElement.getText();
				ports.add(port);
			}
		}
		context.put("ports", ports);

		MahdlVelocityEngine.engine.getTemplate("verilog-module.vm").merge(context, out);
	}

	private String vectorTypeRangeToString(DataType_Vector dataType) {
		Expression sizeExpression = dataType.getSize();
		// TODO evaluate!
		if (sizeExpression instanceof Expression_Literal) {
			Literal literal = ((Expression_Literal) sizeExpression).getLiteral();
			if (literal instanceof Literal_Integer) {
				int value = Integer.parseInt(((Literal_Integer) literal).getValue().getText());
				return "[" + (value - 1) + ":0]";
			}
		}
		return "[(" + expressionToString(sizeExpression) + ")-1:0]";
	}

	private String expressionToString(Expression expression) {
		return "TODO"; // TODO
	}

	public static final class Port {

		public String comma;
		public String direction;
		public String dataType;
		public String name;

		public String getComma() {
			return comma;
		}

		public String getDirection() {
			return direction;
		}

		public String getDataType() {
			return dataType;
		}

		public String getName() {
			return name;
		}

	}

}
