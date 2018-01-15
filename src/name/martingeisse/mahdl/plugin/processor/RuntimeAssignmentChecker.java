/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
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
	private final Map<String, Named> definitions;

	public RuntimeAssignmentChecker(@NotNull ErrorHandler errorHandler, @NotNull ExpressionTypeChecker expressionTypeChecker, @NotNull Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.expressionTypeChecker = expressionTypeChecker;
		this.definitions = definitions;
	}

	/**
	 * Ensures that the specified left side is assignable and that the right side produces a compatible type.
	 */
	public void checkRuntimeAssignment(@NotNull Expression leftSide, @NotNull Expression rightSide, boolean allowContinuous, boolean allowClocked) {
		// TODO convert constants
		checkLValue(leftSide, allowContinuous, allowClocked);
		ProcessedDataType leftSideType = expressionTypeChecker.check(leftSide);
		ProcessedDataType rightSideType = expressionTypeChecker.check(rightSide);
		checkRuntimeAssignmentType(rightSide, leftSideType, rightSideType);

	}

	/**
	 * Ensures that the right side produces a type compatible with the left side's type.
	 */
	private void checkRuntimeAssignmentType(@NotNull PsiElement errorSource, @NotNull ProcessedDataType leftSideType, @NotNull ProcessedDataType rightSideType) {
		// TODO convert constants
		ProcessedDataType.Family variableTypeFamily = leftSideType.getFamily();
		ProcessedDataType.Family valueTypeFamily = rightSideType.getFamily();
		if (variableTypeFamily == ProcessedDataType.Family.UNKNOWN || valueTypeFamily == ProcessedDataType.Family.UNKNOWN) {
			// if either type is unknown, an error has already been reported, and we don't want any followup errors
			return;
		}
		if (variableTypeFamily != ProcessedDataType.Family.BIT && variableTypeFamily != ProcessedDataType.Family.VECTOR) {
			// integer and text should not exist at run-time, and a whole memory cannot be assigned to at once
			errorHandler.onError(errorSource, "cannot run-time assign to type " + leftSideType);
			return;
		}
		if (!rightSideType.equals(leftSideType)) {
			// otherwise, the types must be equal. They're bit or vector of some size, and we don't have any implicit
			// conversion, truncating or expanding.
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