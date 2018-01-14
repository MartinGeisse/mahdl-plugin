/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public abstract class SignalLike extends Named {

	private final DataType dataTypeElement;
	private final ProcessedDataType processedDataType;
	private final Expression initializer;

	public SignalLike(@NotNull PsiElement nameElement,
					  @NotNull DataType dataTypeElement,
					  @NotNull ProcessedDataType processedDataType,
					  @Nullable Expression initializer) {
		super(nameElement);
		this.dataTypeElement = dataTypeElement;
		this.processedDataType = processedDataType;
		this.initializer = initializer;
	}

	@NotNull
	public DataType getDataTypeElement() {
		return dataTypeElement;
	}

	@NotNull
	public ProcessedDataType getProcessedDataType() {
		return processedDataType;
	}

	@Nullable
	public Expression getInitializer() {
		return initializer;
	}

}
