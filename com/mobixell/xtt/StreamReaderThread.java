package com.mobixell.xtt;

import java.io.InputStreamReader;
import java.io.InputStream;

public class StreamReaderThread extends Thread
{
    StringBuffer mOut = null;
    InputStreamReader mIn = null;
    InterrupterThread interrupter = null;
    Object lock = new Object();
    
    public StreamReaderThread(InputStream in, StringBuffer out, InterrupterThread interrupter)
    {
        mOut=out;
        try
        {
            mIn=new InputStreamReader(in,XTTProperties.getCharSet());
        }
        catch(java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("StreamReaderThread:" +"Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            mIn=new InputStreamReader(in);
        }
        this.interrupter = interrupter;
    }
    
    //Am I finished?
    public void waitFor()
    {
        synchronized(lock)
        {
            //If you get the lock we're done.
        }

    }

    public void run()
    {
        synchronized(lock)
        {
            int ch;
            try
            {
                while(-1 != (ch=mIn.read()))
                {
                    mOut.append((char)ch);
                    if(interrupter != null)
                    {
                        interrupter.reset();
                    }
                }
            }
            catch (Exception e)
            {
                XTTProperties.printDebug("StreamReaderThread: Given up reading.");
            }
            lock.notifyAll();
        }
    }
}