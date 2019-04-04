
# Index Selection

Index selection has container and index sub-expressions. The container must be a vector or matrix whose size (first
size for memories) is relevant here: We want to ensure statically that the index cannot be *out-of-bounds*, that is,
be greater or equal than the container size. We also want the index to be non-negative.

Intuitively, a constant index can be bounds-checked easily -- just evaluate the constant expression and check. We want
to restrict this to formally constant so the bounds check does not depend on constant folding.

A non-constant index must be a vector and can, in principle, be out of bounds if the container size is less than
2^(the index size). We don't want compilation to *depend* on any stronger proof about the index value, so we
demand that the container as at least that size if the index is a non-constant vector.

An interesting case is that of a constant vector. Since using constant vectors as indices isn't particularly important
to express a design in a clear way, we currently restrict them as we do for non-constant vectors. It might not seem
obvious from the outside, but the compiler is simplified by a great deal because the type-check for an expression can
be clearly separated from constant folding and checking the resulting values. If constant vector indices were allowed to
be possibly out-of-bounds in type as long as they are in-bounds in value, the type check has to be delayed until it
has been determined whether the index vector is formally constant.

**The more important rationale:**
It is also less obvious in such a case which kinds of expressions are allowed for the index of a container that does
not have a power-of-two size. Most of the language behavior and what is allowed is defined in terms of *types*, not
*constness*, and is much easier to understand that way (see operators.md). Making an exception here seems ugly.

It might have been possible to mix constant folding into the expression processor without a lot of effort. But even
then, it is still unclear whether this is a good idea: Most of the behavior of the language is bound to types, not
formal constness. For example, whether -3 < 5 can be evaluated depends on the left and right types: If the left-hand
operand is a vector, -3 cannot be represented and is either wrapped or produces an error (depending on the exact
expression) but with an integer it works. This *could* have been defined in terms of constness, but doing so makes code
hard to understand. Another example would be (myVectorSignal < 5) vs. (myVectorSignal < -3). The former is okay while
the latter is a compile-time error because it can never be true. But based solely on constness, this is hard to define,
while in types, -3 cannot be a vector and using the integer -3 causes a compile-time conversion error -- exactly what
we want. As a third example, type conversion in (myVector + myConstant) converts the constant to a vector (if it is an
integer) because of *types*. Defining this on constness and on type conversion of the result is ugly and cumbersome.
**Conclusion**: It makes sense to use a value-based bounds check based on *types*, not *constness*, too. This means that
for an index vector, the bounds check is based on its type, not value.

# Fixed Range Selection (Verilog: a[b:c])

For range-selection, things are even worse: We need to statically determine the selected size, and that the from-index
is not less than the to-index. Applying the same logic as for index-select, both range ends must be integers since
vectors "could be anything within their value range".

**OTOH, range-selection shows that expression processing and constant folding MUST work in lock-step!** The expression
processor must evaluate embedded formally constant sub-expressions to find out the result type of a range-selection!

# Dynamic Range Selection (Verilog: a[b+:c] and a[b-:c])

First, observe that using the above rules, downwards range selection cannot work at all unless the selection width
is 1, which makes it equivalent to index selection: The dynamic part of the range "could be" everything down to zero,
and any width greater than 1 "could be" out of range, which must be rejected according to the above rules. Since
downward range selection isn't fundamentally more powerful than upwards range selection, I'd rather just throw it
out of the language.

Next, consider upwards range selection. This is an operator that I never used in practice. Even when it is absolutely
needed, there are two alternatives:
* use the shift operator and fixed range selection
* provide a built-in function for upwards range selection

This means, like signed arithmetic, upwards range selection is not commonly enough used to deserve its own operator in
the language. (I would even suspect it to be much less common than signed arithmetic).

With both kinds of dynamic selection removed, fixed range selection can be renamed to just "range selection".
