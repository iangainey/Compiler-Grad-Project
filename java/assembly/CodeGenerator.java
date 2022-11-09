package assembly;

import java.util.List;

import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;
import compiler.Scope.SymbolTableEntry;

/*
 * Use 3AC code, using pretty much only adds, subtracts, and moves
 * Then, in function can generate riscV code for each instruction
 * Take first instruction, generate code, add to a new instructionlist that
 * in the end is returned
 */

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public String intTempPrefix = "$t";
	static final public String floatTempPrefix = "$f";
	
	int loopLabel;
	int elseLabel;
	int outLabel;

	static final public int numIntRegisters = 32;
	static final public int numFloatRegisters = 32;

	String currFunc;
	
	public CodeGenerator() {
		loopLabel = 0;
		elseLabel = 0;
		outLabel = 0;
		intRegCount = 0;		
		floatRegCount = 0;
	}

	public int getIntRegCount() {
		return intRegCount;
	}

	public int getFloatRegCount() {
		return floatRegCount;
	}
	
	/**
	 * Generate code for Variables
	 * 
	 * Create a code object that just holds a variable
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();
		if (sym.isLocal()) {
			co.temp = "$l" + String.valueOf(sym.getAddress());
		} else {
			co.temp = "$g" + sym.getName();
		}


		return co;
	}

	/** Generate code for IntLiterals
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new Li(generateTemp(Scope.Type.INT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/** Generate code for FloatLiterals
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new FImm(generateTemp(Scope.Type.FLOAT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for binary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from left child
	 * Step 1a: if left child is an lval, add a load to get the data
	 * Step 2: add code from right child
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate binary operation using temps from left and right
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(BinaryOpNode node, CodeObject left, CodeObject right) {

		CodeObject co = new CodeObject();
		
		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		InstructionList il = new InstructionList();

		il.addAll(left.code);
		il.addAll(right.code);

		
		switch (node.getType()) {
			case INT:
				switch(node.getOp()) {
					case ADD: 
						Instruction iAdd = new Add((left.temp), (right.temp), (generateTemp(Scope.Type.INT)));
						il.add(iAdd);
						co.temp = iAdd.getDest();
					break;
					case SUB: 
						Instruction iSub = new Sub((left.temp), (right.temp), (generateTemp(Scope.Type.INT)));
						il.add(iSub);
						co.temp = iSub.getDest();
					break;
					case MUL: 
						Instruction iMul = new Mul((left.temp), (right.temp), (generateTemp(Scope.Type.INT)));
						il.add(iMul);
						co.temp = iMul.getDest();
					break;
					case DIV: 
						Instruction iDiv = new Div((left.temp), (right.temp), (generateTemp(Scope.Type.INT)));
						il.add(iDiv);
						co.temp = iDiv.getDest();
					break;
					default:
						throw new Error("Issue with int in operation");
				}
			break;
			case FLOAT:
				switch(node.getOp()) {
					case ADD: 
						Instruction fAdd = new FAdd((left.temp), (right.temp), (generateTemp(Scope.Type.FLOAT)));
						il.add(fAdd);
						co.temp = fAdd.getDest();
					break;
					case SUB: 
						Instruction fSub = new FSub((left.temp), (right.temp), (generateTemp(Scope.Type.FLOAT)));
						il.add(fSub);
						co.temp = fSub.getDest();
					break;
					case MUL: 
						Instruction fMul = new FMul((left.temp), (right.temp), (generateTemp(Scope.Type.FLOAT)));
						il.add(fMul);
						co.temp = fMul.getDest();
					break;
					case DIV: 
						Instruction fDiv = new FDiv((left.temp), (right.temp), (generateTemp(Scope.Type.FLOAT)));
						il.add(fDiv);
						co.temp = fDiv.getDest();
					break;
					default:
						throw new Error("Issue with float in operation");
			}
			break;
			default:
				throw new Error("Issue w/binary operator");
		}
		co.code.addAll(il);
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for unary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from child expression
	 * Step 2: generate instruction to perform unary operation
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		InstructionList il = new InstructionList();

		il.addAll(expr.code);
		switch(node.getType()) {
			case INT: 
				Instruction i = new Neg((expr.temp), (generateTemp(Scope.Type.INT)));
				il.add(i);
				co.temp = i.getDest();
			break;
			case FLOAT: 
				Instruction f = new FNeg((expr.temp), (generateTemp(Scope.Type.INT)));
				il.add(f);
				co.temp = f.getDest();
			break;
			default:
				throw new Error("Issur w/unary operation");
		}

		co.code.addAll(il);
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for assignment statements
	 * 
	 * Step 0: create new code object
	 * Step 1: if LHS is a variable, generate a load instruction to get the address into a register
	 * Step 1a: add code from LHS of assignment (make sure it results in an lval!)
	 * Step 2: add code from RHS of assignment
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate store
	 * 
	 * Hint: it is going to be easiest to just generate a store with a 0 immediate
	 * offset, and the complete store address in a register:
	 * 
	 * sw rhs 0(lhs)
	 */
	@Override
	protected CodeObject postprocess(AssignNode node, CodeObject left,
			CodeObject right) {
		
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		InstructionList il = new InstructionList();

		il.addAll(left.code);
		il.addAll(right.code);
		switch(left.getType()) {
			case INT: 
				il.add(new Mv((right.temp), (left.temp)));
				//il.add(new Sw(right.temp, left.temp, "0"));
			break;
			case FLOAT: 
				il.add(new FMv((right.temp), (left.temp)));
				//il.add(new Fsw(right.temp, left.temp, "0"));
			break;
			default:
				throw new Error("Issue in 3AC assignment");
		}
		

		co.code.addAll(il);

		co.type = left.getType();

		return co;
	}

	/**
	 * Add together all the lists of instructions generated by the children
	 */
	@Override
	protected CodeObject postprocess(StatementListNode node,
			List<CodeObject> statements) {
		CodeObject co = new CodeObject();
		//add the code from each individual statement
		for (CodeObject subcode : statements) {
			co.code.addAll(subcode.code);
		}
		co.type = null; //set to null to trigger errors
		return co;
	}
	
	/**
	 * Generate code for read
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from VarNode (make sure it's an lval)
	 * Step 2: generate GetI instruction, storing into temp
	 * Step 3: generate store, to store temp in variable
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(ReadNode node, CodeObject var) {
		
		//Step 0
		CodeObject co = new CodeObject();

		//Generating code for read(id)
		assert(var.getSTE() != null); //var had better be a variable

		InstructionList il = new InstructionList();
		switch(node.getType()) {
			case INT: 
				//Code to generate if INT:
				//geti var.tmp
				Instruction geti = new GetI(var.temp);
				il.add(geti);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf var.tmp
				Instruction getf = new GetF(var.temp);
				il.add(getf);
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		co.code.addAll(il);

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * Generate code for print
	 * 
	 * Step 0: create new code object
	 * 
	 * If printing a string:
	 * Step 1: add code from expression to be printed (make sure it's an lval)
	 * Step 2: generate a PutS instruction printing the result of the expression
	 * 
	 * If printing an integer:
	 * Step 1: add code from the expression to be printed
	 * Step 1a: if it's an lval, generate a load to get the data
	 * Step 2: Generate PutI that prints the temporary holding the expression
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType() == Scope.Type.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);

			//Step 2:
			Instruction write = new PutS(expr.temp);
			co.code.add(write);
		} else {			
			//Step 1:
			co.code.addAll(expr.code);

			//Step 2:
			//if type of writenode is int, use puti, if float, use putf
			Instruction write = null;
			switch(node.getWriteExpr().getType()) {
			case STRING: throw new Error("Shouldn't have a STRING here");
			case INT: write = new PutI(expr.temp); break;
			case FLOAT: write = new PutF(expr.temp); break;
			default: throw new Error("WriteNode has a weird type");
			}

			co.code.add(write);
		}

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Generating an instruction sequence for a conditional expression
	 * 
	 * Implement this however you like. One suggestion:
	 *
	 * Create the code for the left and right side of the conditional, but defer
	 * generating the branch until you process IfStatementNode or WhileNode (since you
	 * do not know the labels yet). Modify CodeObject so you can save the necessary
	 * information to generate the branch instruction in IfStatementNode or WhileNode
	 * 
	 * Alternate idea 1:
	 * 
	 * Don't do anything as part of CodeGenerator. Create a new visitor class
	 * that you invoke *within* your processing of IfStatementNode or WhileNode
	 * 
	 * Alternate idea 2:
	 * 
	 * Create the branch instruction in this function, then tweak it as necessary in
	 * IfStatementNode or WhileNode
	 * 
	 * Hint: you may need to preserve extra information in the returned CodeObject to
	 * make sure you know the type of branch code to generate (int vs float)
	 */
	@Override
	protected CodeObject postprocess(CondNode node, CodeObject left, CodeObject right) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */

		co.code.addAll(left.code);
		co.code.addAll(right.code);
		/* Want to add additional info to CodeObject
		Store reversed op
		*/
		
		
		switch(node.getReversedOp()) {
			case GT: //Greater than, generate bgt
				co.branch = "GT";
				break;
			case GE: //Greater than or equal, generate bge
				co.branch = "GE";
				break;
			case LT: //Less than, generate blt
				co.branch = "LT";
				break;
			case LE: //Less than or equal, generate ble
				co.branch = "LE";
				break;
			case NE: //Not equal, generate bne
				co.branch = "NE";
				break;
			case EQ: //Equal, generate beq
				co.branch = "EQ";
				break;
			default:
				throw new Error("Issue with storing branch type in CodeObject(In codegenerator)");
		}
		co.src1 = left.temp;
		co.src2 = right.temp;
		if (left.type == Scope.Type.FLOAT || right.type == Scope.Type.FLOAT) {
			co.floatCond = true;
		}

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		<cond code>
	 *		<flipped branch> elseLabel
	 *		<then code>
	 *		j outLabel
	 *		elseLabel:
	 *		<else code>
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 * 
	 */
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */

		//Generate labels
		/* Want labels to be unique.  */
		String elseTxt = generateElseLabel();
		String outTxt = generateOutLabel();
		Instruction elseLabel = new Label(elseTxt);
		Instruction jumpLabel = new J(outTxt);
		Instruction endLabel = new Label(outTxt);

		//Add code from conditional
		co.code.addAll(cond.code);

		//Create branch
		InstructionList il = new InstructionList();
		if (cond.floatCond) {
			//FloatComparison
			String temp = generateTemp(Scope.Type.INT);
			switch(cond.branch) {
				case "LT": //Want to BNE
				//Compares src1 and src2, writes the result (1 or 0 as true or false)
				//To temp. Then compare temp to 0. Branch if not equal
					Instruction ltFlt = new Flt(cond.src1, cond.src2, temp);
					il.add(ltFlt);
					Instruction ltFltB = new Bne(temp, "x0", elseTxt);
					il.add(ltFltB);
				break;
				case "GE":
				//Swap compare around as it's opposite LT
					Instruction geFlt = new Flt(cond.src2, cond.src1, temp);
					il.add(geFlt);
					Instruction geFltB = new Bne(temp, "x0", elseTxt);
					il.add(geFltB);
				break;
				case "EQ":
					Instruction eqFeq = new Feq(cond.src1, cond.src2, temp);
					il.add(eqFeq);
					Instruction eqFeqB = new Bne(temp, "x0", elseTxt);
					il.add(eqFeqB);
				break;
				case "NE": 
					Instruction neFeq = new Feq(cond.src1, cond.src2, temp);
					il.add(neFeq);
					Instruction neFeqB = new Beq(temp, "x0", elseTxt);
					il.add(neFeqB);
				break;
				case "LE":
					Instruction leFle = new Fle(cond.src1, cond.src2, temp);
					il.add(leFle);
					Instruction leFleB = new Bne(temp, "x0", elseTxt);
					il.add(leFleB);
				break;
				case "GT":
				//Swap conditions around as it's opposite LE
					Instruction geFle = new Fle(cond.src2, cond.src1, temp);
					il.add(geFle);
					Instruction geFleB = new Bne(temp, "x0", elseTxt);
					il.add(geFleB);
				break;
				default:
					throw new Error("Issue with if float branch");
			}
		} 
		else {
			//Not a float comparison
	switch(cond.branch) {
		case "GT": //Greater than, generate bgt
			Instruction gt = new Bgt(cond.src1, cond.src2, elseTxt);
			il.add(gt);
			break;
		case "GE": //Greater than or equal, generate bge
			Instruction ge = new Bge(cond.src1, cond.src2, elseTxt);
			il.add(ge);
			break;
		case "LT": //Less than, generate blt
			Instruction lt = new Blt(cond.src1, cond.src2, elseTxt);
			il.add(lt);
			break;
		case "LE": //Less than or equal, generate ble
			Instruction le = new Ble(cond.src1, cond.src2, elseTxt);
			il.add(le);
			break;
		case "NE": //Not equal, generate bne
			Instruction ne = new Bne(cond.src1, cond.src2, elseTxt);
			il.add(ne);
			break;
		case "EQ": //Equal, generate beq
			Instruction eq = new Beq(cond.src1, cond.src2, elseTxt);
			il.add(eq);
			break;
		default:
			throw new Error("Issue with if non-float branch");
	}
	}

	//There will be the branch statement with the label by this point
		//First add the then code
		il.addAll(tlist.code);
		//Then add jump label to jump to end label if it fell through then code
		il.add(jumpLabel);
		//Add else label for branch to jump to if else
		il.add(elseLabel);
		//Add else code
		il.addAll(elist.code);
		//Add end label that would be jumped too from the jump label
		il.add(endLabel);
		//Add all these instructions to the code for the If CodeObject
		co.code.addAll(il);

		return co;
	}

		/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		loopLabel:
	 *		<cond code>
	 *		<flipped branch> outLabel
	 *		<body code>
	 *		j loopLabel
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(WhileNode node, CodeObject cond, CodeObject slist) {
		//Step 0:
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */

		//Step 1: Generate Labels
		String loopTxt = generateLoopLabel();
		String outTxt = generateOutLabel();
		Instruction loopLabel = new J(loopTxt);
		Instruction loopStart = new Label(loopTxt);
		Instruction outLabel = new Label(outTxt);

		
		

		//Step 4: Generate branch 
		InstructionList il = new InstructionList();

		//4.1: LoopLabel
		il.add(loopStart);

		//Step 4.2: Add code from conditional
		il.addAll(cond.code);

		//4.3: Branch
		switch (cond.branch) {
			case "LE": //Ble
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction leFle = new Fle(cond.src1, cond.src2, temp);
					il.add(leFle);
					Instruction leFleB = new Bne(temp, "x0", outTxt);
					il.add(leFleB);
				} else {
					Instruction le = new Ble(cond.src1, cond.src2, outTxt);
					il.add(le);
				}
			break;
			case "LT": //Blt
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction ltFlt = new Flt(cond.src1, cond.src2, temp);
					il.add(ltFlt);
					Instruction ltFltB = new Bne(temp, "x0", outTxt);
					il.add(ltFltB);
				} else {
					Instruction lt = new Blt(cond.src1, cond.src2, outTxt);
					il.add(lt);
				}
			break;
			case "GE": //Bge
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction geFlt = new Flt(cond.src2, cond.src1, temp);
					il.add(geFlt);
					Instruction geFltB = new Bne(temp, "x0", outTxt);
					il.add(geFltB);
				} else {	
					Instruction ge = new Bge(cond.src1, cond.src2, outTxt);
					il.add(ge);
				}
			break;
			case "GT": //Bgt
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction geFle = new Fle(cond.src2, cond.src1, temp);
					il.add(geFle);
					Instruction geFleB = new Bne(temp, "x0", outTxt);
					il.add(geFleB);
				} else {
					Instruction gt = new Bgt(cond.src1, cond.src2, outTxt);
					il.add(gt);
				}
			break;
			case "EQ": //Beq
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction eqFeq = new Feq(cond.src1, cond.src2, temp);
					il.add(eqFeq);
					Instruction eqFeqB = new Bne(temp, "x0", outTxt);
					il.add(eqFeqB);
				} else {
					Instruction eq = new Beq(cond.src1, cond.src2, outTxt);
					il.add(eq);
				}
			break;
			case "NE": //Bne
				if (cond.floatCond) {
					//Float Comparison
					String temp = generateTemp(Scope.Type.INT);
					Instruction neFeq = new Feq(cond.src1, cond.src2, temp);
					il.add(neFeq);
					Instruction neFeqB = new Beq(temp, "x0", outTxt);
					il.add(neFeqB);
				} else {
					Instruction ne = new Bne(cond.src1, cond.src2, outTxt);
					il.add(ne);
				}
			break;
			default:
				throw new Error("Issue with while branch");
		}

		//4.4: Body code
		il.addAll(slist.code);

		//4.5: J Loop
		il.add(loopLabel);

		//4.6: Out Label
		il.add(outLabel);

		co.code.addAll(il);

		return co;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generating code for returns
	 * 
	 * Step 0: Generate new code object
	 * 
	 * Step 1: Add retExpr code to code object (rvalify if necessary)
	 * 
	 * Step 2: Store result of retExpr in appropriate place on stack (fp + 8)
	 * 
	 * Step 3: Jump to out label (use @link{generateFunctionOutLabel()})
	 */
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/* MODIFY THIS TO GENERATE 3AC */

		InstructionList il = new InstructionList();

		il.addAll(retExpr.code);
		
		switch(node.getFuncSymbol().getReturnType()) {
			case INT:
				il.add(new Mv(retExpr.temp, generateTemp(Scope.Type.INT)));
			break;
			case FLOAT:
				il.add(new FMv(retExpr.temp, generateTemp(Scope.Type.FLOAT)));
			break;
			default: throw new Error("Issue pushing return");
		}
		
		il.add(new J(generateFunctionOutLabel()));

		co.code.addAll(il);
		
		return co;
	}

	@Override
	protected void preprocess(FunctionNode node) {
		// Generate function label information, used for other labels inside function
		currFunc = node.getFuncName();

		//reset register counts; each function uses new registers!
		intRegCount = 0;
		floatRegCount = 0;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generate code for functions
	 * 
	 * Step 1: add the label for the beginning of the function
	 * 
	 * Step 2: manage frame  pointer
	 * 			a. Save old frame pointer
	 * 			b. Move frame pointer to point to base of activation record (current sp)
	 * 			c. Update stack pointer
	 * 
	 * Step 3: allocate new stack frame (use scope infromation from FunctionNode)
	 * 
	 * Step 4: save registers on stack (Can inspect intRegCount and floatRegCount to know what to save)
	 * 
	 * Step 5: add the code from the function body
	 * 
	 * Step 6: add post-processing code:
	 * 			a. Label for `return` statements inside function body to jump to
	 * 			b. Restore registers
	 * 			c. Deallocate stack frame (set stack pointer to frame pointer)
	 * 			d. Reset fp to old location
	 * 			e. Return from function
	 */
	@Override
	protected CodeObject postprocess(FunctionNode node, CodeObject body) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/** ADD REGISTER ALLOCATION HERE
		 * 
		 * You may find it useful to do this in the following way:
		 * 
		 * 1. Write a register allocator class that is initialized with the number of int/fp registers to use, the code from
		 * 		`body`, and the function scope from `node` (the function scope gives you access to local/global variables)
		 * 2. Within the register allocator class, do the following
		 * 		a. Split the code in body into basic blocks
		 * 		b. (573 version) Perform liveness analysis on each basic block (assume globals and locals are live)
		 * 		b. (468/595 version) Assume all locals/globals/temporaries are live all the time
		 * 		c. Perform register allocation on each basic block using the algorithms presented in class,
		 * 			converting 3AC into assembly code with macro expansion
		 * 			i. Add code to track the state of the registers for each basic block (what is assigned to the register, whether it's dirty)
		 * 			ii. As you perform register allocation within a basic block, spill registers to memory as necessary. Use any
		 * 				heuristic you want to determine which registers to allocate and which to spill
		 * 			iii. If you need to spill a temporary to memory, you'll find it easiest to add the temporary as a new "local" variable
		 * 				to the local scope (you can just use the temporary name as the variable name); that will automatically allocate a spot
		 * 				in the activation record for it.
		 * 			iv. At the end of each basic block, save all dirty/live registers that hold globals/locals back to the stack
		 * 3. Once register allocation is done, track:
		 * 		a. How big the local scope is after spilling temporaries -- this affects allocating the stack frame
		 * 		b. How many total registers you used -- this affects the register save/restore code
		 * 4. Now generate code for your function as before, but using the updated information for register save/restore and frame allocation
		 */

		InstructionList il = new InstructionList();

		il.add(new Blank());

		il.add(new Blank("Function: " + currFunc));

		//Step 1: Add label for beginning of function
		//Want instruction of func_functionName:
		il.add(new Label(generateFunctionLabel(currFunc)));

		//Step 2: Manage frame pointer
		//a. Save old frame pointer
		il.add(new Sw("fp", "sp", "0"));

		//b. Move frame pointer to point to base of activation record
		il.add(new Mv("sp", "fp"));

		//c. Update stack pointer
		//Move sp down to next currently empty location
		il.add(new Addi("sp", "-4", "sp"));

		//3. Allocate new stack frame
		//Create space for all local variables? Get quantity from node's symbol table 
		//Move stack pointer down the value of node's scope's local quantity multiplied by 4
		il.add(new Addi("sp", "-" + String.valueOf((4*node.getScope().getNumLocals())), "sp"));

		il.add(new Blank("Saving Registers"));

		//Figure out how to know which registers to save?

		

		//InstructionList BBs[] = (generateBasicBlocks(getIntRegCount(), getFloatRegCount(), body, node.getScope()));
		LocalRegisterAllocator regAlloc = new LocalRegisterAllocator();
		il.addAll(regAlloc.allocate(body, node).code);
		//il.addAll(body.code);

		

		//c. Deallocate stack frame (Move stack pointer to frame pointer)
		il.add(new Mv("fp", "sp"));

		//d. Reset frame pointer to previous frame pointer
		il.add(new Lw("fp", "fp", "0"));

		//e. Return from function
		il.add(new Ret());

		il.add(new Blank("End of function " + currFunc));

		co.code.addAll(il);

		


		return co;
	}
	
	/**
	 * Generate code for the list of functions. This is the "top level" code generation function
	 * 
	 * Step 1: Set fp to point to sp
	 * 
	 * Step 2: Insert a JR to main
	 * 
	 * Step 3: Insert a HALT
	 * 
	 * Step 4: Include all the code of the functions
	 */
	@Override
	protected CodeObject postprocess(FunctionListNode node, List<CodeObject> funcs) {
		CodeObject co = new CodeObject();

		co.code.add(new Mv("sp", "fp"));
		co.code.add(new Jr(generateFunctionLabel("main")));
		co.code.add(new Halt());
		co.code.add(new Blank());

		//add code for each of the functions
		for (CodeObject c : funcs) {
			co.code.addAll(c.code);
			co.code.add(new Blank());
		}

		return co;
	}

	/**
	* 
	* FILL IN FOR STEP 4
	* 
	* Generate code for a call expression
	 * 
	 * Step 1: For each argument:
	 * 
	 * 	Step 1a: insert code of argument (don't forget to rvalify!)
	 * 
	 * 	Step 1b: push result of argument onto stack 
	 * 
	 * Step 2: alloate space for return value
	 * 
	 * Step 3: push current return address onto stack
	 * 
	 * Step 4: jump to function
	 * 
	 * Step 5: pop return address back from stack
	 * 
	 * Step 6: pop return value into fresh temporary (destination of call expression)
	 * 
	 * Step 7: remove arguments from stack (move sp)
	 */
	@Override
	protected CodeObject postprocess(CallNode node, List<CodeObject> args) {
		
		//STEP 0
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/* MODIFY THIS TO GENERATE 3AC */

		InstructionList il = new InstructionList();

		/*
		 * For each argument: 
		 * 1. rvalify if needed, then add code
		 * 2. Push the result onto the stack, so need to store depending on type of argument
		 * 3. Move stack pointer down to next open location on the stack
		 */
		int argsQuantity = args.size();
		for (int i = 0; i < argsQuantity; i++) {
			
			//Insert code of argument
			il.addAll(args.get(i).code);

			//Push result of argument onto stack
			switch(args.get(i).getType()) {
				case INT: 
					il.add(new Mv(args.get(i).temp, "sp"));
				break;
				case FLOAT: 
					il.add(new FMv(args.get(i).temp, "sp"));
				break;
				default: throw new Error("Issue pushing argument onto stack");
			}

			//Move sp
			il.add(new Addi("sp", "-4", "sp"));
		}

		//Allocate space for return value
		il.add(new Addi("sp", "-4", "sp"));

		//Push current return address onto stack
		il.add(new Sw("ra", "sp", "0"));

		//Move sp down
		il.add(new Addi("sp", "-4", "sp"));

		//Jump to function
		il.add(new Jr(generateFunctionLabel(node.getFuncName())));
		
		//Pop return address back from stack
		//To do so, increase stack pointer by 4 to point at ra
		il.add(new Addi("sp", "4", "sp"));
		//Load return address at sp into ra
		il.add(new Lw("ra", "sp", "0"));
		//Move stack pointer up to pop off ra off stack and point at return address
		il.add(new Addi("sp", "4", "sp"));

		//Pop return value into fresh temporary (destination of call expression)
		//Destination of call expression refers to this co's temp.

		switch(node.getType()) {
			case INT: 
				Instruction returnValInt = new Lw(generateTemp(Scope.Type.INT), "sp", "0");
				il.add(returnValInt);
				co.temp = returnValInt.getDest();
			break;
			case FLOAT: 
				Instruction returnValFlt = new Flw(generateTemp(Scope.Type.FLOAT), "sp", "0");
				il.add(returnValFlt);
				co.temp = returnValFlt.getDest();
			break; 
			default:
				throw new Error("Issue storing return value in callnode");
		}

		//Pop arguments off stack(Move sp up 4 for every argument)
		il.add(new Addi("sp", Integer.toString(4*args.size()), "sp"));
		
		co.code.addAll(il);
		


		return co;
	}	
	
	/**
	 * Generate a fresh temporary
	 * 
	 * @return new temporary register name
	 */
	protected String generateTemp(Scope.Type t) {
		switch(t) {
			case INT: return intTempPrefix + String.valueOf(++intRegCount);
			case FLOAT: return floatTempPrefix + String.valueOf(++floatRegCount);
			default: throw new Error("Generating temp for bad type");
		}
	}

	protected String generateLoopLabel() {
		return "loop_" + String.valueOf(++loopLabel);
	}

	protected String generateElseLabel() {
		return  "else_" + String.valueOf(++elseLabel);
	}

	protected String generateOutLabel() {
		return "out_" +  String.valueOf(++outLabel);
	}

	protected String generateFunctionLabel() {
		return "func_" + currFunc;
	}

	protected String generateFunctionLabel(String func) {
		return "func_" + func;
	}

	protected String generateFunctionOutLabel() {
		return "func_ret_" + currFunc;
	}
	
	/**
	 * Take a code object that results in an lval, and create a new code
	 * object that adds a load to generate the rval.
	 * 
	 * @param lco The code object resulting in an address
	 * @return A code object with all the code of <code>lco</code> followed by a load
	 *         to generate an rval
	 */
	protected CodeObject rvalify(CodeObject lco) {
		
		assert (lco.lval == true);
		CodeObject co = new CodeObject();

		/* THIS WON'T BE NECESSARY IF YOU'RE GENERATING 3AC */

		/* DON'T FORGET TO ADD CODE TO GENERATE LOADS FOR LOCAL VARIABLES */

		return co;
	}

	/**
	 * Generate an instruction sequence that holds the address of the variable in a code object
	 * 
	 * If it's a global variable, just get the address from the symbol table
	 * 
	 * If it's a local variable, compute the address relative to the frame pointer (fp)
	 * 
	 * @param lco The code object holding a variable
	 * @return a list of instructions that puts the address of the variable in a register
	 */
	private InstructionList generateAddrFromVariable(CodeObject lco) {

		InstructionList il = new InstructionList();

		//Step 1:
		SymbolTableEntry symbol = lco.getSTE();
		String address = symbol.addressToString();

		//Step 2:
		Instruction compAddr = null;
		if (symbol.isLocal()) {
			//If local, address is offset
			//need to load fp + offset
			//addi tmp' fp offset
			compAddr = new Addi("fp", address, generateTemp(Scope.Type.INT));
		} else {
			//If global, address in symbol table is the right location
			//la tmp' addr //Register type needs to be an int
			compAddr = new La(generateTemp(Scope.Type.INT), address);
		}
		il.add(compAddr); //add instruction to code object

		return il;
	}

}


