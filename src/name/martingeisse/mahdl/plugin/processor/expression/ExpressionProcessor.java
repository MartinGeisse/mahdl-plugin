package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;

/**
 *
 */
public interface ExpressionProcessor {

	ProcessedExpression process(ExtendedExpression expression);

	ProcessedExpression process(Expression expression);

}
