package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.DataType;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.Expression_Identifier;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.Constant;
import name.martingeisse.mahdl.plugin.processor.definition.Named;

import java.util.Map;

/**
 *
 */
public class ExpressionTypeChecker {

	private final Map<String, Named> definitions;

	public ExpressionTypeChecker(Map<String, Named> definitions) {
		this.definitions = definitions;
	}

	public ProcessedDataType check(Expression expression) {

		if (expression instanceof Expression_Identifier) {
			LeafPsiElement identifierElement = ((Expression_Identifier) expression).getIdentifier();
			String identifierText = identifierElement.getText();
			Named definition = definitions.get(identifierText);
			if (definition == null) {
				onError(identifierElement, "cannot resolve symbol " + identifierText);
			} else {

			}
		}
	}

}
