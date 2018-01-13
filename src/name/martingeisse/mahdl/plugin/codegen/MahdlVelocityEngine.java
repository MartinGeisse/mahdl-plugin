/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.NullLogChute;

/**
 *
 */
public final class MahdlVelocityEngine {

	public static final VelocityEngine engine;
	static {
		engine = new VelocityEngine();
		engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());
		engine.setProperty(VelocityEngine.INPUT_ENCODING, "UTF-8");
		engine.setProperty(VelocityEngine.OUTPUT_ENCODING, "UTF-8");
		engine.setProperty(VelocityEngine.RESOURCE_LOADER, "classpath");
		engine.setProperty("classpath.loader.description", "classpath-based resource loader");
		engine.setProperty("classpath.resource.loader.instance", new MyResourceLoader());
		engine.setProperty("classpath.resource.loader.cache", true);
		engine.init();
	}

	// prevent instantiation
	private MahdlVelocityEngine() {
	}

}
