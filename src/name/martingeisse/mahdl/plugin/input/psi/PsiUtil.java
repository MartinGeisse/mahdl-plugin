package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.FileContentUtil;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.IdentifierExpressionReference;

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
		return null; // TODO return new fewefwefwefwfew(node);
	}

	public static PsiReference getReference(InstancePortName node) {
		return null; // TODO return new fewefwefwefwfew(node);
	}

	public static PsiReference getReference(Expression_Identifier node) {
		return new IdentifierExpressionReference(node);
	}

	public static PsiReference getReference(InstanceReferenceName node) {
		return null; // TODO return new fewefwefwefwfew(node);
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

}
