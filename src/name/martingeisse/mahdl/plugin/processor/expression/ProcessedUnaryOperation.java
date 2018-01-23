package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedUnaryOperation extends ProcessedExpression {

	private final ProcessedExpression operand;
	private final ProcessedUnaryOperator operator;

	public ProcessedUnaryOperation(PsiElement errorSource,
								   ProcessedExpression operand,
								   ProcessedUnaryOperator operator) throws TypeErrorException {
		super(errorSource, operator.checkType(operator.checkType(operand.getDataType())));
		this.operand = operand;
		this.operator = operator;
	}

	public ProcessedExpression getOperand() {
		return operand;
	}

	public ProcessedUnaryOperator getOperator() {
		return operator;
	}

}
