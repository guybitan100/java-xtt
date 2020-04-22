package com.mobixell.xtt;


import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.net.InetSocketAddress;


/**
 * a very simple, multi-threaded HTTP server. Implementation notes are in TCPProxy.html, and also
 * as comments in the source code
 *
 * @version     $Id: TCPProxy.java,v 1.5 2010/07/09 10:50:32 mlichtin Exp $
 * @author Roger Soder
 */
public class TCPProxy implements Runnable
{
    private final static int DEFAULTPORT = 8080;

    private InetSocketAddress endpoint = null;
    public InetSocketAddress getEndpoint(){return endpoint;}
    private int port;
    public  int getPort(){return port;}

    private ServerSocket tcpListener = null;
    
    private static HashMap<String,TCPProxy> serverMap=new HashMap<String,TCPProxy>();

    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<TCPProxyWorker> runningthreads = new Vector<TCPProxyWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(TCPProxyWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}
    
    public static final String tantau_sccsid = "@(#)$Id: TCPProxy.java,v 1.5 2010/07/09 10:50:32 mlichtin Exp $";

    private int firstwait = -1;
    public int getFirstWait(){return firstwait;}
    private int wait = -1;
    public int getWait(){return wait;}
    private int action = -1;
    public int getAction(){return action;}
    private int actionvalue = -1;
    public int getActionValue(){return actionvalue;}
    private int whichside = 0;
    public int getWhichSide(){return whichside;}

    /**
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = DEFAULTPORT;
        int timeout = DEFAULTTIMEOUT;

        int firstwait = -1;
        int wait = -1;
        int action = -1;
        int actionvalue = -1;
        int whichside = 0;
        InetSocketAddress endpoint=null;

        String tracing="";
        String tracingFormat=null;
        
        String remotehostname=null;
        int remoteport=-1;
        
        try
        {
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                } else if ((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    port=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--remotehost"))
                {
                    remotehostname=a[++i];
                } else if (a[i].equalsIgnoreCase("--remoteport"))
                {
                    remoteport=Integer.parseInt(a[++i]);
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--timeout"))||(a[i].equalsIgnoreCase("-o")))
                {
                    timeout=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--action"))
                {
                    action=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--actionvalue"))
                {
                    actionvalue=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--firstwait"))
                {
                    firstwait=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--wait"))
                {
                    wait=Integer.parseInt(a[++i]);
                } else if (a[i].equalsIgnoreCase("--whichside"))
                {
                    whichside=Integer.parseInt(a[++i]);
                } else
                {
                    System.out.println("Error: Invalid command line option: "+a[i]);
                    showHelp();
                }
            }
            if(remotehostname==null ||remoteport<=0)
            {
                endpoint=null;
            } else
            {
                endpoint=new InetSocketAddress(remotehostname, remoteport);
            }
            if(endpoint==null)
            {
                throw new Exception("Remote Host and Port need to be set!");
            }

            if(tracing.equalsIgnoreCase(""))
            {
                XTTProperties.setTracing(XTTProperties.DEBUG);
            } else if(tracing.equalsIgnoreCase("f")||tracing.equalsIgnoreCase("fail"))
            {
                XTTProperties.setTracing(XTTProperties.FAIL);
            } else if(tracing.equalsIgnoreCase("w")||tracing.equalsIgnoreCase("warn"))
            {
                XTTProperties.setTracing(XTTProperties.WARN);
            } else if(tracing.equalsIgnoreCase("i")||tracing.equalsIgnoreCase("info"))
            {
                XTTProperties.setTracing(XTTProperties.INFO);
            } else if(tracing.equalsIgnoreCase("v")||tracing.equalsIgnoreCase("verbose"))
            {
                XTTProperties.setTracing(XTTProperties.VERBOSE);
            } else if(tracing.equalsIgnoreCase("d")||tracing.equalsIgnoreCase("debug"))
            {
                XTTProperties.setTracing(XTTProperties.DEBUG);
            } else
            {
                throw new Exception("Invalid option: "+tracing);
            }
            if(tracingFormat!=null)
            {
                XTTProperties.setPrintFormat(tracingFormat);
            }
            TCPProxy ws = null;
            XTTProperties.printDebug("Version: TCPProxy: "+FunctionModule.parseVersion(TCPProxy.tantau_sccsid)+" TCPProxyWorker: "+FunctionModule.parseVersion(TCPProxyWorker.tantau_sccsid));
            ws = new TCPProxy(port, endpoint, timeout, firstwait, wait, action, actionvalue, whichside);
            ws.run();
        }
        catch (Exception e)
        {
            System.out.println("Error: Invalid command line option");
            XTTProperties.printException(e);
            showHelp(); //If the command line options were bad, show the help
        }
    }

    private static void showHelp()
    {
        System.out.println("eXtreme Test Tool (XTT) - Web Server - 724 Solutions");
        System.out.println("Version: TCPProxy: "+FunctionModule.parseVersion(TCPProxy.tantau_sccsid)+" TCPProxyWorker: "+FunctionModule.parseVersion(TCPProxyWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the TCPProxy on (default "+DEFAULTPORT+")");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the TCPProxy");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify the timeout time in ms for the TCPProxy");
        System.out.println("");
        System.out.println("\t--action <action>");
        System.out.println("\t\t");
        System.out.println("");
        System.out.println("\t--actionvalue <actionvalue>");
        System.out.println("\t\t");
        System.out.println("");
        System.out.println("\t--firstwait <firstwait>");
        System.out.println("\t\t");
        System.out.println("");
        System.out.println("\t--wait <wait>");
        System.out.println("\t\t");
        System.out.println("");
        System.out.println("\t--whichside <whichside>");
        System.out.println("\t\t");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the TCPProxy class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of TCPProxyWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param listenport
     * @param endpoint
     * @param timeout
     * @throws Exception
     */
    public TCPProxy(int listenport, InetSocketAddress endpoint, int timeout) throws Exception
    {
        this(listenport, endpoint, timeout, -1, -1, -1, -1, 0);
    }
    public TCPProxy(int listenport, InetSocketAddress endpoint, int timeout, int firstwait, int wait, int action, int actionvalue, int whichside) throws Exception
    {
        try
        {
            this.port       = listenport;
            this.timeout    = timeout;
            this.endpoint   = endpoint;
            this.firstwait  = firstwait  ;
            this.wait       = wait       ;
            this.action     = action     ;
            this.actionvalue= actionvalue;
            this.whichside  = whichside  ;
            
            tcpListener = new ServerSocket();
            tcpListener.setReuseAddress(true);
            tcpListener.bind(new java.net.InetSocketAddress(port));

            synchronized(serverMap)
            {
                serverMap.put(port+"",this);
            }
            XTTProperties.printInfo("TCPProxy: listening on tcp port:"+port+" timout:"+timeout+" Remotesocket: "+endpoint);
        } catch (Exception ex)
        {
            try
            {
                tcpListener.close();
            } catch (Exception cle){}
            throw ex;
        }
    }

