package co.uk.brookes.codegeneration.builder;

import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.Struct;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Author: Fantou Thomas
 * Date: 7/2/13
 */
public class Builder {
    static ArrayList<Instruction> constants;
    static ArrayList<Instruction> init;
    static ArrayList<Instruction> rooting;
    static boolean isInitOver = false;
    static XMLManager xml;

    static int idx;

    public static void init() {
        constants = new ArrayList<Instruction>();
        init = new ArrayList<Instruction>();
        rooting = new ArrayList<Instruction>();

        xml = new XMLManager();
        idx = 0;

        //default nodes
        addLabel("localhost");
        addLabel("all");
    }

    public static void add(Obj o) {
        switch(o.kind) {
            case Obj.var_:
                switch(o.type.kind) {
                    case Struct.caste_: //add label instruction
                        addLabel(o.name);
                        addEnv(o);
                        break;
                    default: //TODO list/record/enum ?
                        addLabel(o.name);
                        addState(o);
                        break;
                }
                break;
            case Obj.action_:
                addLabel(o.name);
                addAction(o);
            case Obj.type_:
                switch(o.type.kind) {
                    case Struct.caste_: //add final caste instruction
                        addCaste(o);
                        break;
                }
                break;
        }
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
        constants.add(ins);
    }

    static void addEnv(Obj o) {
        Instruction ins = new Instruction();
        ins.index = idx++;
        ins.kind = Instruction.constant_;
        ins.code = Instruction.env_;
        ins.name = getIndex(o.name);
        ins.casteName = getIndex(o.type.name);

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
        String sepChar = ";"; //used for the split
        for(Instruction i : constants)
            switch(i.code) {
                case Instruction.state_: sStates += i.index + sepChar; break;
                case Instruction.action_: sActions += i.index + sepChar; break;
                case Instruction.env_ : sEnvs += i.index + sepChar; break;
            }
        ins.states = sStates.split(sepChar);
        ins.actions = sActions.split(sepChar);
        ins.envs = sEnvs.split(sepChar);

        //TODO: how to fill these fields ?
        ins.url = getIndex("localhost");
        ins.cons = new String[0];

        constants.add(ins);
    }

    //add a simple label with a name
    static public void add(String str) {
        addLabel(str);
    }

    // Write the code buffer to the output stream
    public static void write() {
        xml.generateFile(constants, init, rooting);
    }

    static Instruction getInstruction(String name) {
        Instruction instruction = null;
        for(Instruction ins : constants)
            if(ins.name.equals(name))
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

}
