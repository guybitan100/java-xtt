package com.mobixell.xtt;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;


public class StringOutputStream extends OutputStream
{

    private ByteArrayOutputStream buffer=new ByteArrayOutputStream();

    public void close() throws java.io.IOException
    {
        buffer.close();
    }
    public void flush() throws java.io.IOException
    {
        buffer.flush();
    }
    public void write(byte[] b) throws java.io.IOException
    {
        buffer.write(b);
    }
    public void write(byte[] b, int off, int len) throws java.io.IOException
    {
        buffer.write(b,off,len);
    }
    public void write(int b) throws java.io.IOException
    {
        buffer.write(b);
    }
    public String toString()
    {
        try
        {
            return buffer.toString(XTTProperties.getCharSet());
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            return buffer.toString();
        }
    }

    public void clear()
    {
        buffer.reset();
    }

	public static final String tantau_sccsid = "@(#)$Id: StringOutputStream.java,v 1.3 2007/03/01 12:26:10 rsoder Exp $";
}