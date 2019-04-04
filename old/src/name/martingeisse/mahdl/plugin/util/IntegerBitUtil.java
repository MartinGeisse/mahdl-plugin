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
	public static BigInteger convertToSignedInteger(@NotNull BitSet bits, int size) {
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

	@NotNull
	public static BigInteger convertToUnsignedInteger(@NotNull BitSet bits) {
		return convertToSignedInteger(bits, bits.length() + 1);
	}

}
