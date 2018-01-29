package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;

/**
 *
 */
public final class ProcessedConstantValue extends ProcessedExpression {

	private final ConstantValue value;

	public ProcessedConstantValue(PsiElement errorSource, ConstantValue value) {
		super(errorSource, value.getDataType());
		this.value = value;
	}

	public ConstantValue getValue() {
		return value;
	}

	@Override
	public ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return value;
	}

}
