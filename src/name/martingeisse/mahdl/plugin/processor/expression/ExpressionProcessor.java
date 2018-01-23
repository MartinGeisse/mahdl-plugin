package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.ModuleInstance;
import name.martingeisse.mahdl.plugin.processor.definition.Named;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.LiteralParser;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

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
		// TODO handle bit literals
		if (expression instanceof Expression_Literal) {
			return process((Expression_Literal) expression);
		} else if (expression instanceof Expression_Identifier) {
			return process((Expression_Identifier) expression);
		} else if (expression instanceof Expression_InstancePort) {
			// TODO
		} else if (expression instanceof Expression_IndexSelection) {
			// TODO
		} else if (expression instanceof Expression_RangeSelectionFixed) {
			// TODO
		} else if (expression instanceof Expression_RangeSelectionUpwards) {
			// TODO
		} else if (expression instanceof Expression_RangeSelectionDownwards) {
			// TODO
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

	private ProcessedExpression process(Expression_IndexSelection expression) {

		ProcessedExpression container = process(expression.getContainer());
		int containerSizeIfKnown = determineContainerSize(container, true, "index-select");

		ProcessedExpression index = process(expression.getIndex());
		index = handleIndex(index, containerSizeIfKnown);

		if (containerSizeIfKnown == -1 || index instanceof UnknownExpression) {
			return new UnknownExpression(expression);
		} else {
			// TODO determine output type

			// TODO difference between constant and non-constant?
			//
			// signal vector[6] foo = ...;
			// signal bit bar = vector[5]; // this is okay
			// signal bit baz = vector[3d5]; // unclear! The index is valid but its type could select out-of-range.
			//
			// There shouldn't be a difference between constant and non-constant here (otherwise depends on folding,
			// which is informal), so should probably reject the second one based on its *type*, not accept it based
			// on its *value*.
			//

			return new ProcessedIndexSelection(expression, dataType, container, index);
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
		// TODO insert type conversions
		if (index.getDataType() instanceof ProcessedDataType.Integer) {
			return index;
		} else if (index.getDataType() instanceof ProcessedDataType.Vector) {
			if (containerSizeIfKnown < 0) {
				return new UnknownExpression(index.getErrorSource());
			} else {
				// It is an error for the index size to be too large, even if the index is a constant whose actual
				// value is within bounds.
				int indexSize = ((ProcessedDataType.Vector) index.getDataType()).getSize();
				// TODO ??? if (containerSizeIfKnown < (1 << indexSize))
				return
			}
		} else {
			return error(index, "cannot use an expression of type " + index.getDataType().getFamily() + " as index");
		}


		BigInteger numericIndexValue = indexValue.convertToInteger();
		if (numericIndexValue == null) {
			error(indexExpression, "value of type  " + indexValue.getDataTypeFamily().getDisplayString() + " cannot be converted to integer");
			return -1;
		}
		int intValue = numericIndexValue.intValue();
		if (intValue >= containerSize) {
			error(indexExpression, "index " + numericIndexValue + " is out of bounds for type " + containerType);
			return -1;
		}
		return intValue;
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
