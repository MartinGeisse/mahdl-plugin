/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.type;

import name.martingeisse.mahdl.plugin.input.psi.DataType;

/**
 * Converts the PSI representation of data types to {@link ProcessedDataType} objects.
 */
public interface DataTypeProcessor {

	ProcessedDataType processDataType(DataType dataType);

}
