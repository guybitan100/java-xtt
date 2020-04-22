package com.mobixell.xtt;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Random;

import java.util.TreeSet;
import java.util.SortedSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;

/**
 * CIMDWorker. Processes a connection which has been received by the SMSCServer

 * @author Roger Soder
 * @version $Id: CIMDWorker.java,v 1.6 2010/03/25 10:18:30 rajesh Exp $
 */
public class CIMDWorker extends CIMDConstants implements SMSCWorker
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
    private BufferedOutputStream gcimdOUT=null;
    private BufferedInputStream gcimdIN=null;

    // request count
    private int reqCount=0;
    private int maxRequestCount = -1;

    private int windowSize=1;
    private int openWindows=0;
    private Object windowKey=new Object();

    private int responseDelay=0;
    private int maxSessions=0;
    private int sessionTimeout=0;

    // System ID that is allowed to bind, empty if any
    private static String systemid="";
    // Password used to bind to the smsc
    private static String password="";
    // Key for the password
    private static Object passwordKey=new Object();
    // System name the SMSC answers with
    private static String systemname="XTT-Mobixell-SMSC";

    // null if not connected, else the systemid of the connection
    private static Vector<String> connected  =new Vector<String>();
    private static Vector<String> connectedIp=new Vector<String>();
    private static HashMap<String,Vector<String>> sessions=new HashMap<String,Vector<String>>();

    private String myConnection=null;
    private String myConnectionIp=null;
    // Key for waiting for a connection
    private static Object conkey = new Object();
    private static int concount  = 0;
    // Key for waiting for a message
    private static Object msgkey = new Object();
    // number of messages received
    private static int msgcount  = 0;

    // Key for waiting for a wsp message (all udh packets available)
    private static Object wspkey = new Object();
    // number of wsp messages received
    private static int wspcount  = 0;

    // Stores previous UDH Segments for connecting them togehter
    private static UDHSegmentStore udhSegmentStore=new UDHSegmentStore();

    // Stores all open Workers with ID as key:
    private static Map<String,CIMDWorker> workerMap= Collections.synchronizedMap(new HashMap<String,CIMDWorker>());
    private static Map<String,String> workerIDIPMap= Collections.synchronizedMap(new HashMap<String,String>());
    private String myRemoteConnection=null;

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


    /* Socket to client we're handling, which will be set by the SMSCServer
       when dispatching the request to us */
    private Socket s = null;

    /**
     * Creates a new CIMDWorker.
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
    public CIMDWorker(int id,Socket se,SMSCServer myServer, int maxRequestCount, int windowSize, int responseDelay, int maxSessions, int sessionTimeout
        ,boolean injectAutoMessages, int earliestAutoMessageSendTime, int latestAutoMessageSendTime, int autoMessageRetryTime)
    {
        this.s = se;
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
            myRemoteConnection=s.getInetAddress()+":"+s.getPort();
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
            XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): stop request for id: "+id+" -> closing socket");
            s.close();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        synchronized(this)
        {
            notifyAll();
        }
        XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): setStop() finished");
    }

    /**
     * Start the worker thread
     */
    public synchronized void run()
    {
        handleClient();
    }

    /**
     * Handles the CIMD request
     * @throws IOException
     */
    private void handleClient()
    {
        // Increase the current count of running workers
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instances "+instances);
            key.notify();
        }

        XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): Client connected: "+s.getRemoteSocketAddress()+"\n"+s.getLocalSocketAddress()+"\n");


        try
        {
            // Set the streams
            gcimdIN = new BufferedInputStream(s.getInputStream());
            gcimdOUT = new BufferedOutputStream(s.getOutputStream());

            // Say hello to the world
            XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): Client connected: "+s.getRemoteSocketAddress()+"\n");
            // Create a new header object
            PDUHeader pdu_head=null;
            // As long as it takes
            boolean doWhile=true;
            // do the loop
            while(doWhile&&s.isConnected()&&!s.isClosed()&&!stop)
            {
                pdu_head=new PDUHeader(gcimdIN, gcimdOUT);

                // Try reading the first 7 Bytes fo the header which are always the same
                // This method finishes on a disconnect or close of the socket
                // and of course on receiving the 13 bytes
                // it returns false when there was no data in 500ms
                // it returns true else
                do
                {
                    // Inject automessages
                    if(injectAutoMessages)injectAutoMessage(gcimdOUT);
                }while(!pdu_head.readPDUHeader());

                if(maxRequestCount==0&&reqCount>maxRequestCount)
                {
                    return;
                }
                synchronized(windowKey)
                {
                    if(openWindows>windowSize)
                    {
                        XTTProperties.printWarn("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): windowSize reached: "+openWindows+" disconnecting");
                        doWhile=false;
                        return;
                    }

                }
                if(pdu_head.command_operation>=50)
                {
                    synchronized(responseKey)
                    {
                        response=readResponse("CIMDWorker("+myServerPort+"/"+getWorkerId()+").readResponse",gcimdIN,pdu_head);
                        gotResponse=true;
                        responseWindow--;
                        responseKey.notifyAll();
                        XTTProperties.printTransaction("CIMDWORKER/RESPONSE"+XTTProperties.DELIMITER+ConvertLib.createString(response));
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
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): timeout after "+sessionTimeout+"ms");
            } else
            {
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+ste.getMessage());
                XTTProperties.printException(ste);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }

        // Something was wrong with the socket, perhaps it was closed on a set Stop
        } catch (java.net.SocketException se)
        {
            XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): java.net.SocketException: "+se.getMessage());
            //XTTProperties.printException(se);
        // Everything else goes here which should not happen
        } catch (Exception e)
        {
            XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+e.getMessage());
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
                conkey.notifyAll();
            }
            // Make sure the socket is closed
            try
            {
                s.close();
            } catch (java.io.IOException ioex){}
            myServer.removeWorker(this);
            XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): Client disconnected");
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
    
    /**
     * Method to handle PDU header command
     * @param pdu_head - Header
     * @return
     * @throws Exception
     */
    private boolean switchCommand(PDUHeader pdu_head) throws Exception
    {
        // Decide what to do on the received header command
        switch(pdu_head.command_operation)
        {
            // Connection requested
            case LOGIN_REQUEST:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): LOGIN_REQUEST");
                cmd_Login(pdu_head);
                break;
            case SUBMIT_MESSAGE_REQUEST:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): SUBMIT_MESSAGE_REQUEST");
                cmd_SubmitMessage(pdu_head);
                break;
        /*
            case CALL_INPUT_OPERATION:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): CALL_INPUT_OPERATION");
                cmd_type01(pdu_head);
                break;
            case MULTIADDRESS_CALL_INPUT:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): MULTIADDRESS_CALL_INPUT");
                cmd_type02(pdu_head);
                break;
            case SUPPSERVICE_CALL_INPUT:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): SUPPSERVICE_CALL_INPUT");
                cmd_type03(pdu_head);
                break;
            case SMS_TRANSFER_OPERATION:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): SMS_TRANSFER_OPERATION");
                cmd_type30(pdu_head);
                break;
            case SMT_ALERT_OPERATION:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): SMT_ALERT_OPERATION");
                cmd_type31(pdu_head);
                break;
            case SUBMIT_SHORT_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): SUBMIT_SHORT_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELIVER_SHORT_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): DELIVER_SHORT_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELIVER_NOTIFICATION:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): DELIVER_NOTIFICATION");
                cmd_type5x(pdu_head);
                break;
            case MODIFY_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): MODIFY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case INQUIRY_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): INQUIRY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case DELETE_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): DELETE_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case RESPONSE_INQUIRY_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): RESPONSE_INQUIRY_MESSAGE");
                cmd_type5x(pdu_head);
                break;
            case RESPONSE_DELETE_MESSAGE:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): RESPONSE_DELETE_MESSAGE");
                cmd_type5x(pdu_head);
                break;
        */
            // This is happening when we have a disconnect on the connection
            // because -1 is not an allowed command id
            case -1:
                XTTProperties.printVerbose("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): pdu empty, possible disconnect");
                return false;
            // A command was received that we currently don't support
            default:
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): unsupported command operation: "+pdu_head.command_operation);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                PDUBody pdu_body=new PDUBody();
                pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
                return false;
        }
        return true;

    }
    
    
    private void cmd_Login(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdTypeLoginReply reply=new CmdTypeLoginReply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdTypeLoginReply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdTypeLoginReply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                boolean positive=true;
                /*
                HashSet<Integer> allowedParameters=new HashSet<Integer>();
                allowedParameters.add(PARAM_USER_IDENTITY);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_SUBADDR);
                allowedParameters.add(PARAM_WINDOW_SIZE);
                HashSet<Integer> mandatoryParameters=new HashSet<Integer>();
                mandatoryParameters.add(PARAM_USER_IDENTITY);
                mandatoryParameters.add(PARAM_PASSWORD);

                Set<Integer> receivedParameters=pdu_body.getParameterKeys();
                if(!allowedParameters.containsAll(receivedParameters))
                {
                    XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): unallowed parameter recieved");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    positive=false;
                }
                if(!receivedParameters.containsAll(mandatoryParameters))
                {
                    XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): missing mandatory parameter");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    positive=false;
                }*/
                String userIdentity=pdu_body.getParameter(PARAM_USER_IDENTITY);
                // Store the variables in memory under the OAdC
                String storeVar[]=new String[]{"smsc/CIMD/"+userIdentity};
                //long xtttimestamp=System.currentTimeMillis();

                pdu_body.storeParameters(storeVar);
                for(int i=0;i<storeVar.length;i++)
                {
                    XTTProperties.printDebug("CIMDWorker.cmd_Login("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): stored  parameters in "+storeVar[i]);
                }

                // Create the response
                PDUHeader outHead          = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation  = LOGIN_RESPONSE;
                outHead.command_trn        = pdu_head.command_trn;

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
                String receivedPassword=pdu_body.getParameter(PARAM_PASSWORD);
                if (!thispw.equals("")&&!thispw.equals(receivedPassword))
                {
                    outHead.writePDU(
                         PARAM_ERROR_CODE+":"+100+"\t"
                        +PARAM_ERROR_TEXT+":"+getErrorMessage(100)+"\t"
                        ,mywindow);
                    positive=false;
                } else if(maxSessions>0&&numSessions>=maxSessions)
                {
                    outHead.writePDU(
                         PARAM_ERROR_CODE+":"+103+"\t"
                        +PARAM_ERROR_TEXT+":"+getErrorMessage(103)+"\t"
                        ,mywindow);
                    positive=false;
                } else
                {
                    if(sessionTimeout>0)
                    {
                        s.setSoTimeout(sessionTimeout);
                    }
                    outHead.writePDU("",mywindow);
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
                        myConnection=userIdentity;
                        myConnectionIp=s.getInetAddress().toString();
                        connected.add(myConnection);
                        sessions.get(myServerPort).add(myConnection);
                        connectedIp.add(myConnectionIp);
                        conkey.notifyAll();
                        concount++;
                    }
                }
            }catch (Exception ex)
            {
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private void cmd_SubmitMessage(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdSubmitMessageReply reply=new CmdSubmitMessageReply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
            t.start();
        } else
        {
            reply.run();
        }

    }

    private class CmdSubmitMessageReply implements Runnable
    {
        PDUBody pdu_body=null;
        PDUHeader pdu_head=null;
        int mywindow=-1;
        public CmdSubmitMessageReply(PDUHeader pdu_head,PDUBody pdu_body, int window)
        {
            this.pdu_head=pdu_head;
            this.pdu_body=pdu_body;
            this.mywindow=window;
        }
        public void run()
        {
            try
            {
                boolean positive=true;
                boolean wspb=false;
                /*
                HashSet<Integer> allowedParameters=new HashSet<Integer>();
                allowedParameters.add(PARAM_DESTINATION_ADDRESS);
                allowedParameters.add(PARAM_ORIGINATING_ADDRESS);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                allowedParameters.add(PARAM_PASSWORD);
                HashSet<Integer> mandatoryParameters=new HashSet<Integer>();
                mandatoryParameters.add(PARAM_DESTINATION_ADDRESS);

                Set<Integer> receivedParameters=pdu_body.getParameterKeys();
                if(!allowedParameters.containsAll(receivedParameters))
                {
                    XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): unallowed parameter recieved");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    positive=false;
                }
                if(!receivedParameters.containsAll(mandatoryParameters))
                {
                    XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): missing mandatory parameter");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    positive=false;
                }*/
                // Store the variables in memory under the OAdC
                String storeVar[]=pdu_body.createStoreVar("smsc/CIMD/",PARAM_DESTINATION_ADDRESS);
                long xtttimestamp=System.currentTimeMillis();
                pdu_body.storeParameters(storeVar);

                for(int i=0;i<storeVar.length;i++)
                {
                    XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp",""+xtttimestamp);
                    XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport",""+myServerPort);
                    XTTProperties.printDebug("CIMDWorker.cmd_SubmitMessage("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): stored  parameters in "+storeVar[i]);
                }

                String userDataHeader=pdu_body.getParameter(PARAM_USER_DATA_HEADER);
                String userDataBinary=pdu_body.getParameter(PARAM_USER_DATA_BINARY);
                if(userDataHeader!=null)
                {
                    byte[] header=ConvertLib.getBytesFromByteString(userDataHeader);
                    byte[] body=new byte[0];
                    if(userDataBinary!=null)
                    {
                        body=ConvertLib.getBytesFromByteString(userDataBinary);
                    }
                    byte[] wspdata=new byte[header.length+body.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(wspdata,pointer,header);
                    pointer=ConvertLib.addBytesToArray(wspdata,pointer,body);

                    UDHDecoder udh=new UDHDecoder(wspdata,0,storeVar,udhSegmentStore);
                    udh.decode();
                    // if we have all segments of an udh we decode the completed data
                    if(udh.hasAllSegments())
                    {
                        // Decode the complete data as WSP protocoll
                        XTTProperties.printDebug("CIMDWorker.cmd_SubmitMessage("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: all segments received, decoding WSP:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                        WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                        wsp.decode();
                        // Finaly decode the remaining data as mms
                        MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                        mms.decode();
                        wspb=true;
                    } else
                    {
                        XTTProperties.printDebug("CIMDWorker.cmd_SubmitMessage("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: not all segments received, waiting to decode");
                    }
                    XTTProperties.printTransaction("CIMDWORKER/CMD_SUBMITMESSAGE"+XTTProperties.DELIMITER+pdu_body.getParameter(PARAM_DESTINATION_ADDRESS)+XTTProperties.DELIMITER+pdu_body.getParameter(PARAM_ORIGINATING_ADDRESS)+XTTProperties.DELIMITER+udh.getReference()+XTTProperties.DELIMITER+udh.getSegments()+XTTProperties.DELIMITER+udh.getSegmentNumber());
                }

                // Create the response
                PDUHeader outHead          = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation  = SUBMIT_MESSAGE_RESPONSE;
                outHead.command_trn        = pdu_head.command_trn;

                String msgdate=createDate();
                StringBuffer response=new StringBuffer();
                Vector<String> destinations=pdu_body.getParameters(PARAM_DESTINATION_ADDRESS);
                Iterator<String> it=destinations.iterator();
                while(it.hasNext())
                {
                    response.append(createResponseParameter(PARAM_DESTINATION_ADDRESS,it.next())+createResponseParameter(PARAM_SERVICE_CENTRE_TIME_STAMP,msgdate));
                    //response.append(ConvertLib.intToString(PARAM_DESTINATION_ADDRESS,3)+":"+it.next()+"\t"+ConvertLib.intToString(PARAM_SERVICE_CENTRE_TIME_STAMP,3)+":"+msgdate+"\t");
                }

                outHead.writePDU(response.toString(),mywindow);

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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }

/*

    private void cmd_type5x(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType5XReply reply=new CmdType5XReply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                long xtttimestamp=System.currentTimeMillis();

                if(body.length!=35)
                {
                    errorcode=SYNTAX_ERROR;
                } else
                {
                    // Store the variables in memory under the OAdC
                    int i=1;
                    String storeVar="smsc/CIMD/"+body[i];
                    XTTProperties.setVariable(storeVar+"/"+"xtttimestamp",""+xtttimestamp);
                    XTTProperties.setVariable(storeVar+"/"+"xttserverport",""+myServerPort);
                    buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                    buffer.append("\n  "+"AdC    = "+body[i]);
                    AdC=body[i];
                    XTTProperties.setVariable(storeVar+"/"+"AdC"    ,body[i++]);
                    buffer.append("\n  "+"OAdC   = "+body[i]);
                    oadc=body[i];
                    XTTProperties.setVariable(storeVar+"/"+"OAdC"   ,body[i++]);
                    buffer.append("\n  "+"AC     = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"AC"     ,body[i++]);
                    buffer.append("\n  "+"NRq    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NRq"    ,body[i++]);
                    buffer.append("\n  "+"NAdC   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NAdC"   ,body[i++]);
                    buffer.append("\n  "+"NT     = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NT"     ,body[i++]);
                    buffer.append("\n  "+"NPID   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NPID"   ,body[i++]);
                    buffer.append("\n  "+"LRq    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"LRq"    ,body[i++]);
                    buffer.append("\n  "+"LRAd   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"LRAd"   ,body[i++]);
                    buffer.append("\n  "+"LPID   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"LPID"   ,body[i++]);
                    buffer.append("\n  "+"DD     = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"DD"     ,body[i++]);
                    buffer.append("\n  "+"DDT    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"DDT"    ,body[i++]);
                    buffer.append("\n  "+"VP     = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"VP"     ,body[i++]);
                    buffer.append("\n  "+"RPID   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"RPID"   ,body[i++]);
                    buffer.append("\n  "+"SCTS   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"SCTS"   ,body[i++]);
                    buffer.append("\n  "+"Dst    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"Dst"    ,body[i++]);
                    buffer.append("\n  "+"Rsn    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"Rsn"    ,body[i++]);
                    buffer.append("\n  "+"DSCTS  = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"DSCTS"  ,body[i++]);

                    buffer.append("\n  "+"MT     = "+body[i]);
                    String MT=body[i];
                    XTTProperties.setVariable(storeVar+"/"+"MT"     ,body[i++]);
                    String msg="";
                    if(body[i-1].equals("2"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setVariable(storeVar+"/"+"NB"  ,body[i++]);
                        buffer.append("\n  "+"NMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*640)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setVariable(storeVar+"/"+"NMsg",body[i++]);
                    } else if(body[i-1].equals("3"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setVariable(storeVar+"/"+"NB"  ,body[i++]);
                        buffer.append("\n  "+"AMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*640)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setVariable(storeVar+"/"+"AMsg",body[i++]);
                    } else if(body[i-1].equals("4"))
                    {
                        buffer.append("\n  "+"NB     = "+body[i]);
                        XTTProperties.setVariable(storeVar+"/"+"NB"  ,body[i++]);
                        buffer.append("\n  "+"TMsg   = "+body[i]);
                        msg=body[i];
                        if(msg.length()>2*160)
                        {
                            errorcode=MESSAGE_TOO_LONG;
                        }
                        XTTProperties.setVariable(storeVar+"/"+"TMsg",body[i++]);
                    }
                    buffer.append("\n  "+"MMS    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"MMS"    ,body[i++]);
                    buffer.append("\n  "+"PR     = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"PR"     ,body[i++]);
                    buffer.append("\n  "+"DCs    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"DCs"    ,body[i++]);
                    buffer.append("\n  "+"MCLs   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"MCLs"   ,body[i++]);
                    buffer.append("\n  "+"RPI    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"RPI"    ,body[i++]);
                    buffer.append("\n  "+"CPg    = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"CPg"    ,body[i++]);
                    buffer.append("\n  "+"RPLy   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"RPLy"   ,body[i++]);
                    buffer.append("\n  "+"OTOA   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"OTOA"   ,body[i++]);
                    buffer.append("\n  "+"HPLMN  = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"HPLMN"  ,body[i++]);
                    buffer.append("\n  "+"XSer   = "+body[i]);
                    String XSer=body[i];
                    int numDD=0;
                    if(XSer.startsWith("01"))
                    {
                        numDD=(Integer.decode("0x"+XSer.substring(2,4))).intValue();
                        buffer.append("\n  "+"         "+"XSer Type of service 01, GSM UDH information - "+numDD+" Octets");
                    }
                    XTTProperties.setVariable(storeVar+"/"+"XSer"   ,body[i++]);
                    buffer.append("\n  "+"RES4   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"RES4"   ,body[i++]);
                    buffer.append("\n  "+"RES5   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"RES5"   ,body[i++]);

                    XTTProperties.printDebug("CIMDWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+storeVar+buffer);

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
                            XTTProperties.printDebug("CIMDWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: all segments received, decoding WSP:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                            WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                            wsp.decode();
                            // Finaly decode the remaining data as mms
                            MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                            mms.decode();
                            wspb=true;
                        } else
                        {
                            XTTProperties.printDebug("CIMDWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): udh: not all segments received, waiting to decode");
                        }
                    }
                    XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE5X"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);
                }

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                // Send the ACK response
                byte[] systemmessage=ConvertLib.getOctetByteArrayFromString(systemname);

                if(errorcode>0)
                {
                    String errorstring=""+errorcode;
                    if(errorcode<10)errorstring="0"+errorcode;
                    switch(errorcode)
                    {
                        case MESSAGE_TOO_LONG:
                            outHead.writePDU("/N/"+errorstring+"/ Message too long/",mywindow);
                            break;
                        case SYNTAX_ERROR:
                            outHead.writePDU("/N/"+errorstring+"/Syntax error/",mywindow);
                            break;
                    }
                } else
                {
                    String msgdate=createDate();
                    switch(pdu_head.command_operation_type)
                    {
                        case SUBMIT_SHORT_MESSAGE:
                            outHead.writePDU("/A/"+"/"+AdC+":"+msgdate+"/",mywindow);
                            if(injectAutoMessages)
                            {
                                byte[] amsg=ConvertLib.createBytes(new String("Message for "+AdC+", with identification "+msgdate+" has been delivered"));
                                XTTProperties.printDebug("CIMDWorker.cmd_type5x("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): created auto message type "+DELIVER_NOTIFICATION);
                                addAutoMessage(new SMSCOriginatedMessage('O',DELIVER_NOTIFICATION,"/"+oadc+"/"+AdC+"/////////////"+msgdate+"/0///3//"+ConvertLib.outputBytes(amsg,0,amsg.length)+"/////////////"));
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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type01(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType01Reply reply=new CmdType01Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                String storeVar="smsc/CIMD/"+body[i];
                XTTProperties.setVariable(storeVar+"/"+"xtttimestamp",""+xtttimestamp);
                XTTProperties.setVariable(storeVar+"/"+"xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setVariable(storeVar+"/"+"AdC"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setVariable(storeVar+"/"+"OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"AC"     ,body[i++]);

                buffer.append("\n  "+"MT     = "+body[i]);
                String MT=body[i];
                XTTProperties.setVariable(storeVar+"/"+"MT"     ,body[i++]);
                String msg="";
                if(body[i-1].equals("2"))
                {
                    buffer.append("\n  "+"NMsg   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NMsg",body[i++]);
                } else if(body[i-1].equals("3"))
                {
                    buffer.append("\n  "+"AMsg   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"AMsg",body[i++]);
                }

                XTTProperties.printDebug("CIMDWorker.cmd_type01("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+storeVar+buffer);
                XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE01"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+AdC+":"+createDate()+"/",mywindow);

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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private void cmd_type02(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType02Reply reply=new CmdType02Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                String storeVar="smsc/CIMD/";

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

                XTTProperties.printDebug("CIMDWorker.cmd_type02("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+buffer);
                XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE02"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                StringBuffer response=new StringBuffer("");
                String date=createDate();

                tmp="";
                for(int j=0;j<numrecipient;j++)
                {
                    response.append(tmp+recipients[j]+":"+date);
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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }

    private void cmd_type03(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType03Reply reply=new CmdType03Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                String storeVar="smsc/CIMD/"+body[i];
                XTTProperties.setVariable(storeVar+"/"+"xtttimestamp",""+xtttimestamp);
                XTTProperties.setVariable(storeVar+"/"+"xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"RAd    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setVariable(storeVar+"/"+"RAd"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setVariable(storeVar+"/"+"OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"AC"     ,body[i++]);
                buffer.append("\n  "+"NPL    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"NPL"    ,body[i++]);
                buffer.append("\n  "+"GA:s   = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"GA:s"   ,body[i++]);
                buffer.append("\n  "+"RP     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"RP"     ,body[i++]);
                buffer.append("\n  "+"PR     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"PR"     ,body[i++]);
                buffer.append("\n  "+"LPR    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"LPR"    ,body[i++]);
                buffer.append("\n  "+"UR     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"UR"     ,body[i++]);
                buffer.append("\n  "+"LUR    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"LUR"    ,body[i++]);
                buffer.append("\n  "+"RC     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"RC"     ,body[i++]);
                buffer.append("\n  "+"LRC    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"LRC"    ,body[i++]);
                buffer.append("\n  "+"DD     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"DD"     ,body[i++]);
                buffer.append("\n  "+"DDT    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"DDT"    ,body[i++]);

                buffer.append("\n  "+"MT     = "+body[i]);
                String MT=body[i];
                XTTProperties.setVariable(storeVar+"/"+"MT"     ,body[i++]);
                String msg="";
                if(body[i-1].equals("2"))
                {
                    buffer.append("\n  "+"NMsg   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"NMsg",body[i++]);
                } else if(body[i-1].equals("3"))
                {
                    buffer.append("\n  "+"AMsg   = "+body[i]);
                    XTTProperties.setVariable(storeVar+"/"+"AMsg",body[i++]);
                }

                XTTProperties.printDebug("CIMDWorker.cmd_type03("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+storeVar+buffer);
                XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE03"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+MT);

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+AdC+":"+createDate()+"/",mywindow);

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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type30(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType30Reply reply=new CmdType30Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                String storeVar="smsc/CIMD/"+body[i];
                XTTProperties.setVariable(storeVar+"/"+"xtttimestamp",""+xtttimestamp);
                XTTProperties.setVariable(storeVar+"/"+"xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setVariable(storeVar+"/"+"AdC"    ,body[i++]);
                buffer.append("\n  "+"OAdC   = "+body[i]);
                String oadc=body[i];
                XTTProperties.setVariable(storeVar+"/"+"OAdC"   ,body[i++]);
                buffer.append("\n  "+"AC     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"AC"     ,body[i++]);
                buffer.append("\n  "+"NRq    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"NRq"    ,body[i++]);
                buffer.append("\n  "+"NAd    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"NAd"    ,body[i++]);
                buffer.append("\n  "+"NPID   = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"NPID"   ,body[i++]);
                buffer.append("\n  "+"DD     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"DD"     ,body[i++]);
                buffer.append("\n  "+"DDT    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"DDT"    ,body[i++]);
                buffer.append("\n  "+"VP     = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"VP"     ,body[i++]);
                buffer.append("\n  "+"AMsg   = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"AMsg"   ,body[i++]);

                XTTProperties.printDebug("CIMDWorker.cmd_type30("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+storeVar+buffer);
                XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE30"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+oadc+XTTProperties.DELIMITER+"");

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
                outHead.command_operation_type = pdu_head.command_operation_type;
                outHead.command_trn            = pdu_head.command_trn;
                outHead.command_operation      = 'R';

                outHead.writePDU("/A/"+"/"+AdC+":"+createDate()+"/",mywindow);

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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }


    private void cmd_type31(PDUHeader pdu_head) throws java.lang.Exception
    {
        PDUBody pdu_body=new PDUBody();
        pdu_body.readPDUBody(pdu_head,pdu_head.getcimdIN());
        if(pdu_body.getError())return;

        CmdType31Reply reply=new CmdType31Reply(pdu_head,pdu_body,pdu_head.getWindow());
        if(windowSize>1)
        {
            Thread t=new Thread(reply,"CIMDWorker-"+getWorkerId());
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
                String storeVar="smsc/CIMD/"+body[i];
                XTTProperties.setVariable(storeVar+"/"+"xtttimestamp",""+xtttimestamp);
                XTTProperties.setVariable(storeVar+"/"+"xttserverport",""+myServerPort);
                buffer.append("\n  "+"XTTSERVERPORT = "+myServerPort);
                buffer.append("\n  "+"AdC    = "+body[i]);
                String AdC=body[i];
                XTTProperties.setVariable(storeVar+"/"+"AdC"    ,body[i++]);
                buffer.append("\n  "+"PID    = "+body[i]);
                XTTProperties.setVariable(storeVar+"/"+"PID"    ,body[i++]);

                XTTProperties.printDebug("CIMDWorker.cmd_type31("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): Received: "+body.length+" fields\n  CIMD:"+pdu_body.getReceived()+"etx\n stored in "+storeVar+buffer);
                XTTProperties.printTransaction("CIMDWORKER/CMD_TYPE30"+XTTProperties.DELIMITER+AdC+XTTProperties.DELIMITER+""+XTTProperties.DELIMITER+"");

                // Create the response
                PDUHeader outHead              = new PDUHeader(pdu_head.getcimdIN(),pdu_head.getcimdOUT());
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
                XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"/"+mywindow+"): exception: "+ex.getMessage());
                XTTProperties.printException(ex);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                try
                {
                    s.close();
                } catch(Exception iox)
                {}
            }
        }
    }

*/
    private String createDate()
    {
        String dateFormat="yyMMddkkmmss";//"yyyy'-'MM'-''T'kk':'mm':'ss'Z'";
        String datestring="";
        SimpleDateFormat format=new SimpleDateFormat(dateFormat,Locale.US);
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
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
        XTTProperties.printDebug("CIMDWorker$injectAutoMessage("+myServerPort+"/"+getWorkerId()+"): currentTime="+currentTime+" sendtime="+message.getSendTime());

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
                //throw new NullPointerException("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): Response body may not be null");
                throw new NullPointerException("CIMDWorker$SMSCOriginatedMessage("+myServerPort+"/"+getWorkerId()+"): Response body may not be null");
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
                XTTProperties.printTransaction("CIMDWORKER/AUTOINJECTMESSAGE"+XTTProperties.DELIMITER+"stx"+response+checksum+"etx"+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER+autoMessages.size());
            }

            //XTTProperties.printDebug("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): writeBody: \n  CIMD: stx"+response+checksum+"etx"
            XTTProperties.printDebug("CIMDWorker$SMSCOriginatedMessage("+myServerPort+"/"+getWorkerId()+"): writeBody: \n  CIMD: stx"+response+checksum+"etx"
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
        }

    }

    private String createResponseParameter(int type, String value)
    {
        return ConvertLib.intToString(type,3)+":"+value+"\t";
        //response.append(ConvertLib.intToString(PARAM_DESTINATION_ADDRESS,3)+":"+it.next()+"\t"+ConvertLib.intToString(PARAM_SERVICE_CENTRE_TIME_STAMP,3)+":"+msgdate+"\t");
    }

    private class PDUBody
    {
        private int checksum   = 0;
        private int command_operation=0;
        //private String storeVar= null;
        HashMap <Integer, Vector<String>> parameters=new HashMap <Integer, Vector<String>>();

        public int getCheckSum()   {return checksum;}
        //public String getStoreVar(){return storeVar;}

        private boolean error = false;
        public boolean getError(){return error;}

        private BufferedInputStream cimdIN  = null;

        public PDUBody()
        {
        }

        public void readPDUBody(PDUHeader inHead, BufferedInputStream in) throws java.lang.Exception
        {
            cimdIN=in;
            error=false;
            this.command_operation=inHead.command_operation;

            if(readtimeout>0&&s!=null)
            {
                s.setSoTimeout(readtimeout);
            }
            int lastByte=-1;
            int currentByte=-1;
            int parameterCode=-1;
            int receivedChecksum=-1;
            StringBuffer parameterData=null;
            StringBuffer debug=new StringBuffer();
            byte[] code=new byte[3];
            Vector<String> parameterDatas=null;
            int parameterCount=0;
            checksum=inHead.getCheckSum();
            try
            {
                while(lastByte!=ETX)
                {
                    parameterData=new StringBuffer();
                    parameterCode=-1;
                    //First byte, could be ETX when there is no Checksum
                    currentByte=in.read();
                    if(currentByte==ETX)
                    {
                        debug.append("  ETX\n");
                        lastByte=currentByte;
                        break;
                    }
                    if(currentByte==-1)break;
                    code[0]=(byte)currentByte;

                    // Second byte
                    currentByte=in.read();
                    if(currentByte==-1)break;
                    code[1]=(byte)currentByte;

                    //third byte could be ETX
                    currentByte=in.read();
                    if(currentByte==ETX)
                    {
                        lastByte=currentByte;
                        receivedChecksum=Integer.decode("0x"+ConvertLib.createString(code,0,2));
                        debug.append("  "+ConvertLib.createString(code,0,2)+"ETX\n");
                        break;
                    }
                    if(currentByte==-1)break;
                    code[2]=(byte)currentByte;

                    checksum=calculateChecksum(checksum,code);
                    parameterCode=ConvertLib.getIntFromStringBytes(code,0,3);

                    // Fourth byte should be a COLON
                    currentByte=in.read();
                    if(currentByte==-1||currentByte!=COLON)break;
                    checksum=calculateChecksum(checksum,currentByte);

                    // Fifth byte and following until TAB is payload
                    while(currentByte!=TAB)
                    {
                        currentByte=in.read();
                        if(currentByte==-1)break;
                        checksum=calculateChecksum(checksum,currentByte);
                        if(currentByte==TAB){break;}
                        parameterData.append((char)currentByte);
                    }
                    parameterDatas=parameters.get(parameterCode);
                    if(parameterDatas==null)
                    {
                        parameterDatas=new Vector<String>();
                    }
                    parameterDatas.add(parameterData.toString());
                    parameters.put(parameterCode,parameterDatas);
                    debug.append("  "+ConvertLib.createString(""+getParameterName(parameterCode),32)+" = "+parameterData+"\n");
                    parameterCount++;
                }
            } catch (java.net.SocketTimeoutException ste)
            {
                XTTProperties.printDebug("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+ste.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ste);
                }
            }
            try
            {
                if(sessionTimeout>0&&s!=null)
                {
                    s.setSoTimeout(sessionTimeout);
                } else if(readtimeout>0&&s!=null)
                {
                    s.setSoTimeout(0);
                }
            } catch(Exception ex){}

            XTTProperties.printDebug("CIMDWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): Received: "+parameterCount+" parameters\n"+debug);
            if(lastByte!=ETX)
            {
                XTTProperties.printVerbose("CIMDWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): ETX not received: 0x"+ConvertLib.intToHex(lastByte));
                error=true;
                // Create the response
                //inHead.command_operation      = 'R';
                //inHead.writePDU("/N/02/etx not received/",inHead.getWindow());
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

            } else if (receivedChecksum!=-1&&receivedChecksum!=(checksum%256))
            {
                XTTProperties.printVerbose("CIMDWorker$PDUBody("+myServerPort+"/"+getWorkerId()+"): CheckSum not valid: calculated "+ConvertLib.intToHex(checksum%256)+"!= received "+ConvertLib.intToHex(receivedChecksum));
                error=true;
                // Create the response
                //inHead.command_operation      = 'R';
                //inHead.writePDU("/N/01/Invalid checksum supplied/",inHead.getWindow());
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

        public Set<Integer> getParameterKeys()
        {
            return parameters.keySet();
        }

        public String getParameter(int parameterCode)
        {
            Vector<String> parameterDatas=parameters.get(parameterCode);
            if(parameterDatas==null)return null;
            return parameterDatas.elementAt(0);
        }
        public Vector<String> getParameters(int parameterCode)
        {
            return parameters.get(parameterCode);
        }

        public String[] createStoreVar(String storeVarHead, int parameterCode)
        {
            Vector<String> parameterDatas=parameters.get(parameterCode);
            String[] storeVar=new String[parameterDatas.size()];
            Iterator<String> it=parameterDatas.iterator();
            int i=0;
            while(it.hasNext())
            {
                storeVar[i++]=storeVarHead+it.next();
            }
            return storeVar;
        }
        public void storeParameters(String[] storeVar)
        {
            for(int x=0;x<storeVar.length;x++)
            {
                XTTProperties.setVariable(storeVar[x]+"/length",""+parameters.keySet().size());
            }
            Iterator<Integer> keys=parameters.keySet().iterator();
            while(keys.hasNext())
            {
                storeParameter(storeVar,keys.next());
            }
        }
        public void storeParameter(String[] storeVar, int parameterCode)
        {
            String parameterName=getParameterName(parameterCode);
            Vector<String> parameterDatas=parameters.get(parameterCode);
            StringBuffer currentDataList=new StringBuffer();
            String currentData=null;
            Iterator<String> it=parameterDatas.iterator();
            String divider=null;
            int i=0;
            while(it.hasNext())
            {
                currentData=it.next();
                /*
                switch(parameterCode)
                {
                    case PARAM_USER_IDENTITY                   :
                    case PARAM_PASSWORD                        :
                    case PARAM_SUBADDR                         :
                    case PARAM_WINDOW_SIZE                     :
                    case PARAM_DESTINATION_ADDRESS             :
                    case PARAM_ORIGINATING_ADDRESS             :
                    case PARAM_ORIGINATING_IMSI                :
                    case PARAM_ALPHANUMERIC_ORIGINATING_ADDRESS:
                    case PARAM_ORIGINATED_VISITED_MSC_ADDRESS  :
                    case PARAM_DATA_CODING_SCHEME              :
                    case PARAM_USER_DATA_HEADER                :
                    case PARAM_USER_DATA                       :
                    case PARAM_USER_DATA_BINARY                :
                    case PARAM_TRANSPORTTYPE                   :
                    case PARAM_MESSAGE_TYPE                    :
                    case PARAM_MORE_MESSAGES_TO_SEND           :
                    case PARAM_OPERATION_TIMER                 :
                    case PARAM_DIALOGUE_ID                     :
                    case PARAM_USSD_PHASE                      :
                    case PARAM_SERVICE_CODE                    :
                    case PARAM_VALIDITY_PERIOD_RELATIVE        :
                    case PARAM_VALIDITY_PERIOD_ABSOLUTE        :
                    case PARAM_PROTOCOL_IDENTIFIER             :
                    case PARAM_FIRST_DELIVERY_TIME_RELATIVE    :
                    case PARAM_FIRST_DELIVERY_TIME_ABSOLUTE    :
                    case PARAM_REPLY_PATH                      :
                    case PARAM_STATUS_CODE                     :
                    case PARAM_STATUS_ERROR_CODE               :
                    case PARAM_DISCHARGE_TIME                  :
                    case PARAM_TARIFF_CLASS                    :
                    case PARAM_SERVICE_DESCRIPTION             :
                    case PARAM_MESSAGE_COUNT                   :
                    case PARAM_PRIORITY                        :
                    case PARAM_DELIVERY_REQUEST_MODE           :
                    case PARAM_SERVICE_CENTER_ADDRESS          :
                    case PARAM_GET_PARAMETER                   :
                    case PARAM_MC_TIME                         :
                    case PARAM_ERROR_CODE                      :
                    case PARAM_ERROR_TEXT                      :
                    default:
                        break;
                }*/
                for(int x=0;x<storeVar.length;x++)
                {
                    XTTProperties.setVariable(storeVar[x]+"/"+parameterName+"/"+i,currentData);
                }
                currentDataList.append(currentData+divider);
                divider=";";
                i++;
            }
            for(int x=0;x<storeVar.length;x++)
            {
                XTTProperties.setVariable(storeVar[x]+"/"+parameterName  ,currentDataList.toString());
                XTTProperties.setVariable(storeVar[x]+"/"+parameterName+"/length",""+i);
            }
        }

    }

    private int calculateChecksum(int startsum, byte[] b)
    {
        return calculateChecksum(startsum, b, 0, b.length);
    }
    private int calculateChecksum(int startsum, byte[] b, int start, int len)
    {
        int checksum=startsum;
        for(int i=0;i<start+len;i++)
        {
            checksum=calculateChecksum(checksum, b[start+i]);
        }
        return checksum;
    }
    private int calculateChecksum(int checksum, int abyte)
    {
        return (checksum+abyte)&0xFF;
    }
    private class PDUHeader
    {
        public int  command_operation      = 0;
        public int  command_trn            = 0;
        private int checksum               = 0;
        private BufferedInputStream cimdIN  = null;
        private BufferedOutputStream cimdOUT= null;
        private byte[] head=null;
        private int thiswindow=-3;
        public int getWindow(){return thiswindow;}

        public byte[] getBytes(){return head;}
        public int getCheckSum(){return checksum;}
        public BufferedInputStream getcimdIN(){return cimdIN;}
        public BufferedOutputStream getcimdOUT(){return cimdOUT;}


        public PDUHeader(BufferedInputStream cimdIN,BufferedOutputStream cimdOUT,int command_operation,int command_trn)
        {
            this.cimdIN = cimdIN;
            this.cimdOUT= cimdOUT;
            this.command_operation      = command_operation;
            this.command_trn            = command_trn;
        }
        public PDUHeader(BufferedInputStream cimdIN,BufferedOutputStream cimdOUT)
        {
            // Set the streams
            this.cimdIN = cimdIN;
            this.cimdOUT= cimdOUT;
        }
        public boolean readPDUHeader() throws Exception
        {
            return readPDUHeader(cimdIN);
        }
        public boolean readPDUHeader(BufferedInputStream in) throws Exception
        {
            int firstByte=-1;
            try
            {
                if(s!=null)s.setSoTimeout(autoMessageRetryTime);
                firstByte=in.read();
            } catch (java.net.SocketTimeoutException stex)
            {
                return false;
            } finally
            {
                if(s!=null)s.setSoTimeout(0);
            }
            if(firstByte!=STX)
            {
                if(firstByte==-1)
                {
                    return true;
                }
                throw new Exception("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): STX not received: 0x"+ConvertLib.intToHex(firstByte));
            }

            head=new byte[HEADLENGTH];
            HTTPHelper.readBytes(in,head);

            XTTProperties.printVerbose("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): Received: 8 bytes");
            command_operation   = ConvertLib.getIntFromStringBytes(head, 0,2);
            command_trn         = ConvertLib.getIntFromStringBytes(head, 3,3);

            checksum=0;
            checksum=calculateChecksum(checksum,STX);
            checksum=calculateChecksum(checksum,head);

            XTTProperties.printDebug("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"): Received: \n  CIMD: 'stx"+new String(head)+"'"
                +"\n  command_operation = "+command_operation
                +"\n  command_trn       = "+command_trn
                +"\n");
            if(command_operation<50)
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
            writePDU(cimdOUT,databody,window);
        }
        public void writePDU(BufferedOutputStream out, String databody, int window) throws Exception
        {
            if(databody==null)
            {
                throw new NullPointerException("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): Response body may not be null");
            }
            if(responseDelay>0)
            {
                Thread.sleep(responseDelay);
            }
            String response=ConvertLib.intToString(command_operation,2)
                            +":"+ConvertLib.intToString(command_trn,3)+"\t"
                            +databody;
            byte[] body=ConvertLib.getOctetByteArrayFromString(response);
            int checksumR=0;
            checksumR=calculateChecksum(checksumR,STX);
            checksumR=calculateChecksum(checksumR,body);
            String checksumS=ConvertLib.intToHex(checksumR,2);

            synchronized(out)
            {
                out.write(STX);
                out.write(body);
                out.write(ConvertLib.getOctetByteArrayFromString(checksumS));
                out.write(ETX);
                out.flush();
            }

            XTTProperties.printDebug("CIMDWorker$PDUHeader("+myServerPort+"/"+getWorkerId()+"/"+window+"): writeBody: \n  CIMD: stx"+response+checksum+"etx"
                +"\n  command_operation = "+command_operation
                +"\n  command_trn       = "+command_trn
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
                XTTProperties.printFail("CIMDWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(connected.isEmpty())
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for any CIMD Bind on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("CIMDWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for any CIMD Bind on SMSC no timeout");
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
                XTTProperties.printFail("CIMDWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connected.contains(oadc))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for CIMD Bind with OAdC='"+oadc+"' on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(!connected.contains(oadc))
                    {
                        XTTProperties.printFail("CIMDWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    } else
                    {
                        XTTProperties.printDebug("waitForBind: connection with OAdC='"+oadc+"' found!");
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for CIMD Bind with OAdC='"+oadc+"' on SMSC no timeout");
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
                XTTProperties.printFail("CIMDWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connectedIp.contains(remoteAddress.toString()))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for CIMD Bind with remoteAddress='"+remoteAddress+"' on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(!connectedIp.contains(remoteAddress.toString()))
                    {
                        XTTProperties.printFail("CIMDWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    } else
                    {
                        XTTProperties.printDebug("waitForBind: CIMD connection with remoteAddress='"+remoteAddress+"' found!");
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for CIMD Bind with remoteAddress='"+remoteAddress+"' on SMSC no timeout");
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
                XTTProperties.printFail("CIMDWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connection)
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForMessage: waiting for CIMD Message with OAdC='"+oadc+"' on SMSC timeout:"+wait);
                    msgkey.wait(wait);
                } else
                {
                    XTTProperties.printInfo("waitForMessage: waiting for CIMD Message with OAdC='"+oadc+"' on SMSC no timeout");
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
            XTTProperties.printFail("CIMDWorker.waitForMessages: no instance running!");
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
                        XTTProperties.printFail("CIMDWorker.waitForMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                if(wait>0)
                {
                    XTTProperties.printInfo("CIMDWorker.waitForMessages: "+msgcount+"/"+number+" timeout="+wait);
                    prevcount=msgcount;
                    msgkey.wait(wait);
                    if(msgcount==prevcount)
                    {
                        XTTProperties.printFail("CIMDWorker.waitForMessages: "+msgcount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("CIMDWorker.waitForMessages: "+msgcount+"/"+number);
                    msgkey.wait();
                }
            }
            XTTProperties.printInfo("CIMDWorker.waitForMessages: "+msgcount+"/"+number);
        }
    }

    public static void waitForTimeoutMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("CIMDWorker.waitForTimeoutMessages: no instance running!");
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
                        XTTProperties.printFail("CIMDWorker.waitForTimeoutMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                XTTProperties.printInfo("CIMDWorker.waitForTimeoutMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=msgcount;
                msgkey.wait(wait);
                if(msgcount==prevcount)
                {
                    XTTProperties.printInfo("CIMDWorker.waitForTimeoutWSPMessages: timed out with no messages!");
                    return;
                }
            }
            XTTProperties.printFail("CIMDWorker.waitForTimeoutWSPMessages: Message received! "+wspcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    
    /**
     * Waits for connection to bind with server socket
     * @param number - time to wait
     * @throws java.lang.InterruptedException
     */
    public static void waitForBinds(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("CIMDWorker.waitForBinds: no instance running!");
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
                    XTTProperties.printInfo("CIMDWorker.waitForBinds: "+concount+"/"+number+" timeout="+wait);
                    prevcount=concount;
                    conkey.wait(wait);
                    if(concount==prevcount)
                    {
                        XTTProperties.printFail("CIMDWorker.waitForBinds: "+concount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("CIMDWorker.waitForBinds: "+concount+"/"+number);
                    conkey.wait();
                }
            }
            XTTProperties.printInfo("CIMDWorker.waitForBinds: "+concount+"/"+number);
        }
    }
    /**
     * Wait for a number of wsp messages
     */
    public static void waitForWSPMessages(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("CIMDWorker.waitForWSPMessages: no instance running!");
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
                        XTTProperties.printFail("CIMDWorker.waitForWSPMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                if(wait>0)
                {
                    XTTProperties.printInfo("CIMDWorker.waitForWSPMessages: "+wspcount+"/"+number+" timeout="+wait);
                    prevcount=wspcount;
                    wspkey.wait(wait);
                    if(wspcount==prevcount)
                    {
                        XTTProperties.printFail("CIMDWorker.waitForWSPMessages: "+wspcount+"/"+number+" timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("CIMDWorker.waitForWSPMessages: "+wspcount+"/"+number);
                    wspkey.wait();
                }
            }
            XTTProperties.printInfo("CIMDWorker.waitForWSPMessages: "+wspcount+"/"+number);
        }
    }

    /**
     * Wait for a number of wsp messages
     */
    public static void waitForTimeoutWSPMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("CIMDWorker.waitForTimeoutWSPMessages: no instance running!");
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
                        XTTProperties.printFail("CIMDWorker.waitForTimeoutWSPMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                XTTProperties.printInfo("CIMDWorker.waitForTimeoutWSPMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=wspcount;
                wspkey.wait(wait);
                if(wspcount==prevcount)
                {
                    XTTProperties.printInfo("CIMDWorker.waitForTimeoutWSPMessages: timed out with no WSP messages!");
                    return;
                }
            }
            XTTProperties.printFail("CIMDWorker.waitForTimeoutWSPMessages: WSP message received! "+wspcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    /**
     * Set the password
     * @param pw
     */
    public static void setPassword(String pw)
    {
        synchronized(passwordKey)
        {
            password=new String(pw);
        }
    }
    
    /**
     * Retrieve the password
     * @return It returns password as string
     */
    public static String getPassword()
    {
        synchronized(passwordKey)
        {
            return new String(password);
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
        synchronized(conkey)
        {
            connected=new Vector<String>();
            connectedIp=new Vector<String>();
            concount=0;
        }
        // Clear the segment store
        udhSegmentStore.clear();
        nextMessageDelay=0;
    }
    
    /**
     * Method to read the response
     * @param functionname
     * @param in - data received in response
     * @return It returns the byte array
     * @throws Exception
     */
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

            XTTProperties.printDebug(functionname+": Received: \n  CIMD: stx"+new String(head)+""
                +"\n  command_trn            = "+command_trn
                +"\n  command_length         = "+command_length
                +"\n  command_operation      = "+command_operation
                +"\n  command_operation_type = "+command_operation_type
                +"\n");
        } else
        {
            head=pdu_head.getBytes();
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
            XTTProperties.printDebug(functionname+": Received: "+bbody.length+" bytes\n  CIMD:"+bbodyString);
            XTTProperties.printFail(functionname + ": Error: ETX not received: 0x"+ConvertLib.intToHex(lastByte));
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return response;
        } else if (!ConvertLib.intToHex(rchecksum,2).equals(body[body.length-1]))
        {
            XTTProperties.printDebug(functionname+": Received: "+bbody.length+" bytes\n  CIMD:"+bbodyString+"etx");
            XTTProperties.printFail(functionname + ": Error: CheckSum not valid: calculated "+ConvertLib.intToHex(rchecksum,2)+"!= received "+body[body.length-1]);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return response;
        } else
        {
            XTTProperties.printDebug(functionname+"  : Received: "+bbody.length+" bytes\n  CIMD:"+bbodyString+"etx");
        }
        return response;
    }

    /**
     * Method to read CIMD message
     * @param responseDelay - time to wait before response
     * @param cimdIN - input data
     * @param cimdOUT 
     */
    public static void awaitCIMDMessage(int responseDelay, BufferedInputStream cimdIN,BufferedOutputStream cimdOUT)
    {

        CIMDWorker worker=new CIMDWorker(-1,null,null,0,1,responseDelay, 0, 0,false,0,0,500);
        boolean ok=worker.awaitCIMDMessage(cimdIN, cimdOUT);
    }
    private boolean awaitCIMDMessage(BufferedInputStream cimdIN,BufferedOutputStream cimdOUT)
    {

        try
        {
            PDUHeader pdu_head=new PDUHeader(cimdIN, cimdOUT);
            // Try reading the first 13 Bytes fo the header which are always the same
            // This method finishes on a disconnect or close of the socket
            // and of course on receiving the 13 bytes
            pdu_head.readPDUHeader();
            return this.switchCommand(pdu_head);
        } catch (Exception e)
        {
            XTTProperties.printFail("CIMDWorker("+myServerPort+"/"+getWorkerId()+"): exception: "+e.getMessage());
            XTTProperties.printException(e);
        }
        return false;
    }



    public static BufferedOutputStream getOutputStream(InetAddress ip,int port)
    {
        //System.out.println(ip+":"+port+"\n"+workerIDIPMap);
        return getOutputStream(workerIDIPMap.get(ip+":"+port));
    }
    
    /**
     * Handles the output data
     * @param workerId
     * @return It returns BefferedOutputStream
     */
    public static BufferedOutputStream getOutputStream(String workerId)
    {
        //System.out.println(workerId+"\n"+workerMap);
        CIMDWorker worker=(CIMDWorker)workerMap.get(workerId.trim());
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
        return gcimdOUT;
    }

    public static byte[] getResponse(InetAddress ip,int port)
    {
        return getResponse(workerIDIPMap.get(ip+":"+port));
    }
    public static byte[] getResponse(String workerId)
    {
        CIMDWorker worker=(CIMDWorker)workerMap.get(workerId);
        if(worker==null)
        {
            return new byte[0];
        } else
        {
            return worker.getResponse();
        }
    }
    
    /**
     * Handles the response
     * @return It returns the byte array
     */
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
                        XTTProperties.printDebug("CIMDWorker.getResponse(): waiting for any CIMD Message on SMSC timeout:"+wait);
                        responseKey.wait(wait);
                    } else
                    {
                        XTTProperties.printDebug("CIMDWorker.getResponse(): waiting for any CIMD Message on SMSC no timeout");
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

    public static final String tantau_sccsid = "@(#)$Id: CIMDWorker.java,v 1.6 2010/03/25 10:18:30 rajesh Exp $";
}
