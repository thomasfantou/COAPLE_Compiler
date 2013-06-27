
package co.uk.brookes;

import co.uk.brookes.co.uk.brookes.symboltable.Obj;
import co.uk.brookes.co.uk.brookes.symboltable.STab;
import co.uk.brookes.co.uk.brookes.symboltable.Struct;

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

    private static final int  // token and char codes
        none        = 0,
        ident       = 1,
        letter      = 201,
        number      = 2,
        digit       = 202,
        atsign      = 3,
        charVal     = 203,
        stringVal   = 204,
        type_		= 4,
        integer_	= 6,
        real_	    = 7,
        bool_		= 8,
        char_		= 9,
        string_		= 10,
        record_		= 11,
        of_			= 12,
        end_		= 15,
        list_		= 16,
        enumerate_	= 17,
        caste_		= 19,
        init_		= 20,
        body_		= 21,
        inherits_	= 22,
        observes_	= 23,
        all_		= 24,
        in_			= 25,
        var_		= 26,
        set_		= 28,
        state_		= 31,
        action_		= 32,
        affect_		= 35,
        begin_		= 36,
        join_		= 37,
        quit_		= 38,
        suspend_	= 39,
        resume_		= 40,
        create_		= 41,
        destroy_	= 42,
        loop_		= 43,
        while_		= 44,
        do_			= 45,
        repeat_		= 46,
        until_		= 47,
        for_		= 48,
        to_			= 49,
        by_			= 50,
        forall_		= 51,
        if_			= 52,
        then_		= 53,
        elseif_		= 54,
        else_		= 55,
        case_		= 56,
        with_		= 58,
        when_		= 59,
        exist_		= 60,
        and_		= 61,
        or_			= 62,
        xor_		= 63,
        not_        = 64,
        plus        = 100,
        minus       = 101,
        times       = 102,
        slash       = 103,
        rem         = 104,
        eqlSign     = 105,
        neq         = 106,
        lss         = 107,
        leq         = 108,
        gtr         = 109,
        geq         = 110,
        assign      = 111,
        semicolon   = 112,
        colon       = 113,
        comma       = 114,
        period      = 115,
        lpar        = 116,
        rpar        = 117,
        lbrack      = 118,
        rbrack      = 119,
        lbrace      = 120,
        rbrace      = 121,
        aslash      = 122,
        effect      = 123,
        apostr      = 124,
        quote       = 125,
        excl        = 126,
        concat      = 127;
	

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
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
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
		while (la.kind == type_) {
			TypeDec();
		}
		while (la.kind == caste_) {
			CasteDec();
		}
	}

    //TypeDec =
    //  "type" StructureType {StructureType} "end".
	void TypeDec() {
		Expect(type_);
		StructureType();
		while (la.kind == record_ || la.kind == list_ || la.kind == enumerate_) {
			StructureType();
		}
        Expect(end_);
	}


    //CasteDec =
    //    "caste" ident [Inheritances] ";"
    //    [EnvironmentDecs]
    //    [StateDecs]
    //    [ActionDecs]
    //    "init" [Statement]
    //"body"
    //    [Decs]
    //    [ActionDecs]
    //    [Statement]
    //"end" ident.
	void CasteDec() {
		Expect(caste_);
		Expect(ident);
        Object obj = STab.insert(Obj.type_, t.val, new Struct(Struct.caste_));
		if (la.kind == inherits_) {
			Inheritances();
		}
		Expect(semicolon);
        STab.openScope();
        if (la.kind == observes_) {
			EnvironmentDecs();
		}
		if (la.kind == state_) {
			StateDecs();
		}
		if (la.kind == action_) {
			ActionDecs();
		}
		Expect(init_);
        if (StartOf(3)) {
            Statement();
        }
		Expect(body_);
		if (la.kind == var_) {
			Decs();
		}
		if (la.kind == action_) {
			ActionDecs();
		}
        if (StartOf(3)) {
            Statement();
        }
		Expect(end_);
		Expect(ident);
        STab.closeScope();
	}

    //StructureType =
    //    RecordType
    //    | ListType
    //    | EnumeratedType.
	Struct StructureType() {
        Struct type = null;
		if (la.kind == record_) {
			RecordType();
            type = new Struct(Struct.record_);
		} else if (la.kind == list_) {
			ListType();
            type = new Struct(Struct.list_);
		} else if (la.kind == enumerate_) {
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
		} else if (la.kind == record_ || la.kind == list_ || la.kind == enumerate_) {
            type = StructureType();
		} else if (la.kind == ident) {
			ID();
            //TODO get type of ID
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
		if (la.kind == integer_) {
			Get();
            type = new Struct(Struct.integer_);
		} else if (la.kind == real_) {
			Get();
            type = new Struct(Struct.real_);
		} else if (la.kind == bool_) {
			Get();
            type = new Struct(Struct.bool_);
		} else if (la.kind == char_) {
			Get();
            type = new Struct(Struct.char_);
		} else if (la.kind == string_) {
			Get();
            type = new Struct(Struct.string_);
		} else SynErr(73);

        return type;
	}

    //ID =
    //  ident [url].
	String ID() {
		Expect(ident);
        String val = t.val;
		if (la.kind == atsign) {
			Get();
		}
        return val;
	}

    //RecordType =
    //    "record" ident "of" { ident {"," ident} ":" TypeExp ";" }
    //    "end".
	void RecordType() {
		Expect(record_);
		Expect(ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.record_));
		Expect(of_);
        STab.openScope();
        while(la.kind == ident) {
            ArrayList<String> vars = new ArrayList<String>();
            Get();
            vars.add(t.val);
            while (la.kind == comma) {
                Get();
                Expect(ident);
                vars.add(t.val);
            }
            Expect(colon);
            Struct type = TypeExp();
            for(String v : vars)
                STab.insert(Obj.typeField_, v, type);
            Expect(semicolon);
        }
		Expect(end_);
        STab.closeScope();
	}

    //ListType =
    //  "list" ident "of" TypeExp "end".
	void ListType() {
		Expect(list_);
		Expect(ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.list_));
		Expect(of_);
        STab.openScope();
		Struct type = TypeExp();
        obj.type.listType = type;
		Expect(end_);
        STab.closeScope();
	}

    //EnumeratedType =
    //  "enumerate" ident "=" IDList "end".
	void EnumeratedType() {
		Expect(enumerate_);
		Expect(ident);
        Obj obj = STab.insert(Obj.type_, t.val, new Struct(Struct.enum_));
		Expect(eqlSign);
        STab.openScope();
		ArrayList<String> ids = IDList();
        for(int i = 0; i < ids.size(); i++)
            STab.insert(Obj.typeField_, ids.get(i), new Struct(Struct.integer_));
		Expect(end_);
        STab.closeScope();
	}

    //IDList =
    //  ident {"," ident}.
	ArrayList<String> IDList() {
        ArrayList<String> ids = new ArrayList<String>();
		Expect(ident);
        ids.add(t.val);
		while (la.kind == comma) {
			Get();
			Expect(ident);
            ids.add(t.val);
		}
        return ids;
	}

    //Inheritances =
    //  "inherits" ident {"," ident}.
	void Inheritances() { //TODO
		Expect(inherits_);
		Expect(ident);
		while (la.kind == comma) {
			Get();
			Expect(ident);
		}
	}

    //EnvironmentDecs =
    //    "observes" {EnvDec ";"}.
	void EnvironmentDecs() {
		Expect(observes_);
		while (StartOf(2)) {
			EnvDec();
			Expect(semicolon);
		}
	}

    //StateDecs =
    //    "state" Decs.
	void StateDecs() {
		Expect(state_);
		Decs();
	}

    //ActionDecs =
    //    ActionDec ";" {ActionDec ";"}.
	void ActionDecs() {
		ActionDec();
		Expect(semicolon);
		while (la.kind == action_) {
			ActionDec();
			Expect(semicolon);
		}
	}

    //Statement =
    //    Designator (":=" Exp | "(" [ParameterList] ")") ";"
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
		switch (la.kind) {
		case ident: {
			Designator();
			if (la.kind == assign) {
				Get();
				Exp();
			} else if (la.kind == lpar) {
				Get();
				if (la.kind == ident) {
					ParameterList();
				}
				Expect(rpar);
			} else SynErr(74);
            Expect(semicolon);
			break;
		}
		case join_: case quit_: case suspend_: case resume_: {
			CasteEvent();
            Expect(semicolon);
			break;
		}
		case create_: case destroy_: {
			AgentEvent();
            Expect(semicolon);
			break;
		}
		case begin_: {
			Block();
			break;
		}
		case loop_: case while_: case repeat_: case for_: {
			LoopStatement();
			break;
		}
		case if_: {
			IfStatement();
			break;
		}
		case case_: {
			CaseStatement();
			break;
		}
		case with_: {
			WithStatement();
			break;
		}
		case forall_: {
			ForAllStatement();
			break;
		}
		case when_: {
			WhenStatement();
			break;
		}
		default: SynErr(75); break;
		}
	}

    //Block =
    //  "begin" { Statement } "end".
    void Block() {
        Expect(begin_);
        STab.openScope();
        while(StartOf(3)) {
            Statement();
        }
        Expect(end_);
        STab.closeScope();
    }

    //Decs =
    //    Dec ";" {Dec ";"}.
	void Decs() {
		Dec();
		Expect(semicolon);
		while (la.kind == var_) {
			Dec();
			Expect(semicolon);
		}
	}

    //EnvDec =
    //    ident
    //    | "all" "in" ID
    //    | "var" ident "in" ID [":=" AgentID]
    //    | "set" ident "in" ID [":=" AgentSetEnum].
	void EnvDec() { //TODO when ID struct type is implemented
		if (la.kind == ident) {
			Get();
		} else if (la.kind == all_) {
			Get();
			Expect(in_);
			ID();
		} else if (la.kind == var_) {
			Get();
            Expect(ident);
			Expect(in_);
			ID();
			if (la.kind == assign) {
				Get();
				AgentID();
			}
		} else if (la.kind == set_) {
			Get();
            Expect(ident);
			Expect(in_);
			ID();
			if (la.kind == assign) {
				Get();
				AgentSetEnum();
			}
		} else SynErr(76);
	}

    //AgentID	=
    //  ident "in" ID.
	void AgentID() { //TODO struct = agent ?
		Expect(ident);
		Expect(25);
		ID();
	}

    //AgentSetEnum =
    //  "{"
    //    ident {"," ident}
    //  "}".
	void AgentSetEnum() {
		Expect(lbrace);
		Expect(ident);
		while (la.kind == comma) {
			Get();
			Expect(ident);
		}
		Expect(rbrace);
	}

    //Dec =
    //  "var" IDList ":" TypeExp [":=" ident].
	void Dec() {
		Expect(var_);
		ArrayList<String> ids = IDList();
		Expect(colon);
		Struct type = TypeExp();
        for(String id : ids)
            STab.insert(Obj.var_, id, type);
		if (la.kind == assign) {
			Get();
			Expect(ident);
		}
	}

    //ActionDec =
    //"action" IDList "("[ParameterList]")"
    //    [Statement]
    //    [Impact].
	void ActionDec() {
		Expect(action_);
        ArrayList<String> ids = IDList();
        Obj[] objs = new Obj[ids.size()];
        for(int i = 0; i < ids.size(); i++) //because IDList
            objs[i] = STab.insert(Obj.action_, ids.get(i), new Struct(Struct.none_));
		Expect(lpar);
        STab.openScope();
		if (la.kind == ident) {
			ParameterList();
		}
		Expect(rpar);
		if (StartOf(3)) {
			Statement();
		}
        for(int i = 0; i < objs.length; i++)
            objs[i].locals = STab.curScope.locals;
		if (la.kind == affect_) {
			Impact();
		}
        STab.closeScope();
	}

    //ParameterList =
    //  Parameter {"," Parameter}.
	int ParameterList() {
        int nParam = 0;
		Parameter();
        nParam++;
		while (la.kind == comma) {
			Get();
			Parameter();
            nParam++;
		}
        return nParam;
	}

    //Impact =
    //  "affect" ID { "," ID }.
	void Impact() {
		Expect(affect_);
		ID();
		while (la.kind == comma) {
			Get();
			ID();
		}
	}

    //Parameter =
    //  ident {"," ident} ":" TypeExp.
	void Parameter() {
        ArrayList<String> ids = new ArrayList<String>();
		Expect(ident);
        ids.add(t.val);
		while (la.kind == comma) {
			Get();
			Expect(ident);
            ids.add(t.val);
		}
		Expect(colon);
		Struct type = TypeExp();
        for(String id : ids)
            STab.insert(Obj.var_, id, type);
	}

    //Designator =
    //  ident { "." ident } ["[" Exp "]"].<
	void Designator() {
		Expect(ident);
		while (la.kind == period) {
			Get();
			Expect(ident);
		}
        if (la.kind == lbrack) {
            Get();
            Exp();
            Expect(rbrack);
        }
	}

    //Exp =
    //    Term {Addop Term}.
	void Exp() {
		Term();
		while (la.kind == plus || la.kind == minus || la.kind == concat) {
			Addop();
			Term();
		}
	}

    //Conditions =
    //  Condition {Boolop Condition}.
    void Conditions() {
        Condition();
        while(la.kind == and_ || la.kind == or_) {
            Boolop();
            Condition();
        }
    }

    //Condition =
    //  Exp [Relop Exp].
    void Condition() {
        Exp();
        if(la.kind == neq || la.kind == gtr || la.kind == geq
                || la.kind == lss || la.kind == leq || la.kind == eqlSign) {
            Relop();
            Exp();
        }
    }

    //CasteEvent =
    //  "join" ID "("[ident]")"
    //  | "quit" ID
    //  | "suspend" ID
    //  | "resume" ID.
	void CasteEvent() {
		if (la.kind == join_) {
			Get();
			ID();
			Expect(lpar);
			if (la.kind == ident) {
				Get();
			}
			Expect(rpar);
		} else if (la.kind == quit_) {
			Get();
			ID();
		} else if (la.kind == suspend_) {
			Get();
			ID();
		} else if (la.kind == resume_) {
			Get();
			ID();
		} else SynErr(77);
	}

    //AgentEvent =
    //  "create" ident "of" ID "("[ident]")" [url]
    //  | "destroy" ident.
	void AgentEvent() { //TODO is it like "new object()"
		if (la.kind == create_) {
			Get();
			Expect(ident);
			Expect(of_);
			ID();
			Expect(lpar);
			if (la.kind == ident) {
				Get();
			}
			Expect(rpar);
			if (la.kind == atsign) {
				Get();
			}
		} else if (la.kind == destroy_) {
			Get();
			Expect(ident);
		} else SynErr(78);
	}

    //LoopStatement =
    //  ForLoop
    //  | WhileLoop
    //  | RepeatLoop
    //  | Loop.
	void LoopStatement() {
		if (la.kind == for_) {
			ForLoop();
		} else if (la.kind == while_) {
			WhileLoop();
		} else if (la.kind == repeat_) {
			RepeatLoop();
		} else if (la.kind == loop_) {
			Loop();
		} else SynErr(79);
	}

    //IfStatement =
    //    "if" Conditions "then" Statement
    //    {"elseif" Conditions "then" Statement}
    //    ["else" Statement]
    //    "end".
	void IfStatement() {
		Expect(if_);
        Conditions();
		Expect(then_);
		Statement();
		while (la.kind == elseif_) {
			Get();
            Conditions();
			Expect(then_);
			Statement();
		}
		if (la.kind == else_) {
			Get();
			Statement();
		}
		Expect(end_);
	}

    //CaseStatement =
    //  "case" Exp "of"
    //  {Exp "->" Statement}
    //  ["else" Statement]
    //  "end".
	void CaseStatement() {
		Expect(case_);
		Exp();
		Expect(of_);
		while (startOfExp.get(la.kind)) {
			Exp();
			Expect(effect);
			Statement();
		}
		if (la.kind == else_) {
			Get();
			Statement();
		}
		Expect(end_);
	}

    //WithStatement =
    //  "with" Exp "do" Statement "end".
	void WithStatement() {
		Expect(with_);
		Exp();
		Expect(do_);
		Statement();
		Expect(end_);
	}

    //ForAllStatement =
    //  "forall" ident "in" Exp "do"
    //  Statement
    //  "end".
	void ForAllStatement() {
		Expect(forall_);
		Expect(ident);
		Expect(in_);
		Exp();
		Expect(do_);
		Statement();
		Expect(end_);
	}

    //WhenStatement =
    //  "when" {Scenario "->" Statement} "end".
	void WhenStatement() {
		Expect(when_);
		while (la.kind == ident || la.kind == exist_ || la.kind == all_) {
			Scenario();
			Expect(effect);
			Statement();
		}
		Expect(end_);
	}

    //ForLoop =
    //  "for" ident ":=" Exp "to" Exp ["by" Exp] "do"
    //  Statement
    //  "end".
	void ForLoop() {
		Expect(for_);
		Expect(ident);
		Expect(assign);
		Exp();
		Expect(to_);
		Exp();
		if (la.kind == by_) {
			Get();
			Exp();
		}
		Expect(do_);
		Statement();
		Expect(end_);
	}

    //WhileLoop =
    //  "while" Conditions "do" Statement "end".
	void WhileLoop() {
		Expect(while_);
        Conditions();
		Expect(do_);
		Statement();
		Expect(end_);
	}

    //RepeatLoop =
    //  "repeat" Statement "until" Conditions "end".
	void RepeatLoop() {
		Expect(repeat_);
		Statement();
		Expect(until_);
        Conditions();
		Expect(end_);
	}

    //Loop =
    //  "loop" Statement "end".
	void Loop() {
		Expect(loop_);
		Statement();
		Expect(end_);
	}

    //Scenario =
    //  (ident ["in" ID] ":" ActionPattern
    //  | "exist" ident "in" ID ":" ActionPattern
    //  | "all" ident "in" ID ":" ActionPattern)
    //  [("and"|"or"|"xor") Scenario].
	void Scenario() {
		if (la.kind == ident) {
			Get();
			if (la.kind == in_) {
				Get();
				ID();
			}
			Expect(colon);
			ActionPattern();
		} else if (la.kind == exist_) {
			Get();
			Expect(ident);
            Obj obj = STab.insert(Obj.var_, t.val, new Struct(Struct.none_)); //TODO set right type
            Expect(in_);
			ID();
			Expect(colon);
			ActionPattern();
		} else if (la.kind == 24) {
			Get();
			Expect(ident);
            Obj obj = STab.insert(Obj.var_, t.val, new Struct(Struct.none_)); //TODO set right type
            Expect(in_);
            ID();
            Expect(colon);
			ActionPattern();
		} else SynErr(80);
		if (la.kind == and_ || la.kind == or_ || la.kind == xor_) {
			if (la.kind == and_) {
				Get();
			} else if (la.kind == or_) {
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
		Expect(ident);
		Expect(lpar);
		if (la.kind == ident || la.kind == number || la.kind == lpar) {
			PatternPara();
			while (la.kind == comma) {
				Get();
				PatternPara();
			}
		}
		Expect(rpar);
	}

    //PatternPara =
    //  Exp.
	void PatternPara() {
		Exp();
	}

    //Term =
    //  Factor {Mulop Factor}.
	void Term() {
		Factor();
		while (la.kind == times || la.kind == slash || la.kind == rem) {
			Mulop();
			Factor();
		}
	}

    //Addop =
    //  "+" | "-" | "++".
	void Addop() {
		if (la.kind == plus) {
			Get();
		} else if (la.kind == minus) {
			Get();
		}
        else if (la.kind == concat) {
            Get();
        } else SynErr(81);
	}

    //Mulop =
    //  "*" | "/" | "%".
	void Mulop() {
		if (la.kind == times) {
			Get();
		} else if (la.kind == slash) {
			Get();
		} else if (la.kind == rem) {
			Get();
		} else SynErr(82);
	}

    //Relop =
    //  "=" | "!=" | ">" | ">=" | "<" | "<=".
    void Relop() {
        if(la.kind == neq || la.kind == gtr || la.kind == geq
                || la.kind == lss || la.kind == leq || la.kind == eqlSign)
            Get();
    }

    //Boolop =
    //  "and" | "or".
    void Boolop() {
        if(la.kind == and_ || la.kind == or_)
            Get();
    }

    //Factor =
    //  number
    //  | ["not"] Designator
    //  | "(" Exp ")"
    //  | charVal
    //  | stringVal.
	void Factor() {
		if (la.kind == number) {
			Get();
		} else if (la.kind == ident || la.kind == not_) {
            if(la.kind == not_) Get();
			Designator();
		} else if (la.kind == lpar) {
			Get();
			Exp();
			Expect(rpar);
		} else if (la.kind == charVal) {
            Get();
        }  else if (la.kind == stringVal) {
            Get();
        }

        else SynErr(83);
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
        s.set(number); s.set(not_); s.set(ident); s.set(lpar); s.set(charVal); s.set(stringVal);
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
	public int count = 0;                                    // number of errors detected
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
