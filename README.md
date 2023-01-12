# Compiler-Grad-Project
A compiler written in Java that converts a subset of C code (referred to as microC or uC) into RISC-V Assembly.  
The main branch includes a compiler that will perform compilation of control structures such as if/else and while loops, functions, pointers, arrays, memory allocation and type conversions.  
The Register Allocation branch is built off the main branch prior to pointers, arrarys, memory allocation, and type conversions being added. Instead, it supports Local Register Allocation to limit the quantity of registers available to the compiler. It also uses an intermediate representation, allowing for different optimizations to be added.
Both of these branches are further explained below.


## Table of Contents
- [Background](#background)
- [Compiler Features](#compiler-features)
  * [Control Flow](#control-flow)
  * [Functions](#functions)
  * [Pointers](#pointers)
  * [Arrays](#arrays)
  * [Memory Allocation](#memory-allocation)
  * [Type Conversion](#type-conversion)
  * [Intermediate Representation](#intermediate-representation)
  * [Register Allocation](#register-allocation)
  * [Grammar](#grammar)
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

This compiler also supports a C language with functions.
These functions can have integer, float, or void return types.
This part of the code generation uses the stack to pass arguments to and return values from functions. This process can be seen starting at line 1030 in CodeGenerator.java 

Overall, the process followed by this compiler when a function is called can be broken down into the following steps:  
- Push function arguments to stack
- Allocate space for return value
- Save the return address of the calling function on the stack
- Jump to the called function
- Save the old frame pointer of the calling function
- Set the frame pointer to the top of the stack
- Allocate space on the stack for local variables of called function
- Save registers that may be written to in the called function
Then, the function can execute. This section can be seen in CodeGenerator.java beginning around line 910.  


When a return statement is called in the function, the steps taken are:
- Push return value to appropriate location on the stack (Where space was allocated previously)
- Jump to functions out label
- Restore all saved registers to their previous state
- Deallocate function stack frame by resetting the stack pointer to the top of the caller's activation record
- Resetting the frame pointer back to the caller's frame pointer location
- Return to calling function

### Pointers
This language supports both pointer dereferencing and address of expressions.  
Pointer Dereferencing Syntax, which dereferences the pointer expr:
```
* expr;  
```

Address of Expression Syntax, this gets the address of a pointer expr:
```
& expr;  
```

This language also supports pointer arithmetic, through treating the address stored in a pointer as an integer type:
ptr has an address of 0x100  
The operation * (ptr + 10) will result in an address of 0x110  

The code generation for pointer dereferencing can e found at line 1125 in CodeGenerator.java  
This is performed by first insuring the expr in the dereference operation is an r-value, and then adding the code passed up through the AST to it. Then, the type it points to is modified to represent a pointer type  

The code generation for address of operations can be found at line 1144 in CodeGenerator.java  
This is performed through getting the appropriate address of a variable, inheriting the type of what it points to, and loading the address from the appropriate scope (global or local)

### Arrays
This language supports arrays, but does not support array types. Instead, it uses pointers and pointer arithmetic to reference the proper index of an array.  

An example of adding a value to an array arr at index ind:
```
int * arr;
arr[ind] = value;
```
Or, in addition to the above block, to set var equal to a value fram an array arr at an index ind:
```
var = arr[ind];
```

The supporting methods to this is through using addresses and pointer arithmetic in the form of:
```
arr[index] = (arr + index * size_of_type);
```
Where size of type is the size of the type of data stored in the array in bytes.  
This process can be seen in the Micro.g4 grammar file around line 180

### Memory Allocation
This language supports allocating memory on the heap through use of malloc and free.  

The code generation for malloc can be seen at line 1161 in CodeGenerator.java  
This is accomplished through generating an instruction malloc that takes an argument of the temorary that holds the data to be stored on the heap, and an address to store it at on the heap. Malloc will allocate a number of bytes on the heap equal to the size in bytes of the data stored in the temporary passed into malloc.  

The code generation for free can be seen at line 1182 of CodeGenerator.java.  
This is accomplished through generating an instruction free that takes the location on the heap to be freed as an argument. If a variable is passed to free, it will free the address stored in that variable, as it expects the vairable to be a pointer.  


### Type Conversion
This language supports integer and float data types. It also supports both casting and type conversion between these types.  

Casting is accomplished starting at line 259 in CodeGenerator.java  
The type to cast to is passed up the AST to the CastNode, and the appropriate Risc-V instruction is generated depening on the cast type. The type information of the vairable is then updated to reflect the new type.  

The syntax for a cast, or explicit conversion, is:
```
float x;
int y;
y = (int) x;
```
This section of uC code convert the value at x from a float to an integer, and then assigns this integer value to integer y.  

Implicit Type Conversions, or Type Promotions, are supported according to the following rules:
- In a binary expression, if one of the 2 operands is an integer and the other operand is a float: Implicity cast the integer to a float
  * ```
    float x; 
    int y;
    x = 2.0;
    y = 3;
    return (x + y)
    ```
    In this case, the operation would add 2.0 and 3.0, returning 5.0
- In an assigment statement, either:
  * If left-hand side operand is an integer, and right-hand side operand is a float, cast the RHS to an integer
  * If left-hand side operand is a float, and right-hand side operand is an integer, cast the RHS to a foat

The code generation for binary expression implicit conversions can be found beginning at line 124 in CodeGenerator.java   
The code generation for assignment type conversions can be found starting at line 330 in CodeGenerator.java  


### Intermediate Representation

### Register Allocation

### Grammar
The development of this Compiler used a Context-free grammar as a set of rules to construct the program with. This can be seen in the Micro.G4 grammar file.
The basis of the grammar was provided by the course. Grammar used in implementing most of the following compiler features were part of the project development.


## Sources

https://riscv.org//wp-content/uploads/2017/05/riscv-spec-v2.2.pdf
