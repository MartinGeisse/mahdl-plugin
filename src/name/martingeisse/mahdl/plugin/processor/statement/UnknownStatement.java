/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;

/**
 * This statement indicates that a processing error has occurred that makes it impossible to treat it as any specific
 * statement anymore. No further errors should be generated for this statement.
 */
public final class UnknownStatement extends ProcessedStatement {

	public UnknownStatement(PsiElement errorSource) {
		super(errorSource);
	}

}
