/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class PicoblazeAssemblerFileTypeFactory extends FileTypeFactory {

	@Override
	public void createFileTypes(@NotNull final FileTypeConsumer consumer) {
		consumer.consume(PicoblazeAssemblerFileType.INSTANCE, PicoblazeAssemblerFileType.INSTANCE.getDefaultExtension());
	}

}
