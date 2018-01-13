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

	public static PsiElement setText(LeafPsiElement element, String newText) {
		return (PsiElement) element.replaceWithText(newText);
	}

	public static <T> T getAncestor(PsiElement element, Class<T> nodeClass) {
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

	public static VirtualFile getSourceRoot(PsiElement psiElement) {

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

	public static LeafPsiElement getNameIdentifier(Module node) {
		return node.getModuleName();
	}

	public static LeafPsiElement getNameIdentifier(PortDefinition node) {
		return node.getIdentifier();
	}

	public static LeafPsiElement getNameIdentifier(SignalLikeDefinition node) {
		if (node instanceof SignalLikeDefinition_WithoutInitializer) {
			return ((SignalLikeDefinition_WithoutInitializer) node).getIdentifier();
		} else if (node instanceof SignalLikeDefinition_WithInitializer) {
			return ((SignalLikeDefinition_WithInitializer) node).getIdentifier();
		} else {
			return null;
		}
	}

	public static LeafPsiElement getNameIdentifier(ImplementationItem_ModuleInstance node) {
		return node.getInstanceName();
	}

	//
	// reference support
	//

	public static PsiReference getReference(QualifiedModuleName node) {
		return new ModuleReference(node);
	}

	public static PsiReference getReference(InstancePortName node) {
		return new ModuleInstancePortReference(node);
	}

	public static PsiReference getReference(Expression_Identifier node) {
		return new IdentifierExpressionReference(node);
	}

	public static PsiReference getReference(InstanceReferenceName node) {
		return new ModuleInstanceReference(node);
	}

	//
	// safe delete
	//

	public static void delete(Module node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(PortDefinition node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(SignalLikeDefinition node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(ImplementationItem_ModuleInstance node) throws IncorrectOperationException {
		delete(node, node::superclassDelete);
	}

	public static void delete(ASTWrapperPsiElement node, Runnable actualDeleteCallback) throws IncorrectOperationException {
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

	public static String[] parseQualifiedModuleName(QualifiedModuleName name) {
		List<String> segments = new ArrayList<>();
		for (LeafPsiElement segment : name.getSegments().getAll()) {
			segments.add(segment.getText());
		}
		return segments.toArray(new String[segments.size()]);
	}

	public static String getSimpleModuleName(QualifiedModuleName name) {
		MutableObject<LeafPsiElement> elementHolder = new MutableObject<>();
		name.getSegments().foreach(elementHolder::setValue);
		return elementHolder.getValue().getText();
	}

}
