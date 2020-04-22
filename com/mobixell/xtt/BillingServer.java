package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mobixell.xtt.BillingServer;
import com.mobixell.xtt.BillingWorker;
import com.mobixell.xtt.VODAPAMIWorker;
import com.mobixell.xtt.XTTProperties;

/**
 * A billing server that can use multiple interfaces depending on the specific billing server protocol used.
 *
 *
 * @version     $Id: BillingServer.java,v 1.3 2010/07/09 10:50:32 mlichtin Exp $
 */
public class BillingServer implements Runnable
{
    private int port;
    private static ServerSocket ss = null;

    private static String protocol="";

    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    static protected Vector<BillingWorker> runningthreads = new Vector<BillingWorker>();

    static int idCounter = 0;
    private static void resetWorkerId(){idCounter = 0;}

    public static int waterMark = -1;
    public static int extraWater = -1;
    public static int percentAbove = -1;
    public static int percentBelow = -1;

    public static void closeSocket() throws Exception
    {
        ss.close();
    }
    public static boolean checkSocket()
    {
        return (ss==null);
    }

    public BillingServer(int port, String protocol)  throws Exception
    {
        setPort(port);
        this.protocol = protocol;
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new java.net.InetSocketAddress(port));
        resetWorkerId();
    }

    /**
     * Main method for Billing Server
     * @param args     may hold port number upon which to listen for billing requests
     */
    public static void main (String [] args)
    {
        if(args.length < 2)
        {
            System.out.println("int port string protocol");
            System.out.println("int port string protocol int base/int diff-int %below/int %above");
            System.exit(1);
        }
        try
        {
            XTTProperties.setTracing(XTTProperties.DEBUG);
            BillingServer bs = null;
            int port = Integer.parseInt(args[0]);
            String protocol = args[1];
            if(args.length > 2)
            {
                String delayInfo = args[2];
                String regex = "(.*?)/(.*?)-(.*?)/(.*)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(delayInfo);

                if(matcher.find())
                {
                    if(matcher.groupCount() == 4)
                    {
                        //XTTProperties.printInfo(">>" + matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3) + " " + matcher.group(4) + "<<");
                        try
                        {
                            waterMark = Integer.parseInt(matcher.group(1));
                            extraWater = Integer.parseInt(matcher.group(2));
                            percentBelow = Integer.parseInt(matcher.group(3));
                            percentAbove = Integer.parseInt(matcher.group(4));
                        }
                        catch(NumberFormatException nfe)
                        {
                            XTTProperties.printFail("BillingServer: Invalid numbers");
                            System.exit(1);
                        }
                    }
                    else
                    {
                        XTTProperties.printFail("BillingServer: only " + matcher.groupCount() +" groups");
                    }
                }
                else
                {
                    XTTProperties.printFail("BillingServer: couldn't parse delay info");
                }
            }
            bs = new BillingServer(port,protocol);
            bs.run();
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
            //e.printStackTrace();
        }
    }

    public void run()
    {
        Socket s;
        /*try
        {*/
            while (true)
            {
                try
                {
                    s = null;
                    try
                    {
                        s = ss.accept();
                    }
                    catch (java.io.IOException e)
                    {
                        if (e.getMessage().equals("socket closed"))
                        {
                            if(stopGracefully)
                            {
                                XTTProperties.printDebug("BillingServer: accept() interrupted by stop request");
                                break;
                            } else
                            {
                                XTTProperties.printDebug("BillingServer: accept() interrupted - stopping workers");
                                stopWorkers();
                                break;
                            }
                        }

                        XTTProperties.printDebug("BillingServer: accept() interrupted");
                        //e.printStackTrace();
                        //throw new java.io.IOException(e.getMessage());
                    }

                    if(this.protocol.equals("VODAPAMI"))
                    {
                         VODAPAMIWorker vw = new VODAPAMIWorker(idCounter++,s,this);
                         XTTProperties.printDebug("BillingServer: starting new VODAPAMIWorker: id " + vw.getWorkerId());
                         runningthreads.add(vw);
                         vw.start();
                    }
                    else
                    {
                        XTTProperties.printFail("BillingServer: Something is fishy, tried to use an unsupported protocol");
                    }
                }
                catch(Exception e)
                {
                    //Don't break out the loop at all!
                    e.printStackTrace();
                }
            }
  /*      }
        catch (java.io.IOException ex)
        {
            XTTProperties.printDebug("BillingServer: Exception in run()      : " + ex.getMessage());
            return;
        }*/
    }

    /**
     * Stop the billing server
     * @throws Exception
     */
    public void stopGracefully() throws java.lang.Exception
    {
        stopGracefully = true;
        XTTProperties.printDebug("BillingServer: close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        //wait for process to stop, be sure that BillingServer exits
        //this.join(); //please do that outside the BillingServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("BillingServer: Killing workers");
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            BillingWorker w =(BillingWorker)runningthreads.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            }
            catch (Exception ex){}
        }
        runningthreads = new Vector<BillingWorker>();  // bye-bye threads table, ready to start up again
    }

    public int getPort()
	{
		return port;
	}
	public void setPort(int port)
	{
		this.port = port;
	}

	public static final String tantau_sccsid = "@(#)$Id: BillingServer.java,v 1.3 2010/07/09 10:50:32 mlichtin Exp $";
}