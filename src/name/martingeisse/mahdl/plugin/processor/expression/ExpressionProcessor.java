package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression_Normal;
import name.martingeisse.mahdl.plugin.input.psi.ExtendedExpression_Switch;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ExpressionProcessor {

	private final ErrorHandler errorHandler;
	private final LocalDefinitionResolver localDefinitionResolver;

	public ExpressionProcessor(ErrorHandler errorHandler, LocalDefinitionResolver localDefinitionResolver) {
		this.errorHandler = errorHandler;
		this.localDefinitionResolver = localDefinitionResolver;
	}

	public ProcessedExpression process(ExtendedExpression expression) {
		if (expression instanceof ExtendedExpression_Normal) {
			return process(((ExtendedExpression_Normal) expression).getExpression());
		} else if (expression instanceof ExtendedExpression_Switch) {
			return process((ExtendedExpression_Switch)expression);
		} else {
			return error(expression, "unknown expression type");
		}
	}

	private ProcessedExpression process(ExtendedExpression_Switch expression) {
		// TODO
	}

	public ProcessedExpression process(Expression expression) {
		// TODO
	}

	@NotNull
	private ProcessedExpression error(@NotNull PsiElement element, @NotNull String message) {
		errorHandler.onError(element, message);
		return UnknownExpression.INSTANCE;
	}

}
