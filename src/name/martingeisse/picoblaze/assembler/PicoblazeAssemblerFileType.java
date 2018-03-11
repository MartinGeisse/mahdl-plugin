/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 *
 */
public class PicoblazeAssemblerFileType extends LanguageFileType {

	@NonNls
	public static final String DEFAULT_EXTENSION = "psm";

	public static final PicoblazeAssemblerFileType INSTANCE = new PicoblazeAssemblerFileType();

	public PicoblazeAssemblerFileType() {
		super(PicoblazeAssemblerLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public String getName() {
		return "PICOBLAZE";
	}

	@NotNull
	@Override
	public String getDescription() {
		return "Picoblaze Assembler File";
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return DEFAULT_EXTENSION;
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return AllIcons.FileTypes.Text;
	}
}
