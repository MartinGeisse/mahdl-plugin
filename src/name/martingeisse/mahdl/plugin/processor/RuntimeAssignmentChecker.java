package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

import java.util.Map;

/**
 * This class checks for type safety of run-time assignments (excluding constant initializers) and that the left-hand
 * side denotes an L-value. It is not concerned with detecting multiple or missing assignments.
 */
public final class RuntimeAssignmentChecker {

	private final ErrorHandler errorHandler;
	private final Map<String, Named> definitions;

	public RuntimeAssignmentChecker(ErrorHandler errorHandler, Map<String, Named> definitions) {
		this.errorHandler = errorHandler;
		this.definitions = definitions;
	}

	private void checkRuntimeAssignmentType(PsiElement errorSource, ProcessedDataType variableType, ProcessedDataType valueType) {
		ProcessedDataType.Family variableTypeFamily = variableType.getFamily();
		ProcessedDataType.Family valueTypeFamily = valueType.getFamily();
		if (variableTypeFamily == ProcessedDataType.Family.UNKNOWN || valueTypeFamily == ProcessedDataType.Family.UNKNOWN) {
			// if either type is unknown, an error has already been reported, and we don't want any followup errors
			return;
		}
		if (variableTypeFamily != ProcessedDataType.Family.BIT && variableTypeFamily != ProcessedDataType.Family.VECTOR) {
			// integer and text should not exist at run-time, and a whole memory cannot be assigned to at once
			errorHandler.onError(errorSource, "cannot run-time assign to type " + variableType);
			return;
		}
		if (!valueType.equals(variableType)) {
			// otherwise, the types must be equal. They're bit or vector of some size, and we don't have any implicit
			// conversion, truncating or expanding.
			errorHandler.onError(errorSource, "cannot convert from type " + valueType + " to type " + variableType + " at run-time");
		}
	}

	private void checkLValue(Expression expression, boolean allowContinuous, boolean allowClocked) {
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
			Expression_BinaryConcat concat = (Expression_BinaryConcat)expression;
			checkLValue(concat.getLeftOperand(), allowContinuous, allowClocked);
			checkLValue(concat.getRightOperand(), allowContinuous, allowClocked);
		} else if (expression instanceof Expression_Parenthesized) {
			checkLValue(((Expression_Parenthesized) expression).getExpression(), allowContinuous, allowClocked);
		} else {
			errorHandler.onError(expression, "expression cannot be assigned to");
		}
	}

}
