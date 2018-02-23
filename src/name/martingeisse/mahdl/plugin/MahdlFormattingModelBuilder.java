/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

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
import name.martingeisse.mahdl.plugin.input.NonterminalGroups;
import name.martingeisse.mahdl.plugin.input.Symbols;
import name.martingeisse.mahdl.plugin.input.TokenGroups;
import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *
 */
public class MahdlFormattingModelBuilder implements FormattingModelBuilder {

	private static final TokenSet WHITESPACE_AND_COMMENT_SYMBOLS = TokenSet.orSet(TokenGroups.WHITESPACE, TokenGroups.COMMENTS);

	private static final TokenSet NORMALLY_INDENTED_SYMBOLS = TokenSet.create(
		Symbols.synthetic_List_PortDefinitionGroup,
		Symbols.synthetic_List_PortConnection,
		Symbols.synthetic_List_Statement,
		Symbols.synthetic_List_StatementCaseItem_Nonempty,
		Symbols.synthetic_List_ExpressionCaseItem_Nonempty
	);

	private static final TokenSet POSSIBLE_PARENTS_OF_NORMALLY_INDENTED_SYMBOLS = TokenSet.create(
		Symbols.module,
		Symbols.implementationItem_ModuleInstance,
		Symbols.statement_Block,
		Symbols.statementCaseItem_Default,
		Symbols.statementCaseItem_Value,
		Symbols.statement_Switch
	);

	private static final TokenSet NOT_INDENTED_SYMBOLS = TokenSet.orSet(NonterminalGroups.STATEMENTS, TokenSet.create(

		// relative to its parent, a closing parenthesis/bracket/brace is never indented
		Symbols.CLOSING_PARENTHESIS,
		Symbols.CLOSING_SQUARE_BRACKET,
		Symbols.CLOSING_CURLY_BRACE,

		// list elements for which the list is already indented normally
		Symbols.portDefinitionGroup,
		Symbols.portConnection,
		// all statements -- merged into the TokenSet above
		Symbols.statementCaseItem_Default,
		Symbols.expressionCaseItem_Default,
		Symbols.statementCaseItem_Value,
		Symbols.expressionCaseItem_Value,

		// top-level elements
		Symbols.module,
		Symbols.KW_INTERFACE,
		Symbols.synthetic_List_ImplementationItem,
		Symbols.implementationItem_SignalLikeDefinitionGroup,
		Symbols.KW_CONSTANT,
		Symbols.KW_SIGNAL,
		Symbols.KW_REGISTER,
		Symbols.implementationItem_DoBlock,
		Symbols.KW_DO,
		Symbols.implementationItem_ModuleInstance

	));

	private static final TokenSet POSTFIX_OPERATORS = TokenSet.create(
//		Symbols.ASTERISK, Symbols.PLUS, Symbols.QUESTION_MARK
	);

	private static final TokenSet INFIX_OPERATORS = TokenSet.create(
//		Symbols.BAR
	);

	@NotNull
	@Override
	public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
		MyBlock block = new MyBlock(element.getNode(), null);
		return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
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

		public void dump(int dumpIndent) {
			for (int i = 0; i < dumpIndent; i++) {
				System.out.print("  ");
			}
			System.out.println(getNode().getElementType() + " -- indent: " + getIndent());
			for (Block child : getSubBlocks()) {
				((MyBlock) child).dump(dumpIndent + 1);
			}
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
			if (myNode.getTreeParent() == null) {
				return Indent.getNoneIndent();
			}
			IElementType type = myNode.getElementType();
			IElementType parentType = myNode.getTreeParent().getElementType();

			if (TokenGroups.WHITESPACE.contains(type)) {
				return Indent.getNoneIndent();
			}

			if (TokenGroups.COMMENTS.contains(type)) {
				// Comment symbols are not handled by the parser, but rather implicitly attached to the AST produced
				// by the parser. This sometimes produces a wrong result, especially for comments in an AST list node:
				// in such a case, a comment before the first list node is wrongly attached outside of the list, and
				// thus not indented. We solve this by looking for the first non-comment token to see if it is
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
//			if (type1 == Symbols.EXPANDS_TO || type2 == Symbols.EXPANDS_TO) {
//				return Spacing.createSpacing(1, 1, 0, false, 0);
//			}
			if (INFIX_OPERATORS.contains(type1) || INFIX_OPERATORS.contains(type2)) {
				return Spacing.createSpacing(1, 1, 0, true, 1);
			}
//			if (node1 != null && node2 != null && node1.getTreeParent() != null && node1.getTreeParent() == node2.getTreeParent()) {
//				if (node1.getTreeParent().getElementType() == Symbols.expression_Sequence) {
//					return Spacing.createSpacing(1, 1, 0, true, 1);
//				}
//			}

			return null;
		}

		@Override
		public boolean isLeaf() {
			return myNode.getFirstChildNode() == null;
		}

		@NotNull
		@Override
		public ChildAttributes getChildAttributes(int newChildIndex) {
			IElementType type = myNode.getElementType();
			if (!POSSIBLE_PARENTS_OF_NORMALLY_INDENTED_SYMBOLS.contains(type)) {
				return new ChildAttributes(Indent.getNoneIndent(), null);
			}
			List<ASTNode> children = ContainerUtil.mapNotNull(myNode.getChildren(null), node -> {
				if (node.getElementType() == TokenType.WHITE_SPACE || node.getTextLength() == 0) {
					return null;
				} else {
					return node;
				}
			});
			if (newChildIndex >= children.size()) {
				return new ChildAttributes(Indent.getNoneIndent(), null);
			}
			if (type == Symbols.module) {
				return getChildAttributes(newChildIndex, children, Symbols.OPENING_CURLY_BRACE, Symbols.CLOSING_CURLY_BRACE);
			} else if (type == Symbols.implementationItem_ModuleInstance) {
				// TODO switch to braces when the syntax is changed
				return getChildAttributes(newChildIndex, children, Symbols.OPENING_PARENTHESIS, Symbols.CLOSING_PARENTHESIS);
			} else if (type == Symbols.statement_Switch) {
				return getChildAttributes(newChildIndex, children, Symbols.OPENING_CURLY_BRACE, Symbols.CLOSING_CURLY_BRACE);
			} else if (type == Symbols.statementCaseItem_Value || type == Symbols.statementCaseItem_Default) {
				return getChildAttributes(newChildIndex, children, Symbols.COLON, null);
			} else {
				return new ChildAttributes(Indent.getNormalIndent(), null);
			}
		}

		private ChildAttributes getChildAttributes(int newChildIndex, List<ASTNode> children, IElementType startType, IElementType stopType) {
			boolean indented = false;
			for (int i = 0; i < newChildIndex; i++) {
				IElementType childType = children.get(i).getElementType();
				if (childType == startType) {
					indented = true;
				} else if (childType == stopType) {
					indented = false;
				}
			}
			return new ChildAttributes(indented ? Indent.getNormalIndent() : Indent.getNoneIndent(), null);
		}

	}
}
