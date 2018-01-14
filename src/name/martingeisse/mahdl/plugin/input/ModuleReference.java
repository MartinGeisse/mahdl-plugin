/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.input.psi.PsiUtil;
import name.martingeisse.mahdl.plugin.input.psi.QualifiedModuleName;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ModuleReference implements PsiReference {

	private final QualifiedModuleName moduleName;

	public ModuleReference(@NotNull QualifiedModuleName moduleName) {
		this.moduleName = moduleName;
	}

	@Override
	@NotNull
	public PsiElement getElement() {
		return moduleName;
	}

	@Override
	@NotNull
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	@Override
	public PsiElement resolve() {

		VirtualFile sourceRoot = PsiUtil.getSourceRoot(moduleName);
		String[] segments = PsiUtil.parseQualifiedModuleName(moduleName);
		VirtualFile targetVirtualFile = sourceRoot;
		for (int i = 0; i < segments.length - 1; i++) {
			targetVirtualFile = targetVirtualFile.findChild(segments[i]);
			if (targetVirtualFile == null) {
				return null;
			}
		}
		targetVirtualFile = targetVirtualFile.findChild(segments[segments.length - 1] + ".mahdl");
		if (targetVirtualFile == null) {
			return null;
		}

		PsiFile targetPsiFile = PsiManager.getInstance(moduleName.getProject()).findFile(targetVirtualFile);
		if (!(targetPsiFile instanceof MahdlSourceFile)) {
			return null;
		}

		return ((MahdlSourceFile)targetPsiFile).getModule();
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		// this removes whitespace and comments from the module name
		String[] segments = PsiUtil.parseQualifiedModuleName(moduleName);
		return StringUtils.join(segments, '.');
	}

	@Override
	@NotNull
	public PsiElement handleElementRename(@Nullable String newName) throws IncorrectOperationException {

		// TODO have to see if the argument is a fully qualified name or a simple name. Especially
		// when the newName does not contain a dot, we need to know if it's a local rename or if the
		// module got moved to the root package (and potentially renamed).

//		if (newName.indexOf('.') < 0) {
//			ImmutableList<LeafPsiElement> segments = moduleName.getSegments().getAll();
//			LeafPsiElement lastSegment = segments
//
//			String lastSegment = PsiUtil.getSimpleModuleName(moduleName);
//			newName = StringUtils.
//		}
//		return PsiUtil.setText(expression.getIdentifier(), newName);

		throw new IncorrectOperationException("TODO");
	}

	@Override
	@NotNull
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (psiElement instanceof Module) {
			throw new IncorrectOperationException("cannot bind this reference to a non-module PSI node");
		}
		throw new IncorrectOperationException("not yet supported"); // TODO I have no idea how to manipulate the PSI that way
	}

	@Override
	public boolean isReferenceTo(@Nullable PsiElement psiElement) {
		if (psiElement instanceof Module) {
			String candidateModuleName = ((Module) psiElement).getModuleName().getText();
			String lastSegmentOfReference = PsiUtil.getSimpleModuleName(moduleName);
			if (candidateModuleName.equals(lastSegmentOfReference)) {
				PsiElement resolved = resolve();
				return (resolved != null && resolved.equals(psiElement));
			}
		}
		return false;
	}

	@NotNull
	@Override
	public Object[] getVariants() {

		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data

		// TODO this should use a file-based index!

		List<Object> variants = new ArrayList<>();
		VirtualFile sourceRoot = PsiUtil.getSourceRoot(moduleName);
		if (sourceRoot != null) {
			findModules(null, sourceRoot, variants);
		}
		return variants.toArray();
	}

	private void findModules(@Nullable String prefix, @NotNull VirtualFile folder, @NotNull List<Object> variants) {
		for (VirtualFile child : folder.getChildren()) {
			String childName = child.getName();
			if (child.isDirectory() && childName.indexOf('.') < 0) {
				String childPrefix = (prefix == null ? childName : (prefix + '.' + childName));
				findModules(prefix, child, variants);
			} else if (childName.endsWith(".mahdl")) {
				String simpleModuleName = childName.substring(0, childName.length() - ".mahdl".length());
				String fullModuleName = (prefix == null ? simpleModuleName : (prefix + '.' + simpleModuleName));
				variants.add(fullModuleName);
			}
		}
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
