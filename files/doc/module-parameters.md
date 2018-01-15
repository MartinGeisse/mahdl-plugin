
# Module Parameters

## Problem

* Module parameters interact with the type system since vector size
expressions can use constants.
* As a result, type safety cannot be checked until the value of all constants
used in the types are known.
* In general this isn't the case until module instantiation.
* Thus, type errors cannot be found until module instantiation.
* At instantiation time, type errors cannot be shown at the offending
construct, only at the instantiation construct, even when the instantiation
isn't at fault and even though it might not give sufficient information
to understand the problem.

## Benefit

* Module parameters allow limited polymorphism: Vector sizes and
certain run-time values are specified at construction time.
* For run-time values, signals can serve a somewhat similar role,
although their effect might be unspecified if they are not constant
and they cannot reliably be used in operations without producing run-time
cost in terms of FPGA resources used.
* The benefit is thus
  * vector sizes defined at instantiation
  * the calculation for run-time constants can be specified in a reusable way 

## Limitations

* Vector Computations on vectors of parameterized size cannot be specifid
other than in terms of vector operators and vector functions. This is what
verilog needs generate clauses for.
* Either synthesis-time or pre-synthesis optimization or complicated generate
clauses are needed to define optional parts of the circuit.
* For anything but simple cases, even generate clauses and optimization cannot
achieve what is needed. Professional tools use code generators for CPUs, memory
interfaces, buses, ... 

## Conclusion

MaHDL does not support defining parameterized modules. It relies on external
code generation or templating for this. A MaHDL-defined module knows the values
of all constants used in its definition at definition time, without reference to
its intantiation.

MaHDL may, in the future, support parameter binding at module instantiation,
under the following conditions:
* any such module cannot be defined directly in MaHDL due to the inability to
define parameterized modules in MaHDL. It must be defined through a templating /
code generation system. That is, this limited future support only provides a
convenient way to trigger code generation through module instantiation.
* Doing so will cause any parameter-dependent problems to be delayed until
instantiation, and will report them at the instantiation site. Any parameterized
modules should therefore define strict rules for allowed parameter sets and
produce useful error messages based on these rules.
* *This is a consideration for future versions though. For now, the code generator
must be invoked explicitly and produce a MaHDL module definition with all constants
bound to values, and with a unique name.*
