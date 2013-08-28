
package co.uk.brookes;

import co.uk.brookes.codegeneration.builder.Builder;
import co.uk.brookes.codegeneration.builder.Instruction;
import co.uk.brookes.symboltable.Obj;
import co.uk.brookes.symboltable.STab;
import co.uk.brookes.symboltable.Struct;
import co.uk.brookes.codegeneration.Code;
import co.uk.brookes.codegeneration.Operand;

import java.util.ArrayList;
import java.util.BitSet;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _url = 3;
	public static final int maxT = 300;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

    static public Token t;    // last recognized token
    static public Token la;   // lookahead token
    static int errDist = minErrDist;
	
	public Scanner scanner;
    static public Errors errors;

    private static BitSet startOfExp;
    private static BitSet syncStat;
	

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	static void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

    static public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}

    void Sync(String msg) {
        SemErr(msg);
        while (!syncStat.get(la.kind)) Get();
        errDist = 0;
    }
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
        if(la.kind <= 71) //default generated set[] table is of size 71
		    return set[s][la.kind];
        else return false;
	}

	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

    //Coaple = Prog.
	void Coaple() {
        STab.openScope();
		Prog();
	}

    //Prog =
    //  {TypeDec} {CasteDec}.
	void Prog() {
		while (la.kind == Token.type_) {
			TypeDec();
		}
		while (la.kind == Token.caste_) {
			CasteDec();
		}
	}

    //TypeDec =
    //  "type" StructureType {StructureType} "end".
	void TypeDec() {
		Expect(Token.type_);
		StructureType();
		while (la.kind == Token.record_ || la.kind == Token.list_ || la.kind == Token.enumerate_) {
			StructureType();
		}
        Expect(Token.end_);
	}


    //CasteDec =
    //    "caste" ident ["("[ParameterList]")"] [Inheritances] ";"
    //    [EnvironmentDecs]
    //    "state" [StateDecs]
    //    [ActionDecs]
    //    "init" [Statement]
    //"body"
    //    [LocalDecs]
    //    [Statement]
    //"end" ident.
	void CasteDec() {
		Expect(Token.caste_);
		Expect(Token.ident);
        Obj o = STab.insert(Obj.type_, t.val, new Struct(Struct.caste_));
        Builder.add(o.name);
        STab.openScope();
        if(la.kind == Token.lpar) {
            Get();
            if (la.kind == Token.ident) {
                ParameterList();
            }
            Expect(Token.rpar);
        }
		if (la.kind == Token.inherits_) {
			Inheritances();
		}
		Expect(Token.semicolon);
        if (la.kind == Token.observes_) {
            Builder.step = Builder.envdec_;
			EnvironmentDecs();
		}
		if (la.kind == Token.state_) {
            Builder.step = Builder.statedec_;
            Get();
			StateDecs();
		}
		if (la.kind == Token.action_) {
            Builder.step = Builder.actiondec_;
			ActionDecs();
		}
		Expect(Token.init_);
        Builder.step = Builder.init_;
        if (StartOf(3)) {
            Statement();
        }
        Builder.endInit();
		Expect(Token.body_);
        Builder.step = Builder.rooting_;
		if (la.kind == Token.var_) {
            LocalDecs();
		}
        if (StartOf(3)) {
            Statement();
        }
		Expect(Token.end_);
		Expect(Token.ident);
        Builder.endRooting();
        Builder.step = Builder.castereview_;
        Builder.add(o);
        STab.closeScope();
	}

    //StructureType =
    //    RecordType
    //    | ListType
    //    | EnumeratedType.
	Struct StructureType() {
        Struct type = null;
		if (la.kind == Token.record_) {
			RecordType();
            type = new Struct(Struct.record_);
		} else if (la.kind == Token.list_) {
			ListType();
            type = new Struct(Struct.list_);
		} else if (la.kind == Token.enumerate_) {
			EnumeratedType();
            type = new Struct(Struct.enum_);
		} else SynErr(71);
        return type;
	}

    //TypeExp =
    //    PrimitiveType
    //    | StructureType
    //    | ID.
	Struct TypeExp() {
        Struct type = null;
		if (StartOf(1)) {
            type = PrimitiveType();
		} else if (la.kind == Token.record_ || la.kind == Token.list_ || la.kind == Token.enumerate_) {
            type = StructureType();
		} else if (la.kind == Token.ident) {
			String id = ID();
            Obj obj = STab.find(id);
            if(!obj.equals(STab.noObj)){
                if(obj.kind == Obj.type_) {
                    type = obj.type;
                    type.name = id;
                }
                else
                    SemErr("invalid type");
            }

		} else SynErr(72);
        return type;
	}

    //PrimitiveType =
    //  "integer"
    //  | "real"
    //  | "bool"
    //  | "char"
    //  | "string".
	Struct PrimitiveType() {
        Struct type = null;
		if (la.kind == Token.integer_) {
			Get();
            type = new Struct(Struct.integer_);
		} else if (la.kind == Token.real_) {
			Get();
            type = new Struct(Struct.real_);
		} else if (la.kind == Token.bool_) {
			Get();
            type = new Struct(Struct.bool_);
		} else if (la.kind == Token.char_) {
			Get();
            type = new Struct(Struct.char_);
		} else if (la.kind == Token.string_) {
			Get();
            type = new Struct(Struct.string_);
		} else SynErr(73);

        return type;
	}

    //ID =
    //  ident [url].
	String ID() {
		Expect(Token.ident);
        String val = t.val;
		if (la.kind == Token.atsign) {
			Get();
		}
        return val;
	}

    //RecordType =
    //    "record" ident "of" { ident {"," ident} ":" TypeExp ";" }
    //    "end".
	void RecordType() {
		Expect(Token.record_);
		Expect(Token.ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.record_));
		Expect(Token.of_);
        STab.openScope();
        while(la.kind == Token.ident) {
            ArrayList<String> vars = new ArrayList<String>();
            Get();
            vars.add(t.val);
            while (la.kind == Token.comma) {
                Get();
                Expect(Token.ident);
                vars.add(t.val);
            }
            if (la.kind != Token.colon) {
                Sync("type expected");
            } else {
                Expect(Token.colon);
                Struct type = TypeExp();
                for(String v : vars)
                    STab.insert(Obj.typeField_, v, type);
                Expect(Token.semicolon);
            }
        }
		Expect(Token.end_);
        STab.setFields(obj);
        STab.closeScope();
	}

    //ListType =
    //  "list" ident "of" TypeExp "end".
	void ListType() {
		Expect(Token.list_);
		Expect(Token.ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.list_));
		Expect(Token.of_);
        STab.openScope();
		Struct type = TypeExp();
        obj.type.listType = type;
		Expect(Token.end_);
        STab.closeScope();
	}

    //EnumeratedType =
    //  "enumerate" ident "=" IDList "end".
	void EnumeratedType() {
		Expect(Token.enumerate_);
		Expect(Token.ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.enum_));
		Expect(Token.eqlSign);
        STab.openScope();
		ArrayList<String> ids = IDList();
        for(int i = 0; i < ids.size(); i++)
            STab.insert(Obj.typeField_, ids.get(i), new Struct(Struct.integer_));
		Expect(Token.end_);
        STab.setFields(obj);
        STab.closeScope();
	}

    //IDList =
    //  ident {"," ident}.
	ArrayList<String> IDList() {
        ArrayList<String> ids = new ArrayList<String>();
        if (la.kind != Token.ident) {
            Sync("invalid identifier");
        }
        else{
            Expect(Token.ident);
            ids.add(t.val);
            while (la.kind == Token.comma) {
                Get();
                Expect(Token.ident);
                ids.add(t.val);
            }
        }
        return ids;
	}

    //Inheritances =
    //  "inherits" ident {"," ident}.
	void Inheritances() { //TODO
		Expect(Token.inherits_);
		Expect(Token.ident);
		while (la.kind == Token.comma) {
			Get();
			Expect(Token.ident);
		}
	}

    //EnvironmentDecs =
    //    "observes" {EnvDec ";"}.
	void EnvironmentDecs() {
		Expect(Token.observes_);
		while (StartOf(2)) {
			EnvDec();
			Expect(Token.semicolon);
		}
	}

    //StateDecs =
    //  StateDec ";" {StateDec ";"}.
	void StateDecs() {
        StateDec();
        Expect(Token.semicolon);
        while (la.kind == Token.var_) {
            StateDec();
            Expect(Token.semicolon);
        }
	}

    //ActionDecs =
    //    ActionDec ";" {ActionDec ";"}.
	void ActionDecs() {
		ActionDec();
		Expect(Token.semicolon);
		while (la.kind == Token.action_) {
			ActionDec();
			Expect(Token.semicolon);
		}
	}

    //Statement =
    //    Designator (":=" Exp | "(" [IDList] ")") ";"
    //    | CasteEvent ";"
    //    | AgentEvent ";"
    //    | Block
    //    | LoopStatement
    //    | IfStatement
    //    | CaseStatement
    //    | WithStatement
    //    | ForAllStatement
    //    | WhenStatement.
	void Statement() {
        if (!StartOf(3)) {
            Sync("invalid start of statement");
        } else {
            Builder.statementStart();
            switch (la.kind) {
                case Token.ident: {
                    Obj obj = Designator();
                    Operand op = new Operand(obj);
                    if (la.kind == Token.assign) {
                        obj.initialized = true;
                        Get();
                        int type = Exp();
                        Struct stype = new Struct(type); //create the struct to check if it is assignable
                        if(type == Struct.none_)    //if the expression is an invalid factor
                            while(la.kind != Token.semicolon && la.kind != Token.eof) Get();
                        else if(op.type != null) {  //if the designator is undeclared
                            if(!stype.assignableTo(op.type))
                                SemErr("Incompatible types: " + Struct.values[type] + " to " + Struct.values[op.type.kind]);
                        }
                        Builder.assign(op);
                    } else if (la.kind == Token.lpar) { //this is the call of an action
                        Get();

                        int nPars = 0;
                        if (la.kind == Token.ident) {
                            ArrayList<String> ids = IDList();
                            nPars = ids.size();
                            if(nPars == op.obj.nPars){     //if the action call has the same amount of param
                                for(int i = 0; i < ids.size(); i++){ //we are looping through the params
                                    Obj o = STab.find(ids.get(i));
                                    if(o != STab.noObj) {   //if the parameter is defined
                                        for(Obj p = op.obj.locals; p != null; p = p.next) {
                                            if(p.adr == i) {    //for the param number i, we will find the object in the locals variable of the method to the position i
                                                if(p.type.kind != o.type.kind) {
                                                    SemErr("Param number " + i + " has uncompatible type");
                                                }
                                                break;
                                            }
                                        }
                                        Builder.load(new Operand(o));
                                        Builder.setParam(o, i);
                                    }
                                }
                            }
                        }
                        if(op.obj != null && nPars != op.obj.nPars){     //if the action call doesn't have the same amount of param
                            SemErr("Invalid amount of parameters");
                        }

                        Builder.putAction(obj);

                        Expect(Token.rpar);
                    } else SynErr(74);
                    Expect(Token.semicolon);
                    break;
                }
                case Token.join_: case Token.quit_: case Token.suspend_: case Token.resume_: {
                    CasteEvent();
                    Expect(Token.semicolon);
                    break;
                }
                case Token.create_: case Token.destroy_: {
                    AgentEvent();
                    Expect(Token.semicolon);
                    break;
                }
                case Token.begin_: {
                    Block();
                    break;
                }
                case Token.loop_: case Token.while_: case Token.repeat_: case Token.for_: {
                    LoopStatement();
                    break;
                }
                case Token.if_: {
                    IfStatement();
                    break;
                }
                case Token.case_: {
                    CaseStatement();
                    break;
                }
                case Token.with_: {
                    WithStatement();
                    break;
                }
                case Token.forall_: {
                    ForAllStatement();
                    break;
                }
                case Token.when_: {
                    WhenStatement();
                    break;
                }
                default: SynErr(75); break;
            }
        }
	}

    //Block =
    //  "begin" { Statement } "end".
    void Block() {
        Expect(Token.begin_);
        STab.openScope();
        while(StartOf(3)) {
            Statement();
        }
        Expect(Token.end_);
        STab.closeScope();
    }

    //StateDec =
    //    "var" IDList ":" TypeExp [":=" ident].
	void StateDec() {
        Expect(Token.var_);
        ArrayList<String> ids = IDList();
        if (la.kind != Token.colon) {
            Sync("type expected");
        } else {
            Expect(Token.colon);

            Struct type = TypeExp();
            if (type == null) {
                Sync("invalid start of statement");
            } else {
                for(String id : ids){
                    Obj obj = STab.insert(Obj.state_, id, type);
                    Builder.add(obj);
                }
                if (la.kind == Token.assign) {
                    Get();
                    Expect(Token.ident);
                }
            }
        }
	}

    //EnvDec =
    //    ident
    //    | "all" ident "in" ID
    //    | "var" IDList "in" ID [":=" AgentID]
    //    | "set" IDList "in" ID [":=" AgentSetEnum].
	void EnvDec() {
		if (la.kind == Token.ident) {
			Get();
		} else if (la.kind == Token.all_) {
			Get();
            Expect(Token.ident);
            String ident = t.val;
			Expect(Token.in_);
			String id = ID();
            Builder.add(id);    //add the value to the constants object file
            Obj objID = STab.find(id);
            if(!objID.equals(STab.noObj)){ //if object defined
                if(objID.kind == Obj.type_ && objID.type.kind == Struct.caste_) { //if it's a caste
                    Obj objIdent = STab.insert(Obj.environment_, ident, objID.type);
                    objIdent.type.name = id;
                    Builder.add(objIdent);
                }
                else
                    SemErr("invalid caste");
            }
            else {  //TODO: if caste undefined (but what if it is defined later or in another file) (proposition: stack it and check the stack when the first parse is done)
                Struct struct = new Struct(Struct.caste_);
                struct.name = id;
                Obj objIdent = STab.insert(Obj.environment_, ident, struct);
                Builder.add(objIdent);
            }
		} else if (la.kind == Token.var_) {
			Get();
            ArrayList<String> ids = IDList();
			Expect(Token.in_);
            String id = ID();
            Builder.add(id);    //add the value to the constants object file
            Obj objID = STab.find(id);
            if(!objID.equals(STab.noObj)){ //if object defined
                if(objID.kind == Obj.type_ && objID.type.kind == Struct.caste_) { //if it's a caste
                    for(String ident : ids){
                        Obj objIdent = STab.insert(Obj.environment_, ident, objID.type);
                        objIdent.type.name = id;
                        Builder.add(objIdent);
                        //the env val is now added to the constant section, then we add instructions for observing it
                        Operand op = new Operand(objIdent);
                        Builder.observe(op);
                    }
                }
                else
                    SemErr("invalid caste");
            }
            else {  //TODO: if caste undefined (but what if it is defined later or in another file) (proposition: stack it and check the stack when the first parse is done)
                Struct struct = new Struct(Struct.caste_);
                struct.name = id;
                for(String ident : ids){
                    Obj objIdent = STab.insert(Obj.environment_, ident, struct);
                    Builder.add(objIdent);
                    //the env val is now added to the constant section, then we add instructions for observing it
                    Operand op = new Operand(objIdent);
                    Builder.observe(op);
                }
            }
			if (la.kind == Token.assign) {
				Get();
				AgentID();
			}
		} else if (la.kind == Token.set_) {
			Get();
            IDList();
			Expect(Token.in_);
			ID();
			if (la.kind == Token.assign) {
				Get();
				AgentSetEnum();
			}
		} else SynErr(76);
	}

    //AgentID	=
    //  ident "in" ID.
	void AgentID() { //TODO ex : var mon in Monitor := x in Monitor;    why repeat "in Monitor" with the x ?
		Expect(Token.ident);
		Expect(25);
		ID();
	}

    //AgentSetEnum =
    //  "{"
    //    ident {"," ident}
    //  "}".
	void AgentSetEnum() {
		Expect(Token.lbrace);
		Expect(Token.ident);
		while (la.kind == Token.comma) {
			Get();
			Expect(Token.ident);
		}
		Expect(Token.rbrace);
	}

    //LocalDecs =
    //  LocalDec ";" {LocalDec ";"}.
    void LocalDecs() {
        LocalDec();
        Expect(Token.semicolon);
        while (la.kind == Token.var_) {
            LocalDec();
            Expect(Token.semicolon);
        }
    }

    //LocalDec =
    //  "var" IDList ":" TypeExp [":=" ident].
	void LocalDec() {
		Expect(Token.var_);
		ArrayList<String> ids = IDList();
		Expect(Token.colon);
		Struct type = TypeExp();
        for(String id : ids)
            STab.insert(Obj.var_, id, type);
		if (la.kind == Token.assign) {
			Get();
			Expect(Token.ident);
		}
	}

    //ActionDec =
    //"action" IDList "("[ParameterList]")"
    //    [Statement]
    //    [Impact].
	void ActionDec() {
		Expect(Token.action_);
        ArrayList<String> ids = IDList();
        Builder.setCurrentActionNames(ids); //the statement of the actions can be correctly related to the actions objects
        Obj[] objs = new Obj[ids.size()];
        int nParam = 0;
        for(int i = 0; i < ids.size(); i++) //because IDList
            objs[i] = STab.insert(Obj.action_, ids.get(i), new Struct(Struct.none_));
		Expect(Token.lpar);
        STab.openScope();
		if (la.kind == Token.ident) {
			nParam = ParameterList();
		}
        for(int i = 0; i < objs.length; i++)
            objs[i].nPars = nParam;
        Builder.setCurrentActionParam(nParam);

		Expect(Token.rpar);
		if (StartOf(3)) {
			Statement();
		}
        for(int i = 0; i < objs.length; i++) {
            objs[i].locals = STab.curScope.locals;
            Builder.add(objs[i]);
        }
		if (la.kind == Token.affect_) {
			Impact();
		}
        STab.closeScope();
	}

    //ParameterList =
    //  Parameter {"," Parameter}.
	int ParameterList() {
        int nParam = 0;
		nParam += Parameter();
		while (la.kind == Token.comma) {
			Get();
			nParam += Parameter();
		}
        return nParam;
	}

    //Impact =
    //  "affect" ID { "," ID }.
	void Impact() {
		Expect(Token.affect_);
		ID();
		while (la.kind == Token.comma) {
			Get();
			ID();
		}
	}

    //Parameter =
    //  ident {"," ident} ":" TypeExp.
	int Parameter() {
        int nParam = 0;
        ArrayList<String> ids = new ArrayList<String>();
		Expect(Token.ident);
        ids.add(t.val);
        nParam++;
		while (la.kind == Token.comma) {
			Get();
			Expect(Token.ident);
            ids.add(t.val);
            nParam++;
		}
        if (la.kind != Token.colon) {
            Sync("type expected");
        } else {
            Expect(Token.colon);
            Struct type = TypeExp();
            for(String id : ids) {
                Obj obj = STab.insert(Obj.var_, id, type);
                obj.initialized = true; //a parameter is an initialized variable
            }
        }
        return nParam;
	}

    //Designator =
    //  ident { "." ident } ["[" Exp "]"].
	Obj Designator() {
        Operand op;
        Expect(Token.ident);
        Obj obj = STab.find(t.val);
        while (la.kind == Token.period) {
            Get();
            Expect(Token.ident);
        }
        if (la.kind == Token.lbrack) {
            Get();
            Exp();
            Expect(Token.rbrack);
        }

        return obj;
	}

    //Exp =
    //    Term {Addop Term}.
	int Exp() {
        int type = Term();
        while (la.kind == Token.plus || la.kind == Token.minus || la.kind == Token.concat) {
            String insVal = Addop();
            int type2 = Term();
            if((type == Struct.integer_ && type2 == Struct.integer_) ||
                    (type == Struct.real_ && type2 == Struct.real_) ||
                    (type == Struct.string_ && type2 == Struct.string_)) {  //TODO: string concatenation
                switch(type){
                    case Struct.integer_: Builder.put(insVal + Instruction.typeint_); break;
                    case Struct.real_: Builder.put(insVal + Instruction.typereal_); break;
                }
            }
            else {
                SemErr("Incompatible types: " + Struct.values[type] + " and " + Struct.values[type2]);
                while(la.kind != Token.semicolon && la.kind != Token.eof) Get();
            }
        }

        return type;
	}

    //Conditions =
    //  Condition {Boolop Condition}.
    //return addresses of jumps which need to be fixed later
    ArrayList<Integer> Conditions() {
        int[] res = Condition();    //return type of exp and relation operator
        ArrayList<Integer> addresses = new ArrayList<Integer>();
        while(la.kind == Token.and_ || la.kind == Token.or_) {
            res[2] = la.kind;   //add the token to the result
            addresses.add(Builder.condition(res));  //the Builder put the right instruction regarding the information in res, the address of the instruction will be required for a fixup
            Boolop();
            res = Condition();
        }
        addresses.add(Builder.condition(res));
        return addresses;
    }

    //Condition =
    //  Exp [Relop Exp].
    int[] Condition() { //return the the type of expression + relation operator
        int[] res = new int[3]; //Builder.condition expect 3 information: 0- variable type, 1- relation operator, 2- boolean operator
        int type = Exp();
        res[0] = type;
        res[1] = Token.none; //by default, there is no relation operator (case of boolean condition)
        res[2] = Token.none; //by default, there is no boolean operator
        if(la.kind == Token.neq || la.kind == Token.gtr || la.kind == Token.geq
                || la.kind == Token.lss || la.kind == Token.leq || la.kind == Token.eqlSign) {
            res[1] = la.kind;
            Relop();
            int type2 = Exp();
            if((type == Struct.integer_ && type2 == Struct.integer_) || //TODO: Only integer, real and string are treated here for condition
                (type == Struct.real_ && type2 == Struct.real_) ||
                (type == Struct.string_ && type2 == Struct.string_)) {
            }
            else{
                Sync("Not comparable types: " + Struct.values[type] + " and " + Struct.values[type2]);
            }
        }
        return res;
    }

    //CasteEvent =
    //  "join" ID "("[ident]")"
    //  | "quit" ID
    //  | "suspend" ID
    //  | "resume" ID.
	void CasteEvent() {
		if (la.kind == Token.join_) {
			Get();
			ID();
			Expect(Token.lpar);
			if (la.kind == Token.ident) {
				Get();
			}
			Expect(Token.rpar);
		} else if (la.kind == Token.quit_) {
			Get();
			ID();
		} else if (la.kind == Token.suspend_) {
			Get();
			ID();
		} else if (la.kind == Token.resume_) {
			Get();
			ID();
		} else SynErr(77);
	}

    //AgentEvent =
    //  "create" ident "of" ID "("[ident]")" [url]
    //  | "destroy" ident.
	void AgentEvent() { //TODO is it like "new object()"
		if (la.kind == Token.create_) {
			Get();
			Expect(Token.ident);
			Expect(Token.of_);
			ID();
			Expect(Token.lpar);
			if (la.kind == Token.ident) {
				Get();
			}
			Expect(Token.rpar);
			if (la.kind == Token.atsign) {
				Get();
			}
		} else if (la.kind == Token.destroy_) {
			Get();
			Expect(Token.ident);
		} else SynErr(78);
	}

    //LoopStatement =
    //  ForLoop
    //  | WhileLoop
    //  | RepeatLoop
    //  | Loop.
	void LoopStatement() {
		if (la.kind == Token.for_) {
			ForLoop();
		} else if (la.kind == Token.while_) {
			WhileLoop();
		} else if (la.kind == Token.repeat_) {
			RepeatLoop();
		} else if (la.kind == Token.loop_) {
			Loop();
		} else SynErr(79);
	}

    //IfStatement =
    //    "if" Conditions "then" Statement
    //    {"elseif" Conditions "then" Statement}
    //    ["else" Statement]
    //    "end".
	void IfStatement() {
		Expect(Token.if_);
        Builder.statementType = Builder.if_statement; //the conditions has to generate the 'if' instructions (because Conditions is also used in 'while' and 'repeat')
        ArrayList<Integer> addresses = Conditions();
        ArrayList<Integer> jumpAddresses = new ArrayList<Integer>();    //after every statement of 'if' and 'elseif', we need to jump over the 'else'
		Expect(Token.then_);
		Statement();
        jumpAddresses.add(Builder.put(Instruction.jump_));   //jump over the "elseif" and "else" conditions
        Builder.fixup(addresses);
		while (la.kind == Token.elseif_) {
			Get();
            addresses = Conditions();
			Expect(Token.then_);
			Statement();
            jumpAddresses.add(Builder.put(Instruction.jump_));   //jump over the "elseif" and "else" conditions
            Builder.fixup(addresses);
		}
		if (la.kind == Token.else_) {
			Get();
			Statement();
		}
        Builder.fixup(jumpAddresses);   //fixup the instruction to jump after the statement

		Expect(Token.end_);
	}

    //CaseStatement =
    //  "case" Exp "of"
    //  {Exp "->" Statement}
    //  ["else" Statement]
    //  "end".
	void CaseStatement() {
		Expect(Token.case_);
		Exp();
		Expect(Token.of_);
		while (startOfExp.get(la.kind)) {
			Exp();
			Expect(Token.effect);
			Statement();
		}
		if (la.kind == Token.else_) {
			Get();
			Statement();
		}
		Expect(Token.end_);
	}

    //WithStatement =
    //  "with" Exp "do" Statement "end".
	void WithStatement() {
		Expect(Token.with_);
		Exp();
		Expect(Token.do_);
		Statement();
		Expect(Token.end_);
	}

    //ForAllStatement =
    //  "forall" ident "in" Exp "do"
    //  Statement
    //  "end".
	void ForAllStatement() {
		Expect(Token.forall_);
		Expect(Token.ident);
		Expect(Token.in_);
		Exp();
		Expect(Token.do_);
		Statement();
		Expect(Token.end_);
	}

    //WhenStatement =
    //  "when" {Scenario "->" Statement} "end".
	void WhenStatement() {
		Expect(Token.when_);
		while (la.kind == Token.ident || la.kind == Token.exist_ || la.kind == Token.all_) {
			Scenario();
			Expect(Token.effect);
			Statement();
		}
		Expect(Token.end_);
	}

    //ForLoop =
    //  "for" ident ":=" Exp "to" Exp ["by" Exp] "do"
    //  Statement
    //  "end".
	void ForLoop() {
		Expect(Token.for_);
		Expect(Token.ident);
		Expect(Token.assign);
		Exp();
		Expect(Token.to_);
		Exp();
		if (la.kind == Token.by_) {
			Get();
			Exp();
		}
		Expect(Token.do_);
		Statement();
		Expect(Token.end_);
	}

    //WhileLoop =
    //  "while" Conditions "do" Statement "end".
	void WhileLoop() {
        Builder.statementType = Builder.while_statement; //the conditions has to generate the 'while' instructions (because Conditions is also used in 'if' and 'repeat')
		Expect(Token.while_);
        String startAddressOfCondition = Builder.getCurrentAddress();  //we will need to jump back to the condition at the end of the statement
        ArrayList<Integer> addresses = Conditions();
		Expect(Token.do_);
		Statement();
        Builder.put(Instruction.jump_ + " " + startAddressOfCondition);
        Builder.fixup(addresses);
		Expect(Token.end_);
	}

    //RepeatLoop =
    //  "repeat" Statement "until" Conditions "end".
	void RepeatLoop() {
        Builder.statementType = Builder.repeat_statement; //the conditions has to generate the 'repeat' instructions (because Conditions is also used in 'if' and 'while')
		Expect(Token.repeat_);
		Statement();
		Expect(Token.until_);
        ArrayList<Integer> addresses = Conditions();
        Builder.fixup(addresses);   //jump instruction is in the JumpsIn list, it will be fixed up to jump back in the statement

		Expect(Token.end_);
	}

    //Loop =
    //  "loop" Statement "end".
	void Loop() {
		Expect(Token.loop_);
		Statement();
		Expect(Token.end_);
	}

    //Scenario =
    //  (ident ["in" ID] ":" ActionPattern
    //  | "exist" ident "in" ID ":" ActionPattern
    //  | "all" ident "in" ID ":" ActionPattern)
    //  [("and"|"or"|"xor") Scenario].
	void Scenario() {
		if (la.kind == Token.ident) {
			Get();
			if (la.kind == Token.in_) {
				Get();
				ID();
			}
			Expect(Token.colon);
			ActionPattern();
		} else if (la.kind == Token.exist_) {
			Get();
			Expect(Token.ident);
            Obj obj = STab.insert(Obj.var_, t.val, new Struct(Struct.none_)); //TODO set right type
            Expect(Token.in_);
			ID();
			Expect(Token.colon);
			ActionPattern();
		} else if (la.kind == 24) {
			Get();
			Expect(Token.ident);
            Obj obj = STab.insert(Obj.var_, t.val, new Struct(Struct.none_)); //TODO set right type
            Expect(Token.in_);
            ID();
            Expect(Token.colon);
			ActionPattern();
		} else SynErr(80);
		if (la.kind == Token.and_ || la.kind == Token.or_ || la.kind == Token.xor_) {
			if (la.kind == Token.and_) {
				Get();
			} else if (la.kind == Token.or_) {
				Get();
			} else {
				Get();
			}
			Scenario();
		}
	}

    //ActionPattern =
    //  ident "("[PatternPara {"," PatternPara}]")".
	void ActionPattern() {
		Expect(Token.ident);
		Expect(Token.lpar);
		if (la.kind == Token.ident || la.kind == Token.number || la.kind == Token.lpar) {
			PatternPara();
			while (la.kind == Token.comma) {
				Get();
				PatternPara();
			}
		}
		Expect(Token.rpar);
	}

    //PatternPara =
    //  Exp.
	void PatternPara() {
		Exp();
	}

    //Term =
    //  Factor {Mulop Factor}.
	int Term() {
        int type = Struct.none_;
		type = Factor();
		while (la.kind == Token.times || la.kind == Token.slash || la.kind == Token.rem) {
			String insVal = Mulop();

			int type2 = Factor();
            if((type == Struct.integer_ && type2 == Struct.integer_) ||
                    (type == Struct.real_ && type2 == Struct.real_)) {
                switch(type) {
                    case Struct.integer_: Builder.put(insVal + Instruction.typeint_); break;
                    case Struct.real_: Builder.put(insVal + Instruction.typereal_); break;
                }
            }
            else {
                SemErr("Incompatible types: " + Struct.values[type] + " and " + Struct.values[type2]);
                while(la.kind != Token.semicolon && la.kind != Token.eof) Get();
            }

		}
        return type;
	}

    //Addop =
    //  "+" | "-" | "++".
	String Addop() {
        String insVal = "";
		if (la.kind == Token.plus) {
			Get();
            insVal = Instruction.add_;
		} else if (la.kind == Token.minus) {
			Get();
            insVal = Instruction.sub_;
		}
        else if (la.kind == Token.concat) {
            Get();
        } else SynErr(81);
        return insVal;
	}

    //Mulop =
    //  "*" | "/" | "%".
	String Mulop() {
        String insVal = "";
		if (la.kind == Token.times) {
			Get();
            insVal = Instruction.mul_;
		} else if (la.kind == Token.slash) {
			Get();
            insVal = Instruction.div_;
		} else if (la.kind == Token.rem) {
			Get();
		} else SynErr(82);
        return insVal;
	}

    //Relop =
    //  "=" | "!=" | ">" | ">=" | "<" | "<=".
    void Relop() {
        if(la.kind == Token.neq || la.kind == Token.gtr || la.kind == Token.geq
                || la.kind == Token.lss || la.kind == Token.leq || la.kind == Token.eqlSign)
            Get();
    }

    //Boolop =
    //  "and" | "or".
    void Boolop() {
        if(la.kind == Token.and_ || la.kind == Token.or_)
            Get();
    }

    //Factor =
    //  number
    //  | ["not"] (Designator | "true" | "false")
    //  | "(" Exp ")"
    //  | charVal
    //  | stringVal.
	int Factor() {
        int type = Struct.none_;
		if (la.kind == Token.number) {
			Get();
            if(!t.val.contains(".")) {
                type = Struct.integer_;
                int number = Integer.parseInt(t.val);
                Operand op = new Operand(number);
                Builder.load(op);
            } else {
                type = Struct.real_;
                float number = Float.parseFloat(t.val);
                Operand op = new Operand(number);
                Builder.load(op);
            }
		} else if (la.kind == Token.ident || la.kind == Token.not_ ||
                la.kind == Token.true_ || la.kind == Token.false_) {
            if(la.kind == Token.not_) Get();
            if(la.kind == Token.ident) {
                Obj obj = Designator();
                if(!obj.initialized)
                    SemErr("variable \"" + obj.name + "\" not initialized");
                Operand op = new Operand(obj);
                if(op.type != null)
                    type = op.type.kind;
                Builder.load(op);
            }
            else if(la.kind == Token.true_ || la.kind == Token.false_) {
                Operand op;
                if(la.kind == Token.true_)
                    op = new Operand(true);
                else
                    op = new Operand(false);
                Get();
                Builder.load(op);
            }
		} else if (la.kind == Token.lpar) {
			Get();
			type = Exp();
			Expect(Token.rpar);
		} else if (la.kind == Token.charVal) {
            type = Struct.char_;
            Get();
            Builder.addCons(t.val);
            Builder.loadIns(t.val);
        }  else if (la.kind == Token.stringVal) {
            type = Struct.string_;
            Get();
            Builder.addCons(t.val);
            Builder.loadIns(t.val);
        }
        else SynErr(83);

        return type;
	}



	public void Parse() {
        init();
		la = new Token();
		la.val = "";		
		Get();
		Coaple();
		Expect(0);

	}

    void init() {
        //init "start of" bitsets
        BitSet s;
        s = new BitSet(64); startOfExp = s;
        s.set(Token.number); s.set(Token.not_); s.set(Token.ident); s.set(Token.lpar); s.set(Token.charVal); s.set(Token.stringVal); s.set(Token.true_); s.set(Token.false_);

        //for the error synchronization, those are the token for the recovery
        s = new BitSet(64); syncStat = s;
        s.set(Token.type_); s.set(Token.caste_); s.set(Token.init_); s.set(Token.body_); s.set(Token.observes_);
        s.set(Token.var_); s.set(Token.state_); s.set(Token.action_); s.set(Token.affect_); s.set(Token.begin_); s.set(Token.join_); s.set(Token.quit_); s.set(Token.suspend_); s.set(Token.resume_);
        s.set(Token.create_); s.set(Token.destroy_); s.set(Token.loop_); s.set(Token.while_); s.set(Token.do_); s.set(Token.repeat_); s.set(Token.until_); s.set(Token.for_); s.set(Token.to_); s.set(Token.by_);
        s.set(Token.forall_); s.set(Token.if_); s.set(Token.then_); s.set(Token.elseif_); s.set(Token.else_); s.set(Token.case_); s.set(Token.with_); s.set(Token.when_); s.set(Token.exist_);
    }

    //for StartOf, created by Coco/R
    //used only for de StartOf of the default grammar, further implementation use bitsets
	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x},
		{x,x,x,x, x,x,T,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x},
		{x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,T,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x},
		{x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,T,T,T, T,T,T,T, T,x,T,x, T,x,x,T, T,x,x,x, T,x,T,T, x,x,x,x, x,x,x,x, x,x,x,x}

	};
} // end Parser


