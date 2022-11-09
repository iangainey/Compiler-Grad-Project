package assembly;

import java.util.List;
import java.util.Vector;
import java.util.Set;


import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;
import compiler.Scope.SymbolTableEntry;
import compiler.LocalScope;

public class LocalRegisterAllocator extends CodeGenerator{

    RegisterFile registers = new RegisterFile();

    LocalScope funcScope;
    String currFunc;
    Scope.Type returnType;


    public CodeObject allocate(CodeObject body, FunctionNode node) {
        //This is the entry point from codegenerator.
        //This is called to convert the 3AC body to assembly, performing register allocation
        //Called to convert 3AC body to assembly w/register assignment from codegenerator
        CodeObject co = new CodeObject();

        //Scope to be used for determining global/local/temp
        funcScope = node.getScope();
        currFunc = node.getFuncName();

        //Splits body up into basic blocks
        Vector<InstructionList> BBs = generateBasicBlocks(body);

        InstructionList il = new InstructionList();
        //Loop through each basic block. Convert 3AC to assembly, perform register
        //assignment, and add to output il
        for (int i = 0; i < BBs.size(); i++) {
            il.addAll(allocateBasicBlock(BBs.get(i)).code);
        }

        co.code.addAll(registers.saveRegsStart());
        
        co.code.addAll(il);

        co.code.addAll(registers.saveRegsEnd());

        return co;
    }

    public Vector<InstructionList> generateBasicBlocks(CodeObject body) {
        //Takes the function body codeobject, and splits it up into a vector of type
        //instructionlist. Each node in the vector is a instructionlist of a single basic block

        //Step 1: Identify leaders
            //A leader of a basic block is either the first line of a function or a label
        //Step 2: Identify end
            //The end of a basic block is a branch or jump statement. These are the last statements in the block
            Vector<InstructionList> BBs = new Vector<InstructionList>();
        
            InstructionList BB = new InstructionList();
            for (int i = 0; i < body.code.nodes.size(); i++) {
                //If i is a leader, start basic block with i
                //First line or label
                Instruction stmt = body.code.nodes.get(i);
                
                String op = null;
                if (stmt.getOC() != null) {
                    op = stmt.getOC().toString();
                }
                if (BB.isEmpty()) {
                    //If empty, first statement is leader
                    BB = new InstructionList();
                    //BB.add(new Blank("Start of BB"));
                    BB.add(stmt);
                    //if (String.valueOf(stmt).contains("out") && String.valueOf(stmt).contains("J") == false) {
                        //throw new Error(String.valueOf(stmt));
                    //}
                    if (i == body.code.nodes.size()-1) {
                        //If last statement (or only)
                        BBs.add(BB);
                    }
                }
                else if (stmt.toString().contains(":")) {
                    //if not empty, and statement is label, new basic block
                    BBs.add(BB);
                    BB = new InstructionList();
                    //BB.add(new Blank("Start of BB"));
                    BB.add(stmt);
                    
                }
                else if((op == "BEQ" || op == "BGE" || op == "BGT" || op == "BLE" || op == "BLT" || op == "BNE" || op == "J") && op != null) {
                    //If a branch or jump, end basic block w/it
                    BB.add(stmt);
                    //BB.add(new Blank("End of BB"));
                    //Add basic block to list of blocks
                    BBs.add(BB);
                    //Overwrite BB w/a new list to create a new basic block
                    BB = new InstructionList();
                }
    
                else {
                    BB.add(stmt);
                }
            }
            return BBs;
    
    }

