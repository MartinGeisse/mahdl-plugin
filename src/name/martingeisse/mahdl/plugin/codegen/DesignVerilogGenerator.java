/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.processor.ModuleProcessor;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleDefinition;
import name.martingeisse.mahdl.plugin.util.UserMessageException;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class DesignVerilogGenerator {

	private final Module toplevelModule;
	private final Set<String> requestedModuleNames = new HashSet<>();
	private final Set<String> generatedModuleNames = new HashSet<>();
	private final OutputConsumer outputConsumer;

	public DesignVerilogGenerator(Module toplevelModule, OutputConsumer outputConsumer) {
		this.toplevelModule = toplevelModule;
		this.outputConsumer = outputConsumer;
	}

	public void generate() throws Exception {
		// TODO implement multi-module designs
		generateModule(toplevelModule);
	}

	private void generateModule(Module module) throws Exception {
		ModuleProcessor moduleProcessor = new ModuleProcessor(module, (errorSource, message) -> {
			throw new UserMessageException(message);
		});
		ModuleDefinition moduleDefinition = moduleProcessor.process();
		StringWriter writer = new StringWriter();
		new ModuleVerilogGenerator(moduleDefinition, writer).run();
		outputConsumer.consume(module.getName(), writer.toString());
		// TODO add sub-modules to requestedModuleNames (or even use modules themselves, not names, since the PSI has been resolved already)
	}

	public interface OutputConsumer {
		public void consume(String moduleName, String generatedCode) throws Exception;
	}

}
