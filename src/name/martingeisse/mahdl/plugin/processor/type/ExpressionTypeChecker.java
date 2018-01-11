package name.martingeisse.mahdl.plugin.processor.type;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.Expression_Identifier;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.definition.Named;

import java.util.Map;

/**
 *
 */
public final class ExpressionTypeChecker {

	private final ErrorHandler errorHandler;
	private final Map<String, Named> definitions;

	public ExpressionTypeChecker(ErrorHandler errorHandler, Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.definitions = definitions;
	}

	public ProcessedDataType check(Expression expression) {
		// TODO

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
