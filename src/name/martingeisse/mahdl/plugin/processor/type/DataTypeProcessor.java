/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.type;

import name.martingeisse.mahdl.plugin.input.psi.DataType;
import org.jetbrains.annotations.NotNull;

public interface DataTypeProcessor {

	@NotNull
	public ProcessedDataType processDataType(@NotNull DataType dataType);

}
