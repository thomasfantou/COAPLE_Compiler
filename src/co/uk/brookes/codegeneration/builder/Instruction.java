package co.uk.brookes.codegeneration.builder;

import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.Struct;

/**
 * Author: Fantou Thomas
 * Date: 7/1/13
 */
public class Instruction {
    public static final int  // instruction codes
        pushvar_         =  1,
        storevar_        =  2,
        pushselfid_      =  3,
        push_            =  4,
        pop_             =  5,
        dup_             =  6,
        pushconstantsec_ =  7,
        pushlistentry_   =  8,
        add_             =  9,
        sub_             = 10,
        mul_   = 11,
        div_   = 12,
        int2string_    = 13,
        if_    = 14,
        comparestring_      = 15,
        nop_      = 16,
        return_      = 17,
        jump_     = 18,
        quit_      = 19,
        yield_      = 20,
        loadcaste_    = 21,
        agentnew_      = 22,
        agentalloc_       = 23,
        agentdealloc_         = 24,
        agentregister_         = 25,
        agentregisterpost_         = 26,
        agentready_         = 27,
        instanceof_         = 28,
        instanceset_  = 29,
        instancesetpost_ = 30,
        sendmessage_= 31,
        updatestate_ = 32,
        updateaction_ = 33,
        setstate_ = 34,
        param_    = 35,
        paramstring_     = 36,
        observe_ = 37,
        patternexist_     = 38,
        patternmatch_ = 39,
        resetvar_ = 40,
        pushenvthruvar_     = 41,
        pushenvthrustate_ = 42,
        pushstate_ = 43,
        loaddll_   = 44,
        dllcall_     = 45,
        freedll_    = 46,
        output_     = 47,
        print_    = 48,
        openwindow_      = 49,
        setagentstate_      = 50,
        recordnew_ = 51,
        putfield_   = 52,
        getfield_   = 53,
        pulishstate_   = 54,
        listnew_   = 55,
        putentry__   = 56,
        getentry_   = 57;

    public static final int  // instruction kind
        constant_ = 0,
        bytecode_     = 1;

    public static final int  // constant codes
        label_   = 0,
        state_  = 1,
        action_ = 2,
        env_    = 3,
        caste_  = 4;




    public int kind;    //constant / bytecode
    public int code;    //Init/Rooting : instruction code or Constant : constant code
    public int index;   //Constants : element index param
    public String val;  //Constants(utf8) : xml value
    public String name; //Constants(state/env/action) : name node
    public int typeCode; //Constants(state) : struct type
    public String type; //Contants(env)
    public String scope;    //Constants(env) :
    public String casteName;    //Constants(env)
    public String url;  // Constants(env)

    public int nParam;   //Constants(action)
    public Obj params; //Constants(action) : parameters

    //caste constant information
    public String[] cons;
    public String[] states;
    public String[] actions;
    public String[] envs;

    public Instruction() {


    }
}
