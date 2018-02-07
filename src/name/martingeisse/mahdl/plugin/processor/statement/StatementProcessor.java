package name.martingeisse.mahdl.plugin.processor.statement;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class StatementProcessor {

	private final ErrorHandler errorHandler;
	private final ExpressionProcessor expressionProcessor;

	public StatementProcessor(ErrorHandler errorHandler, ExpressionProcessor expressionProcessor) {
		this.errorHandler = errorHandler;
		this.expressionProcessor = expressionProcessor;
	}

	public ProcessedDoBlock process(ImplementationItem_DoBlock doBlock) {
		DoBlockTrigger trigger = doBlock.getTrigger();
		ProcessedExpression clock;
		if (trigger instanceof DoBlockTrigger_Combinatorial) {
			clock = null;
		} else if (trigger instanceof DoBlockTrigger_Clocked) {
			clock = expressionProcessor.process(((DoBlockTrigger_Clocked) trigger).getClockExpression());
		} else {
			error(trigger, "unknown trigger type");
			return null;
		}
		ProcessedStatement body = process(doBlock.getStatement());
		return new ProcessedDoBlock(clock, body);
	}

	public ProcessedStatement process(Statement statement) {
		if (statement instanceof Statement_Block) {

			Statement_Block block = (Statement_Block) statement;
			List<ProcessedStatement> processedBodyStatements = new ArrayList<>();
			for (Statement bodyStatement : block.getBody().getAll()) {
				processedBodyStatements.add(process(bodyStatement));
			}
			return new ProcessedBlock(ImmutableList.copyOf(processedBodyStatements));

		} else if (statement instanceof Statement_Assignment) {

			Statement_Assignment assignment = (Statement_Assignment) statement;
			ProcessedExpression leftHandSide = expressionProcessor.process(assignment.getLeftSide());
			ProcessedExpression rightHandSide = expressionProcessor.process(assignment.getRightSide());
			return new ProcessedAssignment(leftHandSide, rightHandSide);

		} else if (statement instanceof Statement_IfThen) {

			Statement_IfThen ifStatement = (Statement_IfThen) statement;
			return processIfStatement(ifStatement.getCondition(), ifStatement.getThenBranch(), null);

		} else if (statement instanceof Statement_IfThenElse) {

			Statement_IfThenElse ifStatement = (Statement_IfThenElse) statement;
			return processIfStatement(ifStatement.getCondition(), ifStatement.getThenBranch(), ifStatement.getElseBranch());

		} else if (statement instanceof Statement_Switch) {

			return error(statement, "switch statements not implemented yet");

		} else if (statement instanceof Statement_Break) {

			return error(statement, "break statements not implemented yet");

		} else {
			return error(statement, "unknown statement type");

		}
	}

	private ProcessedIf processIfStatement(Expression condition, Statement thenBranch, Statement elseBranch) {
		ProcessedExpression processedCondition = expressionProcessor.process(condition);
		ProcessedStatement processedThenBranch = process(thenBranch);
		ProcessedStatement processedElseBranch;
		if (elseBranch == null) {
			processedElseBranch = new Nop();
		} else {
			processedElseBranch = process(elseBranch);
		}
		return new ProcessedIf(processedCondition, processedThenBranch, processedElseBranch);
	}

	// ------
	// TODO for now, processed statements don't have an error source

	@NotNull
	private UnknownStatement error(@NotNull PsiElement errorSource, @NotNull String message) {
		errorHandler.onError(errorSource, message);
		return new UnknownStatement();
	}

	@NotNull
	private UnknownStatement error(@NotNull ProcessedExpression processedExpression, @NotNull String message) {
		return error(processedExpression.getErrorSource(), message);
	}

//	/**
//	 * This method is sometimes called with a sub-statement of the current statement as the error source, in case
//	 * that error can be attributed to that sub-statement.
//	 * <p>
//	 * The same error source gets attached to the returned {@link UnknownStatement} as the error source for other
//	 * error messages added later, which is wrong in principle. However, no further error messages should be generated
//	 * at all, and so this wrong behavior should not matter.
//	 *
//	 *
//	 */
//	@NotNull
//	private UnknownStatement error(@NotNull PsiElement errorSource, @NotNull String message) {
//		errorHandler.onError(errorSource, message);
//		return new UnknownStatement(errorSource);
//	}
//
//	/**
//	 * the same note as for the other error method above applies to this one
//	 */
//	@NotNull
//	private UnknownStatement error(@NotNull ProcessedExpression processedExpression, @NotNull String message) {
//		return error(processedExpression.getErrorSource(), message);
//	}

}