    public CodeObject allocateBasicBlock(InstructionList bb) {
        //This function takes a single basic block and performs register allocation as well
        //as converts the 3AC to assembly

        //Step 1: Insert blanks/start of bb blank
        CodeObject co = new CodeObject();
        
        InstructionList il = new InstructionList();

        il.add(new Blank());

        il.add(new Blank("Start of BB"));
        //Step 2: Loop through the InstructionList basic block
            //Step 2a: Check opcode of instruction each line, and generate appropriate instructions
            for (int i = 0; i < bb.size(); i++) {
            
                //For each instruction
                Instruction stmt = bb.nodes.get(i);
    
                //Beginning of basic blocks:
                    //First line of program
                    //Labels
                    //First line after a branch
                //End of basic blocks:
                    //J's
                    //Branch
                    //When a label is the next instruction
    
                //When to call saves
                    //Before a branch
                    //Before a label
                    //Before a j
                
                String oc = String.valueOf(stmt.getOC());
            //Branch Statement: Branch is the last instruction in a basic block. Check,
            //If it's not, throw an error. If it is, convert the 3AC code to assembly,
            //but ensure that registers are saved prior to inserting branch code.
            Set<String> branchOps = Set.of("BEQ", "BGE", "BGT", "BLE", "BLT", "BNE");

            if (branchOps.contains(oc) || oc == "FLT.S" || oc == "FLE.S" || oc == "FEQ.S") {
                //Is a branch
                InstructionList temp = new InstructionList();
                temp.addAll(new macroExpandBranch(stmt).toReturn());

                for (int ins = 0; ins < temp.size(); ins++) {
                    //il.add(new Blank("Before: " + temp.nodes.get(ins)));
                    if (branchOps.contains(String.valueOf(temp.nodes.get(ins).getOC()))) {
                        //If a branch statement
                        //il.add(new Blank("HIT: " + temp.nodes.get(ins)));
                        il.add(new Blank("Saving registers at end of BB"));
                        il.addAll(registers.saveRegsEndBB());
                        il.add(temp.nodes.get(ins));
                    } else {
                        //il.add(new Blank("Else'd: " + temp.nodes.get(ins)));
                        il.add(temp.nodes.get(ins));
                    }
                }
            }
            else if (oc == "PUTI" || oc == "PUTF") {
                //If OC is a put, expanded in same way
                il.addAll(new macroExpandPut(stmt).toReturn());
            }
            else if (oc == "PUTS") {
                il.addAll(new macroExpandPutS(stmt).toReturn());
            }
            else if (oc == "GETI" || oc == "GETF") {
                //If oc is a get, expanded in same way
                il.addAll(new macroExpandGet(stmt).toReturn());
            }
            else if (oc == "ADD" || oc == "SUB" || oc == "DIV" || oc == "MUL" || oc == "FADD.S" || oc == "FSUB.S" || oc == "FDIV.S" || oc == "FMUL.S") {
                //Expanded in same way, will need to differentiate floats tho
                il.addAll(new macroExpandOp(stmt).toReturn());
            }
            else if (oc == "LI" || oc == "FIMM.S") {
                il.addAll(new macroExpandLoadImm(stmt).toReturn());
            }
            else if (oc == "SW" || oc == "FSW") {
                il.add(stmt);
            }
            else if (oc == "MV" || oc == "FMV.S") {
                il.addAll(new macroExpandMove(stmt).toReturn());
            }
            else if (oc == "NEG" || oc == "FNEG") {
                il.addAll(new macroExpandUnary(stmt).toReturn());
            } 
            else if (oc == "LW" || oc == "FLW") {
                il.addAll(new macroExpandLoadWrd(stmt).toReturn());
            }
            //else if (oc == "FLT.S" || oc == "FLE.S" || oc == "FEQ.S") {
                //il.addAll()
            //}
            //Jump: If opcode is J, NOT JR, need to get a return value and store appropriatly
            //Return val sould be in destination of previous statement added to il
            //After return val is obtained, need to save registers, then generate appropriate 
            //store for return value. Don't forget to still add the J statement
            else if (oc.contains("J") && oc.contains("JR") == false) {
                String returnReg = il.getLast().getDest();
                il.add(new Blank("Saving registers at end of BB"));
                //If this is the end of a function, save the return value
                if (stmt.toString().contains("func")) {
                    //Add last instruction in basic block as return register, save to return address
                         
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(currFunc);
                    switch(ste.getType()) {
                    case INT: 
                        il.add(new Sw(returnReg, "fp", "8"));
                    break;
                    case FLOAT: 
                        il.add(new Fsw(returnReg, "fp", "8"));
                    break;
                    }
                }
                
                il.addAll(registers.saveRegsEndBB());
                il.add(stmt); 
            }
            else {
                il.add(stmt);
            }
            //Last line: If the statement is neither of the above, but it is the last line of the 
            //basic block, need t0 save registers
            if (i == bb.size()-1 && branchOps.contains(oc) == false && oc.contains("J") == false) {
                il.add(new Blank("Saving registers at end of BB"));
                il.addAll(registers.saveRegsEndBB());
            }
            //To test, uncomment this
            //il.add(bb.nodes.get(i));     
    }
    il.add(new Blank("End of BB"));

    co.code.addAll(il);
    return co;
}


