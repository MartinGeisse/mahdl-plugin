/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks expressions for type safety and returns their types. If an expression or sub-expression is constant, then
 * type safety rules are slightly relaxed.
 *
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
 */
public final class ExpressionTypeChecker {

	private final ErrorHandler errorHandler;
	private final Map<String, Named> definitions;
	private final Map<Expression, ConstantValue> convertedConstantExpressionValues;

	public ExpressionTypeChecker(@NotNull ErrorHandler errorHandler, @NotNull Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.definitions = definitions;
		this.convertedConstantExpressionValues = new HashMap<>();
	}

	@NotNull
	public Map<Expression, ConstantValue> getConvertedConstantExpressionValues() {
		return convertedConstantExpressionValues;
	}

	@NotNull
	public ProcessedDataType checkExpression(@NotNull Expression expression) {
		// TODO
		if (expression instanceof Expression_Literal) {

		} else if (expression instanceof Expression_Identifier) {

		} else if (expression instanceof Expression_InstancePort) {

		} else if (expression instanceof Expression_IndexSelection) {

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

}
