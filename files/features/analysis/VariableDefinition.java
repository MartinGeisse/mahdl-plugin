package name.martingeisse.mahdl.plugin.analysis;

/**
 *
 */
public abstract class VariableDefinition extends ModuleLocalDefinition {

	private final Range arrayRange;

	public VariableDefinition(String name, Range arrayRange) {
		super(name);
		this.arrayRange = arrayRange;
	}

	public Range getArrayRange() {
		return arrayRange;
	}

}
