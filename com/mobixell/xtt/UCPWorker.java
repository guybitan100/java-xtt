package com.mobixell.xtt;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Random;
import java.util.TreeSet;
import java.util.SortedSet;

/**
 * UCPWorker. Processes a connection which has been received by the SMSCServer

 * @author Roger Soder
 * @version $Id: UCPWorker.java,v 1.35 2010/03/18 05:30:40 rajesh Exp $
 */
public class UCPWorker extends Thread implements UCPConstants, SMSCWorker
{
    // True if the Worker should stop
    private boolean stop = false;
    // ID of the Worker
    private int id;

    // Number of instances running
    private static int instances=0;
    // Key for the instances and the current message id
    private static Object key = new Object();

    // Input and Output Streams
    private BufferedOutputStream gucpOUT=null;
    private BufferedInputStream gucpIN=null;

    // request count
    private int reqCount=0;
    private int maxRequestCount = -1;

    private int windowSize=1;
    private int openWindows=0;
    private Object windowKey=new Object();

    private int responseDelay=0;
    private int maxSessions=0;
    private int sessionTimeout=0;

    private String myOAdC=null;

    // System ID that is allowed to bind, empty if any
    private static String systemid="";
    // Password used to bind to the smsc
    private static String password="";
    // Key for the password
    private static Object passwordKey=new Object();
    // System name the SMSC answers with
    private static String systemname="XTT-724-SMSC";

    // null if not connected, else the systemid of the connection
    private static Vector<String> connected  =new Vector<String>();
    private static Vector<String> connectedIp=new Vector<String>();
    private static HashMap<String,Vector<String>> sessions   = new HashMap<String,Vector<String>>();

    private String myConnection=null;
    private String myConnectionIp=null;
    // Key for waiting for a connection
    private static Object conkey = new Object();
    private static int concount  = 0;
    // Key for waiting for a message
    private static Object msgkey = new Object();
    // number of messages received
    private static int msgcount  = 0;
    // Key for waiting for a message
    private static Object omsgkey         = new Object();
    // number of messages received
    private static int omsgcount          = 0;
    private static int overrideMsgCount   = -1;
    private static int overrideReturnCode = 0;
    private static String overridePattern = null;


    // Key for waiting for a wsp message (all udh packets available)
    private static Object wspkey = new Object();
    // number of wsp messages received
    private static int wspcount  = 0;

    // Stores previous UDH Segments for connecting them togehter
    private static UDHSegmentStore udhSegmentStore=new UDHSegmentStore();

    // Stores all open Workers with ID as key:
    private static Map<String,UCPWorker> workerMap= Collections.synchronizedMap(new HashMap<String,UCPWorker>());
    private static Map<String,String> workerIDIPMap= Collections.synchronizedMap(new HashMap<String,String>());
    // Stores all workers with Phone number as key
    private static HashMap<String,UCPWorker> workers = new HashMap<String,UCPWorker>();
    private String myRemoteConnection=null;
    private UCPWorker thisWorker=this;

    private byte[] response=null;
    private boolean gotResponse=false;
    private Object responseKey=new Object();
    private int responseWindow=0;

    private static int readtimeout=0;
    public static void setReadTimeout(int timeout){readtimeout=timeout;}

    private SMSCServer myServer=null;
    private String myServerPort="";

    private boolean injectAutoMessages=true;
    private int autoMessagesTRN=0;
    private int earliestAutoMessageSendTime=0;
    private int latestAutoMessageSendTime=0;
    private int autoMessageRetryTime=500;
    private static Random random=new Random();
    private SortedSet<SMSCOriginatedMessage> autoMessages = Collections.synchronizedSortedSet(new TreeSet<SMSCOriginatedMessage>());

    private static int nextMessageDelay=0;public static void setNextMessageDelay(int delay){nextMessageDelay=delay;}

    private static Map<String,Subscriber> subscriberSCTS=Collections.synchronizedMap(new HashMap<String,Subscriber>());

    /* Socket to client we're handling, which will be set by the SMSCServer
       when dispatching the request to us */
    private Socket socket = null;

    /**
     * Creates a new UCPWorker.
     *
     * @param id     ID number of this worker thread
     * @param se      Socket of the connection
     * @param maxRequestCount   this number specifies how many requests
     *                          will be accepted on the socket before it is
     *                          closed. A negative number means, never close
     *                          the socket.
     * @param windowSize        integer containing the window size of the connections
     * @param responseDelay     integer containing the number of miliseconds to wait before sending a response.
     * @param maxSessions       maximum number of sessions
     * @param sessionTimeout    integer in miliseconds
     */
    public UCPWorker(int id,Socket se,SMSCServer myServer, int maxRequestCount, int windowSize, int responseDelay, int maxSessions, int sessionTimeout
        ,boolean injectAutoMessages, int earliestAutoMessageSendTime, int latestAutoMessageSendTime, int autoMessageRetryTime)
    {
        super("UCPWorker-"+id);
        this.socket = se;
        this.id = id;
        this.maxRequestCount = maxRequestCount;
        this.windowSize = windowSize;
        this.responseDelay = responseDelay;
        this.maxSessions = maxSessions;
        this.sessionTimeout = sessionTimeout;
        this.injectAutoMessages=injectAutoMessages;
        this.earliestAutoMessageSendTime=earliestAutoMessageSendTime;
        this.latestAutoMessageSendTime=latestAutoMessageSendTime;
        this.autoMessageRetryTime=autoMessageRetryTime;
        this.myServer=myServer;
        if(myServer==null)
        {
            myServerPort="-";
        } else
        {
            myServer.addWorker();
            myServerPort=myServer.getPort()+"";
            if(sessions.get(myServerPort)==null)sessions.put(myServerPort,new Vector<String>());
        }
        if(id>=0)
        {
            myRemoteConnection=socket.getInetAddress()+":"+socket.getPort();
            workerMap.put(id+"",this);
            workerIDIPMap.put(myRemoteConnection,id+"");
        }
    }

    public int getWorkerId()
    {
        return id;
    }
    public int getInstances()
    {
        synchronized (key)
        {
            return instances;
        }
    };

