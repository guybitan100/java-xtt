package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;

/**
 * a very simple, multi-threaded SMTP server. Implementation notes are in SMSCServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: SMSCServer.java,v 1.15 2010/07/09 10:50:32 mlichtin Exp $
 */
public class SMSCServer implements Runnable
{
    private int port;
    public  int getPort(){return port;}

    private ServerSocket ss = null;
    private static HashMap<String,SMSCServer> serverMap=new HashMap<String,SMSCServer>();

    private boolean stopGracefully = false;

    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private final static String serverName="SMSC - 724 Solutions XTT Test-Server";
    private String protocol="";
    private int maxRequestCount  = -1;
    private int windowSize       = 1;
    private int responseDelay    = 0;
    private int maxSessions      = 0;
    private int maxConnection    = 0;
    private int sessionTimeout   = 0;
    private int readTimeout      = 0;
    private boolean injectAutoMessages=false;
    private int earliestAutoMessageSendTime=0;
    private int latestAutoMessageSendTime=0;
    private int autoMessageRetryTime=500;
    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(SMSCWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<SMSCWorker> runningthreads = new Vector<SMSCWorker>();

    private static int idCounter = 0;
    public static void resetWorkerId(){idCounter = 0;}

    /* static class data/methods */
    public String getProtocol()
    {
        return protocol;
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
            SMSCServer s=null;
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
            SMSCServer s=serverMap.get(port);
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
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int usePort=0;
        String protocol="";
        String tracing="";
        String tracingFormat=null;
        if (a.length == 0)
        {
            showHelp();
        }
        try
        {
            int maxRequestCount  = -1;
            int windowSize       = 1;
            int responseDelay    = 0;
            int maxSessions      = 0;
            int maxConnection    = 0;
            int sessionTimeout   = 0;
            int readTimeout      = 0;
            boolean injectAutoMessages=false;
            int earliestAutoMessageSendTime=0;
            int latestAutoMessageSendTime=0;
            int autoMessageRetryTime=500;
            
            for (int i = 0; i<a.length ; i++) //Loop around the arguments
            {
                if (a[i].equalsIgnoreCase("--help")) //Check each argument
                {
                    showHelp();
                } else if ((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    usePort=Integer.parseInt(a[++i]);
                } else if ((a[i].equalsIgnoreCase("--protocol"))||(a[i].equalsIgnoreCase("-c")))
                {
                    protocol=a[++i];
                } else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--enableTransactions")))
                {
                    XTTProperties.setLogTransactions(true);
                } else if (a[i].equalsIgnoreCase("--UCPMaxRequestCount"))
                {
                    maxRequestCount = Integer.parseInt(a[++i]);
                    if(maxRequestCount<0){maxRequestCount=-1;}
                } else if (a[i].equalsIgnoreCase("--UCPWindowSize"))
                {
                    windowSize = Integer.parseInt(a[++i]);
                    if(windowSize<=1){windowSize=1;}
                } else if (a[i].equalsIgnoreCase("--UCPResponseDelay"))
                {
                    responseDelay = Integer.parseInt(a[++i]);
                    if(responseDelay<=0){responseDelay=0;}
                } else if (a[i].equalsIgnoreCase("--UCPSessionTimeout"))
                {
                    sessionTimeout = Integer.parseInt(a[++i]);
                    if(sessionTimeout<=0){sessionTimeout=0;}
                } else if (a[i].equalsIgnoreCase("--UCPReadTimeout"))
                {
                    readTimeout = Integer.parseInt(a[++i]);
                    if(readTimeout<=0){readTimeout=0;}
                } else if (a[i].equalsIgnoreCase("--UCPMaxSessions"))
                {
                    maxSessions = Integer.parseInt(a[++i]);
                    if(maxSessions<=0){maxSessions=0;}
                } else if (a[i].equalsIgnoreCase("--UCPMaxConnections"))
                {
                    maxConnection = Integer.parseInt(a[++i]);
                    if(maxConnection<=0){maxConnection=0;}
                } else if (a[i].equalsIgnoreCase("--enableInjectAutoMessages"))
                {
                    injectAutoMessages = true;
                } else if (a[i].equalsIgnoreCase("--earliestAutoMessageSendTime"))
                {
                    earliestAutoMessageSendTime = Integer.parseInt(a[++i]);
                    if(earliestAutoMessageSendTime<=0){earliestAutoMessageSendTime=0;}
                } else if (a[i].equalsIgnoreCase("--latestAutoMessageSendTime"))
                {
                    latestAutoMessageSendTime = Integer.parseInt(a[++i]);
                    if(latestAutoMessageSendTime<=0){latestAutoMessageSendTime=0;}
                } else if (a[i].equalsIgnoreCase("--autoMessageRetryTime"))
                {
                    autoMessageRetryTime = Integer.parseInt(a[++i]);
                    if(autoMessageRetryTime<=0){autoMessageRetryTime=0;}
                }                else
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
            SMSCServer ws = null;
            XTTProperties.printDebug("Version: SMSCServer: "+FunctionModule.parseVersion(SMSCServer.tantau_sccsid)
                                            +" SMPPWorker: "+FunctionModule.parseVersion(SMPPWorker.tantau_sccsid)
                                            +" UCPWorker : "+FunctionModule.parseVersion(UCPWorker.tantau_sccsid)
                                            +" CIMDWorker: "+FunctionModule.parseVersion(CIMDWorker.tantau_sccsid));
            ws = new SMSCServer(usePort,protocol,maxRequestCount,windowSize,responseDelay,maxSessions,sessionTimeout,readTimeout,maxConnection
                ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
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
        System.out.println("eXtreme Test Tool (XTT) - SMSC Simulator - 724 Solutions");
        System.out.println("Version: SMSCServer: "+FunctionModule.parseVersion(SMSCServer.tantau_sccsid)
                                  +" SMPPWorker: "+FunctionModule.parseVersion(SMPPWorker.tantau_sccsid)
                                  +" UCPWorker : "+FunctionModule.parseVersion(UCPWorker.tantau_sccsid)
                                  +" CIMDWorker: "+FunctionModule.parseVersion(CIMDWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the SMSC on");
        System.out.println("");
        System.out.println("\t-c <protocol>, --protocol <protocol>");
        System.out.println("\t\tSpecify the protocol of the SMSC,\n\t\tsupported: SMPP,UCP");
        System.out.println("");
        System.out.println("\t--enableTransactions");
        System.out.println("\t\tEnable the printing of transactions\n\t\tlike enabling tracing/showTransactions in configuration");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");
        System.out.println("\t--enableInjectAutoMessages");
        System.out.println("\t\tSend UCP 53/SMPP MC Delivery Receipt answers to messages");
        System.out.println("\t--earliestAutoMessageSendTime timems");
        System.out.println("\t\tTime to wait at least before sending auto message");
        System.out.println("\t--latestAutoMessageSendTime timems");
        System.out.println("\t\tTime to wait max before sending auto message");
        System.out.println("\t--autoMessageRetryTime timems");
        System.out.println("\t\tRetry sending after this time when no messages are arriving");
        System.out.println("");
        System.out.println("\tUCP Specific options:");
        System.out.println("\t--UCPMaxRequestCount numRequests");
        System.out.println("\t\tMax number of requests per connection before disconnecting");
        System.out.println("\t--UCPWindowSize maxNumWindows");
        System.out.println("\t\tWindow size for one connection");
        System.out.println("\t--UCPResponseDelay delayms");
        System.out.println("\t\tTime to wait in ms before sending a response");
        System.out.println("\t--UCPSessionTimeout delayms");
        System.out.println("\t\tTime to wait idle after UCP60 before disconnecting");
        System.out.println("\t--UCPReadTimeout delayms");
        System.out.println("\t\tTime to wait for an UCP body to be read");
        System.out.println("\t--UCPMaxSessions maxNumSessions");
        System.out.println("\t\tMax number of UCP 60 sessions allowed");
        System.out.println("\t--UCPMaxConnections maxNumConnections");
        System.out.println("\t\tMax number of connections allowed");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    /**
     * Create a new instance of the SMSCServer class, listening on the specified
     * port for SMPP requests.
     * A ServerSocket is started to listen for incoming requests, which will
     * then be dispatched to an available worker thread
     * @param port      port to listen ion
     * @param protocol  SMPP and UCP are currently only supported
     * @throws Exception
     */
    public SMSCServer(int port, String protocol) throws Exception
    {
        this(port,protocol,0,1,0,0,0,0,0,false,0,0,500);
    }
    public SMSCServer (int port, String protocol, int maxreqC,int windowS, int responseD,int maxSessions, int sessionTimeout, int readTimeout, int maxConnection
        ,boolean injectAutoMessages, int earliestAutoMessageSendTime, int latestAutoMessageSendTime, int autoMessageRetryTime) throws Exception
    {
        if(!checkSocket(""+port))
        {
            XTTProperties.printFail("SMSCServer: already a SMSCServer running on port "+port);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            throw new Exception("SMSCServer: already a SMSCServer running on port "+port);
        }
        this.port = port;
        this.protocol=protocol.toUpperCase();
        this.maxRequestCount=maxreqC;
        this.injectAutoMessages=injectAutoMessages;
        this.earliestAutoMessageSendTime=earliestAutoMessageSendTime;
        this.latestAutoMessageSendTime=latestAutoMessageSendTime;
        this.autoMessageRetryTime=autoMessageRetryTime;
        if(this.protocol.equalsIgnoreCase("UCP"))
        {
            this.windowSize     = windowS;
            this.responseDelay  = responseD;
            this.maxSessions    = maxSessions;
            this.sessionTimeout = sessionTimeout;
            this.readTimeout    = readTimeout;
            this.maxConnection  = maxConnection;
            UCPWorker.setReadTimeout(readTimeout);
        }
        if(!this.protocol.equalsIgnoreCase("SMPP")&&!this.protocol.equalsIgnoreCase("UCP")&&!this.protocol.equalsIgnoreCase("CIMD"))
        {
            XTTProperties.printFail("SMSCServer: protocol "+protocol+" not supported, supported protocols are: SMPP,UCP,CIMD");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            throw new Exception("Protocol not supported: "+protocol);
        }

        // listen for incoming non-secure SMPP requests
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new java.net.InetSocketAddress(port));

        //if( IntegrationTest.isVerbose() )
        String maxreq="";
        if(maxRequestCount>=0)
        {
            maxreq=" maxRequestCount: "+maxRequestCount;
        }
        if(windowSize>1)
        {
            maxreq=maxreq+" windowSize: "+windowSize;
        }
        if(responseDelay>0)
        {
            maxreq=maxreq+" responseDelay: "+responseDelay;
        }
        if(maxSessions>0)
        {
            maxreq=maxreq+" maxSessions: "+maxSessions;
        }
        if(maxSessions>0)
        {
            maxreq=maxreq+" maxConnection: "+maxConnection;
        }
        if(sessionTimeout>0)
        {
            maxreq=maxreq+" sessionTimeout: "+sessionTimeout;
        }
        if(readTimeout>0)
        {
            maxreq=maxreq+" readTimeout: "+readTimeout;
        }
        if(injectAutoMessages==true)
        {
            maxreq=maxreq+" injectAutoMessages: "+injectAutoMessages
                         +" earliestAutoMessageSendTime: "+earliestAutoMessageSendTime
                         +" latestAutoMessageSendTime: "+latestAutoMessageSendTime
                         +" autoMessageRetryTime: "+autoMessageRetryTime;
        }

        synchronized(serverMap)
        {
            serverMap.put(port+"",this);
        }
        XTTProperties.printInfo("SMSCServer("+port+"): started: listening on port " + port+" protocol: "+protocol+maxreq);
    }
    /**
     * Stop the smsc server
     * @throws Exception
     */
    public void stopGracefully() throws java.lang.Exception
    {
        stopGracefully = true;
        XTTProperties.printDebug("SMSCServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that SMSCServer exits
        //this.join(); //please do that outside the SMSCServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("SMSCServer("+port+"): Killing workers start");
        Vector<SMSCWorker> close=new Vector<SMSCWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            SMSCWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join(10000);
            } catch (Exception ex){}
        }
        runningthreads = new Vector<SMSCWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("SMSCServer("+port+"): Killing workers done");
    }

    /**
     * Part of Thread() interface, start the SMSCServer
     */
    public void run()
    {
        Socket s;
        try
        {
            while (true)
            {
                // wait for a connection request to the SMSCServer
                s = null;
                try
                {
                    s = ss.accept();
                } catch (java.io.IOException e)
                {
                    if(stopGracefully)
                    {
                        XTTProperties.printDebug("SMSCServer("+port+"): accept() interrupted by stop request");
                        break;
                    }/* else
                    {
                        XTTProperties.printDebug("SMSCServer: accept() interrupted - stopping workers");
                        stopWorkers();
                        break;
                    }*/
                        XTTProperties.printDebug("SMSCServer("+port+"): accept() interrupted - stopping workers");
                        stopWorkers();
                    //XTTProperties.printDebug("SMSCServer: accept() interrupted");
                    //e.printStackTrace();
                    throw e;
                }

                if((maxConnection>0) && (getNumWorkers()>=maxConnection))
                {
                    try
                    {
                        XTTProperties.printDebug("SMSCServer("+port+"): maxConnections("+getNumWorkers()+"/"+maxConnection+") reached, closing connection!");
                        s.close();
                    } catch(Exception e)
                    {
                        //e.printStackTrace();
                    }
                } else
                {
                    String maxC="";
                    if(maxConnection>0){maxC=" ("+getNumWorkers()+"/"+maxConnection+")";}
                    // we have received a request, so start a new worker
                    if(this.protocol.equalsIgnoreCase("SMPP"))
                    {
                         SMPPWorker ws = new SMPPWorker(idCounter++,s,this,maxRequestCount
                            ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
                         XTTProperties.printDebug("SMSCServer("+port+"): starting new SMPPWorker: id " + ws.getWorkerId()+maxC);
                         runningthreads.add(ws);
                         ws.start();
                    } else if(this.protocol.equalsIgnoreCase("UCP"))
                    {
                         UCPWorker ws = new UCPWorker(idCounter++,s,this,maxRequestCount, windowSize, responseDelay, maxSessions, sessionTimeout
                            ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
                          XTTProperties.printDebug("SMSCServer("+port+"): starting new UCPWorker: id " + ws.getWorkerId()+maxC);
                         runningthreads.add(ws);
                         ws.start();
                    } else if(this.protocol.equalsIgnoreCase("CIMD"))
                    {
                         CIMDWorker ws = new CIMDWorker(idCounter++,s,this,maxRequestCount, windowSize, responseDelay, maxSessions, sessionTimeout
                            ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
                          XTTProperties.printDebug("SMSCServer("+port+"): starting new CIMDWorker: id " + ws.getWorkerId()+maxC);
                         runningthreads.add(ws);
                         ws.start();
                    } else
                    {
                        XTTProperties.printFail("SMSCServer("+port+"): Something is fishy, tried to use an unsupported protocol");
                    }
                }
            }

            //    XTTProperties.printDebug("SMSCServer: free all resources");
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
            XTTProperties.printDebug("SMSCServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
        //XTTProperties.printInfo("SMSCServer: stopped");
    }

    public static final String tantau_sccsid = "@(#)$Id: SMSCServer.java,v 1.15 2010/07/09 10:50:32 mlichtin Exp $";

}
