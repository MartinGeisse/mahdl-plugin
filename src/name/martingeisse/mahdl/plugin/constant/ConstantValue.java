package name.martingeisse.mahdl.plugin.constant;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public abstract class ConstantValue {

	private ConstantValue() {
	}

	public abstract String getDataTypeDisplayString();
	public abstract String getDataTypeFamilyDisplayString();

	public abstract BigInteger convertToInteger();

	public static final class Bit extends ConstantValue {

		private final boolean set;

		Bit(boolean set) {
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

		public String getDataTypeDisplayString() {
			return "bit";
		}

		public String getDataTypeFamilyDisplayString() {
			return "bit";
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}
	}

	public static final class Vector extends ConstantValue {

		private final int size;
		private final BitSet bits;

		Vector(int size, BitSet bits) {
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

		public String getDataTypeDisplayString() {
			return "vector[" + size + "]";
		}

		public String getDataTypeFamilyDisplayString() {
			return "vector";
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
	}

	public static final class Memory extends ConstantValue {

		private final int firstSize, secondSize;
		private final BitSet bits;

		Memory(int firstSize, int secondSize, BitSet bits) {
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

		public String getDataTypeDisplayString() {
			return "memory[" + firstSize + "][" + secondSize + "]";
		}

		public String getDataTypeFamilyDisplayString() {
			return "memory";
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

	}

	public static final class Integer extends ConstantValue {

		private final BigInteger value;

		Integer(BigInteger value) {
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

		public String getDataTypeDisplayString() {
			return "integer";
		}

		public String getDataTypeFamilyDisplayString() {
			return "integer";
		}

		@Override
		public BigInteger convertToInteger() {
			return value;
		}
	}

	public static final class Text extends ConstantValue {

		private final String value;

		Text(String value) {
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

		public String getDataTypeDisplayString() {
			return "text";
		}

		public String getDataTypeFamilyDisplayString() {
			return "text";
		}

		@Override
		public BigInteger convertToInteger() {
			return null;
		}

	}

}
