package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public abstract class ProcessedStatement {

	private final PsiElement errorSource;

	public ProcessedStatement(PsiElement errorSource) {
		this.errorSource = errorSource;
	}

	public PsiElement getErrorSource() {
		return errorSource;
	}

}
