
# Operators

This section defines the operators available in MaHDL.

The following definitions are used as helpers:
* Conversion of a vector to an integer is the interpretation as unsigned binary digits. There are no signed vectors.
* Conversion of an integer to a vector of size N is taking the N lowest-order bits of the two's complement
representation of that integer.
* It is a compile-time error if a constant sub-expression of integer type is converted to a vector of some size whose
value is different from the original value of that sub-expression (this handles overflow or underflow in implicit
constant conversion).

## Type-Symmetric Integer/Vector Operators

TSIVOs are binary operators that accept as operands either
* two integers, producing an integer or boolean result using the integer meaning of the operator
* two vectors of the same size, producing a vector of that size or a boolean, by taking the integer meaning of the
operator on the integer meaning of the operands, and converting the result back to a vector (if non-boolean)
* an integer and a vector, which converts the integer to a vector of the same size as the other vector (with possible
compile-time overflow errors) and then acting on the two vectors.

These operators are "type-symmetric" in that their behavior with respect to types treats both operands equally. They
are "Integer/Vector" operators in that their meaning is defined in terms of the equivalent meaning of integers and
vectors.

## Type-Asymmetric Integer/Vector Operators

TAIVOs are also based on the equivalent meaning of integers and vectors, but they are not symmetric towards their
operands.

### Shift-left and shift-right operators

All shifted-in bits are 0.

The result type is the same as the type of the left operand. If the left operand is an integer, then only the
integer meaning of the right operand is used. TODO left vector

## Integer/Vector Operators

This section deals with Integer/Vector operators, which are a subset of the operators in MaHDL. The binary plus operator
(addition) is an example for such an operator. As a counter-example, the concatenation operator falls outside the rules
defined here, since it treats vectors solely as bit patterns and has no meaning at all in terms of integers.

Observe that most operators are well-defined for integer operands, producing an integer result. This is even the case
for the bitwise operators AND, OR and XOR, using the definitions of those operators from BigInteger. Based on this, we
want:
* a vector to have a well-defined meaning as an integer
* the effect of an operator on vectors to be compatible with the effect on integers
* a constant expression using only vectors to produce the same value, whether it is folded at compile-time or evaluated
at run-time
* an expression using a vector and an integer to produce the same value, whether whether it is folded at compile-time
(only possible if constant) or evaluated at run-time by turning the integer into a vector implicitly.

The last rule implies that in an expression involving a vector and an integer, such as (myVector + 1), the integer must
have a well-defined meaning as a vector. We therefore add that we want:

* the integer meaning of a vector and the vector meaning of an integer to be the inverse of each other

Since there are only finitely many vector values for a given vector size, but infinitely many integers, turning an
integer into a vector can change the result of the expression in a non-obvious way. Also, the two relations above cannot
really be the inverse if one of them is ambiguous. We therefore add that we want:

* one of the integers associated with a vector value to be canonical, and defined to be *the* integer meaning of that
vector.

We achieve all this using the following rules:

TODO
* **The canonical integer meaning of a vector is its unsigned integer interpretation. There are no signed vectors.** 
* **An IVO always acts on the integer meaning of its operands. **
* **An IVO produces either a boolean or integer intermediate result. If the intermediate result is boolean, this is also
the final result. Otherwise, the operands are consulted to determine the result vector size. It is operator-specific
which operands are consulted. If all consulted operands are integers, then the final result is equal to the intermediate
result. Otherwise the determined result vector size must be unambiguous -- called N here -- and the final result is
obtained by converting the intermediate result to a vector of size N by taking the N lowest-order bits of the
intermediate integer result.**
* **Since IVOs always work on the integer meaning of a vector -- which is unsigned -- there are no IVOs that treat
vectors as signed. Any negative operand must be an integer, not a vector.**

For example, the binary plus operator adds its operand values. If both operands are vectors, their size must be the
same, or a compiler error occurs. N is defined to be that size, or if only one operand is a vector, equal to that
vector's size. This truncates the result of addition to the size of the operands, possibly resulting in silent overflow.


Observations:

* The unsigned interpretation makes some expressions pointless, such as

      result1 = (myVector < 0);
      constant integer foo = -5;
      result2 = (myVector == foo);

  Both expressions are always false. It is nevertheless allowed by the language, but
  the compiler should produce a warning here.

* The unary minus operator seems special at first in that we need to treat its
operand as signed. This isn't the case though; trimming the result to the same
size as the operand causes the distinction to disappear. Another way to see it is
that this operator can be defined without reference to signed-ness, as
 
        -n := ~n + 1

## Non-IVO Operators

TODO change wording -- not exactly true this way

We do not want operators to be exempt from the way we treat IVOs. So there won't be
any operators that treat vectors as signed, or treat integers as vectors, or other
behavior that we wanted to avoid. Non-IVO operators are a different thing: They are
operators whose meaning cannot usefully be defined in terms of the integer values
of their operands. Consequently, they do not convert vector operands to integer
operands or vice versa, but act on their value in specific ways:

* **Concatenation Operator: If either operand is a text value, then the other operand
is turned into a text value, and the result is the concatenated text value. Otherwise,
both operands must be either bits or vectors, and the result is a vector containing
the concatenated bits from both values.**
