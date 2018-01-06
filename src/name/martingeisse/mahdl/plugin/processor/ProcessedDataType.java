package name.martingeisse.mahdl.plugin.processor;

import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.util.IntegerBitUtil;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 *
 */
public abstract class ProcessedDataType {

	public enum Family {

		BIT, VECTOR, MEMORY, INTEGER, TEXT, UNKNOWN;

		public String getDisplayString() {
			return name().toLowerCase();
		}

	}

	public abstract Family getFamily();
	public abstract ConstantValue convertValueImplicitly(ConstantValue inputValue);

	public static final class Unknown extends ProcessedDataType {

		public static final Unknown INSTANCE = new Unknown();

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Unknown;
		}

		@Override
		public int hashCode() {
			return Unknown.class.hashCode();
		}

		public String toString() {
			return "unknown";
		}

		@Override
		public Family getFamily() {
			return Family.UNKNOWN;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			return ConstantValue.Unknown.INSTANCE;
		}

	}

	public static final class Bit extends ProcessedDataType {

		public static final Bit INSTANCE = new Bit();

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Bit;
		}

		@Override
		public int hashCode() {
			return Bit.class.hashCode();
		}

		public String toString() {
			return "bit";
		}

		@Override
		public Family getFamily() {
			return Family.BIT;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			return inputValue instanceof ConstantValue.Bit ? inputValue : ConstantValue.Unknown.INSTANCE;
		}

	}

	// note: the Java BitSet uses the same index values as the MaHDL vector, just the from/to notation is reversed.
	public static final class Vector extends ProcessedDataType {

		private final int size;

		public Vector(int size) {
			this.size = size;
		}

		public int getSize() {
			return size;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Vector) {
				Vector other = (Vector)obj;
				return size == other.size;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(Vector.class).append(size).toHashCode();
		}

		public String toString() {
			return "vector[" + size + "]";
		}

		@Override
		public Family getFamily() {
			return Family.VECTOR;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			if (inputValue instanceof ConstantValue.Vector) {
				ConstantValue.Vector vector = (ConstantValue.Vector)inputValue;
				if (vector.getSize() == size) {
					return inputValue;
				} else {
					// no automatic truncating because it just causes bugs, and no automatic expansion because we
					// don't know how to fill (e.g. could be zero-extend, one-extend, sign-extend, ...)
					return ConstantValue.Unknown.INSTANCE;
				}
			} else if (inputValue instanceof ConstantValue.Integer) {
				ConstantValue.Integer integer = (ConstantValue.Integer)inputValue;
				if (integer.getValue().bitLength() > size) {
					// no automatic truncating
					return ConstantValue.Unknown.INSTANCE;
				}
				return new ConstantValue.Vector(size, IntegerBitUtil.convertToBitSet(integer.getValue(), size));
			} else {
				return ConstantValue.Unknown.INSTANCE;
			}
		}
	}

	public static final class Memory extends ProcessedDataType {

		private final int firstSize, secondSize;

		public Memory(int firstSize, int secondSize) {
			this.firstSize = firstSize;
			this.secondSize = secondSize;
		}

		public int getFirstSize() {
			return firstSize;
		}

		public int getSecondSize() {
			return secondSize;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Memory) {
				Memory other = (Memory)obj;
				return firstSize == other.firstSize && secondSize == other.secondSize;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(Memory.class).append(firstSize).append(secondSize).toHashCode();
		}

		public String toString() {
			return "memory[" + firstSize + "][" + secondSize + "]";
		}

		@Override
		public Family getFamily() {
			return Family.MEMORY;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			if (inputValue instanceof ConstantValue.Memory) {
				ConstantValue.Memory memory = (ConstantValue.Memory)inputValue;
				if (memory.getFirstSize() == firstSize && memory.getSecondSize() == secondSize) {
					return memory;
				}
			}
			return ConstantValue.Unknown.INSTANCE;
		}

	}

	public static final class Integer extends ProcessedDataType {

		public static final Integer INSTANCE = new Integer();

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Integer;
		}

		@Override
		public int hashCode() {
			return Integer.class.hashCode();
		}

		public String toString() {
			return "integer";
		}

		@Override
		public Family getFamily() {
			return Family.INTEGER;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			if (inputValue instanceof ConstantValue.Integer) {
				return inputValue;
			} else {
				return ConstantValue.Unknown.INSTANCE;
			}
		}

	}

	public static final class Text extends ProcessedDataType {

		public static final Text INSTANCE = new Text();

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Text;
		}

		@Override
		public int hashCode() {
			return Text.class.hashCode();
		}

		public String toString() {
			return "text";
		}

		@Override
		public Family getFamily() {
			return Family.TEXT;
		}

		@Override
		public ConstantValue convertValueImplicitly(ConstantValue inputValue) {
			if (inputValue instanceof ConstantValue.Text) {
				return inputValue;
			} else {
				return ConstantValue.Unknown.INSTANCE;
			}
		}

	}


}
