package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;

/**
 *
 */
public abstract class SignalLike extends Named {

	private final DataType dataTypeElement;
	private final Expression initializer;

	public SignalLike(PsiElement nameElement, DataType dataTypeElement, Expression initializer) {
		super(nameElement);
		this.dataTypeElement = dataTypeElement;
		this.initializer = initializer;
	}

	public DataType getDataTypeElement() {
		return dataTypeElement;
	}

	public Expression getInitializer() {
		return initializer;
	}

}
