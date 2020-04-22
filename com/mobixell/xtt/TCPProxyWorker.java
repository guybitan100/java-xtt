package com.mobixell.xtt;

import java.net.Socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public class TCPProxyWorker extends Thread
{
    private Socket inSocket = null;
    private Socket outSocket = null;

    private int myTimeout = 60000;
    private int id = -1;

    private TCPProxy myProxy = null;

    private boolean stop = false;

    public int getWorkerId()
    {
        return id;
    }

    public TCPProxyWorker(int id, Socket s, TCPProxy myProxy, int timeout)
    {
        this.id = id;
        this.inSocket=s;
        this.myProxy = myProxy;
        this.myTimeout = timeout;
    }

    public void handleClient()
    {
        try
        {
            outSocket = new Socket();
            outSocket.connect(myProxy.getEndpoint());

            inSocket.setSoTimeout(0);
            outSocket.setSoTimeout(0);
            inSocket.setTcpNoDelay(true);
            outSocket.setTcpNoDelay(true);

            SocketStream aToB = null;
            SocketStream bToA = null;

            if(myProxy.getWhichSide() < 0)
            {
                XTTProperties.printDebug("TCPProxyWorker: Only performing actions on the way in");
                aToB = new SocketStream(inSocket, outSocket, myProxy.getFirstWait(), myProxy.getAction(), myProxy.getWait(), myProxy.getActionValue());
                bToA = new SocketStream(outSocket, inSocket, -1, -1, -1, -1);
            } else if(myProxy.getWhichSide() > 0)
            {
                XTTProperties.printDebug("TCPProxyWorker: Only performing actions on the way back");
                aToB = new SocketStream(inSocket, outSocket, -1, -1, -1, -1);
                bToA = new SocketStream(outSocket, inSocket, myProxy.getFirstWait(), myProxy.getAction(), myProxy.getWait(), myProxy.getActionValue());
            } else
            {
                XTTProperties.printDebug("TCPProxyWorker: Performing actions on both sides.");
                aToB = new SocketStream(inSocket, outSocket, myProxy.getFirstWait(), myProxy.getAction(), myProxy.getWait(), myProxy.getActionValue());
                bToA = new SocketStream(outSocket, inSocket, myProxy.getFirstWait(), myProxy.getAction(), myProxy.getWait(), myProxy.getActionValue());
            }

            aToB.start();
            bToA.run();
        } catch(Exception e)
        {
            XTTProperties.printException(e);
        } finally
        {
            try{inSocket.close();}catch(Exception e){}
            try{outSocket.close();}catch(Exception e){}
            myProxy.removeWorker(this);
        }
    }
    public static int getSideNumber(String sn) throws NumberFormatException
    {
        int a=-1;
        try
        {
            a=Integer.decode(sn);
        } catch(Exception e)
        {
            if(sn.equalsIgnoreCase("in"))
            {
                return -1;
            } else if(sn.equalsIgnoreCase("out"))
            {
                return 1;
            } else if(sn.equalsIgnoreCase("both"))
            {
                return 0;
            } else
            {
                throw new NumberFormatException("Invalid Side Name, no side defined for '"+sn+"'");
            }
        }
        return a;
    }

    public static final int FIRSTACTION         = 1;
    public static final int DELAY               = 1;
    public static final int RANDOMBYTES         = 2;
    public static final int RANDOMALPHANUMERIC  = 3;
    public static final int LASTACTION          = 3;
    public static int getAction(String action) throws NumberFormatException
    {
        int a=-1;
        try
        {
            a=Integer.decode(action);
        } catch(Exception e)
        {
            if(action.equalsIgnoreCase("delay"))
            {
                return DELAY;
            } else if(action.equalsIgnoreCase("randombytes"))
            {
                return RANDOMBYTES;
            } else if(action.equalsIgnoreCase("randomalphanumeric"))
            {
                return RANDOMALPHANUMERIC;
            } else
            {
                throw new NumberFormatException("Invalid Action Name, no action defined for '"+action+"'");
            }
        }
        if(a<FIRSTACTION||a>LASTACTION)
        {
            throw new NumberFormatException("Invalid Action Range, no action defined for '"+a+"'");
        }
        return a;
    }

    private class SocketStream extends Thread
    {
        private Socket in=null;
        private Socket out=null;

        private int firstwait           = -1;
        private int action              = -1;
        private int wait                = -1;
        private int actionvalue         = -1;
        private int randomizer          = 0;

        public SocketStream(Socket in, Socket out, int firstwait , int action, int wait, int actionvalue)
        {
            this.in             = in;
            this.out            = out;
            this.firstwait      = firstwait;
            this.action         = action;
            this.wait           = wait;
            this.actionvalue    = actionvalue;
            XTTProperties.printDebug("TCPProxyWorker: Started: firstwait: " + firstwait + " action: " + action +" wait: " +wait + " actionvalue: " + actionvalue);
        }

        public void run()
        {
            try
            {
                BufferedInputStream  inputStream  = new BufferedInputStream(in.getInputStream(),65536);
                BufferedOutputStream outputStream = new BufferedOutputStream(out.getOutputStream());
                int thebyte = 0;
                int counter = 0;
                while(thebyte != -1)
                {
                    thebyte = inputStream.read();
                    counter++;
//System.out.println(counter+": "+thebyte+" "+firstwait+" "+((firstwait >0) && (counter == firstwait)));
                    if((firstwait >0) && (counter == firstwait))
                    {
                        switch(action)
                        {
                            case DELAY:
                                try
                                {
                                    XTTProperties.printDebug("TCPProxyWorker(" + in.getPort() + "): first wait counter: " + counter + ", sleeping for " + actionvalue + " ms");
                                    Thread.sleep(actionvalue);
                                } catch (Exception e)
                                {
                                    XTTProperties.printException(e);
                                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                                }
                                break;
                            case RANDOMBYTES:
                            case RANDOMALPHANUMERIC:
                                try
                                {
                                    XTTProperties.printDebug("TCPProxyWorker(" + in.getPort() + "): first wait counter: " + counter + ", randomizing for " + actionvalue +" bytes");
                                    randomizer=actionvalue;
                                } catch (Exception e)
                                {
                                    XTTProperties.printException(e);
                                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if((wait > 0) && (counter > wait) && ((counter % wait)==0))
                    {
                        switch(action)
                        {
                            case DELAY:
                                try
                                {
                                    XTTProperties.printDebug("TCPProxyWorker(" + in.getPort() + "): wait counter: " + counter + ", sleeping for " + actionvalue +" ms");
                                    Thread.sleep(actionvalue);
                                } catch (Exception e)
                                {
                                    XTTProperties.printException(e);
                                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                                }
                                break;
                            case RANDOMBYTES:
                            case RANDOMALPHANUMERIC:
                                try
                                {
                                    XTTProperties.printDebug("TCPProxyWorker(" + in.getPort() + "): wait counter: " + counter + ", randomizing for " + actionvalue +" bytes");
                                    randomizer=actionvalue;
                                } catch (Exception e)
                                {
                                    XTTProperties.printException(e);
                                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if(thebyte == -1)break;
                    if(randomizer>0)
                    {
                        switch(action)
                        {
                            default:
                            case RANDOMBYTES:
                                thebyte=RandomLib.getRandomInt(256);
                                break;
                            case RANDOMALPHANUMERIC:
                                thebyte=RandomLib.getRandomAplhaNumericByte();
                                break;
                        }
                        randomizer--;
                    }
                    outputStream.write(thebyte);
                    outputStream.flush();
                }
            } catch(java.net.SocketException se)
            {
                if(se.getMessage().equalsIgnoreCase("socket closed"))
                {
                    XTTProperties.printDebug("TCPProxyWorker(" + in.getPort() + "): Socket Closed");
                } else
                {
                    XTTProperties.printFail("TCPProxyWorker: BufferedStream: Socket Error.");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    if(XTTProperties.printDebug("null"))
                    {
                        XTTProperties.printException(se);
                    }
                }
            } catch(Exception e)
            {
                XTTProperties.printFail("TCPProxyWorker: BufferedStream: Exception");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug("null"))
                {
                    XTTProperties.printException(e);
                }
            } finally
            {
                try{in.close();}catch(Exception e){}
                try{out.close();}catch(Exception e){}
            }
        }
    }

    public void run()
    {
        handleClient();
    }

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        XTTProperties.printDebug("SIPWorker(/"+getWorkerId()+"): setting stop");
        this.stop = true;
    }
    public synchronized void doStop()
    {
        this.stop = true;
        XTTProperties.printDebug("SIPWorker(/"+getWorkerId()+"): doing stop");
        try
        {
            this.inSocket.close();
        } catch (Exception ex){}
        // Notify anyone waiting on this object
        notifyAll();
    }

    public static final String tantau_sccsid = "@(#)$Id: TCPProxyWorker.java,v 1.5 2010/01/15 10:22:00 rsoder Exp $";
}