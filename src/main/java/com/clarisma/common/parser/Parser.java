/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.clarisma.common.util.FileLocation;
import com.clarisma.common.text.Strings;

/**
 * An extendable general-purpose lexer/parser, especially suited
 * for generating abstract syntax trees.
 *
 */

// TODO: string lexing isn't a good fit for regex expressions, because \ has a different
//  meaning for C-style strings vs. regexes

public class Parser implements CharSequence, FileLocation // TODO: remove FileLocation?
{
	/**
	 * The file from which the source content originated
	 * (used solely for error reporting) 
	 */
	protected String fileName;
	protected int tabSize = 4;
	/**
	 * The source content
	 */
	protected CharSequence buf;
	/**
	 * The position where the current token starts
	 */
	protected int pos;
	/**
	 * The line of the current token (1-based)
	 */
	protected int line;
	/**
	 * The column where the current token starts (1-based)
	 */
	protected int column;
	/**
	 * The type of the current token
	 */
	protected Object tokenType;
	/**
	 * The literal value of the current token
	 */
	protected CharSequence tokenValue;
	/**
	 * A sorted list of all strings that represent tokens...
	 */
	protected List<String> tokenStrings;
	/**
	 * ... and a list of the actual tokens these strings
	 * represent.
	 */
	protected List<Object> tokens;
	/**
	 * A mapping of keywords, i.e. strings that might otherwise 
	 * be considered identifiers, to the tokens they represent
	 * (built from `tokens` and `tokenStrings` once the actual
	 * parsing starts)
	 */
	protected Map<String,Object> keywordTokens;
	/**
	 * An index to speed up token matching: each array position
	 * represents an ASCII character (0-127) and contains the index
	 * of the last string in `tokenStrings` that begins with
	 * this character (built once actual parsing starts).
	 * Matching always starts with the last entry in `tokenStrings`
	 * and works backwards, in order to match the longest 
	 * possible sequence. This way, ">=" is matched before ">",
	 * since it appears later in the alphabetically ordered
	 * list. 
	 */
	protected short initialCharTokens[];
	/**
	 * A regex pattern used to match identifiers
	 */
	protected Pattern identifierPattern;
	
	protected final static String START = "<start>";
	protected final static String END = "<end>";
	protected final static String WHITESPACE = "<ws>";
	protected final static String QUOTATION_MARK = "\"";
	protected final static String STRING = "<string>";
	protected final static String INVALID = "<invalid>";
	protected final static String INVALID_STRING = "<invalid-string>";
	protected final static String COMMENT = "<comment>";
	protected final static String NUMBER = "<number>";
	protected final static String IDENTIFIER = "<id>";
	protected final static String MULTILINE_COMMENT_START = 
			"<multiline-comment-start>";
	protected final static String MULTILINE_COMMENT_END = 
			"<multiline-comment-end>";
	/**
	 * The default identifier pattern: an identifier must 
	 * contain only letters, numbers, or underscore characters,
	 * and must start with a letter or underscore. 
	 */
	public final static Pattern DEFAULT_IDENTIFIER_PATTERN =
		Pattern.compile("[a-zA-Z_]\\w*");
	
	public Parser()
	{
		identifierPattern = DEFAULT_IDENTIFIER_PATTERN;
		tokenStrings = new ArrayList<>();
		tokens = new ArrayList<>();
		keywordTokens = new HashMap<>();
		// index 0 is treated specially, so insert a dummy entry
		tokenStrings.add("");
		tokens.add(null);
		addDefaultTokens();
	}

	/**
	 * Adds a string and its corresponding token (which can be the
	 * string itself) to the lexer table. This method must be
	 * called before parsing starts. Unless `replace` is `true`,
	 * an exception is thrown if a token already exists for the
	 * given string. Multiple strings can produce the same token.
	 * 
	 * @param string		the string the lexer should match ...  
	 * @param token			... and the token to be produced
	 * @param replace		If `true`, any previous string/token
	 * 						association will be replaced
	 */
	public void addToken(String string, Object token, boolean replace)
	{
		int n = Collections.binarySearch(tokenStrings, string);
		if (n<0)
		{
			n = -n-1;
			tokenStrings.add(n, string);
			tokens.add(n, token);
		}
		else
		{
			if(!replace)
			{
				throw new RuntimeException(
					String.format(
						"A token has already been assigned " +
						"for \"%s\". Use replaceToken() if you " +
						"want to override it.", string));
			}
			tokens.set(n, token);
		}
	}

	public void addToken(String string, Object token)
	{
		addToken(string, token, false);
	}
	
	public void replaceToken(String string, Object token)
	{
		addToken(string, token, true);
	}
	

