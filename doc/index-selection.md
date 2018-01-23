
# Index Selection

Index selection has container and index sub-expressions. The container must be a vector or memory whose size (first
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

It is also less obvious in such a case which kinds of expressions are allowed for the index of a container that does
not have a power-of-two size. Most of the language behavior and what is allowed is defined in terms of *types*, not
*constness*, and is much easier to understand that way (see operators.md). Making an exception here seems like an ugly
exception.

**TODO** it might be possible to mix constant folding into the expression processor without a lot of effort. But even
then, it is still unclear whether this is a good idea: Most of the behavior of the language is bound to types, not
formal constness. For example, whether -3 < 5 can be evaluated depends on the left and right types: If the left-hand
operand is a vector, -3 cannot be represented and is either wrapped or produces an error (depending on the exact
expression) but with an integer it works. This *could* have been defined in terms of constness, but doing so makes code
hard to understand. Another example would be (myVectorSignal < 5) vs. (myVectorSignal < -3). The former is okay while
the latter is a compile-time error because it can never be true. But based solely on constness, this is hard to define,
while in types, -3 cannot be a vector and using the integer -3 causes a compile-time conversion error -- exactly what
we want.

