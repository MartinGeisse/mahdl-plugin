/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.FileContentUtil;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class PsiUtil {

	// prevent instantiation
	private PsiUtil() {
	}

	//
	// general
	//

	@NotNull
	public static PsiElement setText(@NotNull LeafPsiElement element, @NotNull String newText) {
		return (PsiElement) element.replaceWithText(newText);
	}

	@Nullable
	public static <T> T getAncestor(@NotNull PsiElement element, @NotNull Class<T> nodeClass) {
		while (true) {
			if (nodeClass.isInstance(element)) {
				return nodeClass.cast(element);
			}
			if (element == null || element instanceof PsiFile) {
				return null;
			}
			element = element.getParent();
		}
	}

	public static void foreachPsiNode(@NotNull PsiElement root, @NotNull Consumer<PsiElement> consumer) {
		if (root instanceof ASTWrapperPsiElement) {
			InternalPsiUtil.foreachChild((ASTWrapperPsiElement) root, child -> {
				consumer.consume(child);
				foreachPsiNode(child, consumer);
			});
		}
	}

	@Nullable
	public static VirtualFile getSourceRoot(@NotNull PsiElement psiElement) {
		PsiFile originPsiFile = psiElement.getContainingFile();
		if (originPsiFile == null) {
			return null;
		}
		VirtualFile originVirtualFile = originPsiFile.getVirtualFile();
		if (originVirtualFile == null) {
			return null;
		}
		return ProjectRootManager.getInstance(originPsiFile.getProject()).getFileIndex().getSourceRootForFile(originVirtualFile);
	}

	@NotNull
	public static VirtualFile resolveModuleNameToVirtualFile(QualifiedModuleName moduleName) throws ReferenceResolutionException {
		VirtualFile sourceRoot = getSourceRoot(moduleName);
		if (sourceRoot == null) {
			throw new ReferenceResolutionException("the module name is not located inside a source root");
		}
		String[] segments = parseQualifiedModuleName(moduleName);
		VirtualFile targetVirtualFile = sourceRoot;
		for (int i = 0; i < segments.length - 1; i++) {
			targetVirtualFile = targetVirtualFile.findChild(segments[i]);
			if (targetVirtualFile == null) {
				String path = sourceRoot.getPath() + '/' + StringUtils.join(segments, '/') + ".mahdl";
				throw new ReferenceResolutionException("could not locate module file " + path + ": folder " + segments[i] + " not found");
			}
		}
		VirtualFile file = targetVirtualFile.findChild(segments[segments.length - 1] + ".mahdl");
		if (file == null) {
			String path = sourceRoot.getPath() + '/' + StringUtils.join(segments, '/') + ".mahdl";
			throw new ReferenceResolutionException("module file " + path + " not found");
		}
		return file;
	}

	@NotNull
	public static Module resolveModuleName(QualifiedModuleName moduleName) throws ReferenceResolutionException {
		VirtualFile targetVirtualFile = resolveModuleNameToVirtualFile(moduleName);
		PsiFile targetPsiFile = PsiManager.getInstance(moduleName.getProject()).findFile(targetVirtualFile);
		if (!(targetPsiFile instanceof MahdlSourceFile)) {
			throw new ReferenceResolutionException(targetVirtualFile.getPath() + " is not a MaHDL source file");
		}
		Module module = ((MahdlSourceFile) targetPsiFile).getModule();
		if (module == null) {
			throw new ReferenceResolutionException("target file does not contain a module");
		}
		return module;
	}

	//
	// naming support
	//

	@Nullable
	public static QualifiedModuleName getNameIdentifier(@NotNull Module node) {
		return node.getModuleName();
	}

	@Nullable
	public static String getName(@NotNull Module node) {
		QualifiedModuleName name = node.getModuleName();
		return name == null ? null : canonicalizeQualifiedModuleName(name);
	}

	public static PsiElement setName(@NotNull Module node, @NotNull String newName) {
		throw new IncorrectOperationException("renaming module not yet implemented");
	}

	@Nullable
	public static LeafPsiElement getNameIdentifier(@NotNull PortDefinition node) {
		return node.getIdentifier();
	}

	@Nullable
	public static LeafPsiElement getNameIdentifier(@NotNull SignalLikeDefinition node) {
		if (node instanceof SignalLikeDefinition_WithoutInitializer) {
			return ((SignalLikeDefinition_WithoutInitializer) node).getIdentifier();
		} else if (node instanceof SignalLikeDefinition_WithInitializer) {
			return ((SignalLikeDefinition_WithInitializer) node).getIdentifier();
		} else {
			return null;
		}
	}

	@Nullable
	public static LeafPsiElement getNameIdentifier(@NotNull ImplementationItem_ModuleInstance node) {
		return node.getInstanceName();
	}

	//
	// reference support
	//

	@NotNull
	public static PsiReference getReference(@NotNull QualifiedModuleName node) {
		return new ModuleReference(node);
	}

	@NotNull
	public static PsiReference getReference(@NotNull InstancePortName node) {
		return new ModuleInstancePortReference(node);
	}

	@NotNull
	public static PsiReference getReference(@NotNull Expression_Identifier node) {
		return new IdentifierExpressionReference(node);
	}

	@NotNull
	public static PsiReference getReference(@NotNull InstanceReferenceName node) {
		return new ModuleInstanceReference(node);
	}

	//
	// safe delete
	//

	public static void delete(@NotNull Module node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(@NotNull PortDefinition node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(@NotNull SignalLikeDefinition node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(@NotNull ImplementationItem_ModuleInstance node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(@NotNull ASTWrapperPsiElement node, @NotNull Runnable actualDeleteCallback) throws IncorrectOperationException {
		PsiFile psiFile = node.getContainingFile();
		if (psiFile != null) {
			VirtualFile virtualFile = psiFile.getVirtualFile();
			if (virtualFile != null) {
				actualDeleteCallback.run();
				FileContentUtil.reparseFiles(virtualFile);
				return;
			}
		}
		throw new IncorrectOperationException("could not determine containing virtual file to reparse after safe delete");
	}

	//
	// other
	//

	@NotNull
	public static String canonicalizeQualifiedModuleName(@NotNull QualifiedModuleName name) {
		return StringUtils.join(parseQualifiedModuleName(name), '.');
	}

	@NotNull
	public static String[] parseQualifiedModuleName(@NotNull QualifiedModuleName name) {
		List<String> segments = new ArrayList<>();
		for (LeafPsiElement segment : name.getSegments().getAll()) {
			segments.add(segment.getText());
		}
		return segments.toArray(new String[segments.size()]);
	}

}
