/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.AssignmentValidator;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO switch / break
 */
public final class StatementProcessor {

	private final ErrorHandler errorHandler;
	private final ExpressionProcessor expressionProcessor;
	private final AssignmentValidator assignmentValidator;

	public StatementProcessor(ErrorHandler errorHandler, ExpressionProcessor expressionProcessor, AssignmentValidator assignmentValidator) {
		this.errorHandler = errorHandler;
		this.expressionProcessor = expressionProcessor;
		this.assignmentValidator = assignmentValidator;
	}

	public ProcessedDoBlock process(ImplementationItem_DoBlock doBlock) {
		DoBlockTrigger trigger = doBlock.getTrigger();
		AssignmentValidator.TriggerKind triggerKind;
		ProcessedExpression clock;
		if (trigger instanceof DoBlockTrigger_Combinatorial) {
			triggerKind = AssignmentValidator.TriggerKind.CONTINUOUS;
			clock = null;
		} else if (trigger instanceof DoBlockTrigger_Clocked) {
			triggerKind = AssignmentValidator.TriggerKind.CLOCKED;
			Expression clockExpression = ((DoBlockTrigger_Clocked) trigger).getClockExpression();
			clock = expressionProcessor.process(clockExpression, ProcessedDataType.Bit.INSTANCE);
		} else {
			error(trigger, "unknown trigger type");
			return null;
		}
		ProcessedStatement body = process(doBlock.getStatement(), triggerKind);
		return new ProcessedDoBlock(clock, body);
	}

	public ProcessedStatement process(Statement statement, AssignmentValidator.TriggerKind triggerKind) {
		if (statement instanceof Statement_Block) {

			Statement_Block block = (Statement_Block) statement;
			List<ProcessedStatement> processedBodyStatements = new ArrayList<>();
			for (Statement bodyStatement : block.getBody().getAll()) {
				processedBodyStatements.add(process(bodyStatement, triggerKind));
			}
			return new ProcessedBlock(statement, ImmutableList.copyOf(processedBodyStatements));

		} else if (statement instanceof Statement_Assignment) {

			Statement_Assignment assignment = (Statement_Assignment) statement;
			ProcessedExpression leftHandSide = expressionProcessor.process(assignment.getLeftSide());
			ProcessedExpression rightHandSide = expressionProcessor.process(assignment.getRightSide());
			assignmentValidator.validateAssignmentTo(leftHandSide, triggerKind);
			return new ProcessedAssignment(statement, leftHandSide, rightHandSide);

		} else if (statement instanceof Statement_IfThen) {

			Statement_IfThen ifStatement = (Statement_IfThen) statement;
			return processIfStatement(statement, ifStatement.getCondition(), ifStatement.getThenBranch(), null, triggerKind);

		} else if (statement instanceof Statement_IfThenElse) {

			Statement_IfThenElse ifStatement = (Statement_IfThenElse) statement;
			return processIfStatement(statement, ifStatement.getCondition(), ifStatement.getThenBranch(), ifStatement.getElseBranch(), triggerKind);

		} else if (statement instanceof Statement_Switch) {

			return process((Statement_Switch)statement);

		} else {
			return error(statement, "unknown statement type");

		}
	}

	private ProcessedIf processIfStatement(PsiElement errorSource, Expression condition, Statement thenBranch, Statement elseBranch, AssignmentValidator.TriggerKind triggerKind) {
		ProcessedExpression processedCondition = expressionProcessor.process(condition, ProcessedDataType.Bit.INSTANCE);
		ProcessedStatement processedThenBranch = process(thenBranch, triggerKind);
		ProcessedStatement processedElseBranch;
		if (elseBranch == null) {
			processedElseBranch = new Nop(errorSource);
		} else {
			processedElseBranch = process(elseBranch, triggerKind);
		}
		return new ProcessedIf(errorSource, processedCondition, processedThenBranch, processedElseBranch);
	}

	private ProcessedSwitchStatement process(Statement_Switch switchStatement) {
		// TODO Verilog has no break and uses comma for multi-selector-value cases. Sounds far more logical.
	}

	/**
	 * This method is sometimes called with a sub-statement of the current statement as the error source, in case
	 * that error can be attributed to that sub-statement.
	 * <p>
	 * The same error source gets attached to the returned {@link UnknownStatement} as the error source for other
	 * error messages added later, which is wrong in principle. However, no further error messages should be generated
	 * at all, and so this wrong behavior should not matter.
	 */
	@NotNull
	private UnknownStatement error(@NotNull PsiElement errorSource, @NotNull String message) {
		errorHandler.onError(errorSource, message);
		return new UnknownStatement(errorSource);
	}

}
