package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
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
 * a very simple, multi-threaded HTTP server. Implementation notes are in SIPServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: SIPServer.java,v 1.14 2010/07/09 10:50:32 mlichtin Exp $
 * @author Roger Soder
 */
public class SIPServer extends Thread implements Runnable
{
    private final static int DEFAULTPORT = 4060;
    private final static int DEFAULTSECUREPORT = 4061;

    private int port;
    public  int getPort(){return port;}
    private int secport;
    public  int getSecurePort(){return secport;}

    private ServerSocket    tcpListener         = null;
    private ServerSocket    tcpListenerSecure   = null;
    private DatagramSocket  udpListener         = null;
    
    private static HashMap<String,SIPServer> serverMap=new HashMap<String,SIPServer>();

    private String KEYSTORE = "xttserver";
    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private char[] KEYPW="xttxtt".toCharArray();

    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<SIPWorker> runningthreads = new Vector<SIPWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(SIPWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}
    
    private SIPServer thisserver=this;

    public static final String tantau_sccsid = "@(#)$Id: SIPServer.java,v 1.14 2010/07/09 10:50:32 mlichtin Exp $";

    /**
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = DEFAULTPORT;
        int secport = DEFAULTSECUREPORT;
        int timeout = DEFAULTTIMEOUT;

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
                } else if ((a[i].equalsIgnoreCase("--secure"))||(a[i].equalsIgnoreCase("-s")))
                {
                    secport=Integer.parseInt(a[++i]);
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
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
            SIPServer ws = null;
            XTTProperties.printDebug("Version: SIPServer: "+FunctionModule.parseVersion(SIPServer.tantau_sccsid)+" SIPWorker: "+FunctionModule.parseVersion(SIPWorker.tantau_sccsid));
            ws = new SIPServer(port, secport, timeout);
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
        System.out.println("Version: SIPServer: "+FunctionModule.parseVersion(SIPServer.tantau_sccsid)+" SIPWorker: "+FunctionModule.parseVersion(SIPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the SIPServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure SIPServer (default port 443)");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the SIPServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify the timeout time in ms for the SIPServer");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the SIPServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of SIPWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param listenport
     * @param securelistenport
     * @param timeout
     * @throws Exception
     */
    public SIPServer(int listenport, int securelistenport, int timeout) throws Exception
    {
        super("SIPServer-"+listenport);
        try
        {
            this.port = listenport;
            this.secport=securelistenport;
            this.timeout = timeout;
    
            tcpListener = new ServerSocket();
            tcpListener.setReuseAddress(true);
            tcpListener.bind(new java.net.InetSocketAddress(port));
            udpListener = new DatagramSocket();
            udpListener.setReuseAddress(true);
            udpListener.bind(new java.net.InetSocketAddress(port));

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
    
            tcpListenerSecure   = sslss;
            tcpListenerSecure.setReuseAddress(true);
            tcpListenerSecure.bind(new java.net.InetSocketAddress(secport));

            synchronized(serverMap)
            {
                serverMap.put(port+"",this);
            }
            XTTProperties.printInfo("SIPServer: listening on tcp/udp ports:"+port+" secureport:"+secport+" timout:"+timeout);
        } catch (Exception ex)
        {
            try
            {
                tcpListener.close();
            } catch (Exception cle){}
            try
            {
                udpListener.close();
            } catch (Exception cle){}
            try
            {
                tcpListenerSecure.close();
            } catch (Exception cle){}
            throw ex;
        }
    }

    public SIPServer(int timeout) throws Exception
    {
        this(DEFAULTPORT,DEFAULTSECUREPORT,timeout);
    }

