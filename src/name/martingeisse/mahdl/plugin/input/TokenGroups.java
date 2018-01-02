package name.martingeisse.mahdl.plugin.input;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;

/**
 *
 */
public final class TokenGroups {

	// prevent instantiation
	private TokenGroups() {
	}

	public static final TokenSet KEYWORDS = TokenSet.create(
		Symbols.KW_MODULE,
		Symbols.KW_INTERFACE,
		Symbols.KW_INPUT,
		Symbols.KW_OUTPUT,
		Symbols.KW_INOUT,
		Symbols.KW_CONSTANT,
		Symbols.KW_SIGNAL,
		Symbols.KW_REGISTER,
		Symbols.KW_BIT,
		Symbols.KW_VECTOR,
		Symbols.KW_MEMORY,
		Symbols.KW_INTEGER,
		Symbols.KW_TEXT,
		Symbols.KW_DO,
		Symbols.KW_IF,
		Symbols.KW_ELSE,
		Symbols.KW_SWITCH,
		Symbols.KW_CASE,
		Symbols.KW_DEFAULT,
		Symbols.KW_BREAK
	);

	public static final TokenSet OPERATORS = TokenSet.create(
		Symbols.OP_NOT,
		Symbols.OP_AND,
		Symbols.OP_OR,
		Symbols.OP_XOR,
		Symbols.OP_SHIFT_LEFT,
		Symbols.OP_SHIFT_RIGHT,
		Symbols.OP_PLUS,
		Symbols.OP_MINUS,
		Symbols.OP_TIMES,
		Symbols.OP_DIVIDED_BY,
		Symbols.OP_REMAINDER,
		Symbols.OP_EQUAL,
		Symbols.OP_NOT_EQUAL,
		Symbols.OP_GREATER_THAN,
		Symbols.OP_GREATER_THAN_OR_EQUAL,
		Symbols.OP_LESS_THAN,
		Symbols.OP_LESS_THAN_OR_EQUAL,
		Symbols.OP_MUX,
		Symbols.OP_CONCAT
	);

	public static final TokenSet PUNCTUATION = TokenSet.create(
		Symbols.CLOSING_CURLY_BRACE,
		Symbols.CLOSING_PARENTHESIS,
		Symbols.CLOSING_SQUARE_BRACKET,
		Symbols.OPENING_CURLY_BRACE,
		Symbols.OPENING_PARENTHESIS,
		Symbols.OPENING_SQUARE_BRACKET,
		Symbols.DOT,
		Symbols.COLON,
		Symbols.SEMICOLON,
		Symbols.COMMA,
		Symbols.EQUALS
	);

	public static final TokenSet LITERALS = TokenSet.create(
		Symbols.INTEGER_LITERAL,
		Symbols.VECTOR_LITERAL,
		Symbols.TEXT_LITERAL
	);

	public static final TokenSet IDENTIFIERS = TokenSet.create(
		Symbols.IDENTIFIER
	);

	public static final TokenSet COMMENTS = TokenSet.create(
		Symbols.BLOCK_COMMENT,
		Symbols.LINE_COMMENT
	);

	public static final TokenSet WHITESPACE = TokenSet.create(
		TokenType.WHITE_SPACE
	);

	public static final TokenSet BAD_CHARACTER = TokenSet.create(
		TokenType.BAD_CHARACTER
	);

	public static final TokenSet ALL_PARSER_VISIBLE = TokenSet.orSet(
		KEYWORDS,
		OPERATORS,
		PUNCTUATION,
		LITERALS,
		IDENTIFIERS,
		BAD_CHARACTER
	);

	public static final TokenSet ALL = TokenSet.orSet(
		ALL_PARSER_VISIBLE,
		WHITESPACE,
		COMMENTS
	);

}
