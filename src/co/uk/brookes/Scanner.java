
package co.uk.brookes;

import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.HashMap;

class Token {
	public int kind;    // token kind
	public int pos;     // token position in bytes in the source text (starting at 0)
	public int charPos; // token position in characters in the source text (starting at 0)
	public int col;     // token column (starting at 1)
	public int line;    // token line (starting at 1)
	public String val;  // token value
	public Token next;  // ML 2005-03-11 Peek tokens are kept in linked list
}

//-----------------------------------------------------------------------------------
// Buffer
//-----------------------------------------------------------------------------------
class Buffer {
	// This Buffer supports the following cases:
	// 1) seekable stream (file)
	//    a) whole stream in buffer
	//    b) part of stream in buffer
	// 2) non seekable stream (network, console)

	public static final int EOF = Character.MAX_VALUE + 1;
	private static final int MIN_BUFFER_LENGTH = 1024; // 1KB
	private static final int MAX_BUFFER_LENGTH = MIN_BUFFER_LENGTH * 64; // 64KB
	private byte[] buf;   // input buffer
	private int bufStart; // position of first byte in buffer relative to input stream
	private int bufLen;   // length of buffer
	private int fileLen;  // length of input stream (may change if stream is no file)
	private int bufPos;      // current position in buffer
	private RandomAccessFile file; // input stream (seekable)
	private InputStream stream; // growing input stream (e.g.: console, network)

	public Buffer(InputStream s) {
		stream = s;
		fileLen = bufLen = bufStart = bufPos = 0;
		buf = new byte[MIN_BUFFER_LENGTH];
	}

	public Buffer(String fileName) {
		try {
			file = new RandomAccessFile(fileName, "r");
			fileLen = (int) file.length();
			bufLen = Math.min(fileLen, MAX_BUFFER_LENGTH);
			buf = new byte[bufLen];
			bufStart = Integer.MAX_VALUE; // nothing in buffer so far
			if (fileLen > 0) setPos(0); // setup buffer to position 0 (start)
			else bufPos = 0; // index 0 is already after the file, thus setPos(0) is invalid
			if (bufLen == fileLen) Close();
		} catch (IOException e) {
			throw new FatalError("Could not open file " + fileName);
		}
	}

	// don't use b after this call anymore
	// called in UTF8Buffer constructor
	protected Buffer(Buffer b) {
		buf = b.buf;
		bufStart = b.bufStart;
		bufLen = b.bufLen;
		fileLen = b.fileLen;
		bufPos = b.bufPos;
		file = b.file;
		stream = b.stream;
		// keep finalize from closing the file
		b.file = null;
	}

	protected void finalize() throws Throwable {
		super.finalize();
		Close();
	}

	protected void Close() {
		if (file != null) {
			try {
				file.close();
				file = null;
			} catch (IOException e) {
				throw new FatalError(e.getMessage());
			}
		}
	}

	public int Read() {
		if (bufPos < bufLen) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else if (getPos() < fileLen) {
			setPos(getPos());         // shift buffer start to pos
			return buf[bufPos++] & 0xff; // mask out sign bits
		} else if (stream != null && ReadNextStreamChunk() > 0) {
			return buf[bufPos++] & 0xff;  // mask out sign bits
		} else {
			return EOF;
		}
	}

	public int Peek() {
		int curPos = getPos();
		int ch = Read();
		setPos(curPos);
		return ch;
	}

	// beg .. begin, zero-based, inclusive, in byte
	// end .. end, zero-based, exclusive, in byte
	public String GetString(int beg, int end) {
		int len = 0;
		char[] buf = new char[end - beg];
		int oldPos = getPos();
		setPos(beg);
		while (getPos() < end) buf[len++] = (char) Read();
		setPos(oldPos);
		return new String(buf, 0, len);
	}

	public int getPos() {
		return bufPos + bufStart;
	}

