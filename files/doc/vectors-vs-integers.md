
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

* **Any operation is defined for two vectors by interpreting these two vectors
as unsigned integers, then applying the operator on those intergers. The result
is converted to a vector by taking the N lowest-order bits, where N is determined
in an operator-specific way.**
* **An integer is defined to be equivalent to a *signed infinite vector*, that is,
a vector of infinite size whose high-order bits, starting at some index, all have
the same value. This value is the sign bit. The integer value of such a vector can
be obtained by taking its low-order bits and at least one sign bit, and interpreting
it as a two's complement value.**
* **Using the first rule to define operations, but with the interpretation of
signed infinite vectors taken from the second rule, it is irrelevant whether
we compute an operation on the numeric value of an integer or on its signed
infinite vector. We can use whatever way is more convenient. Note that we
need the second rule's interpretation since "unsigned" isn't well-defined
for SIVs.**
* **Note also that a finite vector can be turned into a SIV of the same numeric
value by taking the finite vector as the low-order part and setting the sign
bit to 0.**
* **Based on these definitions and observations, any operation on two vector
or integer values is defined by turning both values into the corresponding 

TODO: I intended to define operations by turning values into vectors and reporting
an error if this causes overflow. This doesn'T work with numeric values or
SIVs. ???
TODO: why define SIVs at all? Can't we just define that operations always act
on numeric values? I.e. define that all operators turn vectors to integers as
unsigned, act on integers, then turn the result into a vector using the operand
sizes. This may cause warnings in some cases, e.g.: result = myVector < -3
because this is never true, but SIVs don'T help the slightest in resolving that.
Even with SIVs, we still interpret finite vectors as unsigned.


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

