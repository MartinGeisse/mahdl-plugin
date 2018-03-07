/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.indexing.FileBasedIndex;
import name.martingeisse.mahdl.plugin.MahdlModuleIndex;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.ReferenceResolutionException;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.input.psi.PsiUtil;
import name.martingeisse.mahdl.plugin.input.psi.QualifiedModuleName;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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
		try {
			return PsiUtil.resolveModuleName(moduleName, PsiUtil.ModuleNameResolutionUseCase.REFERENCE_RESOLUTION);
		} catch (ReferenceResolutionException e) {
			return null;
		}
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
			String canonicalReferenceModuleName = PsiUtil.canonicalizeQualifiedModuleName(moduleName);
			String canonicalCandidateModuleName = PsiUtil.canonicalizeQualifiedModuleName(((Module) psiElement).getModuleName());
			if (canonicalCandidateModuleName.equals(canonicalReferenceModuleName)) {
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

		// A simple file-based index doesn't work here since it doesn't delete obsolete keys, and we can't
		// distinguish those from real ones. If performance becomes a problem, use stub trees (stub-backed PSI
		// instead of AST-backed PSI) and stub indexes.
		List<Object> variants = new ArrayList<>();
		VirtualFile sourceRoot = PsiUtil.getSourceRoot(moduleName);
		if (sourceRoot != null) {
			VfsUtilCore.visitChildrenRecursively(sourceRoot, new VirtualFileVisitor<Object>(VirtualFileVisitor.SKIP_ROOT) {
				@Override
				public boolean visitFile(@NotNull VirtualFile file) {
					String name = file.getName();
					String prefix = (String)getCurrentValue();
					if (!file.isDirectory() && name.endsWith(".mahdl")) {
						String simpleModuleName = name.substring(0, name.length() - ".mahdl".length());
						String fullModuleName = (prefix == null ? simpleModuleName : (prefix + '.' + simpleModuleName));
						variants.add(fullModuleName);
					}
					setValueForChildren(prefix == null ? name : (prefix + '.' + name));
					return (name.indexOf('.') < 0);
				}
			});
		}
		return variants.toArray();

	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
