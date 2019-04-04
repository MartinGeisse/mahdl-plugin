/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.definition.Named;

/**
 * This interface resolves local names to definitions such as constants, signals, and so on.
 */
public interface LocalDefinitionResolver {

	Named getDefinition(String name);

}
