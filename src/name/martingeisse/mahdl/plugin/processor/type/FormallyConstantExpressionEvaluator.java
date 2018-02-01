package name.martingeisse.mahdl.plugin.processor.type;

import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public interface FormallyConstantExpressionEvaluator {

	@NotNull
	ConstantValue evaluate(@NotNull ExtendedExpression expression);

	@NotNull
	ConstantValue evaluate(@NotNull Expression expression);

}
