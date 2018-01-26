
# Operators

This section defines the operators available in MaHDL.

The following definitions are used as helpers:
* Conversion of a vector to an integer is the interpretation as unsigned binary digits. There are no signed vectors.
* Conversion of an integer to a vector of size N is taking the N lowest-order bits of the two's complement
representation of that integer.
* It is a compile-time error if a constant sub-expression of integer type is converted to a vector whose integer
value is different from the original value of that sub-expression. Put differently: overflow/underflow is not allowed
in constant integer-to-vector conversion.

Observations:
* The unary minus operator seems special at first in that we need to treat its
operand as signed. This isn't the case though; trimming the result to the same
size as the operand causes the distinction to disappear. Another way to see it is
that this operator can be defined without reference to signed-ness, as
 
        -n := ~n + 1


## Type-Symmetric Integer/Vector Operators

TSIVOs are binary operators that accept any combination of integers and vectors as operands. The meaning of TSIVOs
is defined in terms of integer operands, but an actual operand may be subject to type conversion before the TSIVO
acts on it, even if it already is an integer. Some TSIVOs produce a boolean result; the others produce either an
integer or vector, depending on the operands.

These operators are "type-symmetric" in that their behavior with respect to types treats both operands equally. They
are "Integer/Vector" operators in that their meaning is defined in terms of the equivalent meaning of integers and
vectors.

TSIVOs accept as operands either
* two integers. No type conversion is performed on the operands. The result is the integer or boolean result of the
integer meaning of the operator.
* two vectors of the same size. The integer meaning of the operator on the integer meaning of its operands is taken, and
if the result is an integer (not a boolean), then it is converted to the vector type of the operands.
* an integer and a vector. The integer is converted to the same type as the vector. The operator is then applied to
the integer meaning of both vectors. If the result is an integer (not a boolean), then the integer result is again
converted to the same vector type as the operands.

Using a TSIVO on two vectors of different size is a compile-time error.

The double conversion step for mixed integer/vector operands is intentional:
* Converting an integer operand to a vector ensures that the behavior of an expression stays the same, no matter
whether it is constant-folded at compile time or evaluated at runtime. If the conversion causes an overflow, the
rules dictate that a compile-time error is reported.
* Converting the result is needed since the result cannot be an integer at run-time. Conversion to the operand size
specifically makes the distinction between signed and unsigned disappear for various operators (especially addition,
subtraction and multiplication). The distinction can be made manually by extending the operators manually first.



## Type-Asymmetric Integer/Vector Operators

TAIVOs are also based on the equivalent meaning of integers and vectors, but they are not symmetric towards their
operands.

Currently, the shift-left and shift-right operators are the only TAIVOs. These act on their operands in the following
way:
* if the left operand is an integer, the right operand must be constant, making the whole expression constant.
* the shifted value is taken from the integer meaning of the left operand, without any further conversion.
* the shift amount is the integer meaning of the right operand, without any further conversion. A negative shift
amount shifts to the opposite direction.
* the result type is that of the left operand

Observations and rationale:
* If the left-hand operand is an integer, then we don't have an "obvious" vector size to impose on it as we have for
TSIVOs, nor do we have an obvious result vector size. Without a vector size, we cannot implement a run-time shift, so
we demand that the whole expression is constant and perform the shift purely on the integer meaning of the operands,
without any further conversion, at compile-time.
* If the left-hand operand is a vector, it cannot be negative. An "arithmetic" (sign-extending) right-shift is
therefore not supported directly. All shifted-in bits are 0.
* There is no "wrap-around" for the shift amount like some languages do, e.g. Java. That is, if the left-hand operand
is a vector and the right-hand operand is equal to the size of the left-hand operand, the result is a zero vector,
because the operator shifts by (left-operand-size) digits which is *not* equivalent to shifting by zero digits.
* Although a ngative right-hand operand inverts the shift direction, arbitrary inversion cannot happen at run-time since
a right-hand integer must be constant (either always or never inverts the shift direction, and is a "constant shift"
anyway), and a right-hand vector cannot be negative.

## Other Operators

These operators do not treat integers and vectors as equivalent:

* Concatenation Operator: If either operand is a text value, then the other operand is turned into a text value, and
the result is the concatenated text value. Otherwise, both operands must be either bits or vectors, and the result is
a vector containing the concatenated bits from both values.
