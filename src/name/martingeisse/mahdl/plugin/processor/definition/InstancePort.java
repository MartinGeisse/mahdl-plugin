/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class InstancePort {

	private final String name;
	private final PortDirection direction;
	private final ProcessedDataType dataType;

	public InstancePort(String name, PortDirection direction, ProcessedDataType dataType) {
		this.name = name;
		this.direction = direction;
		this.dataType = dataType;
	}

	public String getName() {
		return name;
	}

	public PortDirection getDirection() {
		return direction;
	}

	public ProcessedDataType getDataType() {
		return dataType;
	}

}
