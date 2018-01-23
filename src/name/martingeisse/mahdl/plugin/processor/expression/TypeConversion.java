package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 * Represents a conversion from one type to another. In processed expressions, these conversions are made explicit.
 * <p>
 * Special case: Using 0 or 1 as a bit literal is NOT handled by treating it as an integer literal with a conversion.
 * Instead, these are recognized by the {@link ExpressionProcessor} and turned into a bit literal. Actual bit
 * literals therefore only exist in processed expressions, not on syntax level. The reason is that we generally do
 * NOT want an implicit conversion from integer to bit -- these conversions only make sense for 0 or 1, and we still
 * don't want to allow them for any integer expression that turns out to be 0 or 1. We only want to allow using 0
 * or 1 directly as literals. To make this rule useful, such bit literals are not allowed in any ambiguous or
 * non-obvious case.
 */
public abstract class TypeConversion extends ProcessedExpression {

	private final ProcessedExpression operand;

	private TypeConversion(PsiElement errorSource, ProcessedDataType dataType, ProcessedExpression operand) {
		super(errorSource, dataType);
		this.operand = operand;
	}

	public ProcessedExpression getOperand() {
		return operand;
	}

	public static final class ToText extends TypeConversion {

		public ToText(PsiElement errorSource, ProcessedExpression operand) {
			super(errorSource, ProcessedDataType.Text.INSTANCE, operand);
		}

		@Override
		public ProcessedDataType.Text getDataType() {
			return (ProcessedDataType.Text)super.getDataType();
		}
	}

	public static final class BitToVector extends TypeConversion {

		public BitToVector(PsiElement errorSource, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Vector(1), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Bit)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Vector getDataType() {
			return (ProcessedDataType.Vector)super.getDataType();
		}
	}

	public static final class IntegerToVector extends TypeConversion {

		public IntegerToVector(PsiElement errorSource, int targetSize, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Vector(targetSize), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Integer)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Vector getDataType() {
			return (ProcessedDataType.Vector)super.getDataType();
		}
	}

	public static final class VectorToInteger extends TypeConversion {

		public VectorToInteger(PsiElement errorSource, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Integer(), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Vector)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Integer getDataType() {
			return (ProcessedDataType.Integer)super.getDataType();
		}
	}

}