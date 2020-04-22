package com.mobixell.xtt;

import java.io.InputStream;


public class StringBufferInputStream extends InputStream
{

    private int index=0;
    int available=0;
    int i=0;
    byte[] c=null;
    private StringBuffer buffer=null;

    public StringBufferInputStream(StringBuffer buffer)
    {
        this.buffer=buffer;
    }
    public synchronized int available()
    {
        available=buffer.length()-index;
        if(available>0)
        {
            return available;
        }
        return 0;
    }
    public void close()
    {
    }
    public void mark(int readlimit){}

    public synchronized boolean markSupported()
    {
        return false;
    }

    public synchronized int read()
    {
        if(index<buffer.length())
        {
            return (int)buffer.charAt(index++);
        }
        return -1;
    }
    public synchronized int read(byte[] b)
    {
        if(b==null)throw new NullPointerException();
        return read(b,0, b.length);
    }
    public synchronized int read(byte[] b, int off, int len)
    {
        if(b==null)
        {
            throw new NullPointerException();
        } else if((off<0)||(len<0)||(off+len>b.length))
        {
            throw new IndexOutOfBoundsException();
        } else if(index>=buffer.length())
        {
            return -1;
        } else if(len==0)
        {
            return 0;
        }

        if(index+len>buffer.length())
        {
            len=buffer.length()-index;
        }
        
        int pointer=0;
        int lencount=0;
        while(pointer<b.length&&lencount<len)
        {
            c=ConvertLib.createBytes(buffer.substring(index, index+1));
            for(int j=0;j<c.length;j++)
            {
                b[off+(pointer++)]=c[j];
            }
            index++;
            lencount++;
        }
        return len;
    }

    public void reset(){}
    public synchronized long skip(long n)
    {
        if(n+index>buffer.length())
        {
            int retval=buffer.length()-index;
            index=buffer.length();
            return retval;
        }
        index=index+(int)n;
        return n;
    }

	public static final String tantau_sccsid = "@(#)$Id: StringBufferInputStream.java,v 1.6 2007/03/01 12:26:10 rsoder Exp $";
}