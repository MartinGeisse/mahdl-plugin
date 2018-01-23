
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
has been determined whether the index vector is formally constant. It is also less obvious in such a case which kinds
of expressions are allowed for the index of a container that does not have a power-of-two size.