class Errors {
	public static int count = 0;                                    // number of errors detected
    public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
    public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
        errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "number expected"; break;
			case 3: s = "url expected"; break;
			case 4: s = "\"type\" expected"; break;
			case 6: s = "\"integer\" expected"; break;
			case 7: s = "\"real\" expected"; break;
			case 8: s = "\"bool\" expected"; break;
			case 9: s = "\"char\" expected"; break;
			case 10: s = "\"string\" expected"; break;
			case 11: s = "\"record\" expected"; break;
			case 12: s = "\"of\" expected"; break;
			case 15: s = "\"end\" expected"; break;
			case 16: s = "\"list\" expected"; break;
			case 17: s = "\"enumerate\" expected"; break;
			case 19: s = "\"caste\" expected"; break;
			case 20: s = "\"init\" expected"; break;
			case 21: s = "\"body\" expected"; break;
			case 22: s = "\"inherits\" expected"; break;
			case 23: s = "\"observes\" expected"; break;
			case 24: s = "\"all\" expected"; break;
			case 25: s = "\"in\" expected"; break;
			case 26: s = "\"var\" expected"; break;
			case 28: s = "\"set\" expected"; break;
			case 31: s = "\"state\" expected"; break;
			case 32: s = "\"action\" expected"; break;
			case 35: s = "\"affect\" expected"; break;
			case 36: s = "\"begin\" expected"; break;
			case 37: s = "\"join\" expected"; break;
			case 38: s = "\"quit\" expected"; break;
			case 39: s = "\"suspend\" expected"; break;
			case 40: s = "\"resume\" expected"; break;
			case 41: s = "\"create\" expected"; break;
			case 42: s = "\"destroy\" expected"; break;
			case 43: s = "\"loop\" expected"; break;
			case 44: s = "\"while\" expected"; break;
			case 45: s = "\"do\" expected"; break;
			case 46: s = "\"repeat\" expected"; break;
			case 47: s = "\"until\" expected"; break;
			case 48: s = "\"for\" expected"; break;
			case 49: s = "\"to\" expected"; break;
			case 50: s = "\"by\" expected"; break;
			case 51: s = "\"forall\" expected"; break;
			case 52: s = "\"if\" expected"; break;
			case 53: s = "\"then\" expected"; break;
			case 54: s = "\"elseif\" expected"; break;
			case 55: s = "\"else\" expected"; break;
			case 56: s = "\"case\" expected"; break;
			case 58: s = "\"with\" expected"; break;
			case 59: s = "\"when\" expected"; break;
			case 60: s = "\"exist\" expected"; break;
			case 61: s = "\"and\" expected"; break;
			case 62: s = "\"or\" expected"; break;
			case 63: s = "\"xor\" expected"; break;
			case 70: s = "??? expected"; break;
			case 71: s = "invalid StructureType"; break;
			case 72: s = "invalid TypeExp"; break;
			case 73: s = "invalid PrimitiveType"; break;
			case 74: s = "invalid Statement"; break;
			case 75: s = "invalid Statement"; break;
			case 76: s = "invalid EnvDec"; break;
			case 77: s = "invalid CasteEvent"; break;
			case 78: s = "invalid AgentEvent"; break;
			case 79: s = "invalid LoopStatement"; break;
			case 80: s = "invalid Scenario"; break;
			case 81: s = "invalid Addop"; break;
			case 82: s = "invalid Mulop"; break;
			case 83: s = "invalid Factor"; break;
            case 100: s = "\"+\" expected"; break;
            case 101: s = "\"-\" expected"; break;
            case 102: s = "\"*\" expected"; break;
            case 103: s = "\"/\" expected"; break;
            case 104: s = "\"%\" expected"; break;
            case 105: s = "\"=\" expected"; break;
            case 106: s = "\"!=\" expected"; break;
            case 107: s = "\"<\" expected"; break;
            case 108: s = "\"<=\" expected"; break;
            case 109: s = "\">\" expected"; break;
            case 110: s = "\">=\" expected"; break;
            case 111: s = "\":=\" expected"; break;
            case 112: s = "\";\" expected"; break;
            case 113: s = "\":\" expected"; break;
            case 114: s = "\",\" expected"; break;
            case 115: s = "\".\" expected"; break;
            case 116: s = "\"(\" expected"; break;
            case 117: s = "\")\" expected"; break;
            case 118: s = "\"[\" expected"; break;
            case 119: s = "\"]\" expected"; break;
            case 120: s = "\"{\" expected"; break;
            case 121: s = "\"}\" expected"; break;
            case 123: s = "\"->\" expected"; break;
            case 124: s = "\"\'\" expected"; break;
            case 125: s = "\"\"\" expected"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);

		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
