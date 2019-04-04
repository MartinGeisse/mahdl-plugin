/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.util;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.BitSet;

/**
 *
 */
public class IntegerBitUtilTest {

	@Test
	public void testConvertToBitSetNegative() {
		Assert.assertEquals(buildBitSet(), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 0));
		Assert.assertEquals(buildBitSet(true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 1));
		Assert.assertEquals(buildBitSet(true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 2));
		Assert.assertEquals(buildBitSet(false, true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 3));
		Assert.assertEquals(buildBitSet(false, false, true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 4));
		Assert.assertEquals(buildBitSet(true, false, false, true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 5));
		Assert.assertEquals(buildBitSet(true, true, false, false, true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 6));
		Assert.assertEquals(buildBitSet(true, true, true, false, false, true, true), IntegerBitUtil.convertToBitSet(BigInteger.valueOf(-13), 7));
	}

	private BitSet buildBitSet(boolean... bits) {
		BitSet bitSet = new BitSet();
		for (int i = 0; i < bits.length; i++) {
			int index = bits.length - 1 - i;
			bitSet.set(index, bits[i]);
		}
		return bitSet;
	}

}
