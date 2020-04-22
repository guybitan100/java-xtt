package com.mobixell.xtt.radius;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Vector;
import java.util.HashMap;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.FunctionModule;

/**
 * a very simple, multi-threaded HTTP server. Implementation notes are in RadiusServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: RadiusServer.java,v 1.6 2008/03/17 08:48:12 rsoder Exp $
 * @author Gavin Cattell & Roger Soder
 */
public class RadiusServer extends Thread
{
    private int port;
    public  int getPort(){return port;}

    private byte[] mySecret;
    public  byte[] getSecret(){return mySecret;}

    private DatagramSocket ss = null;
    private static HashMap<String,RadiusServer> serverMap=new HashMap<String,RadiusServer>();


    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<RadiusWorker> runningthreads = new Vector<RadiusWorker>();

    private static int idCounter = 0;
    public static void resetWorkerId(){idCounter = 0;}

    /* the Radius server's virtual root */

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(RadiusWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}


    public static final String tantau_sccsid = "@(#)$Id: RadiusServer.java,v 1.6 2008/03/17 08:48:12 rsoder Exp $";


    /**
     * Main method for Radius server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = 1812;
        int timeout = DEFAULTTIMEOUT;
        String tracing="";
        String sharedsecret="tttester";
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
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--secret"))||(a[i].equalsIgnoreCase("-s")))
                {
                    sharedsecret=a[++i];
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
            RadiusServer ws = null;
            XTTProperties.printDebug("Version: RadiusServer: "+FunctionModule.parseVersion(RadiusServer.tantau_sccsid)+" RadiusWorker: "+FunctionModule.parseVersion(RadiusWorker.tantau_sccsid));
            ws = new RadiusServer(port, timeout, sharedsecret);
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
        System.out.println("eXtreme Test Tool (XTT) - Radius Server - Mobixell");
        System.out.println("Version: RadiusServer: "+FunctionModule.parseVersion(RadiusServer.tantau_sccsid)+" RadiusWorker: "+FunctionModule.parseVersion(RadiusWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the RadiusServer on (default 80)");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the RadiusServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }

    /**
     * Create a new instance of the RadiusServer class, listening on the specified
     * non-secure port for HTTP requests.
     * The configured number of RadiusWorker threads are started, and stored to the
     * "threads" Vector
     * A DatagramSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port
     * @throws Exception
     */

    public RadiusServer (int port, int timeout, String secret) throws Exception
    {
        super("RadiusServer-"+port);
        this.port = port;
        mySecret=secret.getBytes(XTTProperties.getCharSet());

        ss = new DatagramSocket(port);

        synchronized(serverMap)
        {
            serverMap.put(port+"",this);
        }
        XTTProperties.printInfo("RadiusServer: listening on port:"+port+" timout:"+timeout);
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
            RadiusServer s=null;
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
            RadiusServer s=(RadiusServer)serverMap.get(port);
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
        XTTProperties.printDebug("RadiusServer("+port+"): close DatagramSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        //stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that RadiusServer exits
        //this.join(); //please do that outside the RadiusServer
    }

    /*
    private void stopWorkers()
    {
        XTTProperties.printDebug("RadiusServer("+port+"): Killing workers start");
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            RadiusWorker w =runningthreads.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<RadiusWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("RadiusServer("+port+"): Killing workers done");
    }*/


    /**
     * Part of Thread() interface, start the Radius server
     */
    public void run()
    {
        byte[] buf = new byte[4096]; //Max Radius Packet size
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try
        {
            while(true)
            {
                ss.receive(packet);
                new RadiusWorker(idCounter++,packet,ss,this).start();

                buf = new byte[4096];
                packet = new DatagramPacket(buf, buf.length);
            }
        }
        catch(SocketException e)
        {
            //
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }

}
