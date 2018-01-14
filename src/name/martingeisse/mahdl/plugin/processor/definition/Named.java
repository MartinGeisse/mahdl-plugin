/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public abstract class Named {

	private final PsiElement nameElement;

	public Named(@NotNull PsiElement nameElement) {
		this.nameElement = nameElement;
	}

	@NotNull
	public final PsiElement getNameElement() {
		return nameElement;
	}

	@NotNull
	public final String getName() {
		return nameElement.getText();
	}

}
