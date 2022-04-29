package com.clarisma.common.util;

import java.nio.ByteBuffer;

public class Bytes
{
    /**
     * Searches a byte array for the first occurrence 
     * of a byte array pattern.
     * 
     * Implementation of KMP from
     * http://helpdesk.objects.com.au/java/search-a-byte-array-for-a-byte-sequence
     * 
     */
    public static int indexOf(byte[] data, byte[] pattern) 
    {
        int[] failure = computeFailure(pattern);
        int j = 0;
        for (int i = 0; i < data.length; i++) 
        {
            while (j > 0 && pattern[j] != data[i]) 
            {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) 
            { 
                j++; 
            }
            if (j == pattern.length) 
            {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    private static int[] computeFailure(byte[] pattern) 
    {
        int[] failure = new int[pattern.length];
        int j = 0;
        for (int i = 1; i < pattern.length; i++) 
        {
            while (j>0 && pattern[j] != pattern[i]) 
            {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) 
            {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }

    public static int getInt(byte[] ba, int pos)
    {
        return
            (ba[pos] & 0xff) |
                ((ba[pos+1] & 0xff) << 8) |
                ((ba[pos+2] & 0xff) << 16) |
                ((ba[pos+3] & 0xff) << 24);
    }

    public static void putInt(byte[] ba, int pos, int v)
    {
        ba[pos] = (byte)v;
        ba[pos+1] = (byte)(v >>> 8);
        ba[pos+2] = (byte)(v >>> 16);
        ba[pos+3] = (byte)(v >>> 24);
    }

    public static void putShort(byte[] ba, int pos, int v)
    {
        ba[pos] = (byte)v;
        ba[pos+1] = (byte)(v >>> 8);
    }

    public static long getLong(byte[] ba, int pos)
    {
        return ((long)getInt(ba, pos) & 0xffff_ffffl) | ((long)getInt(ba, pos+4) << 32);
    }

    public static void putLong(byte[] ba, int pos, long v)
    {
        putInt(ba, pos, (int)v);
        putInt(ba, pos+4, (int)(v >>> 32));
    }

    /**
     * Reads a string from a buffer. A String must be in the following format:
     * one or two bytes (using multi-byte encoding) that indicate the length,
     * followed by the UTF-8 encoded content of the string.
     *
     * Note that only string lengths up to 32K are supported.
     *
     * @param buf	the buffer
     * @param p		the position of the string
     * @return
     */
    public static String readString(ByteBuffer buf, int p)
    {
        // TODO: This may overrun if string is zero-length
        int len = buf.getChar(p);
        if((len & 0x80) != 0)
        {
            len = (len & 0x7f) | (len >> 1) & 0xff00;
            p+=2;
        }
        else
        {
            len &= 0x7f;
            p++;
        }

        byte[] chars = new byte[len];
        buf.get(p, chars);
        try
        {
            return new String(chars, "UTF-8");
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Unable to decode string.", ex);
        }
    }

    /**
     * Compares an ASCII string stored in a buffer to a match string.
     *
     * @param buf
     * @param p
     * @param s
     * @return
     */
    public static boolean stringEqualsAscii(ByteBuffer buf, int p, String s)
    {
        int len = buf.getChar(p);
        if((len & 0x80) != 0)
        {
            len = (len & 0x7f) | (len >> 1) & 0xff00;
            p+=2;
        }
        else
        {
            len &= 0x7f;
            p++;
        }
        if(len != s.length()) return false;
        for(int i=0; i<len; i++)
        {
            if(s.charAt(i) != buf.get(p++)) return false;
        }
        return true;
    }

}
