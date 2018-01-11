package name.martingeisse.mahdl.plugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.processor.ModuleProcessor;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlAnnotator implements Annotator {

	/**
	 * This method gets called on ALL PsiElements, post-order.
	 */
	@Override
	public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder) {
		if (psiElement instanceof Module) {
			annotate((Module) psiElement, annotationHolder);
		}
	}

	private void annotate(@NotNull Module module, @NotNull AnnotationHolder annotationHolder) {
		new ModuleProcessor(module, annotationHolder::createErrorAnnotation).process();
	}

}
