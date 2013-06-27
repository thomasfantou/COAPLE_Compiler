package co.uk.brookes.co.uk.brookes.symboltable;

/**
 * Author: Fantou Thomas
 * Date: 6/21/13
 */
public class Scope {
    public Scope outer;		// to outer scope
    public Obj   locals;	// to local variables of this scope
    public int   nVars;     // number of variables in this scope
}