    public static void setProxyAction(String port, int firstwait, int wait, int action, int actionvalue, int whichside)
    {
        synchronized(serverMap)
        {
            TCPProxy s=(TCPProxy)serverMap.get(port);
            s.setProxyAction(firstwait, wait, action, actionvalue, whichside);
        }
    }
    public void setProxyAction(int firstwait, int wait, int action, int actionvalue, int whichside)
    {
        this.firstwait = firstwait;
        this.wait = wait;
        this.action = action;
        this.actionvalue = actionvalue;
        this.whichside = whichside;    
    }

    public void closeSocket() throws Exception
    {
        synchronized(serverMap)
        {
            tcpListener.close();
            serverMap.remove(port+"");
        }
    }
    public static void closeSockets() throws Exception
    {
        synchronized(serverMap)
        {
            java.util.Iterator<String> it=serverMap.keySet().iterator();
            TCPProxy s=null;
            String currentPort=null;
            while(it.hasNext())
            {
                currentPort=it.next();
                s=serverMap.get(currentPort);
                s.stopGracefully();
            }
            serverMap.clear();
        }
    }
    public static void closeSocket(String port) throws Exception
    {
        synchronized(serverMap)
        {
            TCPProxy s=(TCPProxy)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }
    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            TCPProxy s=(TCPProxy)serverMap.get(port);
            return s.getIdCounter();
        }
    }

    public static boolean checkSockets()
    {
        synchronized(serverMap)
        {
            //System.out.println(serverMap.isEmpty());
            return serverMap.isEmpty();
        }
    }
    public boolean checkSocket()
    {
        synchronized(serverMap)
        {
            return (tcpListener == null);
        }
    }
    public static boolean checkSocket(String port)
    {
        synchronized(serverMap)
        {
            return (serverMap.get(port)==null);
        }
    }

    /**
     * Stop the smsc server
     * @throws Exception
     */
    public void stopGracefully() throws java.lang.Exception
    {
        stopGracefully = true;
        XTTProperties.printDebug("TCPProxy("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        tcpListener.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that TCPProxy exits
        //this.join(); //please do that outside the TCPProxy
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("TCPProxy("+port+"): Killing workers start");
        Vector<TCPProxyWorker> close=new Vector<TCPProxyWorker>();
        TCPProxyWorker w=null;
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            w=runningthreads.get(i);
            close.add(w);
            w.setStop();
        }
        for (int i = 0; i < close.size(); ++i)
        {
            w=close.get(i);
            w.doStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<TCPProxyWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("TCPProxy("+port+"): Killing workers done");
    }


    /**
     * Part of Thread() interface, start the Web server
     */
    public void run()
    {
        ServerSocket ss=tcpListener;
        try
        {
            Socket socket=null;
            while (true)
            {
                // wait for a connection request to the Web server
                socket = null;
                try
                {
                    socket = ss.accept();
                } catch (java.io.IOException e)
                {
                    if (stopGracefully)
                    {
                        XTTProperties.printDebug("TCPProxy("+ss.getLocalPort()+"): TCP accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("TCPProxy("+ss.getLocalPort()+"): TCP accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }
                XTTProperties.printDebug("TCPProxy: TCP connection request received");
                TCPProxyWorker ws = new TCPProxyWorker(idCounter++,socket,this,timeout);
                XTTProperties.printDebug("TCPProxy("+ss.getLocalPort()+"): starting new TCP TCPProxyWorker: id " + ws.getWorkerId());
                runningthreads.add(ws);
                ws.start();
            }
            if (socket != null)
            {
                socket.close();
            }
            if (ss != null)
            {
                ss.close();
            }
        } catch (java.io.IOException ex)
        {
            XTTProperties.printDebug("TCPProxy("+ss.getLocalPort()+"): Exception in TCPListener.run(): " + ex.getMessage());
            return;
        }        
    }
}
