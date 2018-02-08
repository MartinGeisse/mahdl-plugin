package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;

/**
 *
 */
public final class ProcessedAssignment extends ProcessedStatement {

	private final ProcessedExpression leftHandSide;
	private final ProcessedExpression rightHandSide;

	public ProcessedAssignment(PsiElement errorSource, ProcessedExpression leftHandSide, ProcessedExpression rightHandSide) {
		super(errorSource);
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
	}

	public ProcessedExpression getLeftHandSide() {
		return leftHandSide;
	}

	public ProcessedExpression getRightHandSide() {
		return rightHandSide;
	}

}
