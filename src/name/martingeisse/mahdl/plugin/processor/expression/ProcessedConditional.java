package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedConditional extends ProcessedExpression {

	private final ProcessedExpression condition;
	private final ProcessedExpression thenBranch;
	private final ProcessedExpression elseBranch;

	public ProcessedConditional(PsiElement errorSource,
								ProcessedExpression condition,
								ProcessedExpression thenBranch,
								ProcessedExpression elseBranch) throws TypeErrorException {
		super(errorSource, checkTypes(condition, thenBranch, elseBranch));
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

	private static ProcessedDataType checkTypes(ProcessedExpression condition,
												ProcessedExpression thenBranch,
												ProcessedExpression elseBranch) throws TypeErrorException {
		// TODO
	}

	public ProcessedExpression getCondition() {
		return condition;
	}

	public ProcessedExpression getThenBranch() {
		return thenBranch;
	}

	public ProcessedExpression getElseBranch() {
		return elseBranch;
	}

}
