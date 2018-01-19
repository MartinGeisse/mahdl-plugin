/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.LiteralParser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Checks expressions for type safety and returns their types.
 *
 * This type checker obviously takes type specifiers of constants, signals etc. into account, and relies on
 * constant evaluation in those type specifiers to be finished to know the types. It will, however, never evaluate
 * a constant to check for type safety of the expression the constant appears in! That is, it only checks types, not
 * values. This excludes, for example, overflow in constant conversion and out-of-range errors for index selection
 * with a constant index.
 *
 * <p>
 * TODO do not use the {@link ConstantExpressionEvaluator} to try to evaluate a constant sub-expression and possibly
 * find errors. There is no reliable way to evaluate an expression which is not known to be constant and which may
 * contain type errors, while still generating good error messages. Instead, make this type checker handle the
 * constant compile-time type system too, and have it check all expressions first, including constant ones. Then,
 * if the type check is okay, attempt to find constant sub-expressions. (TODO: find by try-eval or by is-constant?
 * This can be answered by asking: What if an expression is constant but produces an error during folding, e.g. a
 * conversion error or an overflow. Should it still be folded or should the error be deferred to run-time? If such
 * errors should be deferred to run-time, just try to fold and handle each error by not folding. If such errors should
 * be compile-time errors, then we must first run a separate step to know if we should fold, and then handle errors
 * by reporting them. -- They should be compile-time errors for usability and per the language spec, so we need a
 * separate step to identify sub-expressions to fold. Basic idea: anything that does not contain a reference to a
 * non-constant signal-like)
 *
 * TODO we need similar conversion rules as for TSIVOs for index selection and range selection too! idea:
 * the left-hand type must be a vector/memory anyway, so we have a size. demand that the right-hand type is
 * either an integer, then it is constant and must be within range. Or it is a vector, then it's not always constant
 * but demand that leftSize >= 2^rightSize, so it's always in range. The user has to extend the left-hand side if
 * it is too short, and by that define what the result is for out-of-range.
 */
public final class ExpressionTypeChecker {

	private final ErrorHandler errorHandler;
	private final Map<String, Named> definitions;

	public ExpressionTypeChecker(@NotNull ErrorHandler errorHandler, @NotNull Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.definitions = definitions;
	}

	@NotNull
	public ProcessedDataType checkExpression(@NotNull Expression expression) {
		// TODO
		if (expression instanceof Expression_Literal) {
			try {
				ConstantValue literalValue = LiteralParser.parseLiteral((Expression_Literal) expression);
				return literalValue.getDataType();
			} catch (LiteralParser.ParseException e) {
				return error(expression, e.getMessage());
			}
		} else if (expression instanceof Expression_Identifier) {
			String name = ((Expression_Identifier) expression).getIdentifier().getText();
			Named definition = definitions.get(name);
			if (definition == null) {
				return error(expression, "cannot resolve symbol '" + name + "'");
			}
		} else if (expression instanceof Expression_InstancePort) {
			// TODO

		} else if (expression instanceof Expression_IndexSelection) {
			Expression_IndexSelection indexSelection = (Expression_IndexSelection)expression;
			ProcessedDataType containerType = checkExpression(indexSelection.getContainer());
			ProcessedDataType indexType = checkExpression(indexSelection.getIndex());
			int containerSize;
			if (containerType instanceof ProcessedDataType.Vector) {
				containerSize = ((ProcessedDataType.Vector) containerType).getSize();
			} else if (containerType instanceof ProcessedDataType.Memory) {
				containerSize = ((ProcessedDataType.Memory) containerType).getFirstSize();
			} else {
				return error(indexSelection.getContainer(),
					"index selection is only allowed for vector and memory types, found " + containerType);
			}


		} else if (expression instanceof Expression_RangeSelection) {

		} else if (expression instanceof UnaryOperation) {

		} else if (expression instanceof BinaryOperation) {

		} else if (expression instanceof Expression_Mux) {

		} else if (expression instanceof Expression_FunctionCall) {

		} else if (expression instanceof Expression_Parenthesized) {

		}

//		if (expression instanceof Expression_Identifier) {
//			LeafPsiElement identifierElement = ((Expression_Identifier) expression).getIdentifier();
//			String identifierText = identifierElement.getText();
//			Named definition = definitions.get(identifierText);
//			if (definition == null) {
//				errorHandler.onError(identifierElement, "cannot resolve symbol " + identifierText);
//			} else {
//
//			}
//		}

		return null;
	}

	@NotNull
	public ProcessedDataType checkImplicitConversion(@NotNull Expression expression, @NotNull DataType targetType) {

	}

	@NotNull
	private ProcessedDataType error(@NotNull PsiElement element, @NotNull String message) {
		errorHandler.onError(element, message);
		return ProcessedDataType.Unknown.INSTANCE;
	}

}
