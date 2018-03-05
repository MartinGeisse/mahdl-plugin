/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
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
import name.martingeisse.mahdl.plugin.input.ReferenceResolutionException;
import name.martingeisse.mahdl.plugin.input.reference.LocalReference;
import name.martingeisse.mahdl.plugin.input.reference.ModuleInstancePortReference;
import name.martingeisse.mahdl.plugin.input.reference.ModuleReference;
import org.apache.commons.lang3.StringUtils;
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
	public static VirtualFile getVirtualFile(@NotNull PsiElement psiElement) {
		PsiFile originPsiFile = psiElement.getContainingFile();
		return (originPsiFile == null ? null : originPsiFile.getOriginalFile().getVirtualFile());
	}

	@Nullable
	public static VirtualFile getSourceRoot(@NotNull PsiElement psiElement) {
		PsiFile originPsiFile = psiElement.getContainingFile();
		if (originPsiFile == null) {
			return null;
		}
		VirtualFile originVirtualFile = originPsiFile.getOriginalFile().getVirtualFile();
		if (originVirtualFile == null) {
			return null;
		}
		return ProjectRootManager.getInstance(originPsiFile.getProject()).getFileIndex().getSourceRootForFile(originVirtualFile);
	}

	// the useCase has an effect on error messages but otherwise doesn't influence resolution
	@NotNull
	public static Module resolveModuleName(QualifiedModuleName moduleName, ModuleNameResolutionUseCase useCase) throws ReferenceResolutionException {
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
		targetVirtualFile = targetVirtualFile.findChild(segments[segments.length - 1] + ".mahdl");
		if (targetVirtualFile == null) {
			String path = sourceRoot.getPath() + '/' + StringUtils.join(segments, '/') + ".mahdl";
			throw new ReferenceResolutionException("module file " + path + " not found");
		}
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

	public enum ModuleNameResolutionUseCase {
		NAME_DECLARATION_VALIDATION,
		REFERENCE_RESOLUTION
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
	public static LeafPsiElement getNameIdentifier(@NotNull ModuleInstanceDefinition node) {
		return node.getIdentifier();
	}

	//
	// reference support
	//

	// TODO autocomplete for module name in instance decl doesn't work
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
		return new LocalReference(node.getIdentifier());
	}

	@NotNull
	public static PsiReference getReference(@NotNull InstanceReferenceName node) {
		return new LocalReference(node.getIdentifier());
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

	public static void delete(@NotNull ModuleInstanceDefinition node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(@NotNull ASTWrapperPsiElement node, @NotNull Runnable actualDeleteCallback) throws IncorrectOperationException {
		PsiFile psiFile = node.getContainingFile();
		if (psiFile != null) {
			VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
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
