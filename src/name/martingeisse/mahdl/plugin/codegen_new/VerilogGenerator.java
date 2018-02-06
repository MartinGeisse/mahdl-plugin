/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen_new;

import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.io.PrintWriter;
import java.io.Writer;

/**
 *
 */
public class VerilogGenerator {

	private final ModuleDefinition module;
	private final PrintWriter out;

	public VerilogGenerator(ModuleDefinition module, Writer out) {
		this(module, new PrintWriter(out));
	}

	public VerilogGenerator(ModuleDefinition module, PrintWriter out) {
		this.module = module;
		this.out = out;
	}

	public void run() {

		// print module intro
		out.println();
		out.print("module " + module.getName() + "(");
		foreachDefinition(ModulePort.class, (port, first) -> {
			if (first) {
				out.print(", ");
			}
			out.print(port.getName());
		});
		out.println(");");

		// print port definitions
		out.println();
		foreachDefinition(ModulePort.class, (port, first) -> {
			out.print('\t');
			out.print(port.getDirection() == PortDirection.IN ? "input" : "output");
			printBitOrVectorSuffix(port.getProcessedDataType());
			out.print(' ');
			out.print(port.getName());
			out.println(';');
		});

		// print forward declarations
		out.println();
		foreachDefinition(SignalLike.class, (signalLike, first) -> {
			out.print('\t');
			if (signalLike instanceof Signal) {
				out.print("wire");
				// TODO not really -- depends on the way this is assigned. But we can always
				// extract a complex assignment so a signal is always a wire -- maybe we should do that
			} else if (signalLike instanceof Register) {
				out.print("reg");
			} else {
				return;
			}
			printBitOrVectorSuffix(signalLike.getProcessedDataType());
			out.print(' ');
			out.print(signalLike.getName());
			out.println(';');
		});

		// print continuous assignments
		out.println();
		// TODO

		// print register initializers (initial block)
		out.println();
		// TODO

		// print do-blocks
		out.println();
		// TODO

		// print module instances
		out.println();
		// TODO

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
		public void call(T definition, boolean first);
	}

	private void printBitOrVectorSuffix(ProcessedDataType processedDataType) {
		if (processedDataType instanceof ProcessedDataType.Vector) {
			ProcessedDataType.Vector vectorType = (ProcessedDataType.Vector) processedDataType;
			out.print("[" + (vectorType.getSize() - 1) + ":0]");
		} else if (!(processedDataType instanceof ProcessedDataType.Bit)) {
			throw new ModuleCannotGenerateCodeException("invalid data type: " + processedDataType);
		}
	}

}
