package co.uk.brookes.symboltable;

/**
 * Author: Fantou Thomas
 * Date: 6/21/13
 */
public class Obj {
    public static final int // object kinds
            con_  = 0,
            var_  = 1,
            type_ = 2,
            action_ = 3,
            typeField_ = 4,
            environment_ = 5,
            state_  = 6;

    public static final String[] values = {
            "con", "var", "type", "action", "type_field", "environment"};

    public int    kind;		// Con, Var, Type, Meth, Prog
    public String name;		// object name
    public Struct type;	 	// object type
    public int    val;      // Con: value
    public int    adr;      // address in the scope of the current kind
    public int    level;    // Var/Con/TypeField: declaration level
    public int    nPars;    // Action: number of parameters
    public Obj    locals;   // Action: parameters and local objects
    public Obj    next;		// next local object in this scope

    public Obj(int kind, String name, Struct type) {
        this.kind = kind; this.name = name; this.type = type;
    }
}
