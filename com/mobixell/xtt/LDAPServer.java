package com.mobixell.xtt;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.HashMap;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

import org.jdom.Document;
import org.jdom.Element;

/**
 * a very simple, multi-threaded HTTP server. Implementation notes are in LDAPServer.html, and also
 * as comments in the source code
 *
 * @version     $Id: LDAPServer.java,v 1.9 2010/07/09 10:50:32 mlichtin Exp $
 * @author Gavin Cattell & Roger Soder
 */
public class LDAPServer extends Thread implements Runnable
{
    private int port;
    public  int getPort(){return port;}

    private ServerSocket ss = null;
    private static HashMap<String,LDAPServer> serverMap=new HashMap<String,LDAPServer>();

    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private boolean stopGracefully = false;

    /* A set of worker threads is available. This is where they live when
     * they are idle */
    private Vector<LDAPWorker> runningthreads = null;

    private static int idCounter = 0;
    public int getIdCounter(){return idCounter;}
    public static void resetWorkerId(){idCounter = 0;}

    /* timeout on client connections */
    public static final int DEFAULTTIMEOUT = 30000;
    private int timeout = DEFAULTTIMEOUT;

    private int workerCounter    = 0;
    public synchronized void addWorker(){workerCounter++;}
    public synchronized void removeWorker(LDAPWorker w){workerCounter--;runningthreads.remove(w);}
    public synchronized int getNumWorkers(){return workerCounter;}

    private Document ldapConfig=null;
    private HashMap<String,String> userMap=null;
    private HashMap<String,HashMap<String,LDAPEntry>> baseDNentries=null;

    public static final String tantau_sccsid = "@(#)$Id: LDAPServer.java,v 1.9 2010/07/09 10:50:32 mlichtin Exp $";

    /**
     * Main method for Web server
     * @param a     may hold port number, protocol etc. upon which to listen for SMPP/UCP requests
     * @throws Exception
     */
    public static void main (String [] a)
    {
        int port    = 389;
        int timeout = DEFAULTTIMEOUT;
        boolean runSecure=false;
        String tracing="";
        String tracingFormat=null;
        Document ldapConfig=null;
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
                } else if ((a[i].equalsIgnoreCase("--timeout"))||(a[i].equalsIgnoreCase("-o")))
                {
                    timeout=Integer.parseInt(a[++i]);
                } else if ((a[i].equalsIgnoreCase("--config"))||(a[i].equalsIgnoreCase("-c")))
                {
                    if (!a[++i].endsWith(".xml"))
                    {
                        XTTProperties.printFail("Invalid config file type, must be .XML");
                        throw new Exception("Invalid config file type, must be .XML");
                    } else
                    {
                        ldapConfig=XTTXML.readXML(a[i]);
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
            LDAPServer ws = null;
            XTTProperties.printDebug("Version: LDAPServer: "+FunctionModule.parseVersion(LDAPServer.tantau_sccsid)+" LDAPWorker: "+FunctionModule.parseVersion(LDAPWorker.tantau_sccsid));
            ws = new LDAPServer(port, timeout, runSecure, ldapConfig);
            ws.run();
        } catch (Exception e)
        {
            System.out.println("Error: Invalid command line option");
            XTTProperties.printException(e);
            showHelp(); //If the command line options were bad, show the help
        }
    }

    private static void showHelp()
    {
        System.out.println("eXtreme Test Tool (XTT) - Web Server - 724 Solutions");
        System.out.println("Version: LDAPServer: "+FunctionModule.parseVersion(LDAPServer.tantau_sccsid)+" LDAPWorker: "+FunctionModule.parseVersion(LDAPWorker.tantau_sccsid));
        System.out.println("");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\t--help\n\t\tThis help");
        System.out.println("");
        System.out.println("\t-c <config>, --config <config>");
        System.out.println("\t\tSpecify the file where the ldap configuration is");
        System.out.println("");
        System.out.println("\t-t <level>, --tracing <level>");
        System.out.println("\t\tSpecify the tracing level to use,\n\t\tsupported: fail, warn, info, verbose, debug");
        System.out.println("");
        System.out.println("\t-p <portnum>, --port <portnum>");
        System.out.println("\t\tSpecify the port to run the LDAPServer on (default 80)");
        System.out.println("");
        System.out.println("\t-s, --secure");
        System.out.println("\t\tStart the Secure LDAPServer (default port 443)");
        System.out.println("");
        System.out.println("\t-o <timeoutms>, --timeout <timeoutms>");
        System.out.println("\t\tSpecify the timeout time in ms for the LDAPServer");
        System.out.println("");
        System.out.println("\t-f <traceformat>, --formattracing <traceformat>");
        System.out.println("\t\tSpecify trace format");
        System.out.println("");

        System.exit(1); //Exit and fail if you had to show this help
    }


