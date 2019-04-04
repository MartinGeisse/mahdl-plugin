package name.martingeisse.mahdl.plugin;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.util.ProcessingContext;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 *
 */
public class MahdlModuleNameRenameInputValidator implements RenameInputValidator {

	private static final Pattern VALID_MODULE_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*(\\.[a-zA-Z_][a-zA-Z_0-9]*)*");

	@NotNull
	@Override
	public ElementPattern<? extends PsiElement> getPattern() {
		return StandardPatterns.instanceOf(Module.class);
	}

	@Override
	public boolean isInputValid(@NotNull String s, @NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
		return VALID_MODULE_NAME_PATTERN.matcher(s).matches();
	}

}
