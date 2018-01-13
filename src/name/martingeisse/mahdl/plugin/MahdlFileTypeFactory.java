/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlFileTypeFactory extends FileTypeFactory {

	@Override
	public void createFileTypes(@NotNull final FileTypeConsumer consumer) {
		consumer.consume(MahdlFileType.INSTANCE, MahdlFileType.INSTANCE.getDefaultExtension());
	}

}
