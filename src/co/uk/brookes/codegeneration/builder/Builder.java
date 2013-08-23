package co.uk.brookes.codegeneration.builder;

import co.uk.brookes.Parser;
import co.uk.brookes.Token;
import co.uk.brookes.codegeneration.Operand;
import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.Struct;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Author: Fantou Thomas
 * Date: 7/2/13
 */
public class Builder {
    static ArrayList<Instruction> constants;
    static ArrayList<Instruction> init;
    static ArrayList<Instruction> rooting;

    static XMLManager xml;

    public static int step;
    public final static int //steps used for the builder to set the instructions to their right place (e.g. var from envdec or state are threated differently)
        envdec_ = 0,
        statedec_ = 1,
        actiondec_ = 2,
        init_ = 3,
        rooting_ = 4,
        castereview_ = 5;

    static int idx;

    static int //byte code pc/constant address (same than the one generated in the CAVM)
        curr_init = 0,
        curr_rooting = 0,
        curr_cons = 0;

    public static int statementType;    //define what is the current statement type to set the right instructions
    public final static int
        none = 0,
        if_statement = 1,
        while_statement = 2,
        repeat_statement = 3;

    public static ArrayList<Integer> JumpsIn; //this is a temporary list in which we add address of jumps that will jump into the statement during a fixup, instead of jumping outside
    static int startAddressOfStatement;


    public static void init() {
        constants = new ArrayList<Instruction>();
        init = new ArrayList<Instruction>();
        rooting = new ArrayList<Instruction>();

        xml = new XMLManager();
        idx = 0;

        step = envdec_;
        statementType = none;

        JumpsIn = new ArrayList<Integer>();
        startAddressOfStatement = 0;

        //default nodes
        addLabel("localhost");
        addLabel("all");
    }

    //notify the begining of a statement, to store the address of its first instruction
    public static void statementStart() {
        switch(step) {
            case init_: startAddressOfStatement = curr_init; break;
            case rooting_: startAddressOfStatement = curr_rooting; break;
        }
    }

    public static int getCurrentAddress() {
        switch(step) {
            case init_: return curr_init;
            case rooting_: return curr_rooting;
        }
        return -1;
    }


    //----------------- XML : Constant_section --------------

    public static void add(Obj o) {     //for generating the XML, we use Obj, to add them in the Constant_section
        switch(step) {
            case envdec_:
                if(o.kind == Obj.environment_) {
                    addLabel(o.name);
                    addEnv(o);
                }
                break;
            case statedec_: //TODO list/record/enum ?
                if(o.kind == Obj.state_) {
                    addLabel(o.name);
                    addState(o);
                }
                break;
            case actiondec_:
                if(o.kind == Obj.action_) {
                    addLabel(o.name);
                    addAction(o);
                }
                break;
            case castereview_:
                if(o.type.kind == Struct.caste_) { //add final caste instruction
                    addCaste(o);
                }
                break;
        }
    }

    //add const (e.g. string val)
    public static void addCons(String con) {
        addLabel(con);
        Instruction ins = new Instruction();
        ins.kind = Instruction.constant_;
        ins.code = Instruction.cons_;
        ins.name = getIndex(con);
        ins.val = con;
        ins.address = curr_cons++;
        constants.add(ins);
    }