	public void setPos(int value) {
		if (value >= fileLen && stream != null) {
			// Wanted position is after buffer and the stream
			// is not seek-able e.g. network or console,
			// thus we have to read the stream manually till
			// the wanted position is in sight.
			while (value >= fileLen && ReadNextStreamChunk() > 0);
		}

		if (value < 0 || value > fileLen) {
			throw new FatalError("buffer out of bounds access, position: " + value);
		}

		if (value >= bufStart && value < bufStart + bufLen) { // already in buffer
			bufPos = value - bufStart;
		} else if (file != null) { // must be swapped in
			try {
				file.seek(value);
				bufLen = file.read(buf);
				bufStart = value; bufPos = 0;
			} catch(IOException e) {
				throw new FatalError(e.getMessage());
			}
		} else {
			// set the position to the end of the file, Pos will return fileLen.
			bufPos = fileLen - bufStart;
		}
	}
	
	// Read the next chunk of bytes from the stream, increases the buffer
	// if needed and updates the fields fileLen and bufLen.
	// Returns the number of bytes read.
	private int ReadNextStreamChunk() {
		int free = buf.length - bufLen;
		if (free == 0) {
			// in the case of a growing input stream
			// we can neither seek in the stream, nor can we
			// foresee the maximum length, thus we must adapt
			// the buffer size on demand.
			byte[] newBuf = new byte[bufLen * 2];
			System.arraycopy(buf, 0, newBuf, 0, bufLen);
			buf = newBuf;
			free = bufLen;
		}
		
		int read;
		try { read = stream.read(buf, bufLen, free); }
		catch (IOException ioex) { throw new FatalError(ioex.getMessage()); }
		
		if (read > 0) {
			fileLen = bufLen = (bufLen + read);
			return read;
		}
		// end of stream reached
		return 0;
	}
}

//-----------------------------------------------------------------------------------
// UTF8Buffer
//-----------------------------------------------------------------------------------
class UTF8Buffer extends Buffer {
	UTF8Buffer(Buffer b) { super(b); }

	public int Read() {
		int ch;
		do {
			ch = super.Read();
			// until we find a utf8 start (0xxxxxxx or 11xxxxxx)
		} while ((ch >= 128) && ((ch & 0xC0) != 0xC0) && (ch != EOF));
		if (ch < 128 || ch == EOF) {
			// nothing to do, first 127 chars are the same in ascii and utf8
			// 0xxxxxxx or end of file character
		} else if ((ch & 0xF0) == 0xF0) {
			// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x07; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F; ch = super.Read();
			int c4 = ch & 0x3F;
			ch = (((((c1 << 6) | c2) << 6) | c3) << 6) | c4;
		} else if ((ch & 0xE0) == 0xE0) {
			// 1110xxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x0F; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F;
			ch = (((c1 << 6) | c2) << 6) | c3;
		} else if ((ch & 0xC0) == 0xC0) {
			// 110xxxxx 10xxxxxx
			int c1 = ch & 0x1F; ch = super.Read();
			int c2 = ch & 0x3F;
			ch = (c1 << 6) | c2;
		}
		return ch;
	}
}

//-----------------------------------------------------------------------------------
// StartStates  -- maps characters to start states of tokens
//-----------------------------------------------------------------------------------
class StartStates {
	private static class Elem {
		public int key, val;
		public Elem next;
		public Elem(int key, int val) { this.key = key; this.val = val; }
	}

	private Elem[] tab = new Elem[128];

	public void set(int key, int val) {
		Elem e = new Elem(key, val);
		int k = key % 128;
		e.next = tab[k]; tab[k] = e;
	}

	public int state(int key) {
		Elem e = tab[key % 128];
		while (e != null && e.key != key) e = e.next;
		return e == null ? 0: e.val;
	}
}

//-----------------------------------------------------------------------------------
// Scanner
//-----------------------------------------------------------------------------------
public class Scanner {
	static final char EOL = '\n';
    static final char eofCh = '\u0080';
	static final int  eofSym = 0;
	static final int maxT = 300;
	static final int noSym = 70;


	public Buffer buffer; // scanner buffer

