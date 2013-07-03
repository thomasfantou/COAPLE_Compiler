package co.uk.brookes.codegeneration;

import co.uk.brookes.Parser;

/**
 * Author: Fantou Thomas
 * Date: 7/1/13
 */
public class Code {



    private static final int bufSize = 8192; //TODO dynamic buffer size ?

    private static byte[] buf;	// code buffer
    public static int pc;		// next free byte in code buffer


    //--------------- code buffer access ----------------------

    //put instructions
    public static void put(int x) {
        if (pc >= bufSize) {
            if (pc == bufSize) Parser.SemErr("program too large");
            pc++;
        } else
            buf[pc++] = (byte)x;
    }



    //----------------- instruction generation --------------

    // Load the operand x to the expression stack
    public static void load(Operand x){
        switch(x.kind){

//            case Operand.con_:
//                if(0 <= x.val && x.val <= 5)put(const0+x.val);
//                else if(x.val == -1)put(const_m1);
//                else{put(const_);put4(x.val);}
//                break;

//            case Operand.state_:
//                put(getstatic);
//                put2(x.adr);
//                break;
        }
        x.kind = Operand.stack_;
    }
}
