package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;

/**
 *
 */
public final class Constant extends SignalLike {

	public Constant(PsiElement nameElement, DataType dataTypeElement, Expression initializer) {
		super(nameElement, dataTypeElement, initializer);
	}

}
