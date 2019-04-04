package name.martingeisse.mahdl.plugin.input;

import java.lang.Error;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

%%

%class FlexGeneratedMahdlLexer
%implements FlexLexer
%public
%unicode
%function advance
%type IElementType
%eof{ return;
%eof}

// whitespace
Whitespace = [\ \n\r\t\f]

// Comments. Note: do NOT make the newline a part of the LineComment -- it will confuse the auto-formatter. The
// "longest match" algorithm will eat everything before the newline even without specifying it explicitly.
//
// Also, the IDE commenter wants an unterminated block comment to formally be a comment too. We use a rule that
// matches a block comment without the terminating characters. If the comment is properly terminated, the normal
// block comment rule will match a longer substring and take precedence.
UnterminatedBlockComment = "/*" {CommentContent} \**
TerminatedBlockComment = "/*" {CommentContent} \*+ "/"
BlockComment = {UnterminatedBlockComment} | {TerminatedBlockComment}
LineComment = "//" [^\r\n]*
CommentContent = ( [^*] | \*+[^*/] )*

// other
PositiveInteger = ([1-9][0-9]*)
UnsignedInteger = "0" | {PositiveInteger}

%%

// whitespace
{Whitespace} { return TokenType.WHITE_SPACE; }

// parentheses, braces and brackets
\( { return Symbols.OPENING_PARENTHESIS; }
\) { return Symbols.CLOSING_PARENTHESIS; }
\[ { return Symbols.OPENING_SQUARE_BRACKET; }
\] { return Symbols.CLOSING_SQUARE_BRACKET; }
\{ { return Symbols.OPENING_CURLY_BRACE; }
\} { return Symbols.CLOSING_CURLY_BRACE; }

// other puctuation
\. { return Symbols.DOT; }
\: { return Symbols.COLON; }
\; { return Symbols.SEMICOLON; }
\, { return Symbols.COMMA; }
\= { return Symbols.EQUALS; }

// bit and vector operators
\~ { return Symbols.OP_NOT; }
\& { return Symbols.OP_AND; }
\| { return Symbols.OP_OR; }
\^ { return Symbols.OP_XOR; }
\<\< { return Symbols.OP_SHIFT_LEFT; }
\>\> { return Symbols.OP_SHIFT_RIGHT; }

// arithmetic operators
\+ { return Symbols.OP_PLUS; }
\- { return Symbols.OP_MINUS; }
\* { return Symbols.OP_TIMES; }
\/ { return Symbols.OP_DIVIDED_BY; }
\% { return Symbols.OP_REMAINDER; }

// comparison operators
\=\= { return Symbols.OP_EQUAL; }
\!\= { return Symbols.OP_NOT_EQUAL; }
\> { return Symbols.OP_GREATER_THAN; }
\>\= { return Symbols.OP_GREATER_THAN_OR_EQUAL; }
\< { return Symbols.OP_LESS_THAN; }
\<\= { return Symbols.OP_LESS_THAN_OR_EQUAL; }

// other operators
\? { return Symbols.OP_CONDITIONAL; }
\_ { return Symbols.OP_CONCAT; }

// module and interface keywords
native { return Symbols.KW_NATIVE; }
module { return Symbols.KW_MODULE; }
interface { return Symbols.KW_INTERFACE; }
in { return Symbols.KW_IN; }
out { return Symbols.KW_OUT; }

// value source kind keywords
constant { return Symbols.KW_CONSTANT; }
signal { return Symbols.KW_SIGNAL; }
register { return Symbols.KW_REGISTER; }

// data type keywords
bit { return Symbols.KW_BIT; }
vector { return Symbols.KW_VECTOR; }
matrix { return Symbols.KW_MATRIX; }
integer { return Symbols.KW_INTEGER; }
text { return Symbols.KW_TEXT; }
clock { return Symbols.KW_CLOCK; }

// statement keywords
do { return Symbols.KW_DO; }
if { return Symbols.KW_IF; }
else { return Symbols.KW_ELSE; }
switch { return Symbols.KW_SWITCH; }
case { return Symbols.KW_CASE; }
default { return Symbols.KW_DEFAULT; }

// literals
{UnsignedInteger} { return Symbols.INTEGER_LITERAL; }
{PositiveInteger}[b][01]+ { return Symbols.VECTOR_LITERAL; }
{PositiveInteger}[o][0-7]+ { return Symbols.VECTOR_LITERAL; }
{PositiveInteger}[d][0-9]+ { return Symbols.VECTOR_LITERAL; }
{PositiveInteger}[h][0-9a-fA-F]+ { return Symbols.VECTOR_LITERAL; }

// identifiers
[a-zA-Z_][a-zA-Z_0-9]* { return Symbols.IDENTIFIER; }

// comments
{BlockComment} { return Symbols.BLOCK_COMMENT; }
{LineComment} { return Symbols.LINE_COMMENT; }

// fallback for unknown characters
[^] { return TokenType.BAD_CHARACTER; }