    static void addLabel(String val) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.label_;
        ins.val = val;
        constants.add(ins);
    }

    static void addState(Obj o) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.state_;
        ins.name = getIndex(o.name);
        ins.typeCode = o.type.kind;
        ins.address = o.adr;
        constants.add(ins);
    }

    static void addAction(Obj o) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.action_;
        ins.name = getIndex(o.name);
        ins.nParam = o.nPars;
        ins.params = o.locals;
        ins.address = o.adr;
        constants.add(ins);
    }

    static void addEnv(Obj o) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.env_;
        ins.name = getIndex(o.name);
        ins.casteName = getIndex(o.type.name);
        ins.address = o.adr;

        //TODO: how to fill these fields ?
        ins.url = getIndex("localhost");
        ins.type = "0";
        ins.scope = getIndex("all");
        ins.typeCode = Struct.integer_;

        constants.add(ins);
    }


    static void addCaste(Obj o) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.caste_;
        ins.name = getIndex(o.name);

        //init and set the tab values
        String sStates = "";
        String sActions = "";
        String sEnvs = "";
        String sCons = "";
        String sepChar = ";"; //used for the split
        for(Instruction i : constants)
            switch(i.code) {
                case Instruction.state_: sStates += i.index + sepChar; break;
                case Instruction.action_: sActions += i.index + sepChar; break;
                case Instruction.env_ : sEnvs += i.index + sepChar; break;
                case Instruction.cons_ : sCons += i.name + sepChar; break;
            }
        if(sStates != "") ins.states = sStates.split(sepChar); else ins.states = new String[0];
        if(sActions != "") ins.actions = sActions.split(sepChar); else ins.actions = new String[0];
        if(sEnvs != "") ins.envs = sEnvs.split(sepChar); else ins.envs = new String[0];
        if(sCons != "") ins.cons = sCons.split(sepChar); else ins.cons = new String[0];

        //TODO: how to fill these fields ?
        ins.url = getIndex("localhost");

        constants.add(ins);
    }

    //add a simple label with a name
    static public void add(String str) {
        addLabel(str);
    }

    static Instruction getInstruction(String name) {
        String nameIdx = getIndex(name);
        Instruction instruction = null;
        for(Instruction ins : constants)
            if(ins.name != null)
                if(ins.name.equals(nameIdx))
                    instruction = ins;
        return instruction;
    }

    static String getIndex(String name){
        int idx = -1;
        for(Instruction ins : constants)
            if(ins.code == Instruction.label_)
                if(ins.val.equals(name))
                    idx = ins.index;

        return String.valueOf(idx);
    }

    //----------------- instruction generation --------------

    //use operand to chose the right set of instructions
    public static void load(Operand op) {
        switch(op.kind){
            case Operand.con_:
                switch(op.type.kind) {
                    case Struct.integer_: put(Instruction.push_ + Instruction.typeint_); break;
                    case Struct.real_: put(Instruction.push_ + Instruction.typereal_); break;
                }
                put(op.val);
            break;
            case Operand.state_:
                put(Instruction.pushstate_);
                put(String.valueOf(op.adr));
            break;

        }
    }

    //insert something that is not an operand (e.g string value)
    public static void loadIns(String val) {
        Instruction ins = getInstruction(val);
        switch(ins.code) {
            case Instruction.cons_:
                put(Instruction.pushconstantpoolasstring_);
                put(String.valueOf(ins.address)); //address of the constant
                break;
        }
    }

    //insert the observe instruction while declaring environment
    public static void observe(Operand op) {
        if(op.kind == Operand.environment_) {
            put(Instruction.observe_);
            put(String.valueOf(op.adr));
            put(Instruction.sendmessage_);
        }
    }

    //assigning a value to a variable
    public static void assign(Operand op) {
        switch(op.kind){
            case Operand.state_:
                put(Instruction.setstate_);
                put(String.valueOf(op.adr));
                put(Instruction.upstate_);
                put(String.valueOf(op.adr));
                put(Instruction.sendmessage_);
                break;
        }
    }

    public static void endInit() {
        put(Instruction.return_);
    }
    public static void endRooting() {
        put(Instruction.return_);
        put(Instruction.quit_);
    }

    //called in Conditions(), can be called in a if, while and repeat statement.
    //return the address of the instruction (for a jump forward, we need to fixup the jump address later)
    public static int condition(int[] inf) {    //inf[0] = type, inf[1] = operator, inf[2] = token
        switch(inf[0]){
            case Struct.integer_: put(Instruction.sub_ + Instruction.typeint_); break;
            case Struct.real_: put(Instruction.sub_ + Instruction.typereal_); break;
            case Struct.string_: put(Instruction.comparestring_); break;
            //case Struct.char_: put(Instruction.comparestring_); break;
        }
        switch(statementType) {
            case if_statement:
            case while_statement:
                switch(inf[1]){
                    case Token.eqlSign: if(inf[2] == Token.or_) return put(Instruction.ifeq_); else return put(Instruction.ifne_);
                    case Token.neq: if(inf[2] == Token.or_) return put(Instruction.ifne_); else return put(Instruction.ifeq_);
                    case Token.gtr: if(inf[2] == Token.or_) return put(Instruction.iflt_); else Parser.SemErr("Operator '>' cannot be used, see Builder.condition() explanation"); break; //TODO: implement IFGE in the VM
                    case Token.geq: if(inf[2] == Token.or_) return put(Instruction.ifle_); else  return put(Instruction.ifgt_);
                    case Token.lss: if(inf[2] == Token.or_) return put(Instruction.ifgt_); else return put(Instruction.ifle_);
                    case Token.leq: /*if(inf[2] == Token.or_) return put(Instruction.ifge_); else*/ return put(Instruction.iflt_); //TODO: implement IFGE in the VM
                }
            break;
            case repeat_statement:
            break;
        }
        return 0;
    }

    //when a jump forward is required, the fixup will fixup the previous jump instruction to set the jump address
    public static void fixup(ArrayList<Integer> addresses) {
        switch(step) {
            case init_: for(Integer address : addresses) init.get(address).fixup(getFixupJumpAddress(address)); break;
            case rooting_: for(Integer address : addresses) rooting.get(address).fixup(getFixupJumpAddress(address)); break;
        }
    }
    public static void fixup(int address) {
        switch(step) {
            case init_: init.get(address).fixup(getFixupJumpAddress(address)); break;
            case rooting_:  rooting.get(address).fixup(getFixupJumpAddress(address)); break;
        }
    }

    private static int getFixupJumpAddress(int srcAddress) {
        int addressToJump = 0;
        if(JumpsIn.contains(srcAddress)) {
            addressToJump = startAddressOfStatement;
            JumpsIn.remove(JumpsIn.indexOf(srcAddress));
        }
        else {
            switch(step) {
                case init_: addressToJump = curr_init; break;
                case rooting_: addressToJump = curr_rooting; break;
            }
        }
        return addressToJump;
    }

    static public int put(String code) {
        Instruction ins = new Instruction(code);
        switch(step) {
            case envdec_: ins.address = curr_init++; init.add(ins); break; //environment observed are instruction in the init block
            case init_: ins.address = curr_init++; init.add(ins); break;
            case rooting_: ins.address = curr_rooting++; rooting.add(ins); break;
        }
        return ins.address;
    }


    //-------------------------------


    // Write the code buffer to the output stream
    public static void write() {
        xml.generateFile(constants, init, rooting);
    }

}
