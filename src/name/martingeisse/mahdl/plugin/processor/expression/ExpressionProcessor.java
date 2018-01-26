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

import java.util.BitSet;

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
				return process((Expression_IndexSelection) expression);
			} else if (expression instanceof Expression_RangeSelection) {
				return process((Expression_RangeSelection) expression);
			} else if (expression instanceof UnaryOperation) {
				return process((UnaryOperation) expression);
			} else if (expression instanceof BinaryOperation) {
				return process((BinaryOperation) expression);
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
						" must index a container vector of at least " + (1 << indexSize) + " in size, found " +
						containerSizeIfKnown);
				} else {
					return index;
				}
			}
		} else {
			return error(index, "cannot use an expression of type " + index.getDataType().getFamily() + " as index");
		}
	}

	private ProcessedExpression process(UnaryOperation expression) {
		ProcessedExpression operand = process(expression.getOperand());
		if (operand.getDataType() instanceof ProcessedDataType.Unknown) {
			return new UnknownExpression(expression);
		}
		ProcessedUnaryOperator operator = ProcessedUnaryOperator.from(expression);
		// unary operators have simple type handling -- we can even use the safety check and TypeErrorException to
		// detect errors without checking ourselves.
		try {
			return new ProcessedUnaryOperation(expression, operand, operator);
		} catch (TypeErrorException e) {
			return error(expression, "cannot apply operator " + operator + " to an operand of type " + operand.getDataType());
		}
	}

	private ProcessedExpression process(BinaryOperation expression) throws TypeErrorException {
		ProcessedExpression leftOperand = process(expression.getLeftOperand());
		ProcessedExpression rightOperand = process(expression.getRightOperand());
		if (leftOperand.getDataType() instanceof ProcessedDataType.Unknown || rightOperand.getDataType() instanceof ProcessedDataType.Unknown) {
			return new UnknownExpression(expression);
		}

		// handle concatenation operator -- it can have one of two entirely different meanings and has complex type handling
		if (expression instanceof Expression_BinaryConcat) {
			return handleConcatenation((Expression_BinaryConcat)expression, leftOperand, rightOperand);
		}
		ProcessedBinaryOperator operator = ProcessedBinaryOperator.from(expression);

		// now, only logical operators can handle bit values, and only if both operands are bits.
		if ((leftOperand.getDataType() instanceof ProcessedDataType.Bit) != (rightOperand.getDataType() instanceof ProcessedDataType.Bit)) {
			return error(expression, "this operator cannot be used for " + leftOperand.getDataType().getFamily() +
				" and " + rightOperand.getDataType().getFamily() + " operands");
		}
		if (leftOperand.getDataType() instanceof ProcessedDataType.Bit) {
			return new ProcessedBinaryOperation(expression, leftOperand, rightOperand, operator);
		}

		// all other binary operators are IVOs
		{
			boolean error = false;
			if (!(leftOperand.getDataType() instanceof ProcessedDataType.Vector) && !(leftOperand.getDataType() instanceof ProcessedDataType.Integer)) {
				error = true;
			}
			if (!(rightOperand.getDataType() instanceof ProcessedDataType.Vector) && !(rightOperand.getDataType() instanceof ProcessedDataType.Integer)) {
				error = true;
			}
			if (error) {
				return error(expression, "cannot apply operator " + operator + " to operands of type " + leftOperand.getDataType() + " and " + rightOperand.getDataType());
			}
		}

		// handle TAIVOs (shift operators) specially (no conversion; result type is that of the left operand)
		if (expression instanceof Expression_BinaryShiftLeft || expression instanceof Expression_BinaryShiftRight) {
			return new ProcessedBinaryOperation(expression, leftOperand, rightOperand, operator);
		}

		// handle TSIVOs
		if (leftOperand.getDataType() instanceof ProcessedDataType.Vector) {
			int leftSize = ((ProcessedDataType.Vector) leftOperand.getDataType()).getSize();
			if (rightOperand.getDataType() instanceof ProcessedDataType.Vector) {
				int rightSize = ((ProcessedDataType.Vector) rightOperand.getDataType()).getSize();
				if (leftSize != rightSize) {
					return error(expression, "cannot apply operator " + operator + " to vectors of different sizes " +
						leftSize + " and " + rightSize);
				}
			} else {
				rightOperand = new TypeConversion.IntegerToVector(leftSize, rightOperand);
			}
		} else {
			if (rightOperand.getDataType() instanceof ProcessedDataType.Vector) {
				int rightSize = ((ProcessedDataType.Vector) rightOperand.getDataType()).getSize();
				leftOperand = new TypeConversion.IntegerToVector(rightSize, leftOperand);
			}
		}
		return new ProcessedBinaryOperation(expression, leftOperand, rightOperand, operator);

	}

	private ProcessedExpression handleConcatenation(Expression_BinaryConcat expression,
													ProcessedExpression leftOperand,
													ProcessedExpression rightOperand) throws TypeErrorException {


		// handle text concatenation
		if (leftOperand.getDataType() instanceof ProcessedDataType.Text || rightOperand.getDataType() instanceof ProcessedDataType.Text) {
			return new ProcessedBinaryOperation(expression, leftOperand, rightOperand, ProcessedBinaryOperator.TEXT_CONCAT);
		}

		// handle bit / vector concatenation
		boolean typeError = false;
		if (leftOperand.getDataType() instanceof ProcessedDataType.Bit) {
			leftOperand = new TypeConversion.BitToVector(leftOperand);
		} else if (!(leftOperand.getDataType() instanceof ProcessedDataType.Vector)) {
			typeError = true;
		}
		if (rightOperand.getDataType() instanceof ProcessedDataType.Bit) {
			rightOperand = new TypeConversion.BitToVector(rightOperand);
		} else if (!(rightOperand.getDataType() instanceof ProcessedDataType.Vector)) {
			typeError = true;
		}
		if (typeError) {
			return error(expression, "cannot apply concatenation operator to operands of type " + leftOperand.getDataType() + " and " + rightOperand.getDataType());
		} else {
			return new ProcessedBinaryOperation(expression, leftOperand, rightOperand, ProcessedBinaryOperator.VECTOR_CONCAT);
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
