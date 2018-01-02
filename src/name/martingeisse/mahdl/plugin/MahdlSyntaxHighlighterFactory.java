package name.martingeisse.mahdl.plugin;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import name.martingeisse.mahdl.plugin.input.MahdlLexer;
import name.martingeisse.mahdl.plugin.input.Symbols;
import name.martingeisse.mahdl.plugin.input.TokenGroups;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

	@Override
	@NotNull
	public SyntaxHighlighter getSyntaxHighlighter(final Project project, final VirtualFile virtualFile) {
		return new SyntaxHighlighterBase() {

			@NotNull
			@Override
			public Lexer getHighlightingLexer() {
				return new MahdlLexer();
			}

			@NotNull
			@Override
			public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
				if (TokenGroups.KEYWORDS.contains(tokenType)) {
					return MahdlTextAttributeKeys.KEYWORD;
				} else if (TokenGroups.OPERATORS.contains(tokenType)) {
					return MahdlTextAttributeKeys.OPERATOR;
				} else if (TokenGroups.IDENTIFIERS.contains(tokenType)) {
					return MahdlTextAttributeKeys.IDENTIFIER;
				} else if (tokenType == Symbols.OPENING_PARENTHESIS) {
					return MahdlTextAttributeKeys.PARENTHESES;
				} else if (tokenType == Symbols.CLOSING_PARENTHESIS) {
					return MahdlTextAttributeKeys.PARENTHESES;
				} else if (tokenType == Symbols.OPENING_SQUARE_BRACKET) {
					return MahdlTextAttributeKeys.BRACKETS;
				} else if (tokenType == Symbols.CLOSING_SQUARE_BRACKET) {
					return MahdlTextAttributeKeys.BRACKETS;
				} else if (tokenType == Symbols.OPENING_CURLY_BRACE) {
					return MahdlTextAttributeKeys.BRACES;
				} else if (tokenType == Symbols.CLOSING_CURLY_BRACE) {
					return MahdlTextAttributeKeys.BRACES;
				} else if (tokenType == Symbols.DOT) {
					return MahdlTextAttributeKeys.DOT;
				} else if (tokenType == Symbols.COLON) {
					return MahdlTextAttributeKeys.COLON;
				} else if (tokenType == Symbols.SEMICOLON) {
					return MahdlTextAttributeKeys.SEMICOLON;
				} else if (tokenType == Symbols.COMMA) {
					return MahdlTextAttributeKeys.COMMA;
				} else if (tokenType == Symbols.EQUALS) {
					return MahdlTextAttributeKeys.EQUALS;
				} else if (TokenGroups.LITERALS.contains(tokenType)) {
					return (tokenType == Symbols.TEXT_LITERAL ? MahdlTextAttributeKeys.TEXT_LITERAL : MahdlTextAttributeKeys.NUMERIC_LITERAL);
				} else if (tokenType == Symbols.BLOCK_COMMENT) {
					return MahdlTextAttributeKeys.BLOCK_COMMENT;
				} else if (tokenType == Symbols.LINE_COMMENT) {
					return MahdlTextAttributeKeys.LINE_COMMENT;
				} else if (tokenType == TokenType.BAD_CHARACTER) {
					return MahdlTextAttributeKeys.BAD_CHARACTER;
				}
				return EMPTY;
			}

		};
	}

}
