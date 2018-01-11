package name.martingeisse.mahdl.plugin.processor.definition;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class Signal extends SignalLike {

	public Signal(PsiElement nameElement, DataType dataTypeElement, ProcessedDataType processedDataType, Expression initializer) {
		super(nameElement, dataTypeElement, processedDataType, initializer);
	}

}
