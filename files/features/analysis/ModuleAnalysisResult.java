package name.martingeisse.mahdl.plugin.analysis;

import com.google.common.collect.ImmutableMap;

/**
 *
 */
public final class ModuleAnalysisResult {

	private ImmutableMap<String, PortDirection> portDirections;
	private ImmutableMap<String, ModuleLocalDefinition> localDefinitions;

	public ModuleAnalysisResult(ImmutableMap<String, PortDirection> portDirections, ImmutableMap<String, ModuleLocalDefinition> localDefinitions) {
		this.portDirections = portDirections;
		this.localDefinitions = localDefinitions;
	}

	public ImmutableMap<String, PortDirection> getPortDirections() {
		return portDirections;
	}

	public void setPortDirections(ImmutableMap<String, PortDirection> portDirections) {
		this.portDirections = portDirections;
	}

	public ImmutableMap<String, ModuleLocalDefinition> getLocalDefinitions() {
		return localDefinitions;
	}

	public void setLocalDefinitions(ImmutableMap<String, ModuleLocalDefinition> localDefinitions) {
		this.localDefinitions = localDefinitions;
	}

}
