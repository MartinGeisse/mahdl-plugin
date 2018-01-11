package name.martingeisse.mahdl.plugin.processor.constant;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public abstract class ConstantValue {

	private ConstantValue() {
	}

	public abstract ProcessedDataType.Family getDataTypeFamily();
	public abstract ProcessedDataType getDataType();

	public abstract Boolean convertToBoolean();
	public abstract BigInteger convertToInteger();
	public abstract ConstantValue selectIndex(int index);
	public abstract ConstantValue selectRange(int from, int to);

	public static final class Unknown extends ConstantValue {

		public static final Unknown INSTANCE = new Unknown();

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.UNKNOWN;
		}

		@Override
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Unknown.INSTANCE;
		}

		@Override
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			return null;
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			return null;
		}

	}

	public static final class Bit extends ConstantValue {

		private final boolean set;

		public Bit(boolean set) {
			this.set = set;
		}

		public boolean isSet() {
			return set;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Bit && set == ((Bit) obj).set;
		}

		@Override
		public int hashCode() {
			return Boolean.hashCode(set);
		}

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.BIT;
		}

		@Override
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Bit.INSTANCE;
		}

		@Override
		public Boolean convertToBoolean() {
			return set;
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			return null;
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			return null;
		}

	}

	// note: the Java BitSet uses the same index values as the MaHDL vector, just the from/to notation is reversed.
	public static final class Vector extends ConstantValue {

		private final int size;
		private final BitSet bits;

		public Vector(int size, BitSet bits) {
			this.size = size;
			this.bits = bits;
		}

		public int getSize() {
			return size;
		}

		public BitSet getBits() {
			return (BitSet)bits.clone();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Vector) {
				Vector other = (Vector)obj;
				return size == other.size && bits.equals(other.bits);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(size).append(bits).toHashCode();
		}

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.VECTOR;
		}

		@Override
		public ProcessedDataType getDataType() {
			return new ProcessedDataType.Vector(size);
		}

		@Override
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		public BigInteger convertToInteger() {
			final int length = bits.length();
			int index = 0;
			BigInteger significance = BigInteger.ONE;
			BigInteger result = BigInteger.ZERO;
			while (index < length) {
				if (bits.get(index)) {
					result = result.add(significance);
					index++;
					significance = significance.shiftLeft(1);
				}
			}
			return result;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			if (index < 0 || index >= size) {
				return null;
			} else {
				return new Bit(bits.get(index));
			}
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			if (to < 0 || from < to || from >= size) {
				return null;
			}
			return new Vector(from - to + 1, bits.get(to, from));
		}

	}

	public static final class Memory extends ConstantValue {

		private final int firstSize, secondSize;
		private final BitSet bits;

		public Memory(int firstSize, int secondSize, BitSet bits) {
			this.firstSize = firstSize;
			this.secondSize = secondSize;
			this.bits = bits;
		}

		public int getFirstSize() {
			return firstSize;
		}

		public int getSecondSize() {
			return secondSize;
		}

		public BitSet getBits() {
			return (BitSet)bits.clone();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Memory) {
				Memory other = (Memory)obj;
				return firstSize == other.firstSize && secondSize == other.secondSize && bits.equals(other.bits);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(firstSize).append(secondSize).append(bits).toHashCode();
		}

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.MEMORY;
		}

		@Override
		public ProcessedDataType getDataType() {
			return new ProcessedDataType.Memory(firstSize, secondSize);
		}

		@Override
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			if (index < 0 || index >= firstSize) {
				return null;
			}
			return new Vector(secondSize, bits.get(index * secondSize, (index + 1) * secondSize - 1));
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			return null;
		}

	}

	public static final class Integer extends ConstantValue {

		private final BigInteger value;

		public Integer(BigInteger value) {
			this.value = value;
		}

		public BigInteger getValue() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Integer && value.equals(((Integer) obj).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.INTEGER;
		}

		@Override
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Integer.INSTANCE;
		}

		@Override
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		public BigInteger convertToInteger() {
			return value;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			return null;
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			return null;
		}

	}

	public static final class Text extends ConstantValue {

		private final String value;

		public Text(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Text && value.equals(((Text) obj).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.TEXT;
		}

		@Override
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Text.INSTANCE;
		}

		@Override
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

		@Override
		public ConstantValue selectIndex(int index) {
			return null;
		}

		@Override
		public ConstantValue selectRange(int from, int to) {
			return null;
		}

	}

}