    public void setMyConfiguration(Document configuration) throws Exception
    {
        ldapConfig=configuration;
        if(ldapConfig==null)
        {
            throw new Exception("ldap server configuration missing");
        }
        userMap=new HashMap<String,String>();
        baseDNentries=new HashMap<String,HashMap<String,LDAPEntry>>();
        buildConfiguration();
    }

    /**
     * Create a new instance of the LDAPServer class, listening on the specified
     * non-secure port for LDAP requests.
     * @param port
     * @throws Exception
     */
    public LDAPServer (int port, int timeout, boolean runSecure, Document configuration) throws Exception
    {
        super("LDAPServer-"+port);
        runningthreads = new Vector<LDAPWorker>();
        this.port = port;
        this.timeout = timeout;
        setMyConfiguration(configuration);

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
        
        XTTProperties.printInfo("LDAPServer: listening on port:"+port+" timout:"+timeout +" secure:"+runSecure);
    }

    private void buildConfiguration() throws Exception
    {
        synchronized(this)
        {
            Element root = ldapConfig.getRootElement();
            if(!root.getName().equals("LDAP"))
            {
                throw new Exception("LDAPServer.buildConfiguration(): root node is not called LDAP");
            }
            Element logins=root.getChild("logins");
            if(logins==null)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node LDAP/logins missing");
            }
            List loginList=logins.getChildren("login");
            if(loginList==null||loginList.size()==0)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node LDAP/logins/login missing, one or more needed");
            }
            ListIterator it=loginList.listIterator();
            Element user=null;
            while(it.hasNext())
            {
                user=(Element)it.next();
                try
                {
                    userMap.put(user.getChild("name").getText(),user.getChild("password").getText());
                } catch (NullPointerException npe)
                {
                    throw new Exception("LDAPServer.buildConfiguration(): node LDAP/logins/login/name or LDAP/logins/login/passowrd missing");
                }
            }
            XTTProperties.printDebug("LDAPServer.buildConfiguration(): loaded user map:\n"+userMap);
            
