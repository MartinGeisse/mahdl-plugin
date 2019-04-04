/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input;

import com.intellij.psi.tree.TokenSet;

/**
 *
 */
public final class NonterminalGroups {

	// prevent instantiation
	private NonterminalGroups() {
	}

	public static final TokenSet STATEMENTS = TokenSet.create(
		Symbols.statement_Assignment,
		Symbols.statement_Block,
		Symbols.statement_IfThen,
		Symbols.statement_IfThenElse,
		Symbols.statement_Switch
	);

}
