package com.mobixell.xtt;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * a very simple, multi-threaded MSRP server. Implementation notes are in
 * MSRPServer.html, and also as comments in the source code
 * 
 * @version $Id: MSRPServer.java,v 1.2 2010/07/09 10:50:32 mlichtin Exp $
 * @author Anil Wadhai
 */
public class MSRPServer extends Thread implements Runnable
{
    private final static int                   DEFAULTPORT       = 493;
    private final static int                   DEFAULTSECUREPORT = 8493;
    private int                                port;
    private boolean                            isSecure          = false;
    private ServerSocket                       tcpListener       = null;
    private static HashMap<String, MSRPServer> serverMap         = new HashMap<String, MSRPServer>();
    private char[]                             KEYSTOREPW        = "xttxtt".toCharArray();
    private boolean                            stopGracefully    = false;



    /*
     * A set of worker threads is available. This is where they live when they
     * are idle
     */
    private Vector<MSRPWorker>                 runningthreads    = new Vector<MSRPWorker>();
    private static int                         idCounter         = 0;
    /* timeout on client connections */
    public static final int                    DEFAULTTIMEOUT    = 30000;
    private int                                timeout           = DEFAULTTIMEOUT;
    private int                                workerCounter     = 0;
    private MSRPServer                         thisserver        = this;
  
    public int getPort()
    {
        return port;
    }

    public int getIdCounter()
    {
        return idCounter;
    }

    public static void resetWorkerId()
    {
        idCounter = 0;
    }

    public synchronized void addWorker()
    {
        workerCounter++;
    }

    public synchronized void removeWorker(MSRPWorker w)
    {
        workerCounter--;
        runningthreads.remove(w);
    }

    public synchronized int getNumWorkers()
    {
        return workerCounter;
    }

    public static HashMap<String, MSRPServer> getServerMap()
    {
        return serverMap;
    }

    public Vector<MSRPWorker> getMSRPWorkers()
    {
        return runningthreads;
    }

