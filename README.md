# MicroJava Compiler

A compiler for a MicroJava language variant, implemented in Java. It uses
JFlex and CUP to generate a lexer, parser, and abstract syntax tree, then
performs semantic analysis and generates bytecode for the MicroJava runtime.
The grammar includes arrays, classes, enums, loops, and switch statements.

## Project layout

- `spec/` contains the lexer and grammar specifications.
- `src/` contains semantic analysis, symbol-table extensions, and code generation.
- `test/` contains compiler drivers and example MicroJava programs.
- `config/` contains the logging configuration.

## Requirements

Install a JDK and Apache Ant. Place the following dependencies in a `lib/`
directory at the project root:

- `JFlex.jar`
- `cup_v10k.jar` (the course version supporting `-ast` and `-buildtree`)
- `log4j-1.2.17.jar`
- `symboltable-1-1.jar`
- `mj-runtime-1.1.jar`

The course-supplied libraries are not included. Use the versions provided
with the MicroJava course; a standard CUP distribution may not support the
AST-generation options. If the symbol-table file has a different name, copy
it to `lib/symboltable-1-1.jar`.

## Build and run

From the project directory:

```text
ant compile
ant run
ant runObj
```

`ant compile` regenerates the lexer, parser, and AST and compiles the Java
sources to `build/classes`. `ant run` compiles `test/program.mj` into
`test/program.obj`. `ant runObj` disassembles and executes that bytecode,
using `input.txt` for program input. Replace `test/program.mj` with another
example to try it. Run `ant test` to check compiler diagnostics.

Generated files, build output, logs, object files, and dependency JARs are
not part of the repository. Keep them out of commits.

## Known limitations

Class and inheritance support is incomplete: the included `test303.mj`
currently reports errors and may terminate with an exception.