	Token t;           // current token
	int ch;            // current input character
	int pos;           // byte position of current character
	int charPos;       // position by unicode characters starting with 0
	int col;           // column number of current character
	int line;          // line number of current character
	int oldEols;       // EOLs that appeared in a comment;
	static final StartStates start; // maps initial token character to start state
	static final Map literals;      // maps literal strings to literal kinds

	Token tokens;      // list of tokens already peeked (first token is a dummy)
	Token pt;          // current peek token
	
	char[] tval = new char[16]; // token text used in NextToken(), dynamically enlarged
	int tlen;          // length of current token

    private static final int  // token and char codes
        none        = 0,
        ident       = 1,
        identChar   = 201,
        number      = 2,
        digit       = 202,
        decimal     = 205,
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
        concat      = 127,
        eof         = 128;

	static {
		start = new StartStates();
		literals = new HashMap();
		for (int i = 65; i <= 90; ++i) start.set(i, identChar);
		for (int i = 97; i <= 122; ++i) start.set(i, identChar);
		for (int i = 48; i <= 57; ++i) start.set(i, digit);
		for (int i = 64; i <= 64; ++i) start.set(i, atsign);
		start.set(44, comma);       // ,
		start.set(58, colon);       // :
		start.set(59, semicolon);   // ;
		start.set(61, eqlSign);     // =
		start.set(123, lbrace);     // {
		start.set(125, rbrace);     // }
		start.set(40, lpar);        // (
		start.set(41, rpar);        // )
        start.set(91, lbrack);      // [
        start.set(93, rbrack);      // ]
		start.set(45, minus);       // -
		start.set(43, plus);        // +
		start.set(42, times);       // *
		start.set(47, slash);       // /
		start.set(37, rem);         // %
        start.set(46, period);      // .
        start.set(39, apostr);      // '
        start.set(34, quote);       // "
        start.set(95, identChar);   // _
        start.set(33, excl);        // !
        start.set(60, lss);         // <
        start.set(62, gtr);         // >
        start.set(Buffer.EOF, -1);
		literals.put("type", new Integer(type_));
		literals.put("integer", new Integer(integer_));
		literals.put("real", new Integer(real_));
		literals.put("bool", new Integer(bool_));
		literals.put("char", new Integer(char_));
		literals.put("string", new Integer(string_));
		literals.put("record", new Integer(record_));
		literals.put("of", new Integer(of_));
		literals.put("end", new Integer(end_));
		literals.put("list", new Integer(list_));
		literals.put("enumerate", new Integer(enumerate_));
		literals.put("caste", new Integer(caste_));
		literals.put("init", new Integer(init_));
		literals.put("body", new Integer(body_));
		literals.put("inherits", new Integer(inherits_));
		literals.put("observes", new Integer(observes_));
		literals.put("all", new Integer(all_));
		literals.put("in", new Integer(in_));
		literals.put("var", new Integer(var_));
		literals.put("set", new Integer(set_));
		literals.put("state", new Integer(state_));
		literals.put("action", new Integer(action_));
		literals.put("affect", new Integer(affect_));
		literals.put("begin", new Integer(begin_));
		literals.put("join", new Integer(join_));
		literals.put("quit", new Integer(quit_));
		literals.put("suspend", new Integer(suspend_));
		literals.put("resume", new Integer(resume_));
		literals.put("create", new Integer(create_));
		literals.put("destroy", new Integer(destroy_));
		literals.put("loop", new Integer(loop_));
		literals.put("while", new Integer(while_));
		literals.put("do", new Integer(do_));
		literals.put("repeat", new Integer(repeat_));
		literals.put("until", new Integer(until_));
		literals.put("for", new Integer(for_));
		literals.put("to", new Integer(to_));
		literals.put("by", new Integer(by_));
		literals.put("forall", new Integer(forall_));
		literals.put("if", new Integer(if_));
		literals.put("then", new Integer(then_));
		literals.put("elseif", new Integer(elseif_));
		literals.put("else", new Integer(else_));
		literals.put("case", new Integer(case_));
		literals.put("with", new Integer(with_));
		literals.put("when", new Integer(when_));
		literals.put("exist", new Integer(exist_));
		literals.put("and", new Integer(and_));
		literals.put("or", new Integer(or_));
		literals.put("xor", new Integer(xor_));
        literals.put("not", new Integer(not_));

	}
	
