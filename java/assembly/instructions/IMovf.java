package assembly.instructions;

/*
 * Class corresponding to RISC-V IMOVF.S instructoion
 * 
 * Models: IMOVF.S dest src
 */

public class IMovf extends Instruction {
    
    /**
     * Initializes a IMOVF.S instruction that will print: IMOVF.S dest src
     * 
     * @param src source operand 1
     * @param dest destination operand
     */

    public IMovf(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.IMOVFS;
    }

    /**
     * @return "IMOVFS dest src"
     */

    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }
}
