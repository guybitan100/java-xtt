package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * a very simple, multi-threaded SMTP server. Implementation notes are in SMTPServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: SMTPServer.java,v 1.7 2010/07/09 10:50:32 mlichtin Exp $
 */
public class SMTPServer implements Runnable
{
    private int port;
    private static ServerSocket ss = null;
    /**
     * Check if the server is running.
     * @return boolean  true if it is running.
     */
    public static boolean checkSocket()
    {
        return (ss==null);
    }
    /**
     * Close the server socket and shutting it down this way., should use stopGracefully
     * @throws Exception
     */
    public static void closeSocket() throws Exception
    {
        ss.close();
    }

    private boolean stopGracefully = false;

    public static final int DEFAULTTIMEOUT = 30000;
    public static int timeout = DEFAULTTIMEOUT;

    private final static String serverName="SMTP - 724 Solutions XTT Test-Server";
    private static String serverHost="";

    private static int idCounter=0;
    public static void resetWorkerId(){idCounter = 0;}


    /* A set of worker threads is available. This is where they live when
     * they are idle */
    static Vector<SMTPWorker> runningthreads = new Vector<SMTPWorker>();

    /**
     * Return the full server gretting string.
     * @return String  contains the greeting String in the format "hostname servername"
     */
    public static String getServerName()
    {
        String spacer="";
        if(!serverHost.equals(""))spacer=" ";
        return serverHost+spacer+serverName;
    }

    /**
     * Return the hostname.
     * @return String  contains the hostname
     */
    public static String getHostName()
    {
        return serverHost;
    }

    /**
     * Main method for SMTP server
     * @param a     may hold port number upon which to listen for non-secure
     *              SMTP requests
     * @throws Exception
     */
    /*
    public static void main (String [] a){
        if(a.length != 3)
        {
            System.out.println("int port, String hostname, int timeout");
            System.exit(1);
        }
        try
        {
            XTTProperties.setTracing(XTTProperties.DEBUG);
            SMTPServer ws = null;
            ws = new SMTPServer(Integer.parseInt(a[0]), a[1], Integer.parseInt(a[2]));
            ws.run();
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
            //e.printStackTrace();
        }
    }
    */
    public static void main (String [] a)
    {
        int usePort=25;
        String hostname="";
        String tracing="";
        if (a.length == 0)
        {
            showHelp();
        }
        try
        {
            int timeout = DEFAULTTIMEOUT;
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                } else if ((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    usePort=Integer.parseInt(a[++i]);
                } else if ((a[i].equalsIgnoreCase("--name"))||(a[i].equalsIgnoreCase("-n")))
                {
                    hostname=a[++i];
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
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
            SMTPServer ws = null;
            XTTProperties.printDebug("Version: SMTPServer: "+FunctionModule.parseVersion(SMTPServer.tantau_sccsid)+" SMTPWorker: "+FunctionModule.parseVersion(SMTPWorker.tantau_sccsid));
            ws = new SMTPServer(usePort, hostname, timeout);
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
        System.out.println("eXtreme Test Tool (XTT) - SMTP Simulator - 724 Solutions");
        System.out.println("Version: SMTPServer: "+FunctionModule.parseVersion(SMTPServer.tantau_sccsid)+" SMTPWorker: "+FunctionModule.parseVersion(SMTPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the SMTP server on, defualt 25");
        System.out.println("");
        System.out.println("\t-n <hostname>, --name <hostname>");
        System.out.println("\t\tSpecify the hostname of the SMTP server");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the SMTP server, default 30000");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }

    /**
     * Create a new instance of the SMTPServer class, listening on the specified
     * port for SMTP requests.
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port  integer containing the port to start the server on
     * @param hostname  the host name the server shall respond
     * @param timeout  integer containing the timout on connections in miliseconds
     * @throws Exception
     */
    public SMTPServer (int port, String hostname, int timeout) throws Exception
    {
        this.port = port;
        this.serverHost=hostname;
        this.timeout = timeout;

        runningthreads=new Vector<SMTPWorker>();
        // listen for incoming non-secure SMTP requests
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new java.net.InetSocketAddress(port));

        //if( IntegrationTest.isVerbose() )
        XTTProperties.printInfo("SMTPServer: started listening on port: " + port+" timout: "+timeout+" hostname: "+hostname);
    }

    /**
     * Stop the smtp server and all workers
     * @throws Exception
     */
    public void stopGracefully() throws java.lang.Exception
    {
        XTTProperties.printDebug("SMTPServer: close ServerSocket and request stop");
        stopGracefully = true;
        //enforce run() to break in infinite loop with SocketException
        ss.close();

        stopWorkers();
        //wait for process to stop, be sure that SMTPServer exits
        //this.join(); //please do that outside the SMTPServer
    }

    private void stopWorkers() throws Exception
    {
        XTTProperties.printDebug("SMTPServer("+port+"): Killing workers start");
        Vector<SMTPWorker> close=new Vector<SMTPWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            SMTPWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join(10000);
            } catch (Exception ex){}
        }
        runningthreads = new Vector<SMTPWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("SMTPServer("+port+"): Killing workers done");
    }

    /**
     * Part of Thread() interface, start the SMTPServer
     */
    public void run()
    {
        //SMTPWorker.receivedMailInit();
        Socket s;
        while (true)
        {
            // wait for a connection request to the Web server
            s = null;

            try
            {
                s = ss.accept();
                // we have received a request, so dispatch it to an available worker
                // thread. If there are no threads available, start a new one
                SMTPWorker w = null;
                XTTProperties.printDebug("SMTPServer: connection request received");
                SMTPWorker ws = new SMTPWorker(idCounter++);
                ws.setSocket(s);
                runningthreads.add(ws);
                ws.start();
            } catch (java.io.IOException e)
            {
                if(stopGracefully)
                {
                    XTTProperties.printDebug("SMTPServer: accept() interrupted by stop request");
                    break;
                }
                XTTProperties.printDebug("SMTPServer: accept() interrupted");
                try
                {
                    stopWorkers();
                } catch(Exception ex){}
                break;
                //e.printStackTrace();
                //throw new java.io.IOException(e.getMessage());
            }
        }

        try
        {
            if (s != null)
            {
                s.close();
            }
            if (ss != null)
            {
                ss.close();
            }
        } catch (java.io.IOException ex){}
    }

    public static final String tantau_sccsid = "@(#)$Id: SMTPServer.java,v 1.7 2010/07/09 10:50:32 mlichtin Exp $";

}
