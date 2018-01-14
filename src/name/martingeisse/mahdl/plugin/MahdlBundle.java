/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

/**
 *
 */
public class MahdlBundle extends AbstractBundle {

	public static final String BUNDLE = "name.martingeisse.mahdl.plugin.MahdlBundle";

	private static final MahdlBundle INSTANCE = new MahdlBundle();

	@Nullable
	public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
		return INSTANCE.getMessage(key, params);
	}

	private MahdlBundle() {
		super(BUNDLE);
	}

}
