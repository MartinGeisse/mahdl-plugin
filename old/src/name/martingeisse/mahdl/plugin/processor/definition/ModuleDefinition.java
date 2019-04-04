/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedDoBlock;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ModuleDefinition {

	private final boolean isNative;

	@NotNull
	private final String name;

	@NotNull
	private final ImmutableMap<String, Named> definitions;

	@NotNull
	private final ImmutableList<ProcessedDoBlock> doBlocks;

	public ModuleDefinition(boolean isNative, @NotNull String name, @NotNull ImmutableMap<String, Named> definitions, @NotNull ImmutableList<ProcessedDoBlock> doBlocks) {
		this.isNative = isNative;
		this.name = name;
		this.definitions = definitions;
		this.doBlocks = doBlocks;
	}

	public boolean isNative() {
		return isNative;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@NotNull
	public ImmutableMap<String, Named> getDefinitions() {
		return definitions;
	}

	@NotNull
	public ImmutableList<ProcessedDoBlock> getDoBlocks() {
		return doBlocks;
	}

}
