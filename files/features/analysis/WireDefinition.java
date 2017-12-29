package name.martingeisse.mahdl.plugin.analysis;

/**
 *
 */
public final class WireDefinition extends ModuleLocalDefinition {

	private final Range dataRange;

	public WireDefinition(String name, Range dataRange) {
		super(name);
		this.dataRange = dataRange;
	}

	public Range getDataRange() {
		return dataRange;
	}

}
