package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.definition.Named;

/**
 * This interface resolves local names to definitions such as constants, signals, and so on.
 */
public interface LocalDefinitionResolver {

	Named getDefinition(String name);

}
