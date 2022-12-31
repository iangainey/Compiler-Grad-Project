package assembly;

import java.util.List;

import compiler.Scope.SymbolTableEntry;
import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public char intTempPrefix = 't';
	static final public char floatTempPrefix = 'f';
	
	int loopLabel;
	int elseLabel;
	int outLabel;

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
	
	//Instructor supplied "Starter Code"
	/**
	 * Generate code for Variables
	 * 
	 * Create a code object that just holds a variable
	 * 
	 * Important: add a pointer from the code object to the symbol table entry
	 *            so we know how to generate code for it later (we'll need to find
	 *            the address)
	 * 
	 * Mark the code object as holding a variable, and also as an lval
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();

		return co;
	}

	//Instructor supplied "Starter Code"
	/** Generate code for IntLiterals
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new Li(generateTemp(Scope.InnerType.INT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	//Instructor supplied "Starter Code"
	/** Generate code for FloatLiteras
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguFIish
		//for readability purposes.
		//li tmp' value
		Instruction i = new FImm(generateTemp(Scope.InnerType.FLOAT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	//Generate code for binary operations.
	@Override
	protected CodeObject postprocess(BinaryOpNode node, CodeObject left, CodeObject right) {

		CodeObject co = new CodeObject();

		//If left child is an lval, add load then add code
		if (left.lval == true) {
			left = rvalify(left);
		}

		co.code.addAll(left.code);

		if ((left.getType() != null && right.getType() != null) && left.getType().type != right.getType().type) {
			//Type discrepancy, so implicit conversion
			if (left.getType().type == Scope.InnerType.INT) {
				//Need to convert to a float
				Instruction ItoFloat = new IMovf(left.temp, generateTemp(Scope.InnerType.FLOAT));
				co.code.add(ItoFloat);
				left.temp = ItoFloat.getDest();
				left.type = right.getType();
			}
		} 

		if (right.lval == true) {
			right = rvalify(right);
		}
		
		co.code.addAll(right.code);

		//If left or right type is not null, and left and right type are not equal, and neither left or right type are pointers
		if ((left.getType() != null && right.getType() != null) && left.getType().type != right.getType().type && (left.getType().type != Scope.InnerType.PTR && right.getType().type != Scope.InnerType.PTR)) {
			if (right.getType().type == Scope.InnerType.INT) {
				//Need to convert to a float
				Instruction ItoFloat = new IMovf(right.temp, generateTemp(Scope.InnerType.FLOAT));
				co.code.add(ItoFloat);
				right.temp = ItoFloat.getDest();
				right.type = left.getType();
			}
		} 
		//Should now have rvals, which is what I want to operate on 

		InstructionList il = new InstructionList();
		if (left.getType() != null) {
		switch(left.getType().type) {
			case PTR:
			case INT: 
			switch(node.getOp()) {
				//Instruction dependent on which op it is
				case ADD:
					//Add instruction dependent on type, then make sure to update temp location
					Instruction iA = new Add(left.temp , right.temp, generateTemp(Scope.InnerType.INT));				
					il.add(iA);
					co.temp = iA.getDest();
					break;
				case SUB:
					Instruction iS = new Sub(left.temp , right.temp, generateTemp(Scope.InnerType.INT));
					il.add(iS);
					co.temp = iS.getDest();
					break;
				case MUL: 
					Instruction iM = new Mul(left.temp , right.temp, generateTemp(Scope.InnerType.INT));
					il.add(iM);
					co.temp = iM.getDest();
					break;
				case DIV: 
					Instruction iD = new Div(left.temp , right.temp, generateTemp(Scope.InnerType.INT));
					il.add(iD);
					co.temp = iD.getDest();
					break;
				default:
					throw new Error("Issue with operator");
			}
			break;
			case FLOAT: 
			switch(node.getOp()) {
				//Instruction dependent on which op it is
				case ADD:
					Instruction iA = new FAdd(left.temp , right.temp, generateTemp(Scope.InnerType.FLOAT));
					il.add(iA);
					co.temp = iA.getDest();
					break;
				case SUB:
					Instruction iS = new FSub(left.temp , right.temp, generateTemp(Scope.InnerType.FLOAT));
					il.add(iS);
					co.temp = iS.getDest();
					break;
				case MUL: 
					Instruction iM = new FMul(left.temp , right.temp, generateTemp(Scope.InnerType.FLOAT));
					il.add(iM);
					co.temp = iM.getDest();
					break;
				case DIV: 
					Instruction iD = new FDiv(left.temp , right.temp, generateTemp(Scope.InnerType.FLOAT));
					il.add(iD);
					co.temp = iD.getDest();
					break;
				default:
					throw new Error("Issue with operator");
			}
			break;
			case STRING: 
				throw new Error("Cannot perform operation on a string");
			default:
				throw new Error("Issue with binary operation" + String.valueOf(node.getType().type));

		}
		}
		co.code.addAll(il);
		co.lval = false;
		co.type = left.getType();


		return co;
	}

	//Generate code for unary operations.
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();

		if (expr.lval == true) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		switch(node.getType().type) {
			case INT: 
				Instruction i = new Neg(expr.temp, generateTemp(Scope.InnerType.INT));
				co.code.add(i);
				co.temp = i.getDest();
				
			break;
			case FLOAT: 
				Instruction iF = new FNeg(expr.temp, generateTemp(Scope.InnerType.INT));
				co.code.add(iF);
				co.temp = iF.getDest();
			break;
			default:
				throw new Error("Issue with urary operation");
		}
		co.type = node.getType();

		return co;
	}
	
	//Generate code for explicit casts 
	@Override
	protected CodeObject postprocess(CastNode node, CodeObject expr) {

		CodeObject co = new CodeObject();

		if (expr.lval) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);
		switch(node.getType().type) {
			//Type is the type to cast to, not from
			case INT: 
				if (expr.getType().type == Scope.InnerType.INT) {
					//Don't need to cast to an int, already an int
					co.temp = expr.temp;
					break;
				}
				Instruction FtoInt = new FMovi(expr.temp, generateTemp(Scope.InnerType.INT));
				co.code.add(FtoInt);
				co.temp = FtoInt.getDest();
				co.type = node.getType();
			break;
			case FLOAT: 
				if (expr.getType().type == Scope.InnerType.FLOAT) {
					//Don't need to cast to a float, already a float
					co.temp = expr.temp;
					break;
				}
				Instruction ItoFloat = new IMovf(expr.temp, generateTemp(Scope.InnerType.FLOAT));
				co.code.add(ItoFloat);
				co.temp = ItoFloat.getDest();
				co.type = node.getType();
			break;
			default:
				throw new Error("Invalid type to cast to");
		}
		

		return co;
	}


	//Generate code for assignment statements
	//sw rhs 0(lhs)
	@Override
	protected CodeObject postprocess(AssignNode node, CodeObject left,
			CodeObject right) {
		
		CodeObject co = new CodeObject();

		assert(left.lval == true); //left hand side had better hold an address

		//Step 1a
		if (left.isVar()) {
			//If a variable, need to load address to store to
			InstructionList il = new InstructionList();

			if (left.getSTE().isLocal()) {
				//If it's a local variable, need to store accounting for the offset

				//Add left and right code, rvalifying if needed
				co.code.addAll(left.code);

				if (right.lval == true) {
					right = rvalify(right);
				}

				co.code.addAll(right.code);

				
				if (left.getType() != right.getType() && left.getType() != null && right.getType() != null) {

					if (left.getType().type == Scope.InnerType.INT && right.getType().type == Scope.InnerType.FLOAT) {
						//Cast RHS to an int
						Instruction FtoInt = new FMovi(right.temp, generateTemp(Scope.InnerType.INT));
						co.code.add(FtoInt);
						co.temp = FtoInt.getDest();
						right.temp = FtoInt.getDest();
						co.type = left.getType();
					}
					else if (left.getType().type == Scope.InnerType.FLOAT && right.getType().type == Scope.InnerType.INT) {
						//Cast rhs to a float
						Instruction ItoFloat = new IMovf(right.temp, generateTemp(Scope.InnerType.FLOAT));
						co.code.add(ItoFloat);
						co.temp = ItoFloat.getDest();
						right.temp = ItoFloat.getDest();
						co.type = left.getType();
					}
					
				}
				//If local, store directly to offset address

				switch (left.getType().type) {
					case INT: 
						//Store the right side of assignment statement into the address of the left
						//side of the assignment statement, which is offset from the frame pointer.
						Instruction loadIntLocal = new Sw(right.temp, "fp", String.valueOf(left.getSTE().addressToString()));
						il.add(loadIntLocal);
						co.temp = loadIntLocal.getDest();
						co.type = left.getType();
					break;
					case FLOAT: 
						Instruction loadFloatLocal = new Fsw(right.temp, "fp", String.valueOf(left.getSTE().addressToString()));
						il.add(loadFloatLocal);
						co.temp = loadFloatLocal.getDest();
						co.type = left.getType();
					break;
					case PTR:
						Instruction ptrSw = new Sw(right.temp, "fp", String.valueOf(left.getSTE().addressToString()));
						il.add(ptrSw);
						co.temp = ptrSw.getDest();
						co.type = left.getType();
					break;
					default: throw new Error("Issue in load in rvalify" + String.valueOf(left.getType().type));
				}

				co.code.addAll(il);

			}
			else { 
				//If left side is a global variable
				//load address then store to that address

				//Load address from left variable, add to co
				co.code.addAll(generateAddrFromVariable(left));
				//Update temp to address generated from variable
				left.temp = co.code.getLast().getDest();

				//Now that the address has been loaded, add code of the left side
				il.addAll(left.code);
				//right side needs to be an rval. So, if it's an lval, convert to rval
				if (right.lval == true) {
					right = rvalify(right);
				}
				//Now that right side is a rval, add code of the right side
				il.addAll(right.code);

				//Now, need to generate a store (sw rhs 0(lhs))
				//Store rhs into lhs with an offset of 0
				//Depending on type, generate correct store
				switch(node.getType().type) {
					case INT:
						il.add(new Sw(right.temp, left.temp, "0"));
						break;
					case FLOAT:
						il.add(new Fsw(right.temp, left.temp, "0"));
						break;
					default:
						throw new Error("Not able to assign an int or float");
				}
				//Add instruction list code to code object
				co.code.addAll(il);
			}
		} 
		else {
			//If it's not a var, it's a pointer
			if (right.lval) {
				right = rvalify(right);
			}
			co.code.addAll(left.code);
			co.code.addAll(right.code);

			switch (node.getType().type) {
				case PTR:
				case INT: 
					co.code.add(new Sw(right.temp, left.temp, "0"));
				break;
				case FLOAT: 
					co.code.add(new Fsw(right.temp, left.temp, "0"));
				break;
				default:
					throw new Error("Invalid type" + String.valueOf(node.getType().type));
			}
			
			co.type = left.getType();
		}

		//Just as a precaution to ensure it's kept as an lval incase it is inadvertenly altered, probably unnecessary tho
		co.lval = true;
		
		return co;
	}

	//Instructor supplied "Starter Code"
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
	
	//Instructor supplied "Starter Code"
	//Generate code for read
	@Override
	protected CodeObject postprocess(ReadNode node, CodeObject var) {
		
		//Step 0
		CodeObject co = new CodeObject();

		//Generating code for read(id)
		assert(var.getSTE() != null); //var had better be a variable

		InstructionList il = new InstructionList();
		switch(node.getType().type) {
			case INT: 
				//Code to generate if INT:
				//geti tmp
				//if var is global: la tmp', <var>; sw tmp 0(tmp')
				//if var is local: sw tmp offset(fp)
				Instruction geti = new GetI(generateTemp(Scope.InnerType.INT));
				il.add(geti);
				InstructionList store = new InstructionList();
				if (var.getSTE().isLocal()) {
					store.add(new Sw(geti.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					store.addAll(generateAddrFromVariable(var));
					store.add(new Sw(geti.getDest(), store.getLast().getDest(), "0"));
				}
				il.addAll(store);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf tmp
				//if var is global: la tmp', <var>; fsw tmp 0(tmp')
				//if var is local: fsw tmp offset(fp)
				Instruction getf = new GetF(generateTemp(Scope.InnerType.FLOAT));
				il.add(getf);
				InstructionList fstore = new InstructionList();
				if (var.getSTE().isLocal()) {
					fstore.add(new Fsw(getf.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					fstore.addAll(generateAddrFromVariable(var));
					fstore.add(new Fsw(getf.getDest(), fstore.getLast().getDest(), "0"));
				}
				il.addAll(fstore);
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

	//Instructor supplied "Starter Code"
	//Generate code for print
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType().type == Scope.InnerType.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);
			
			System.out.println("; generating code to print " + expr.getSTE());

			//Get the address of the variable
			InstructionList addrCo = generateAddrFromVariable(expr);
			co.code.addAll(addrCo);

			//Step 2:
			Instruction write = new PutS(addrCo.getLast().getDest());
			co.code.add(write);
		} else {
			//Step 1a:
			//if expr is an lval, load from it
			if (expr.lval == true) {
				expr = rvalify(expr);
			}
			
			//Step 1:
			co.code.addAll(expr.code);

			//Step 2:
			//if type of writenode is int, use puti, if float, use putf
			Instruction write = null;
			switch(node.getWriteExpr().getType().type) {
			case STRING: throw new Error("Shouldn't have a STRING here");
			case INT: 
			case PTR: //should work the same way for pointers
				write = new PutI(expr.temp); break;
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


	//Generating an instruction sequence for a conditional expression
	@Override
	protected CodeObject postprocess(CondNode node, CodeObject left, CodeObject right) {
		CodeObject co = new CodeObject();

		/* Create code for left and right conditional. Check if they're lvals,
		 * if so want to rvalify as want rvals to be used in comparison.
		 */
		if (left.lval == true) {
			left = rvalify(left);
		}
		if (right.lval == true) {
			right = rvalify(right);
		}
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

		if ((left.getType() != null && right.getType() != null) && (left.getType().type == Scope.InnerType.FLOAT || right.getType().type == Scope.InnerType.FLOAT)) {
			co.floatCond = true;
		}

		return co;
	}

	//Generating instruction sequence for if/else statements
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();

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
			String temp = generateTemp(Scope.InnerType.INT);
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

	//Generate instruction sequence for while loops
	@Override
	protected CodeObject postprocess(WhileNode node, CodeObject cond, CodeObject slist) {
		//Step 0:
		CodeObject co = new CodeObject();

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
					String temp = generateTemp(Scope.InnerType.INT);
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
					String temp = generateTemp(Scope.InnerType.INT);
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
					String temp = generateTemp(Scope.InnerType.INT);
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
					String temp = generateTemp(Scope.InnerType.INT);
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
					String temp = generateTemp(Scope.InnerType.INT);
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
					String temp = generateTemp(Scope.InnerType.INT);
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

	//Generate code for returns
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		CodeObject co = new CodeObject();

		if (node.getFuncSymbol().getReturnType().type == Scope.InnerType.VOID) {
			return co;
		}

		InstructionList il = new InstructionList();
		//Step 1: Check retExpr for lval, rvalify if needed. Then add code
		if (retExpr.lval) {
			retExpr = rvalify(retExpr);
		}
		il.addAll(retExpr.code);
		
		//Step 2: Store result of retExpr in appropriate place on stack (8 above fp)
		switch(node.getFuncSymbol().getReturnType().type) {
			case PTR:
			case INT:
				il.add(new Sw(retExpr.temp, "fp", "8"));
			break;
			case FLOAT:
				il.add(new Fsw(retExpr.temp, "fp", "8"));
			break;
			default: throw new Error("Issue pushing return");
		}

		//3. Jump to out label
		il.add(new J(generateFunctionOutLabel()));

		co.code.addAll(il);

		return co;
	}

	//Instructor supplied "Starter Code"
	@Override
	protected void preprocess(FunctionNode node) {
		// Generate function label information, used for other labels inside function
		currFunc = node.getFuncName();

		//reset register counts; each function uses new registers!
		intRegCount = 0;
		floatRegCount = 0;
	}


	//Generate code for functions
	@Override
	protected CodeObject postprocess(FunctionNode node, CodeObject body) {
		CodeObject co = new CodeObject();

		InstructionList il = new InstructionList();

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

		//4. Save registers on stack
		/*
		 * When function is called, reg counts are set to 0. Every time a temp is generated
		 * the reg count goes up, so, need to save all registers that were generated
		 * can do this by using prefix depending on type, then storing that register from 1 
		 * to reg count
		 */
		int intRegistersSaved = 0;
		int floatRegistersSaved = 0;
		if (getIntRegCount() > 0) {
			//Save these registers
			for (int i = 1; i <= getIntRegCount(); i++) {
				il.add(new Sw((intTempPrefix + String.valueOf(i)), "sp", "0"));
				il.add(new Addi("sp", "-4", "sp"));
				intRegistersSaved++;
			}

		}
		 
		if (getFloatRegCount() > 0) {
			//Save these float registers
			for (int i = 1; i <= getFloatRegCount(); i++) {
				il.add(new Fsw((floatTempPrefix + String.valueOf(i)), "sp", "0"));
				il.add(new Addi("sp", "-4", "sp"));
				floatRegistersSaved++;
			}
		}
		
		

		//Step 5: Add code from function body
		il.addAll(body.code);
		
		//Step 6: Add post processing code

		//a. Label for return statements inside function body to jump to
		il.add(new Label(generateFunctionOutLabel()));

		//b. Restore registers
		//Want to move sp up for, load data, repeat
		//Call in opposite order as above: floats saved last, so they must be restored first
		if (floatRegistersSaved > 0) {
			for (int i = floatRegistersSaved; i >= 1; i--) {
				il.add(new Addi("sp", "4", "sp"));
				il.add(new Flw((floatTempPrefix + String.valueOf(i)), "sp", "0"));
			}
		}
		if (intRegistersSaved > 0) {
			for (int i = intRegistersSaved; i >= 1; i--) {
				il.add(new Addi("sp", "4", "sp"));
				il.add(new Lw((intTempPrefix + String.valueOf(i)), "sp", "0"));
			}
		}
		
		//c. Deallocate stack frame (Move stack pointer to frame pointer)
		il.add(new Mv("fp", "sp"));

		//d. Reset frame pointer to previous frame pointer
		il.add(new Lw("fp", "fp", "0"));

		//e. Return from function
		il.add(new Ret());

		co.code.addAll(il);

		return co;
	}

	
	//Instructor supplied "Starter Code"
	//Generate code for the list of functions. This is the "top level" code generation function
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


	//Generate code for a call expression
	@Override
	protected CodeObject postprocess(CallNode node, List<CodeObject> args) {
		
		//STEP 0
		CodeObject co = new CodeObject();

		InstructionList il = new InstructionList();

		/*
		 * For each argument: 
		 * 1. rvalify if needed, then add code
		 * 2. Push the result onto the stack, so need to store depending on type of argument
		 * 3. Move stack pointer down to next open location on the stack
		 */
		int argsQuantity = args.size();
		for (int i = 0; i < argsQuantity; i++) {
			//Rvalify if required
			if (args.get(i).lval == true) {
				//If it's an lval, want it as an rval
				args.set(i, rvalify(args.get(i)));
			}

			//Insert code of argument
			il.addAll(args.get(i).code);

			//Push result of argument onto stack
			if (args.get(i).getType() != null) {
			switch(args.get(i).getType().type) {
				case INT: 
					il.add(new Sw(args.get(i).temp, "sp", "0"));
				break;
				case FLOAT: 
					il.add(new Fsw(args.get(i).temp, "sp", "0"));
				break;
				case PTR: 
					il.add(new Sw(args.get(i).temp, "sp", "0"));
				break;
				default: throw new Error("Issue pushing argument onto stack");
			}
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
		switch (node.getType().type) {
			case PTR:
			case INT: 
				Instruction returnValInt = new Lw(generateTemp(Scope.InnerType.INT), "sp", "0");
				il.add(returnValInt);
				co.temp = returnValInt.getDest();
			break;
			case FLOAT:
				Instruction returnValFlt = new Flw(generateTemp(Scope.InnerType.FLOAT), "sp", "0");
				il.add(returnValFlt);
				co.temp = returnValFlt.getDest();
			break;
			case VOID: 
				//Do nothing
			break;
			default: throw new Error("Issue storing return value" + String.valueOf(node.getType().type));
		}

		//Pop arguments off stack(Move sp up 4 for every argument)
		il.add(new Addi("sp", Integer.toString(4*args.size()), "sp"));

		co.code.addAll(il);

		return co;
	}	
	
	//Generate code for * (expr)
	@Override
	protected CodeObject postprocess(PtrDerefNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		if (expr.lval == true) {
			expr = rvalify(expr);
		}

		co.code.addAll(expr.code);

		co.type = node.getType();
		co.temp = expr.temp;
		co.lval = true;

		return co;
	}

	
	//Generate code for a & (expr)
	@Override
	protected CodeObject postprocess(AddrOfNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		if (expr.getSTE() != null && expr.getSTE().isLocal()) {
			co.code.addAll(generateAddrFromVariable(expr));
			expr.temp = co.code.getLast().getDest();
		}
		co.code.addAll(expr.code);
		//co.code.add(new Label(String.valueOf(expr.temp)));
		co.temp = expr.temp;
		co.lval = false;
		co.type = node.getType();

		return co;
	}

	//Generate code for malloc
	@Override
	protected CodeObject postprocess(MallocNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		if (expr.lval) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);

		String newDest = generateTemp(Scope.InnerType.INT);
		Instruction alloc = new Malloc(expr.temp, newDest);
		co.code.add(alloc);
		co.temp = newDest;
		co.type = node.getType();

		return co;
	}
	

	//Generate code for free
	@Override
	protected CodeObject postprocess(FreeNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		if (expr.lval) {
			expr = rvalify(expr);
		}
		co.code.addAll(expr.code);

		Instruction freeIns = new Free(expr.temp);
		co.code.add(freeIns);

		return co;
	}

	//Instructor supplied "Starter Code"
	/**
	 * Generate a fresh temporary
	 * 
	 * @return new temporary register name
	 */
	protected String generateTemp(Scope.InnerType t) {
		switch(t) {
			case INT: 
			case PTR: //works the same for pointers
				return intTempPrefix + String.valueOf(++intRegCount);
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
		
		/*
		 * If rvalify is being called, lco must be a variable(I believe), so this rvalify function
		 * generates a load from the address of the variable, and returns a code object that has the
		 * value at that address stored in it's temp
		 */
		
		 //Calling this to turn an lval to an rval, this better be an lval
		 assert (lco.lval == true);

		 CodeObject co = new CodeObject();
 
		 InstructionList il = new InstructionList();
		 
		 if (lco.getSTE() != null && lco.getSTE().isLocal()) {

			 //If lco is a local variable, need to generate load with offset relative to frame pointer
			 switch (lco.getType().type) {
				 case INT: 
					 Instruction loadIntLocal = new Lw(generateTemp(Scope.InnerType.INT), "fp", String.valueOf(lco.getSTE().addressToString()));
					 il.add(loadIntLocal);
					 co.temp = loadIntLocal.getDest();
					 co.type = lco.getType();
				 break;
				 case FLOAT: 
					 Instruction loadFloatLocal = new Flw(generateTemp(Scope.InnerType.FLOAT), "fp", String.valueOf(lco.getSTE().addressToString()));
					 il.add(loadFloatLocal);
					 co.temp = loadFloatLocal.getDest();
					 co.type = lco.getType();
				 break;
				 case PTR: 
				 	Instruction ptrLoad = new Lw(generateTemp(Scope.InnerType.INT), "fp", String.valueOf(lco.getSTE().addressToString()));
					il.add(ptrLoad);
					co.temp = ptrLoad.getDest();
					co.type = lco.getType();
				 break;
				 default: throw new Error("Issue in load in rvalify");
			 }
			 il.addAll(lco.code);
		 }
		 else {
			 //If lco is a global variable, need to generate load with no offset. 
 
			 /*
			  * Generate load to get address from symbol table as it is accurate with no offset 
			  * Here, this call could be considered a bit unnecessary and a load directly in this function
			  * may make it easier to read. However, due to part of this code being provided by the instructors,
			  * trying to keep it similar to that code
			  */
			if (lco.getSTE() != null) {
				il.addAll(generateAddrFromVariable(lco));
				//Update temp to temporary generated in above call
				lco.temp = il.getLast().getDest();
			}
			 
 
			 //Not that lco's address has been loaded, add lco code
			 il.addAll(lco.code);
 
			 //Generate load with no offset
			 if (lco.getType() == null) {
				//throw new Error(String.valueOf(lco.type));
			 }
			 if (lco.getType() != null) {
			 switch (lco.getType().type) {
				 case INT: 
					 Instruction loadInt = new Lw(generateTemp(Scope.InnerType.INT), lco.temp, "0");
					 il.add(loadInt);
					 co.temp = loadInt.getDest();
				 break;
				 case FLOAT: 
					 Instruction loadFlt = new Flw(generateTemp(Scope.InnerType.FLOAT), lco.temp, "0");
					 il.add(loadFlt);
					 co.temp = loadFlt.getDest();
				 break;
				 case PTR: 
				 	Instruction loadPtr = new Lw(generateTemp(Scope.InnerType.INT), lco.temp, "0");
					il.add(loadPtr);
					co.temp = loadPtr.getDest();
				 break;
				 default:
					 throw new Error("Issue in rvalify");
			 }
			}
		 }
		
		 
		 co.code.addAll(il);
		 co.lval = false;
		 co.type = lco.getType();

		return co;
	}

	//Instructor supplied "Starter Code"
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
			compAddr = new Addi("fp", address, generateTemp(Scope.InnerType.INT));
		} else {
			//If global, address in symbol table is the right location
			//la tmp' addr //Register type needs to be an int
			compAddr = new La(generateTemp(Scope.InnerType.INT), address);
		}
		il.add(compAddr); //add instruction to code object

		return il;
	}

}