    /**
     * Method to clsoe the socket
     * @throws Exception
     */
    public void closeSocket() throws Exception
    {
        synchronized(serverMap)
        {
            tcpListener.close();
            tcpListenerSecure.close();
            udpListener.close();
            serverMap.remove(port+"");
        }
    }
    public static void closeSockets() throws Exception
    {
        synchronized(serverMap)
        {
            java.util.Iterator<String> it=serverMap.keySet().iterator();
            SIPServer s=null;
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
    /**
     * Method to close socket
     * @param port - port number
     * @throws Exception
     */
    public static void closeSocket(String port) throws Exception
    {
        synchronized(serverMap)
        {
            SIPServer s=(SIPServer)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }
    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            SIPServer s=(SIPServer)serverMap.get(port);
            return s.getIdCounter();
        }
    }
    
    /**
     * Method to create UDP socket
     * @param port - port number
     * @return - socket
     */
    public static DatagramSocket createSocketUDP(String port)
    {
        synchronized(serverMap)
        {
            SIPServer s=(SIPServer)serverMap.get(port);
            return s.getUDPSocket();
        }
    }
    private DatagramSocket getUDPSocket()
    {
        return udpListener;
    }
    
    /**
     * Method to create TCP socket
     * @param port -port number
     * @param socket -socket
     * @return It returns SIPWorker
     */
    public static SIPWorker createSocketTCP(String port, Socket socket)
    {
        synchronized(serverMap)
        {
            SIPServer s=(SIPServer)serverMap.get(port);
            if(s==null)
            {
                XTTProperties.printWarn("SIPServer: no server found running on port="+port);
                return null;
            }
            return s.createTCPWorker(Integer.parseInt(port),socket);
        }
    }
    
    /**
     * Method to create TCP worker thead
     * @param port - port number
     * @param socket
     * @return
     */
    private SIPWorker createTCPWorker(int port,Socket socket)
    {
        XTTProperties.printDebug("SIPServer: creating TCP connection worker");
        SIPWorker ws = new SIPWorker(idCounter++,socket,thisserver,timeout,port,true);
        XTTProperties.printDebug("SIPServer("+port+"): starting new TCP SIPWorker: id " + ws.getWorkerId());
        runningthreads.add(ws);
        ws.start();
        return ws;
    }
    public static SIPWorker getWorker(String port)
    {
        synchronized(serverMap)
        {
            SIPServer s=(SIPServer)serverMap.get(port);
            return s.runningthreads.get(0);
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
        XTTProperties.printDebug("SIPServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        tcpListener.close();
        tcpListenerSecure.close();
        udpListener.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that SIPServer exits
        //this.join(); //please do that outside the SIPServer
    }
    
    /**
     * Method to stop worker threads
     */
    private void stopWorkers()
    {
        XTTProperties.printDebug("SIPServer("+port+"): Killing workers start");
        Vector<SIPWorker> close=new Vector<SIPWorker>();
        SIPWorker w=null;
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
        runningthreads = new Vector<SIPWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("SIPServer("+port+"): Killing workers done");
    }


    /**
     * Part of Thread() interface, start the Web server
     */
    public void run()
    {
        Thread ttcp=new TCPListener(tcpListener);
        ttcp.start();
        Thread tsec=new TCPListener(tcpListenerSecure);
        tsec.start();
        Thread tudp=new UDPListener(udpListener);
        tudp.start();
    }

    private class TCPListener extends Thread
    {
        ServerSocket ss=null;
        public TCPListener(ServerSocket socket)
        {
            this.ss=socket;
        }
        public void run()
        {
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
                            XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): TCP accept() interrupted by stop request");
                            break;
                        }
                        XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): TCP accept() interrupted - stopping workers");
                        stopWorkers();
                        throw e;
                    }
                    XTTProperties.printDebug("SIPServer: TCP connection request received");
                    SIPWorker ws = new SIPWorker(idCounter++,socket,thisserver,timeout,ss.getLocalPort());
                    XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): starting new TCP SIPWorker: id " + ws.getWorkerId());
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
                XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): Exception in TCPListener.run(): " + ex.getMessage());
                return;
            }
        }
    }
    
    private class UDPListener extends Thread
    {
        DatagramSocket ss=null;
        public UDPListener(DatagramSocket socket)
        {
            this.ss=socket;
        }
        public void run()
        {
            try
            {
                DatagramPacket packet = null;
                byte[] buffer = null;
                SIPWorker ws = null;
                while (true)
                {
                    // wait for a connection request to the Web server
                    buffer = new byte[65535];
                    packet = new DatagramPacket(buffer, buffer.length);
                    try
                    {
                        ss.receive(packet);
                    } catch (java.io.IOException e)
                    {
                        if (stopGracefully)
                        {
                            XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): UDP receive() interrupted by stop request");
                            break;
                        }
                        XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): UDP receive() interrupted - stopping workers");
                        stopWorkers();
                        throw e;
                    }
                    XTTProperties.printDebug("SIPServer: UDP connection request received");
                    ws = new SIPWorker(idCounter++,ss,packet,thisserver,timeout,ss.getLocalPort());
                    XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): starting new UDP SIPWorker: id " + ws.getWorkerId());
                    runningthreads.add(ws);
                    ws.start();
                }
                if (ss != null)
                {
                    ss.close();
                }
            } catch (java.io.IOException ex)
            {
                XTTProperties.printDebug("SIPServer("+ss.getLocalPort()+"): Exception in UDPListener.run(): " + ex.getMessage());
                return;
            }
        }
    }

}
