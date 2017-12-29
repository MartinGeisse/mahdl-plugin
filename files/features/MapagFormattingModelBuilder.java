package name.martingeisse.verilog.plugin.input;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.ContainerUtil;
import name.martingeisse.mapag.input.Symbols;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *
 */
public class MapagFormattingModelBuilder implements FormattingModelBuilder {

	// comments are not indented relative to their parent, which is usually indented itself
	private static final TokenSet COMMENT_SYMBOLS = TokenSet.create(Symbols.LINE_COMMENT, Symbols.BLOCK_COMMENT);

	// whitespace is needed for the workaround for comments
	private static final TokenSet WHITESPACE_SYMBOLS = TokenSet.create(TokenType.WHITE_SPACE);
	private static final TokenSet WHITESPACE_AND_COMMENT_SYMBOLS = TokenSet.orSet(WHITESPACE_SYMBOLS, COMMENT_SYMBOLS);

	private static final TokenSet DEFAULT_INDENTED_SYMBOLS = TokenSet.create(

		// terminal list inside a precedence declaration
		Symbols.precedenceDeclaration_Normal_Terminals_List,
		Symbols.precedenceDeclarationSymbol

	);

	private static final TokenSet NORMALLY_INDENTED_SYMBOLS = TokenSet.create(
		Symbols.grammar_TerminalDeclarations_Identifiers_List,
		Symbols.grammar_PrecedenceTable_PrecedenceDeclarations_List,
		Symbols.production_Multi_Alternatives_List,
		Symbols.alternativeAttribute_ResolveBlock_ResolveDeclarations_List
	);

	private static final TokenSet NOT_INDENTED_SYMBOLS = TokenSet.create(

		// relative to its parent, a closing brace is never indented
		Symbols.CLOSING_CURLY_BRACE,

		// list elements for which the list is already indented normally
		Symbols.terminalDeclaration,
		Symbols.precedenceDeclaration_Normal,
		Symbols.precedenceDeclaration_ErrorWithSemicolon,
		Symbols.precedenceDeclaration_ErrorWithoutSemicolon,
		Symbols.production_Multi_Alternatives_Named,
		Symbols.production_Multi_Alternatives_Named_Named,
		Symbols.production_Multi_Alternatives_Unnamed_Unnamed,
		Symbols.resolveDeclaration,

		// top-level elements
		Symbols.grammar_TerminalDeclarations,
		Symbols.grammar_PrecedenceTable,
		Symbols.grammar_Productions_List,
		Symbols.KW_START,
		Symbols.production_SingleUnnamed,
		Symbols.production_SingleNamed,
		Symbols.production_Multi,
		Symbols.production_ErrorWithNonterminalNameWithSemicolon,
		Symbols.production_ErrorWithNonterminalNameWithClosingCurlyBrace,
		Symbols.production_ErrorWithNonterminalNameAtEof,
		Symbols.production_ErrorWithoutNonterminalNameWithSemicolon,
		Symbols.production_ErrorWithoutNonterminalNameWithClosingCurlyBrace,
		Symbols.production_ErrorWithoutNonterminalNameAtEof

	);

	private static final TokenSet POSTFIX_OPERATORS = TokenSet.create(Symbols.ASTERISK, Symbols.PLUS, Symbols.QUESTION_MARK);

	private static final TokenSet INFIX_OPERATORS = TokenSet.create(Symbols.BAR);


	@NotNull
	@Override
	public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
		MyBlock block = new MyBlock(element.getNode(), null);
		return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
	}

	private void dump(MyBlock block, int dumpIndent) {
		for (int i = 0; i < dumpIndent; i++) {
			System.out.print("  ");
		}
		System.out.println(block.getNode().getElementType() + " -- indent: " + block.getIndent());
		for (Block child : block.getSubBlocks()) {
			dump((MyBlock) child, dumpIndent + 1);
		}
	}

	@Nullable
	@Override
	public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
		return null;
	}

	public static class MyBlock extends AbstractBlock {

		public MyBlock(@NotNull ASTNode node, @Nullable Wrap wrap) {
			super(node, wrap, null);
		}

		@Override
		protected List<Block> buildChildren() {
			return ContainerUtil.mapNotNull(myNode.getChildren(null), node -> {
				if (node.getElementType() == TokenType.WHITE_SPACE || node.getTextLength() == 0) {
					return null;
				} else {
					return buildChild(node);
				}
			});
		}

		private Block buildChild(ASTNode childNode) {
			return new MyBlock(childNode, null);
		}

		@Override
		public Indent getIndent() {
			IElementType type = myNode.getElementType();
			if (COMMENT_SYMBOLS.contains(type)) {
				// Comment symbols are not handled by the parser, but rather implicitly attached to the AST produced
				// by the parser. This sometimes produces a wrong result, especially for comments in an AST list node:
				// in such a case, a comment before the first list node is wrongly attached outside of the list, and
				// thus not indented. We solve this by looking fir the first non-comment token to see if it is
				// normally indented.
				ASTNode node = getNode();
				while (node != null && WHITESPACE_AND_COMMENT_SYMBOLS.contains(node.getElementType())) {
					node = node.getTreeNext();
				}
				if (node != null && NORMALLY_INDENTED_SYMBOLS.contains(node.getElementType())) {
					return Indent.getNormalIndent();
				}
				return Indent.getNoneIndent();
			}
			if (DEFAULT_INDENTED_SYMBOLS.contains(type)) {
				return null;
			}
			if (NORMALLY_INDENTED_SYMBOLS.contains(type)) {
				return Indent.getNormalIndent();
			}
			if (NOT_INDENTED_SYMBOLS.contains(type)) {
				return Indent.getNoneIndent();
			}
			return null;
		}

		private boolean isInside(ASTNode node, IElementType type) {
			while (node != null) {
				if (node.getElementType() == type) {
					return true;
				}
				node = node.getTreeParent();
			}
			return false;
		}

		@Nullable
		@Override
		public Spacing getSpacing(@Nullable Block block1, @NotNull Block block2) {
			ASTNode node1 = (block1 instanceof MyBlock) ? ((MyBlock) block1).getNode() : null;
			IElementType type1 = node1 != null ? node1.getElementType() : null;
			ASTNode node2 = (block2 instanceof MyBlock) ? ((MyBlock) block2).getNode() : null;
			IElementType type2 = node2 != null ? node2.getElementType() : null;

			if (type1 == Symbols.COMMA || POSTFIX_OPERATORS.contains(type1)) {
				return Spacing.createSpacing(1, 1, 0, true, 2);
			}
			if (type2 == Symbols.COMMA || POSTFIX_OPERATORS.contains(type2)) {
				return Spacing.createSpacing(0, 0, 0, false, 0);
			}
			if (type1 == Symbols.EXPANDS_TO || type2 == Symbols.EXPANDS_TO) {
				return Spacing.createSpacing(1, 1, 0, false, 0);
			}
			if (INFIX_OPERATORS.contains(type1) || INFIX_OPERATORS.contains(type2)) {
				return Spacing.createSpacing(1, 1, 0, true, 1);
			}
			if (node1 != null && node2 != null && node1.getTreeParent() != null && node1.getTreeParent() == node2.getTreeParent()) {
				if (node1.getTreeParent().getElementType() == Symbols.expression_Sequence) {
					return Spacing.createSpacing(1, 1, 0, true, 1);
				}
			}

			return null;
		}

		@Override
		public boolean isLeaf() {
			return myNode.getFirstChildNode() == null;
		}

	}
}
