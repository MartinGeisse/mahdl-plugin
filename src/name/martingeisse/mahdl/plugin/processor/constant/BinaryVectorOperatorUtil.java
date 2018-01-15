package name.martingeisse.mahdl.plugin.processor.constant;

import name.martingeisse.mahdl.plugin.input.psi.BinaryOperation;

import java.math.BigInteger;
import java.util.BitSet;

/**
 * Helps evaluate binary expressions that operate on two same-sized vector values.
 */
class BinaryVectorOperatorUtil {

	static ConstantValue evaluate(BinaryOperation expression, int size, BitSet leftBits, BitSet rightBits) {
		// TODO

		// XOR, OR, AND -- all others work on numbers. And even those three can be performed on BigInteger
		// --> use only numbers; define operations on vectors to be those on their unsigned interpretation
		// (even unary minus) --> very simple language spec!

	}

}
