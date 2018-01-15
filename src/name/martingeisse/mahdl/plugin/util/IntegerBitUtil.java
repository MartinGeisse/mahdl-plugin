/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.util;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public final class IntegerBitUtil {

	// prevent instantiation
	private IntegerBitUtil() {
	}

	/**
	 * Returns a BitSet with the (size) lowest bits from the two's complement representation of the specified value.
	 *
	 * TODO test if this works correctly for negative values! The definition is OK, but the implementation is untested!
	 */
	@NotNull
	public static BitSet convertToBitSet(@NotNull BigInteger value, int size) {
		final BitSet bits = new BitSet(size);
		int bitIndex = 0;
		while (bitIndex < size) {
			if (value.testBit(bitIndex)) {
				bits.set(bitIndex);
			}
			bitIndex++;
		}
		return bits;
	}

	@NotNull
	public static BigInteger convertToInteger(@NotNull BitSet bits, int size) {
		int index = 0;
		BigInteger significance = BigInteger.ONE;
		BigInteger result = BigInteger.ZERO;
		while (index < size - 1) {
			if (bits.get(index)) {
				result = result.add(significance);
				index++;
				significance = significance.shiftLeft(1);
			}
		}
		if (bits.get(size - 1)) {
			// two's complement: the sign bit has negated significance
			result = result.subtract(significance);
		}
		return result;
	}

	// TODO we possibly only need unsigned conversion, so we could simplify this code. Signed conversion is currently
	// only needed for numeric negation (unary minus), which would work well using unsigned conversion. We *should*
	// make sure that the bitset used in a constant vector value is restricted to the vector size, and also that it is
	// immutable!
	@NotNull
	public static BigInteger convertToUnsignedInteger(@NotNull BitSet bits) {
		return convertToInteger(bits, bits.length() + 1);
	}

}