    public abstract class macroExpand3O {

        Register src1;
        Register src2;
        Register dest;
        String value;
        Instruction.OpCode oc;
        Scope.Type type;
        Set<String> excls = Set.of("x0", "x1","x2", "x8");
        InstructionList il;

        public macroExpand3O() {
            //Default constructor, but shouldn't ever need
        }

        public macroExpand3O(Instruction i) {
            //Get opcode, type, src's and destination for any instruction that has them
            this.oc = i.getOC();
            if(i.toString().contains("F") || i.toString().contains(".S")) {
                this.type = Scope.Type.FLOAT;
            } else {
                this.type = Scope.Type.INT;
            }
            il = new InstructionList();
            if (i.getOperand(Instruction.Operand.SRC1) != null && this.oc != Instruction.OpCode.PUTS) {
                this.src1 = ensureSource(i.getOperand(Instruction.Operand.SRC1), type, il);
            }
            if (i.getOperand(Instruction.Operand.SRC2) != null) {
                this.src2 = ensureSource(i.getOperand(Instruction.Operand.SRC2), type, il);
            }
            if (i.getOperand(Instruction.Operand.DEST) != null) {
                if (this.oc == Instruction.OpCode.FLT || this.oc == Instruction.OpCode.FLE || this.oc == Instruction.OpCode.FEQ) {
                    this.dest = ensureDest(i.getOperand(Instruction.Operand.DEST), Scope.Type.INT, il);
                }
                else {
                    this.dest = ensureDest(i.getOperand(Instruction.Operand.DEST), type, il);
                }
            }
            if (i.getLabel() != null) {
                this.value = i.getLabel();
            }
        }
        
        public InstructionList toReturn() {
            return il;
        }
    }

    public class macroExpandBranch extends macroExpand3O {
        
