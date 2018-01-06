package name.martingeisse.mahdl.plugin.util;

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
	public static BitSet convertToBitSet(BigInteger value, int size) {
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

}