	public void addToken(Object token)
	{
		addToken(token.toString(), token, false);
	}

	public void addTokens(Object... tokens)
	{
		for(Object t: tokens) addToken(t.toString(), t, false);
	}

	protected void addDefaultTokens()
	{
		addToken(" ", WHITESPACE);
		addToken("\t", WHITESPACE);
		addToken("\n", WHITESPACE);
		addToken("\r", WHITESPACE);
		addToken("\"", QUOTATION_MARK);
		addToken("\'", QUOTATION_MARK);
	}
	
	protected void initLexer()
	{
		if(initialCharTokens != null) return;
		
		initialCharTokens = new short[128];
		char prevInitialChar = 0;
		for(int n=tokens.size()-1; n>0; n--)
		{
			String tokenString = tokenStrings.get(n);
			Matcher matcher = identifierPattern.matcher(tokenString);
			if(matcher.matches())
			{
				keywordTokens.put(tokenString, tokens.get(n));
				continue;
			}
			char initialChar = tokenString.charAt(0); 
			if(initialChar == prevInitialChar) continue;
			if(initialChar < 128)
			{
				initialCharTokens[initialChar] = (short)n;
			}
			prevInitialChar = initialChar;
		}
	}
	
	public Pattern setIdentifierPattern(Pattern pattern)
	{
		Pattern oldPattern = identifierPattern;
		identifierPattern = pattern;
		return oldPattern;
	}
	
	/**
	 * Starts the parsing process for the given character sequence.
	 * Initializes the lexer and moves to the first non-whitespace
	 * token. To consume tokens and turn them into production
	 * elements, call the appropriate production methods of the
	 * parser subclass for the grammar to be parsed.  
	 * 
	 * @param s		the character sequence to be parsed
	 */
	public void parse(CharSequence s)
	{
		buf = s;
		pos = 0;
		line = 1;
		column = 1;
		tokenType = START;
		tokenValue = "";
		initLexer();
		nextToken();
	}
	
	/**
	 * Checks whether more tokens are available to be consumed.
	 * 
	 * @return	`true` if more tokens are available 
	 */
	public boolean hasMore()
	{
		return tokenType != END;
	}
	
	protected final int matchString(String s)
	{
		int len = s.length();
		int charsRemaining = length();
		if (charsRemaining < len) len = charsRemaining;
		int i=0;
		for(; i<len; i++)
		{
			if(charAt(i) != s.charAt(i)) break;
		}
		return i;
	}
	
	private void consumeToken()
	{
		int len = tokenValue.length();
		for(int i=0; i<len; i++)
		{
			char ch = buf.charAt(pos);
			if(ch == '\n')
			{
				line++;
				column=1;
			}
			else if(ch == '\t')
			{
				column += tabSize - ((column-1) % tabSize);
			}
			else
			{
				column++;
			}
			pos++;
		}
	}

	/**
	 * Matches the next token. Tokens of type WHITESPACE are skipped. 
	 */
	protected void nextToken()
	{
	main:
		for(;;)
		{
			// System.out.format("Token: %s (%s)\n", tokenType, tokenValue);
			consumeToken();
			if(pos >= buf.length())
			{
				tokenType = END;
				tokenValue = "";
				return;
			}
			char ch = buf.charAt(pos);
			if(ch < 128)
			{
				for(int tokenIndex = initialCharTokens[ch];
					tokenIndex > 0; tokenIndex--)
				{
					String tokenString = tokenStrings.get(tokenIndex);
					int charsMatched = matchString(tokenString);
					if(charsMatched == tokenString.length())
					{
						tokenType = tokens.get(tokenIndex);
						tokenValue = tokenString;
						if(tokenType==WHITESPACE)
						{
							continue main;
						}
						if(tokenType==COMMENT)
						{
							int n=0;
							for(;n<length(); n++)
							{
								if(charAt(n) == '\n') break;
							}
							tokenValue = subSequence(0, n);
							continue main;
						}
						if(tokenType==QUOTATION_MARK)
						{
							matchQuoted();
							return;
						}
						return;
					}
					if(charsMatched==0) break;
				}
			}
			
			// TODO: swapped the sequence of "identifier" and "number"
			// to allow ids that start with a number, check!!! 
			if(identifier() != null) return;
			if(number() != null) return;
			tokenType= INVALID;
			tokenValue = subSequence(0, 1);
			return;
		}
	}
	
	protected final boolean accept(Object tok)
	{
		return tokenType == tok;
	}
	
	protected final boolean acceptAndConsume(Object tok)
	{
		if (tokenType == tok)
		{
			nextToken();
			return true;
		}
		return false;
	}
	
