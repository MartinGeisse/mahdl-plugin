/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

/**
 *
 */
public class ModuleNamingStrategy {

	// prevent instantiation
	private ModuleNamingStrategy() {
	}

	public static String getVerilogNameForMahdlName(String mahdlName) {
		return mahdlName.replace('.', '_');
	}

}
