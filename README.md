# Compiler-Grad-Project
A compiler written in Java that converts a subset of C code (referred to as microC or uC) into RISC-V Assembly.  
The main branch includes a compiler that will perform compilation of control structures such as if/else and while loops, functions, pointers, arrays, memory allocation and type conversions.  
The Register Allocation branch is built off the main branch prior to pointers, arrarys, memory allocation, and type conversions being added. Instead, it supports Local Register Allocation to limit the quantity of registers available to the compiler. It also uses an intermediate representation, allowing for different optimizations to be added.
Both of these branches are further explained below.


## Table of Contents
- [Background](#background)
- [Compiler Features](#compiler-features)
- [Control Flow](#control-flow)
- [Functions](#functions)
- [Pointers](#pointers)
- [Arrays](#arrays)
- [Memory Allocation](#memory-allocation)
- [Type Conversion](#type-conversion)
- [Intermediate Representation](#intermediate-representation)
- [Register Allocation](#register-allocation)
- [Grammar](#grammar)
- [Sources](#sources)


## Background
This Compiler Project was built as the main project of an Introduction to Compilers course I took at Purdue University in the Fall semester of 2022.
This course covered:
- Regular Expressions, Lexing, Context-Free Grammars, and LL(1) Parsing
- Sematic Actions, Abstract Syntax Tree Construction, and translating AST's into executable code
- Functions in Programming Languages and generating code for functions
- Type Checking
- Common Sub-Expression Elimination and Dead Code Elimination Optimizations
- Local and Global Register Allocation
- Dataflow Analysis such as Lattice Theory, Pointer & Loop Analysis, and Dependencies Analysis

## Compiler Features
This compiler project uses ANTLR to parse the input program provided with a context-free grammar(see the [Grammar](#grammar) section below)  
This then builds a parse tree that defines the structure of the program.  

This parse tree is used to build a Symbol Table, and an Abstract Syntax Tree.  
The Abstract Syntax Tree (AST) has nodes that are recursivly passed up the tree. The nodes are defined in the AST folder, and the structure of the AST can found through the semantic actions seen in the Micro.g4 grammar file.   
These AST Nodes are then used to generate assembly code.


### Control Flow

This compiler features control flow structures of if/else statements and while loops. These control flow structures are supported for both integer and float data types, the 2 numerical types used in this compiler.  

The section of this code can be seen beginning around line 570 in the Code Generator file, in the java/assembly/CodeGenerator.java path. This is a condition node, which will correspond to either a if/else branch or a while loop branch.  
This is translated into Risc-V Assembly where a if a conditional branch evaluates to true, then the execution of the assembly jumps to a label further down, skipping the branched code. Due to this and it's difference from C, it can be observed within the code generation for the conditional node it uses the reverse of the comparison operator to determine the appropriate branch.  
The code generation for conditionals support the following Risc-V instructions:
- BEQ, corresponding to "Branch if equal To"
- BNE, corresponding to "Branch if not equal To"
- BLT, corresponding to "Branch if less than"
- BLE, corresponding to "Branch if less than or equal to"
- BGT, corresponding to "Branch if greater than"
- BGE, corresponding to "Branch if greater than or equal to"

Risv-V does not have conditional branch instructions for floating point types. To allow for conditional branches of these types, the following comparison operators are used:  
- FEQ.S, corresponding to ==
- FLT.S, corresponding to <=
- FLE.S, corresponding to <
The subsequent comparisons are used through reversing polarity of these operators in both the if/else and while loop functions.

### Functions


### Pointers

### Arrays

### Memory Allocation

### Type Conversion

### Intermediate Representation

### Register Allocation

### Grammar
The development of this Compiler used a Context-free grammar as a set of rules to construct the program with. This can be seen in the Micro.G4 grammar file.
The basis of the grammar was provided by the course. Grammar used in implementing most of the following compiler features were part of the project development.


## Sources

https://riscv.org//wp-content/uploads/2017/05/riscv-spec-v2.2.pdf
