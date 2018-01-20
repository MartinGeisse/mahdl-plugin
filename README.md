
# MaHDL

MaHDL is a hardware description language, implemented as an IntelliJ plugin and compiled to Verilog for implementation.

## Building

The project can be opened as an IntelliJ plugin project. A fairly recent version of IntelliJ is needed.

Part of the project is, of course, the lexer and parser. They are built separately if needed. The Java code for the
current version is checked in to Git though, so you only need to rebuild them when changing the grammar.

To build the lexer, JFlex is needed (preferably the corresponding IntelliJ plugin). For the parser, MaPaG
(https://github.com/MartinGeisse/mapag) is needed, which only exists as an IntelliJ plugin and must currently be built
separately in a similar way as MaHDL.

While an implementation of MaHDL outside of IntelliJ is fundamentally possible, nothing in this project supports it in
any way. For example, to even start such an implementation, a MaPaG "target" ouside IntelliJ would be needed.

## Design Concepts

MaHDL is build on the following main design decisions:
* **RTL.** The level of abstraction used in MaHDL is register-transfer level.
* **Implementation-oriented.** MaHDL is an implementation language. Each feature must be geared towards implementing a
design, e.g. on an FPGA. This does not mean that all features are synthesizable, but rather that all features should
play a useful role in synthesis / implementation.
* **Less is better.** MaHDL should only contain features that provide a benefit in the general case *or* are required
to implement edge cases. The latter it taken very strict: If some feature can be reasonably built outside MaHDL and
brought together in a complete design, it is better left out of the language.
* **Strictness.** MaHDL should refuce to work with designs which are over-specified or under-specified, handle data
types loosely, and so on.
* **Built for tools.** Every language feature should be built with tooling and IDEs in mind, to support meta-operations
on designs written in MaHDL as well as quick error feedback.

Examples for design decisions made with these concepts in mind:
* *RTL:* Asynchronous loops are forbidden. Dealing with transient signal glitches does not happen in MaHDL. High-level
data types are not supported.
* *Implementation-oriented:* MaHDL does not support language constructs which are meant solely for simulation. This
includes things as non-synthesizable behavioral descriptions written for simulation and signal delays. Note that this
also excludes post-implementation timing simulation since it is impossible to back-annotate the original source code
with actual signal delays from implementation -- if one wants to accept the abstraction mismatch inherent in this
approach, then the delays must simply be provided to a simulation tool by other means than the MaHDL source code.
* *Less is better*: This rule could be discussed in its own section. As a summary, MaHDL does not contain many
features one would expect in an HDL simply because a one-size-fits-all language is bound to be a huge mess. MaHDL is
intended to be one language in a toolbox of languages, each being optimized for its purpose. A concrete example is
that MaHDL does not support the definition of parameterized modules because those are a huge tradeoff between
flexibility, language complexity, good error reporting, and so on. Instead, parameterized modules should be implemented
using a templating / code generation system, with MaHDL being the output of that system. This is the approach that is
generally being taken anyway: For example, FPGA vendors would rather provide a "memory interface generator" than a
memory interface module that is parameterized.
* *Strictness*: There are no rules in the language like signals that are defined implicitly, or implicit type
conversions unless they are obvious and intuitive, or things that are "maybe synthesizable".
* *Built for tools*: A design written in MaHDL should be understandable to software, especially tools written to
navigate and manipulate the MaHDL code. This starts with clear rules on which files are part of a design: We don't want
a situation where multiple files define a module with the same name, and the tools being unable to find out which one
is the right one. Strictness plays a role here too, clearly defining undesirable constructs to be wrong and being
reported as compile-time errors: For example, MaHDL ensures that accessing one bit of a bit vector is never
out-of-bounds through its type system; Verilog makes it hard to impossible to even decide this problem and just defines
that out-of-bounds access is allowed and produces the 'undefined' value.
