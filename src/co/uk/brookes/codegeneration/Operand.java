package co.uk.brookes.codegeneration;

import co.uk.brookes.Parser;
import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.STab;
import co.uk.brookes.symboltable.Struct;

/**
 * Author: Fantou Thomas
 * Date: 7/1/13
 */
public class Operand {
    public static final int  // item kinds
            con_    = 0,
            state_  = 1,
            local_ = 2,
            stack_  = 3,
            action_   = 4,
            listelem_ = 5,
            field_ = 6,
            environment_ = 7;

    public int    kind;	//
    public Struct type;	// item type
    public Obj obj;  // Action
    public String    val;  // Con: value
    public int    adr;  // Local, State, Field, Action: address

    public Operand(Obj o) {
        type = o.type; val = String.valueOf(o.val); adr = o.adr; kind = stack_; // default
        switch (o.kind) {
            case Obj.con_:
                kind = con_; break;
            case Obj.state_:
                kind = state_; break;
            case Obj.var_:
                kind = local_; break;
            case Obj.action_:
                kind = action_; obj = o; break;
            case Obj.environment_:
                kind = environment_; break;
            case Obj.type_:
                Parser.SemErr("type identifier not allowed here"); break;
            default:
                Parser.SemErr("wrong kind of identifier"); break;
        }
    }

    public Operand(int val) {
        kind = con_; this.val = String.valueOf(val); type = STab.integerType;
    }

    public Operand(float val) {
        kind = con_; this.val = String.valueOf(val); type = STab.realType;
    }

    public Operand(String val) {
        kind = con_; this.val = val; type = STab.stringType;
    }

    public Operand(int kind, int val, Struct type) {
        this.kind = kind; this.val = String.valueOf(val); this.type = type;
    }
}
