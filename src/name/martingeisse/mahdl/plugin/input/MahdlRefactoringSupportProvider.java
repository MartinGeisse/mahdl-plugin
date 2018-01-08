package name.martingeisse.mahdl.plugin.input;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.ImplementationItem_ModuleInstance;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import name.martingeisse.mahdl.plugin.input.psi.PortDefinition;
import name.martingeisse.mahdl.plugin.input.psi.SignalLikeDefinition;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlRefactoringSupportProvider extends RefactoringSupportProvider {

	@Override
	public boolean isSafeDeleteAvailable(@NotNull PsiElement element) {
		return (element instanceof Module) || (element instanceof PortDefinition) || (element instanceof SignalLikeDefinition) || (element instanceof ImplementationItem_ModuleInstance);
	}

}
