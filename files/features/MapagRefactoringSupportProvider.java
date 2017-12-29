package name.martingeisse.verilog.plugin.input;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import name.martingeisse.mapag.input.psi.Production;
import name.martingeisse.mapag.input.psi.TerminalDeclaration;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MapagRefactoringSupportProvider extends RefactoringSupportProvider {

	@Override
	public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
		return (element instanceof TerminalDeclaration) || (element instanceof Production);
	}

}
