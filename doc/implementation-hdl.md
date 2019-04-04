
# MaHDL is an Implementation HDL

MaHDL does not allow sophisticated behavioral modeling. The main reason was to keep the language
simple, while supporting all constructs needed for synthesis. This causes the language to be
mostly synthesizable (although 100% synthesizability was not a goal).

MaHDL also does not support domain-specific modeling. This, too, is done to keep the language
simple. Other HDLs use parameterized modules to support limited DSM. Parameterization is another topic
where MaHDL is intentionally limited.

I want to keep it that way for several reasons:
- it keeps the language simple and well-defined and allows better error reporting
- other tools can be used for behavioral modeling and for DSM
- MaHDL can be used as an implementation step between a high-level model and generated Verilog code
- Even with a high-level model in use, various "wiring" parts are needed for a complete system.
    MaHDL is sufficient to describe these parts directly. The alternative is doing the same in Verilog, which
    is more complex.

It would be useful to provide a MaHDL code generation library to use in code generators that turn a high-level
model into MaHDL. From there, synthesizable Verilog code can be generated.
