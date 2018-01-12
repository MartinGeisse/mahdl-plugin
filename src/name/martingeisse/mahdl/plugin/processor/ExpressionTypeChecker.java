package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks expressions for type safety and returns their types. If an expression or sub-expression is constant, then
 * type safety rules are slightly relaxed
 */
public final class ExpressionTypeChecker {

	private final ErrorHandler errorHandler;
	private final Map<String, Named> definitions;
	private final Map<Expression, ConstantValue> convertedConstantExpressionValues;

	public ExpressionTypeChecker(ErrorHandler errorHandler, Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.definitions = definitions;
		this.convertedConstantExpressionValues = new HashMap<>();
	}

	public Map<Expression, ConstantValue> getConvertedConstantExpressionValues() {
		return convertedConstantExpressionValues;
	}

	public ProcessedDataType check(Expression expression) {
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

}
