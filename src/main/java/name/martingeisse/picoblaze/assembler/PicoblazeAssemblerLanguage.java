/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class PicoblazeAssemblerLanguage extends Language {

	public static final PicoblazeAssemblerLanguage INSTANCE = new PicoblazeAssemblerLanguage();

	public PicoblazeAssemblerLanguage() {
		super("PICOBLAZE", "text/x-picoblaze");
	}

	@NotNull
	@Override
	public String getDisplayName() {
		return "Picoblaze Assembler";
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}

}
