package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.PortDirection;

/**
 *
 */
public final class Port extends SignalLike {

	private final PortDirection directionElement;

	public Port(PsiElement nameElement, PortDirection directionElement, DataType dataTypeElement) {
		super(nameElement, dataTypeElement, null);
		this.directionElement = directionElement;
	}

	public PortDirection getDirectionElement() {
		return directionElement;
	}

}
