package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public interface ExpressionProcessor {

	ProcessedExpression process(ExtendedExpression expression);

	ProcessedExpression process(Expression expression);

	ProcessedExpression convertImplicitly(ProcessedExpression sourceExpression, ProcessedDataType targetType);

	ErrorHandler getErrorHandler();

}
