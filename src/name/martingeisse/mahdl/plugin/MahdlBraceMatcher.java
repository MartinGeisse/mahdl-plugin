/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import name.martingeisse.mahdl.plugin.input.Symbols;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class MahdlBraceMatcher implements PairedBraceMatcher {

	@NotNull
	@Override
	public BracePair[] getPairs() {
		return new BracePair[]{
			new BracePair(Symbols.OPENING_CURLY_BRACE, Symbols.CLOSING_CURLY_BRACE, true),
			new BracePair(Symbols.OPENING_SQUARE_BRACKET, Symbols.CLOSING_SQUARE_BRACKET, false),
			new BracePair(Symbols.OPENING_PARENTHESIS, Symbols.CLOSING_PARENTHESIS, false),
		};
	}

	@Override
	public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType iElementType, @Nullable IElementType iElementType1) {
		return true;
	}

	// note: IntelliJ docs claim that this is only called for structural braces
	@Override
	public int getCodeConstructStart(@Nullable PsiFile file, int openingBraceOffset) {
		// This method is only called when clicking on a closing brace of a block longer than one screen. In such a
		// case, IntelliJ first determines the corresponding opening brace, then calls this method to get the starting
		// line of the code construct (which may start even before the opening brace). Not too important for now.
		return openingBraceOffset;
	}

}
