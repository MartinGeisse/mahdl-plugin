package name.martingeisse.mahdl.plugin.actions;

import name.martingeisse.mahdl.plugin.util.UserMessageException;

/**
 * This exception type indicates a problem with the code generation configuration.
 */
public class ConfigurationException extends UserMessageException {

	public ConfigurationException(String message) {
		super(message);
	}

}