        public macroExpandBranch(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.BEQ) {
                il.add(new Beq(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.BGE) {
                il.add(new Bge(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.BGT) {
                il.add(new Bgt(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.BLE) {
                il.add(new Ble(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.BLT) {
                il.add(new Blt(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.BNE) {
                il.add(new Bne(src1.name, src2.name, value));
            }
            else if (this.oc == Instruction.OpCode.FLT) {
                il.add(new Flt(src1.name, src2.name, dest.name));
            }
            else if (this.oc == Instruction.OpCode.FLE) {
                il.add(new Fle(src1.name, src2.name, dest.name));
            }
            else if (this.oc == Instruction.OpCode.FEQ) {
                il.add(new Feq(src1.name, src2.name, dest.name));
            }
        }
    }
    public class macroExpandPut extends macroExpand3O {
        
        public macroExpandPut(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.PUTI) {
                il.add(new PutI(src1.name));
            }
            else if (this.oc == Instruction.OpCode.PUTF) {
                il.add(new PutF(src1.name));
            }
        }
    }
    public class macroExpandPutS extends macroExpand3O {
        
        public macroExpandPutS(Instruction i) {
            super(i);
            //PutS is only print address
            getGlobalAddress(i.getOperand(Instruction.Operand.SRC1), il);
            il.add(new PutS("x3"));
        }
    }

    public class macroExpandGet extends macroExpand3O {
        
        public macroExpandGet(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.GETI) {
                il.add(new GetI(dest.name));
            }
            else if (this.oc == Instruction.OpCode.GETF) {
                il.add(new GetF(dest.name));
            }
        }
    }
    public class macroExpandOp extends macroExpand3O {
        
        public macroExpandOp(Instruction i) {
            super(i);
            //Operations such as add, sub, div, and mul (for floats too)
            if (this.oc == Instruction.OpCode.ADD) {
                il.add(new Add(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.SUB) {
                il.add(new Sub(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.DIV) {
                il.add(new Div(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.MUL) {
                il.add(new Mul(src1.name, src2.name, dest.name));
            }
            //Floats
            if (this.oc == Instruction.OpCode.FADDS) {
                il.add(new FAdd(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.FSUBS) {
                il.add(new FSub(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.FDIVS) {
                il.add(new FDiv(src1.name, src2.name, dest.name));
            }
            if (this.oc == Instruction.OpCode.FMULS) {
                il.add(new FMul(src1.name, src2.name, dest.name));
            }
        }
    }
    public class macroExpandLoadImm extends macroExpand3O {
        
        public macroExpandLoadImm(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.LI) {
                il.add(new Li(dest.name, value)); 
            }
            else if (this.oc == Instruction.OpCode.FIMMS) {
                il.add(new FImm(dest.name, value));
            }
        }
    }
    public class macroExpandLoadWrd extends macroExpand3O {
        
        public macroExpandLoadWrd(Instruction i) {
            super(i);
            if (this.dest.holds == null) {
                //ra was passed in, no need to generate a load its generate in ensureDest
            } else {
                switch(type) {
                    case INT: 
                        il.add(new Lw(dest.name, "sp", "0"));
                    break;
                    case FLOAT: 
                        il.add(new Flw(dest.name, "sp", "0"));
                    break;
                    default:
                        throw new Error("Invalid type for loadwrd");
                }
                
            }
        }
    }
    public class macroExpandStore extends macroExpand3O {
        
        public macroExpandStore(Instruction i) {
            super(i);
        }
    }
    public class macroExpandUnary extends macroExpand3O {
        
        public macroExpandUnary(Instruction i) {
            super(i);
            if (this.type == Scope.Type.INT) {
                il.add(new Neg(src1.name, dest.name));
            } 
            else {
                il.add(new FNeg(src1.name, dest.name));
            }
        }
    }
    public class macroExpandMove extends macroExpand3O {
        //Moves can just be move src into dest
        //Or move src into a dest of sp. If so, this should be a store reg into sp w/0 offset
        public macroExpandMove(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.MV) {
                if (this.dest.holds == null) {
                    //sp was passed in as the dest of a move. This should be a store
                    il.add(new Sw(src1.name, "sp", "0"));
                    src1.dirty = false;
                } else {
                    il.add(new Mv(src1.name, dest.name));
                }
            }
            else if (this.oc == Instruction.OpCode.FMVS) {
                if (this.dest.holds == null) {
                    //sp was passed in as the dest of a move. This should be a store
                    il.add(new Fsw(src1.name, "sp", "0"));
                    src1.dirty = false;
                } else {
                    il.add(new FMv(src1.name, dest.name));
                }
            }
        }
    }
    public void getGlobalAddress(String src, InstructionList il) {
        if (src.contains("$g")) {
            assert (src.length() > 2);
            SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
            String address = ste.addressToString();
            il.add(new La("x3", address));
        }
    }

    public Register ensureSource(String src, Scope.Type type, InstructionList il) {
        //Note: Registers hold values updated here, as if it's sp or ra it will no be given a hold value it will stay null
        //Step 1: Get the register src is in, or if not, gets a free register to put src in
        Register reg = null;
        
        reg = registers.findMappedReg(src,type);
        
        if (reg == null) {
            reg = registers.findReg(type);

            if (src.contains("$g")) {
                //Global var
                assert(src.length() > 2);
                SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
                if (ste != null) {
                    //reg = registers.getx3();
                    String address = ste.addressToString(); 
                    il.add(new La("x3", address));
                    switch(type) {
                        case INT: 
                            il.add(new Lw(reg.name, "x3", "0"));
                        break;
                        case FLOAT: 
                            il.add(new Flw(reg.name, "x3", "0"));
                        break;
                        default:
                            throw new Error("Issue w/ensureSource");
                    }
                    
                    reg.holds = src;
                    //Not dirty, just loaded from memory
                }
            }

            //Get a free register
            
            //Step 2: If src is not in a register, load it into one
            //Find if src is a variable or a temporary or just a number

            
            //If src is a global variable:
            //Generate a la to register x3 from the variables register
            //Then, generate a lw to to the new register from x3, with 0 offset
            //Now, global variable is loaded into the register that will be returned
            
            

            //If src is a temporary, no need to generate anything be loaded into it,
            //just put in register and return register
            else if (src.contains("$t")) {
                //Temporary register
                reg.holds = src;
            }

            //If src is a local variable:
                //Local variables will always have the address after the $l? Guess so.
                //So, if it's a src, need to load from it to use it. Generate a Lw reg offset(fp)
            //Issue: Many may hold $l, but different numbers. Make sure never conflicts?
            else if (src.contains("$l")) {
                assert(src.length() > 2);
                SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
                if (ste == null) {
                    //Should be null for a offset as src
                    String offset = src.substring(2); //For local vars gives offset
                    switch(type) {
                        case INT: 
                            il.add(new Lw(reg.name, "fp", offset));
                        break;
                        case FLOAT: 
                            il.add(new Flw(reg.name, "fp", offset));
                        break;
                        default:
                            throw new Error("Invalid type for load");
                    }
                    
                    reg.holds = src;
                    //Not dirty, just loaded from memory
                }
            }
            //Also: Will I need to handle sp and ra? How could this be done better?
            //Sometimes sp may be in the baseAddress (src). To handle, don't assign a register
            //just perform the operations needed manually using sp
            //Can also have a Lw ra, 0(sp). 
            //Want to handle sp and ra by checking for a $ sign, if so, converting as needed,
            //but they shouldn't use a register...

            //If its an ra, just put the same line of code down.
            //If its an sp, will need to handle proper storing. I think they will only be stores
            else if (src.contains("ra") && src.contains("$") == false) {
                //Actually, should only ever be a store, dest will only ever have an Lw
                il.add(new Sw("ra", "sp", "0"));
            }
            else if (src.contains("sp") && src.contains("$") == false) {
                //Dont think i'll do anything here - may just need dest for Lw ra 0(sp)
            }

        }
        //Uncomment for testing
        //il.add(new Blank("Src Reg: " + reg.name + " holds: " + src));
        return reg;
    }

    public Register ensureDest(String dest, Scope.Type type, InstructionList il) {

        Register reg = null;
        //Find if reg is already in a register
        reg = registers.findMappedReg(dest, type);
        //il.add(new Blank("Reg " + reg.name + " marked as dirty"));
        
        if (reg == null) {
            //Reg is not in a register
            reg = registers.findReg(type);
            
            if (dest.contains("ra") && dest.contains("$") == false) {
                //If ra, dont want to put it into a register
                il.add(new Lw("ra", "sp", "0"));
            }
            else if (dest.contains("sp") && dest.contains("$") == false) {
                //Dont do anything, move generates proper store if reg.holds is left null
            }
            else {
                reg.holds = dest;
                reg.dirty = true;   //Destination of something, so different than value
                //in memory, therefore dirty
                
                
            }
            
            
        }
        //Dont generate a load if dest is a global
        //Dont generate a load if dest is a local variable
        //May also have a move w/sp in the dest. This would generate a store
        //Otherwise, nothing to do with sp or ra in here

        reg.dirty = true; //Even if already in a reg, this is a dest so dirty

        //Uncomment for testing
        //il.add(new Blank("Reg " + reg.name + " marked as dirty"));

        //Uncomment for testing
        //il.add(new Blank("Dest Reg: " + reg.name + " holds: " + dest));
        return reg;
    }

    public class Register {
        Scope.Type type;    //Type of register(int or float)
        String name;        //Name of register(x# for ints, f# for floats)
        String holds;       //Temporary or literal mapped to register
        boolean dirty;      //If register is dirty (holds value different than on stack)
        boolean free;
    }
    
    public class RegisterFile {
        //Array of registers and methods to operate on them


        //Create vectors for registers
        Vector<Register> registerListInt = new Vector<Register>();
        Vector<Register> registerListFloat = new Vector<Register>();
        Vector<Register> intRegsToSave = new Vector<Register>();
        Vector<Register> floatRegsToSave = new Vector<Register>();


        Set<String> excls = Set.of("x0", "x1","x2", "x8"); 
        

        RegisterFile() {
            //Constructor
            //Start at 3 as 0, 1, and 2 int registers unavailable
            //And use 3 only for LA's?
            for (int i = 0; i < numIntRegisters; i++) {
                if (excls.contains("x" + String.valueOf(i))) { continue; } //8 unavailable for ints, fp
                Register reg = new Register();
                reg.type = Scope.Type.INT;
                reg.name = "x" + String.valueOf(i);
                reg.holds = null;   //Start out not holding anything
                reg.free = true;
                reg.dirty = false;  //Begin not dirty
                registerListInt.add(reg);
            }
            for (int i = 0; i < numFloatRegisters; i++) {
                Register reg = new Register();
                reg.type = Scope.Type.FLOAT;
                reg.name = "f" + String.valueOf(i);
                reg.holds = null;
                reg.free = true;
                reg.dirty = false;
                registerListFloat.add(reg);
            }
        }

        public Register findMappedReg(String src, Scope.Type type) {
            //Find a register if it's mapped to a variable/temp/val
            if (src == null) { return null; } //If null, no var mapped to it
            switch (type) {
                case INT: 
                for (int i = 0; i < registerListInt.size(); i++) {

                    //Loop through register list, check each one
                    if (src.equals(registerListInt.get(i).holds)) {
                        //If src is found in a register
                        return registerListInt.get(i);
                    }
                }
                break;
                case FLOAT: 
                for (int i = 0; i < registerListFloat.size(); i++) {
                    //Loop through register list, check each one
                    if (src.equals(registerListFloat.get(i).holds)) {
                        //If src is found in a register
                        return registerListFloat.get(i);
                    }
                }
                break;
                default:
                    throw new Error("Register of invalid type");
            }
            Register reg = null;
            return (reg); //Return a null register to indicate none found

        }
        public Register findReg(Scope.Type type) {
            //Find an available register, free up a reg if not available
            //Should not return excluded registers
            Register reg = null;
            switch(type) {
                case INT: 
                    //Find int reg
                for (int i = 0; i < registerListInt.size(); i++) {
                    if ("x3".equals(registerListInt.get(i).name)) {
                        //Skip x3. only use for LA's
                        continue;
                    }
                    //Loop through all regs, find available reg w/lowest number
                    if (registerListInt.get(i).holds == null) {
                        //If register holds null it is free, return it
                        //Account for now the register is not free
                        return registerListInt.get(i);
                    }
                }
                break;
                case FLOAT: 
                    //Find float reg
                for (int i = 0; i < registerListFloat.size(); i++) {
                    //Loop through all regs, find available reg w/lowest number
                    if (registerListFloat.get(i).holds == null) {
                        //If register holds null it is free, return it
                        //Account for now the register is not free
                        return registerListFloat.get(i);
                    }
                }
                break;
                default:
                    throw new Error("Reg type error");
            }
            if (reg == null) {
                //No regs are available. Need to free one(spill if needed) and return it
                //If i can find a dirty register, spill it. It'll need to be saved anyways
                switch(type) {
                    case INT: 
                        for (int i = 0; i < registerListInt.size(); i++) {
                            if (registerListInt.get(i).dirty) {
                                //If register dirty, spill it
                                generateRegisterSpill(registerListInt.get(i), type);
                                return registerListInt.get(i);
                            }
                        }
                    break;
                    case FLOAT: 
                        for (int i = 0; i < registerListFloat.size(); i++) {
                            if (registerListInt.get(i).dirty) {
                                //If register dirty, spill it
                                generateRegisterSpill(registerListInt.get(i), type);
                                return registerListInt.get(i);
                            }
                        }
                    break; 
                    default:
                        throw new Error("Issue finding dirty reg");
                }
            }
            return reg; 
        }
        public Register getx3() {
            Register reg = null;
            for (int i = 0; i < registerListInt.size(); i++) {
                if("x3".equals(registerListInt.get(i).name)) {
                    return registerListInt.get(i);
                }
            }
            return reg;
        }
        public InstructionList saveRegsStart() {
            InstructionList il = new InstructionList();
            for (int i = 0; i < intRegsToSave.size(); i++) {
                if ("x3".equals(intRegsToSave.get(i).name)) {
                    continue;
                }
                Register currReg = intRegsToSave.get(i);
                il.add(new Sw(currReg.name, "sp", "0"));
                il.add(new Addi("sp", "-4", "sp"));
            }
            for (int i = 0; i < floatRegsToSave.size(); i++) {
                Register currReg = floatRegsToSave.get(i);
                il.add(new Fsw(currReg.name, "sp", "0"));
                il.add(new Addi("sp", "-4", "sp"));
            }
            return il;
        }
        public InstructionList saveRegsEnd() {
            //Save and clear regs
            InstructionList il = new InstructionList();
            il.add(new Label("func_ret_" + currFunc));
            il.add(new Blank("Restore registers"));

            for (int i = floatRegsToSave.size()-1; i >= 0; i--) {
                //Generate store
                //Want Sw reg 0(location)
                
                Register currReg = floatRegsToSave.get(i);
                String name = currReg.holds;
                SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                if (ste != null) {
                    String address = ste.addressToString();
                    //reg.holds = address;
                    il.add(new La("x3", address));
                }
                il.add(new Addi("sp", "4", "sp"));
                il.add(new Flw(currReg.name, "sp", "0"));
                currReg = null;
            }

            for (int i = intRegsToSave.size()-1; i >= 0; i--) {
                    if ("x3".equals(intRegsToSave.get(i).name)) {
                        continue;
                    }
                    //Generate store
                    //Want Sw reg 0(location)
                    
                    Register currReg = intRegsToSave.get(i);
                    String name = currReg.holds;
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                    if (ste != null) {
                        String address = ste.addressToString();
                        //reg.holds = address;
                        il.add(new La("x3", address));
                    }
                    il.add(new Addi("sp", "4", "sp"));
                    il.add(new Lw(currReg.name, "sp", "0"));
                    currReg = null;
            }
            
            return il;
        }

        public InstructionList saveRegsEndBB() {
            Vector<String> savedRegs = new Vector<String>();
            InstructionList il = new InstructionList();

            for (int i = 0; i < registerListInt.size(); i++) {

                if (registerListInt.get(i).dirty) {
                    //If register is dirty, need to save it
                    if ("x3".equals(registerListInt.get(i).name)) {
                        continue; //Don't save x3, it'll always be overwritten and data is temporary
                    }
                    //Want to load data into x3, and then store
                    if (registerListInt.get(i).holds != null) {
                        //If null, no need to store

                        //If it's a temporary, no need to store. 
                        if (registerListInt.get(i).name.contains("x") && registerListInt.get(i).holds.contains("$t") == false) {
                            //If its a int register that does not hold a temporary
                            String name = registerListInt.get(i).holds.substring(2);
                            SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                            if (ste != null) {
                                //If it is found as a variable being held
                                String address = ste.addressToString();
                                il.add(new La("x3", address));
                                il.add(new Sw(registerListInt.get(i).name, "x3", "0"));
                            }
                        }
                        if (registerListInt.get(i).holds.contains("$l")) {
                            //Local variable, store needs to be offset
                            String offset = registerListInt.get(i).holds.substring(2); //Holds offset after $l
                            il.add(new Sw(registerListInt.get(i).name, "fp", offset));
                            
                        }
                    }
                    registerListInt.get(i).dirty = false;
                }
                if (registerListInt.get(i).holds != null) {
                    if (intRegsToSave.contains(registerListInt.get(i)) == false) {
                        intRegsToSave.add(registerListInt.get(i));
                    }
                    
                    registerListInt.get(i).holds = null;
                } 
            }
            for (int i = 0; i < registerListFloat.size(); i++) {

                if (registerListFloat.get(i).dirty) {
                    //If register is dirty, need to save it
                    if ("x3".equals(registerListFloat.get(i).name)) {
                        continue; //Don't save x3, it'll always be overwritten and data is temporary
                    }
                    //Want to load data into x3, and then store
                    if (registerListFloat.get(i).holds != null) {
                        //If null, no need to store

                        //If it's a temporary, no need to store. 
                        if (registerListFloat.get(i).name.contains("f") && registerListFloat.get(i).holds.contains("$t") == false) {
                            //If its a int register that does not hold a temporary
                            String name = registerListFloat.get(i).holds.substring(2);
                            SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                            if (ste != null) {
                                //If it is found as a variable being held
                                String address = ste.addressToString();
                                il.add(new La("x3", address));
                                il.add(new Fsw(registerListFloat.get(i).name, "x3", "0"));
                            }
                        }
                        if (registerListFloat.get(i).holds.contains("$l")) {
                            //Local variable, store needs to be offset
                            String offset = registerListFloat.get(i).holds.substring(2); //Holds offset after $l
                            il.add(new Fsw(registerListFloat.get(i).name, "fp", offset));
                            
                        }
                    }
                    registerListFloat.get(i).dirty = false;
                }
                if (registerListFloat.get(i).holds != null) {
                    if (floatRegsToSave.contains(registerListFloat.get(i)) == false) {
                        floatRegsToSave.add(registerListFloat.get(i));
                    }
                    
                    registerListFloat.get(i).holds = null;
                } 
            }
            return il;
        }

        /*
        public InstructionList saveRegsEndBB() {
            //Save and clear regs
            //SAVE TEMPORARIES AS A NEW LOCAL VAR!
            Vector<String> savedRegs = new Vector<String>();
            InstructionList il = new InstructionList();
            for (int i = 0; i < registerListInt.size(); i++) {
                //il.add(new Blank("Before: " + registerListInt.get(i).name));
                //il.add(new Blank("Current: " + registerListInt.get(i).name + " is " + String.valueOf(registerListInt.get(i).dirty)));
                if (registerListInt.get(i).dirty) {
                    if ("x3".equals(registerListInt.get(i).name)) {
                        continue;
                    }
                    
                    //Generate store
                    //Want Sw reg 0(location)
                    Register currReg = registerListInt.get(i);

                    //il.add(new Blank("Dirty saved: " + registerListInt.get(i).name + " holding: " + currReg.holds));
                    if (savedRegs.contains(currReg.name)) {
                        //il.add(new Blank("Skipped over: " + currReg.name));
                        continue;
                    }
                    if (currReg.holds == null) {
                        //Dont need to save!
                    }
                    else {
                    if ((currReg.name.contains("x")) && (currReg.holds.contains("$t") == false) && (currReg.holds.contains("sp") == false)) {
                        //Not a temporary, don't need to save temps to stack
                        //il.add(new Blank("Saved: " + currReg.name));
                        String name = currReg.holds.substring(2);
                        SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                        if (ste == null) {
                           //throw new Error(currReg.name);
                        } else {
                            String address = ste.addressToString();
                            il.add(new La("x3", address));
                            il.add(new Sw(currReg.name, "x3", "0"));
                            savedRegs.add(currReg.name);
                        }
                        
                    }
                    if (currReg.holds.contains("$l")) {
                        //Local var, generate store w/offset
                        String offset = currReg.holds.substring(2);
                        //il.add(new Blank(currReg.name + " holds: " + currReg.holds));
                        il.add(new Sw(currReg.name, "fp", offset));
                    }
                    if (savedRegs.contains(currReg.name) == false) {
                        intRegsToSave.add(currReg);
                    }
                    registerListInt.get(i).dirty = false;
                    
                    
                }

                    //il.add(new Blank("Wiped: " + currReg.name));
                }
                registerListInt.get(i).holds = null;
                
                //il.add(new Blank(registerListInt.get(i).name + "holds: " + registerListInt.get(i).holds));
            }
            for (int i = 0; i < registerListFloat.size(); i++) {
                if (registerListFloat.get(i).dirty) {
                    //Generate store
                    //Want Sw reg 0(location)
                    Register currReg = registerListFloat.get(i);
                    if (savedRegs.contains(currReg.name)) {
                        continue;
                    }
                    if (currReg.holds == null) {
                        //Dont need to save!
                    } else {
                    if ((currReg.name.contains("f")) && (currReg.holds.contains("$f") == false) && (currReg.holds.contains("sp") == false)) {
                        //Not a temporary, don't need to save temps to stack
                        String name = currReg.holds.substring(2);
                        SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                        if (ste == null) {
                            //throw new Error(currReg.holds);
                        } else {
                        String address = ste.addressToString();
                        il.add(new La("x3", address));
                        il.add(new Fsw(currReg.name, "x3", "0"));
                        savedRegs.add(currReg.name);
                        }
                    }
                    if (savedRegs.contains(currReg.name) == false) {
                        floatRegsToSave.add(currReg);
                    }
                    
                    registerListFloat.get(i).dirty = false;
                }
                }
            }
            return il;
        }
        */
        
        public void generateRegisterSpill(Register reg, Scope.Type typ) {

        }
    
    }
}
