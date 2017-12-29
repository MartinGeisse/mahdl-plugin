package name.martingeisse.mahdl.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.verilog.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class VerilogAnnotator implements Annotator {

	/**
	 * This method gets called on ALL PsiElements, post-order.
	 */
	@Override
	public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder) {
		if (psiElement instanceof SourceFileRoot) {
			annotate((SourceFileRoot)psiElement, annotationHolder);
		}
	}

	private void annotate(@NotNull SourceFileRoot psiElement, @NotNull AnnotationHolder annotationHolder) {
		for (ModuleWithKeywords moduleWithKeywords : psiElement.getModules().getAll()) {
			if (moduleWithKeywords instanceof ModuleWithKeywords_Module) {
				annotate(((ModuleWithKeywords_Module)moduleWithKeywords).getModule(), annotationHolder);
			} else if (moduleWithKeywords instanceof ModuleWithKeywords_Macromodule) {
				annotate(((ModuleWithKeywords_Macromodule)moduleWithKeywords).getModule(), annotationHolder);
			}
		}
	}

	private void annotate(@NotNull Module module, @NotNull AnnotationHolder annotationHolder) {

	}


	// 			annotationHolder.createErrorAnnotation(psiElement.getNode(), message);


}
