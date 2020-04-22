package com.mobixell.xtt;

import java.io.File;
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
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * a very simple, multi-threaded HTTP server. Implementation notes are in RTSPServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: RTSPServer.java,v 1.6 2010/07/09 10:50:32 mlichtin Exp $
 * @author Roger Soder
 */
public class RTSPServer extends Thread implements Runnable
{
    private final static int DEFAULTPORT = 554;
    private final static int DEFAULTSECUREPORT = 5554;

    private int port;
    public  int getPort(){return port;}
    /* is the server in secure mode */

    private boolean isSecure = false;

    private ServerSocket    tcpListener         = null;
    private DatagramSocket  udpListener         = null;
    
    private static HashMap<String,RTSPServer> serverMap=new HashMap<String,RTSPServer>();

    private char[] KEYSTOREPW = "xttxtt".toCharArray();

    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<RTSPWorker> runningthreads = new Vector<RTSPWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(RTSPWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}
    
    private RTSPServer thisserver=this;

    /* the server's virtual root */
    private File myRoot = null;

    public static final String tantau_sccsid = "@(#)$Id: RTSPServer.java,v 1.6 2010/07/09 10:50:32 mlichtin Exp $";

    /**
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = DEFAULTPORT;
        //int secport = DEFAULTSECUREPORT;
        int timeout = DEFAULTTIMEOUT;
        boolean runSecure=false;
        boolean portSet=false;
        String rootDir = ".";

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
                    portSet=true;
                } else if ((a[i].equalsIgnoreCase("--secure"))||(a[i].equalsIgnoreCase("-s")))
                {
                    runSecure=true;
                    if(!portSet)port=DEFAULTSECUREPORT;
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--timeout"))||(a[i].equalsIgnoreCase("-o")))
                {
                    timeout=Integer.parseInt(a[++i]);
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
            RTSPServer ws = null;
            XTTProperties.printDebug("Version: RTSPServer: "+FunctionModule.parseVersion(RTSPServer.tantau_sccsid)+" RTSPWorker: "+FunctionModule.parseVersion(RTSPWorker.tantau_sccsid));
            ws = new RTSPServer(port, runSecure, timeout, rootDir);
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
        System.out.println("Version: RTSPServer: "+FunctionModule.parseVersion(RTSPServer.tantau_sccsid)+" RTSPWorker: "+FunctionModule.parseVersion(RTSPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the RTSPServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure RTSPServer (default port 443)");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the RTSPServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify the timeout time in ms for the RTSPServer");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the RTSPServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of RTSPWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param listenport    port the server listens on in TCP and UDP
     * @param runSecure     is the TCP encrypted
     * @param timeout       timout on connections to the server
     * @param rootDir       server root directory
     * @throws Exception
     */
    public RTSPServer(int listenport, boolean runSecure, int timeout, String rootDir) throws Exception
    {
        super("RTSPServer-"+listenport);
        try
        {
            this.port = listenport;
            //this.secport=securelistenport;
            this.timeout = timeout;
            this.myRoot = new java.io.File(rootDir);

            //tcpListener         = new ServerSocket(port);
            udpListener         = new DatagramSocket(port);
    
            // listen for incoming non-secure HTTP requests
            if(!runSecure)
            {
                tcpListener = new ServerSocket();
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
    
                TrustManager[] trustAllCerts = new TrustManager[]
                {
                    new X509TrustManager()
                    {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers()
                        {
                            Throwable stack = new Throwable();
                            stack = stack.fillInStackTrace();
                            
                            XTTProperties.printException(stack);                        
                            return new java.security.cert.X509Certificate[0];
                        }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }            
                };
                
                //tmf.getTrustManagers()
                
                sslc.init(kmf.getKeyManagers(),trustAllCerts, new java.security.SecureRandom());
    
                SSLServerSocketFactory sslsocketfactory = sslc.getServerSocketFactory();
                sslss = (SSLServerSocket) sslsocketfactory.createServerSocket();
                
                tcpListener = sslss;
            }    
            tcpListener.setReuseAddress(true);
            tcpListener.bind(new java.net.InetSocketAddress(port));

            synchronized(serverMap)
            {
                serverMap.put(port+"",this);
            }
            XTTProperties.printInfo("RTSPServer: listening on tcp/udp ports:"+port+" secure:"+isSecure+" timout:"+timeout);
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
            throw ex;
        }
    }

