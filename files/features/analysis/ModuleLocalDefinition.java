package name.martingeisse.mahdl.plugin.analysis;

/**
 *
 */
public abstract class ModuleLocalDefinition {

	private final String name;

	public ModuleLocalDefinition(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
