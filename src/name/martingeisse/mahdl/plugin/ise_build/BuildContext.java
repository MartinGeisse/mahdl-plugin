package name.martingeisse.mahdl.plugin.ise_build;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.input.psi.Module;

/**
 *
 */
public final class BuildContext {

	private final Module toplevelModule;
	private final ImmutableSet<Module> generatedModules;
	private final Configuration configuration;
	private final VirtualFile buildFolder;

	public BuildContext(Module toplevelModule, ImmutableSet<Module> generatedModules, Configuration configuration, VirtualFile buildFolder) {
		this.toplevelModule = toplevelModule;
		this.generatedModules = generatedModules;
		this.configuration = configuration;
		this.buildFolder = buildFolder;
	}

	public Module getToplevelModule() {
		return toplevelModule;
	}

	public ImmutableSet<Module> getGeneratedModules() {
		return generatedModules;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public VirtualFile getBuildFolder() {
		return buildFolder;
	}

}
