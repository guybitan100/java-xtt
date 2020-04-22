package com.mobixell.xtt;

import java.io.OutputStream;
import java.util.Vector;

/**
 * OutputStream which writes to multiple other OutputStreams
 *
 */

public class MultiOutputStream extends OutputStream
{

    private Vector<OutputStream> streams=new Vector<OutputStream>();
    private int i=0;

    /**
     *  Create new MultiOutputStream
     */
    public MultiOutputStream(){}
    /**
     *  Create new MultiOutputStream
     * @param os    OutputStream to initially add
     */
    public MultiOutputStream(OutputStream os)
    {
        addOutputStream(os);
    }

    /**
     *  Closes this output stream and its contained streams and releases any system resources associated with this stream.
     */
    public synchronized void close() throws java.io.IOException
    {
        for(i=0;i<streams.size();i++)
        {
            streams.get(i).close();
        }
    }

    /**
     *  Flushes this output stream and forces any buffered output bytes to be written out.
     */
    public synchronized void flush() throws java.io.IOException
    {
        for(i=0;i<streams.size();i++)
        {
            streams.get(i).flush();
        }
    }

    /**
     *  Writes b.length bytes from the specified byte array to this output stream.
     */
    public synchronized void write(byte[] b) throws java.io.IOException
    {
        for(int i=0;i<streams.size();i++)
        {
            streams.get(i).write(b);
        }
    }

    /**
     *  Writes len bytes from the specified byte array starting at offset off to this output stream.
     */
    public synchronized void write(byte[] b, int off, int len) throws java.io.IOException
    {
        for(int i=0;i<streams.size();i++)
        {
            streams.get(i).write(b,off,len);
        }
    }

    /**
     *  Writes the specified byte to this output stream.
     */
    public synchronized void write(int b) throws java.io.IOException
    {
        for(int i=0;i<streams.size();i++)
        {
            streams.get(i).write(b);
        }
    }

    /**
     *  Add an output Stream to the list of output streams.
     * @param os    OutputStream to add
     */
    public synchronized void addOutputStream(OutputStream os)
    {
        if(os==null)throw new NullPointerException();
        streams.add(os);
    }

    /**
     *  remove an output Stream from the list of output streams.
     * @param os    OutputStream to remove
     */
    public synchronized void removeOutputStream(OutputStream os)
    {
        if(os==null)return;
        streams.remove(os);
    }

    /**
     *  Create a new Vector to store the OutputStreams in
     */
    public synchronized void resetStreamVector()
    {
        streams=new Vector<OutputStream>();
    }
    /**
     *  Returns the Vector where the OutputStreams are stored in
     * @return Vector
     */
    public synchronized Vector<OutputStream> getStreamVector()
    {
        return streams;
    }

    public static final String tantau_sccsid = "@(#)$Id: MultiOutputStream.java,v 1.2 2006/07/21 17:04:27 cvsbuild Exp $";
}