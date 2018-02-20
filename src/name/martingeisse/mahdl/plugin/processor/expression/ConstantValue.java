/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public abstract class ConstantValue {

	private ConstantValue() {
	}

	@NotNull
	public abstract ProcessedDataType.Family getDataTypeFamily();

	@NotNull
	public abstract ProcessedDataType getDataType();

	@Nullable
	public abstract Boolean convertToBoolean();

	// note: treats vectors as unsigned
	@Nullable
	public abstract BigInteger convertToInteger();

	@NotNull
	public abstract String convertToString();

	@NotNull
	public abstract ConstantValue selectIndex(int index);

	@NotNull
	public abstract ConstantValue selectRange(int from, int to);

	public static final class Unknown extends ConstantValue {

		public static final Unknown INSTANCE = new Unknown();

		@Override
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.UNKNOWN;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Unknown.INSTANCE;
		}

		@Override
		@Nullable
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		@Nullable
		public BigInteger convertToInteger() {
			return null;
		}

		@NotNull
		@Override
		public String convertToString() {
			return "(unknown)";
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			return Unknown.INSTANCE;
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			return Unknown.INSTANCE;
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
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.BIT;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Bit.INSTANCE;
		}

		@Override
		@NotNull
		public Boolean convertToBoolean() {
			return set;
		}

		@Override
		@Nullable
		public BigInteger convertToInteger() {
			return null;
		}

		@NotNull
		@Override
		public String convertToString() {
			return set ? "1" : "0";
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			return Unknown.INSTANCE;
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			return Unknown.INSTANCE;
		}

	}

	// note: the Java BitSet uses the same index values as the MaHDL vector, just the from/to notation is reversed.
	public static final class Vector extends ConstantValue {

		private final int size;
		private final BitSet bits;

		public Vector(int size, BigInteger integerValue) {
			this(size, IntegerBitUtil.convertToBitSet(integerValue, size), false);
		}

		public Vector(int size, @NotNull BitSet bits) {
			this(size, bits, true);
		}

		private Vector(int size, @NotNull BitSet bits, boolean cloneBits) {
			if (bits.length() > size) {
				throw new IllegalArgumentException("bit set length " + bits.length() + " is greater than the vector size " + size);
			}
			this.size = size;
			this.bits = cloneBits ? (BitSet) bits.clone() : bits;
		}

		public int getSize() {
			return size;
		}

		/**
		 * Returns a new BitSet containing the bits from this vector. The returned BitSet is NOT backed by this
		 * vector and may be modified by the caller.
		 */
		@NotNull
		public BitSet getBits() {
			return (BitSet) bits.clone();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Vector) {
				Vector other = (Vector) obj;
				return size == other.size && bits.equals(other.bits);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(size).append(bits).toHashCode();
		}

		@Override
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.VECTOR;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return new ProcessedDataType.Vector(size);
		}

		@Override
		@Nullable
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		@NotNull
		public BigInteger convertToInteger() {
			return IntegerBitUtil.convertToUnsignedInteger(bits);
		}

		@NotNull
		public String getHexLiteral() {
			StringBuilder builder = new StringBuilder();
			int paddedLength = bits.length();
			if (paddedLength < 1) {
				return "0";
			}
			if (paddedLength % 4 != 0) {
				paddedLength = paddedLength + 4 - paddedLength % 4;
			}
			for (int index = paddedLength - 1; index >= 0; index -= 4) {
				int digitValue = (bits.get(index) ? 8 : 0) + (bits.get(index - 1) ? 4 : 0) +
					(bits.get(index - 2) ? 2 : 0) + (bits.get(index - 3) ? 1 : 0);
				builder.append(digitValue < 10 ? ((char) ('0' + digitValue)) : ((char) ('a' + digitValue - 10)));
			}
			return builder.toString();
		}

		@NotNull
		@Override
		public String convertToString() {
			return size + "h" + getHexLiteral();
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			if (index < 0 || index >= size) {
				return Unknown.INSTANCE;
			} else {
				return new Bit(bits.get(index));
			}
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			if (to < 0 || from < to || from >= size) {
				return Unknown.INSTANCE;
			}
			return new Vector(from - to + 1, bits.get(to, from), false);
		}

	}

	// TODO columns are stored in big endian order. What about rows?
	public static final class Matrix extends ConstantValue {

		private final int firstSize, secondSize;
		private final BitSet bits;

		public Matrix(int firstSize, int secondSize, @NotNull BitSet bits) {
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

		@NotNull
		public BitSet getBits() {
			return (BitSet) bits.clone();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Matrix) {
				Matrix other = (Matrix) obj;
				return firstSize == other.firstSize && secondSize == other.secondSize && bits.equals(other.bits);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(firstSize).append(secondSize).append(bits).toHashCode();
		}

		@Override
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.MATRIX;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return new ProcessedDataType.Matrix(firstSize, secondSize);
		}

		@Override
		@Nullable
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		@Nullable
		public BigInteger convertToInteger() {
			return null;
		}

		@NotNull
		@Override
		public String convertToString() {
			return "(matrix)";
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			if (index < 0 || index >= firstSize) {
				return Unknown.INSTANCE;
			}
			return new Vector(secondSize, bits.get(index * secondSize, (index + 1) * secondSize - 1), false);
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			return Unknown.INSTANCE;
		}

	}

	public static final class Integer extends ConstantValue {

		private final BigInteger value;

		public Integer(@NotNull BigInteger value) {
			this.value = value;
		}

		@NotNull
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
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.INTEGER;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Integer.INSTANCE;
		}

		@Override
		@Nullable
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		@NotNull
		public BigInteger convertToInteger() {
			return value;
		}

		@NotNull
		@Override
		public String convertToString() {
			return value.toString();
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			return Unknown.INSTANCE;
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			return Unknown.INSTANCE;
		}

	}

	public static final class Text extends ConstantValue {

		private final String value;

		public Text(@NotNull String value) {
			this.value = value;
		}

		@NotNull
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
		@NotNull
		public ProcessedDataType.Family getDataTypeFamily() {
			return ProcessedDataType.Family.TEXT;
		}

		@Override
		@NotNull
		public ProcessedDataType getDataType() {
			return ProcessedDataType.Text.INSTANCE;
		}

		@Override
		@Nullable
		public Boolean convertToBoolean() {
			return null;
		}

		@Override
		@Nullable
		public BigInteger convertToInteger() {
			return null;
		}

		@NotNull
		@Override
		public String convertToString() {
			return value;
		}

		@Override
		@NotNull
		public ConstantValue selectIndex(int index) {
			return Unknown.INSTANCE;
		}

		@Override
		@NotNull
		public ConstantValue selectRange(int from, int to) {
			return Unknown.INSTANCE;
		}

	}

}
