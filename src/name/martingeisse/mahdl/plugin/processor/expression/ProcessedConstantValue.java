package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

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
	public ConstantValue evaluateFormallyConstant(FormallyConstantEvaluationContext context) {
		return value;
	}

}
