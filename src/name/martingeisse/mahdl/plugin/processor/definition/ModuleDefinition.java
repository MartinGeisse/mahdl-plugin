/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedDoBlock;

/**
 *
 */
public final class ModuleDefinition {

	private final String name;
	private final ImmutableMap<String, Named> definitions;
	private final ImmutableList<ProcessedDoBlock> doBlocks;

	public ModuleDefinition(String name, ImmutableMap<String, Named> definitions, ImmutableList<ProcessedDoBlock> doBlocks) {
		this.name = name;
		this.definitions = definitions;
		this.doBlocks = doBlocks;
	}

	public String getName() {
		return name;
	}

	public ImmutableMap<String, Named> getDefinitions() {
		return definitions;
	}

	public ImmutableList<ProcessedDoBlock> getDoBlocks() {
		return doBlocks;
	}

}
