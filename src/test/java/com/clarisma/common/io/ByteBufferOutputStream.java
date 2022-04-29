package com.clarisma.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ByteBufferOutputStream extends OutputStream
{
    private ByteBuffer buf;
    private int pos;

    public ByteBufferOutputStream(ByteBuffer buf, int pos)
    {
        this.buf = buf;
        this.pos = pos;
    }

    @Override public void write(int b)
    {
        buf.put(pos++, (byte)b);
    }

    @Override public void write(byte[] b, int off, int len)
    {
        buf.put(pos, b, off, len);
        pos += len;
    }
}
