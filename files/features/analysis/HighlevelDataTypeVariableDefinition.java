package name.martingeisse.mahdl.plugin.analysis;

/**
 *
 */
public final class HighlevelDataTypeVariableDefinition extends VariableDefinition {

	private final HighlevelDataType type;

	public HighlevelDataTypeVariableDefinition(String name, Range arrayRange, HighlevelDataType type) {
		super(name, arrayRange);
		this.type = type;
	}

	public HighlevelDataType getType() {
		return type;
	}

}
