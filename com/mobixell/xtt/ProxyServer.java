package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.io.FileInputStream;

import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * a very simple, multi-threaded ICAP server. Implementation notes are in ProxyServer.html, and also
 * as comments in the source code. In Standalone mode the services /reqmod and /respmod are  added by default.
 *
 * @version     $Id: ProxyServer.java,v 1.3 2010/07/09 10:50:32 mlichtin Exp $
 * @author Roger Soder
 */
public class ProxyServer extends Thread
{
    private int port;
    public  int getPort(){return port;}
    private String ipAddr = "";
    private ServerSocket ss = null;
    private static HashMap<String,ProxyServer> serverMap=new HashMap<String,ProxyServer>();

    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<ProxyWorker> runningthreads = new Vector<ProxyWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* the ICAP server's virtual root */
    private String name = null;

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(ProxyWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}

    public static final String tantau_sccsid = "@(#)$Id: ProxyServer.java,v 1.3 2010/07/09 10:50:32 mlichtin Exp $";


    /**
     * Main method for ICAP server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = 8088;
        String ip = null;
        String name = "XTTProxy1";
        int timeout = DEFAULTTIMEOUT;
        boolean runSecure=false;
        String tracing="";
        String tracingFormat=null;
        
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
                } 
                else if ((a[i].equalsIgnoreCase("--ip"))||(a[i].equalsIgnoreCase("-i")))
                {
                    ip=a[++i];
                }else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--name"))||(a[i].equalsIgnoreCase("-n")))
                {
                    name=a[++i];
                } else if ((a[i].equalsIgnoreCase("--timeout"))||(a[i].equalsIgnoreCase("-o")))
                {
                    timeout=Integer.parseInt(a[++i]);
                } else
                {
                    showHelp();
                }
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
            ProxyServer ws = null;
            XTTProperties.printDebug("Version: ProxyServer: "+FunctionModule.parseVersion(ProxyServer.tantau_sccsid)+" ProxyWorker: "+FunctionModule.parseVersion(ProxyWorker.tantau_sccsid));
            ws = new ProxyServer(ip,port, name, timeout, runSecure);
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
        System.out.println("eXtreme Test Tool (XTT) - ICAP Server - 724 Solutions");
        System.out.println("Version: ProxyServer: "+FunctionModule.parseVersion(ProxyServer.tantau_sccsid)+" ProxyWorker: "+FunctionModule.parseVersion(ProxyWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the ProxyServer on (default 8088)");
        System.out.println("");
        //System.out.println("\t-s, --secure");
        //System.out.println("\t\tStart the Secure ProxyServer (default port 443)");
        //System.out.println("");
        System.out.println("\t-n <name>, --name <name>");
        System.out.println("\t\tName for via header of the ProxyServer");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the ProxyServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }

    /**
     * Create a new instance of the ProxyServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of ProxyWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port
     * @throws Exception
     */

    public ProxyServer (String ip,int port, String name, int timeout, boolean runSecure) throws Exception
    {
        super("ProxyServer-"+port);
        // WE DO NOT SUPPORT THIS AT THE MOMMENT
        runSecure=false;
        this.port = port;
        this.name = name;
        this.ipAddr = ip;
        this.timeout = timeout;

        // listen for incoming non-secure HTTP requests
        if(!runSecure)
        {
            ss = new ServerSocket();
        } else
        {
            SSLServerSocket sslss = null;
            KeyStore keystore     = null;
            KeyManagerFactory kmf = null;
            SSLContext sslc       = null;

            sslc = SSLContext.getInstance("SSL");
            kmf = KeyManagerFactory.getInstance("SunX509");
            keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream("key/xttkeystore"), KEYSTOREPW);
            kmf.init(keystore, KEYSTOREPW);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keystore);

            sslc.init(kmf.getKeyManagers(),tmf.getTrustManagers(), new java.security.SecureRandom());

            SSLServerSocketFactory sslsocketfactory = sslc.getServerSocketFactory();
            sslss = (SSLServerSocket) sslsocketfactory.createServerSocket();

            ss = sslss;
        }
        ss.setReuseAddress(true);
        
        if (ipAddr == null) ss.bind(new java.net.InetSocketAddress(port));
		else ss.bind(new java.net.InetSocketAddress(ipAddr, port));
        
        synchronized(serverMap)
        {
            serverMap.put(port+"",this);
        }
        XTTProperties.printInfo("ProxyServer: listening on port:"+port+" timout:"+timeout +" name:"+name);
    }

    public ProxyServer (String ip,int port, String name, int timeout) throws Exception
    {
        this(ip,port,name,timeout,false);
    }

    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            ProxyServer s=(ProxyServer)serverMap.get(port);
            return s.getIdCounter();
        }
    }

    public void closeSocket() throws Exception
    {
        synchronized(serverMap)
        {
            ss.close();
            serverMap.remove(port+"");
        }
    }
    public static void closeSockets() throws Exception
    {
        synchronized(serverMap)
        {
            java.util.Iterator<String> it=serverMap.keySet().iterator();
            ProxyServer s=null;
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
            ProxyServer s=(ProxyServer)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
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
            return (ss == null);
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
        XTTProperties.printDebug("ProxyServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that ProxyServer exits
        //this.join(); //please do that outside the ProxyServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("ProxyServer("+port+"): Killing workers start");
        Vector<ProxyWorker> close=new Vector<ProxyWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            ProxyWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<ProxyWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("ProxyServer("+port+"): Killing workers done");
    }


    /**
     * Part of Thread() interface, start the ICAP server
     */
    public void run()
    {
        try
        {
            Socket s=null;
            while (true)
            {
                // wait for a connection request to the ICAP server
                s = null;
                try
                {
                    s = ss.accept();
                } catch (java.io.IOException e)
                {
                    if (stopGracefully)
                    {
                        XTTProperties.printDebug("ProxyServer("+port+"): accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("ProxyServer("+port+"): accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }
                XTTProperties.printDebug("ProxyServer: connection request received");
                ProxyWorker ws = new ProxyWorker(idCounter++,s,name,timeout,this);
                XTTProperties.printDebug("ProxyServer("+port+"): starting new ProxyWorker: id " + ws.getWorkerId());
                runningthreads.add(ws);
                ws.start();
            }
            if (s != null)
            {
                s.close();
            }
            if (ss != null)
            {
                ss.close();
            }
        } catch (java.io.IOException ex)
        {
            XTTProperties.printDebug("ProxyServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
    }

}
