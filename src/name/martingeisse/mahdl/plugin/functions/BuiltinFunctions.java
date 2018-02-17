/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class BuiltinFunctions {

	// prevent instantiation
	private BuiltinFunctions() {
	}

	public static final ImmutableMap<String, BuiltinFunction> FUNCTIONS;

	static {
		List<BuiltinFunction> functions = new ArrayList<>();
		// TODO

		ImmutableMap.Builder<String, BuiltinFunction> builder = ImmutableMap.builder();
		for (BuiltinFunction function : functions) {
			builder.put(function.getName(), function);
		}
		FUNCTIONS = builder.build();
	}

}
