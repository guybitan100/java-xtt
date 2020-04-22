package com.mobixell.xtt;

import java.io.File;
import java.io.FileNotFoundException;
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
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mobixell.xtt.util.OSUtils;

import java.lang.reflect.Constructor;

/**
 * a very simple, multi-threaded HTTP server. Implementation notes are in WebServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: WebServer.java,v 1.14 2010/07/09 10:50:32 mlichtin Exp $
 * @author Gavin Cattell & Roger Soder
 */
public class WebServer extends Thread implements WebHttpConstants, Runnable, javax.net.ssl.HandshakeCompletedListener
{
    private int port;
    public  int getPort(){return port;}
    private String ipAddr = "";
    public  String getIp(){return ipAddr;}
    private ServerSocket ss = null;
    private static HashMap<String,WebServer> serverMap=new HashMap<String,WebServer>();

    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<WebWorker> runningthreads = new Vector<WebWorker>();

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* the web server's virtual root */
    private File myRoot = null;
    
    /* is the server in secure mode */
    private boolean isSecure = false;

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(WebWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}

    private WebWorkerExtension webWorkerExtension = null;

    public static final String tantau_sccsid = "@(#)$Id: WebServer.java,v 1.14 2010/07/09 10:50:32 mlichtin Exp $";

    /**
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
    	String ipAddr = null;
        int port    = 80;
        String rootDir = ".";
        int timeout = 10000;
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
                    
                } else if ((a[i].equalsIgnoreCase("--ip"))||(a[i].equalsIgnoreCase("-i")))
                {
                	ipAddr= a[++i];
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
            WebServer ws = null;
            XTTProperties.printDebug("Version: WebServer: "+FunctionModule.parseVersion(WebServer.tantau_sccsid)+" WebWorker: "+FunctionModule.parseVersion(WebWorker.tantau_sccsid));
            ws = new WebServer(ipAddr,port, rootDir, timeout, runSecure);
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
        System.out.println("Version: WebServer: "+FunctionModule.parseVersion(WebServer.tantau_sccsid)+" WebWorker: "+FunctionModule.parseVersion(WebWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-i <portnum>, --ip <portnum>");
        System.out.println("\t\tSpecify the ip to bind the WebServer on (default local host)");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the WebServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure WebServer (default port 443)");
        System.out.println("");
        System.out.println("\t-r <directorypath>, --rootdir <directorypath>");
        System.out.println("\t\tSpecify the root directory of the WebServer");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the WebServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify the timeout time in ms for the WebServer");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the WebServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of WebWorker threads are started, and stored to the
     * "threads" Vector
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port
     * @throws Exception
     */

    public WebServer(String ip,int port, String rootDir, int timeout, boolean runSecure) throws Exception
    {
        super("WebServer-"+port);
        this.ipAddr=ip;
        this.port = port;
        this.myRoot = new java.io.File(rootDir);

        if(!myRoot.isDirectory())
        {
            throw new FileNotFoundException("Not a directory: "+rootDir);
        }

        this.timeout = timeout;
        this.isSecure = runSecure;

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
            
            sslc.init(kmf.getKeyManagers(),trustAllCerts, new java.security.SecureRandom());

            SSLServerSocketFactory sslsocketfactory = sslc.getServerSocketFactory();
            sslss = (SSLServerSocket) sslsocketfactory.createServerSocket();
            
            ss = sslss;
        }

        ss.setReuseAddress(true);
       
        if (ipAddr=="null" || ipAddr==null) 
        {
        	ip= OSUtils.getIpAddr();
        	ss.bind(new java.net.InetSocketAddress(port));
        	
        }
        else
        {
        	ss.bind(new java.net.InetSocketAddress(ipAddr,port));
        }
        
