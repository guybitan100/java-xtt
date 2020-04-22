package com.mobixell.xtt.hive;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.util.Stack;
import java.security.MessageDigest;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.hive.Hive;
import com.mobixell.xtt.gui.FileTransferProgress;

/*
        Format:

          0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     Length                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        /                                               /
        /                                               /
        /                      Data                     /
        /                                               /
        /                                               /
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                                               |
        |                                               |
        |                                               |
        |                  SHA-512 Hash                 |
        |                                               |
        |                                               |
        |                                               |
        |                                               |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

*/

public class FileTransferAgent extends Thread
{
    //Global Variables
    private static final int SENDINGMODE   = 0;
    private static final int RECEIVINGMODE = 1;

    private static Stack<Integer> ports = null;

    private int mode = -1;
    private int port = -1;
    private File file = null;

    private long filepointer = 0;
    private long filesize = -1;

    private long startTime = -1;
    private long endTime = -1;

    private boolean finished = false;
    private boolean cancelled = false;

    private FileTransferProgress progressWindow = null;

    public final Object progresskey = new Object();

    //Sender Variables
    private InetAddress recipient = null;
    //Recipient Variables

    //Sending constructor
    public FileTransferAgent(String filename, InetAddress recipient, int port)
    {
        mode = SENDINGMODE;
        file = new File(filename);
        filesize = file.length();
        this.port = port;
        this.recipient = recipient;

        if(XTTProperties.getXTTGui() != null)
        {
            progressWindow = new FileTransferProgress(XTTProperties.getXTTGui(),this,filename);
        }
    }

    //Receiving constructor
    public FileTransferAgent(File saveTo) throws java.util.EmptyStackException
    {
        mode = RECEIVINGMODE;

        if(ports == null) initialize();

        this.port = ports.pop();

        this.file = saveTo;

        if(XTTProperties.getXTTGui() != null)
        {
            progressWindow = new FileTransferProgress(XTTProperties.getXTTGui(),this,saveTo.getName());
        }
    }

    //Add ports to the Stack.
    private void initialize()
    {
        ports = new Stack<Integer>();
        ports.push(9884);
        ports.push(9885);
        ports.push(9886);
        ports.push(9887);
        ports.push(9888);
        ports.push(9889);
        ports.push(9890);
        ports.push(9891);
        ports.push(9892);
        ports.push(9893);
        ports.push(9894);
    }

    public void run()
    {
        switch(mode)
        {
            case SENDINGMODE:
                runSend();
                break;
            case RECEIVINGMODE:
                runReceive();
                break;
            default:
                break;
        }
    }

    public long getFileSize()
    {
        return filesize;
    }
    public long getCurrentSize()
    {
        return filepointer;
    }
    public boolean isFinished()
    {
        return finished;
    }
    public void cancelTransfer()
    {
        cancelled = true;
        synchronized(progresskey)
        {
            progresskey.notify();
        }        
    }
    public long elapsedTime()
    {
        if(startTime <= 0)
            return 0;
        else if(endTime <= 0)
            return (System.currentTimeMillis() - startTime);
        else
            return endTime-startTime;
    }

    public int getPort()
    {
        return port;
    }

    private void runSend()
    {
        Socket s = null;
        InputStream in = null;
        OutputStream out = null;
        startTime = System.currentTimeMillis();
        try
        {
            if(progressWindow != null)  progressWindow.view();
            Hive.printDebug("FileTransferAgent: Sending file to " + recipient.getHostAddress() + ":" + port);

            MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
            sha512Digest.reset();

            s = new Socket(recipient, port);
            out = new BufferedOutputStream(s.getOutputStream());
            in = new DigestInputStream(new BufferedInputStream(new FileInputStream(file),65536),sha512Digest);

            byte[] length = ConvertLib.getByteArrayFromLong(file.length(),8);
            out.write(length);

            int thebyte = 0;
            filepointer = 0;
            long progressNotify = file.length() / 102400;
            if(progressNotify <= 0) progressNotify = 1;
            int pointer = 0;
            long packetcount = 0;
            byte[] buffer = new byte[1024];
            while(filepointer < file.length() && !cancelled)
            {
                pointer = 0;
                pointer=in.read(buffer);
                packetcount++;
                filepointer += pointer;
                out.write(buffer,0,pointer);
                if((packetcount % progressNotify)==0)
                {
                    synchronized(progresskey)
                    {
                        progresskey.notify();
                    }
                }
            }
            synchronized(progresskey)
            {
                finished = true;
                progresskey.notify();
            }
            out.write(sha512Digest.digest());
            try
            {
                out.flush();
                out.close();
            }
            catch(java.net.SocketException se)
            {
                Hive.printDebug("FileTransferAgent: File already closed");
            }
        }
        catch(java.net.SocketException se)
        {
            XTTProperties.printFail("FileTransferAgent: Socket Error, Transfer Aborted");
            if(Hive.printDebug(null))
            {
                Hive.printException(se);
            }
        }
        catch(Exception e)
        {
            if(Hive.printDebug(null))
            {
                Hive.printException(e);
            }
        }
        finally
        {
            endTime=System.currentTimeMillis();
            synchronized(progresskey)
            {
                finished = true;
                progresskey.notify();
            }
            try{s.close();}catch(Exception e){}
            try{out.flush();out.close();}catch(Exception e){}
            try{in.close();}catch(Exception e){}
        }
    }

