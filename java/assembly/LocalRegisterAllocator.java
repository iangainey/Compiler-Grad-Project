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

/*
 * 
 * Current Issue:
 * sp and ra is getting stored in registers hold section
 * They should not be in the registers, they're hogging them
 * and therefore resulting in more registers being used
 */

public class LocalRegisterAllocator extends CodeGenerator {

    RegisterFile registers = new RegisterFile();

    LocalScope funcScope;
    String currFunc;
    Scope.Type returnType;

    public Vector<InstructionList> generateBasicBlocks(CodeObject body) {

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

    public CodeObject allocate(CodeObject body, FunctionNode node) {

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

    public CodeObject allocateBasicBlock(InstructionList bb) {
        CodeObject co = new CodeObject();
        //For each instruction in basic block:
            //If i is a branch, call expandbranch, and add code
            //else if i is a put, call expand put, and add code
            //Add in labels and blanks at correct spots
        InstructionList il = new InstructionList();

        il.add(new Blank());

        il.add(new Blank("Start of BB"));
        

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

            //Check next statement, if there's a next
            Set<String> branchOps = Set.of("BEQ", "BGE", "BGT", "BLE", "BLT", "BNE");
            

            if (branchOps.contains(oc)) {
                
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
                

                //Delete/Old
                /*
                if ((i == (bb.size()-1)) && String.valueOf(stmt.getOC()).contains("J") == false) {
                    il.add(new Blank("Saving registers at end of BB"));
                    il.addAll(registers.saveRegsEndBB());
                }

                il.addAll(new macroExpandBranch(stmt).toReturn());
                */
            }
            else if (oc == "PUTS" || oc == "PUTI" || oc == "PUTF") {
                //If OC is a put, expanded in same way
                il.addAll(new macroExpandPut(stmt).toReturn());
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
                il.addAll(new macroExpandLi(stmt).toReturn());
            }
            else if (oc == "SW" || oc == "FSW") {
                //Only stores should be RA-no need to expand, just put right on in
                il.add(stmt);
                //il.addAll(macroExpandStore(stmt));
            }
            else if (oc == "MV" || oc == "FMV.S") {
                il.addAll(new macroExpandMove(stmt).toReturn());
            }
            else if (oc == "NEG" || oc == "FNEG") {
                il.addAll(new macroExpandUnary(stmt).toReturn());
            } 
            else if (oc == "LW" || oc == "FLW") {
                il.addAll(new macroExpandLoad(stmt).toReturn());
            }
            else {
                
                if (String.valueOf(stmt.getOC()).contains("J") && String.valueOf(stmt.getOC()).contains("JR") == false) {
                    //If a J is next, save regs
                    String returnReg = il.getLast().getDest();
                    il.add(new Blank("Saving registers at end of BB"));
                    il.addAll(registers.saveRegsEndBB());
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
                    il.add(stmt); 
                }
                else {
                    //if ((i == (bb.size()-1)) && (String.valueOf(stmt.getOC()).contains("J") == false)) {
                        //il.add(new Blank("Saving registers at end of BB"));
                        //il.addAll(registers.saveRegsEndBB());
                    //}
                    il.add(stmt);   
                     
                }
            }
            if ((i == (bb.size()-1) && (oc == "BEQ" || oc == "BGE" || oc == "BGT" || oc == "BLE" || oc == "BLT" || oc == "BNE") == false)) {
                il.add(new Blank("Saving registers at end of BB"));
                il.addAll(registers.saveRegsEndBB());
            }
            
            
            //Testing
            //il.add(bb.nodes.get(i));
        }
        //registers.clearRegs();
        

        il.add(new Blank("End of BB"));

        //il.add(new Blank());

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

        }

        public macroExpand3O(Instruction i) {
            this.oc = i.getOC();
            if(i.toString().contains("F") || i.toString().contains(".S")) {
                this.type = Scope.Type.FLOAT;
            } else {
                this.type = Scope.Type.INT;
            }
            il = new InstructionList();
            if (i.getOperand(Instruction.Operand.SRC1) != null) {
                this.src1 = ensureSource(i.getOperand(Instruction.Operand.SRC1), type, il);
            }
            if (i.getOperand(Instruction.Operand.SRC2) != null) {
                this.src2 = ensureSource(i.getOperand(Instruction.Operand.SRC2), type, il);
            }
            if (i.getOperand(Instruction.Operand.DEST) != null) {
                this.dest = ensureDest(i.getOperand(Instruction.Operand.DEST), type, il);
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
            
        }
    }
    public class macroExpandPut extends macroExpand3O {

        public macroExpandPut(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.PUTS) {
                il.add(new PutS(src1.name));
            }
            else if (this.oc == Instruction.OpCode.PUTI) {
                il.add(new PutI(src1.name));
            }
            else if (this.oc == Instruction.OpCode.PUTF) {
                il.add(new PutF(src1.name));
            }
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
    public class macroExpandLi extends macroExpand3O {

        public macroExpandLi(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.LI) {
                il.add(new Li(dest.name, value)); 
            }
            else if (this.oc == Instruction.OpCode.FIMMS) {
                il.add(new FImm(dest.name, value));
            }
            
        }

    }
    public class macroExpandMove extends macroExpand3O {

        public macroExpandMove(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.MV) {
                if (("sp".equals(dest.holds) || "sp".equals(src1.holds)) || ("ra".equals(dest.holds) || "ra".equals(src1.holds))) {
                    if ("sp".equals(dest.holds)) {
                        il.add(new Sw(src1.name, "sp", "0"));
                    }
                    
                    //il.add(new Blank(dest.holds + " <- DEST  SRC -> " + src1.holds));
                    //dest.holds = null;
                    //src1.holds = null;
                    //il.add(new Blank("Removed holds from" + src1.name + " and " + dest.name));
                } else {
                    il.add(new Mv(src1.name, dest.name));
                }
                
            }
            else if (this.oc == Instruction.OpCode.FMVS) {
                if (("sp".equals(dest.holds) || "sp".equals(src1.holds)) || ("ra".equals(dest.holds) || "ra".equals(src1.holds))) {
                    //il.add(new Fsw(src1.name, "sp", "0"));
                } else {
                    il.add(new FMv(src1.name, dest.name));
                }
            }
        }

    }
    public class macroExpandOp extends macroExpand3O {

