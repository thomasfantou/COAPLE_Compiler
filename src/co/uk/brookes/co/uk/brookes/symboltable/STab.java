package co.uk.brookes.co.uk.brookes.symboltable;

import co.uk.brookes.Parser;
import co.uk.brookes.main;

/**
 * Author: Fantou Thomas
 * Date: 6/21/13
 */
public class STab {
    public static Scope curScope;	    // current scope
    public static int   curLevel;	    // nesting level of current scope

    public static Struct integerType;	// predefined types
    public static Struct realType;
    public static Struct boolType;
    public static Struct charType;
    public static Struct stringType;
    public static Struct noType;

    public static Obj noObj;

    //-------------- initialization of the symbol table ------------

    public static void init() {  // build the universe
        curScope = new Scope();
        curScope.outer = null;
        curLevel = -1;

        // create predeclared types
        integerType = new Struct(Struct.integer_);
        realType = new Struct(Struct.real_);
        boolType = new Struct(Struct.bool_);
        charType = new Struct(Struct.char_);
        stringType = new Struct(Struct.string_);
        noObj = new Obj(Obj.var_, "???", noType);

        // create predeclared objects
        insert(Obj.type_, "integer", integerType);
        insert(Obj.type_, "real", realType);
        insert(Obj.type_, "char", charType);
        insert(Obj.type_, "bool", boolType);
        insert(Obj.type_, "string", stringType);
    }

    //------------- Object insertion and retrieval --------------

    // Create a new object with the given kind, name and type
    // and insert it into the top scope.
    public static Obj insert(int kind, String name, Struct type)
    {
        //--- create object node
        Obj obj = new Obj(kind, name, type);
        if (kind == Obj.var_)
        {
            obj.adr = curScope.nVars; curScope.nVars++;
            obj.level = curLevel;
        }
        //--- append object node
        Obj p = curScope.locals, last = null;
        while (p != null)
        {
            if (p.name.equals(name))  error("Variable \'" + name + "\' is already defined in the scope");
            last = p; p = p.next;
        }
        if (last == null) curScope.locals = obj; else last.next = obj;
        if(main.LOGGING_SYMBAB_ENABLED){
            for(int i = 0; i < curLevel + 1; i++) System.out.print("--");
            System.out.println(Obj.values[kind] + "(" + Struct.values[type.kind] +") \"" + name + "\"");
        }
        return obj;
    }

    // Retrieve the object with the given name from the top scope
    public static Obj find(String name)
    {
        for (Scope s = curScope; s != null; s = s.outer)
            for (Obj p = s.locals; p != null; p = p.next)
                if (p.name.equals(name)) return p;
        error(name + " is undeclared");
        return noObj;
    }

    // Retrieve a field from a caste
    public static Obj findField(String name, Struct type)
    {
        if(type.kind == Struct.caste_ ||
                type.kind == Struct.enum_ ||
                type.kind == Struct.list_ ||
                type.kind == Struct.record_)
        {
            for(Obj p = type.fields; p != null; p = p.next)
                if(p.name.equals(name)) return p;
            error(name + " is not a field from the type");
        }
        return noObj;
    }


    //------------------ scope management ---------------------

    public static void openScope()
    {
        Scope s = new Scope();
        s.outer = curScope;
        curScope = s;
        curLevel++;
    }

    public static void closeScope()
    {
        curScope = curScope.outer;
        curLevel--;
    }

    //------------------ error management ---------------------

    private static void error(String msg)
    {
        Parser.SemErr(msg);
    }

}