    /**
     * Main method for MSRP server
     * 
     * @param a
     *            may hold port number, protocol etc. upon which to listen for
     *            TCP requests
     * @throws Exception
     */
    public static void main(String[] a)
    {
        int port             = DEFAULTPORT;
        int timeout          = DEFAULTTIMEOUT;
        boolean runSecure    = false;
        boolean portSet      = false;
        String tracing       = "";
        String tracingFormat = null;
        try
        {
            for (int i = 0; i<a.length; i++) // Loop around the arguments
            {
                if(a[i].equalsIgnoreCase("--help")) // Check each argument
                {
                    showHelp();
                } else if((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    port = Integer.parseInt(a[++i]);
                    portSet = true;
                } else if((a[i].equalsIgnoreCase("--secure"))||(a[i].equalsIgnoreCase("-s")))
                {
                    runSecure = true;
                    if(!portSet)
                        port = DEFAULTSECUREPORT;
                } else if((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing = a[++i];
                } else if((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat = a[++i];
                } else if((a[i].equalsIgnoreCase("--timeout"))||(a[i].equalsIgnoreCase("-o")))
                {
                    timeout = Integer.parseInt(a[++i]);
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
            MSRPServer ws = null;
            XTTProperties.printDebug("Version: MSRPServer: "+FunctionModule.parseVersion(MSRPServer.tantau_sccsid)+" MSRPWorker: "+FunctionModule.parseVersion(MSRPWorker.tantau_sccsid));
            ws = new MSRPServer(port, runSecure, timeout);
            ws.run();
        } catch (Exception e)
        {
            System.out.println("Error: Invalid command line option");
            XTTProperties.printException(e);
            showHelp(); // If the command line options were bad, show the help
        }
    }

    private static void showHelp()
    {
        System.out.println("eXtreme Test Tool (XTT) - Web Server - 724 Solutions");
        System.out.println("Version: MSRPServer: "+FunctionModule.parseVersion(MSRPServer.tantau_sccsid)+" MSRPWorker: "+FunctionModule.parseVersion(MSRPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the MSRPServer on (default 493)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure MSRPServer (default port 8493)");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the MSRPServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify the timeout time in ms for the MSRPServer");
        System.out.println("");

        System.exit(1); // Exit and fail if you had to show this help
    }

    /**
     * Create a new instance of the MSRPServer class, listening on the specified
     * non-secure port for HTTP requests. The configured number of MSRPWorker
     * threads are started, and stored to the "threads" Vector A ServerSocket is
     * started to listen for incoming requests, which will then be dispatched to
     * an available worker thread
     * 
     * @param listenport
     *            port the server listens on in TCP and TLS
     * @param runSecure
     *            is the TCP encrypted
     * @param timeout
     *            timout on connections to the server
     * @throws Exception
     */
    public MSRPServer(int listenport, boolean runSecure, int timeout) throws Exception
    {
        super("MSRPServer-"+listenport);
        try
        {
            this.port     = listenport;
            this.timeout  = timeout;
            this.isSecure = runSecure;
            if(!runSecure)
            {
                tcpListener = new ServerSocket();
            } else
            {
                SSLServerSocket sslss = null;
                KeyStore keystore = null;
                KeyManagerFactory kmf = null;
                SSLContext sslc = null;

                sslc = SSLContext.getInstance("SSL");
                kmf = KeyManagerFactory.getInstance("SunX509");
                keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream("key/xttkeystore"), KEYSTOREPW);
                kmf.init(keystore, KEYSTOREPW);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(keystore);

                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
                {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        Throwable stack = new Throwable();
                        stack = stack.fillInStackTrace();

                        XTTProperties.printException(stack);
                        return new java.security.cert.X509Certificate[0];
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                    {
                    }
                } };

                sslc.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
                SSLServerSocketFactory sslsocketfactory = sslc.getServerSocketFactory();
                sslss = (SSLServerSocket) sslsocketfactory.createServerSocket();
                tcpListener = sslss;
            }
            tcpListener.setReuseAddress(true);
            tcpListener.bind(new java.net.InetSocketAddress(port));

            synchronized (serverMap)
            {
                serverMap.put(port+"", this);
            }
            XTTProperties.printInfo("MSRPServer: listening on tcp ports:"+port+" secure:"+isSecure+" timout:"+timeout);
        } catch (Exception ex)
        {
            try
            {
                tcpListener.close();
            } catch (Exception cle)
            {
            }
            throw ex;
        }
    }

    public MSRPServer(int timeout) throws Exception
    {
        this(DEFAULTPORT, false, timeout);
    }

    public void closeSocket() throws Exception
    {
        synchronized (serverMap)
        {
            tcpListener.close();
            serverMap.remove(port+"");
        }
    }

    public static void closeSockets() throws Exception
    {
        synchronized (serverMap)
        {
            java.util.Iterator<String> it = serverMap.keySet().iterator();
            MSRPServer s = null;
            String currentPort = null;
            while (it.hasNext())
            {
                currentPort = it.next();
                s = serverMap.get(currentPort);
                s.stopGracefully();
            }
            serverMap.clear();
        }
    }

    public static void closeSocket(String port) throws Exception
    {
        synchronized (serverMap)
        {
            MSRPServer s = (MSRPServer) serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }

    public static int getLastWorkerId(String port)
    {
        synchronized (serverMap)
        {
            MSRPServer s = (MSRPServer) serverMap.get(port);
            return s.getIdCounter();
        }
    }

    public static Socket createSocketTCP(String port, Socket socket)
    {
        synchronized (serverMap)
        {
            MSRPServer s = (MSRPServer) serverMap.get(port);
            s.createTCPWorker(Integer.parseInt(port), socket);
            return socket;
        }
    }

    private void createTCPWorker(int port, Socket socket)
    {
        XTTProperties.printDebug("MSRPServer: creating TCP connection worker");
        MSRPWorker ws = new MSRPWorker(idCounter++, socket, thisserver, timeout, port, true);
        XTTProperties.printDebug("MSRPServer("+port+"): starting new MSRPWorker: id "+ws.getWorkerId());
        runningthreads.add(ws);
        ws.start();
    }

    public static boolean checkSockets()
    {
        synchronized (serverMap)
        {
            return serverMap.isEmpty();
        }
    }

    public boolean checkSocket()
    {
        synchronized (serverMap)
        {
            return (tcpListener==null);
        }
    }

    public static boolean checkSocket(String port)
    {
        synchronized (serverMap)
        {
            return (serverMap.get(port)==null);
        }
    }
    
    /**
     * returning the socket of current worker of specified server.
     * required server get by providing the serverPort
     * 
     * @param port
     * @return socket
     */
    public static Socket getSocket(String port)
    {
        MSRPServer s = serverMap.get(port);
        MSRPWorker mw = s.getMSRPWorkers().get(0); 
        return mw.getCurrentTCPSocket();
    }

    /**
     * Stop the msrp server
     * @throws Exception
     */
    public void stopGracefully() throws java.lang.Exception
    {
        stopGracefully = true;
        XTTProperties.printDebug("MSRPServer("+port+"): close ServerSocket and request stop");
        tcpListener.close();
        stopWorkers();

        synchronized (serverMap)
        {
            serverMap.remove(this);
        }
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("MSRPServer("+port+"): Killing workers start");
        Vector<MSRPWorker> close = new Vector<MSRPWorker>();
        MSRPWorker w = null;
        for (int i = 0; i<runningthreads.size(); ++i)
        {
            w = runningthreads.get(i);
            close.add(w);
            w.setStop();
        }
        for (int i = 0; i<close.size(); ++i)
        {
            w = close.get(i);
            w.doStop();
            // wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex)
            {
            }
        }
        runningthreads = new Vector<MSRPWorker>(); // bye-bye threads table,
                                                   // ready to start up again
        XTTProperties.printDebug("MSRPServer("+port+"): Killing workers done");
    }

    /**
     * Part of Thread() interface, start the msrp server
     */
    public void run()
    {
        Thread ttcp = new TCPListener(tcpListener);
        ttcp.start();
    }

    private class TCPListener extends Thread
    {
        ServerSocket ss = null;

        public TCPListener(ServerSocket socket)
        {
            this.ss = socket;
        }

        public void run()
        {
            try
            {
                Socket socket = null;
                while (true)
                {
                    // wait for a connection request to the Web server
                    socket = null;
                    try
                    {
                        socket = ss.accept();
                    } catch (java.io.IOException e)
                    {
                        if(stopGracefully)
                        {
                            XTTProperties.printDebug("MSRPServer("+ss.getLocalPort()+"): TCP accept() interrupted by stop request");
                            break;
                        }
                        XTTProperties.printDebug("MSRPServer("+ss.getLocalPort()+"): TCP accept() interrupted - stopping workers");
                        stopWorkers();
                        throw e;
                    }
                    XTTProperties.printDebug("MSRPServer: TCP connection request received");
                    MSRPWorker ws = new MSRPWorker(idCounter++, socket, thisserver, timeout, ss.getLocalPort());
                    XTTProperties.printDebug("MSRPServer("+ss.getLocalPort()+"): starting new TCP MSRPWorker: id "+ws.getWorkerId());
                    runningthreads.add(ws);
                    ws.start();
                }
                if(socket!=null)
                {
                    socket.close();
                }
                if(ss!=null)
                {
                    ss.close();
                }
            } catch (java.io.IOException ex)
            {
                XTTProperties.printDebug("MSRPServer("+ss.getLocalPort()+"): Exception in TCPListener.run(): "+ex.getMessage());
                return;
            }
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: MSRPServer.java,v 1.2 2010/07/09 10:50:32 mlichtin Exp $";
}