    private void runReceive()
    {
        ServerSocket ss = null;
        Socket s = null;
        InputStream in = null;
        OutputStream out = null;
        try
        {
            ss = new ServerSocket(port);
            Hive.printDebug("FileTransferAgent: Waiting for file on " + port);

            MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
            sha512Digest.reset();

            s = ss.accept();
            in = new BufferedInputStream(s.getInputStream(),65536);
            out = new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(file)),sha512Digest);

            if(progressWindow != null)  progressWindow.view();

            byte[] lengthBytes = new byte[8];
            byte[] sha512HashBytes = new byte[64];
            long length = -1;
            String sha512Hash = null;

            //Read the length
            int pointer = 0;
            while(pointer!=8)
            {
                pointer += in.read(lengthBytes,pointer,8);
            }
            length = ConvertLib.getLongFromByteArray(lengthBytes,0,8);
            filesize = length;

            Hive.printDebug("Length recieved: " + length);
            //Read the file
            int thebyte = 0;
            filepointer = 0;
            long progressNotify = length / 102400;
            if(progressNotify <= 0) progressNotify = 1;
            byte[] buffer = new byte[1024];
            pointer = 0;
            long packetcount = 0;
            try
            {
                while(filepointer < length && !cancelled)
                {
                    pointer = 0;
                    if(length-filepointer < 1024)
                        buffer = new byte[(int)(length-(long)filepointer)];
                    pointer=in.read(buffer);
                    packetcount++;
                    filepointer += pointer;
                    out.write(buffer,0,pointer);
                    if((packetcount % progressNotify)==0)
                    {
                        synchronized(progresskey)
                        {
                            progresskey.notify();
                        }
                    }
                }
            }
            catch(java.net.SocketException se)
            {
                XTTProperties.printFail("FileTransferAgent: Socket Error, Transfer Aborted");
                if(Hive.printDebug(null))
                {
                    Hive.printException(se);
                }
                cancelled = true;
            }
            //Add the ArrayIndexOutOfBounds since the write can throw this.
            catch(ArrayIndexOutOfBoundsException aiobe)
            {
                XTTProperties.printFail("FileTransferAgent: Socket Error, Transfer Aborted");
                if(Hive.printDebug(null))
                {
                    Hive.printException(aiobe);
                }
                cancelled = true;
            }
            synchronized(progresskey)
            {
                finished = true;
                progresskey.notify();
            }

            if(cancelled)
            {
                XTTProperties.printFail("FileTransferAgent: Transfer Aborted.");
                file.delete();
            }
            else
            {
                //Read the hash
                pointer = 0;
                while(pointer != 64)
                {
                    pointer += in.read(sha512HashBytes,pointer,64);
                }
                sha512Hash = ConvertLib.getHexStringFromByteArray(sha512HashBytes);
                String mySha512Hash = ConvertLib.getHexStringFromByteArray(sha512Digest.digest());
                if(!sha512Hash.equals(mySha512Hash))
                {
                    Hive.printFail("FileTransferAgent: Hashs didn't match");
                    Hive.printDebug("FileTransferAgent: \n" + mySha512Hash + "\n" + sha512Hash);
                }
                else
                {
                    Hive.printDebug("FileTransferAgent: Hashs matched");
                }
            }
        }
        catch(Exception e)
        {
            if(Hive.printDebug(null))
            {
                Hive.printException(e);
            }
        }
        finally
        {
            endTime = System.currentTimeMillis();
            synchronized(progresskey)
            {
                finished = true;
                progresskey.notify();
            }
            ports.push(port);
            try{s.close();}catch(Exception e){}
            try{ss.close();}catch(Exception e){}
            try{out.flush();out.close();}catch(Exception e){}
            try{in.close();}catch(Exception e){}
        }
    }
}