	/**
	 * Checks whether the current token equals the specified token.
	 * If not, throws a parse exception.
	 *  
	 * @param tok the token to match
	 */
	// TODO: boolean return, in case error overrridden to not throw
	protected final void expect(Object tok)
	{
		if(tokenType != tok)
		{
			error(String.format("Expected %s, but got %s",
				tok, tokenType));
		}
	}
	
	// TODO: rename?
	protected void resetLexer()
	{
		tokenType = null;
		tokenValue = "";
		nextToken();
	}
	
	protected void error(String msg)
	{
		msg = String.format("%s [%d:%d]: %s",
			fileName==null ? "<none>" : fileName,
			line, column, msg);
		throw new ParserException(msg);
	}
	
	protected void error(String msg, Object... args)
	{
		error(String.format(msg, args));
	}
	
	protected String stringValue()
	{
		return tokenValue.toString();
	}

	/**
	 * Returns the actual string value of a literal string
	 * token, chopping off the quotation marks and turning
	 * escape sequences into actual characters.
	 * Example: "a\\b" becomes a\b
	 * @return
	 */
	protected String unquotedStringValue()
	{
		return Strings.unescape(tokenValue.toString(), true);
	}
	
	protected int intValue()
	{
		return Integer.parseInt(tokenValue.toString());
	}
	
	protected long longValue()
	{
		return Long.parseLong(tokenValue.toString());
	}

	protected double doubleValue()
	{
		return Double.parseDouble(tokenValue.toString());
	}

	/*
	public static int matchWhitespace(CharSequence s) 
	{
		int n=0;
		for(; n<s.length(); n++)
		{
			if(!Character.isWhitespace(s.charAt(n))) break;
		}
		return n;
	}
	*/
	
	public Object identifier()
	{
		Matcher matcher = identifierPattern.matcher(this);
		if(matcher.lookingAt())
		{
			tokenValue = subSequence(0, matcher.end());
			tokenType = keywordTokens.getOrDefault(tokenValue, IDENTIFIER);
			// System.out.format("<id>: %s\n", tokenValue);
			return tokenType;
		}
		return null;
	}

	// TODO: make private?
	// TODO: should accept whitespace between minus sign and first number
	public Object number() 
	{
		boolean minus = false;
		boolean decimal = false;
		boolean digits = false;
		int n=0;
		for(; n < length(); n++)
		{
			char ch = charAt(n);
			if (ch=='-')
			{
				if (minus || digits || decimal) break;
				minus=true;
				continue;
			}
			if (ch=='.')
			{
				if (decimal) break;
				decimal=true;
				continue;
			}
			if(Character.isDigit(ch)) 
			{
				digits=true;
				continue;
			}
			break;
		}
		if(!digits) return null;
		tokenType = NUMBER;
		tokenValue = subSequence(0,n);
		// System.out.format("Matched number: %s\n",  tokenValue);
		return tokenType;
	}

	// not used
	public static int matchNumber_unused(CharSequence s) 
	{
		boolean minus = false;
		boolean decimal = false;
		boolean digits = false;
		int n=0;
		for(; n < s.length(); n++)
		{
			char ch = s.charAt(n);
			if (ch=='-')
			{
				if (minus || digits || decimal) break;
				minus=true;
				continue;
			}
			if (ch=='.')
			{
				if (decimal) break;
				decimal=true;
				continue;
			}
			if(Character.isDigit(ch)) 
			{
				digits=true;
				continue;
			}
			break;
		}
		return digits ? n : 0;
	}

	public void matchQuoted() 
	{
		char chQuote = charAt(0);
		int n=1;
		for(; n < length(); n++)
		{
			char ch = charAt(n);
			if(ch == '\"' || ch == '\'')
			{
				if (ch != chQuote) continue;
				chQuote = 0;
				n++;
				break;
			}
			if(ch=='\\')  // escape
			{
				n++;
				if(n >= length()) break;
				if(Strings.unescape(ch) == Character.MAX_VALUE) break;
			}
			else if (Strings.escape(ch) != Character.MAX_VALUE)
			{
				break;
			}
		}
		tokenType = (chQuote==0) ? STRING : INVALID_STRING;
		tokenValue = subSequence(0, n);
	}
	
	public int length() 
	{
		return buf.length()-pos;
	}

	public char charAt(int index) 
	{
		return buf.charAt(pos+index);
	}

	public CharSequence subSequence(int start, int end) 
	{
		return buf.subSequence(pos+start, pos+end);
	}
	
	public String getFile ()
	{
		return fileName;
	}
	
    public int getLine ()
    {
    	return line;
    }
    
    public int getColumn ()
    {
    	return column;
    }
}
