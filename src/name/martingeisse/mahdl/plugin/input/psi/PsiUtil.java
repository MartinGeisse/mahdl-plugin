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
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.FileContentUtil;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.IdentifierExpressionReference;
import name.martingeisse.mahdl.plugin.input.ModuleInstancePortReference;
import name.martingeisse.mahdl.plugin.input.ModuleInstanceReference;
import name.martingeisse.mahdl.plugin.input.ModuleReference;
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

	@Nullable
	public static VirtualFile getSourceRoot(@NotNull PsiElement psiElement) {

//		com.intellij.openapi.module.Module ideModule = ModuleUtil.findModuleForPsiElement(moduleName);
//		if (ideModule == null) {
//			return null;
//		}


		PsiFile originPsiFile = psiElement.getContainingFile();
		if (originPsiFile == null) {
			return null;
		}
		Project project = originPsiFile.getProject();
		if (project == null) {
			return null;
		}
		VirtualFile originVirtualFile = originPsiFile.getVirtualFile();
		if (originVirtualFile == null) {
			return null;
		}
//		com.intellij.openapi.module.Module ideModule = ModuleUtil.findModuleForFile(virtualFile, project);
//		if (ideModule == null) {
//			return null;
//		}
//		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(ideModule);
//		if (moduleRootManager == null) {
//			return null;
//		}

		return ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(originVirtualFile);
	}

	//
	// naming support
	//

	@Nullable
	public static LeafPsiElement getNameIdentifier(@NotNull Module node) {
		return node.getModuleName();
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
	public static String[] parseQualifiedModuleName(@NotNull QualifiedModuleName name) {
		List<String> segments = new ArrayList<>();
		for (LeafPsiElement segment : name.getSegments().getAll()) {
			segments.add(segment.getText());
		}
		return segments.toArray(new String[segments.size()]);
	}

	@NotNull
	public static String getSimpleModuleName(@NotNull QualifiedModuleName name) {
		MutableObject<LeafPsiElement> elementHolder = new MutableObject<>();
		name.getSegments().foreach(elementHolder::setValue);
		return elementHolder.getValue().getText();
	}

}
