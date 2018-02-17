/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.*;

/**
 * This class is like {@link ExpressionVerilogGenerator} but for L-expressions.
 * <p>
 * A fundamental difference between these two classes is that this variable generator cannot extract any L-expressions.
 * This ight have been possible for continuous assignments, but for clocked assignments, there is no way part of a
 * complex left-hand side can be extracted.
 * <p>
 * Right now, this isn't a problem because Verilog seems to support all kinds of left-hand side expressions we use.
 * Synthesis tools might be more picky, though, and if this ever becomes a problem, the best way to deal with the
 * problem is probably not extraction but breaking apart the assignment in a way that is specific to the LHS expression
 * type. For example, for two 8-bit signal x and y, (x _ y)[11:4] would be split into two assignments to x[3:0] and
 * y[7:4], using a corresponding slice of the RHS.
 * <p>
 * Extracting R-expressions that are embedded in the L-expressions, such as the index of a selection, is no problem and
 * is delegated to the {@link ExpressionVerilogGenerator}.
 */
public final class VariableVerilogGenerator {

	private final ExpressionVerilogGenerator expressionVerilogGenerator;

	public VariableVerilogGenerator(ExpressionVerilogGenerator expressionVerilogGenerator) {
		this.expressionVerilogGenerator = expressionVerilogGenerator;
	}

	/**
	 * Generates the code for the specified expression to the builder.
	 */
	public void generate(ProcessedExpression expression, StringBuilder builder) {
		if (expression instanceof UnknownExpression) {

			throw new ModuleHasErrorsException();

		} else if (expression instanceof SignalLikeReference) {

			builder.append(((SignalLikeReference) expression).getDefinition().getName());

		} else if (expression instanceof SyntheticSignalLikeExpression) {

			builder.append(((SyntheticSignalLikeExpression) expression).getName());

		} else if (expression instanceof InstancePortReference) {

			InstancePortReference instancePortReference = (InstancePortReference) expression;
			builder.append(instancePortReference.getModuleInstance().getName());
			builder.append('.').append(instancePortReference.getPort().getName());

		} else if (expression instanceof ProcessedIndexSelection) {

			ProcessedIndexSelection selection = (ProcessedIndexSelection) expression;
			generate(selection.getContainer(), builder);
			builder.append('[');
			expressionVerilogGenerator.generate(selection.getIndex(), builder);
			builder.append(']');

		} else if (expression instanceof ProcessedRangeSelection) {

			ProcessedRangeSelection selection = (ProcessedRangeSelection) expression;
			generate(selection.getContainer(), builder);
			builder.append('[').append(selection.getFromIndex()).append(':').append(selection.getToIndex()).append(']');

		} else if (expression instanceof ProcessedBinaryOperation) {

			ProcessedBinaryOperation operation = (ProcessedBinaryOperation) expression;
			ProcessedBinaryOperator operator = operation.getOperator();
			if (operator == ProcessedBinaryOperator.VECTOR_CONCAT) {
				builder.append('{');
				generate(operation.getLeftOperand(), builder);
				builder.append(", ");
				generate(operation.getRightOperand(), builder);
				builder.append('}');
			} else {
				throw newInvalidAssignmentTargetException(expression);
			}

		} else {
			throw newInvalidAssignmentTargetException(expression);
		}
	}

	private static ModuleCannotGenerateCodeException newInvalidAssignmentTargetException(ProcessedExpression expression) {
		return new ModuleCannotGenerateCodeException("invalid assignment target: " + expression);
	}

}
