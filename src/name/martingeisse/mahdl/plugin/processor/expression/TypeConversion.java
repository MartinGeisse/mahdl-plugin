package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;

import java.math.BigInteger;
import java.util.BitSet;

/**
 * Represents a conversion from one type to another. In processed expressions, these conversions are made explicit.
 * <p>
 * To-text conversion is not represented by this class because it is not needed: Text values can only occur in
 * formally constant (sub-)expressions, and constant evaluation can convert to text implicitly since all implementations
 * of {@link ConstantValue} implement {@link ConstantValue#convertToString()}.
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

	private TypeConversion(ProcessedDataType dataType, ProcessedExpression operand) {
		this(operand.getErrorSource(), dataType, operand);
	}

	private TypeConversion(PsiElement errorSource, ProcessedDataType dataType, ProcessedExpression operand) {
		super(errorSource, dataType);
		this.operand = operand;
	}

	public ProcessedExpression getOperand() {
		return operand;
	}

	protected abstract ConstantValue perform(FormallyConstantEvaluationContext context, ConstantValue operandValue);

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return perform(context, operand.evaluateFormallyConstant(context));
	}

	public static final class BitToVector extends TypeConversion {

		public BitToVector(ProcessedExpression operand) throws TypeErrorException {
			super(new ProcessedDataType.Vector(1), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Bit)) {
				throw new TypeErrorException();
			}
		}

		public BitToVector(PsiElement errorSource, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Vector(1), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Bit)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Vector getDataType() {
			return (ProcessedDataType.Vector) super.getDataType();
		}

		@Override
		protected ConstantValue perform(FormallyConstantEvaluationContext context, ConstantValue operandValue) {
			if (operandValue instanceof ConstantValue.Bit) {
				BitSet bits = new BitSet();
				bits.set(0, ((ConstantValue.Bit) operandValue).isSet());
				return new ConstantValue.Vector(1, bits);
			} else {
				return context.evaluationInconsistency(this, "got wrong operand value: " + operandValue);
			}
		}

	}

	public static final class IntegerToVector extends TypeConversion {

		public IntegerToVector(int targetSize, ProcessedExpression operand) throws TypeErrorException {
			super(new ProcessedDataType.Vector(targetSize), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Integer)) {
				throw new TypeErrorException();
			}
		}

		public IntegerToVector(PsiElement errorSource, int targetSize, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Vector(targetSize), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Integer)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Vector getDataType() {
			return (ProcessedDataType.Vector) super.getDataType();
		}

		@Override
		protected ConstantValue perform(FormallyConstantEvaluationContext context, ConstantValue operandValue) {
			BigInteger integer = operandValue.convertToInteger();
			if (integer == null) {
				return context.evaluationInconsistency(this, "got wrong operand value: " + operandValue);
			} else {
				int size = getDataType().getSize();
				BitSet bits = IntegerBitUtil.convertToBitSet(integer, size);
				return new ConstantValue.Vector(size, bits);
			}
		}

	}

	public static final class VectorToInteger extends TypeConversion {

		public VectorToInteger(ProcessedExpression operand) throws TypeErrorException {
			super(new ProcessedDataType.Integer(), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Vector)) {
				throw new TypeErrorException();
			}
		}

		public VectorToInteger(PsiElement errorSource, ProcessedExpression operand) throws TypeErrorException {
			super(errorSource, new ProcessedDataType.Integer(), operand);
			if (!(operand.getDataType() instanceof ProcessedDataType.Vector)) {
				throw new TypeErrorException();
			}
		}

		@Override
		public ProcessedDataType.Integer getDataType() {
			return (ProcessedDataType.Integer) super.getDataType();
		}

		@Override
		protected ConstantValue perform(FormallyConstantEvaluationContext context, ConstantValue operandValue) {
			if (operandValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector operandVector = (ConstantValue.Vector)operandValue;
				return new ConstantValue.Integer(IntegerBitUtil.convertToUnsignedInteger(operandVector.getBits()));
			} else {
				return context.evaluationInconsistency(this, "got wrong operand value: " + operandValue);
			}
		}

	}

}