        public macroExpandOp(Instruction i) {
            super(i);
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
    public class macroExpandLoad extends macroExpand3O {

        public macroExpandLoad(Instruction i) {
            super(i);
            if (this.oc == Instruction.OpCode.LW) {
                
                if ("ra".equals(src1.holds) == false && "ra".equals(dest.holds) == false) {
                    if ("sp".equals(src1.holds)) {
                        il.add(new Lw(dest.name, "sp", "0"));
                        //il.add(new Blank("Thought so"));
                        dest.holds = i.getDest();
                    } else {
                        il.add(new Lw(dest.name, src1.name, "0"));
                    }
                } 
                if ("ra".equals(src1.holds)) {
                    src1.holds = null;
                    //il.add(new Blank("Src had RA"));
                }
                if ("ra".equals(dest.holds)) {
                    dest.holds = null;
                    //il.add(new Blank("Dest had RA"));
                }
                
                
            } 
            if (this.oc == Instruction.OpCode.FLW) {
                if ("ra".equals(src1.holds) == false && "ra".equals(dest.holds) == false) {
                    if ("sp".equals(src1.holds)) {
                        il.add(new Flw(dest.name, "sp", "0"));
                    } else {
                        il.add(new Flw(dest.name, src1.name, "0"));
                    }
                }
                
            }
        }
    }

    public Register ensureSource(String src, Scope.Type type, InstructionList il) {
        Register reg = null;

        //Find if src is already in a register
        //If it is, return that register
            reg = registers.findMappedReg(src, type);
            if (reg != null) {
                if (reg.name.contains("x5")) {
                    //il.add(new Blank("HEREEE"));
                }
            }
            
            if (reg == null) {
                //If src is not already in a register
                //Find an available register to use
                reg = registers.findReg(type);

                //Depending on instruction, add contents to register or add instructions
            
                //If src contains $g & letters following, it is a global variable
                //Need to genere a la w/x3 as the register to load the address
                if (src.contains("$g") && src.length() > 2) {
                    //Check if letters follow it
                    //Check if index 3 holds a digit. Should be an x, f, t if a register or a var name
                    //Also, check if contents after $l can be found in symbol table. 
                    //If not, it's numbers and an offset
                    //If so, its a local var
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
                    //if ((ste == null) && (Character.isDigit(src.charAt(3)))) {
                    if (ste == null) {
                        //If it does have a digit at index 3, not a var, shouldn't hit this with global?
                    } else {
                        //If there is a variable there
                        String address = ste.addressToString();
                        il.add(new La("x3", address));
                        reg.name = "x3";
                        reg.holds = src;
                        reg.dirty = true;
                    }
                }
                else if (src.contains("$l") && src.length() > 2) {
                    //Check if contents after $l can be found in symbol table. 
                    //If not, it's numbers and an offset
                    //If so, its a local var
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
                    //if ((ste == null) && (Character.isDigit(src.charAt(3)))) {
                    if (ste == null) {
                        //If src contains $l and numbers w/no letters, its a local var
                        //Generate a load to the reg.name w/offset relative to fp
                        String offset = src.substring(2);
                        //il.add(new Blank("Src local offset :" + offset));
                        if (offset.contains("-") == false) {
                            il.add(new Lw(reg.name, "fp", offset));
                            reg.holds = src;
                        } else {
                            reg.holds = src;
                            reg.dirty = true;
                        }
                        
                        
                    } 
                    else {
                        //If src contains $l and letters following, it's a local var
                        //Generate a lw to x3 with address offset from fp
                        String address = ste.addressToString();
                        il.add(new Lw("x3", "fp", address));
                        reg.name = "x3";
                        reg.holds = src;
                        reg.dirty = true;
                    }
                }
                else if (src.contains("sp")) {
                    //If src contains sp, don't generate any instruction in move, instead generate a
                    //store in here as the store is src1 and 0 offset to fp, don't need a dest
                    //ra should be in dest, so handle the lw in there in a similar manner to this
                    il.add(new Sw(reg.name, "sp", "0"));
                    reg.holds = src;
                    reg.dirty = true;
                    //throw new Error("Ok");
                }
                else if (src.contains("ra")) {
                    il.add(new Lw(src, "sp", "0"));
                    reg.holds = src;
                    //reg.dirty = true;
                }
            
            }
            //il.add(new Blank("Src: " + "Reg Name: " + reg.name + " Holds: " + reg.holds));

        return reg;
    }

    public Register ensureDest(String dest, Scope.Type type, InstructionList il) {
        Register reg = null;

        //Find if dest is already in a register
        //If it is, return that register
        reg = registers.findMappedReg(dest, type);

        if (reg == null) {
            //If dest is not already in a register
            //Find an available register to use
            reg = registers.findReg(type);

            //Depending on instruction, add contents to register or add instructions
        
            //If dest contains $g & letters following, it is a global variable
            //Need to genere a la w/x3 as the register to load the address
            if (dest.contains("$g") && dest.length() > 2) {
                //Check if letters follow it
                //Check if index 3 holds a digit. Should be an x, f, t if a register or a var name
                //Also, check if contents after $l can be found in symbol table. 
                //If not, it's numbers and an offset
                //If so, its a local var
                SymbolTableEntry ste = funcScope.getSymbolTableEntry(dest.substring(2));
                //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
                if (ste == null) {
                    //il.add(new Blank("Well why"));
                } else {
                    //If there is a variable there
                    String address = ste.addressToString();
                    //il.add(new La("x3", address));
                    //reg.name = "x3";
                    reg.holds = dest;
                    reg.dirty = true;
                }
            }
            else if (dest.contains("$l") && dest.length() > 2) {
                //Check if contents after $l can be found in symbol table. 
                //If not, it's numbers and an offset
                //If so, its a local var
                SymbolTableEntry ste = funcScope.getSymbolTableEntry(dest.substring(2));
                //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
                if (ste == null) {
                    //If src contains $l and numbers w/no letters, its a local var
                    //Generate a load to the reg.name w/offset relative to fp
                    String offset = dest.substring(2);
                    if (offset.contains("-")) {
                        //reg.holds = null;
                    } else {
                        il.add(new Lw(reg.name, "fp", offset));
                        reg.holds = dest;
                        reg.dirty = true;
                    }
                    
                } 
                else {
                    //If src contains $l and letters following, it's a local var
                    //Generate a lw to x3 with address offset from fp
                    String address = ste.addressToString();
                    il.add(new Lw("x3", "fp", address));
                    reg.name = "x3";
                    reg.holds = dest;
                    reg.dirty = true;
                }
            }
            else if (dest.contains("sp")) {
                //If src contains sp, don't generate any instruction in move, instead generate a
                //store in here as the store is src1 and 0 offset to fp, don't need a dest
                //ra should be in dest, so handle the lw in there in a similar manner to this
                il.add(new Sw(reg.name, "sp", "0"));
                reg.holds = dest;
                //reg.dirty = true;
                //throw new Error("Ok");
            }
            else if (dest.contains("ra")) {
                il.add(new Lw(dest, "sp", "0"));
                reg.holds = dest;
                //reg.dirty = true;
            }
            else {
                reg.holds = dest;
                reg.dirty = true;
            }
        
        }
        
        //il.add(new Blank("Dest: " + "Reg Name: " + reg.name + " Holds: " + reg.holds));
        return reg;
    }
    
     


    public class Register {
        Scope.Type type;    //Type of register(int or float)
        String name;        //Name of register(x# for ints, f# for floats)
        String holds;       //Temporary or literal mapped to register
        boolean dirty;      //If register is dirty (holds value different than on stack)
        boolean free;
    }
    //ENSURE REG UPDATES HOLDS SO IT IS NOT NULL IF IT HOLDS
    public class RegisterFile {
        //Array of registers and methods to operate on them

        //Create arrays for registers
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
        /* 
        public InstructionList saveRegsStart() {
            //Creates a list of instructions for saving regs at the beginning of a function
            //Collections.sort(intRegsToSave);
            InstructionList saveStart = new InstructionList();
            //il.add(new Blank("Regs to save: "));
            for (int i = 0; i < registerListInt.size(); i++) {
                if (intRegsToSave.contains(registerListInt.get(i).name)) {
                    //If register needs to be saved, save
                    //intRegsToSave.remove(registerListInt.get(i).name);
                    saveStart.add(new Sw(registerListInt.get(i).name, "sp", "0"));
                    saveStart.add(new Addi("sp", "-4", "sp"));
                }
            }
    
            return saveStart;
        
        }
    
        public InstructionList saveRegsEnd() {
            InstructionList saveEnd = new InstructionList();
            //Creates a list of instructions for saving regs at the end of a function
            for (int i = registerListInt.size()-1; i >= 0; i--) {
                //Check if needs to be saved
                if (intRegsToSave.contains(registerListInt.get(i).name)) {
                    //Check if need to load from memory
                    String var = registerListInt.get(i).holds;
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(var);
                    if (ste != null) {
                        //Var does need to be loaded
                        String address = ste.addressToString();
                        saveEnd.add(new La("x3", address));
                    }
                    //Store
                    saveEnd.add(new Addi("sp", "4", "sp"));
                    saveEnd.add(new Sw(registerListInt.get(i).name, "sp", "0"));
                }
                
            }
            return saveEnd;
        }
    
        public InstructionList saveRegsBB() {
            //Creates a list of instructions for saving regs at the end of a basic block.
            //As a result, handles clearing regs after they've been saved
            InstructionList saveBB = new InstructionList();
            //Find any register that is dirty, and then save the values to memory
            //Different kind of stores:
                //Do not save temporaries. Skip them
                //If it's a local variable, need to store w/offset
                //If its a global, load the globsl address into x3 and generate a store int x3 w/no offset
                //Should not have to handle sp or ra, if so throw them out
            
                for (int i = 0; i < registerListInt.size(); i++) {
                    if (registerListInt.get(i).dirty) {
                        //Register is dirty, check if temp and if not store
    
                        if ("x3".equals(registerListInt.get(i).name)) {
                            continue;   //Skip x3, reserved
                        }
    
                        if (registerListInt.get(i).holds.contains("$t")) {
                            //Temporary, do nothing
                        }
                        else if (registerListInt.get(i).name.contains("x")) {
                            //Integer register
                            String name = null;
                            if (registerListInt.get(i).holds.length() > 2) {
                                //Get STE
                                name = registerListInt.get(i).holds.substring(2);
                                SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                                //If STE is null, not a variable
                                if (ste == null) {
                                    if (registerListInt.get(i).holds.contains("$l")) {
                                        //Holds an offset. Hopefully doesn't put this in hold.. we'll see
                                    }
    
                                } else {
                                    //If STE is found:
                                    //Local var or global var
                                    String address = ste.addressToString();
    
                                    if (registerListInt.get(i).holds.contains("$l")) {
                                        //Local var, address is offset from fp
                                        saveBB.add(new Sw(registerListInt.get(i).name, "fp", address));
                                    } else {
                                        //Global var
                                        saveBB.add(new La("x3", address));
                                        saveBB.add(new Sw(registerListInt.get(i).name, "x3", "0"));
                                    }
                                }
                            }
                        }
                        if (registerListInt.get(i).name.length() > 1 && registerListInt.get(i).name.contains("x")) {
                            //If register has a name of x#, save the number so it can be saved at end of function
                            
                            intRegsToSave.add(registerListInt.get(i).name);
                        }
                    }
                    //Clear regs now that any that were dirty are saved
                    clearRegs();
                }
                return saveBB;
        }
        */
        //Currently all saving only has ints - ADD FLOATS
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
                    il.add(new Sw(currReg.name, "sp", "0"));
                    currReg = null;
            }
            for (int i = floatRegsToSave.size()-1; i > 0; i--) {
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
                    il.add(new Fsw(currReg.name, "sp", "0"));
                    currReg = null;
            }
            return il;
        }
        public InstructionList saveRegsEndBB() {
            //Save and clear regs
            //SAVE TEMPORARIES AS A NEW LOCAL VAR!
            Vector<String> savedVars = new Vector<String>();
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
                    if (savedVars.contains(currReg.holds)) {
                        //il.add(new Blank("Skipped over: " + currReg.name));
                        continue;
                    }
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
                            savedVars.add(currReg.holds);
                        }
                        
                    }
                    if (currReg.holds.contains("$l")) {
                        //Local var, generate store w/offset
                        String offset = currReg.holds.substring(2);
                        //il.add(new Blank(currReg.name + " holds: " + currReg.holds));
                        il.add(new Sw(currReg.name, "fp", offset));
                    }
                    intRegsToSave.add(currReg);
                    registerListInt.get(i).dirty = false;
                    
