package com.mobixell.xtt.diameter;

import java.io.File;
import java.util.Vector;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.XTTXML;
import com.mobixell.xtt.FunctionModule;
import com.mobixell.xtt.diameter.client.DiameterClient;
import com.mobixell.xtt.diameter.load.RuleManager;
import com.mobixell.xtt.diameter.load.UsersDataTable;
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
import com.mobixell.xtt.diameter.statistics.CmdDataTable;
import com.mobixell.xtt.diameter.statistics.CsvLog;
import com.mobixell.xtt.diameter.statistics.CsvLog.LogType;
import com.mobixell.xtt.diameter.statistics.StatisticsManager;

import org.jdom.Document;

/**
 * a very simple, multi-threaded Diameter server. Implementation notes are in DiameterServer.html, and also
 * as comments in the source code. 
 *
 * @version     $Id: DiameterServer.java,v 1.6 2011/03/08 10:50:32 guybitan Exp $
 * @author Roger Soder & Guy Bitan
 */
public class DiameterManager
{
	public  static final int DEFAULTPORT = 3868;
	public  static final int DEFAULTSECUREPORT = 1345;
	public  static RuleManager ruleManager = new RuleManager();
	public  static boolean runLoad = false;
	public  static boolean createtranslog = false;
	public  static int localPort;
	public  static String localIpAddr = "";
	public  static String remoteIpAddr = "";
	public  static String customer = "";
	public  static String respDocumentS;
	public  static Document respDocument;
	public  static int timeout;
	public  static boolean runSecure;
	public  static String tracing="";
	public  static String tracingFormat;
    
	public String getIp()
	{
		return localIpAddr;
	}

	public int getPort()
	{
		return localPort;
	}

	public static CsvLog csvTarnlog;
	public static CmdDataTable cmdTab;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<DiameterWorkerServer> runningthreads = new Vector<DiameterWorkerServer>();
    public static UsersDataTable usersTab = new UsersDataTable();
    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 60000;
    
    public static Integer RARU_TIME_OUT_SEC = null;
    public static Integer RART_TIME_OUT_SEC = null;
    public static Integer TPS_INTERVAL = 1;
    public static Integer SCENARIO_CHANGE_EVERY = null;
    public static Boolean IS_SCENARIO_CHANGE = false;
    public static Integer NUM_OF_TCP_CON = 1;
    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(DiameterWorkerServer diameterWorker){workerCounter--;runningthreads.remove(diameterWorker);}
    public synchronized int getNumWorkers(){return workerCounter;}
    public static final String tantau_sccsid = "@(#)$Id: DiameterServer.java,v 1.6 2011/03/08 10:50:32 mlichtin Exp $";

