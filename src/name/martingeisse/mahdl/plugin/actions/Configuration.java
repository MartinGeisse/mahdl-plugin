package name.martingeisse.mahdl.plugin.actions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.util.ParameterUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 *
 */
public final class Configuration {

	private final ImmutableMap<String, String> allProperties;
	private final Set<String> keysNotYetUnderstood;

	public Configuration(Properties allProperties) {
		this(convertProperties(allProperties));
	}

	private static Map<String, String> convertProperties(Properties allProperties) {
		Map<String, String> result = new HashMap<>();
		for (Map.Entry<?, ?> entry : allProperties.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return result;
	}

	public Configuration(Map<String, String> allProperties) {
		this(ImmutableMap.copyOf(allProperties));
	}

	public Configuration(ImmutableMap<String, String> allProperties) {
		ParameterUtil.ensureNotNull(allProperties, "allProperties");
		this.allProperties = allProperties;
		this.keysNotYetUnderstood = new HashSet<>(allProperties.keySet());
	}

	/**
	 * Gets a single property and marks it "understood". Returns null if not found.
	 */
	public String getOptional(String key) {
		ParameterUtil.ensureNotNull(key, "key");
		keysNotYetUnderstood.remove(key);
		return allProperties.get(key);
	}

	/**
	 * Gets a single property and marks it "understood". Throws an exception if not found.
	 */
	public String getRequired(String key) throws ConfigurationException {
		ParameterUtil.ensureNotNull(key, "key");
		String value = allProperties.get(key);
		if (value == null) {
			throw new ConfigurationException("missing code generation property: " + key);
		}
		keysNotYetUnderstood.remove(key);
		return value;
	}

	/**
	 * Gets one of a set of properties. The configuration must not contain more than one of those keys. Returns null if
	 * not found.
	 */
	public String getAtMostOne(String... keys) throws ConfigurationException {
		String value = null;
		String key1 = null;
		String key2 = null;
		for (String key : keys) {
			String currentValue = getOptional(key);
			if (currentValue != null) {
				value = currentValue;
				if (key1 == null) {
					key1 = key;
				} else {
					key2 = key; // continue anyway to mark all keys understood
				}
			}
		}
		if (key2 != null) {
			throw new ConfigurationException("cannot use configuration keys " + key1 + " and " + key2 + " together");
		}
		return value;
	}

	/**
	 * Gets one of a set of properties. The configuration must not contain more than one of those keys. Throws an
	 * exception if not found.
	 */
	public String getExactlyOne(String... keys) throws ConfigurationException {
		String value = getAtMostOne(keys);
		if (value == null) {
			throw new ConfigurationException("missing at least one of these configuration properties: " + join(keys));
		}
		return value;
	}

	/**
	 * Gets all entries starting with the specified prefix as a map and marks them "understood".
	 */
	public Map<String, String> getPrefixed(String prefix) {
		ParameterUtil.ensureNotNull(prefix, "prefix");
		Map<String, String> result = new HashMap<>();
		for (Map.Entry<String, String> entry : allProperties.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith(prefix)) {
				keysNotYetUnderstood.remove(key);
				String suffix = key.substring(prefix.length());
				result.put(suffix, entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Gets a comma-separated list of strings (trimmed), or the empty list if the property does not exist.
	 */
	public ImmutableList<String> getStringList(String name) {
		String propertyValue = getOptional(name);
		if (propertyValue == null || propertyValue.trim().isEmpty()) {
			return ImmutableList.of();
		}
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (String s : StringUtils.split(propertyValue, ',')) {
			builder.add(s.trim());
		}
		return builder.build();
	}

	/**
	 * Throws an exception if any actual configuration property was not understood.
	 */
	public void expectAllUnderstood() throws ConfigurationException {
		if (!keysNotYetUnderstood.isEmpty()) {
			throw new ConfigurationException("unknown configuration property: " + keysNotYetUnderstood);
		}
	}

	private static String join(String[] strings) {
		return StringUtils.join(strings, ", ");
	}
}
