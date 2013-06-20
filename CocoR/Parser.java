
package co.uk.brookes;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _url = 3;
	public static final int _charVal = 4;
	public static final int _stringVal = 5;
	public static final int maxT = 81;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
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
	
	void Coaple() {
		Prog();
	}

	void Prog() {
		while (la.kind == 6) {
			TypeDec();
		}
		while (la.kind == 21) {
			CasteDec();
		}
	}

	void TypeDec() {
		Expect(6);
		StructureType();
		while (la.kind == 7) {
			Get();
			TypeExp();
		}
	}

	void CasteDec() {
		Expect(21);
		Expect(1);
		if (la.kind == 24) {
			Inheritances();
		}
		Expect(16);
		if (la.kind == 25) {
			EnvironmentDecs();
		}
		if (la.kind == 33) {
			StateDecs();
		}
		if (la.kind == 34) {
			ActionDecs();
		}
		Expect(22);
		Statement();
		Expect(23);
		if (la.kind == 28) {
			Decs();
		}
		if (la.kind == 34) {
			ActionDecs();
		}
		Statement();
		Expect(17);
		Expect(1);
	}

	void StructureType() {
		if (la.kind == 13) {
			RecordType();
		} else if (la.kind == 18) {
			ListType();
		} else if (la.kind == 19) {
			EnumeratedType();
		} else SynErr(82);
	}

	void TypeExp() {
		if (StartOf(1)) {
			PrimitiveType();
		} else if (la.kind == 13 || la.kind == 18 || la.kind == 19) {
			StructureType();
		} else if (la.kind == 1) {
			ID();
		} else SynErr(83);
	}

	void PrimitiveType() {
		if (la.kind == 8) {
			Get();
		} else if (la.kind == 9) {
			Get();
		} else if (la.kind == 10) {
			Get();
		} else if (la.kind == 11) {
			Get();
		} else if (la.kind == 12) {
			Get();
		} else SynErr(84);
	}

	void ID() {
		Expect(1);
		if (la.kind == 3) {
			Get();
		}
	}

	void RecordType() {
		Expect(13);
		Expect(1);
		Expect(14);
		Expect(1);
		while (la.kind == 7) {
			Get();
			Expect(1);
		}
		Expect(15);
		TypeExp();
		Expect(16);
		Expect(17);
	}

	void ListType() {
		Expect(18);
		Expect(1);
		Expect(14);
		TypeExp();
		Expect(17);
	}

	void EnumeratedType() {
		Expect(19);
		Expect(1);
		Expect(20);
		IDList();
		Expect(17);
	}

	void IDList() {
		Expect(1);
		while (la.kind == 7) {
			Get();
			Expect(1);
		}
	}

	void Inheritances() {
		Expect(24);
		Expect(1);
		while (la.kind == 7) {
			Get();
			Expect(1);
		}
	}

	void EnvironmentDecs() {
		Expect(25);
		while (StartOf(2)) {
			EnvDec();
			Expect(16);
		}
	}

	void StateDecs() {
		Expect(33);
		Decs();
	}

	void ActionDecs() {
		ActionDec();
		Expect(16);
		while (la.kind == 34) {
			ActionDec();
			Expect(16);
		}
	}

	void Statement() {
		switch (la.kind) {
		case 1: {
			Designator();
			if (la.kind == 29) {
				Get();
				Exp();
			} else if (la.kind == 35) {
				Get();
				if (la.kind == 1) {
					ParameterList();
				}
				Expect(36);
			} else SynErr(85);
			Expect(16);
			break;
		}
		case 39: case 40: case 41: case 42: {
			CasteEvent();
			Expect(16);
			break;
		}
		case 43: case 44: {
			AgentEvent();
			Expect(16);
			break;
		}
		case 38: {
			Block();
			break;
		}
		case 45: case 46: case 48: case 50: {
			LoopStatement();
			break;
		}
		case 54: {
			IfStatement();
			break;
		}
		case 58: {
			CaseStatement();
			break;
		}
		case 60: {
			WithStatement();
			break;
		}
		case 53: {
			ForAllStatement();
			break;
		}
		case 61: {
			WhenStatement();
			break;
		}
		default: SynErr(86); break;
		}
	}

	void Decs() {
		Dec();
		Expect(16);
		while (la.kind == 28) {
			Dec();
			Expect(16);
		}
	}

	void EnvDec() {
		if (la.kind == 1) {
			Get();
		} else if (la.kind == 26) {
			Get();
			Expect(1);
			Expect(27);
			ID();
		} else if (la.kind == 28) {
			Get();
			IDList();
			Expect(27);
			ID();
			if (la.kind == 29) {
				Get();
				AgentID();
			}
		} else if (la.kind == 30) {
			Get();
			IDList();
			Expect(27);
			ID();
			if (la.kind == 29) {
				Get();
				AgentSetEnum();
			}
		} else SynErr(87);
	}

	void AgentID() {
		Expect(1);
		Expect(27);
		ID();
	}

	void AgentSetEnum() {
		Expect(31);
		Expect(1);
		while (la.kind == 7) {
			Get();
			Expect(1);
		}
		Expect(32);
	}

	void Dec() {
		Expect(28);
		IDList();
		Expect(15);
		TypeExp();
		if (la.kind == 29) {
			Get();
			Expect(1);
		}
	}

	void ActionDec() {
		Expect(34);
		IDList();
		Expect(35);
		if (la.kind == 1) {
			ParameterList();
		}
		Expect(36);
		if (StartOf(3)) {
			Statement();
		}
		if (la.kind == 37) {
			Impact();
		}
	}

	void ParameterList() {
		Parameter();
		while (la.kind == 7) {
			Get();
			Parameter();
		}
	}

	void Impact() {
		Expect(37);
		ID();
		while (la.kind == 7) {
			Get();
			ID();
		}
	}

	void Parameter() {
		Expect(1);
		while (la.kind == 7) {
			Get();
			Expect(1);
		}
		Expect(15);
		ID();
	}

	void Designator() {
		Expect(1);
		while (la.kind == 78) {
			Get();
			Expect(1);
		}
		if (la.kind == 79) {
			Get();
			Exp();
			Expect(80);
		}
	}

	void Exp() {
		Term();
		while (la.kind == 66 || la.kind == 67 || la.kind == 68) {
			Addop();
			Term();
		}
	}

	void CasteEvent() {
		if (la.kind == 39) {
			Get();
			ID();
			Expect(35);
			if (la.kind == 1) {
				Get();
			}
			Expect(36);
		} else if (la.kind == 40) {
			Get();
			ID();
		} else if (la.kind == 41) {
			Get();
			ID();
		} else if (la.kind == 42) {
			Get();
			ID();
		} else SynErr(88);
	}

	void AgentEvent() {
		if (la.kind == 43) {
			Get();
			Expect(1);
			Expect(14);
			ID();
			Expect(35);
			if (la.kind == 1) {
				Get();
			}
			Expect(36);
			if (la.kind == 3) {
				Get();
			}
		} else if (la.kind == 44) {
			Get();
			Expect(1);
		} else SynErr(89);
	}

	void Block() {
		Expect(38);
		while (StartOf(3)) {
			Statement();
		}
		Expect(17);
	}

	void LoopStatement() {
		if (la.kind == 50) {
			ForLoop();
		} else if (la.kind == 46) {
			WhileLoop();
		} else if (la.kind == 48) {
			RepeatLoop();
		} else if (la.kind == 45) {
			Loop();
		} else SynErr(90);
	}

	void IfStatement() {
		Expect(54);
		Conditions();
		Expect(55);
		Statement();
		while (la.kind == 56) {
			Get();
			Conditions();
			Expect(55);
			Statement();
		}
		if (la.kind == 57) {
			Get();
			Statement();
		}
		Expect(17);
	}

	void CaseStatement() {
		Expect(58);
		Exp();
		Expect(14);
		while (StartOf(4)) {
			Exp();
			Expect(59);
			Statement();
		}
		if (la.kind == 57) {
			Get();
			Statement();
		}
		Expect(17);
	}

	void WithStatement() {
		Expect(60);
		Exp();
		Expect(47);
		Statement();
		Expect(17);
	}

	void ForAllStatement() {
		Expect(53);
		Expect(1);
		Expect(27);
		Exp();
		Expect(47);
		Statement();
		Expect(17);
	}

	void WhenStatement() {
		Expect(61);
		while (la.kind == 1 || la.kind == 26 || la.kind == 62) {
			Scenario();
			Expect(59);
			Statement();
		}
		Expect(17);
	}

	void ForLoop() {
		Expect(50);
		Expect(1);
		Expect(29);
		Exp();
		Expect(51);
		Exp();
		if (la.kind == 52) {
			Get();
			Exp();
		}
		Expect(47);
		Statement();
		Expect(17);
	}

	void WhileLoop() {
		Expect(46);
		Conditions();
		Expect(47);
		Statement();
		Expect(17);
	}

	void RepeatLoop() {
		Expect(48);
		Statement();
		Expect(49);
		Conditions();
		Expect(17);
	}

	void Loop() {
		Expect(45);
		Statement();
		Expect(17);
	}

	void Conditions() {
		Condition();
		while (la.kind == 63 || la.kind == 64) {
			Boolop();
			Condition();
		}
	}

	void Scenario() {
		if (la.kind == 1) {
			Get();
			if (la.kind == 27) {
				Get();
				ID();
			}
			Expect(15);
			ActionPattern();
		} else if (la.kind == 62) {
			Get();
			Expect(1);
			Expect(27);
			ID();
			Expect(15);
			ActionPattern();
		} else if (la.kind == 26) {
			Get();
			Expect(1);
			Expect(27);
			ID();
			Expect(15);
			ActionPattern();
		} else SynErr(91);
		if (la.kind == 63 || la.kind == 64 || la.kind == 65) {
			if (la.kind == 63) {
				Get();
			} else if (la.kind == 64) {
				Get();
			} else {
				Get();
			}
			Scenario();
		}
	}

	void ActionPattern() {
		Expect(1);
		Expect(35);
		if (StartOf(4)) {
			PatternPara();
			while (la.kind == 7) {
				Get();
				PatternPara();
			}
		}
		Expect(36);
	}

	void PatternPara() {
		Exp();
	}

	void Term() {
		Factor();
		while (la.kind == 69 || la.kind == 70 || la.kind == 71) {
			Mulop();
			Factor();
		}
	}

	void Addop() {
		if (la.kind == 66) {
			Get();
		} else if (la.kind == 67) {
			Get();
		} else if (la.kind == 68) {
			Get();
		} else SynErr(92);
	}

	void Condition() {
		Exp();
		if (StartOf(5)) {
			Relop();
			Exp();
		}
	}

	void Boolop() {
		if (la.kind == 63) {
			Get();
		} else if (la.kind == 64) {
			Get();
		} else SynErr(93);
	}

	void Relop() {
		switch (la.kind) {
		case 20: {
			Get();
			break;
		}
		case 72: {
			Get();
			break;
		}
		case 73: {
			Get();
			break;
		}
		case 74: {
			Get();
			break;
		}
		case 75: {
			Get();
			break;
		}
		case 76: {
			Get();
			break;
		}
		default: SynErr(94); break;
		}
	}

	void Mulop() {
		if (la.kind == 69) {
			Get();
		} else if (la.kind == 70) {
			Get();
		} else if (la.kind == 71) {
			Get();
		} else SynErr(95);
	}

	void Factor() {
		if (la.kind == 2) {
			Get();
		} else if (la.kind == 1 || la.kind == 77) {
			if (la.kind == 77) {
				Get();
			}
			Designator();
		} else if (la.kind == 35) {
			Get();
			Exp();
			Expect(36);
		} else if (la.kind == 4) {
			Get();
		} else if (la.kind == 5) {
			Get();
		} else SynErr(96);
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		Coaple();
		Expect(0);

	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,x,x,x, x,x,x,x, T,T,T,T, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, T,x,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, T,T,T,T, T,T,T,x, T,x,T,x, x,T,T,x, x,x,T,x, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,T,x, T,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,T,T,T, T,x,x,x, x,x,x}

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
			case 4: s = "charVal expected"; break;
			case 5: s = "stringVal expected"; break;
			case 6: s = "\"type\" expected"; break;
			case 7: s = "\",\" expected"; break;
			case 8: s = "\"integer\" expected"; break;
			case 9: s = "\"real\" expected"; break;
			case 10: s = "\"bool\" expected"; break;
			case 11: s = "\"char\" expected"; break;
			case 12: s = "\"string\" expected"; break;
			case 13: s = "\"record\" expected"; break;
			case 14: s = "\"of\" expected"; break;
			case 15: s = "\":\" expected"; break;
			case 16: s = "\";\" expected"; break;
			case 17: s = "\"end\" expected"; break;
			case 18: s = "\"list\" expected"; break;
			case 19: s = "\"enumerate\" expected"; break;
			case 20: s = "\"=\" expected"; break;
			case 21: s = "\"caste\" expected"; break;
			case 22: s = "\"init\" expected"; break;
			case 23: s = "\"body\" expected"; break;
			case 24: s = "\"inherits\" expected"; break;
			case 25: s = "\"observes\" expected"; break;
			case 26: s = "\"all\" expected"; break;
			case 27: s = "\"in\" expected"; break;
			case 28: s = "\"var\" expected"; break;
			case 29: s = "\":=\" expected"; break;
			case 30: s = "\"set\" expected"; break;
			case 31: s = "\"{\" expected"; break;
			case 32: s = "\"}\" expected"; break;
			case 33: s = "\"state\" expected"; break;
			case 34: s = "\"action\" expected"; break;
			case 35: s = "\"(\" expected"; break;
			case 36: s = "\")\" expected"; break;
			case 37: s = "\"affect\" expected"; break;
			case 38: s = "\"begin\" expected"; break;
			case 39: s = "\"join\" expected"; break;
			case 40: s = "\"quit\" expected"; break;
			case 41: s = "\"suspend\" expected"; break;
			case 42: s = "\"resume\" expected"; break;
			case 43: s = "\"create\" expected"; break;
			case 44: s = "\"destroy\" expected"; break;
			case 45: s = "\"loop\" expected"; break;
			case 46: s = "\"while\" expected"; break;
			case 47: s = "\"do\" expected"; break;
			case 48: s = "\"repeat\" expected"; break;
			case 49: s = "\"until\" expected"; break;
			case 50: s = "\"for\" expected"; break;
			case 51: s = "\"to\" expected"; break;
			case 52: s = "\"by\" expected"; break;
			case 53: s = "\"forall\" expected"; break;
			case 54: s = "\"if\" expected"; break;
			case 55: s = "\"then\" expected"; break;
			case 56: s = "\"elseif\" expected"; break;
			case 57: s = "\"else\" expected"; break;
			case 58: s = "\"case\" expected"; break;
			case 59: s = "\"->\" expected"; break;
			case 60: s = "\"with\" expected"; break;
			case 61: s = "\"when\" expected"; break;
			case 62: s = "\"exist\" expected"; break;
			case 63: s = "\"and\" expected"; break;
			case 64: s = "\"or\" expected"; break;
			case 65: s = "\"xor\" expected"; break;
			case 66: s = "\"+\" expected"; break;
			case 67: s = "\"-\" expected"; break;
			case 68: s = "\"++\" expected"; break;
			case 69: s = "\"*\" expected"; break;
			case 70: s = "\"/\" expected"; break;
			case 71: s = "\"%\" expected"; break;
			case 72: s = "\"!=\" expected"; break;
			case 73: s = "\">\" expected"; break;
			case 74: s = "\">=\" expected"; break;
			case 75: s = "\"<\" expected"; break;
			case 76: s = "\"<=\" expected"; break;
			case 77: s = "\"not\" expected"; break;
			case 78: s = "\".\" expected"; break;
			case 79: s = "\"[\" expected"; break;
			case 80: s = "\"]\" expected"; break;
			case 81: s = "??? expected"; break;
			case 82: s = "invalid StructureType"; break;
			case 83: s = "invalid TypeExp"; break;
			case 84: s = "invalid PrimitiveType"; break;
			case 85: s = "invalid Statement"; break;
			case 86: s = "invalid Statement"; break;
			case 87: s = "invalid EnvDec"; break;
			case 88: s = "invalid CasteEvent"; break;
			case 89: s = "invalid AgentEvent"; break;
			case 90: s = "invalid LoopStatement"; break;
			case 91: s = "invalid Scenario"; break;
			case 92: s = "invalid Addop"; break;
			case 93: s = "invalid Boolop"; break;
			case 94: s = "invalid Relop"; break;
			case 95: s = "invalid Mulop"; break;
			case 96: s = "invalid Factor"; break;
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
