/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.MahdlLanguage;
import name.martingeisse.mahdl.plugin.input.psi.PsiFactory;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlParserDefinition implements ParserDefinition {

	public static final IFileElementType FILE_ELEMENT_TYPE = new IFileElementType(MahdlLanguage.INSTANCE);

	private static final TokenSet STRING_LITERALS = TokenSet.create(Symbols.TEXT_LITERAL);

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		return new MahdlLexer();
	}

	@Override
	public PsiParser createParser(Project project) {
		return new MapagGeneratedMahdlParser();
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE_ELEMENT_TYPE;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenGroups.WHITESPACE;
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenGroups.COMMENTS;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return STRING_LITERALS;
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return PsiFactory.createPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new MahdlSourceFile(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}

}
