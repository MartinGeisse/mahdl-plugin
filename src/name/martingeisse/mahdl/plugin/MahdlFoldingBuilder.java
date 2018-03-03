/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem;
import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_DoBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MahdlFoldingBuilder implements FoldingBuilder {

	@NotNull
	@Override
	public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode astNode, @NotNull Document document) {
		List<FoldingDescriptor> foldingDescriptors = new ArrayList<>();
		collectFoldingRegions(astNode.getPsi(), foldingDescriptors);
		return foldingDescriptors.toArray(new FoldingDescriptor[foldingDescriptors.size()]);
	}

	private void collectFoldingRegions(PsiElement psiElement, List<FoldingDescriptor> destination) {
		if (psiElement instanceof ImplementationItem_DoBlock) {
			destination.add(new FoldingDescriptor(psiElement.getNode(), psiElement.getTextRange()));
		} else if (!(psiElement instanceof ImplementationItem)) {
			for (PsiElement child : psiElement.getChildren()) {
				collectFoldingRegions(child, destination);
			}
		}
	}

	@Nullable
	@Override
	public String getPlaceholderText(@NotNull ASTNode astNode) {
		PsiElement psiElement = astNode.getPsi();
		if (psiElement instanceof ImplementationItem_DoBlock) {
			ImplementationItem_DoBlock doBlock = (ImplementationItem_DoBlock)psiElement;
			return "do (" + doBlock.getTrigger().getText() + ") do {...}";
		}
		return psiElement.toString();
	}

	@Override
	public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
		return false;
	}

}
