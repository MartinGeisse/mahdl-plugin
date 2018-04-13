/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedSwitchExpression;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedDoBlock;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedSwitchStatement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class ModuleVerilogGenerator {

	private final ModuleDefinition module;
	private final PrintWriter out;
	private final ExpressionVerilogGenerator expressionVerilogGenerator;
	private final VariableVerilogGenerator variableVerilogGenerator;
	private final StatementVerilogGenerator statementVerilogGenerator;
	private int helperSignalNameGenerationCounter = 0;
	private final Map<ConstantValue.Matrix, String> romContentsToName = new HashMap<>();
	private int memoryFileGenerationCounter = 0;
	private final MemoryFileGenerator memoryFileGenerator;

	public ModuleVerilogGenerator(ModuleDefinition module, Writer out, MemoryFileGenerator memoryFileGenerator) {
		this(module, new PrintWriter(out), memoryFileGenerator);
	}

	public ModuleVerilogGenerator(ModuleDefinition module, PrintWriter out, MemoryFileGenerator memoryFileGenerator) {
		this.module = module;
		this.out = out;
		this.expressionVerilogGenerator = new ExpressionVerilogGenerator(this::extractExpression, this::extractRom);
		this.variableVerilogGenerator = new VariableVerilogGenerator(expressionVerilogGenerator);
		this.statementVerilogGenerator = new StatementVerilogGenerator(expressionVerilogGenerator, variableVerilogGenerator);
		this.memoryFileGenerator = memoryFileGenerator;
	}

	public void run() {

		// print module intro
		out.println();
		out.print("module " + ModuleNamingStrategy.getVerilogNameForMahdlName(module.getName()) + "(");
		foreachDefinition(ModulePort.class, (port, first) -> {
			if (!first) {
				out.print(", ");
			}
			out.print(port.getName());
		});
		out.println(");");

		// print port definitions
		out.println();
		foreachDefinition(ModulePort.class, (port, first) -> {
			out.print('\t');
			// output ports are always assigned to in always-blocks, so they are Verilog "regs"
			out.print(port.getDirection() == PortDirection.IN ? "input" : "output reg");
			out.print(bitOrVectorSuffixToString(port.getProcessedDataType()));
			out.print(' ');
			out.print(port.getName());
			out.println(';');
		});

		// print forward declarations
		out.println();
		foreachDefinition(SignalLike.class, (signalLike, first) -> {
			if (signalLike instanceof Signal) {
				if (signalLike.getProcessedDataType() instanceof ProcessedDataType.Matrix) {
					out.print("\treg");
				} else if (signalLike.getInitializer() == null) {
					out.print("\treg");
				} else {
					out.print("\twire");
				}
			} else if (signalLike instanceof Register) {
				if (signalLike.getProcessedDataType() instanceof ProcessedDataType.Matrix) {
					ProcessedDataType.Matrix type = (ProcessedDataType.Matrix)signalLike.getProcessedDataType();
					out.print("reg[" + (type.getSecondSize() - 1) + ":0] " + signalLike.getName() + "[" + (type.getFirstSize() - 1) + ":0];");
					return;
				} else {
					out.print("\treg");
				}
			} else {
				return;
			}
			out.print(bitOrVectorSuffixToString(signalLike.getProcessedDataType()));
			out.print(' ');
			out.print(signalLike.getName());
			out.println(';');
		});

		// print continuous assignments from signal initializers
		out.println();
		foreachDefinition(Signal.class, (signal, first) -> {
			if (signal.getInitializer() != null) {
				// note: the ModuleProcessor ensures that signals don't have matrix type
				StringBuilder builder = new StringBuilder();
				builder.append("\tassign ");
				builder.append(signal.getName());
				builder.append(" = ");
				expressionVerilogGenerator.generate(signal.getProcessedInitializer(), builder);
				builder.append(';');
				out.println(builder);
			}
		});

		// print register initializers (initial blocks)
		{
			StringBuilder builder = new StringBuilder();
			foreachDefinition(Register.class, (register, first) -> {
				if (register.getInitializer() != null) {
					if (register.getProcessedDataType() instanceof ProcessedDataType.Matrix) {
						ConstantValue.Matrix value = (ConstantValue.Matrix) register.getInitializerValue();
						initializeMatrix(register.getName(), value);
					} else {
						builder.append("\t\t");
						builder.append(register.getName());
						builder.append(" <= ");
						expressionVerilogGenerator.generate(register.getInitializerValue(), builder);
						builder.append(";\n");
					}
				}
			});
			out.println();
			out.println("\tinitial begin");
			out.print(builder);
			out.println("\tend");
		}

		// print do-blocks
		out.println();
		{
			StringBuilder builder = new StringBuilder();
			for (ProcessedDoBlock doBlock : module.getDoBlocks()) {
				builder.append('\n');
				statementVerilogGenerator.generate(doBlock, builder);
			}
			out.println(builder);
		}

		// print module instances
		out.println();
		{
			StringBuilder builder = new StringBuilder();
			foreachDefinition(ModuleInstance.class, (instance, firstModule) -> {
				builder.append('\t');
				builder.append(ModuleNamingStrategy.getVerilogNameForMahdlName(instance.getModuleElement().getName()));
				builder.append(' ');
				builder.append(instance.getName());
				builder.append("(");
				// TODO remove if all kinds of ports work the way they are generated now
//				boolean firstPort = true;
//				for (PortConnection portConnection : instance.getPortConnections().values()) {
//					if (firstPort) {
//						firstPort = false;
//					} else {
//						builder.append(',');
//					}
//					builder.append('\n');
//					builder.append("\t\t.");
//					builder.append(portConnection.getPort().getName());
//					builder.append('(');
//					if (portConnection.getPort().getDirection() == PortDirection.IN) {
//						expressionVerilogGenerator.generate(portConnection.getProcessedExpression(), builder, ExpressionVerilogGenerator.NESTING_TOPLEVEL);
//					} else {
//						variableVerilogGenerator.generate(portConnection.getProcessedExpression(), builder);
//					}
//					builder.append(')');
//				}
				builder.append("\n\t);\n");
			});
			out.println(builder);
		}

		out.println("endmodule");
	}

	// A switch expression will always be extracted, even when it could be turned into a switch statement in-place
	// because it is already a toplevel expression, but for generated code it's okay for now.
	private String extractExpression(ProcessedExpression expression) {
		if (expression.getDataType() instanceof ProcessedDataType.Matrix) {
			// note: selecting a bit or vector from a matrix is allowed at runtime and won't call this method in the
			// first place since the matrix appears as a SignalLikeReference, which won't be extracted
			throw new ModuleCannotGenerateCodeException("cannot handle matrix-valued expression at runtime: " + expression);
		}
		String name = getNextHelperSignalName();
		StringBuilder builder = new StringBuilder();
		if (expression instanceof ProcessedSwitchExpression) {

			ProcessedExpression temporarySignal = new SyntheticSignalLikeExpression(expression.getErrorSource(),
				expression.getDataType(), name);
			ProcessedSwitchExpression switchExpression = (ProcessedSwitchExpression) expression;
			ProcessedSwitchStatement switchStatement = switchExpression.convertToStatement(temporarySignal);

			builder.append("\treg").append(bitOrVectorSuffixToString(expression.getDataType()));
			builder.append(' ').append(name).append(";\n");
			builder.append("\talways @(*) begin\n");
			statementVerilogGenerator.generateSwitch(switchStatement, builder, 2);
			builder.append("\tend\n");

		} else {

			builder.append("\twire").append(bitOrVectorSuffixToString(expression.getDataType()));
			builder.append(' ').append(name).append(" = ");
			expressionVerilogGenerator.generate(expression, builder);
			builder.append(";\n");

		}
		out.println(builder);
		return name;
	}

	private String extractRom(ConstantValue.Matrix value) {
		String existingRomName = romContentsToName.get(value);
		if (existingRomName != null) {
			return existingRomName;
		}
		String romName = "anonymous_rom_" + memoryFileGenerationCounter;
		out.print("reg[" + (value.getSecondSize() - 1) + ":0] " + romName + "[" + (value.getFirstSize() - 1) + ":0];");
		initializeMatrix(romName, value);
		romContentsToName.put(value, romName);
		return romName;
	}

	private void initializeMatrix(String destinationName, ConstantValue.Matrix matrixValue) {
		String filename = ModuleNamingStrategy.getVerilogNameForMahdlName(module.getName()) + memoryFileGenerationCounter + ".mif";
		try {
			memoryFileGenerator.generateMemoryFile(filename, matrixValue);
		} catch (Exception e) {
			throw new ModuleCannotGenerateCodeException("could not generate memory file '" + filename + "': " + e.toString());
		}
		out.println("initial $readmemh(\"" + filename + "\", " + destinationName + ", 0, " +
			(matrixValue.getFirstSize() - 1) + ");");
		memoryFileGenerationCounter++;
	}

	//
	// helpers
	//

	private <T extends Named> void foreachDefinition(Class<T> type, DefinitionCallback<T> callback) {
		boolean first = true;
		for (Named definition : module.getDefinitions().values()) {
			if (type.isInstance(definition)) {
				callback.call(type.cast(definition), first);
				first = false;
			}
		}
	}

	private interface DefinitionCallback<T extends Named> {
		void call(T definition, boolean first);
	}

	private static String bitOrVectorSuffixToString(ProcessedDataType processedDataType) {
		if (processedDataType instanceof ProcessedDataType.Bit || processedDataType instanceof ProcessedDataType.Clock) {
			return "";
		} else if (processedDataType instanceof ProcessedDataType.Vector) {
			ProcessedDataType.Vector vectorType = (ProcessedDataType.Vector) processedDataType;
			return ("[" + (vectorType.getSize() - 1) + ":0]");
		} else {
			throw new ModuleCannotGenerateCodeException("invalid data type: " + processedDataType);
		}
	}

	private String getNextHelperSignalName() {
		while (true) {
			String name = "s" + helperSignalNameGenerationCounter;
			helperSignalNameGenerationCounter++;
			if (module.getDefinitions().get(name) == null) {
				return name;
			}
		}
	}

	//
	// injected services
	//

	public interface MemoryFileGenerator {
		void generateMemoryFile(String filename, ConstantValue.Matrix matrix) throws Exception;
	}

}