	public Scanner (String fileName) {
		buffer = new Buffer(fileName);
		Init();
	}
	
	public Scanner(InputStream s) {
		buffer = new Buffer(s);
		Init();
	}
	
	void Init () {
		pos = -1; line = 1; col = 0; charPos = -1;
		oldEols = 0;
		NextCh();
		if (ch == 0xEF) { // check optional byte order mark for UTF-8
			NextCh(); int ch1 = ch;
			NextCh(); int ch2 = ch;
			if (ch1 != 0xBB || ch2 != 0xBF) {
				throw new FatalError("Illegal byte order mark at start of file");
			}
			buffer = new UTF8Buffer(buffer); col = 0; charPos = -1;
			NextCh();
		}
		pt = tokens = new Token();  // first token is a dummy
	}
	
	void NextCh() {
		if (oldEols > 0) { ch = EOL; oldEols--; }
		else {
			pos = buffer.getPos();
			// buffer reads unicode chars, if UTF8 has been detected
			ch = buffer.Read(); col++; charPos++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && buffer.Peek() != '\n') ch = EOL;
			if (ch == EOL) { line++; col = 0; }
		}

	}
	
	void AddCh() {
		if (tlen >= tval.length) {
			char[] newBuf = new char[2 * tval.length];
			System.arraycopy(tval, 0, newBuf, 0, tval.length);
			tval = newBuf;
		}
		if (ch != Buffer.EOF) {
			tval[tlen++] = (char)ch; 

			NextCh();
		}

	}
	

	boolean Comment0() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
		if (ch == '*') {
			NextCh();
			for(;;) {
				if (ch == '*') {
					NextCh();
					if (ch == '/') {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == '/') {
					NextCh();
					if (ch == '*') {
						level++; NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0; charPos = charPos0;
		}
		return false;
	}

	boolean Comment1() {
		int level = 1, pos0 = pos, line0 = line, col0 = col, charPos0 = charPos;
		NextCh();
		if (ch == '/') {
			NextCh();
			for(;;) {
				if (ch == 13) {
					NextCh();
					if (ch == 10) {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0; charPos = charPos0;
		}
		return false;
	}


	void CheckLiteral() {
		String val = t.val;

		Object kind = literals.get(val);
		if (kind != null) {
			t.kind = ((Integer) kind).intValue();
		}
	}

	Token NextToken() {
		while (ch == ' ' ||
			ch >= 9 && ch <= 10 || ch == 13
		) NextCh();
		if (ch == '/' && Comment0() ||ch == '/' && Comment1()) return NextToken();
		int recKind = noSym;
		int recEnd = pos;
		t = new Token();
		t.pos = pos; t.col = col; t.line = line; t.charPos = charPos;
		int state = start.state(ch);
		tlen = 0; AddCh();

		loop: for (;;) {
            //t.kind = state;
			switch (state) {
				case -1: { t.kind = eofSym; break loop; } // NextCh already done
				case none: {
					if (recKind != noSym) {
						tlen = recEnd - t.pos;
						SetScannerBehindT();
					}
					t.kind = recKind; break loop;
				} // NextCh already done
				case identChar:
					recEnd = pos; recKind = ident;
                    if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch >= 'a' && ch <= 'z') {AddCh(); state = identChar; break;}
					else {t.kind = ident; t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}
				case digit:
					recEnd = pos; recKind = number;
                    if (ch == '.') {AddCh(); state = decimal; break;}
					else if (ch >= '0' && ch <= '9') {AddCh(); state = digit; break;}
					else {t.kind = number; break loop;}
                case decimal:
                    if (ch >= '0' && ch <= '9') {AddCh(); state = decimal; break;}
                    else {t.kind = number; break loop;}
				case atsign:
					recEnd = pos; recKind = atsign;
					if (ch == '/' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z') {AddCh(); state = atsign; break;}
					else {t.kind = atsign; break loop;}
				case comma:                                         // ,
					{t.kind = comma; break loop;}
				case semicolon:                                     // ;
					{t.kind = semicolon; break loop;}
				case eqlSign:                                       // =
					{t.kind = eqlSign; break loop;}
                case lpar:                                          // (
                    {t.kind = lpar; break loop;}
                case rpar:                                          // )
                    {t.kind = rpar; break loop;}
				case lbrack:                                        // [
					{t.kind = lbrack; break loop;}
				case rbrack:                                        // ]
					{t.kind = rbrack; break loop;}
				case lbrace:                                        // {
					{t.kind = lbrace; break loop;}
				case rbrace:                                        // }
					{t.kind = rbrace; break loop;}
				case plus:                                          // +
                    recEnd = pos; recKind = plus;
                    if (ch == '+') {AddCh(); state = concat; break;}
                    else {t.kind = plus; break loop;}
                case concat:                                        // ++
                    {t.kind = concat; break loop;}
				case times:                                         // *
					{t.kind = times; break loop;}
				case slash:                                         // /
					{t.kind = slash; break loop;}
				case rem:                                           // %
					{t.kind = rem; break loop;}
				case period:                                        // .
					{t.kind = period; break loop;}
                case lss:                                           // <
                    recEnd = pos; recKind = lss;
                    if (ch == '=') {AddCh(); state = leq; break;}
                    else {t.kind = lss; break loop;}
                case leq:                                           // <=
                    {t.kind = leq; break loop;}
                case gtr:                                           // >
                    recEnd = pos; recKind = gtr;
                    if (ch == '=') {AddCh(); state = geq; break;}
                    else {t.kind = gtr; break loop;}
                case geq:                                           // >=
                    {t.kind = geq; break loop;}
                case excl:                                          // !
                    if (ch == '=') {AddCh(); state = neq; break; }
                    else {state = 0; break;}
                case neq:                                           // !=
                    {t.kind = neq; break loop;}
                case apostr:                                        // '
                    if (ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z') {AddCh(); state = charVal; break;}
                    else {t.kind = apostr; break loop;}
                case charVal:
                    if (ch == '\'') {AddCh(); t.kind = charVal; break loop; }
                    else {state = 0; break;}
                case quote:                                         // "
                    if (ch <= '!' || ch >= '#' && ch <= 65535) {AddCh(); state = stringVal; break;}
                    else {t.kind = quote; break loop;}
                case stringVal:
                    if (ch == '"') {
                        AddCh(); t.kind = stringVal; break loop;}
                    else if (ch <= '!' || ch >= '#' && ch <= 65535) {AddCh(); state = stringVal; break; }
                    else {state = 0; break;}
                case colon:                                         // :
					recEnd = pos; recKind = colon;
					if (ch == '=') {AddCh(); state = assign; break;}
					else {t.kind = colon; break loop;}
				case minus:                                      // -
					recEnd = pos; recKind = minus;
					if (ch == '>') {AddCh(); state = effect; break;}
					else {t.kind = minus; break loop;}
                case assign:
                    {t.kind = assign; break loop;}
                case effect:
                    {t.kind = effect; break loop;}
                case eofCh: t.kind = eof; break;  // no nextCh() any more
			}
		}
		t.val = new String(tval, 0, tlen);
		return t;
	}
	
	private void SetScannerBehindT() {
		buffer.setPos(t.pos);
		NextCh();
		line = t.line; col = t.col; charPos = t.charPos;
		for (int i = 0; i < tlen; i++) NextCh();
	}
	
	// get the next token (possibly a token already seen during peeking)
	public Token Scan () {
		if (tokens.next == null) {
			return NextToken();
		} else {
			pt = tokens = tokens.next;
			return tokens;
		}
	}

	// get the next token, ignore pragmas
	public Token Peek () {
		do {
			if (pt.next == null) {
				pt.next = NextToken();
			}
			pt = pt.next;
		} while (pt.kind > maxT); // skip pragmas

		return pt;
	}

	// make sure that peeking starts at current scan position
	public void ResetPeek () { pt = tokens; }

} // end Scanner