    /**
     * set flag asking worker thread to stop
     */
    public void setStop()
    {
        this.stop = true;
        try
        {
            XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): stop request for id: "+id+" -> closing socket");
            socket.close();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        synchronized(this)
        {
            notifyAll();
        }
        XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): setStop() finished");
    }

    /**
     * Start the worker thread
     */
    public synchronized void run()
    {
        handleClient();
    }

    /**
     * Handles the ucp request
     * @throws IOException
     */
    private void handleClient()
    {
        // Increase the current count of running workers
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instances "+instances);
            key.notify();
        }

            XTTProperties.printDebug("Client connected: "+socket.getRemoteSocketAddress()+"\n"+socket.getLocalSocketAddress()+"\n");


        try
        {
            // Set the streams
            gucpIN = new BufferedInputStream(socket.getInputStream());
            gucpOUT = new BufferedOutputStream(socket.getOutputStream());

            // Say hello to the world
            XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): Client connected: "+socket.getRemoteSocketAddress()+"\n");
            // Create a new header object
            PDUHeader pdu_head=null;
            // As long as it takes
            boolean doWhile=true;
            // do the loop
            while(doWhile&&socket.isConnected()&&!socket.isClosed()&&!stop)
            {
                pdu_head=new PDUHeader(gucpIN, gucpOUT);

                // Try reading the first 13 Bytes fo the header which are always the same
                // This method finishes on a disconnect or close of the socket
                // and of course on receiving the 13 bytes
                // it returns false when there was no data in 500ms
                // it returns true else
                do
                {
                    // Inject automessages
                    if(injectAutoMessages)injectAutoMessage(gucpOUT);
                }while(!pdu_head.readPDUHeader());

                if(maxRequestCount==0&&reqCount>maxRequestCount)
                {
                    return;
                }
                synchronized(windowKey)
                {
                    if(openWindows>windowSize)
                    {
                        XTTProperties.printWarn("UCPWorker("+myServerPort+"/"+getWorkerId()+"): windowSize reached: "+openWindows+" disconnecting");
                        doWhile=false;
                        return;
                    }

                }
                if(pdu_head.command_operation=='R')
                {
                    synchronized(responseKey)
                    {
                        response=readResponse("UCPWorker("+myServerPort+"/"+getWorkerId()+").readResponse",gucpIN,pdu_head);
                        gotResponse=true;
                        responseWindow--;
                        responseKey.notifyAll();
                        XTTProperties.printTransaction("UCPWORKER/RESPONSE"+XTTProperties.DELIMITER+ConvertLib.createString(response));
                    }
                } else
                {
                    doWhile=switchCommand(pdu_head);
                }
                reqCount++;
                if(maxRequestCount>0&&reqCount>maxRequestCount)
                {
                    return;
                }

                if(nextMessageDelay>0)
                {
                    Thread.sleep(nextMessageDelay);
                }
            }
        } catch (java.net.SocketTimeoutException ste)
        {
            if(sessionTimeout>0)
            {
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): timeout after "+sessionTimeout+"ms");
            } else
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+ste.getMessage());
                XTTProperties.printException(ste);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }

        // Something was wrong with the socket, perhaps it was closed on a set Stop
        } catch (java.net.SocketException se)
        {
            XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): java.net.SocketException: "+se.getMessage());
            //XTTProperties.printException(se);
        // Everything else goes here which should not happen
        } catch (Exception e)
        {
            XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+e.getMessage());
            XTTProperties.printException(e);
            //XTTProperties.setTestStatus(XTTProperties.FAILED);
        } finally
        {
            // The connection is definitively gone now
            synchronized(conkey)
            {
                connected.remove(myConnection);
                connectedIp.remove(myConnectionIp);
                ((Vector)sessions.get(myServerPort)).remove(myConnection);
                myConnection=null;
                myConnectionIp=null;
                workers.remove(myOAdC);
                conkey.notifyAll();
            }
            // Make sure the socket is closed
            try
            {
                socket.close();
            } catch (java.io.IOException ioex){}
            myServer.removeWorker(this);
            XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): Client disconnected");
            // Decrease the number of running instances
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            workerMap.remove(id+"");
            workerIDIPMap.remove(myRemoteConnection);
        }
    }

    private boolean switchCommand(PDUHeader pdu_head) throws Exception
    {
        // Decide what to do on the received header command
        switch(pdu_head.command_operation_type)
        {
            // Connection requested
            case SESSION_MANAGEMENT:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): SESSION_MANAGEMENT");
                cmd_type60(pdu_head);
                break;
            case CALL_INPUT_OPERATION:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): CALL_INPUT_OPERATION");
                cmd_type01(pdu_head);
                break;
            case MULTIADDRESS_CALL_INPUT:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): MULTIADDRESS_CALL_INPUT");
                cmd_type02(pdu_head);
                break;
            case SUPPSERVICE_CALL_INPUT:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): SUPPSERVICE_CALL_INPUT");
                cmd_type03(pdu_head);
                break;
            case SMS_TRANSFER_OPERATION:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): SMS_TRANSFER_OPERATION");
                cmd_type30(pdu_head);
                break;
            case SMT_ALERT_OPERATION:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): SMT_ALERT_OPERATION");
                cmd_type31(pdu_head);
                break;
            case SUBMIT_SHORT_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): SUBMIT_SHORT_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELIVER_SHORT_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): DELIVER_SHORT_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELIVER_NOTIFICATION:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): DELIVER_NOTIFICATION");
                cmd_type5x(pdu_head);
                break;
            case MODIFY_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): MODIFY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case INQUIRY_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): INQUIRY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELETE_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): DELETE_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case RESPONSE_INQUIRY_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): RESPONSE_INQUIRY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case RESPONSE_DELETE_MESSAGE:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): RESPONSE_DELETE_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            // This is happening when we have a disconnect on the connection
            // because -1 is not an allowed command id
            case -1:
                XTTProperties.printVerbose("UCPWorker("+myServerPort+"/"+getWorkerId()+"): pdu empty, possible disconnect");
                return false;
            // A command was received that we currently don't support
            default:
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"): unsupported command operation type: "+pdu_head.command_operation_type);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
        }
        return true;
    }

    private void cmd_type60(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType60Reply reply=new CmdType60Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType60Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType60Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();

                // Store the variables in memory under the OAdC
                myOAdC=body[1];
                String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                long xtttimestamp=System.currentTimeMillis();

                XTTProperties.printDebug("UCPWorker.cmd_type60("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]
                    +"\n  "+"XTTSERVERPORT = "+myServerPort
                    +"\n  "+"OAdC = "+body[1]
                    +"\n  "+"OTON = "+body[2]
                    +"\n  "+"ONPI = "+body[3]
                    +"\n  "+"STYP = "+body[4]
                    +"\n  "+"PWD  = "+ConvertLib.getStringFromByteString(body[5])+" ("+body[5]+")"
                    +"\n  "+"NPWD = "+body[6]
                    +"\n  "+"VERS = "+body[7]
                    +"\n  "+"LAdC = "+body[8]
                    +"\n  "+"LTON = "+body[9]
                    +"\n  "+"LNPI = "+body[10]
                    +"\n  "+"OPID = "+body[11]
                    +"\n  "+"RES1 = "+body[12]
                );
                XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                XTTProperties.setMultipleVariables(storeVar,"/OAdC"  ,body[1]);
                XTTProperties.setMultipleVariables(storeVar,"/OTON"  ,body[2]);
                XTTProperties.setMultipleVariables(storeVar,"/ONPI"  ,body[3]);
                XTTProperties.setMultipleVariables(storeVar,"/STYP"  ,body[4]);
                XTTProperties.setMultipleVariables(storeVar,"/PWD"   ,ConvertLib.getStringFromByteString(body[5]));
                XTTProperties.setMultipleVariables(storeVar,"/NPWD"  ,body[6]);
                XTTProperties.setMultipleVariables(storeVar,"/VERS"  ,body[7]);
                XTTProperties.setMultipleVariables(storeVar,"/LAdC"  ,body[8]);
                XTTProperties.setMultipleVariables(storeVar,"/LTON"  ,body[9]);
                XTTProperties.setMultipleVariables(storeVar,"/LNPI"  ,body[10]);
                XTTProperties.setMultipleVariables(storeVar,"/OPID"  ,body[11]);
                XTTProperties.setMultipleVariables(storeVar,"/RES1"  ,body[12]);

                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE60"+XTTProperties.DELIMITER+body[1]+XTTProperties.DELIMITER+ConvertLib.getStringFromByteString(body[5])+XTTProperties.DELIMITER+body[4]);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = SESSION_MANAGEMENT;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                // Send the ACK response
                byte[] systemmessage=ConvertLib.getOctetByteArrayFromString(systemname);

                // Get the current system password
                String thispw=null;
                synchronized(passwordKey)
                {
                    thispw=new String(password);
                }
                int numSessions=0;
                synchronized(conkey)
                {
                    numSessions=((Vector)sessions.get(myServerPort)).size();
                }
                boolean positive=true;
                if (!thispw.equals("")&&!ConvertLib.getStringFromByteString(body[5]).equals(thispw))
                {
                    outHead.writePDU("/N/07/Invalid Password/",mywindow);
                    positive=false;
                } else if(maxSessions>0&&numSessions>=maxSessions)
                {
                    outHead.writePDU("/N/04/Session not accepted, number of sessions exceeeded/",mywindow);
                    positive=false;
                } else
                {
                    if(sessionTimeout>0)
                    {
                        socket.setSoTimeout(sessionTimeout);
                    }
                    outHead.writePDU("/A/"+ConvertLib.outputBytes(systemmessage,0,systemmessage.length,2,"")+"/",mywindow);
                }
                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                // Notify all waiting for a connect
                if(positive)
                {
                    synchronized(conkey)
                    {
                        myConnection=body[1];
                        myConnectionIp=socket.getInetAddress().toString();
                        connected.add(myConnection);
                        sessions.get(myServerPort).add(myConnection);
                        workers.put(myOAdC,thisWorker);
                        connectedIp.add(myConnectionIp);
                        conkey.notifyAll();
                        concount++;
                    }
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }

    }

    private void cmd_type5x(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType5XReply reply=new CmdType5XReply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType5XReply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType5XReply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            boolean wspb=false;
            int errorcode=0;
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                String AdC=null;
                String oadc="";
                String otoa="";
                long xtttimestamp=System.currentTimeMillis();

                if(body.length!=35)
                {
                    errorcode=SYNTAX_ERROR;
                } else
                {
                    // Store the variables in memory under the OAdC
                    int i=1;
                    String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                    XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                    XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                    buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                    buffer.append("\n  "+"AdC    = "+body[i]);
                    AdC=body[i];
                    XTTProperties.setMultipleVariables(storeVar,"/AdC"    ,body[i++]);
                    buffer.append("\n  "+"OAdC   = "+body[i]);
                    oadc=body[i];
                    XTTProperties.setMultipleVariables(storeVar,"/OAdC"   ,body[i++]);
                    buffer.append("\n  "+"AC     = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/AC"     ,body[i++]);
                    buffer.append("\n  "+"NRq    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NRq"    ,body[i++]);
                    buffer.append("\n  "+"NAdC   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NAdC"   ,body[i++]);
                    buffer.append("\n  "+"NT     = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NT"     ,body[i++]);
                    buffer.append("\n  "+"NPID   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NPID"   ,body[i++]);
                    buffer.append("\n  "+"LRq    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/LRq"    ,body[i++]);
                    buffer.append("\n  "+"LRAd   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/LRAd"   ,body[i++]);
                    buffer.append("\n  "+"LPID   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/LPID"   ,body[i++]);
                    buffer.append("\n  "+"DD     = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/DD"     ,body[i++]);
                    buffer.append("\n  "+"DDT    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/DDT"    ,body[i++]);
                    buffer.append("\n  "+"VP     = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/VP"     ,body[i++]);
                    buffer.append("\n  "+"RPID   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/RPID"   ,body[i++]);
                    buffer.append("\n  "+"SCTS   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/SCTS"   ,body[i++]);
                    buffer.append("\n  "+"Dst    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/Dst"    ,body[i++]);
                    buffer.append("\n  "+"Rsn    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/Rsn"    ,body[i++]);
                    buffer.append("\n  "+"DSCTS  = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/DSCTS"  ,body[i++]);

                    buffer.append("\n  "+"MT     = "+body[i]);
                    String MT=body[i];
                    XTTProperties.setMultipleVariables(storeVar,"/MT"     ,body[i++]);
                    String msg="";
                    if(body[i-1].equals("2"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setMultipleVariables(storeVar,"/NB"  ,body[i++]);
                        buffer.append("\n  "+"NMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*640)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setMultipleVariables(storeVar,"/NMsg",body[i++]);
                    } else if(body[i-1].equals("3"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setMultipleVariables(storeVar,"/NB"  ,body[i++]);
                        buffer.append("\n  "+"AMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*640)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setMultipleVariables(storeVar,"/AMsg",body[i++]);
                    } else if(body[i-1].equals("4"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setMultipleVariables(storeVar,"/NB"  ,body[i++]);
                        buffer.append("\n  "+"TMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*160)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setMultipleVariables(storeVar,"/TMsg",body[i++]);
                    }
                    buffer.append("\n  "+"MMS    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/MMS"    ,body[i++]);
                    buffer.append("\n  "+"PR     = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/PR"     ,body[i++]);
                    buffer.append("\n  "+"DCs    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/DCs"    ,body[i++]);
                    buffer.append("\n  "+"MCLs   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/MCLs"   ,body[i++]);
                    buffer.append("\n  "+"RPI    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/RPI"    ,body[i++]);
                    buffer.append("\n  "+"CPg    = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/CPg"    ,body[i++]);
                    buffer.append("\n  "+"RPLy   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/RPLy"   ,body[i++]);
                    buffer.append("\n  "+"OTOA   = "+body[i]);
                    otoa=body[i];
                    XTTProperties.setMultipleVariables(storeVar,"/OTOA"   ,body[i++]);
                    buffer.append("\n  "+"HPLMN  = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/HPLMN"  ,body[i++]);
                    buffer.append("\n  "+"XSer   = "+body[i]);
                    String XSer=body[i];
                    int numDD=0;
                    if(XSer.startsWith("01"))
                    {
                        numDD=(Integer.decode("0x"+XSer.substring(2,4))).intValue();
                        buffer.append("\n  "+"         "+"XSer Type of service 01, GSM UDH information - "+numDD+" Octets");
                    }
                    XTTProperties.setMultipleVariables(storeVar,"/XSer"   ,body[i++]);
                    buffer.append("\n  "+"RES4   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/RES4"   ,body[i++]);
                    buffer.append("\n  "+"RES5   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/RES5"   ,body[i++]);

                    XTTProperties.printDebug("UCPWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]+buffer);

                    if(XSer.startsWith("01"))
                    {
                        byte[] udhbody=ConvertLib.getBytesFromByteString(XSer.substring(4,4+numDD*2)+msg);
                        //UDH Indicator present
                        UDHDecoder udh=new UDHDecoder(udhbody,0,storeVar,udhSegmentStore);
                        udh.decode();
                        // if we have all segments of an udh we decode the completed data
                        if(udh.hasAllSegments())
                        {
                            // Decode the complete data as WSP protocoll
                            XTTProperties.printDebug("UCPWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: all segments received, decoding WSP:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                            WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                            wsp.decode();
                            // Finaly decode the remaining data as mms
                            MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                            mms.decode();
                            wspb=true;
                        } else
                        {
                            XTTProperties.printDebug("UCPWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: not all segments received, waiting to decode");
                        }
                    }
                    XTTProperties.printTransaction("UCPWORKER/CMD_TYPE5X"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);
                }

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                // Send the ACK response
                byte[] systemmessage=ConvertLib.getOctetByteArrayFromString(systemname);

                synchronized(omsgkey)
                {
                    if((overrideMsgCount>=0&&omsgcount>=overrideMsgCount)||ConvertLib.checkPattern(AdC,overridePattern))
                    {
                        errorcode=overrideReturnCode;
                    }
                    omsgcount++;
                    omsgkey.notifyAll();
                }

                if(errorcode>0)
                {
                    String errorstring=""+errorcode;
                    if(errorcode<10)errorstring="0"+errorcode;
                    switch(errorcode)
                    {
                        case MESSAGE_TOO_LONG:
                            outHead.writePDU("/N/"+errorstring+"/Message too long/",mywindow);
                            break;
                        case SYNTAX_ERROR:
                            outHead.writePDU("/N/"+errorstring+"/Syntax error/",mywindow);
                            break;
                        default:
                            outHead.writePDU("/N/"+errorstring+"/Generic error/",mywindow);
                            break;
                    }
                } else
                {
                    String msgdate=createDate(AdC);
                    switch(pdu_head.command_operation_type)
                    {
                        case SUBMIT_SHORT_MESSAGE:
                            outHead.writePDU("/A/"+"/"+AdC+":"+msgdate+"/",mywindow);
                            if(injectAutoMessages)
                            {
                                if(otoa.equals("5039"))
                                {
                                    byte[] i=socket.getInetAddress().getAddress();
                                    oadc=""+ConvertLib.intToString(0xFF&i[0],3)+ConvertLib.intToString(0xFF&i[1],3)+ConvertLib.intToString(0xFF&i[2],3)+ConvertLib.intToString(0xFF&i[3],3)+socket.getPort();
                                }
                                byte[] amsg=ConvertLib.createBytes(new String("Message for "+AdC+" with identification "+msgdate+" has been delivered"));
                                XTTProperties.printDebug("UCPWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): created auto message type "+DELIVER_NOTIFICATION);
                                addAutoMessage(new SMSCOriginatedMessage('O',DELIVER_NOTIFICATION,"/"+oadc+"/"+AdC+"/////////////"+msgdate+"/0/000/"+createDate()+"/3//"+ConvertLib.outputBytes(amsg,0,amsg.length)+"/////////////"));
                            }
                            break;
                        case DELIVER_SHORT_MESSAGE:
                            outHead.writePDU("/A/"+"/"+AdC+":"+msgdate+"/",mywindow);
                            break;
                        case DELIVER_NOTIFICATION:
                            outHead.writePDU("/A/"+"/"+msgdate+"/",mywindow);
                            break;
                        case MODIFY_MESSAGE:
                            outHead.writePDU("/A/"+"/"+AdC+":"+msgdate+"/",mywindow);
                            break;
                        case INQUIRY_MESSAGE:
                            outHead.writePDU("/A/"+"/"+msgdate+"/",mywindow);
                            break;
                        case DELETE_MESSAGE:
                            outHead.writePDU("/A/"+"/"+msgdate+"/",mywindow);
                            break;
                        case RESPONSE_INQUIRY_MESSAGE:
                            outHead.writePDU("/A/"+"/"+"/",mywindow);
                            break;
                        case RESPONSE_DELETE_MESSAGE:
                            outHead.writePDU("/A/"+"/"+ConvertLib.outputBytes(systemmessage,0,systemmessage.length,2,"")+"/",mywindow);
                            break;
                        default:
                            break;
                    }
                }

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
                if(wspb)
                {
                    synchronized(wspkey)
                    {
                        wspcount++;
                        wspkey.notifyAll();
                    }
                }


            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type01(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType01Reply reply=new CmdType01Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType01Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType01Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                long xtttimestamp=System.currentTimeMillis();

                // Store the variables in memory under the OAdC
                int i=1;
                String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/AdC"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/AC"     ,body[i++]);

                buffer.append("\n  "+"MT     = "+body[i]);
                String MT=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/MT"     ,body[i++]);
                String msg="";
                if(body[i-1].equals("2"))
                {
                    buffer.append("\n  "+"NMsg   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NMsg",body[i++]);
                } else if(body[i-1].equals("3"))
                {
                    buffer.append("\n  "+"AMsg   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/AMsg",body[i++]);
                }

                XTTProperties.printDebug("UCPWorker.cmd_type01("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]+buffer);
                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE01"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+AdC+":"+createDate(AdC)+"/",mywindow);

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private void cmd_type02(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType02Reply reply=new CmdType02Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType02Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType02Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                long xtttimestamp=System.currentTimeMillis();

                // Store the variables in memory under the OAdC
                int i=1;
                String storeVar="smsc/ucp/";

                int numrecipient=Integer.parseInt(body[i++]);
                String[] recipients=new String[numrecipient];
                StringBuffer AdC=new StringBuffer("");
                String tmp="";
                for(int j=0;j<numrecipient;j++)
                {
                    recipients[j]=body[i++];
                    AdC.append(tmp+recipients[j]);
                    tmp=",";
                }
                String oadc=null;
                String MT=null;
                for(int j=0;j<numrecipient;j++)
                {
                    buffer.append("\n "+storeVar+recipients[j]);
                    XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"xtttimestamp",""+xtttimestamp);
                    XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"xttserverport",""+myServerPort);
                    buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                    buffer.append("\n  "+"OAdC   = "+body[i]);
                    oadc=body[i];
                    XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"OAdC"   ,body[i]);
                    buffer.append("\n  "+"AC     = "+body[i+1]);
                    XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"AC"     ,body[i+1]);

                    buffer.append("\n  "+"MT     = "+body[i+2]);
                    MT=body[i+2];
                    XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"MT"     ,body[i+2]);
                    String msg="";
                    if(MT.equals("2"))
                    {
                        buffer.append("\n  "+"NMsg   = "+body[i+3]);
                        XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"NMsg",body[i+3]);
                    } else if(MT.equals("3"))
                    {
                        buffer.append("\n  "+"AMsg   = "+body[i+3]);
                        XTTProperties.setVariable(storeVar+"/"+recipients[j]+"/"+"AMsg",body[i+3]);
                    }
                }

                XTTProperties.printDebug("UCPWorker.cmd_type02("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+buffer);
                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE02"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                StringBuffer response=new StringBuffer("");
                //String date=createDate();

                tmp="";
                for(int j=0;j<numrecipient;j++)
                {
                    response.append(tmp+recipients[j]+":"+createDate(recipients[j]));
                    tmp=",";
                }

                outHead.writePDU("/A/"+response+"/",mywindow);

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private void cmd_type03(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType03Reply reply=new CmdType03Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType03Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=1;
        public CmdType03Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                long xtttimestamp=System.currentTimeMillis();

                // Store the variables in memory under the OAdC
                int i=1;
                String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"RAd    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/RAd"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/AC"     ,body[i++]);
                buffer.append("\n  "+"NPL    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/NPL"    ,body[i++]);
                buffer.append("\n  "+"GA:s   = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/GA:s"   ,body[i++]);
                buffer.append("\n  "+"RP     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/RP"     ,body[i++]);
                buffer.append("\n  "+"PR     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/PR"     ,body[i++]);
                buffer.append("\n  "+"LPR    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/LPR"    ,body[i++]);
                buffer.append("\n  "+"UR     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/UR"     ,body[i++]);
                buffer.append("\n  "+"LUR    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/LUR"    ,body[i++]);
                buffer.append("\n  "+"RC     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/RC"     ,body[i++]);
                buffer.append("\n  "+"LRC    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/LRC"    ,body[i++]);
                buffer.append("\n  "+"DD     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/DD"     ,body[i++]);
                buffer.append("\n  "+"DDT    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/DDT"    ,body[i++]);

                buffer.append("\n  "+"MT     = "+body[i]);
                String MT=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/MT"     ,body[i++]);
                String msg="";
                if(body[i-1].equals("2"))
                {
                    buffer.append("\n  "+"NMsg   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/NMsg",body[i++]);
                } else if(body[i-1].equals("3"))
                {
                    buffer.append("\n  "+"AMsg   = "+body[i]);
                    XTTProperties.setMultipleVariables(storeVar,"/AMsg",body[i++]);
                }

                XTTProperties.printDebug("UCPWorker.cmd_type03("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]+buffer);
                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE03"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+AdC+":"+createDate(AdC)+"/",mywindow);

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type30(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType30Reply reply=new CmdType30Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType30Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType30Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                long xtttimestamp=System.currentTimeMillis();

                // Store the variables in memory under the OAdC
                int i=1;
                String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/AdC"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/AC"     ,body[i++]);
                buffer.append("\n  "+"NRq    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/NRq"    ,body[i++]);
                buffer.append("\n  "+"NAd    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/NAd"    ,body[i++]);
                buffer.append("\n  "+"NPID   = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/NPID"   ,body[i++]);
                buffer.append("\n  "+"DD     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/DD"     ,body[i++]);
                buffer.append("\n  "+"DDT    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/DDT"    ,body[i++]);
                buffer.append("\n  "+"VP     = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/VP"     ,body[i++]);
                buffer.append("\n  "+"AMsg   = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/AMsg"   ,body[i++]);

                XTTProperties.printDebug("UCPWorker.cmd_type30("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]+buffer);
                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE30"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+"");

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+"/"+AdC+":"+createDate(AdC)+"/",mywindow);

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type31(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getUcpIN());
        if(pdu_body.getError())return;

        CmdType31Reply reply=new CmdType31Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"UCPWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdType31Reply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdType31Reply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                String[] body=pdu_body.getBody();
                StringBuffer buffer=new StringBuffer("");
                long xtttimestamp=System.currentTimeMillis();

                // Store the variables in memory under the OAdC
                int i=1;
                String[] storeVar=new String[]{"smsc/ucp/"+body[1],"smsc/ucp/request"};
                XTTProperties.setMultipleVariables(storeVar,"/xtttimestamp",""+xtttimestamp);
                XTTProperties.setMultipleVariables(storeVar,"/xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setMultipleVariables(storeVar,"/AdC"    ,body[i++]);
                buffer.append("\n  "+"PID    = "+body[i]);
                XTTProperties.setMultipleVariables(storeVar,"/PID"    ,body[i++]);

                XTTProperties.printDebug("UCPWorker.cmd_type31("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  UCP:"+pdu_body.getReceived()+"etx\n stored in "+storeVar[0]+buffer);
                XTTProperties.printTransaction("UCPWORKER/CMD_TYPE30"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+""+XTTProperties.DELIMITER+"");

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getUcpIN(),pdu_head.getUcpOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/0000/",mywindow);

                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    socket.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private String createDate(String subscriber)
    {
        Subscriber s=subscriberSCTS.get(subscriber);
        if(s==null)
        {
            s=new Subscriber();
            subscriberSCTS.put(subscriber,s);
        }
        return s.getSCTS();
    }
    private class Subscriber
    {
        long lastSCTS=0;
        public String getSCTS()
        {
            long currentSCTS=System.currentTimeMillis()/1000;
            if(lastSCTS>=currentSCTS)currentSCTS=lastSCTS+1;
            lastSCTS=currentSCTS;
            return createDate(currentSCTS*1000);
        }
    }
    private String createDate()
    {
        return createDate(-1);
    }
    private String createDate(long epochDate)
    {
        String dateFormat="ddMMyykkmmss";//"yyyy'-'MM'-''T'kk':'mm':'ss'Z'";
        String datestring="";
        SimpleDateFormat format=new SimpleDateFormat(dateFormat,Locale.US);
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
        if(epochDate>0)
        {
            calendar.setTime(new java.util.Date(epochDate));
        }
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        datestring=format.format(calendar.getTime());
        return datestring;
    }

    private long getTimeInMillis()
    {
        GregorianCalendar c=new GregorianCalendar();
        return c.getTimeInMillis();
    }

    private void addAutoMessage(SMSCOriginatedMessage message)
    {
        autoMessages.add(message);
    }

    private void injectAutoMessage(BufferedOutputStream out) throws Exception
    {
        if(autoMessages.size()<=0)return;

        long currentTime=getTimeInMillis();
        SMSCOriginatedMessage message=autoMessages.first();
        XTTProperties.printDebug("UCPWorker$injectAutoMessage("+myServerPort+"/"+getWorkerId()+"): currentTime="+currentTime+" sendtime="+message.getSendTime());

        if(message.getSendTime()<=currentTime)
        {
            synchronized(responseKey)
            {
                if(responseWindow<windowSize)
                {
                    responseWindow++;
                    message.writePDU(out,autoMessagesTRN++);
                    autoMessages.remove(message);
                }
            }
        }
    }
    private class SMSCOriginatedMessage implements Comparable<SMSCOriginatedMessage>
    {
        public char command_operation      = ' ';
        public int  command_operation_type = -1;
        private long sendtime=0;public long getSendTime(){return sendtime;};
        private String message="";
        private PDUHeader pduhead=null;

        public SMSCOriginatedMessage(char command_operation,int command_operation_type,String messageBody)
        {
            this.message=messageBody;
            this.pduhead=pduhead;
            this.sendtime=getTimeInMillis();
            this.command_operation=command_operation;
            this.command_operation_type=command_operation_type;
            synchronized(random)
            {
                if(latestAutoMessageSendTime-earliestAutoMessageSendTime>0)
                {
                    sendtime=sendtime+earliestAutoMessageSendTime+random.nextInt(latestAutoMessageSendTime-earliestAutoMessageSendTime);
                } else
                {
                    sendtime=sendtime+earliestAutoMessageSendTime;
                }
            }
        }
        public String toString()
        {
            return message;
        }
        public int compareTo(SMSCOriginatedMessage o)
        {
            if(sendtime<o.sendtime)return -1;
            if(sendtime>o.sendtime)return 1;
            return 0;
        }
        public void writePDU(BufferedOutputStream out,int command_trn) throws Exception
        {
            String databody=message;
            int command_length=0;
            if(databody!=null)
            {
                command_length=HEADLENGTH+databody.length()+2;
            } else
            {
                //throw new NullPointerException("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): Response body may not be null");
                throw new NullPointerException("UCPWorker$SMSCOriginatedMessage("+myServerPort+"/"+getWorkerId()+"): Response body may not be null");
            }
            if(responseDelay>0)
            {
                Thread.sleep(responseDelay);
            }
            String response=ConvertLib.intToString(command_trn,2)
                            +"/"+ConvertLib.intToString(command_length,5)
                            +"/"+command_operation
                            +"/"+ConvertLib.intToString(command_operation_type,2)
                            +databody;
            byte[] body=ConvertLib.getOctetByteArrayFromString(response);
            String checksum=ConvertLib.intToHex(ConvertLib.addBytes(body),2);

            synchronized(out)
            {
                out.write(STX);
                out.write(body);
                out.write(ConvertLib.getOctetByteArrayFromString(checksum));
                out.write(ETX);
                out.flush();
                XTTProperties.printTransaction("UCPWORKER/AUTOINJECTMESSAGE"+XTTProperties.DELIMITER+"stx"+response+checksum+"etx"+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER+autoMessages.size());
            }

            //XTTProperties.printDebug("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): writeBody: \n  UCP: stx"+response+checksum+"etx"
            XTTProperties.printDebug("UCPWorker$SMSCOriginatedMessage("+myServerPort+"/"+getWorkerId()+"): writeBody: \n  UCP: stx"+response+checksum+"etx"
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
        }

    }

    private class PDUBody
    {
        private String body[]  = null;
        private String received= null;
        private int checksum   = 0;
        private int command_operation_type=0;
        private String storeVar= null;

        public String[] getBody()  {return body;}
        public int getCheckSum()   {return checksum;}
        public String getReceived(){return received;}
        public String getStoreVar(){return storeVar;}

        private boolean error = false;
        public boolean getError(){return error;}

        private BufferedInputStream ucpIN  = null;


        public PDUBody()
        {
        }

        public void readPDUBody(PDUHeader inHead, BufferedInputStream in) throws java.lang.Exception
        {
            error=false;
            this.command_operation_type=inHead.command_operation_type;

            byte[] bbody=new byte[inHead.command_length-HEADLENGTH];

            if(readtimeout>0&&socket!=null)
            {
                socket.setSoTimeout(readtimeout);
            }
            int lastByte=-1;
            try
            {
                HTTPHelper.readBytes(in,bbody);

                checksum=ConvertLib.addBytes(bbody,0,bbody.length-2);

                received=ConvertLib.getStringFromOctetByteArray(bbody, 0, bbody.length);
                body=received.split("/");

                lastByte=in.read();
            } catch (java.net.SocketTimeoutException ste)
            {
                XTTProperties.printDebug("UCPWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+ste.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ste);
                }
            }
            try
            {
                if(sessionTimeout>0&&socket!=null)
                {
                    socket.setSoTimeout(sessionTimeout);
                } else if(readtimeout>0&&socket!=null)
                {
                    socket.setSoTimeout(0);
                }
            } catch(Exception ex){}

            if(lastByte!=ETX)
            {
                XTTProperties.printDebug("UCPWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): Received: "+bbody.length+" bytes\n  UCP:"+received);
                XTTProperties.printVerbose("UCPWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): ETX not received: 0x"+ConvertLib.intToHex(lastByte));
                error=true;
                // Create the response
                inHead.command_operation      = 'R';
                inHead.writePDU("/N/02/etx not received/",inHead.getWindow());
                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }

            } else if (!ConvertLib.intToHex(checksum+inHead.getCheckSum(),2).equals(body[body.length-1]))
            {
                XTTProperties.printDebug("UCPWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): Received: "+bbody.length+" bytes\n  UCP:"+received+"etx");
                XTTProperties.printVerbose("UCPWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): CheckSum not valid: calculated "+ConvertLib.intToHex(checksum+inHead.getCheckSum(),2)+"!= received "+body[body.length-1]);
                error=true;
                // Create the response
                inHead.command_operation      = 'R';
                inHead.writePDU("/N/01/Invalid checksum supplied/",inHead.getWindow());
                synchronized(windowKey)
                {
                    openWindows--;
                    windowKey.notifyAll();
                }
                synchronized(msgkey)
                {
                    msgcount++;
                    msgkey.notifyAll();
                }
            }
        }


    }

    private class PDUHeader
    {
        public int  command_length         = 0;
        public char command_operation      = ' ';
        public int  command_operation_type = -1;
        public int  command_trn            = 0;
        private int checksum               = 0;
        private BufferedInputStream ucpIN  = null;
        private BufferedOutputStream ucpOUT= null;
        private byte[] head=null;
        private int thiswindow=-3;
        public int getWindow(){return thiswindow;}

        public byte[] getBytes(){return head;}
        public int getCheckSum(){return checksum;}
        public BufferedInputStream getUcpIN(){return ucpIN;}
        public BufferedOutputStream getUcpOUT(){return ucpOUT;}


        public PDUHeader(BufferedInputStream ucpIN,BufferedOutputStream ucpOUT,char command_operation,int command_operation_type,int command_trn)
        {
            this.ucpIN = ucpIN;
            this.ucpOUT= ucpOUT;
            this.command_operation      = command_operation;
            this.command_operation_type = command_operation_type;
            this.command_trn            = command_trn;
        }
        public PDUHeader(BufferedInputStream ucpIN,BufferedOutputStream ucpOUT)
        {
            // Set the streams
            this.ucpIN = ucpIN;
            this.ucpOUT= ucpOUT;
        }
        public boolean readPDUHeader() throws Exception
        {
            return readPDUHeader(ucpIN);
        }
        public boolean readPDUHeader(BufferedInputStream in) throws Exception
        {
            int firstByte=-1;
            try
            {
                if(socket!=null)socket.setSoTimeout(autoMessageRetryTime);
                firstByte=in.read();
            } catch (java.net.SocketTimeoutException stex)
            {
                return false;
            } finally
            {
                if(socket!=null)socket.setSoTimeout(0);
            }
            if(firstByte!=STX)
            {
                if(firstByte==-1)
                {
                    return true;
                }
                throw new Exception("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): STX not received: 0x"+ConvertLib.intToHex(firstByte));
            }

            head=new byte[HEADLENGTH];
            HTTPHelper.readBytes(in,head);

            XTTProperties.printVerbose("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): Received: 14 bytes");
            command_trn            = ConvertLib.getIntFromStringBytes(head, 0,2);
            command_length         = ConvertLib.getIntFromStringBytes(head, 3,5);
            command_operation      = (char)head[9];
            command_operation_type = ConvertLib.getIntFromStringBytes(head, 11,2);

            checksum=ConvertLib.addBytes(head);

            XTTProperties.printDebug("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): Received: \n  UCP: stx"+new String(head)+""
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
            if(command_operation!='R')
            {
                synchronized(windowKey)
                {
                    openWindows++;
                    thiswindow=openWindows;
                    windowKey.notifyAll();
                }
            }
            return true;
        }
        public void writePDU(String databody, int window) throws Exception
        {
            writePDU(ucpOUT,databody,window);
        }
        public void writePDU(BufferedOutputStream out, String databody, int window) throws Exception
        {
            if(databody!=null)
            {
                command_length=HEADLENGTH+databody.length()+2;
            } else
            {
                throw new NullPointerException("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): Response body may not be null");
            }
            if(responseDelay>0)
            {
                Thread.sleep(responseDelay);
            }
            String response=ConvertLib.intToString(command_trn,2)
                            +"/"+ConvertLib.intToString(command_length,5)
                            +"/"+command_operation
                            +"/"+ConvertLib.intToString(command_operation_type,2)
                            +databody;
            byte[] body=ConvertLib.getOctetByteArrayFromString(response);
            String checksum=ConvertLib.intToHex(ConvertLib.addBytes(body),2);

            synchronized(out)
            {
                out.write(STX);
                out.write(body);
                out.write(ConvertLib.getOctetByteArrayFromString(checksum));
                out.write(ETX);
                out.flush();
            }

            XTTProperties.printDebug("UCPWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): writeBody: \n  UCP: stx"+response+checksum+"etx"
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
        }
    }

    /**
     * Wait on PDU_BIND from any systemid
     */
    public static void waitForBind() throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(conkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("UCPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(connected.isEmpty())
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for any UCP Bind on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("UCPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for any UCP Bind on SMSC no timeout");
                    conkey.wait();
                }
            }
        }
    }

    /**
     * Wait on PDU_BIND from specific OAdC
     */
    public static void waitForBind(String oadc) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(conkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("UCPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            long prevtime=System.currentTimeMillis();
            while(!connected.contains(oadc))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for UCP Bind with OAdC='"+oadc+"' on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(!connected.contains(oadc)&&(System.currentTimeMillis()-prevtime>=wait))
                    {
                        XTTProperties.printFail("UCPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    } else if (connected.contains(oadc))
                    {
                        XTTProperties.printDebug("waitForBind: connection with OAdC='"+oadc+"' found!");
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for UCP Bind with OAdC='"+oadc+"' on SMSC no timeout");
                    conkey.wait();
                }
            }
        }
    }

    /**
     * Wait on PDU_BIND from specific OAdC
     */
    public static void waitForBind(InetAddress remoteAddress) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(conkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("UCPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            long prevtime=System.currentTimeMillis();
            while(!connectedIp.contains(remoteAddress.toString()))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for UCP Bind with remoteAddress='"+remoteAddress+"' on SMSC timeout="+wait);
                    conkey.wait(wait);

                    //System.out.println("connectedIp = " + connectedIp);
                    //System.out.println("remoteAddress.toString() = " + remoteAddress.toString());

                    if(!connectedIp.contains(remoteAddress.toString())&&(System.currentTimeMillis()-prevtime>=wait))
                    {
                        XTTProperties.printFail("UCPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    } else if(connectedIp.contains(remoteAddress.toString()))
                    {
                        //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                        XTTProperties.printDebug("waitForBind: UCP connection with remoteAddress='"+remoteAddress+"' found!");
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for UCP Bind with remoteAddress='"+remoteAddress+"' on SMSC no timeout");
                    conkey.wait();
                }
            }
        }
    }

    /**
     * Wait on MESSAGE from specific systemid
     */
    public static void waitForMessage(String oadc) throws java.lang.InterruptedException
    {
        boolean connection=false;
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(msgkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("UCPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connection)
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForMessage: waiting for UCP Message with OAdC='"+oadc+"' on SMSC timeout:"+wait);
                    msgkey.wait(wait);
                } else
                {
                    XTTProperties.printInfo("waitForMessage: waiting for UCP Message with OAdC='"+oadc+"' on SMSC no timeout");
                    msgkey.wait();
                }
                synchronized(conkey)
                {
                    connection=connected.contains(oadc);
                }
            }
        }
    }
    /**
     * Wait on MESSAGE from any systemid
     */
    public static void waitForMessages(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("UCPWorker.waitForMessages: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        int prevcount=0;

        synchronized(msgkey)
        {
            while(msgcount<number)
            {
                synchronized(conkey)
                {
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("UCPWorker.waitForMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                if(wait>0)
                {
                    XTTProperties.printInfo("UCPWorker.waitForMessages: "+msgcount+"/"+number+" timeout="+wait);
                    prevcount=msgcount;
                    msgkey.wait(wait);
                    if(msgcount==prevcount)
                    {
                        XTTProperties.printFail("UCPWorker.waitForMessages: "+msgcount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("UCPWorker.waitForMessages: "+msgcount+"/"+number);
                    msgkey.wait();
                }
            }
            XTTProperties.printInfo("UCPWorker.waitForMessages: "+msgcount+"/"+number);
        }
    }
    public static void waitForTimeoutMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("UCPWorker.waitForTimeoutMessages: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(msgkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=msgcount+1;
            }
            while(msgcount<number)
            {
                synchronized(conkey)
                {
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("UCPWorker.waitForTimeoutMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                XTTProperties.printInfo("UCPWorker.waitForTimeoutMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=msgcount;
                msgkey.wait(wait);
                if(msgcount==prevcount)
                {
                    XTTProperties.printInfo("UCPWorker.waitForTimeoutMessages: timed out with no messages!");
                    return;
                }
            }
            XTTProperties.printFail("UCPWorker.waitForTimeoutMessages: Message received! "+wspcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void waitForBinds(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("UCPWorker.waitForBinds: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        int prevcount=0;

        synchronized(conkey)
        {
            while(concount<number)
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("UCPWorker.waitForBinds: "+concount+"/"+number+" timeout="+wait);
                    prevcount=concount;
                    conkey.wait(wait);
                    if(concount==prevcount)
                    {
                        XTTProperties.printFail("UCPWorker.waitForBinds: "+concount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("UCPWorker.waitForBinds: "+concount+"/"+number);
                    conkey.wait();
                }
            }
            XTTProperties.printInfo("UCPWorker.waitForBinds: "+concount+"/"+number);
        }
    }
    /**
     * Wait for a number of wsp messages
     */
    public static void waitForWSPMessages(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("UCPWorker.waitForWSPMessages: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        int prevcount=0;

        synchronized(wspkey)
        {
            while(wspcount<number)
            {
                synchronized(conkey)
                {
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("UCPWorker.waitForWSPMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                if(wait>0)
                {
                    XTTProperties.printInfo("UCPWorker.waitForWSPMessages: "+wspcount+"/"+number+" timeout="+wait);
                    prevcount=wspcount;
                    wspkey.wait(wait);
                    if(wspcount==prevcount)
                    {
                        XTTProperties.printFail("UCPWorker.waitForWSPMessages: "+wspcount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("UCPWorker.waitForWSPMessages: "+wspcount+"/"+number);
                    wspkey.wait();
                }
            }
            XTTProperties.printInfo("UCPWorker.waitForWSPMessages: "+wspcount+"/"+number);
        }
    }

    /**
     * Wait for a number of wsp messages
     */
    public static void waitForTimeoutWSPMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("UCPWorker.waitForTimeoutWSPMessages: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(wspkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=wspcount+1;
            }
            while(wspcount<number)
            {
                synchronized(conkey)
                {
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("UCPWorker.waitForTimeoutWSPMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                XTTProperties.printInfo("UCPWorker.waitForTimeoutWSPMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=wspcount;
                wspkey.wait(wait);
                if(wspcount==prevcount)
                {
                    XTTProperties.printInfo("UCPWorker.waitForTimeoutWSPMessages: timed out with no WSP messages!");
                    return;
                }
            }
            XTTProperties.printFail("UCPWorker.waitForTimeoutWSPMessages: WSP message received! "+wspcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    public static void setPassword(String pw)
    {
        synchronized(passwordKey)
        {
            password=new String(pw);
        }
    }
    public static String getPassword()
    {
        synchronized(passwordKey)
        {
            return new String(password);
        }
    }


    public static void setOverrideReturnCode(int overCode,int overCount)
    {
        synchronized(omsgkey)
        {
            overrideReturnCode=overCode;
            overrideMsgCount=overCount;
        }
    }
    public static void setOverridePattern(int overCode,String pattern)
    {
        synchronized(omsgkey)
        {
            overridePattern=pattern;
            overrideReturnCode=overCode;
        }
    }
    public static void initialize()
    {
        synchronized(wspkey)
        {
            wspcount=0;
        }
        synchronized(msgkey)
        {
            msgcount=0;
        }
        synchronized(omsgkey)
        {
            omsgcount=0;
            overrideReturnCode=0;
            overrideMsgCount=-1;
        }
        synchronized(conkey)
        {
            connected=new Vector<String>();
            connectedIp=new Vector<String>();
            concount=0;
        }
        // Clear the segment store
        udhSegmentStore.clear();
        subscriberSCTS.clear();
        nextMessageDelay=0;
        overridePattern=null;
    }

    public static byte[] readResponse(String functionname,InputStream in) throws Exception
    {
        return readResponse(functionname,in,null);
    }
    public static byte[] readResponse(String functionname,InputStream in, PDUHeader pdu_head) throws Exception
    {
        byte[] response=null;
        byte[] head=null;
        int command_length=0;
        if(pdu_head==null)
        {
            int firstByte=in.read();
            if(firstByte!=STX)
            {
                throw new Exception("STX not received: 0x"+ConvertLib.intToHex(firstByte));
            }

            head=new byte[HEADLENGTH];
            HTTPHelper.readBytes(in,head);

            XTTProperties.printVerbose(functionname+": Received: 14 bytes");
            int command_trn             = ConvertLib.getIntFromStringBytes(head, 0,2);
            command_length              = ConvertLib.getIntFromStringBytes(head, 3,5);
            char command_operation      = (char)head[9];
            int command_operation_type  = ConvertLib.getIntFromStringBytes(head, 11,2);

            XTTProperties.printDebug(functionname+": Received: \n  UCP: stx"+new String(head)+""
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
        } else
        {
            head=pdu_head.getBytes();
            command_length=pdu_head.command_length;
        }
        int rchecksum=ConvertLib.addBytes(head);


        byte[] bbody=new byte[command_length-HEADLENGTH];
        HTTPHelper.readBytes(in,bbody);

        rchecksum=rchecksum+ConvertLib.addBytes(bbody,0,bbody.length-2);

        String bbodyString=ConvertLib.getStringFromOctetByteArray(bbody, 0, bbody.length);
        String[] body=bbodyString.split("/");

        response=new byte[command_length];
        for(int i=0;i<head.length;i++)
        {
            response[i]=head[i];
        }
        for(int i=0;i<bbody.length;i++)
        {
            response[i+HEADLENGTH]=bbody[i];
        }


        int lastByte=in.read();
        if(lastByte!=ETX)
        {
            XTTProperties.printDebug(functionname+": Received: "+bbody.length+" bytes\n  UCP:"+bbodyString);
            XTTProperties.printFail(functionname + ": Error: ETX not received: 0x"+ConvertLib.intToHex(lastByte));
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return response;
        } else if (!ConvertLib.intToHex(rchecksum,2).equals(body[body.length-1]))
        {
            XTTProperties.printDebug(functionname+": Received: "+bbody.length+" bytes\n  UCP:"+bbodyString+"etx");
            XTTProperties.printFail(functionname + ": Error: CheckSum not valid: calculated "+ConvertLib.intToHex(rchecksum,2)+"!= received "+body[body.length-1]);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return response;
        } else
        {
            XTTProperties.printDebug(functionname+"  : Received: "+bbody.length+" bytes\n  UCP:"+bbodyString+"etx");
        }
        return response;
    }


    public static void awaitUCPMessage(int responseDelay, BufferedInputStream ucpIN,BufferedOutputStream ucpOUT)
    {

        UCPWorker worker=new UCPWorker(-1,null,null,0,1,responseDelay, 0, 0,false,0,0,500);
        boolean ok=worker.awaitUCPMessage(ucpIN, ucpOUT);
    }
    private boolean awaitUCPMessage(BufferedInputStream ucpIN,BufferedOutputStream ucpOUT)
    {

        try
        {
            PDUHeader pdu_head=new PDUHeader(ucpIN, ucpOUT);
            // Try reading the first 13 Bytes fo the header which are always the same
            // This method finishes on a disconnect or close of the socket
            // and of course on receiving the 13 bytes
            pdu_head.readPDUHeader();
            return this.switchCommand(pdu_head);
        } catch (Exception e)
        {
            XTTProperties.printFail("UCPWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+e.getMessage());
            XTTProperties.printException(e);
        }
        return false;
    }



    public static BufferedOutputStream getOutputStream(InetAddress ip,int port)
    {
        //System.out.println(ip+":"+port+"\n"+workerIDIPMap);
        return getOutputStream(workerIDIPMap.get(ip+":"+port));
    }
    public static BufferedOutputStream getOutputStream(String workerId)
    {
        //System.out.println(workerId+"\n"+workerMap);
        UCPWorker worker=(UCPWorker)workerMap.get(workerId.trim());
        if(worker==null)
        {
            return null;
        } else
        {
            return worker.getOutputStream();
        }
    }
    public BufferedOutputStream getOutputStream()
    {
        return gucpOUT;
    }

    public static Socket getSocket(String OAdC)
    {
        UCPWorker worker=workers.get(OAdC);
        if(worker==null)return null;
        return worker.getSocket();
    }

    public Socket getSocket()
    {
        return socket;
    }

    public static byte[] getResponse(InetAddress ip,int port)
    {
        return getResponse(workerIDIPMap.get(ip+":"+port));
    }
    public static byte[] getResponse(String workerId)
    {
        UCPWorker worker=(UCPWorker)workerMap.get(workerId);
        if(worker==null)
        {
            return new byte[0];
        } else
        {
            return worker.getResponse();
        }
    }
    public byte[] getResponse()
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        synchronized(responseKey)
        {
            if(response==null&&gotResponse==false)
            {
                try
                {
                    if(wait>0)
                    {
                        XTTProperties.printDebug("UCPWorker.getResponse(): waiting for any UCP Message on SMSC timeout:"+wait);
                        responseKey.wait(wait);
                    } else
                    {
                        XTTProperties.printDebug("UCPWorker.getResponse(): waiting for any UCP Message on SMSC no timeout");
                        responseKey.wait();
                    }
                } catch(java.lang.InterruptedException ex){}
            }
            gotResponse=false;
            byte[] ret=response;
            response=null;
            if(ret==null)
            {
                return new byte[0];
            } else
            {
                return ret;
            }
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: UCPWorker.java,v 1.35 2010/03/18 05:30:40 rajesh Exp $";
}
