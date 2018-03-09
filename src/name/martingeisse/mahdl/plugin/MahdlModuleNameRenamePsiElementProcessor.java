package name.martingeisse.mahdl.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlModuleNameRenamePsiElementProcessor extends RenamePsiElementProcessor {

	@Override
	public boolean canProcessElement(@NotNull PsiElement psiElement) {
		return (psiElement instanceof Module);
	}

}
