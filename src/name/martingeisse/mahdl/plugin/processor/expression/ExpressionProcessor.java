package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public interface ExpressionProcessor {

	ProcessedExpression process(ExtendedExpression expression);

	ProcessedExpression process(Expression expression);

	ProcessedExpression convertImplicitly(ProcessedExpression sourceExpression, ProcessedDataType targetType);

}
