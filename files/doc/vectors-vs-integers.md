
# Vectors and Integers

Observe that most operators are well-defined for integer operands, producing
an integer result. This is even the case for the bitwise operators AND, OR and
XOR, using the definitions of those operators from BigInteger. We want the effect
of those operators on vectors to be compatible with the effect on integers. We'll
also need a clear definition on how to interpret a vector. We achieve this using
the following rules:

* **A vector is defined to be equivalent to its unsigned interpretation as an
integer. There are no signed vectors. Since operators always work on the integer
meaning of a vector -- which is unsigned -- there are no operators that treat
vectors as signed. Any negative operand must be an integer, not a vector.**
* **An operator always acts on the integer meaning of its operands, using the
above definition to interpret vectors, and produces either a boolean or integer
intermediate result.**
* **If the intermediate result is boolean, this is also the final result. If it
is an integer, and both operands are integers, then it too is the final result.
Otherwise either operand is a vector (or both are vectors), and the intermediate
result is an integer. This integer is converted to a vector of size N by taking
the N lowest-order bits of the integer result, where N is determined in an
operator-specific way.**

For example, the binary plus operator adds its operand values. If both operands
are vectors, their size must be the same, or a compiler error occurs. N is defined
to be that size, or if only one operand is a vector, equal to that vector's size.
This truncates the result of addition to the size of the operands, possibly
resulting in silent overflow.


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
