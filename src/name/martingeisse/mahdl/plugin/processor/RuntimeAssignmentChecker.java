/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionTypeChecker;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This class checks for type safety of run-time assignments (excluding constant initializers) and that the left-hand
 * side denotes an L-value. It is not concerned with detecting multiple or missing assignments.
 *
 * This class uses the {@link ExpressionTypeChecker} to perform the actual type safety check.
 */
public final class RuntimeAssignmentChecker {

	private final ErrorHandler errorHandler;
	private final ExpressionTypeChecker expressionTypeChecker;
	private final ConstantExpressionEvaluator constantExpressionEvaluator;
	private final Map<String, Named> definitions;

	public RuntimeAssignmentChecker(@NotNull ErrorHandler errorHandler,
									@NotNull ExpressionTypeChecker expressionTypeChecker,
									@NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
									@NotNull Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.expressionTypeChecker = expressionTypeChecker;
		this.constantExpressionEvaluator = constantExpressionEvaluator;
		this.definitions = definitions;
	}

	/**
	 * Ensures that the specified left side is assignable and that the right side produces a compatible type.
	 */
	public void checkRuntimeAssignment(@NotNull Expression leftSide, @NotNull Expression rightSide, boolean allowContinuous, boolean allowClocked) {
		checkLValue(leftSide, allowContinuous, allowClocked);
		ProcessedDataType leftSideType = expressionTypeChecker.check(leftSide);
		ProcessedDataType rightSideType = expressionTypeChecker.check(rightSide);
		checkRuntimeAssignmentType(rightSide, leftSideType, rightSideType);
	}

	/**
	 * Ensures that the right side produces a type compatible with the left side's type.
	 */
	private void checkRuntimeAssignmentType(@NotNull PsiElement errorSource, @NotNull ProcessedDataType leftSideType, @NotNull ProcessedDataType rightSideType) {
		ProcessedDataType.Family leftTypeFamily = leftSideType.getFamily();
		ProcessedDataType.Family rightTypeFamily = rightSideType.getFamily();

		// if either type is unknown, an error has already been reported, and we don't want any followup errors
		if (leftTypeFamily == ProcessedDataType.Family.UNKNOWN || rightTypeFamily == ProcessedDataType.Family.UNKNOWN) {
			return;
		}

		// integer and text should not exist at run-time, and a whole memory cannot be assigned to at once
		if (leftTypeFamily != ProcessedDataType.Family.BIT && leftTypeFamily != ProcessedDataType.Family.VECTOR) {
			errorHandler.onError(errorSource, "cannot run-time assign to type " + leftSideType);
			return;
		}

		// Otherwise, either the types must be equal, or the right-hand side must be a constant that can be converted
		// to the type of the left-hand side. Run-time conversion is not done since all combinations of non-equal
		// run-time types (bit and vectors of some size) cannot be automatically converted, truncated or expanded.
		if (!rightSideType.equals(leftSideType)) {
			ConstantValue constantValue = constantExpressionEvaluator.evaluate()
			// TODO convert constants
			errorHandler.onError(errorSource, "cannot convert from type " + rightSideType + " to type " + leftSideType + " at run-time");
		}
	}

	/**
	 * Ensures that the specified left-side expression is assignable to. The flags control whether the left side
	 * is allowed to be a continuous destination and/or or a clocked destination.
	 */
	private void checkLValue(@NotNull Expression expression, boolean allowContinuous, boolean allowClocked) {
		if (expression instanceof Expression_Identifier) {
			LeafPsiElement identifierElement = ((Expression_Identifier) expression).getIdentifier();
			String identifierText = identifierElement.getText();
			Named definition = definitions.get(identifierText);
			if (definition != null) {
				// undefined symbols are already marked by the ExpressionTypeChecker
				if (definition instanceof Port) {
					PortDirection direction = ((Port) definition).getDirectionElement();
					if (!(direction instanceof PortDirection_Out)) {
						errorHandler.onError(expression, "input port " + definition.getName() + " cannot be assigned to");
					} else if (!allowContinuous) {
						errorHandler.onError(expression, "continuous assignment not allowed in this context");
					}
				} else if (definition instanceof Signal) {
					if (!allowContinuous) {
						errorHandler.onError(expression, "continuous assignment not allowed in this context");
					}
				} else if (definition instanceof Register) {
					if (!allowClocked) {
						errorHandler.onError(expression, "clocked assignment not allowed in this context");
					}
				} else if (definition instanceof Constant) {
					errorHandler.onError(expression, "cannot assign to constant");
				}
			}
		} else if (expression instanceof Expression_IndexSelection) {
			checkLValue(((Expression_IndexSelection) expression).getContainer(), allowContinuous, allowClocked);
		} else if (expression instanceof Expression_RangeSelection) {
			checkLValue(((Expression_RangeSelection) expression).getContainer(), allowContinuous, allowClocked);
		} else if (expression instanceof Expression_InstancePort) {
			// TODO
		} else if (expression instanceof Expression_BinaryConcat) {
			Expression_BinaryConcat concat = (Expression_BinaryConcat) expression;
			checkLValue(concat.getLeftOperand(), allowContinuous, allowClocked);
			checkLValue(concat.getRightOperand(), allowContinuous, allowClocked);
		} else if (expression instanceof Expression_Parenthesized) {
			checkLValue(((Expression_Parenthesized) expression).getExpression(), allowContinuous, allowClocked);
		} else {
			errorHandler.onError(expression, "expression cannot be assigned to");
		}
	}

}
