package com.mobixell.xtt;

import java.io.File;
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
 * a very simple, multi-threaded HTTP server. Implementation notes are in MMSCServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: MMSCServer.java,v 1.8 2010/07/09 10:50:32 mlichtin Exp $
 * @author Gavin Cattell & Roger Soder
 */
public class MMSCServer extends Thread
{
    private int port;
    public  int getPort(){return port;}

    private ServerSocket ss = null;
    private static HashMap<String,MMSCServer> serverMap=new HashMap<String,MMSCServer>();

    private String KEYSTORE = "xttserver";
    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private char[] KEYPW="xttxtt".toCharArray();

    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<MMSCWorker> runningthreads = new Vector<MMSCWorker>();

    private static int idCounter = 0;
    public static void resetWorkerId(){idCounter = 0;}

    /* the STI server's virtual root */
    private File myRoot = null;

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(MMSCWorker worker){workerCounter--;runningthreads.remove(worker);}
    public synchronized int getNumWorkers(){return workerCounter;}


    public static final String tantau_sccsid = "@(#)$Id: MMSCServer.java,v 1.8 2010/07/09 10:50:32 mlichtin Exp $";


    /**
     * Main method for STI server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = 80;
        String rootDir = ".";
        int timeout = DEFAULTTIMEOUT;
        boolean runSecure=false;
        String tracing="";
        String tracingFormat=null;
        try
        {
            boolean portSet=false;
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                } else if ((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    port=Integer.parseInt(a[++i]);
                    portSet=true;
                } else if ((a[i].equalsIgnoreCase("--secure"))||(a[i].equalsIgnoreCase("-s")))
                {
                    runSecure=true;
                    if(!portSet)port=443;
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--rootdir"))||(a[i].equalsIgnoreCase("-r")))
                {
                    rootDir=a[++i];
                    File f=new java.io.File(rootDir);
                    if(!f.isDirectory())
                    {
                        System.out.println("\nERROR: Not a directory: "+rootDir+"\n");
                        showHelp(); //If the command line options were bad, show the help
                        return;
                    }
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
            MMSCServer ws = null;
            XTTProperties.printDebug("Version: MMSCServer: "+FunctionModule.parseVersion(MMSCServer.tantau_sccsid)+" MMSCWorker: "+FunctionModule.parseVersion(MMSCWorker.tantau_sccsid));
            ws = new MMSCServer(port, rootDir, timeout, runSecure);
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
        System.out.println("eXtreme Test Tool (XTT) - MMSC Server - 724 Solutions");
        System.out.println("Version: MMSCServer: "+FunctionModule.parseVersion(MMSCServer.tantau_sccsid)+" MMSCWorker: "+FunctionModule.parseVersion(MMSCWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the MMSCServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure MMSCServer (default port 443)");
        System.out.println("");
        System.out.println("\t-r <directorypath>, --rootdir <directorypath>");
        System.out.println("\t\tSpecify the root directory of the MMSCServer");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the MMSCServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the MMSCServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of MMSCWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port
     * @throws Exception
     */

    public MMSCServer (int port, String rootDir, int timeout, boolean runSecure) throws Exception
    {
        super("MMSCServer-"+port);
        this.port = port;
        this.myRoot = new java.io.File(rootDir);
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
        ss.bind(new java.net.InetSocketAddress(port));

        synchronized(serverMap)
        {
            serverMap.put(port+"",this);
        }
        XTTProperties.printInfo("MMSCServer: listening on port:"+port+" dir:"+myRoot+" timout:"+timeout +" secure:"+runSecure);
    }

    public MMSCServer (int port, String rootDir, int timeout) throws Exception
    {
        this(port,rootDir,timeout,false);
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
            MMSCServer s=null;
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
            MMSCServer s=(MMSCServer)serverMap.get(port);
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
        XTTProperties.printDebug("MMSCServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that MMSCServer exits
        //this.join(); //please do that outside the MMSCServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("MMSCServer("+port+"): Killing workers start");
        Vector<MMSCWorker> close=new Vector<MMSCWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            MMSCWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<MMSCWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("MMSCServer("+port+"): Killing workers done");
    }


    /**
     * Part of Thread() interface, start the STI server
     */
    public void run()
    {
        try
        {
            Socket s=null;
            while (true)
            {
                // wait for a connection request to the STI server
                s = null;
                try
                {
                    s = ss.accept();
                } catch (java.io.IOException e)
                {
                    if (stopGracefully)
                    {
                        XTTProperties.printDebug("MMSCServer("+port+"): accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("MMSCServer("+port+"): accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }
                MMSCWorker w = null;
                XTTProperties.printDebug("MMSCServer: connection request received");
                MMSCWorker ws = new MMSCWorker(idCounter++,s,this,timeout,myRoot);
                XTTProperties.printDebug("MMSCServer("+port+"): starting new MMSCWorker: id " + ws.getWorkerId());
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
            XTTProperties.printDebug("MMSCServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
    }

}
