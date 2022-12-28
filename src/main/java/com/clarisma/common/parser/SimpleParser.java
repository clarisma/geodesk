/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.parser;

// WIP: simplified scannerless parser

import com.clarisma.common.math.MathUtils;

public class SimpleParser
{
    private final String buf;
    private int pos;
    private char nextChar;

    public record Schema(long firstLower, long firstUpper, long subsequentLower, long subsequentUpper)
    {
    }

    public SimpleParser(String s)
    {
        buf = s;
        skipWhitespace();
    }

    private void skipWhitespace()
    {
        skipWhitespace(pos >= buf.length() ? 0xFFFF : buf.charAt(pos));
    }

    private void skipWhitespace(char ch)
    {
        while (ch <= ' ')
        {
            ch = advance();
        }
        nextChar = ch;
    }

    private char advance()
    {
        pos++;
        return pos < buf.length() ? buf.charAt(pos) : 0xFFFF;
    }

    public char nextChar()
    {
        return nextChar;
    }

    /**
     * Attempts to match an identifier that conforms to the given schema
     * (The schema describes which characters that are valid for the first
     * and subsequent identifier characters).
     *
     * @param schema    the identifier schema
     * @return          the identifier, or null if the current token does
     *                  not conform ot the given schema
     */
    public String identifier(Schema schema)
    {
        char ch = nextChar;
        long lowerBits = schema.firstLower;
        long upperBits = schema.firstUpper;
        if (ch < 128)
        {
            long bits = ch < 64 ? lowerBits : upperBits;
            if ((bits & (1L << ch)) == 0) return null;
            // Important to use long constant, so the lower 6 bits of ch
            // will be used for shift
        }
        else
        {
            if (ch == 0xFFFF) return null;
            if (!Character.isLetter(ch)) return null;
        }
        lowerBits = schema.subsequentLower;
        upperBits = schema.subsequentUpper;
        int start = pos;

        for (; ; )
        {
            ch = advance();
            if (ch < 128)
            {
                long bits = ch < 64 ? lowerBits : upperBits;
                if ((bits & (1L << ch)) == 0) break;
                // Important to use long constant, so the lower 6 bits of ch
                // will be used for shift
            }
            else
            {
                if (ch == 0xFFFF) break;
                if (!Character.isLetter(ch)) break;
            }
        }
        String s = buf.substring(start, pos);
        skipWhitespace(ch);
        return s;
    }

    protected void error(String msg)
    {
        throw new ParserException(lineColString() + msg);
    }

    protected long lineCol()
    {
        int line = 1;
        int col = 1;
        for(int n=0; n < pos; n++)
        {
            switch(buf.charAt(n))
            {
            case '\n':
                col = 1;
                line++;
                break;
            case '\r':
                break;
            default:
                col++;
                break;
            }
        }
        return (long)line | (((long)col) << 32);
    }

    protected String lineColString()
    {
        long lineCol = lineCol();
        return "[%d:%d] ".formatted((int)lineCol, (int)(lineCol >>> 32));
    }

    protected long matchQuoted()
    {
        char quoteChar = nextChar;
        if (quoteChar != '\'' && quoteChar != '\"') return 0;
        pos++;
        int start = pos;
        for (; ; )
        {
            if (pos >= buf.length())
            {
                error("Unterminated string literal");
                return -1;
            }
            char ch = buf.charAt(pos);
            if (ch == quoteChar)
            {
                nextChar = ch;
                break;
            }
            if (ch == '\\') pos++;
            pos++;
        }
        long range = (((long) pos) << 32) | start;
        skipWhitespace();
        return range;
    }

    /**
     * Attempts to match a string value (in single or double quotes).
     * If successful, advances to the next token, If the string is unclosed,
     * generates an error.
     *
     * @return  the raw, unescaped string value (without the enclosing quotes),
     *          or null if the current token is not a quote-enclosed string
     */
    public String rawString()
    {
        long range = matchQuoted();
        if(range <= 0) return null;
        return buf.substring((int)range, (int)(range >>> 32));
    }

    /**
     * Attempts to match the given character. If match is successful, advances
     * to the next token.
     *
     * @param ch       the character to match
     * @return         true if matched
     */
    public boolean literal(char ch)
    {
        if(nextChar != ch) return false;
        nextChar = advance();
        skipWhitespace();
        return true;
    }

    /**
     * Attempts to match the given string. If match is successful, advances
     * to the next token.
     *
     * @param s      the string to match
     * @return       true if matched
     */
    public boolean literal(String s)
    {
        int len = s.length();
        if(pos + len >= buf.length()) return false;
        if (buf.regionMatches(pos, s, 0, len))
        {
            pos += len;
            skipWhitespace();
            return true;
        }
        return false;
    }

    /**
     * Attempts to match a number. If successful, advances to next token.
     *
     * @return  the number value, or `NaN` if the current token is not a number
     */
    public double number()
    {
        // first char must be - or . or 0-9
        char first = (char)(nextChar-43);
        if(first > 14) return Double.NaN;
        if ((0b111111111101100 & (1 << first)) == 0) return Double.NaN;
        char ch = nextChar;
        double value = 0;
        boolean negative = false;
        boolean seenDot = false;
        int decimalPos = -1;
        boolean seenDigit = false;
        int n = pos;
        if(ch == '-')
        {
            negative = true;
            n++;
            if(n >= buf.length()) return Double.NaN;
            ch = buf.charAt(n);
        }
        for(;;)
        {
            if(ch == '.')
            {
                if (decimalPos >= 0) break;
                decimalPos = n;
            }
            else
            {
                value = value * 10 + (ch - '0');
                seenDigit = true;
            }
            n++;
            ch = (n < buf.length()) ? buf.charAt(n) : 0xFFFF;
            char next = (char)(ch-43);
            if(next > 14) break;
            if ((0b111111111101000 & (1 << next)) == 0) break;

            // TODO: exponent
        }
        if(!seenDigit) return Double.NaN;
        if(negative) value = -value;
        if(decimalPos >= 0) value /= MathUtils.pow10(n-decimalPos-1);
        pos = n;
        skipWhitespace(ch);
        return value;
    }
}