            Element directory=root.getChild("directory");
            if(directory==null)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node LDAP/directory missing");
            }
            List entrys=directory.getChildren("entry");
            if(entrys==null||entrys.size()==0)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node LDAP/directory/entry missing, one per baseDN needed");
            }
            
            it=entrys.listIterator();
            Element entry=null;
            String baseDN=null;
            while(it.hasNext())
            {
                entry=(Element)it.next();
                baseDN=entry.getChild("baseDN").getText();
                if(baseDN==null)
                {
                    throw new Exception("LDAPServer.buildConfiguration(): node LDAP/directory/entry/baseDN missing");
                } else if(baseDNentries.get(baseDN)!=null)
                {
                    throw new Exception("LDAPServer.buildConfiguration(): duplicate baseDN: "+baseDN);
                }
                XTTProperties.printDebug("LDAPServer.buildConfiguration(): found: baseDN: "+baseDN);
                List data=entry.getChildren("data");
                if(data==null||data.size()==0)
                {
                    throw new Exception("LDAPServer.buildConfiguration(): node LDAP/directory/entry/data missing, one or more per baseDN needed");
                }
                HashMap<String,LDAPEntry> dataEntrys=new HashMap<String,LDAPEntry>();
                ListIterator dataIT=data.listIterator();
                Element ldapEntryElement=null;
                LDAPEntry ldapEntry=null;
                while(dataIT.hasNext())
                {
                    ldapEntryElement=(Element)((Element)dataIT.next()).clone();
                    ldapEntryElement.detach();
                    ldapEntry=new LDAPEntry(baseDN,new Document(ldapEntryElement));
                    dataEntrys.put(ldapEntry.getFilter(),ldapEntry);
                }
                baseDNentries.put(baseDN,dataEntrys);
                
            }
            //XTTProperties.printVerbose("LDAPServer.buildConfiguration(): loaded data:\n"+baseDNentries);
        }
    }

    public Vector<LDAPEntry> getMatches(String baseDN, String filter)
    {
        if(baseDN==null)baseDN="null";
        if(filter==null)filter="null";
        Vector<LDAPEntry> matches=new Vector<LDAPEntry>();
        
        HashMap<String,LDAPEntry> dataEntries=null;
        synchronized(this)
        {
            dataEntries=baseDNentries.get(baseDN);
        }
        if(dataEntries==null)
        {
            return matches;
        } else
        {
            LDAPEntry entry=null;
            synchronized(dataEntries)
            {
                entry=dataEntries.get(filter);
                if(entry!=null)
                {
                    matches.add(entry);
                }
            }
        }
        return matches; 
    }

    public boolean checkLogin(String username, String password)
    {
        if(username==null)username="null";
        if(password==null)password="null";
        String checkPW=null;
        
        synchronized(this)
        {
            checkPW=userMap.get(username);
        }
        if(checkPW==null)
        {
            return false;
        } else
        {
            return checkPW.equals(password);
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
            LDAPServer s=null;
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
            LDAPServer s=(LDAPServer)serverMap.get(port);
            s.stopGracefully();
            serverMap.remove(port);
        }
    }
    public static void setConfiguration(Document config) throws Exception
    {
        synchronized(serverMap)
        {
            java.util.Iterator<String> it=serverMap.keySet().iterator();
            LDAPServer s=null;
            String currentPort=null;
            while(it.hasNext())
            {
                currentPort=it.next();
                s=serverMap.get(currentPort);
                s.setMyConfiguration(config);
            }
        }
    }
    public static void setConfiguration(String port,Document config) throws Exception
    {
        synchronized(serverMap)
        {
            LDAPServer s=(LDAPServer)serverMap.get(port);
            s.setMyConfiguration(config);
        }
    }
    public static int getLastWorkerId(String port)
    {
        synchronized(serverMap)
        {
            LDAPServer s=(LDAPServer)serverMap.get(port);
            return s.getIdCounter();
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
        XTTProperties.printDebug("LDAPServer("+port+"): close ServerSocket and request stop");
        //enforce run() to break in infinite loop
        ss.close();

        stopWorkers();

        synchronized(serverMap)
        {
            serverMap.remove(this);
        }

        //wait for process to stop, be sure that LDAPServer exits
        //this.join(); //please do that outside the LDAPServer
    }

    private void stopWorkers()
    {
        XTTProperties.printDebug("LDAPServer("+port+"): Killing workers start");
        Vector<LDAPWorker> close=new Vector<LDAPWorker>();
        for (int i = 0; i < runningthreads.size(); ++i)
        {
            close.add(runningthreads.get(i));
        }
        for (int i = 0; i < close.size(); ++i)
        {
            LDAPWorker w =close.get(i);
            w.setStop();
            //wait for processes to stop. be sure that all threads exit
            try
            {
                w.join();
            } catch (Exception ex){}
        }
        runningthreads = new Vector<LDAPWorker>();  // bye-bye threads table, ready to start up again
        XTTProperties.printDebug("LDAPServer("+port+"): Killing workers done");
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
                } catch (java.io.IOException e)
                {
                    if (stopGracefully)
                    {
                        XTTProperties.printDebug("LDAPServer("+port+"): accept() interrupted by stop request");
                        break;
                    }
                    XTTProperties.printDebug("LDAPServer("+port+"): accept() interrupted - stopping workers");
                    stopWorkers();
                    throw e;
                }
                XTTProperties.printDebug("LDAPServer: connection request received");
                LDAPWorker ws = new LDAPWorker(idCounter++,s,this,timeout);
                XTTProperties.printDebug("LDAPServer("+port+"): starting new LDAPWorker: id " + ws.getWorkerId());
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
            XTTProperties.printDebug("LDAPServer("+port+"): Exception in run()      : " + ex.getMessage());
            return;
        }
    }

}