        synchronized(serverMap)
        {
            serverMap.put(port+"",this);
        }
        XTTProperties.printInfo("WebServer: listening on ip: " + ip+ " port: "+ port + " dir:"+myRoot+" timout:"+timeout +" secure:"+runSecure);
    }

    public WebServer (int port, String rootDir, int timeout) throws Exception
    {
        this(null,port,rootDir,timeout,false);
    }
    public WebServer (int port, String rootDir, int timeout, boolean runSecure) throws Exception
    {
        this(null,port,rootDir,timeout,false);
    }
    public static void setRequestClientCertificate(String port, boolean request)
    {
        synchronized(serverMap)
        {
            WebServer s=(WebServer)serverMap.get(port);
            s.setRequestClientCertificate(request);
        }
    }

    public void setRequestClientCertificate(boolean request)
    {        
        try
        {
            SSLServerSocket sslss = (SSLServerSocket) ss;
            sslss.setWantClientAuth(request);
            XTTProperties.printInfo("requestClientCertificate: " + request);
        }
        catch(ClassCastException cce)
        {
            XTTProperties.printFail("WebServer: You can't ask for client certificates on a non-secure webserver");
            XTTProperties.setTestStatus(XTTProperties.FAILED);    
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
            WebServer s=null;
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
            WebServer s=(WebServer)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }
    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            WebServer s=(WebServer)serverMap.get(port);
            return s.getIdCounter();
        }
    }
    public static void setDefaultWebWorkerExtension(String port, String extensionClass) throws Exception
    {
        synchronized(serverMap)
        {
            WebServer s=(WebServer)serverMap.get(port);
            s.setDefaultWebWorkerExtension(extensionClass);
        }
    }
    private void setDefaultWebWorkerExtension(String extensionClass) throws Exception
    {
        if(extensionClass==null||extensionClass.equals(""))
        {
            webWorkerExtension=null;
        } else
        {
            ClassLoader loader=this.getClass().getClassLoader();
            Class<?> c=loader.loadClass(extensionClass);
            Constructor<?> webWorkerExtensionConstructor=c.getConstructor(new Class[0]);
            webWorkerExtension=(WebWorkerExtension)webWorkerExtensionConstructor.newInstance(new Object[0]);
        }
    }
    public WebWorkerExtension getDefaultWebWorkerExtension()
    {
        return webWorkerExtension;
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
        XTTProperties.printDebug("WebServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that WebServer exits
        //this.join(); //please do that outside the WebServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("WebServer("+port+"): Killing workers start");
        Vector<WebWorker> close=new Vector<WebWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            WebWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<WebWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("WebServer("+port+"): Killing workers done");
    }


    public void handshakeCompleted(javax.net.ssl.HandshakeCompletedEvent event) 
    {
        try
        {
            java.security.cert.Certificate[] certs = new java.security.cert.Certificate[0];
            certs=event.getPeerCertificates();
            XTTProperties.printInfo("WebServer("+port+"): "+certs.length+" client certificates");
            java.security.cert.X509Certificate x509=null;
            for(int i=0;i<certs.length;i++)
            {
                XTTProperties.printVerbose("WebServer("+port+"): Client Certificate["+i+"]: "+certs[i].getType());
                if(certs[i].getType().equals("X.509"))
                {
                    x509=(java.security.cert.X509Certificate)certs[i];
                    XTTProperties.printDebug(  "WebServer("+port+"): "+i+":serial: "+x509.getSerialNumber() );
                    XTTProperties.printVerbose("WebServer("+port+"): "+i+":algorithm: "+x509.getSigAlgName());
                    XTTProperties.printVerbose("WebServer("+port+"): "+i+":X500Principal: "+x509.getIssuerX500Principal().getName());
                    XTTProperties.printDebug(  "WebServer("+port+"): "+i+":DN: "+x509.getSubjectDN());
                    XTTProperties.printVerbose("WebServer("+port+"): "+i+":NotAfter: " + x509.getNotAfter());
                }
            }
            XTTProperties.printVerbose("WebServer("+port+"): END client certificates");        
        }
        catch (javax.net.ssl.SSLPeerUnverifiedException sslpue)
        {
            XTTProperties.printDebug(  "WebServer("+port+"): "+sslpue.getClass().getName());
            XTTProperties.printDebugException(sslpue);
        }
    }

    /**
     * Part of Thread() interface, start the Web server
     */
    public void run()
    {
        try
        {
            Socket s=null;
            while (true)
            {
                // wait for a connection request to the Web server
                s = null;
                try
                {
                    s = ss.accept();
                    if(isSecure)
                    {
                        SSLSocket ssls = (SSLSocket) s;
                        ssls.addHandshakeCompletedListener(this);    
                    }
                } catch (java.io.IOException e)
                {
                    if (stopGracefully)
                    {
                        XTTProperties.printDebug("WebServer("+port+"): accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("WebServer("+port+"): accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }

                XTTProperties.printDebug("WebServer: connection request received");
                WebWorker ws = new WebWorker(idCounter++,s,this,timeout,myRoot);
                XTTProperties.printDebug("WebServer("+port+"): starting new WebWorker: id " + ws.getWorkerId());
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
            XTTProperties.printDebug("WebServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
    }

}
