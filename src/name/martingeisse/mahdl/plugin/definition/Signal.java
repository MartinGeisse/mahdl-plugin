package name.martingeisse.mahdl.plugin.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;

/**
 *
 */
public final class Signal extends SignalLike {

	public Signal(PsiElement nameElement, DataType dataTypeElement, Expression initializer) {
		super(nameElement, dataTypeElement, initializer);
	}

}
