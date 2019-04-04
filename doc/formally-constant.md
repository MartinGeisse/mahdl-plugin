
# Formally Constant Expressions

An expression is *formally constant* if it does not contain any of the following:
* an identifier that refers to something other than a defined constant
* a switch expression (because we treat them as "too complex", even though it might be possible to support them
	in principle)

An expression is *incidentally constant* if it can be replaced by its value without changing the semantics of the
code. (This definition ignores the fact that not all constant values have a corresponding syntactic literal; we just
assume they had.)

The main difference between formally constant expressions and incidentally constant expressions is that constant folding
(of generally undefined level) is needed to detect incidentally constant expressions, whereas formally constant
expressions can be recognized on a syntactic level. MaHDL demands formally constant expressions whenever an expression
must be compile-time constant, so the difference between "compiled" and "does not compile" is not dependent on constant
folding, which may easily change between compiler versions or even compiler settings.

To be precise, only expressions whose value is actually used can be incidentally constant, that is,
replacing them by some *different* constant value *does* change semantics. Incidentally constant expressions are only
used to discuss optimization and to serve as an example -- they aren't a main concept in MaHDL like formally constant
expressions are. 
