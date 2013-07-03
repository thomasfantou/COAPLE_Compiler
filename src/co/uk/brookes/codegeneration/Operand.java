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
            field_ = 6;

    public int    kind;	//
    public Struct type;	// item type
    public Obj obj;  // Action
    public int    val;  // Con: value
    public int    adr;  // Local, State, Field, Action: address

    public Operand(Obj o) {
        type = o.type; val = o.val; adr = o.adr; kind = stack_; // default
        switch (o.kind) {
            case Obj.con_:
                kind = con_; break;
            case Obj.var_:
                if (o.level == 1) kind = state_; else kind = local_;
                break;
            case Obj.action_:
                kind = action_; obj = o; break;
            case Obj.type_:
                Parser.SemErr("type identifier not allowed here"); break;
            default:
                Parser.SemErr("wrong kind of identifier"); break;
        }
    }

    public Operand(int val) {
        kind = con_; this.val = val; type = STab.integerType;
    }

    public Operand(int kind, int val, Struct type) {
        this.kind = kind; this.val = val; this.type = type;
    }
}
