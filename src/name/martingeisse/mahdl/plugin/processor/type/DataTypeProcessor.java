package name.martingeisse.mahdl.plugin.processor.type;

import name.martingeisse.mahdl.plugin.input.psi.DataType;

/**
 * Converts the PSI representation of data types to {@link ProcessedDataType} objects.
 */
public interface DataTypeProcessor {

	public ProcessedDataType processDataType(DataType dataType);

}
