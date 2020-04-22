package com.mobixell.xtt;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * a very simple, multi-threaded ICAP server. Implementation notes are in ICAPServer.html, and also
 * as comments in the source code. In Standalone mode the services /reqmod and /respmod are  added by default.
 *
 * @version     $Id: ICAPServer.java,v 1.4 2010/07/09 10:50:32 mlichtin Exp $
 * @author Roger Soder
 */
public class ICAPServer extends Thread
{
    private int port;
    public  int getPort(){return port;}

    private ServerSocket ss = null;
    private static HashMap<String,ICAPServer> serverMap=new HashMap<String,ICAPServer>();

    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<ICAPWorker> runningthreads = new Vector<ICAPWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* the ICAP server's virtual root */
    private File myRoot = null;

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(ICAPWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}

    private LinkedHashMap<String,String> methodMap=new LinkedHashMap<String,String>();

    public static final String tantau_sccsid = "@(#)$Id: ICAPServer.java,v 1.4 2010/07/09 10:50:32 mlichtin Exp $";


    /**
     * Main method for ICAP server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = 1344;
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
                } else if (a[i].equalsIgnoreCase("--disableFullStreaming"))
                {
                    ICAPWorker.setEnableFullStreaming(false);
                } else if (a[i].equalsIgnoreCase("--enableKeepAlive"))
                {
                    ICAPWorker.setDefaultKeepAlive(true);
                } else if (a[i].equalsIgnoreCase("--flushLevel"))
                {
                    ICAPWorker.setFlushLevel(Integer.decode(a[++i]));
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
            ICAPServer ws = null;
            XTTProperties.printDebug("Version: ICAPServer: "+FunctionModule.parseVersion(ICAPServer.tantau_sccsid)+" ICAPWorker: "+FunctionModule.parseVersion(ICAPWorker.tantau_sccsid));
            HashMap<String,String> defaultMethods=new HashMap<String,String>();
            defaultMethods.put("/reqmod","REQMOD");
            defaultMethods.put("/respmod","RESPMOD");
            ws = new ICAPServer(port, rootDir, timeout, runSecure, defaultMethods);
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
        System.out.println("Version: ICAPServer: "+FunctionModule.parseVersion(ICAPServer.tantau_sccsid)+" ICAPWorker: "+FunctionModule.parseVersion(ICAPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the ICAPServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure ICAPServer (default port 443)");
        //System.out.println("");
        //System.out.println("\t-r <directorypath>, --rootdir <directorypath>");
        //System.out.println("\t\tSpecify the root directory of the ICAPServer");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the ICAPServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");
        System.out.println("\t--disableFullStreaming");
        System.out.println("\t\tTurn off full streaming which is on by default");
        System.out.println("");
        System.out.println("\t--enableKeepAlive");
        System.out.println("\t\tEnable server keep-alive of client connections.");
        System.out.println("");
        System.out.println("\t--flushLevel");
        System.out.println("\t\tSet the Flush Level in Full streaming mode, 7 bit value integer.");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }

    public String getMethod(String url)
    {
        if(url.startsWith("icap://"))
        {
            String[] urlparts=url.substring(7,url.length()).split("/",2);
            url="/"+urlparts[1];
        }
        XTTProperties.printDebug("ICAPServer.getMethod(): getting method for URL='"+url+"'");
        synchronized(methodMap)
        {
            String service=methodMap.get(url);
            if(service!=null)return service;
            
            Iterator<String> methods=methodMap.keySet().iterator();
            String keyurl=null;
            while(methods.hasNext())
            {
                keyurl=methods.next();
                if(url.startsWith(keyurl))
                {
                    return methodMap.get(keyurl);
                }
            }
            return null;
        }
    }

    /**
     * Create a new instance of the ICAPServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of ICAPWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port
     * @throws Exception
     */

    public ICAPServer (int port, String rootDir, int timeout, boolean runSecure) throws Exception
    {
        this(port,rootDir,timeout,runSecure,null);
    }
    public ICAPServer (int port, String rootDir, int timeout, boolean runSecure, HashMap<String,String> methods) throws Exception
    {
        super("ICAPServer-"+port);
        this.port = port;
        this.myRoot = new java.io.File(rootDir);
        this.timeout = timeout;
        if(methods!=null)
        {
            synchronized(methodMap)
            {
                methodMap.clear();
                methodMap.putAll(methods);
            }
        }

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
        XTTProperties.printInfo("ICAPServer: listening on port:"+port+" timout:"+timeout +" secure:"+runSecure);
    }

    public ICAPServer (int port, String rootDir, int timeout) throws Exception
    {
        this(port,rootDir,timeout,false);
    }

    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            ICAPServer s=(ICAPServer)serverMap.get(port);
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
            ICAPServer s=null;
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
            ICAPServer s=(ICAPServer)serverMap.get(port);
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
        XTTProperties.printDebug("ICAPServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that ICAPServer exits
        //this.join(); //please do that outside the ICAPServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("ICAPServer("+port+"): Killing workers start");
        Vector<ICAPWorker> close=new Vector<ICAPWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            ICAPWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<ICAPWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("ICAPServer("+port+"): Killing workers done");
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
                        XTTProperties.printDebug("ICAPServer("+port+"): accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("ICAPServer("+port+"): accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }
                XTTProperties.printDebug("ICAPServer: connection request received");
                ICAPWorker ws = new ICAPWorker(idCounter++,s,this,timeout,myRoot);
                XTTProperties.printDebug("ICAPServer("+port+"): starting new ICAPWorker: id " + ws.getWorkerId());
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
            XTTProperties.printDebug("ICAPServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
    }

}