                    //il.add(new Blank("Wiped: " + currReg.name));
                }
                //il.add(new Blank(registerListInt.get(i).name + "holds: " + registerListInt.get(i).holds));
            }
            for (int i = 0; i < registerListFloat.size(); i++) {
                if (registerListFloat.get(i).dirty) {
                    //Generate store
                    //Want Sw reg 0(location)
                    Register currReg = registerListFloat.get(i);
                    if (savedVars.contains(currReg.holds)) {
                        continue;
                    }
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
                        savedVars.add(currReg.holds);
                        }
                    }
                    floatRegsToSave.add(currReg);
                    registerListFloat.get(i).dirty = false;
                    
                }
            }
            return il;
        }
        
        public void generateRegisterSpill(Register reg, Scope.Type typ) {

        }

        public void clearRegs() {
            
            for (int i = 0; i < registerListInt.size(); i++) {
                registerListInt.get(i).dirty = false;
                registerListInt.get(i).holds = null;
            }
        }

    }
    /*
     * What is my issue?
     * Currently, correcting loading and storing values relative to fp are wrong
     * What I want:
     * To redo ensureSource and ensureDest, as they should handle all the necessary loading
     * and storing
     * Cases:
     * Move instructions currently may be stores or moves. Maybe just turn into stores?
     * But my issue is I can't do everything in the ensures. cant generate the full instruction
     * What I can generate: If src or dest contains a local variable, load
     * But, soemtimes it contains the offset of the local var from the fp. If this is
     * this case, generate a load from there or a store to there
     * 
     * */
    /* 
    
    


    ATTEMPT
    public Register ensureSource(String src, Scope.Type type, InstructionList il) {
        Register reg = null;
    
        reg = registers.mapRegister(src, type);
    
        if (reg == null) {
            //Then dest is either sp or ra
        }
    
        if (src.contains("$g") && src.length() >2) {
            //Check if letters follow it
            //Check if index 3 holds a digit. Should be an x, f, t if a register or a var name
            //Also, check if contents after $l can be found in symbol table. 
            //If not, it's numbers and an offset
            //If so, its a local var
            SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
            //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
            if (ste == null) {
                //il.add(new Blank("Well why"));
            } else {
                //If there is a variable there
                String address = ste.addressToString();
                il.add(new La("x3", address));
                reg.name = "x3";
                //reg.holds = src;
                reg.dirty = true; //Destination of something, so dirty
            }
        }
        else if ((src.contains("$l") && src.length() > 2)) {
            //Check if contents after $l can be found in symbol table. 
            //If not, it's numbers and an offset
            //If so, its a local var
            SymbolTableEntry ste = funcScope.getSymbolTableEntry(src.substring(2));
            //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
            if (ste == null) {
                //If src contains $l and numbers w/no letters, its a local var
                //Generate a load to the reg.name w/offset relative to fp
                String offset = src.substring(2);
                if (offset.contains("-")) {
                    //reg.holds = null;
                } else {
                    il.add(new Lw(reg.name, "fp", offset));
                    //Loading from memory, so not dirty
                }
                        
            } 
            else {
                //If src contains $l and letters following, it's a local var
                //Generate a lw to x3 with address offset from fp
                String address = ste.addressToString();
                il.add(new Lw("x3", "fp", address));
                reg.name = "x3";
            }
        }
        else if (src.contains("sp")) {
            //If src contains sp, don't generate any instruction in move, instead generate a
            //store in here as the store is src1 and 0 offset to fp, don't need a dest
            //ra should be in dest, so handle the lw in there in a similar manner to this
            il.add(new Sw(reg.name, "sp", "0"));
        }
        else if (src.contains("ra")) {
            il.add(new Lw(src, "sp", "0"));
            //reg.holds = dest;
            //reg.dirty = true;
        }
    
        return reg;
    }
    
    public Register ensureDest(String dest, Scope.Type type, InstructionList il) {
        Register reg = null;
    
        reg = registers.mapRegister(dest, type);
    
        if (reg == null) {
            //Then dest is either sp or ra
        }
    
        if (dest.contains("$g") && dest.length() >2) {
            //Check if letters follow it
            //Check if index 3 holds a digit. Should be an x, f, t if a register or a var name
            //Also, check if contents after $l can be found in symbol table. 
            //If not, it's numbers and an offset
            //If so, its a local var
            SymbolTableEntry ste = funcScope.getSymbolTableEntry(dest.substring(2));
            //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
            if (ste == null) {
                //il.add(new Blank("Well why"));
            } else {
                //If there is a variable there
                String address = ste.addressToString();
                //il.add(new La("x3", address));
                //reg.name = "x3";
                reg.holds = dest;
                reg.dirty = true; //Destination of something, so dirty
            }
        }
        else if ((dest.contains("$l") && dest.length() > 2)) {
            //Check if contents after $l can be found in symbol table. 
            //If not, it's numbers and an offset
            //If so, its a local var
            SymbolTableEntry ste = funcScope.getSymbolTableEntry(dest.substring(2));
            //if ((ste == null) && (Character.isDigit(dest.charAt(3)))) {
            if (ste == null) {
                //If src contains $l and numbers w/no letters, its a local var
                //Generate a load to the reg.name w/offset relative to fp
                String offset = dest.substring(2);
                if (offset.contains("-")) {
                    //reg.holds = null;
                } else {
                    il.add(new Lw(reg.name, "fp", offset));
                    //Loading from memory, so not dirty
                }
                        
            } 
            else {
                //If src contains $l and letters following, it's a local var
                //Generate a lw to x3 with address offset from fp
                String address = ste.addressToString();
                il.add(new Lw("x3", "fp", address));
                reg.name = "x3";
            }
        }
        else if (dest.contains("sp")) {
            //If src contains sp, don't generate any instruction in move, instead generate a
            //store in here as the store is src1 and 0 offset to fp, don't need a dest
            //ra should be in dest, so handle the lw in there in a similar manner to this
            il.add(new Sw(reg.name, "sp", "0"));
            reg.holds = dest;
        }
        else if (dest.contains("ra")) {
            il.add(new Lw(dest, "sp", "0"));
            //reg.holds = dest;
            //reg.dirty = true;
        }
    
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
    
        Vector<Register> registerListInt = new Vector<Register>();
        Vector<Register> registerListFloat = new Vector<Register>();
    
        Set<String> intRegsToSave = Set.of("");
        
    
        Set<String> excls = Set.of("x0", "x1","x2", "x8"); 
    
    
        RegisterFile() {
            //Constructor
            //Initialize register vectors and values
            for (int i = 0; i < numIntRegisters; i++) {
                if (excls.contains("x" + String.valueOf(i))) { continue; } //Skips exclusions
                Register reg = new Register();
                reg.type = Scope.Type.INT;
                reg.name = "x" + String.valueOf(i);
                reg.holds = null;
                reg.dirty = false;
                reg.free = true;
                registerListInt.add(reg);
            }
            for (int i = 0; i < numFloatRegisters; i++) {
                if (excls.contains("f" + String.valueOf(i))) { continue; } //Skips exclusions
                Register reg = new Register();
                reg.type = Scope.Type.FLOAT;
                reg.name = "f" + String.valueOf(i);
                reg.holds = null;
                reg.dirty = false;
                reg.free = true;
                registerListFloat.add(reg);
            }
        }
    
        public Register findMappedReg(String src, Scope.Type type) {
            //Find if src(variable or value) is already in a register. If so, return that register
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
            //Find a register that is available. 
            //If none can be found, free one up (generateSpill)
            Register reg = null;
    
            switch(type) {
                case INT: 
                    for (int i = 0; i < registerListInt.size(); i++) {
                        if ("x3".equals(registerListInt.get(i).name)) {
                            //Skip x3. only use for LA's
                            continue;
                        }
                        //Loop through regs and find one thats available
                        if (registerListInt.get(i).free = true) {
                            registerListInt.get(i).free = false;
                            return registerListInt.get(i);
                        }
                    }
                break;
                case FLOAT: 
                for (int i = 0; i < registerListFloat.size(); i++) {
                    //Loop through regs and find one thats available
                    if (registerListFloat.get(i).free = true) {
                        registerListFloat.get(i).free = false;
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
                                generateRegisterSpill(registerListInt.get(i));
                                return registerListInt.get(i);
                            }
                        }
                    break;
                    case FLOAT: 
                        for (int i = 0; i < registerListFloat.size(); i++) {
                            if (registerListInt.get(i).dirty) {
                                //If register dirty, spill it
                                generateRegisterSpill(registerListInt.get(i));
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
    
        public Register mapRegister(String var, Scope.Type type) {
            //Called whenever a new register is wanted
            //If val in register, returns that register. If not,
            //Finds a register and handles mapping the var to it
            Register reg = null;
    
            if (var == "sp" || var == "ra") {
                return reg;
            }
    
            reg = registers.findMappedReg(var, type);
            if (reg == null) {
                //Var is not in a register
                reg = registers.findReg(type);
            }
            reg.holds = var;
    
            return reg;
    
        }
    
        public void unMapRegister(String var, Scope.Type type) {
            //Unmap the var from the register, making the register now free
            Register reg = null;
    
            reg = registers.findMappedReg(var, type);
            if (reg == null) {
                throw new Error("Called unMapRegister in error");
            }
            reg.holds = null;
            reg.dirty = false;
            reg.free = true;
        }
    
        public void clearRegs() {
            //Clears registers at end of BB so they are all available in next one
            for (int i = 0; i < registerListInt.size(); i++) {
                Register reg = registerListInt.get(i);
                reg.holds = null;
                reg.dirty = false;
                reg.free = true;
            }
    
            for (int i = 0; i < registerListFloat.size(); i++) {
                Register reg = registerListFloat.get(i);
                reg.holds = null;
                reg.dirty = false;
                reg.free = true;
            }
        }
    
        public InstructionList saveRegsStart() {
            //Creates a list of instructions for saving regs at the beginning of a function
            //Collections.sort(intRegsToSave);
            InstructionList saveStart = new InstructionList();
            //il.add(new Blank("Regs to save: "));
            for (int i = 0; i < registerListInt.size(); i++) {
                if (intRegsToSave.contains(registerListInt.get(i).name)) {
                    //If register needs to be saved, save
                    //intRegsToSave.remove(registerListInt.get(i).name);
                    saveStart.add(new Sw(registerListInt.get(i).name, "sp", "0"));
                    saveStart.add(new Addi("sp", "-4", "sp"));
                }
            }
    
            return saveStart;
        
        }
    
        public InstructionList saveRegsEnd() {
            InstructionList saveEnd = new InstructionList();
            //Creates a list of instructions for saving regs at the end of a function
            for (int i = registerListInt.size()-1; i >= 0; i--) {
                //Check if needs to be saved
                if (intRegsToSave.contains(registerListInt.get(i).name)) {
                    //Check if need to load from memory
                    String var = registerListInt.get(i).holds;
                    SymbolTableEntry ste = funcScope.getSymbolTableEntry(var);
                    if (ste != null) {
                        //Var does need to be loaded
                        String address = ste.addressToString();
                        saveEnd.add(new La("x3", address));
                    }
                    //Store
                    saveEnd.add(new Addi("sp", "4", "sp"));
                    saveEnd.add(new Sw(registerListInt.get(i).name, "sp", "0"));
                }
                
            }
            return saveEnd;
        }
    
        public InstructionList saveRegsBB() {
            //Creates a list of instructions for saving regs at the end of a basic block.
            //As a result, handles clearing regs after they've been saved
            InstructionList saveBB = new InstructionList();
            //Find any register that is dirty, and then save the values to memory
            //Different kind of stores:
                //Do not save temporaries. Skip them
                //If it's a local variable, need to store w/offset
                //If its a global, load the globsl address into x3 and generate a store int x3 w/no offset
                //Should not have to handle sp or ra, if so throw them out
            
                for (int i = 0; i < registerListInt.size(); i++) {
                    if (registerListInt.get(i).dirty) {
                        //Register is dirty, check if temp and if not store
    
                        if ("x3".equals(registerListInt.get(i).name)) {
                            continue;   //Skip x3, reserved
                        }
    
                        if (registerListInt.get(i).holds.contains("$t")) {
                            //Temporary, do nothing
                        }
                        else if (registerListInt.get(i).name.contains("x")) {
                            //Integer register
                            String name = null;
                            if (registerListInt.get(i).holds.length() > 2) {
                                //Get STE
                                name = registerListInt.get(i).holds.substring(2);
                                SymbolTableEntry ste = funcScope.getSymbolTableEntry(name);
                                //If STE is null, not a variable
                                if (ste == null) {
                                    if (registerListInt.get(i).holds.contains("$l")) {
                                        //Holds an offset. Hopefully doesn't put this in hold.. we'll see
                                    }
    
                                } else {
                                    //If STE is found:
                                    //Local var or global var
                                    String address = ste.addressToString();
    
                                    if (registerListInt.get(i).holds.contains("$l")) {
                                        //Local var, address is offset from fp
                                        saveBB.add(new Sw(registerListInt.get(i).name, "fp", address));
                                    } else {
                                        //Global var
                                        saveBB.add(new La("x3", address));
                                        saveBB.add(new Sw(registerListInt.get(i).name, "x3", "0"));
                                    }
                                }
                            }
                        }
                        if (registerListInt.get(i).name.length() > 1 && registerListInt.get(i).name.contains("x")) {
                            //If register has a name of x#, save the number so it can be saved at end of function
                            
                            intRegsToSave.add(registerListInt.get(i).name);
                        }
                    }
                    //Clear regs now that any that were dirty are saved
                    clearRegs();
                }
                return saveBB;
        }
    
        public void generateRegisterSpill(Register reg) {
            
        }
    */

}
