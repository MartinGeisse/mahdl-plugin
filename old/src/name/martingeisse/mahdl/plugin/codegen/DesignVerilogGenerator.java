/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.processor.ModuleProcessor;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleDefinition;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.util.UserMessageException;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class DesignVerilogGenerator {

	private final Module toplevelModule;
	private final Set<Module> requestedModules = new HashSet<>();
	private final Set<String> generatedModuleNames = new HashSet<>();
	private final Set<Module> generatedModules = new HashSet<>();
	private final OutputConsumer outputConsumer;

	public DesignVerilogGenerator(Module toplevelModule, OutputConsumer outputConsumer) {
		this.toplevelModule = toplevelModule;
		this.outputConsumer = outputConsumer;
	}

	public Module getToplevelModule() {
		return toplevelModule;
	}

	public Set<Module> getGeneratedModules() {
		return generatedModules;
	}

	public void generate() throws Exception {
		requestedModules.add(toplevelModule);
		while (!requestedModules.isEmpty()) {
			Module module = requestedModules.iterator().next();
			requestedModules.remove(module);
			String name = module.getName();
			if (generatedModuleNames.contains(name)) {
				continue;
			}
			generateModule(module);
			generatedModuleNames.add(name);
			generatedModules.add(module);
		}
		generateModule(toplevelModule);
	}

	private void generateModule(Module module) throws Exception {
		ModuleProcessor moduleProcessor = new ModuleProcessor(module, (errorSource, message) -> {
			throw new UserMessageException(message);
		});
		ModuleDefinition moduleDefinition = moduleProcessor.process();
		if (moduleDefinition.isNative()) {
			return;
		}
		StringWriter writer = new StringWriter();
		ModuleVerilogGenerator.MemoryFileGenerator memoryFileGenerator = (fileName, matrix) -> {
			StringBuilder builder = new StringBuilder();
			for (int rowIndex = 0; rowIndex < matrix.getFirstSize(); rowIndex++) {
				ConstantValue.Vector row = (ConstantValue.Vector)matrix.selectIndex(rowIndex);
				builder.append(row.getHexLiteral()).append('\n');
			}
			outputConsumer.consume(fileName, builder.toString());
		};
		new ModuleVerilogGenerator(moduleDefinition, writer, memoryFileGenerator).run();
		outputConsumer.consume(ModuleNamingStrategy.getVerilogNameForMahdlName(module.getName()) + ".v", writer.toString());
		for (Named definition : moduleDefinition.getDefinitions().values()) {
			if (definition instanceof ModuleInstance) {
				ModuleInstance moduleInstance = (ModuleInstance) definition;
				requestedModules.add(moduleInstance.getModuleElement());
			}
		}
	}

	public interface OutputConsumer {
		void consume(String fileName, String contents) throws Exception;
	}


}
