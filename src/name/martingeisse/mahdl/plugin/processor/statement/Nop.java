package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class Nop extends ProcessedStatement {

	public Nop(PsiElement errorSource) {
		super(errorSource);
	}

}
