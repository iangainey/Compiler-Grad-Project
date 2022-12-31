package assembly.instructions;

/*
 * Not instructor supplied
 * Class corresponding to RISC-V IMOVF.S instructoion
 * 
 * Models: FMOVI.S dest src
 */

public class FMovi extends Instruction {
    
    /**
     * Initializes a FMOVI.S instruction that will print: FMOVI.S dest src
     * 
     * @param src source operand 1
     * @param dest destination operand
     */

    public FMovi(String src, String dest) {
        super();
        this.src1 = src;
        this.dest = dest;
        this.oc = OpCode.FMOVIS;
    }

    /**
     * @return "FMOVIS dest src"
     */

    public String toString() {
        return this.oc + " " + this.dest + ", " + this.src1;
    }
}
