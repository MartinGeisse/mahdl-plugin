package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.LiteralParser;
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
			return process((ExtendedExpression_Switch) expression);
		} else {
			return error(expression, "unknown expression type");
		}
	}

	private ProcessedExpression process(ExtendedExpression_Switch expression) {
		// TODO
	}

	public ProcessedExpression process(Expression expression) {
		try {
			// TODO handle bit literals
			if (expression instanceof Expression_Literal) {
				return process((Expression_Literal) expression);
			} else if (expression instanceof Expression_Identifier) {
				return process((Expression_Identifier) expression);
			} else if (expression instanceof Expression_InstancePort) {
				// TODO
			} else if (expression instanceof Expression_IndexSelection) {
				return process((Expression_IndexSelection)expression);
			} else if (expression instanceof Expression_RangeSelection) {
				return process((Expression_RangeSelection)expression);
			} else if (expression instanceof UnaryOperation) {
				// TODO
			} else if (expression instanceof BinaryOperation) {
				// TODO
			} else if (expression instanceof Expression_Conditional) {
				// TODO
			} else if (expression instanceof Expression_FunctionCall) {
				// TODO
			} else if (expression instanceof Expression_Parenthesized) {
				return process(((Expression_Parenthesized) expression).getExpression());
			} else {
				return error(expression, "unknown expression type");
			}
		} catch (TypeErrorException e) {
			return error(expression, "internal error during type-check");
		}
	}

	private ProcessedExpression process(Expression_Literal expression) {
		try {
			return new ProcessedConstantValue(expression, LiteralParser.parseLiteral(expression));
		} catch (LiteralParser.ParseException e) {
			return error(expression, e.getMessage());
		}
	}

	private ProcessedExpression process(Expression_Identifier expression) {
		String name = expression.getIdentifier().getText();
		Named definition = localDefinitionResolver.getDefinition(name);
		if (definition == null) {
			return error(expression, "cannot resolve symbol '" + name + "'");
		} else if (definition instanceof SignalLike) {
			return new SignalLikeReference(expression, (SignalLike) definition);
		} else if (definition instanceof ModuleInstance) {
			return error(expression, "cannot use a module instance directly in an expression");
		} else {
			return error(expression, "symbol '" + name + "' does not refer to a signal-like");
		}
	}

	private ProcessedExpression process(Expression_IndexSelection expression) throws TypeErrorException {

		ProcessedExpression container = process(expression.getContainer());
		int containerSizeIfKnown = determineContainerSize(container, true, "index-select");

		ProcessedExpression index = process(expression.getIndex());
		index = handleIndex(index, containerSizeIfKnown);

		if (containerSizeIfKnown == -1 || index instanceof UnknownExpression) {
			return new UnknownExpression(expression);
		} else {
			if (container.getDataType() instanceof ProcessedDataType.Vector) {
				return new ProcessedIndexSelection.BitFromVector(expression, container, index);
			} else if (container.getDataType() instanceof ProcessedDataType.Memory) {
				return new ProcessedIndexSelection.VectorFromMemory(expression, container, index);
			} else {
				return error(expression, "unknown container type");
			}
		}

	}

	private ProcessedExpression process(Expression_RangeSelection expression) throws TypeErrorException {

		ProcessedExpression container = process(expression.getContainer());
		int containerSizeIfKnown = determineContainerSize(container, false, "range-select");

		ProcessedExpression fromIndex = process(expression.getFrom());
		if (!(fromIndex.getDataType() instanceof ProcessedDataType.Integer)) {
			fromIndex = error(expression.getFrom(), "from-index must be of type integer, found " + fromIndex.getDataType());
		}

		ProcessedExpression toIndex = process(expression.getTo());
		if (!(toIndex.getDataType() instanceof ProcessedDataType.Integer)) {
			toIndex = error(expression.getTo(), "to-index must be of type integer, found " + toIndex.getDataType());
		}

		if (containerSizeIfKnown == -1 || fromIndex instanceof UnknownExpression || toIndex instanceof UnknownExpression) {
			return new UnknownExpression(expression);
		} else {
			if (container.getDataType() instanceof ProcessedDataType.Vector) {
				// TODO need constant evaluation here!
				return new ProcessedRangeSelection(expression, dataType, container, fromIndex, toIndex);
			} else {
				return error(expression, "unknown container type");
			}
		}

	}

	private int determineContainerSize(@NotNull ProcessedExpression container, boolean allowMemory, String operatorVerb) {
		ProcessedDataType type = container.getDataType();
		if (type instanceof ProcessedDataType.Unknown) {
			return -1;
		} else if (type instanceof ProcessedDataType.Vector) {
			return ((ProcessedDataType.Vector) type).getSize();
		} else if (allowMemory && type instanceof ProcessedDataType.Memory) {
			return ((ProcessedDataType.Memory) type).getFirstSize();
		} else {
			error(container, "cannot " + operatorVerb + " from an expression of type " + type.getFamily().getDisplayString());
			return -1;
		}
	}

	private ProcessedExpression handleIndex(@NotNull ProcessedExpression index, int containerSizeIfKnown) {
		if (index.getDataType() instanceof ProcessedDataType.Integer) {
			// For an integer, the actual value is relevant, so non-PO2-sized containers can be indexed with a
			// constant index without getting errors. There won't be an error based on the type alone nor a type
			// conversion.
			return index;
		} else if (index.getDataType() instanceof ProcessedDataType.Vector) {
			if (containerSizeIfKnown < 0) {
				return new UnknownExpression(index.getErrorSource());
			} else {
				// For a vector, the greatest possible value is releant, not the actual value, even if the vector is
				// constant (see language design documents for details).
				int indexSize = ((ProcessedDataType.Vector) index.getDataType()).getSize();
				if (containerSizeIfKnown < (1 << indexSize)) {
					return error(index, "index of vector size " + indexSize +
						" must index a container vector of at least " + (1 << indexSize) + " in size, found "+
						containerSizeIfKnown);
				} else {
					return index;
				}
			}
		} else {
			return error(index, "cannot use an expression of type " + index.getDataType().getFamily() + " as index");
		}
	}

	@NotNull
	private ProcessedExpression error(@NotNull ProcessedExpression processedExpression, @NotNull String message) {
		return error(processedExpression.getErrorSource(), message);
	}

	@NotNull
	private ProcessedExpression error(@NotNull PsiElement errorSource, @NotNull String message) {
		errorHandler.onError(errorSource, message);
		return new UnknownExpression(errorSource);
	}

}
