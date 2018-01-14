/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public interface BinaryOperation extends NavigatablePsiElement {

	@Nullable
	Expression getLeftOperand();

	@Nullable
	Expression getRightOperand();

}
