/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class MahdlLanguage extends Language {

	public static final MahdlLanguage INSTANCE = new MahdlLanguage();

	public MahdlLanguage() {
		super("MAHDL", "text/x-mahdl");
	}

	@NotNull
	@Override
	public String getDisplayName() {
		return "MaHDL";
	}

	@Override
	public boolean isCaseSensitive() {
		return true;
	}

}
