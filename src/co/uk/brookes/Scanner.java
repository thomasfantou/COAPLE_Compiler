
package co.uk.brookes;

import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.HashMap;


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

	static {
		start = new StartStates();
		literals = new HashMap();
		for (int i = 65; i <= 90; ++i) start.set(i, Token.identChar);
		for (int i = 97; i <= 122; ++i) start.set(i, Token.identChar);
		for (int i = 48; i <= 57; ++i) start.set(i, Token.digit);
		for (int i = 64; i <= 64; ++i) start.set(i, Token.atsign);
		start.set(44, Token.comma);       // ,
		start.set(58, Token.colon);       // :
		start.set(59, Token.semicolon);   // ;
		start.set(61, Token.eqlSign);     // =
		start.set(123, Token.lbrace);     // {
		start.set(125, Token.rbrace);     // }
		start.set(40, Token.lpar);        // (
		start.set(41, Token.rpar);        // )
        start.set(91, Token.lbrack);      // [
        start.set(93, Token.rbrack);      // ]
		start.set(45, Token.minus);       // -
		start.set(43, Token.plus);        // +
		start.set(42, Token.times);       // *
		start.set(47, Token.slash);       // /
		start.set(37, Token.rem);         // %
        start.set(46, Token.period);      // .
        start.set(39, Token.apostr);      // '
        start.set(34, Token.quote);       // "
        start.set(95, Token.identChar);   // _
        start.set(33, Token.excl);        // !
        start.set(60, Token.lss);         // <
        start.set(62, Token.gtr);         // >
        start.set(Buffer.EOF, -1);
		literals.put("type", new Integer(Token.type_));
		literals.put("integer", new Integer(Token.integer_));
		literals.put("real", new Integer(Token.real_));
		literals.put("bool", new Integer(Token.bool_));
		literals.put("char", new Integer(Token.char_));
		literals.put("string", new Integer(Token.string_));
		literals.put("record", new Integer(Token.record_));
		literals.put("of", new Integer(Token.of_));
		literals.put("end", new Integer(Token.end_));
		literals.put("list", new Integer(Token.list_));
		literals.put("enumerate", new Integer(Token.enumerate_));
		literals.put("caste", new Integer(Token.caste_));
		literals.put("init", new Integer(Token.init_));
		literals.put("body", new Integer(Token.body_));
		literals.put("inherits", new Integer(Token.inherits_));
		literals.put("observes", new Integer(Token.observes_));
		literals.put("all", new Integer(Token.all_));
		literals.put("in", new Integer(Token.in_));
		literals.put("var", new Integer(Token.var_));
		literals.put("set", new Integer(Token.set_));
		literals.put("state", new Integer(Token.state_));
		literals.put("action", new Integer(Token.action_));
		literals.put("affect", new Integer(Token.affect_));
		literals.put("begin", new Integer(Token.begin_));
		literals.put("join", new Integer(Token.join_));
		literals.put("quit", new Integer(Token.quit_));
		literals.put("suspend", new Integer(Token.suspend_));
		literals.put("resume", new Integer(Token.resume_));
		literals.put("create", new Integer(Token.create_));
		literals.put("destroy", new Integer(Token.destroy_));
		literals.put("loop", new Integer(Token.loop_));
		literals.put("while", new Integer(Token.while_));
		literals.put("do", new Integer(Token.do_));
		literals.put("repeat", new Integer(Token.repeat_));
		literals.put("until", new Integer(Token.until_));
		literals.put("for", new Integer(Token.for_));
		literals.put("to", new Integer(Token.to_));
		literals.put("by", new Integer(Token.by_));
		literals.put("forall", new Integer(Token.forall_));
		literals.put("if", new Integer(Token.if_));
		literals.put("then", new Integer(Token.then_));
		literals.put("elseif", new Integer(Token.elseif_));
		literals.put("else", new Integer(Token.else_));
		literals.put("case", new Integer(Token.case_));
		literals.put("with", new Integer(Token.with_));
		literals.put("when", new Integer(Token.when_));
		literals.put("exist", new Integer(Token.exist_));
		literals.put("and", new Integer(Token.and_));
		literals.put("or", new Integer(Token.or_));
		literals.put("xor", new Integer(Token.xor_));
        literals.put("not", new Integer(Token.not_));

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
				case Token.none: {
					if (recKind != noSym) {
						tlen = recEnd - t.pos;
						SetScannerBehindT();
					}
					t.kind = recKind; break loop;
				} // NextCh already done
				case Token.identChar:
					recEnd = pos; recKind = Token.ident;
                    if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch >= 'a' && ch <= 'z') {AddCh(); state = Token.identChar; break;}
					else {t.kind = Token.ident; t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}
				case Token.digit:
					recEnd = pos; recKind = Token.number;
                    if (ch == '.') {AddCh(); state = Token.decimal; break;}
					else if (ch >= '0' && ch <= '9') {AddCh(); state = Token.digit; break;}
					else {t.kind = Token.number; break loop;}
                case Token.decimal:
                    if (ch >= '0' && ch <= '9') {AddCh(); state = Token.decimal; break;}
                    else {t.kind = Token.number; break loop;}
				case Token.atsign:
					recEnd = pos; recKind = Token.atsign;
					if (ch == '/' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z') {AddCh(); state = Token.atsign; break;}
					else {t.kind = Token.atsign; break loop;}
				case Token.comma:                                         // ,
					{t.kind = Token.comma; break loop;}
				case Token.semicolon:                                     // ;
					{t.kind = Token.semicolon; break loop;}
				case Token.eqlSign:                                       // =
					{t.kind = Token.eqlSign; break loop;}
                case Token.lpar:                                          // (
                    {t.kind = Token.lpar; break loop;}
                case Token.rpar:                                          // )
                    {t.kind = Token.rpar; break loop;}
				case Token.lbrack:                                        // [
					{t.kind = Token.lbrack; break loop;}
				case Token.rbrack:                                        // ]
					{t.kind = Token.rbrack; break loop;}
				case Token.lbrace:                                        // {
					{t.kind = Token.lbrace; break loop;}
				case Token.rbrace:                                        // }
					{t.kind =Token. rbrace; break loop;}
				case Token.plus:                                          // +
                    recEnd = pos; recKind = Token.plus;
                    if (ch == '+') {AddCh(); state = Token.concat; break;}
                    else {t.kind = Token.plus; break loop;}
                case Token.concat:                                        // ++
                    {t.kind = Token.concat; break loop;}
				case Token.times:                                         // *
					{t.kind = Token.times; break loop;}
				case Token.slash:                                         // /
					{t.kind = Token.slash; break loop;}
				case Token.rem:                                           // %
					{t.kind = Token.rem; break loop;}
				case Token.period:                                        // .
					{t.kind = Token.period; break loop;}
                case Token.lss:                                           // <
                    recEnd = pos; recKind = Token.lss;
                    if (ch == '=') {AddCh(); state = Token.leq; break;}
                    else {t.kind = Token.lss; break loop;}
                case Token.leq:                                           // <=
                    {t.kind = Token.leq; break loop;}
                case Token.gtr:                                           // >
                    recEnd = pos; recKind = Token.gtr;
                    if (ch == '=') {AddCh(); state = Token.geq; break;}
                    else {t.kind = Token.gtr; break loop;}
                case Token.geq:                                           // >=
                    {t.kind = Token.geq; break loop;}
                case Token.excl:                                          // !
                    if (ch == '=') {AddCh(); state = Token.neq; break; }
                    else {state = 0; break;}
                case Token.neq:                                           // !=
                    {t.kind = Token.neq; break loop;}
                case Token.apostr:                                        // '
                    if (ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z') {AddCh(); state = Token.charVal; break;}
                    else {t.kind = Token.apostr; break loop;}
                case Token.charVal:
                    if (ch == '\'') {AddCh(); t.kind = Token.charVal; break loop; }
                    else {state = 0; break;}
                case Token.quote:                                         // "
                    if (ch <= '!' || ch >= '#' && ch <= 65535) {AddCh(); state = Token.stringVal; break;}
                    else {t.kind = Token.quote; break loop;}
                case Token.stringVal:
                    if (ch == '"') {
                        AddCh(); t.kind = Token.stringVal; break loop;}
                    else if (ch <= '!' || ch >= '#' && ch <= 65535) {AddCh(); state = Token.stringVal; break; }
                    else {state = 0; break;}
                case Token.colon:                                         // :
					recEnd = pos; recKind = Token.colon;
					if (ch == '=') {AddCh(); state = Token.assign; break;}
					else {t.kind = Token.colon; break loop;}
				case Token.minus:                                      // -
					recEnd = pos; recKind = Token.minus;
					if (ch == '>') {AddCh(); state = Token.effect; break;}
					else {t.kind = Token.minus; break loop;}
                case Token.assign:
                    {t.kind = Token.assign; break loop;}
                case Token.effect:
                    {t.kind = Token.effect; break loop;}
                case eofCh: t.kind = Token.eof; break;  // no nextCh() any more
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