    public RTSPServer(int timeout) throws Exception
    {
        this(DEFAULTPORT,false,timeout,".");
    }

    /**
     * Method to close the socket
     * @throws Exception
     */
    public void closeSocket() throws Exception
    {
        synchronized(serverMap)
        {
            tcpListener.close();
            udpListener.close();
            serverMap.remove(port+"");
        }
    }
    public static void closeSockets() throws Exception
    {
        synchronized(serverMap)
        {
            java.util.Iterator<String> it=serverMap.keySet().iterator();
            RTSPServer s=null;
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
            RTSPServer s=(RTSPServer)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }
    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            RTSPServer s=(RTSPServer)serverMap.get(port);
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
            RTSPServer s=(RTSPServer)serverMap.get(port);
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
     * @return It returns socket
     */
    
    public static Socket createSocketTCP(String port, Socket socket)
    {
        synchronized(serverMap)
        {
            RTSPServer s=(RTSPServer)serverMap.get(port);
            if(s==null)throw new NullPointerException("RTSPServer: No server running on port "+port);
            s.createTCPWorker(Integer.parseInt(port),socket);
            return socket;
        }
    }
    private void createTCPWorker(int port,Socket socket)
    {
        XTTProperties.printDebug("RTSPServer: creating TCP connection worker");
        RTSPWorker ws = new RTSPWorker(idCounter++,socket,thisserver,timeout,port,false,this.myRoot);
        XTTProperties.printDebug("RTSPServer("+port+"): starting new TCP RTSPWorker: id " + ws.getWorkerId());
        runningthreads.add(ws);
        ws.start();
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
        XTTProperties.printDebug("RTSPServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        tcpListener.close();
        udpListener.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that RTSPServer exits
        //this.join(); //please do that outside the RTSPServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("RTSPServer("+port+"): Killing workers start");
        Vector<RTSPWorker> close=new Vector<RTSPWorker>();
        RTSPWorker w=null;
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
        runningthreads = new Vector<RTSPWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("RTSPServer("+port+"): Killing workers done");
    }


    /**
     * Part of Thread() interface, start the Web server
     */
    public void run()
    {
        Thread ttcp=new TCPListener(tcpListener);
        ttcp.start();
        Thread tudp=new UDPListener(udpListener,getPort());
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
                            XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): TCP accept() interrupted by stop request");
                            break;
                        }
                        XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): TCP accept() interrupted - stopping workers");
                        stopWorkers();
                        throw e;
                    }
                    XTTProperties.printDebug("RTSPServer: TCP connection request received");
                    RTSPWorker ws = new RTSPWorker(idCounter++,socket,thisserver,timeout,ss.getLocalPort(),thisserver.myRoot);
                    XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): starting new TCP RTSPWorker: id " + ws.getWorkerId());
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
                XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): Exception in TCPListener.run(): " + ex.getMessage());
                return;
            }
        }
    }
    
    private class UDPListener extends Thread
    {
        DatagramSocket ss=null;
        private int localport=-1;
        public UDPListener(DatagramSocket socket, int localport)
        {
            this.ss=socket;
            this.localport=localport;
        }
        public void run()
        {
            try
            {
                DatagramPacket packet = null;
                byte[] buffer = null;
                RTSPWorker ws = null;
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
                            XTTProperties.printDebug("RTSPServer("+this.localport+"): UDP receive() interrupted by stop request");
                            break;
                        }
                        XTTProperties.printDebug("RTSPServer("+this.localport+"): UDP receive() interrupted - stopping workers");
                        stopWorkers();
                        throw e;
                    }
                    XTTProperties.printDebug("RTSPServer: UDP connection request received");
                    ws = new RTSPWorker(idCounter++,ss,packet,thisserver,timeout,this.localport,thisserver.myRoot);
                    XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): starting new UDP RTSPWorker: id " + ws.getWorkerId());
                    runningthreads.add(ws);
                    ws.start();
                }
                if (ss != null)
                {
                    ss.close();
                }
            } catch (java.io.IOException ex)
            {
                XTTProperties.printDebug("RTSPServer("+ss.getLocalPort()+"): Exception in UDPListener.run(): " + ex.getMessage());
                return;
            }
        }
    }

}
