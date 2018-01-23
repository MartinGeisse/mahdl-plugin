package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public abstract class ProcessedExpression {

	private final PsiElement errorSource;
	private final ProcessedDataType dataType;

	public ProcessedExpression(PsiElement errorSource, ProcessedDataType dataType) {
		this.errorSource = errorSource;
		this.dataType = dataType;
	}

	public PsiElement getErrorSource() {
		return errorSource;
	}

	public ProcessedDataType getDataType() {
		return dataType;
	}

}
