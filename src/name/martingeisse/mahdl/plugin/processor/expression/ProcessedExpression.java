package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public abstract class ProcessedExpression {

	private final ProcessedDataType dataType;

	public ProcessedExpression(ProcessedDataType dataType) {
		this.dataType = dataType;
	}

	public ProcessedDataType getDataType() {
		return dataType;
	}

}
