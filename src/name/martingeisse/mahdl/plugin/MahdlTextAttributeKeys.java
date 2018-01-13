/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlTextAttributeKeys {

	// generic token groups
	public static final TextAttributesKey[] KEYWORD = single("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
	public static final TextAttributesKey[] OPERATOR = single("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
	public static final TextAttributesKey[] IDENTIFIER = single("IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);

	// punctuation
	public static final TextAttributesKey[] PARENTHESES = single("PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
	public static final TextAttributesKey[] BRACKETS = single("BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
	public static final TextAttributesKey[] BRACES = single("BRACES", DefaultLanguageHighlighterColors.BRACES);
	public static final TextAttributesKey[] DOT = single("DOT", DefaultLanguageHighlighterColors.DOT);
	public static final TextAttributesKey[] COLON = single("COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN);
	public static final TextAttributesKey[] SEMICOLON = single("SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
	public static final TextAttributesKey[] COMMA = single("COMMA", DefaultLanguageHighlighterColors.COMMA);
	public static final TextAttributesKey[] EQUALS = single("EQUALS", DefaultLanguageHighlighterColors.OPERATION_SIGN);

	// literals
	public static final TextAttributesKey[] NUMERIC_LITERAL = single("NUMERIC_LITERAL", DefaultLanguageHighlighterColors.NUMBER);
	public static final TextAttributesKey[] TEXT_LITERAL = single("TEXT_LITERAL", DefaultLanguageHighlighterColors.STRING);

	// special
	public static final TextAttributesKey[] BLOCK_COMMENT = single("BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
	public static final TextAttributesKey[] LINE_COMMENT = single("LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
	public static final TextAttributesKey[] BAD_CHARACTER = new TextAttributesKey[]{HighlighterColors.BAD_CHARACTER};

	private static TextAttributesKey[] single(@NonNls @NotNull String externalName, TextAttributesKey fallbackAttributeKey) {
		return new TextAttributesKey[]{
			TextAttributesKey.createTextAttributesKey(externalName, fallbackAttributeKey)
		};
	}
}
