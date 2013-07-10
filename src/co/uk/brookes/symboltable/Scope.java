package co.uk.brookes.symboltable;

/**
 * Author: Fantou Thomas
 * Date: 6/21/13
 */
public class Scope {
    public Scope outer;		// to outer scope
    public Obj  locals;	// to local variables of this scope
    public int  nVars;     // number of variables in this scope
    public int  nStates;    // number of states in this scope
    public int  nActions;    // number of actions in this scope
    public int  nEnvs;    // number of states in this scope
}