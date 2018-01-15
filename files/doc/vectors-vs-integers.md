
# Vectors and Integers

MaHDL allows to use both vectors and integers in a module definition. At
run-time, only vectors are used. When used in run-time expressions, integers
therefore only provide a convenient way to specify vectors, and all
run-time operators are defined for vectors only. 

On the other hand, many operators such as "plus" or "less-than" are defined in
terms of integers. These operators are defined for vectors based on their
definitions in terms of integers. Since those operators can be used on constant
expressions involving integers, we'll want them to have the same effect whether
we use them on integers or equivalent vectors. We achieve this by the
following basic Rules:

* **Operators are defined for two integer operands based on their integer values.
This definition does not rely on an understanding of vectors.**
* **Operators are defined for two operands, each of which is either an integer or
a vector, by turning vectors into integers, then applying the integer meaning
of the operand. The vector is interpreted as an unsigned representation of all
nonzero lowest-order bits of the integer. If either operand is a vector, the
result is a vector whose size is determined by the operands.**

Observations:

* The unsigned interpretation makes some expressions pointless, such as

      result1 = (myVector < 0);
      constant integer foo = -5;
      result2 = (myVector == foo);

  This expression is never true. It is nevertheless allowed by the language, but
  the compiler should produce a warning here.

* The compiler should also emit a warning if a constant value is truncated. More
formally, it should emit a warning if the result of an operator whose one operand
is a constant sub-expression is the same as if the constant sub-expression produced
another value which consists only of a low-order subrange of bits of the original
value. TODO this definition does not work for negative numbers! Simple example

      signal vector[8] foo = 256;
      
  since this is equivalent to 

      signal vector[8] foo = 0;


TODO all this is based on the integer meaning of operators. Alternative: onvert
both operands to vectors first, and fire a warning if that conversion overflows.
Problem: without knowing the signedness of the target vector it is not clear when
the conversion overflows, but we don't need to know that in detail either.

TODO however the TODO about negative numbers may mean it's actually more useful
to convert to vectors first -- at least for warnings!

What about the usefulness of converting to vector first in terms of the operation?
- +/-/* doesn't matter due to truncation
- //% don't know, but not that important
- comparison: negative numbers cause bugs and those are detected either way
	NO! Here's a problem: For comparison operators (which work unsigned) we don't
	want to convert negative numbers to a vector first.This gives a wrong result.
	For +/-, OTOH, we DO want to do that. So conversion has to depend on the operator.
	This is more complex than working on the integer value.
	
	Problem case:
	
		constant integer myConstant = ...; // value: -3
		myResult = myVector < myConstant; // unsigned comparison to e.g. 253. BAD!
	
- equal / not equal: undefined whether these are bugs, depends on the interpretation of the vector
- shift / concat: can't convert to same size
---


For bitwise operators, the operation on integers is again defined based on the
operations on bits. So it seems that we might use vectors directly. However, the
above definition also defines what happens if either operand is an integer.

The unary minus operator seems special at first in that we need to treat its
operand as signed. This isn't the case though; trimming the result to the same
size as the operand causes the distinction to disappear. Another way to see it is
that this operator can be defined without reference to signed-ness, as
 
        -n := ~n + 1

For other operators such as less-than the signed-ness **does** make a difference.
The language currently doesn't support signed comparison of vectors explicitly.

It would also be nice to define integers as infinite vectors. This should be
possible if we define that all vector or integer values are infinite vectors
with finite interesting bits -- that is, each value is an infinite vector with
a value N such that all bits above index N have the same value. The usefulness
if this is questionable though, since vectors also have an explicit size that
is used to trim the operator result. Also, infinite-sized integers imply that
we need a workaround for operators such as less-than, which has a different
effect when the vector operand is expanded than when the integer operand is trimmed
(**really?** -- it has, if the trimming may change the value without producing
an error).

