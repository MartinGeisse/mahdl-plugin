/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;

/**
 *
 */
public abstract class Named {

	private final PsiElement nameElement;

	public Named(PsiElement nameElement) {
		this.nameElement = nameElement;
	}

	public final PsiElement getNameElement() {
		return nameElement;
	}

	public final String getName() {
		return nameElement.getText();
	}

}