    /**
     * Main method for Diameter server
     * @param a may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        localPort    = DEFAULTPORT;
        localIpAddr = null;
        remoteIpAddr = null;
        respDocumentS = null;
        Document respDocument = null;
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
                } 
                else if ((a[i].equalsIgnoreCase("--port"))||(a[i].equalsIgnoreCase("-p")))
                {
                    localPort=Integer.parseInt(a[++i]);
                    portSet=true;
                } 
                else if ((a[i].equalsIgnoreCase("--localip"))||(a[i].equalsIgnoreCase("-li")))
                {
                	localIpAddr= a[++i];
                } 
                else if ((a[i].equalsIgnoreCase("--remoteip"))||(a[i].equalsIgnoreCase("-ri")))
                {
                	remoteIpAddr= a[++i];
                } 
                else if ((a[i].equalsIgnoreCase("--secure"))||(a[i].equalsIgnoreCase("-s")))
                {
                    runSecure=true;
                    if(!portSet)localPort=443;
                } 
                else if ((a[i].equalsIgnoreCase("--load"))||(a[i].equalsIgnoreCase("-l")))
                {
                    runLoad=true;
                }
                else if ((a[i].equalsIgnoreCase("--createtranslog"))||(a[i].equalsIgnoreCase("-ctl")))
                {
                	createtranslog=true;
                }
                else if ((a[i].equalsIgnoreCase("--customer"))||(a[i].equalsIgnoreCase("-customer")))
                {
                	customer=a[++i];
                }
                else if ((a[i].equalsIgnoreCase("--tcpcon"))||(a[i].equalsIgnoreCase("-tcpc")))
                {
                	NUM_OF_TCP_CON=Integer.parseInt(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--changescenario"))||(a[i].equalsIgnoreCase("-cse")))
                {
                	IS_SCENARIO_CHANGE =  true;
                }
                else if ((a[i].equalsIgnoreCase("--raru"))||(a[i].equalsIgnoreCase("-ru")))
                {
                    RARU_TIME_OUT_SEC=Integer.parseInt(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--rart"))||(a[i].equalsIgnoreCase("-rt")))
                {
                	 RART_TIME_OUT_SEC=Integer.parseInt(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--tpsi"))||(a[i].equalsIgnoreCase("-ti")))
                {
                    TPS_INTERVAL=Integer.parseInt(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--scenarioChangeEvry"))||(a[i].equalsIgnoreCase("-sce")))
                {
                	SCENARIO_CHANGE_EVERY=Integer.parseInt(a[++i]);
                }
                else if ((a[i].equalsIgnoreCase("--tracing"))||(a[i].equalsIgnoreCase("-t")))
                {
                    tracing=a[++i];
                } else if ((a[i].equalsIgnoreCase("--formattracing"))||(a[i].equalsIgnoreCase("-f")))
                {
                    tracingFormat=a[++i];
                } else if ((a[i].equalsIgnoreCase("--responseDocument"))||(a[i].equalsIgnoreCase("-r")))
                {
                    respDocumentS=a[++i];
                    File f=new java.io.File(respDocumentS);
                    if(f.isDirectory())
                    {
                        System.out.println("\nERROR: Is a directory: "+respDocument+"\n");
                        showHelp(); //If the command line options were bad, show the help
                        return;
                    }
                    respDocument=XTTXML.readXML(respDocumentS);
                    if(respDocument==null)
                    {
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
            if (RARU_TIME_OUT_SEC==null) RARU_TIME_OUT_SEC =1;
	        if (RART_TIME_OUT_SEC==null) RART_TIME_OUT_SEC =2;	     
            
            String space = "\n             ";
            String loadParam = "";
            
            String param = "DiameterManager Is Running..." + space + "Local IP:" + localIpAddr + space + "Remote IP: " + remoteIpAddr +
    			space +"listening-port:" + localPort + space +
                "timout:" + timeout + space +
                "secure:" + runSecure + space;
                                    if (runLoad )
                                    {
                                    loadParam = "load-model: " + runLoad + space + 
                                    (NUM_OF_TCP_CON > 0 | NUM_OF_TCP_CON == null ? 
                                            "tcp-connections:" + NUM_OF_TCP_CON : "") +
                                    (RARU_TIME_OUT_SEC > 0 | RARU_TIME_OUT_SEC == null ? space + 
                                    		"raru-delay:" + RARU_TIME_OUT_SEC : "") +
                			        (RART_TIME_OUT_SEC > 0 | RART_TIME_OUT_SEC == null ? space +
                			        		"rart-delay:" + RART_TIME_OUT_SEC : "") +
                                    (TPS_INTERVAL > 0 | TPS_INTERVAL == null ? space + 
                                    		"tps-interval:" + TPS_INTERVAL : "") +
                                    (SCENARIO_CHANGE_EVERY > 0 | SCENARIO_CHANGE_EVERY == null? space + 
                                    		"scenario-change-every:" + SCENARIO_CHANGE_EVERY + (SCENARIO_CHANGE_EVERY == 1  ? " user": " users") : "");
                                    }
            XTTProperties.printInfo(space+param+loadParam+space);
            
            StatisticsManager stsManager = new StatisticsManager();
    		stsManager.start();
    		
    		TelnetServer ts = new TelnetServer(localIpAddr);
    		ts.start();
            
    		if (createtranslog)
    		{
    		csvTarnlog = new CsvLog(LogType.TRANS);
    		cmdTab = new CmdDataTable(csvTarnlog);
    		}
            
            DiameterServer ws = new DiameterServer(localIpAddr, localPort, respDocument, timeout, runSecure);
            ws.start();
            if (runLoad && remoteIpAddr!=null)
            {
            	DiameterClient dc = new DiameterClient(localIpAddr,remoteIpAddr, localPort,respDocument);
            	if (customer.equalsIgnoreCase("OPL"))
            		dc.createWorkers();
            }
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
        System.out.println("eXtreme Test Tool (XTT) - Diameter Manager");
        System.out.println("Version: DiameterServer: "+FunctionModule.parseVersion(DiameterServer.tantau_sccsid)+" DiameterWorker: "+FunctionModule.parseVersion(DiameterWorkerServer.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-customer <OPL,vimpelcom> --customer <OPL,vimpelcom>");
        System.out.println("\t\tSpecify customer name for:");
        System.out.println("\t\t Orange Polin Send RAR on multiple connections (without msisdn)");
        System.out.println("\t\tFor Vimpelcom send RAR on PCEF socket and generate msisdn");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the DiameterServer on (default 80)");
        System.out.println("");
        System.out.println("\t-li <ipaddress>, --localip <ipaddress>");
        System.out.println("\t\tSpecify the ip address to bind the DiameterServer on (default local host)");
        System.out.println("");
        System.out.println("\t-ri <ipaddress>, --remoteip <ipaddress>");
        System.out.println("\t\tSpecify the ip address of the PCEF client");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure DiameterServer (default port 443)");
        System.out.println("");
        System.out.println("\t-l, --load");
        System.out.println("\t\tStart the Load DiameterServer model");
        System.out.println("");
        System.out.println("\t-createtranslog, --ctl");
        System.out.println("\t\tSpecify if create transaction log");
        System.out.println("");
        System.out.println("\t-tcpc, --tcpcon");
        System.out.println("\t\tSpecify the number of tcp connection to PCEF server");
        System.out.println("");
        System.out.println("\t-cse, --changescenario");
        System.out.println("\t\tSpecify if to change scenario if yes specify the number in scenarioChangeEvry parameter");
        System.out.println("");
        System.out.println("\t-ru, --raru");
        System.out.println("\t\tSpecify the RAR-U push delay (sec)");
        System.out.println("");
        System.out.println("\t-rt, --rart");
        System.out.println("\t\tSpecify the RAR-T push delay (sec)");
        System.out.println("");
        System.out.println("\t-ti, --tpsi");
        System.out.println("\t\tSpecify the Tps interval (Sec)");
        System.out.println("");
        System.out.println("\t-sce, --scenarioChangeEvry");
        System.out.println("\t\tSpecify the number of user when the scenario will be change");
        System.out.println("");
        System.out.println("\t-r <directorypath>, --responseDocument <directorypath>");
        System.out.println("\t\tSpecify the MANDATORY response document of the DiameterServer");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the DiameterServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }
}
