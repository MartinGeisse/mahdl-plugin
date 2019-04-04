/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class InstancePort {

	@NotNull
	private final String name;

	@NotNull
	private final PortDirection direction;

	@NotNull
	private final ProcessedDataType dataType;

	public InstancePort(@NotNull String name, @NotNull PortDirection direction, @NotNull ProcessedDataType dataType) {
		this.name = name;
		this.direction = direction;
		this.dataType = dataType;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@NotNull
	public PortDirection getDirection() {
		return direction;
	}

	@NotNull
	public ProcessedDataType getDataType() {
		return dataType;
	}

}
