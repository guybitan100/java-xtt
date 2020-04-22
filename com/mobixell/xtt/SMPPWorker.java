package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Random;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.GregorianCalendar;

/**
 * SMPPWorker. Processes a connection which has been received by the SMSCServer
 * <br>
 * <br>Data is stored:
 * <br>submit_sm: smsc/smpp/[received_destination_addr], smsc/[myServerPort]/smpp/[received_destination_addr]
 * <br>cancel_sm: smsc/smpp/[received_destination_addr], smsc/[myServerPort]/smpp/[received_destination_addr]
 * <br>query_broadcast_sm: smsc/smpp, smsc/[myServerPort]/smpp
 * <br>cancel_broadcast_sm: smsc/smpp, smsc/[myServerPort]/smpp
 * <br>alert_notification: smsc/smpp/[received_esme_addr], smsc/[myServerPort]/smpp/[received_esme_addr]
 * <br>deliver_sm: smsc/smpp/[received_destination_addr], smsc/[myServerPort]/smpp/[received_destination_addr]
 * <br>submit_multi: smsc/smpp/number_of_dests, smsc/[myServerPort]/smpp/number_of_dests
 * <br>      smsc/smpp/[received_destination_addr[i]], smsc/[myServerPort]/smpp/[received_destination_addr[i]]
 * <br>      smsc/smpp/[received_dl_name[i]], smsc/[myServerPort]/smpp/[received_dl_name[i]]
 * <br>broadcast_sm: smsc/smpp, smsc/[myServerPort]/smpp
 * <br>data_sm: smsc/smpp/[received_destination_addr], smsc/[myServerPort]/smpp/[received_destination_addr]
 * <br>query_sm: smsc/smpp/[received_message_id], smsc/[myServerPort]/smpp/[received_message_id]
 * <br>replace_sm: smsc/smpp/[received_message_id], smsc/[myServerPort]/smpp/[received_message_id]
 * <br>bind_receiver: smsc/smpp/[received_system_id], smsc/[myServerPort]/smpp/[received_system_id]
 * <br>bind_transceiver: smsc/smpp/[received_system_id], smsc/[myServerPort]/smpp/[received_system_id]
 * <br>bind_transmitter: smsc/smpp/[received_system_id], smsc/[myServerPort]/smpp/[received_system_id]
 * <br>outbind: smsc/smpp/[received_system_id], smsc/[myServerPort]/smpp/[received_system_id]
 * <br>
 * <br>responses: smsc/smpp, smsc/[myServerPort]/smpp
 *
 * @author Roger Soder
 * @version $Id: SMPPWorker.java,v 1.28 2011/01/28 06:05:10 rajesh Exp $
 */
public class SMPPWorker extends Thread implements SMPPConstants, SMSCWorker
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
    private BufferedOutputStream smppOUT=null;
    private BufferedInputStream smppIN=null;

    // request count
    private int reqCount=0;
    private int maxRequestCount = -1;

    // System ID that is allowed to bind, empty if any
    private static String systemid="";
    // Password used to bind to the smsc
    private static String password="";
    // Key for the password
    private static Object passwordKey=new Object();
    // System name the SMSC answers with
    private static String systemname="MMT35SMPP";

    private static Map<String,SMPPWorker> workerMap= Collections.synchronizedMap(new HashMap<String,SMPPWorker>());

    private static Map<String,ByteArrayWrapper> overrideResponseMap= Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());
    private static Map<String,ByteArrayWrapper> tlvMap= Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());
    private static Map<String,Integer> cmdStatusOverride= Collections.synchronizedMap(new HashMap<String,Integer>());

    // null if not connected, else the systemid of the connection
    private static Vector<String> connected=new Vector<String>();
    private static Vector<String> connectedIp=new Vector<String>();
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

    // SMPP Message id the worker answers with
    private static int message_id=0;

    // Stores previous UDH Segments for connecting them togehter
    private static UDHSegmentStore udhSegmentStore=new UDHSegmentStore();

    private SMSCServer myServer=null;
    private int myServerPort=0;

    private static int nextMessageDelay=0;public static void setNextMessageDelay(int delay){nextMessageDelay=delay;}

    // Auto message stuff
    private boolean injectAutoMessages=true;
    private int autoMessagesCommandSequence=0;
    private int earliestAutoMessageSendTime=0;
    private int latestAutoMessageSendTime=0;
    private int autoMessageRetryTime=500;
    private static Random random=new Random();
    private SortedSet<SMSCOriginatedMessage> autoMessages = Collections.synchronizedSortedSet(new TreeSet<SMSCOriginatedMessage>());

    /* Socket to client we're handling, which will be set by the SMSCServer
       when dispatching the request to us */
    private Socket socket = null;


    /**
     * Creates a new SMPPWorker
     * @param id            ID number of this worker thread
     * @param myServer      Socket of the connection
     */
    SMPPWorker(int id,Socket socket,SMSCServer myServer, int maxRequestCount)
    {
        this(id,socket,myServer, maxRequestCount,false, 0, 0, 0);
    }
    SMPPWorker(int id,Socket socket,SMSCServer myServer, int maxRequestCount
              ,boolean injectAutoMessages, int earliestAutoMessageSendTime, int latestAutoMessageSendTime, int autoMessageRetryTime)
    {
        super("SMPPWorker-"+id);
        this.socket = socket;
        this.id = id;
        this.maxRequestCount = maxRequestCount;
        this.injectAutoMessages=injectAutoMessages;
        this.earliestAutoMessageSendTime=earliestAutoMessageSendTime;
        this.latestAutoMessageSendTime=latestAutoMessageSendTime;
        this.autoMessageRetryTime=autoMessageRetryTime;
        this.myServer=myServer;
        if(myServer!=null)
        {
            myServerPort=myServer.getPort();
            myServer.addWorker();
            workerMap.put(id+"",this);
        } else
        {
            workerMap.put("c"+id,this);
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
            XTTProperties.printDebug("SMPPWorker: stop request for id: "+myServerPort+"/"+getWorkerId()+" -> closing socket");
            socket.close();
        } catch(Exception e)
        {}
        synchronized(this)
        {
            notifyAll();
        }
            XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): setStop() finished");
}

    /**
     * Start the worker thread
     */
    public synchronized void run()
    {
        handleClient();
    }

    /**
     * Handles the SMPP request
     * @throws IOException
     */
    private void handleClient()
    {
        // Increase the current count of running workers
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("SMPPWorker: New Client handled by " +id+" instance "+instances);
            key.notify();
        }

        try
        {
            // Set the streams
            socket.setTcpNoDelay(true);
            smppIN = new BufferedInputStream(socket.getInputStream());
            smppOUT = new BufferedOutputStream(socket.getOutputStream());

            // Say hello to the world
            XTTProperties.printDebug("SMPPWorker: Client connected: "+socket.getRemoteSocketAddress()+"\n");
            // Create a new header object
            SMPP_PDUHeader pdu_head=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
            if(injectAutoMessages)
            {
                pdu_head.setAutoMessageRetryTime(autoMessageRetryTime);
            }
            // As long as it takes
            boolean doWhile=true;
            // do the loop
            while(doWhile&&socket.isConnected()&&!stop)
            {
                // Try reading the first 16 Bytes fo the header which are always the same
                // This method finishes on a disconnect or close of the socket
                // and of course on receiving the 16 bytes
                do
                {
                    // Inject automessages
                    if(injectAutoMessages)injectAutoMessage(smppOUT);
                }while(!pdu_head.readPDUHeader(smppIN,socket));
                
                if(maxRequestCount==0&&reqCount>maxRequestCount)
                {
                    return;
                    //socket.close();
                }
                String function=null;
                // Decide what to do on the received header command
                switch(pdu_head.command_id)
                {
                    // Connection requested
                    case BIND_RECEIVER:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): BIND_RECEIVER");
                        pdu_bind_receiver(pdu_head);
                        break;
                    // Connection requested
                    case BIND_TRANSCEIVER:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): BIND_TRANSCEIVER");
                        pdu_bind_transceiver(pdu_head);
                        break;
                    // Connection requested
                    case BIND_TRANSMITTER:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): BIND_TRANSMITTER");
                        pdu_bind_transmitter(pdu_head);
                        break;
                    // Disconnect requested
                    case UNBIND:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): UNBIND");
                        pdu_unbind(pdu_head);
                        break;
                    // Send an sms
                    case SUBMIT_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): SUBMIT_SM");
                        pdu_submit_sm(pdu_head);
                        break;
                    // Send an sms
                    case DELIVER_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): DELIVER_SM");
                        pdu_deliver_sm(pdu_head);
                        break;
                    // Send an sms
                    case QUERY_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): QUERY_SM");
                        pdu_query_sm(pdu_head);
                        break;
                    // Send an sms
                    //*
                    case CANCEL_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): CANCEL_SM");
                        pdu_cancel_sm(pdu_head);
                        break;
                        //*/
                    // Send an sms
                    case REPLACE_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): REPLACE_SM");
                        pdu_replace_sm(pdu_head);
                        break;
                    // Keep-Alive
                    case ENQUIRE_LINK:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): ENQUIRE_LINK");
                        pdu_enquire_link(pdu_head);
                        break;
                    // Send multiple sms
                    case SUBMIT_MULTI:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): SUBMIT_MULTI");
                        pdu_submit_multi(pdu_head);
                        break;
                    // Send a data sms
                    case DATA_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): DATA_SM");
                        pdu_data_sm(pdu_head);
                        break;
                    // SMPP5
                    case OUTBIND:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): OUTBIND");
                        pdu_outbind(pdu_head);
                        break;
                    case ALERT_NOTIFICATION:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): ALERT_NOTIFICATION");
                        pdu_alert_notification(pdu_head);
                        break;
                    case BROADCAST_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): BROADCAST_SM");
                        pdu_broadcast_sm(pdu_head);
                        break;
                    case QUERY_BROADCAST_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): QUERY_BROADCAST_SM");
                        pdu_query_broadcast_sm(pdu_head);
                        break;
                    case CANCEL_BROADCAST_SM:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): CANCEL_BROADCAST_SM");
                        pdu_cancel_broadcast_sm(pdu_head);
                        break;
                    // Handle the responses
                    case BIND_RECEIVER_RESP      :
                        if(function==null)function="BIND_RECEIVER_RESP";
                    case BIND_TRANSMITTER_RESP   :
                        if(function==null)function="BIND_TRANSMITTER_RESP";
                    case BIND_TRANSCEIVER_RESP   :
                        if(function==null)function="BIND_TRANSCEIVER_RESP";
                    case UNBIND_RESP             :
                        if(function==null)function="UNBIND_RESP";
                    case SUBMIT_SM_RESP          :
                        if(function==null)function="SUBMIT_SM_RESP";
                    case DELIVER_SM_RESP         :
                        if(function==null)function="DELIVER_SM_RESP";
                    case QUERY_SM_RESP           :
                        if(function==null)function="QUERY_SM_RESP";
                    case CANCEL_SM_RESP          :
                        if(function==null)function="CANCEL_SM_RESP";
                    case REPLACE_SM_RESP         :
                        if(function==null)function="REPLACE_SM_RESP";
                    case ENQUIRE_LINK_RESP       :
                        if(function==null)function="ENQUIRE_LINK_RESP";
                    case SUBMIT_MULTI_RESP       :
                        if(function==null)function="SUBMIT_MULTI_RESP";
                    case DATA_SM_RESP            :
                        if(function==null)function="DATA_SM_RESP";
                    case GENERIC_NAK             :
                        if(function==null)function="GENERIC_NAK";
                    case BROADCAST_SM_RESP       :
                        if(function==null)function="BROADCAST_SM_RESP";
                    case QUERY_BROADCAST_SM_RESP :
                        if(function==null)function="QUERY_BROADCAST_SM_RESP";
                    case CANCEL_BROADCAST_SM_RESP:
                        if(function==null)function="CANCEL_BROADCAST_SM_RESP";
                        byte[] response=readResponse("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): "+function,smppIN,new String[]{"smsc/smpp","smsc/"+myServerPort+"/smpp"},pdu_head);
                        break;
                    // This is happening when we have a disconnect on the connection
                    // because 0 is not an allowed command id
                    case 0:
                        XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): pdu empty, possible disconnect");
                        doWhile=false;
                        break;
                    // A command was received that we currently don't support
                    default:
                        XTTProperties.printFail("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): unsupported pdu command id: "+pdu_head.command_id+" from "+socket.getRemoteSocketAddress());
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        doWhile=false;
                        break;
                }
                reqCount++;
                if(maxRequestCount>0&&reqCount>maxRequestCount)
                {
                    return;
                    //socket.close();
                }
                if(nextMessageDelay>0)
                {
                    Thread.sleep(nextMessageDelay);
                }
            }
        // Something was wrong with the socket, perhaps it was closed on a set Stop
        } catch (java.net.SocketException se)
        {
            XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): java.net.SocketException: "+se.getMessage());
            //XTTProperties.printDebugException(se);
        // Everything else goes here which should not happen
        } catch (Exception e)
        {
            XTTProperties.printException(e);
        } finally
        {
            // The connection is definitively gone now
            synchronized(conkey)
            {
                connected.remove(myConnection);
                connectedIp.remove(myConnectionIp);
                myConnection=null;
                myConnectionIp=null;
                conkey.notifyAll();
            }
            /*
            synchronized(wspkey)
            {
                wspcount++;
                wspkey.notifyAll();
            }
            synchronized(msgkey)
            {
                msgcount++;
                msgkey.notifyAll();
            }
            */
            // Make sure the socket is closed
            try
            {
                socket.close();
            } catch (java.io.IOException ioex){}
            if(myServer!=null)myServer.removeWorker(this);
            XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): Client disconnected");
            // Decrease the number of running instances
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            if(myServer!=null)
            {
                workerMap.remove(id+"");
            } else
            {
                workerMap.remove("c"+id);
            }
        }
    }

    /**
     * Handles received SMPP PDU header
     * @param inHead
     * @throws Exception
     */
    private void pdu_submit_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        int    received_dest_addr_ton           =inBody.getIntFromByteArray(1);
        int    received_dest_addr_npi           =inBody.getIntFromByteArray(1);
        String received_destination_addr        =inBody.getStringFromCOctetByteArray(21);
        int    received_esm_class               =inBody.getIntFromByteArray(1);
        int    received_protocol_id             =inBody.getIntFromByteArray(1);
        int    received_priority_flag           =inBody.getIntFromByteArray(1);
        String received_schedule_delivery_time  =inBody.getStringFromCOctetByteArray(17);
        String received_validity_period         =inBody.getStringFromCOctetByteArray(17);
        int    received_registered_delivery     =inBody.getIntFromByteArray(1);
        int    received_replace_if_present_flag =inBody.getIntFromByteArray(1);
        int    received_data_coding             =inBody.getIntFromByteArray(1);
        int    received_sm_default_msg_id       =inBody.getIntFromByteArray(1);
        int    received_sm_length               =inBody.getIntFromByteArray(1);
        byte[] received_short_message_bytes     =inBody.getByteArrayFromOctetByteArray(received_sm_length);
        String received_short_message           =ConvertLib.createString(received_short_message_bytes);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  dest_addr_ton           ='"+received_dest_addr_ton+"'"
                +"\n  dest_addr_npi           ='"+received_dest_addr_npi+"'"
                +"\n  destination_addr        ='"+received_destination_addr+"'"
                +"\n  esm_class               ='"+received_esm_class+"'"
                +"\n  protocol_id             ='"+received_protocol_id+"'"
                +"\n  priority_flag           ='"+received_priority_flag+"'"
                +"\n  schedule_delivery_time  ='"+received_schedule_delivery_time+"'"
                +"\n  validity_period         ='"+received_validity_period+"'"
                +"\n  registered_delivery     ='"+received_registered_delivery+"'"
                +"\n  replace_if_present_flag ='"+received_replace_if_present_flag+"'"
                +"\n  data_coding             ='"+received_data_coding+"'"
                +"\n  sm_default_msg_id       ='"+received_sm_default_msg_id+"'"
                +"\n  sm_length               ='"+received_sm_length+"'"
                +"\n  short_message           ='"+received_short_message+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        String[] storeVar={"smsc/smpp/"+received_destination_addr,"smsc/"+myServerPort+"/smpp/"+received_destination_addr};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"submit_sm"                    );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_ton"      ,""+received_dest_addr_ton         );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_npi"      ,""+received_dest_addr_npi         );
            XTTProperties.setVariable(storeVar[i]+"/"+"destination_addr"   ,""+received_destination_addr      );
            XTTProperties.setVariable(storeVar[i]+"/"+"esm_class"          ,""+received_esm_class             );
            XTTProperties.setVariable(storeVar[i]+"/"+"protocol_id"        ,""+received_protocol_id           );
            XTTProperties.setVariable(storeVar[i]+"/"+"priority_flag"      ,""+received_priority_flag         );
            XTTProperties.setVariable(storeVar[i]+"/"+"schedule_delivery_time",""+received_schedule_delivery_time);
            XTTProperties.setVariable(storeVar[i]+"/"+"validity_period"    ,""+received_validity_period       );
            XTTProperties.setVariable(storeVar[i]+"/"+"registered_delivery",""+received_registered_delivery   );
            XTTProperties.setVariable(storeVar[i]+"/"+"received_replace_if_present_flag",""+received_replace_if_present_flag);
            XTTProperties.setVariable(storeVar[i]+"/"+"data_coding"        ,""+received_data_coding           );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_default_msg_id"  ,""+received_sm_default_msg_id     );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_length"          ,""+received_sm_length             );
            XTTProperties.setVariable(storeVar[i]+"/"+"short_message"      ,""+ConvertLib.base64Encode(received_short_message));
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        if(received_service_type.equalsIgnoreCase("WAP"))
        {
            UDHDecoder udh=new UDHDecoder(received_short_message_bytes,0,storeVar,udhSegmentStore);
            udh.decode();
            if(udh.hasAllSegments())
            {
                // Decode the complete data as WSP protocoll
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): udh: all segments received, decoding:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                wsp.decode();
                // Finaly decode the remaining data as mms
                /*
                MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                mms.decode();
                */
                wspb=true;
            } else
            {
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): udh: not all segments received, waiting to decode");
            }
        }
        XTTProperties.printTransaction("SMPPWORKER/PDU_SUBMIT_SM"+XTTProperties.DELIMITER+received_destination_addr+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =SUBMIT_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;
        Integer cmdStatusOverridei=cmdStatusOverride.get(SUBMIT_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }
        synchronized(omsgkey)
        {
            if((overrideMsgCount>=0&&omsgcount>=overrideMsgCount)||ConvertLib.checkPattern(received_destination_addr,overridePattern))
            {
                outHead.command_status=overrideReturnCode;
            }
            omsgcount++;
            omsgkey.notifyAll();
        }

        ByteArrayWrapper tlvs=tlvMap.get(SUBMIT_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }
        String msgId=createMessageId();
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(SUBMIT_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {

                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(msgId),tlvb);
                if(injectAutoMessages)
                {
                    XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): created auto message type "+DELIVER_SM);
                    addAutoMessage(new SMSCOriginatedMessage(DELIVER_SM,
                        createDeliverSM(received_dest_addr_ton,received_dest_addr_npi,received_destination_addr,
                                        received_source_addr_ton,received_source_addr_npi,received_source_addr,
                                        received_protocol_id,msgId)
                                                            )
                                  );
                }
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Handle a received sms
     */
    private void pdu_cancel_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        int    received_dest_addr_ton           =inBody.getIntFromByteArray(1);
        int    received_dest_addr_npi           =inBody.getIntFromByteArray(1);
        String received_destination_addr        =inBody.getStringFromCOctetByteArray(21);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_cancel_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  dest_addr_ton           ='"+received_dest_addr_ton+"'"
                +"\n  dest_addr_npi           ='"+received_dest_addr_npi+"'"
                +"\n  destination_addr        ='"+received_destination_addr+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        String[] storeVar={"smsc/smpp/"+received_destination_addr,"smsc/"+myServerPort+"/smpp/"+received_destination_addr};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"cancel_sm"                    );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_ton"      ,""+received_dest_addr_ton         );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_npi"      ,""+received_dest_addr_npi         );
            XTTProperties.setVariable(storeVar[i]+"/"+"destination_addr"   ,""+received_destination_addr      );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_CANCEL_SM"+XTTProperties.DELIMITER+received_destination_addr+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =CANCEL_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;
        Integer cmdStatusOverridei=cmdStatusOverride.get(CANCEL_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(CANCEL_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }
        byte[] response=new byte[0];
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(CANCEL_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_cancel_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }


    /**
     * Handle a received sms
     */
    private void pdu_query_broadcast_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_query_broadcast_sm(): translates to: \n"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/";
        String[] storeVar={"smsc/smpp","smsc/"+myServerPort+"/smpp"};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"query_broadcast_sm"           );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_QUERY_BROADCAST_SM"+XTTProperties.DELIMITER+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =QUERY_BROADCAST_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(QUERY_BROADCAST_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(QUERY_BROADCAST_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        // Send the response
        byte[] messageID=ConvertLib.getCOctetByteArrayFromString(msgId);
        byte[] response=new byte[messageID.length+14];
        byte[] message_state=ConvertLib.getByteArrayFromInt(OPT_MESSAGE_STATE,2);
        byte[] broadcast_area_identifier=ConvertLib.getByteArrayFromInt(OPT_BROADCAST_AREA_IDENTIFIER,2);
        byte[] broadcast_area_success=ConvertLib.getByteArrayFromInt(OPT_BROADCAST_AREA_SUCCESS,2);
        int pointer=0;
        for(int i=0;i<messageID.length;i++)
        {
            response[pointer++]=messageID[i];
        }
        response[pointer++]=message_state[0];
        response[pointer++]=message_state[1];
        response[pointer++]=0x00;
        response[pointer++]=0x01;
        response[pointer++]=ENROUTE;
        response[pointer++]=broadcast_area_identifier[0];
        response[pointer++]=broadcast_area_identifier[1];
        response[pointer++]=0x00;
        response[pointer++]=0x00;
        response[pointer++]=broadcast_area_success[0];
        response[pointer++]=broadcast_area_success[1];
        response[pointer++]=0x00;
        response[pointer++]=0x01;
        response[pointer++]=100;


        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(QUERY_BROADCAST_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_query_broadcast_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }


    /**
     * Handle a received sms
     */
    private void pdu_cancel_broadcast_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_cancel_broadcast_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/";
        String[] storeVar={"smsc/smpp","smsc/"+myServerPort+"/smpp"};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"broadcast_sm"                 );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_CANCEL_BROADCAST_SM"+XTTProperties.DELIMITER+XTTProperties.DELIMITER+XTTProperties.DELIMITER+received_service_type);

        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =CANCEL_BROADCAST_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(CANCEL_BROADCAST_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(CANCEL_BROADCAST_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        byte[] response=new byte[0];
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(CANCEL_BROADCAST_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_cancel_broadcast_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Handle a received sms
     */
    private void pdu_alert_notification(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(65);
        int    received_esme_addr_ton           =inBody.getIntFromByteArray(1);
        int    received_esme_addr_npi           =inBody.getIntFromByteArray(1);
        String received_esme_addr               =inBody.getStringFromCOctetByteArray(65);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_alert_notification(): translates to: \n"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  esme_addr_ton           ='"+received_esme_addr_ton+"'"
                +"\n  esme_addr_npi           ='"+received_esme_addr_npi+"'"
                +"\n  esme_addr               ='"+received_esme_addr+"'"
                +"\n");

        // Store the variables in memmory under the esme address
        //String storeVar="smsc/smpp/"+received_esme_addr;
        String[] storeVar={"smsc/smpp/"+received_esme_addr,"smsc/"+myServerPort+"/smpp/"+received_esme_addr};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"alert_notification"           );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"esme_addr_ton"      ,""+received_esme_addr_ton         );
            XTTProperties.setVariable(storeVar[i]+"/"+"esme_addr_npi"      ,""+received_esme_addr_npi         );
            XTTProperties.setVariable(storeVar[i]+"/"+"esme_addr"          ,""+received_esme_addr             );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_ALERT_NOTIFICATION"+XTTProperties.DELIMITER+received_esme_addr+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER);

        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =SUBMIT_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(ALERT_NOTIFICATION+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(ALERT_NOTIFICATION+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        byte[] response=new byte[0];
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(ALERT_NOTIFICATION+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_alert_notification(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Handle a received sms
     */
    private void pdu_deliver_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        int    received_dest_addr_ton           =inBody.getIntFromByteArray(1);
        int    received_dest_addr_npi           =inBody.getIntFromByteArray(1);
        String received_destination_addr        =inBody.getStringFromCOctetByteArray(21);
        int    received_esm_class               =inBody.getIntFromByteArray(1);
        int    received_protocol_id             =inBody.getIntFromByteArray(1);
        int    received_priority_flag           =inBody.getIntFromByteArray(1);
        String received_schedule_delivery_time  =inBody.getStringFromCOctetByteArray(17);
        String received_validity_period         =inBody.getStringFromCOctetByteArray(17);
        int    received_registered_delivery     =inBody.getIntFromByteArray(1);
        int    received_replace_if_present_flag =inBody.getIntFromByteArray(1);
        int    received_data_coding             =inBody.getIntFromByteArray(1);
        int    received_sm_default_msg_id       =inBody.getIntFromByteArray(1);
        int    received_sm_length               =inBody.getIntFromByteArray(1);
        byte[] received_short_message_bytes     =inBody.getByteArrayFromOctetByteArray(received_sm_length);
        String received_short_message           =ConvertLib.createString(received_short_message_bytes);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_deliver_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  dest_addr_ton           ='"+received_dest_addr_ton+"'"
                +"\n  dest_addr_npi           ='"+received_dest_addr_npi+"'"
                +"\n  destination_addr        ='"+received_destination_addr+"'"
                +"\n  esm_class               ='"+received_esm_class+"'"
                +"\n  protocol_id             ='"+received_protocol_id+"'"
                +"\n  priority_flag           ='"+received_priority_flag+"'"
                +"\n  schedule_delivery_time  ='"+received_schedule_delivery_time+"'"
                +"\n  validity_period         ='"+received_validity_period+"'"
                +"\n  registered_delivery     ='"+received_registered_delivery+"'"
                +"\n  replace_if_present_flag ='"+received_replace_if_present_flag+"'"
                +"\n  data_coding             ='"+received_data_coding+"'"
                +"\n  sm_default_msg_id       ='"+received_sm_default_msg_id+"'"
                +"\n  sm_length               ='"+received_sm_length+"'"
                +"\n  short_message           ='"+received_short_message+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/"+received_destination_addr;
        String[] storeVar={"smsc/smpp/"+received_destination_addr,"smsc/"+myServerPort+"/smpp/"+received_destination_addr};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"deliver_sm"                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_ton"      ,""+received_dest_addr_ton         );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_npi"      ,""+received_dest_addr_npi         );
            XTTProperties.setVariable(storeVar[i]+"/"+"destination_addr"   ,""+received_destination_addr      );
            XTTProperties.setVariable(storeVar[i]+"/"+"esm_class"          ,""+received_esm_class             );
            XTTProperties.setVariable(storeVar[i]+"/"+"protocol_id"        ,""+received_protocol_id           );
            XTTProperties.setVariable(storeVar[i]+"/"+"priority_flag"      ,""+received_priority_flag         );
            XTTProperties.setVariable(storeVar[i]+"/"+"schedule_delivery_time",""+received_schedule_delivery_time);
            XTTProperties.setVariable(storeVar[i]+"/"+"validity_period"    ,""+received_validity_period       );
            XTTProperties.setVariable(storeVar[i]+"/"+"registered_delivery",""+received_registered_delivery   );
            XTTProperties.setVariable(storeVar[i]+"/"+"received_replace_if_present_flag",""+received_replace_if_present_flag);
            XTTProperties.setVariable(storeVar[i]+"/"+"data_coding"        ,""+received_data_coding           );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_default_msg_id"  ,""+received_sm_default_msg_id     );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_length"          ,""+received_sm_length             );
            XTTProperties.setVariable(storeVar[i]+"/"+"short_message"      ,""+ConvertLib.base64Encode(received_short_message));
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        if(received_service_type.equalsIgnoreCase("WAP"))
        {
            UDHDecoder udh=new UDHDecoder(received_short_message_bytes,0,storeVar,udhSegmentStore);
            udh.decode();
            if(udh.hasAllSegments())
            {
                // Decode the complete data as WSP protocoll
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_deliver_sm(): udh: all segments received, decoding:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                wsp.decode();
                // Finaly decode the remaining data as mms
                /*
                MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                mms.decode();
                */
                wspb=true;
            } else
            {
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_deliver_sm(): udh: not all segments received, waiting to decode");
            }
        }
        XTTProperties.printTransaction("SMPPWORKER/PDU_DELIVER_SM"+XTTProperties.DELIMITER+received_destination_addr+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =DELIVER_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(DELIVER_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }
        synchronized(omsgkey)
        {
            if((overrideMsgCount>=0&&omsgcount>=overrideMsgCount)||ConvertLib.checkPattern(received_destination_addr,overridePattern))
            {
                outHead.command_status=overrideReturnCode;
            }
            omsgcount++;
            omsgkey.notifyAll();
        }

        ByteArrayWrapper tlvs=tlvMap.get(DELIVER_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(DELIVER_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_deliver_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(msgId),tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Handle a received sms for multiple destinations
     */
    private void pdu_submit_multi(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        int    received_number_of_dests         =inBody.getIntFromByteArray(1);
        int    received_dest_flag[]             =new int[received_number_of_dests];
        int    received_dest_addr_ton[]         =new int[received_number_of_dests];
        int    received_dest_addr_npi[]         =new int[received_number_of_dests];
        String received_destination_addr[]      =new String[received_number_of_dests];
        String received_dl_name[]               =new String[received_number_of_dests];
        for(int i=0;i<received_number_of_dests;i++)
        {
            received_dest_flag[i]               = inBody.getIntFromByteArray(1);
            if(received_dest_flag[i]==1)
            {
                received_dest_addr_ton[i]       =inBody.getIntFromByteArray(1);
                received_dest_addr_npi[i]       =inBody.getIntFromByteArray(1);
                received_destination_addr[i]    =inBody.getStringFromCOctetByteArray(21);
            } else
            {
                received_dl_name[i]             =inBody.getStringFromCOctetByteArray(21);
            }
        }
        int    received_esm_class               =inBody.getIntFromByteArray(1);
        int    received_protocol_id             =inBody.getIntFromByteArray(1);
        int    received_priority_flag           =inBody.getIntFromByteArray(1);
        String received_schedule_delivery_time  =inBody.getStringFromCOctetByteArray(17);
        String received_validity_period         =inBody.getStringFromCOctetByteArray(17);
        int    received_registered_delivery     =inBody.getIntFromByteArray(1);
        int    received_replace_if_present_flag =inBody.getIntFromByteArray(1);
        int    received_data_coding             =inBody.getIntFromByteArray(1);
        int    received_sm_default_msg_id       =inBody.getIntFromByteArray(1);
        int    received_sm_length               =inBody.getIntFromByteArray(1);
        byte[] received_short_message_bytes     =inBody.getByteArrayFromOctetByteArray(received_sm_length);
        String received_short_message           =ConvertLib.createString(received_short_message_bytes);

        // Output the variables
        StringBuffer out=new StringBuffer("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_multi(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"

                +"\n  number_of_dests         ='"+received_number_of_dests+"'");
            for(int i=0;i<received_number_of_dests;i++)
            {
      out.append("\n    dest_flag["+i+"]          ='"+received_dest_flag[i]+"'");
                if(received_dest_flag[i]==1)
                {
      out.append("\n    dest_addr_ton["+i+"]      ='"+received_dest_addr_ton[i]+"'");
      out.append("\n    dest_addr_npi["+i+"]      ='"+received_dest_addr_npi[i]+"'");
      out.append("\n    destination_addr["+i+"]   ='"+received_destination_addr[i]+"'");
                } else
                {
      out.append("\n    dl_name["+i+"]            ='"+received_dl_name[i]+"'");
                }
            }
      out.append("\n  esm_class               ='"+received_esm_class+"'"
                +"\n  protocol_id             ='"+received_protocol_id+"'"
                +"\n  priority_flag           ='"+received_priority_flag+"'"
                +"\n  schedule_delivery_time  ='"+received_schedule_delivery_time+"'"
                +"\n  validity_period         ='"+received_validity_period+"'"
                +"\n  registered_delivery     ='"+received_registered_delivery+"'"
                +"\n  replace_if_present_flag ='"+received_replace_if_present_flag+"'"
                +"\n  data_coding             ='"+received_data_coding+"'"
                +"\n  sm_default_msg_id       ='"+received_sm_default_msg_id+"'"
                +"\n  sm_length               ='"+received_sm_length+"'"
                +"\n  short_message           ='"+received_short_message+"'"
                +"\n");

        // Decode and store the optional parameters of the body
        XTTProperties.printDebug(out.toString());
        inBody.decodeOptionalParameters();

        // Store the variables in memory under the destination address
        String[] storeVar=null;
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<received_number_of_dests;i++)
        {
            XTTProperties.setVariable("smsc/smpp/number_of_dests",""+received_number_of_dests);
            XTTProperties.setVariable("smsc/"+myServerPort+"/smpp/number_of_dests",""+received_number_of_dests);
            if(received_dest_flag[i]==1)
            {
                storeVar=new String[]{"smsc/smpp/"+received_destination_addr[i],"smsc/"+myServerPort+"/smpp/"+received_destination_addr[i]};
            } else
            {
                storeVar=new String[]{"smsc/smpp/"+received_dl_name[i],"smsc/"+myServerPort+"/smpp/"+received_dl_name[i]};
            }
            for(int j=0;j<storeVar.length;j++)
            {
                XTTProperties.setVariable(storeVar[j]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
                XTTProperties.setVariable(storeVar[j]+"/"+"xttserverport"      ,""+myServerPort                   );
                XTTProperties.setVariable(storeVar[j]+"/"+"command_length"     ,""+inHead.command_length          );
                XTTProperties.setVariable(storeVar[j]+"/"+"command_id"         ,""+"submit_multi"                 );
                XTTProperties.setVariable(storeVar[j]+"/"+"command_status"     ,""+inHead.command_status          );
                XTTProperties.setVariable(storeVar[j]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
                XTTProperties.setVariable(storeVar[j]+"/"+"service_type"       ,""+received_service_type          );
                XTTProperties.setVariable(storeVar[j]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
                XTTProperties.setVariable(storeVar[j]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
                XTTProperties.setVariable(storeVar[j]+"/"+"source_addr"        ,""+received_source_addr           );
                XTTProperties.setVariable(storeVar[j]+"/"+"number_of_dests"    ,""+received_number_of_dests       );
                XTTProperties.setVariable(storeVar[j]+"/"+"dest_flag"          ,""+received_dest_flag[i]          );
                if(received_dest_flag[i]==1)
                {
                XTTProperties.setVariable(storeVar[j]+"/"+"dest_addr_ton"      ,""+received_dest_addr_ton[i]      );
                XTTProperties.setVariable(storeVar[j]+"/"+"dest_addr_npi"      ,""+received_dest_addr_npi[i]      );
                XTTProperties.setVariable(storeVar[j]+"/"+"destination_addr"   ,""+received_destination_addr[i]   );
                } else
                {
                XTTProperties.setVariable(storeVar[j]+"/"+"dl_name"   ,""+received_dl_name[i]                     );
                }
                XTTProperties.setVariable(storeVar[j]+"/"+"esm_class"          ,""+received_esm_class             );
                XTTProperties.setVariable(storeVar[j]+"/"+"protocol_id"        ,""+received_protocol_id           );
                XTTProperties.setVariable(storeVar[j]+"/"+"priority_flag"      ,""+received_priority_flag         );
                XTTProperties.setVariable(storeVar[j]+"/"+"schedule_delivery_time",""+received_schedule_delivery_time);
                XTTProperties.setVariable(storeVar[j]+"/"+"validity_period"    ,""+received_validity_period       );
                XTTProperties.setVariable(storeVar[j]+"/"+"registered_delivery",""+received_registered_delivery   );
                XTTProperties.setVariable(storeVar[j]+"/"+"received_replace_if_present_flag",""+received_replace_if_present_flag);
                XTTProperties.setVariable(storeVar[j]+"/"+"data_coding"        ,""+received_data_coding           );
                XTTProperties.setVariable(storeVar[j]+"/"+"sm_default_msg_id"  ,""+received_sm_default_msg_id     );
                XTTProperties.setVariable(storeVar[j]+"/"+"sm_length"          ,""+received_sm_length             );
                XTTProperties.setVariable(storeVar[j]+"/"+"short_message"      ,""+ConvertLib.base64Encode(received_short_message));
                if(received_service_type.equalsIgnoreCase("WAP"))
                {
                    UDHDecoder udh=new UDHDecoder(received_short_message_bytes,0,storeVar,udhSegmentStore);
                    udh.decode();
                    if(udh.hasAllSegments())
                    {
                        // Decode the complete data as WSP protocoll
                        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_multi(): udh: all segments received, decoding:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                        WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                        wsp.decode();
                        // Finaly decode the remaining data as mms
                        /*
                        MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                        mms.decode();
                        */
                        wspb=true;
                    } else
                    {
                        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_multi(): udh: not all segments received, waiting to decode");
                    }
                }
            }
        }
        XTTProperties.printTransaction("SMPPWORKER/PDU_SUBMIT_MULTI"+XTTProperties.DELIMITER+received_destination_addr.length+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);

        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =SUBMIT_MULTI_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(SUBMIT_MULTI+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(SUBMIT_MULTI+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        // Send the response
        byte[] messageID=ConvertLib.getCOctetByteArrayFromString(msgId);
        byte[] response=new byte[messageID.length+1];
        for(int i=0;i<messageID.length;i++)
        {
            response[i]=messageID[i];
        }
        response[messageID.length]=0x00;
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(SUBMIT_MULTI+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_multi(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Handle a received sms
     */
    private void pdu_broadcast_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_priority_flag           =inBody.getIntFromByteArray(1);
        String received_schedule_delivery_time  =inBody.getStringFromCOctetByteArray(17);
        String received_validity_period         =inBody.getStringFromCOctetByteArray(17);
        int    received_replace_if_present_flag =inBody.getIntFromByteArray(1);
        int    received_data_coding             =inBody.getIntFromByteArray(1);
        int    received_sm_default_msg_id       =inBody.getIntFromByteArray(1);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_broadcast_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  priority_flag           ='"+received_priority_flag+"'"
                +"\n  schedule_delivery_time  ='"+received_schedule_delivery_time+"'"
                +"\n  validity_period         ='"+received_validity_period+"'"
                +"\n  replace_if_present_flag ='"+received_replace_if_present_flag+"'"
                +"\n  data_coding             ='"+received_data_coding+"'"
                +"\n  sm_default_msg_id       ='"+received_sm_default_msg_id+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/";
        String[] storeVar={"smsc/smpp","smsc/"+myServerPort+"/smpp"};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"broadcast_sm"                 );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"priority_flag"      ,""+received_priority_flag         );
            XTTProperties.setVariable(storeVar[i]+"/"+"schedule_delivery_time",""+received_schedule_delivery_time);
            XTTProperties.setVariable(storeVar[i]+"/"+"validity_period"    ,""+received_validity_period       );
            XTTProperties.setVariable(storeVar[i]+"/"+"received_replace_if_present_flag",""+received_replace_if_present_flag);
            XTTProperties.setVariable(storeVar[i]+"/"+"data_coding"        ,""+received_data_coding           );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_default_msg_id"  ,""+received_sm_default_msg_id     );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_BROADCAST_SM"+XTTProperties.DELIMITER+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);

        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =BROADCAST_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(BROADCAST_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(BROADCAST_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(BROADCAST_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_broadcast_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(msgId),tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }


    /**
     * Handle a received sms
     */
    private void pdu_data_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_service_type            =inBody.getStringFromCOctetByteArray(6);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(65);
        int    received_dest_addr_ton           =inBody.getIntFromByteArray(1);
        int    received_dest_addr_npi           =inBody.getIntFromByteArray(1);
        String received_destination_addr        =inBody.getStringFromCOctetByteArray(65);
        int    received_esm_class               =inBody.getIntFromByteArray(1);
        int    received_registered_delivery     =inBody.getIntFromByteArray(1);
        int    received_data_coding             =inBody.getIntFromByteArray(1);

        boolean isUDH=((received_esm_class&64)==64);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): translates to: \n"
                +"\n  service_type            ='"+received_service_type+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  dest_addr_ton           ='"+received_dest_addr_ton+"'"
                +"\n  dest_addr_npi           ='"+received_dest_addr_npi+"'"
                +"\n  destination_addr        ='"+received_destination_addr+"'"
                +"\n  esm_class               ='"+received_esm_class+"' -> UDH="+isUDH
                +"\n  registered_delivery     ='"+received_registered_delivery+"'"
                +"\n  data_coding             ='"+received_data_coding+"'"
                +"\n");

        // Store the variables in memory under the destination address
        //String storeVar="smsc/smpp/"+received_destination_addr;
        String[] storeVar={"smsc/smpp/"+received_destination_addr,"smsc/"+myServerPort+"/smpp/"+received_destination_addr};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"data_sm"                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"service_type"       ,""+received_service_type          );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_ton"      ,""+received_dest_addr_ton         );
            XTTProperties.setVariable(storeVar[i]+"/"+"dest_addr_npi"      ,""+received_dest_addr_npi         );
            XTTProperties.setVariable(storeVar[i]+"/"+"destination_addr"   ,""+received_destination_addr      );
            XTTProperties.setVariable(storeVar[i]+"/"+"esm_class"          ,""+received_esm_class             );
            XTTProperties.setVariable(storeVar[i]+"/"+"registered_delivery",""+received_registered_delivery   );
            XTTProperties.setVariable(storeVar[i]+"/"+"data_coding"        ,""+received_data_coding           );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);

        // Because we are just using the smsc to get wap push we can directly decode here:
        // First decode the udh form the optional parameters payload
        if(isUDH)
        {
            inBody.decodeOptionalParameters();
            //UDH Indicator present
            UDHDecoder udh=new UDHDecoder(inBody.getPayload(),0,storeVar,udhSegmentStore);
            udh.decode();
            // if we have all segments of an udh we decode the completed data
            if(udh.hasAllSegments())
            {
                // Decode the complete data as WSP protocoll
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): udh: all segments received, decoding WSP:\n"+ConvertLib.getHexView(udh.getPayload(),0,udh.getPayload().length));
                WSPDecoder wsp=new WSPDecoder(udh.getPayload(),0,storeVar);
                wsp.decode();
                // Finaly decode the remaining data as mms
                if(wsp.getContentType().equals("application/vnd.wap.sic"))
                {
                    XTTProperties.printWarn("SMPPWorker("+myServerPort+"/"+getWorkerId()+"): application/vnd.wap.sic not yet supported");
                } else
                {
                    MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                    mms.decode();
                }
                wspb=true;
            } else
            {
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): udh: not all segments received, waiting to decode");
            }
        } else
        {
            //UDH Indicator not present, decode WAP directly
            inBody.decodeOptionalParameters(udhSegmentStore);
            if(inBody.hasAllSegments())
            {
                // Decode the complete data as WSP protocoll
                if(inBody.getPayload() != null)
                {
                    XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): all segments received, decoding WSP:\n"+ConvertLib.getHexView(inBody.getPayload(),0,inBody.getPayload().length));
                    WSPDecoder wsp=new WSPDecoder(inBody.getPayload(),0,storeVar);
                    wsp.decode();
                    // Finaly decode the remaining data as mms
                    MMSDecoder mms=new MMSDecoder(wsp.getPayload(),0,storeVar);
                    mms.decode();
                    wspb=true;
                }
            } else
            {
                XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): not all segments received, waiting to decode");
            }
        }
        XTTProperties.printTransaction("SMPPWORKER/PDU_DATA_SM"+XTTProperties.DELIMITER+received_destination_addr+XTTProperties.DELIMITER+received_source_addr+XTTProperties.DELIMITER+received_service_type);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =DATA_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(DATA_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }
        synchronized(omsgkey)
        {
            if((overrideMsgCount>=0&&omsgcount>=overrideMsgCount)||ConvertLib.checkPattern(received_destination_addr,overridePattern))
            {
                outHead.command_status=overrideReturnCode;
            }
            omsgcount++;
            omsgkey.notifyAll();
        }

        ByteArrayWrapper tlvs=tlvMap.get(DATA_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(DATA_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_data_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(msgId),tlvb);
                if(injectAutoMessages)
                {
                    XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_submit_sm(): created auto message type "+DELIVER_SM);
                    addAutoMessage(new SMSCOriginatedMessage(DELIVER_SM,
                        createDeliverSM(received_dest_addr_ton,received_dest_addr_npi,received_destination_addr,
                                        received_source_addr_ton,received_source_addr_npi,received_source_addr,
                                        0,msgId)
                                                            )
                                  );
                }
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    /**
     * Calculate the message id for responses
     */
    private String createMessageId()
    {
        int current_message_id;
        synchronized (key)
        {
            message_id++;
            current_message_id=message_id;
            key.notify();
        }
        StringBuffer dispay_message_id=new StringBuffer();
        dispay_message_id.append("XTTMSG-");
        for(int i=0;i<(10-Integer.toHexString(current_message_id).length());i++)
        {
            dispay_message_id.append("0");
        }
        dispay_message_id.append(Integer.toHexString(current_message_id));
        return dispay_message_id.toString();
    }

    private void pdu_query_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_query_sm(): translates to: \n"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/"+received_message_id;
        String[] storeVar={"smsc/smpp/"+received_message_id,"smsc/"+myServerPort+"/smpp/"+received_message_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"          ,""+xtttimestamp                      );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"query_sm"                     );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_QUERY_SM"+XTTProperties.DELIMITER+received_message_id+XTTProperties.DELIMITER+received_source_addr);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =QUERY_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(QUERY_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(QUERY_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        // Send the response
        byte[] messageID=ConvertLib.getCOctetByteArrayFromString(msgId);
        byte[] response=new byte[messageID.length+3];
        for(int i=0;i<messageID.length;i++)
        {
            response[i]=messageID[i];
        }
        response[messageID.length-2]=0x00;
        response[messageID.length-1]=0x00;
        response[messageID.length]=0x00;
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(QUERY_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_query_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }

    private void pdu_replace_sm(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        boolean wspb=false;
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body object from the stream
        inBody.readPDUBody(inHead,smppIN);


        // Map the received body attributes to variables
        String received_message_id              =inBody.getStringFromCOctetByteArray(65);
        int    received_source_addr_ton         =inBody.getIntFromByteArray(1);
        int    received_source_addr_npi         =inBody.getIntFromByteArray(1);
        String received_source_addr             =inBody.getStringFromCOctetByteArray(21);
        String received_schedule_delivery_time  =inBody.getStringFromCOctetByteArray(17);
        String received_validity_period         =inBody.getStringFromCOctetByteArray(17);
        int    received_registered_delivery     =inBody.getIntFromByteArray(1);
        int    received_sm_default_msg_id       =inBody.getIntFromByteArray(1);
        int    received_sm_length               =inBody.getIntFromByteArray(1);
        byte[] received_short_message_bytes     =inBody.getByteArrayFromOctetByteArray(received_sm_length);
        String received_short_message           =ConvertLib.createString(received_short_message_bytes);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_replace_sm(): translates to: \n"
                +"\n  message_id              ='"+received_message_id+"'"
                +"\n  source_addr_ton         ='"+received_source_addr_ton+"'"
                +"\n  source_addr_npi         ='"+received_source_addr_npi+"'"
                +"\n  source_addr             ='"+received_source_addr+"'"
                +"\n  schedule_delivery_time  ='"+received_schedule_delivery_time+"'"
                +"\n  validity_period         ='"+received_validity_period+"'"
                +"\n  registered_delivery     ='"+received_registered_delivery+"'"
                +"\n  sm_default_msg_id       ='"+received_sm_default_msg_id+"'"
                +"\n  sm_length               ='"+received_sm_length+"'"
                +"\n  short_message           ='"+received_short_message+"'"
                +"\n");

        // Store the variables in memmory under the destination address
        //String storeVar="smsc/smpp/"+received_message_id;
        String[] storeVar={"smsc/smpp/"+received_message_id,"smsc/"+myServerPort+"/smpp/"+received_message_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"       ,""+xtttimestamp                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"replace_sm"                   );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status          );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence        );
            XTTProperties.setVariable(storeVar[i]+"/"+"message_id"         ,""+received_message_id            );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_ton"    ,""+received_source_addr_ton       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr_npi"    ,""+received_source_addr_npi       );
            XTTProperties.setVariable(storeVar[i]+"/"+"source_addr"        ,""+received_source_addr           );
            XTTProperties.setVariable(storeVar[i]+"/"+"schedule_delivery_time",""+received_schedule_delivery_time);
            XTTProperties.setVariable(storeVar[i]+"/"+"validity_period"    ,""+received_validity_period       );
            XTTProperties.setVariable(storeVar[i]+"/"+"registered_delivery",""+received_registered_delivery   );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_default_msg_id"  ,""+received_sm_default_msg_id     );
            XTTProperties.setVariable(storeVar[i]+"/"+"sm_length"          ,""+received_sm_length             );
            XTTProperties.setVariable(storeVar[i]+"/"+"short_message"      ,""+ConvertLib.base64Encode(received_short_message));
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();

        XTTProperties.printTransaction("SMPPWORKER/PDU_REPLACE_SM"+XTTProperties.DELIMITER+received_message_id+XTTProperties.DELIMITER+received_source_addr);


        // Create the response header
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id      =REPLACE_SM_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status  =ESME_ROK;

        Integer cmdStatusOverridei=cmdStatusOverride.get(REPLACE_SM+"");
        if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
        {
            outHead.command_status  =cmdStatusOverridei.intValue();
        }

        ByteArrayWrapper tlvs=tlvMap.get(REPLACE_SM+"");
        byte[] tlvb=null;
        if(tlvs!=null)
        {
            tlvb=tlvs.getArray();
        }

        String msgId=createMessageId();
        byte[] response=new byte[0];
        ByteArrayWrapper overrrideResponse=overrideResponseMap.get(REPLACE_SM+"");
        byte[] overrrideResponseB=null;
        if(overrrideResponse!=null)
        {
            overrrideResponseB=overrrideResponse.getArray();
        }
        // Send the response
        synchronized(socket)
        {
            if(overrrideResponseB!=null)
            {
                XTTProperties.printVerbose("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_replace_sm(): writeOverrideResponse: "+overrrideResponseB.length+" bytes\n"+ConvertLib.getHexView(overrrideResponseB));
                smppOUT.write(overrrideResponseB);
                smppOUT.flush();
            } else
            {
                outHead.writePDU(smppOUT,response,tlvb);
            }
        }
        // Notify all waiting for a message to arrive
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
    }



    private void pdu_bind_receiver(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        String received_system_id           =inBody.getStringFromCOctetByteArray(16);
        String received_password            =inBody.getStringFromCOctetByteArray(9);
        String received_system_type         =inBody.getStringFromCOctetByteArray(13);
        int    received_interface_version   =inBody.getIntFromByteArray(1);
        int    received_addr_ton            =inBody.getIntFromByteArray(1);
        int    received_addr_npi            =inBody.getIntFromByteArray(1);
        String received_address_range       =inBody.getStringFromCOctetByteArray(41);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_bind_receiver(): translates to: \n"
                +"\n  system_id         ='"+received_system_id+"'"
                +"\n  password          ='"+received_password+"'"
                +"\n  system_type       ='"+received_system_type+"'"
                +"\n  interface_version ='0x"+ConvertLib.intToHex(received_interface_version)+"'"
                +"\n  addr_ton          ='"+received_addr_ton+"'"
                +"\n  addr_npi          ='"+received_addr_npi+"'"
                +"\n  address_range     ='"+received_address_range+"'"
                +"\n");

        // Store the variables in memory under the system id
        //String storeVar="smsc/smpp/"+received_system_id;
        String[] storeVar={"smsc/smpp/"+received_system_id,"smsc/"+myServerPort+"/smpp/"+received_system_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"       ,""+xtttimestamp               );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort               );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"bind_receiver"            );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence    );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_id"          ,""+received_system_id         );
            XTTProperties.setVariable(storeVar[i]+"/"+"password"           ,""+received_password          );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_type"        ,""+received_system_type       );
            XTTProperties.setVariable(storeVar[i]+"/"+"interface_version"  ,"0x"+ConvertLib.intToHex(received_interface_version));
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_ton"           ,""+received_addr_ton          );
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_npi"           ,""+received_addr_npi          );
            XTTProperties.setVariable(storeVar[i]+"/"+"address_range"      ,""+received_address_range     );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();
        XTTProperties.printTransaction("SMPPWORKER/PDU_BIND_RECEIVER"+XTTProperties.DELIMITER+received_system_id+XTTProperties.DELIMITER+received_password+XTTProperties.DELIMITER+received_system_type);

        // Create the response
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=BIND_TRANSCEIVER_RESP;
        outHead.command_sequence=inHead.command_sequence;

        // Get the current system password
        String thispw=null;
        synchronized(passwordKey)
        {
            thispw=new String(password);
        }

        // Check if the system id is used and if yes if it is correct
        if(!systemid.equals("")&&!received_system_id.equals(systemid))
        {
            outHead.command_status=ESME_RINVSYSID;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Check if the password is correct
        } else if (!thispw.equals("")&&!received_password.equals(thispw))
        {
            outHead.command_status=ESME_RINVPASWD;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Else everything is ok
        } else
        {
            outHead.command_status=ESME_ROK;
            Integer cmdStatusOverridei=cmdStatusOverride.get(BIND_RECEIVER+"");
            if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
            {
                outHead.command_status  =cmdStatusOverridei.intValue();
            }// Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(systemname));
            }
            // Notify all waiting for a connect
            synchronized(conkey)
            {
                myConnection=received_system_id;
                myConnectionIp=socket.getInetAddress().toString();
                connected.add(myConnection);
                connectedIp.add(myConnectionIp);
                conkey.notifyAll();
                concount++;
            }
        }
    }


    /**
     * Does a bind transceiver
     */
    private void pdu_bind_transceiver(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        String received_system_id           =inBody.getStringFromCOctetByteArray(16);
        String received_password            =inBody.getStringFromCOctetByteArray(9);
        String received_system_type         =inBody.getStringFromCOctetByteArray(13);
        int    received_interface_version   =inBody.getIntFromByteArray(1);
        int    received_addr_ton            =inBody.getIntFromByteArray(1);
        int    received_addr_npi            =inBody.getIntFromByteArray(1);
        String received_address_range       =inBody.getStringFromCOctetByteArray(41);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_bind_transceiver(): translates to: \n"
                +"\n  system_id         ='"+received_system_id+"'"
                +"\n  password          ='"+received_password+"'"
                +"\n  system_type       ='"+received_system_type+"'"
                +"\n  interface_version ='0x"+ConvertLib.intToHex(received_interface_version)+"'"
                +"\n  addr_ton          ='"+received_addr_ton+"'"
                +"\n  addr_npi          ='"+received_addr_npi+"'"
                +"\n  address_range     ='"+received_address_range+"'"
                +"\n");

        // Store the variables in memory under the system id
        //String storeVar="smsc/smpp/"+received_system_id;
        String[] storeVar={"smsc/smpp/"+received_system_id,"smsc/"+myServerPort+"/smpp/"+received_system_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"       ,""+xtttimestamp               );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort               );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"bind_transceiver"         );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence    );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_id"          ,""+received_system_id         );
            XTTProperties.setVariable(storeVar[i]+"/"+"password"           ,""+received_password          );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_type"        ,""+received_system_type       );
            XTTProperties.setVariable(storeVar[i]+"/"+"interface_version"  ,"0x"+ConvertLib.intToHex(received_interface_version));
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_ton"           ,""+received_addr_ton          );
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_npi"           ,""+received_addr_npi          );
            XTTProperties.setVariable(storeVar[i]+"/"+"address_range"      ,""+received_address_range     );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();
        XTTProperties.printTransaction("SMPPWORKER/PDU_BIND_TRANCEIVER"+XTTProperties.DELIMITER+received_system_id+XTTProperties.DELIMITER+received_password+XTTProperties.DELIMITER+received_system_type);

        // Create the response
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=BIND_TRANSCEIVER_RESP;
        outHead.command_sequence=inHead.command_sequence;

        // Get the current system password
        String thispw=null;
        synchronized(passwordKey)
        {
            thispw=new String(password);
        }

        // Check if the system id is used and if yes if it is correct
        if(!systemid.equals("")&&!received_system_id.equals(systemid))
        {
            outHead.command_status=ESME_RINVSYSID;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Check if the password is correct
        } else if (!thispw.equals("")&&!received_password.equals(thispw))
        {
            outHead.command_status=ESME_RINVPASWD;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Else everything is ok
        } else
        {
            outHead.command_status=ESME_ROK;
            Integer cmdStatusOverridei=cmdStatusOverride.get(BIND_TRANSCEIVER+"");
            if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
            {
                outHead.command_status  =cmdStatusOverridei.intValue();
            }

            ByteArrayWrapper tlvs=tlvMap.get(BIND_TRANSCEIVER+"");
            byte[] tlvb=null;
            if(tlvs!=null)
            {
                tlvb=tlvs.getArray();
            }
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(systemname),tlvb);
            }
            // Notify all waiting for a connect
            synchronized(conkey)
            {
                myConnection=received_system_id;
                myConnectionIp=socket.getInetAddress().toString();
                connected.add(myConnection);
                connectedIp.add(myConnectionIp);
                conkey.notifyAll();
                concount++;
            }
        }
    }

    /**
     * Does a bind transceiver
     */
    private void pdu_bind_transmitter(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        String received_system_id           =inBody.getStringFromCOctetByteArray(16);
        String received_password            =inBody.getStringFromCOctetByteArray(9);
        String received_system_type         =inBody.getStringFromCOctetByteArray(13);
        int    received_interface_version   =inBody.getIntFromByteArray(1);
        int    received_addr_ton            =inBody.getIntFromByteArray(1);
        int    received_addr_npi            =inBody.getIntFromByteArray(1);
        String received_address_range       =inBody.getStringFromCOctetByteArray(41);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_bind_transmitter(): translates to: \n"
                +"\n  system_id         ='"+received_system_id+"'"
                +"\n  password          ='"+received_password+"'"
                +"\n  system_type       ='"+received_system_type+"'"
                +"\n  interface_version ='0x"+ConvertLib.intToHex(received_interface_version)+"'"
                +"\n  addr_ton          ='"+received_addr_ton+"'"
                +"\n  addr_npi          ='"+received_addr_npi+"'"
                +"\n  address_range     ='"+received_address_range+"'"
                +"\n");

        // Store the variables in memory under the system id
        //String storeVar="smsc/smpp/"+received_system_id;
        String[] storeVar={"smsc/smpp/"+received_system_id,"smsc/"+myServerPort+"/smpp/"+received_system_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"       ,""+xtttimestamp               );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort               );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"bind_transmitter"         );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence    );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_id"          ,""+received_system_id         );
            XTTProperties.setVariable(storeVar[i]+"/"+"password"           ,""+received_password          );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_type"        ,""+received_system_type       );
            XTTProperties.setVariable(storeVar[i]+"/"+"interface_version"  ,"0x"+ConvertLib.intToHex(received_interface_version ));
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_ton"           ,""+received_addr_ton          );
            XTTProperties.setVariable(storeVar[i]+"/"+"addr_npi"           ,""+received_addr_npi          );
            XTTProperties.setVariable(storeVar[i]+"/"+"address_range"      ,""+received_address_range     );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();
        XTTProperties.printTransaction("SMPPWORKER/PDU_BIND_TRANSMITTER"+XTTProperties.DELIMITER+received_system_id+XTTProperties.DELIMITER+received_password+XTTProperties.DELIMITER+received_system_type);

        // Create the response
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=BIND_TRANSMITTER_RESP;
        outHead.command_sequence=inHead.command_sequence;

        // Get the current system password
        String thispw=null;
        synchronized(passwordKey)
        {
            thispw=new String(password);
        }

        // Check if the system id is used and if yes if it is correct
        if(!systemid.equals("")&&!received_system_id.equals(systemid))
        {
            outHead.command_status=ESME_RINVSYSID;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Check if the password is correct
        } else if (!thispw.equals("")&&!received_password.equals(thispw))
        {
            outHead.command_status=ESME_RINVPASWD;
            // Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,null);
            }
        // Else everything is ok
        } else
        {
            outHead.command_status=ESME_ROK;
            Integer cmdStatusOverridei=cmdStatusOverride.get(BIND_TRANSMITTER+"");
            if(cmdStatusOverridei!=null&&cmdStatusOverridei.intValue()>0)
            {
                outHead.command_status  =cmdStatusOverridei.intValue();
            }// Send the response
            synchronized(socket)
            {
                outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(systemname));
            }
            // Notify all waiting for a connect
            synchronized(conkey)
            {
                myConnection=received_system_id;
                myConnectionIp=socket.getInetAddress().toString();
                connected.add(myConnection);
                connectedIp.add(myConnectionIp);
                conkey.notifyAll();
                concount++;
            }
        }
    }

    private void pdu_outbind(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        // Create a new body object
        SMPP_PDUBody inBody=new SMPP_PDUBody("SMPP_PDUBody("+myServerPort+"/"+getWorkerId()+")");
        // read the body from the stream
        inBody.readPDUBody(inHead,smppIN);

        // Map the received body attributes to variables
        String received_system_id           =inBody.getStringFromCOctetByteArray(16);
        String received_password            =inBody.getStringFromCOctetByteArray(9);

        // Output the variables
        XTTProperties.printDebug("SMPPWorker("+myServerPort+"/"+getWorkerId()+").pdu_outbind(): translates to: \n"
                +"\n  system_id         ='"+received_system_id+"'"
                +"\n  password          ='"+received_password+"'"
                +"\n");

        // Store the variables in memory under the system id
        //String storeVar="smsc/smpp/"+received_system_id;
        String[] storeVar={"smsc/smpp/"+received_system_id,"smsc/"+myServerPort+"/smpp/"+received_system_id};
        long xtttimestamp=System.currentTimeMillis();
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+"/"+"xtttimestamp"       ,""+xtttimestamp               );
            XTTProperties.setVariable(storeVar[i]+"/"+"xttserverport"      ,""+myServerPort               );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_length"     ,""+inHead.command_length      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_id"         ,""+"outbind"                  );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_status"     ,""+inHead.command_status      );
            XTTProperties.setVariable(storeVar[i]+"/"+"command_sequence"   ,""+inHead.command_sequence    );
            XTTProperties.setVariable(storeVar[i]+"/"+"system_id"          ,""+received_system_id         );
            XTTProperties.setVariable(storeVar[i]+"/"+"password"           ,""+received_password          );
        }
        // Decode and store the optional parameters of the body
        inBody.setStoreVar(storeVar);
        inBody.decodeOptionalParameters();
        XTTProperties.printTransaction("SMPPWORKER/PDU_OUTBIND"+XTTProperties.DELIMITER+received_system_id+XTTProperties.DELIMITER+received_password+XTTProperties.DELIMITER);

        // Create the response
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=BIND_TRANSCEIVER_RESP;
        outHead.command_sequence=inHead.command_sequence;

        // Get the current system password
        String thispw=null;
        synchronized(passwordKey)
        {
            thispw=new String(password);
        }

        // Check if the system id is used and if yes if it is correct
        if(!systemid.equals("")&&!received_system_id.equals(systemid))
        {
            outHead.command_status=ESME_RINVSYSID;
            // Send the response
            //outHead.writePDU(smppOUT,null);
        // Check if the password is correct
        } else if (!thispw.equals("")&&!received_password.equals(thispw))
        {
            outHead.command_status=ESME_RINVPASWD;
            // Send the response
            //outHead.writePDU(smppOUT,null);
        // Else everything is ok
        } else
        {
            outHead.command_status=ESME_ROK;
            // Send the response
            //outHead.writePDU(smppOUT,ConvertLib.getCOctetByteArrayFromString(systemname));
            // Notify all waiting for a connect
            synchronized(conkey)
            {
                myConnection=received_system_id;
                myConnectionIp=socket.getInetAddress().toString();
                connected.add(myConnection);
                connectedIp.add(myConnectionIp);
                conkey.notifyAll();
                concount++;
            }
        }
    }


    private void pdu_enquire_link(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=ENQUIRE_LINK_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status=ESME_ROK;
        synchronized(socket)
        {
            outHead.writePDU(smppOUT,null);
        }
    }
    private void pdu_unbind(SMPP_PDUHeader inHead) throws Exception // java.io.IOException
    {
        SMPP_PDUHeader outHead=new SMPP_PDUHeader(("SMPP_PDUheader("+myServerPort+"/"+getWorkerId()+")"));
        outHead.command_id=UNBIND_RESP;
        outHead.command_sequence=inHead.command_sequence;
        outHead.command_status=ESME_ROK;
        synchronized(conkey)
        {
            connected.remove(myConnection);
            connectedIp.remove(myConnectionIp);
            myConnection=null;
            myConnectionIp=null;
            conkey.notifyAll();
        }
        synchronized(socket)
        {
            outHead.writePDU(smppOUT,null);
        }
    }

    public static byte[] readResponse(String functionname,BufferedInputStream in, String[] storeVar) throws Exception
    {
        return readResponse(functionname,in, storeVar,null);
    }
    public static byte[] readResponse(String functionname,BufferedInputStream in, String[] storeVar, SMPP_PDUHeader headV) throws Exception
    {
        SMPP_PDUHeader head=headV;
        if(head==null)
        {
            head=new SMPP_PDUHeader(functionname);
            head.readPDUHeader(in);
        }
        if(storeVar!=null)
        {
            for(int i=0;i<storeVar.length;i++)
            {
                XTTProperties.setVariable(storeVar[i]+"/command_length"  ,"0x"+ConvertLib.intToHex(head.command_length  ,8));
                XTTProperties.setVariable(storeVar[i]+"/command_id"      ,"0x"+ConvertLib.intToHex(head.command_id      ,8));
                XTTProperties.setVariable(storeVar[i]+"/command_status"  ,"0x"+ConvertLib.intToHex(head.command_status  ,8));
                XTTProperties.setVariable(storeVar[i]+"/command_sequence","0x"+ConvertLib.intToHex(head.command_sequence,8));
            }
        }
        SMPP_PDUBody body=new SMPP_PDUBody(functionname);
        body.setStoreVar(storeVar);
        switch(head.command_id)
        {
            case BIND_RECEIVER_RESP      :
            case BIND_TRANSMITTER_RESP   :
            case BIND_TRANSCEIVER_RESP   :
            {
                body.readPDUBody(head,in);
                String received_system_id=body.getStringFromCOctetByteArray(16);
                XTTProperties.printDebug(functionname+": translates to: \n"
                        +"\n  system_id         ='"+received_system_id+"'"
                        +"\n");
                if(storeVar!=null)
                {
                    for(int i=0;i<storeVar.length;i++)
                    {
                        XTTProperties.setVariable(storeVar[i]+"/"+"system_id",""+received_system_id);
                    }
                }
                body.decodeOptionalParameters();
                break;
            }
            case UNBIND_RESP             :
            {
                break;
            }
            case SUBMIT_SM_RESP          :
            case DELIVER_SM_RESP         :
            {
                body.readPDUBody(head,in);
                String message_id=body.getStringFromCOctetByteArray(65);
                XTTProperties.printDebug(functionname+": translates to: \n"
                        +"\n  message_id        ='"+message_id+"'"
                        +"\n");
                if(storeVar!=null)
                {
                    for(int i=0;i<storeVar.length;i++)
                    {
                        XTTProperties.setVariable(storeVar[i]+"/"+"message_id",""+message_id);
                    }
                }
                body.decodeOptionalParameters();
                break;
            }
            case QUERY_SM_RESP           :
            {
                body.readPDUBody(head,in);
                String message_id    =body.getStringFromCOctetByteArray(65);
                String final_date    =body.getStringFromCOctetByteArray(17);
                int    message_state =body.getIntFromByteArray(1);
                int    error_code    =body.getIntFromByteArray(1);
                XTTProperties.printDebug(functionname+": translates to: \n"
                        +"\n  message_id        ='"+message_id+"'"
                        +"\n  final_date        ='"+final_date+"'"
                        +"\n  message_state     ='"+message_state+"'"
                        +"\n  error_code        ='"+error_code+"'"
                        +"\n");
                if(storeVar!=null)
                {
                    for(int i=0;i<storeVar.length;i++)
                    {
                        XTTProperties.setVariable(storeVar[i]+"/"+"message_id",""+message_id);
                        XTTProperties.setVariable(storeVar[i]+"/"+"final_date",""+final_date);
                        XTTProperties.setVariable(storeVar[i]+"/"+"message_state",""+message_state);
                        XTTProperties.setVariable(storeVar[i]+"/"+"error_code",""+error_code);
                    }
                }
//                body.decodeOptionalParameters(); //RN - no optional parameters for QUERY_SM_RESP PDUs
                break;
            }
            case CANCEL_SM_RESP          :
            {
                break;
            }
            case REPLACE_SM_RESP         :
                body.readPDUBody(head,in);
//                body.decodeOptionalParameters();  //RN - no optional parameters for QUERY_SM_RESP PDUs
                break;
            case ENQUIRE_LINK_RESP       :
            {
                break;
            }
            case SUBMIT_MULTI_RESP       :
            {
                StringBuffer output=new StringBuffer(functionname+": translates to: \n");
                body.readPDUBody(head,in);
                String message_id    = body.getStringFromCOctetByteArray(65);
                output.append("\n  message_id        ='"+message_id+"'");
                XTTProperties.setVariable(storeVar+"/"+"message_id",""+message_id);
                int no_unsuccess     = body.getIntFromByteArray(1);
                output.append("\n  no_unsuccess      ='"+no_unsuccess+"'");
                XTTProperties.setVariable(storeVar+"/"+"no_unsuccess",""+no_unsuccess);
                for(int i=0;i<no_unsuccess;i++)
                {
                    int    dest_addr_ton    =body.getIntFromByteArray(1);
                    int    dest_addr_npi    =body.getIntFromByteArray(1);
                    String destination_addr =body.getStringFromCOctetByteArray(21);
                    int    error_status_code=body.getIntFromByteArray(4);
                    output.append("\n  dest_addr_ton      ='"+dest_addr_ton+"'");
                    output.append("\n  dest_addr_npi      ='"+dest_addr_npi+"'");
                    output.append("\n  destination_addr   ='"+destination_addr+"'");
                    output.append("\n  error_status_code  ='"+error_status_code+"'");
                    if(storeVar!=null)
                    {
                        for(int j=0;j<storeVar.length;j++)
                        {
                                XTTProperties.setVariable(storeVar[j]+"/"+"dest_addr_ton",""+dest_addr_ton);
                                XTTProperties.setVariable(storeVar[j]+"/"+"dest_addr_npi",""+dest_addr_npi);
                                XTTProperties.setVariable(storeVar[j]+"/"+"destination_addr",""+destination_addr);
                                XTTProperties.setVariable(storeVar[j]+"/"+"error_status_code",""+error_status_code);
                        }
                    }
                }
                XTTProperties.printDebug(output.toString()+"\n");
                body.decodeOptionalParameters();
                break;
            }
            case DATA_SM_RESP            :
            {
                body.readPDUBody(head,in);
                String message_id=body.getStringFromCOctetByteArray(65);
                XTTProperties.printDebug(functionname+": translates to: \n"
                        +"\n  message_id        ='"+message_id+"'"
                        +"\n");
                if(storeVar!=null)
                {
                    for(int i=0;i<storeVar.length;i++)
                    {
                        XTTProperties.setVariable(storeVar[i]+"/"+"message_id",""+message_id);
                    }
                }
                body.decodeOptionalParameters();
                break;
            }
            case GENERIC_NAK             :
            {
                break;
            }
            case BROADCAST_SM_RESP       :
            case QUERY_BROADCAST_SM_RESP :
            {
                body.readPDUBody(head,in);
                String message_id=body.getStringFromCOctetByteArray(65);
                XTTProperties.printDebug(functionname+": translates to: \n"
                        +"\n  message_id        ='"+message_id+"'"
                        +"\n");
                if(storeVar!=null)
                {
                    for(int i=0;i<storeVar.length;i++)
                    {
                        XTTProperties.setVariable(storeVar[i]+"/"+"message_id",""+message_id);
                    }
                }
                body.decodeOptionalParameters();
                break;
            }
            case CANCEL_BROADCAST_SM_RESP:
            {
                break;
            }
            default:
                XTTProperties.printFail(functionname+": command_id '"+head.command_id+"' of response unsupported");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                break;
        }
        return body.getBody();
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
        XTTProperties.printDebug("SMPPWorker$injectAutoMessage("+myServerPort+"/"+getWorkerId()+"): currentTime="+currentTime+" sendtime="+message.getSendTime());

        if(message.getSendTime()<=currentTime)
        {
            synchronized(socket)
            {
                message.writePDU(out,autoMessagesCommandSequence++);
                autoMessages.remove(message);
            }
        }
    }
    private class SMSCOriginatedMessage implements Comparable<SMSCOriginatedMessage>
    {
        public int  command_id      = 0;
        private long sendtime=0;public long getSendTime(){return sendtime;};
        private byte[] packet=null;

        public SMSCOriginatedMessage(int command_id, byte[] packet)
        {
            this.packet=packet;
            this.sendtime=getTimeInMillis();
            this.command_id=command_id;
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
        }/*
        public String toString()
        {
            return message;
        }*/
        public int compareTo(SMSCOriginatedMessage o)
        {
            if(sendtime<o.sendtime)return -1;
            if(sendtime>o.sendtime)return 1;
            return 0;
        }
        public void writePDU(BufferedOutputStream out,int command_sequence) throws Exception
        {
            SMPP_PDUHeader head=new SMPP_PDUHeader("SMPPWorker$SMSCOriginatedMessage("+myServerPort+"/"+getWorkerId()+")",command_id,0,command_sequence);
            head.writePDU(out,packet);
        }
    }
    
    private byte[] createDeliverSM(int ston, int snpi, String saddr, int dton, int dnpi, String daddr, int protocolid, String message_id) throws Exception
    {
        byte[]  service_type            =new byte[1];service_type[0]=0;
        
        byte[]  source_addr_ton         =ConvertLib.getByteArrayFromInt(ston,1);
        byte[]  source_addr_npi         =ConvertLib.getByteArrayFromInt(snpi,1);
        byte[]  source_address          =ConvertLib.getCOctetByteArrayFromString(saddr);

        byte[]  dest_addr_ton           =ConvertLib.getByteArrayFromInt(dton,1);
        byte[]  dest_addr_npi           =ConvertLib.getByteArrayFromInt(dnpi,1);
        byte[]  destination_addr        =ConvertLib.getCOctetByteArrayFromString(daddr);

        int     esmclass                =2;
        byte[]  esm_class               =ConvertLib.getByteArrayFromInt(esmclass,1);
        
        byte[]  protocol_id             =ConvertLib.getByteArrayFromInt(protocolid,1);

        int     priorityflag            =0;
        byte[]  priority_flag           =ConvertLib.getByteArrayFromInt(priorityflag,1);

        byte[]  schedule_delivery_time  =new byte[1];schedule_delivery_time[0]=0;
        byte[]  validity_period         =new byte[1];validity_period[0]=0;
        
        int     registereddelivery      =0;
        byte[]  registered_delivery     =ConvertLib.getByteArrayFromInt(registereddelivery,1);
        int     replace_ifpresent_flag  =0;
        byte[]  replace_if_present_flag =ConvertLib.getByteArrayFromInt(replace_ifpresent_flag,1);
        
        int     datacoding              =0;
        byte[]  data_coding             =ConvertLib.getByteArrayFromInt(datacoding,1);
        
        int     sm_default_msgid        =0;
        byte[]  sm_default_msg_id       =ConvertLib.getByteArrayFromInt(sm_default_msgid,1);
        
        byte[]  short_message           =ConvertLib.getOctetByteArrayFromString("Message for "+daddr+" with identification "+message_id+" has been delivered");
        byte[]  sm_length               =ConvertLib.getByteArrayFromInt(short_message.length,1);
        
        byte[] message_state_tlv        = createTLV("SMPPWorker$createDeliverSM("+myServerPort+"/"+getWorkerId()+")",""+OPT_MESSAGE_STATE,""+2,false);;
        //byte[] network_error_code_tlv   = createTLV("SMPPWorker$createDeliverSM("+myServerPort+"/"+getWorkerId()+")",""+OPT_NETWORK_ERROR_CODE,""+0,false);;
        
        byte[] receipted_message_id_tlv = createTLV("SMPPWorker$createDeliverSM("+myServerPort+"/"+getWorkerId()+")",""+OPT_RECEIPTED_MESSAGE_ID,ConvertLib.outputBytes(ConvertLib.createBytes(message_id))+"00",false);;

        byte[] packet=new byte[service_type.length
                          +source_addr_ton.length
                          +source_addr_npi.length
                          +source_address.length
                          +dest_addr_ton.length
                          +dest_addr_npi.length
                          +destination_addr.length
                          +esm_class.length
                          +protocol_id.length
                          +priority_flag.length
                          +schedule_delivery_time.length
                          +validity_period.length
                          +registered_delivery.length
                          +replace_if_present_flag.length
                          +data_coding.length
                          +sm_default_msg_id.length
                          +short_message.length
                          +sm_length.length
                          +message_state_tlv.length
                          +receipted_message_id_tlv.length
                          ];
        int pointer=0;
        pointer=ConvertLib.addBytesToArray(packet,pointer,service_type            );
        pointer=ConvertLib.addBytesToArray(packet,pointer,source_addr_ton         );
        pointer=ConvertLib.addBytesToArray(packet,pointer,source_addr_npi         );
        pointer=ConvertLib.addBytesToArray(packet,pointer,source_address          );
        pointer=ConvertLib.addBytesToArray(packet,pointer,dest_addr_ton           );
        pointer=ConvertLib.addBytesToArray(packet,pointer,dest_addr_npi           );
        pointer=ConvertLib.addBytesToArray(packet,pointer,destination_addr        );
        pointer=ConvertLib.addBytesToArray(packet,pointer,esm_class               );
        pointer=ConvertLib.addBytesToArray(packet,pointer,protocol_id             );
        pointer=ConvertLib.addBytesToArray(packet,pointer,priority_flag           );
        pointer=ConvertLib.addBytesToArray(packet,pointer,schedule_delivery_time  );
        pointer=ConvertLib.addBytesToArray(packet,pointer,validity_period         );
        pointer=ConvertLib.addBytesToArray(packet,pointer,registered_delivery     );
        pointer=ConvertLib.addBytesToArray(packet,pointer,replace_if_present_flag );
        pointer=ConvertLib.addBytesToArray(packet,pointer,data_coding             );
        pointer=ConvertLib.addBytesToArray(packet,pointer,sm_default_msg_id       );
        pointer=ConvertLib.addBytesToArray(packet,pointer,sm_length               );
        pointer=ConvertLib.addBytesToArray(packet,pointer,short_message           );
        pointer=ConvertLib.addBytesToArray(packet,pointer,message_state_tlv       );
        pointer=ConvertLib.addBytesToArray(packet,pointer,receipted_message_id_tlv);
        return packet;
    }

    public static byte[] createTLV(String funcname, String sparameter_tag, String svalue, boolean doOutput) throws Exception
    {
        int parameter_tag=-1;
        try
        {
            parameter_tag=Integer.decode(sparameter_tag);
        } catch (NumberFormatException nfe)
        {
            parameter_tag=getTAGID(sparameter_tag.toUpperCase().trim());
            if(parameter_tag==-1)
            {
                XTTProperties.printFail(funcname+": '"+sparameter_tag+"' not supported/found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                throw new IllegalArgumentException("'"+sparameter_tag+"' not supported/found");
            }
        }
        byte[] tag=ConvertLib.getByteArrayFromInt(parameter_tag,2);

        int maxlength=-1;
        int minlength=-1;

        byte[] value=null;

        String tag_name=null;

        switch(parameter_tag)
        {
            case OPT_DEST_ADDR_SUBUNIT:
                if(tag_name==null)tag_name="DEST_ADDR_SUBUNIT";
            case OPT_DEST_NETWORK_TYPE:
                if(tag_name==null)tag_name="DEST_NETWORK_TYPE";
            case OPT_DEST_BEARER_TYPE:
                if(tag_name==null)tag_name="DEST_BEARER_TYPE";
            case OPT_DEST_TELEMATICS_ID:
                if(tag_name==null)tag_name="DEST_TELEMATICS_ID";
            case OPT_SOURCE_ADDR_SUBUNIT:
                if(tag_name==null)tag_name="SOURCE_ADDR_SUBUNIT";
            case OPT_SOURCE_NETWORK_TYPE:
                if(tag_name==null)tag_name="SOURCE_NETWORK_TYPE";
            case OPT_SOURCE_BEARER_TYPE:
                if(tag_name==null)tag_name="SOURCE_BEARER_TYPE";
            case OPT_SOURCE_TELEMATICS_ID:
                if(tag_name==null)tag_name="SOURCE_TELEMATICS_ID";
            case OPT_QOS_TIME_TO_LIVE:
                if(tag_name==null)tag_name="QOS_TIME_TO_LIVE";
            case OPT_PAYLOAD_TYPE:
                if(tag_name==null)tag_name="PAYLOAD_TYPE";
            case OPT_MS_MSG_WAIT_FACILITIES:
                if(tag_name==null)tag_name="MS_MSG_WAIT_FACILITIES";
            case OPT_PRIVACY_INDICATOR:
                if(tag_name==null)tag_name="PRIVACY_INDICATOR";
            case OPT_USER_MESSAGE_REFERENCE:
                if(tag_name==null)tag_name="USER_MESSAGE_REFERENCE";
            case OPT_USER_RESPONSE_CODE:
                if(tag_name==null)tag_name="USER_RESPONSE_CODE";
            case OPT_SOURCE_PORT:
                if(tag_name==null)tag_name="SOURCE_PORT";
            case OPT_DESTINATION_PORT:
                if(tag_name==null)tag_name="DESTINATION_PORT";
            case OPT_SAR_MSG_REF_NUM:
                if(tag_name==null)tag_name="SAR_MSG_REF_NUM";
            case OPT_LANGUAGE_INDICATOR:
                if(tag_name==null)tag_name="LANGUAGE_INDICATOR";
            case OPT_SAR_TOTAL_SEGMENTS:
                if(tag_name==null)tag_name="SAR_TOTAL_SEGMENTS";
            case OPT_SAR_SEGMENT_SEQNUM:
                if(tag_name==null)tag_name="SAR_SEGMENT_SEQNUM";
            case OPT_SC_INTERFACE_VERSION:
                if(tag_name==null)tag_name="SC_INTERFACE_VERSION";
            case OPT_CALLBACK_NUM_PRES_IND:
                if(tag_name==null)tag_name="CALLBACK_NUM_PRES_IND";
            case OPT_NUMBER_OF_MESSAGES:
                if(tag_name==null)tag_name="NUMBER_OF_MESSAGES";
            case OPT_DPF_RESULT:
                if(tag_name==null)tag_name="DPF_RESULT";
            case OPT_SET_DPF:
                if(tag_name==null)tag_name="SET_DPF";
            case OPT_MS_AVAILABILITY_STATUS:
                if(tag_name==null)tag_name="MS_AVAILABILITY_STATUS";
            case OPT_DELIVERY_FAILURE_REASON:
                if(tag_name==null)tag_name="DELIVERY_FAILURE_REASON";
            case OPT_MORE_MESSAGES_TO_SEND:
                if(tag_name==null)tag_name="MORE_MESSAGES_TO_SEND";
            case OPT_MESSAGE_STATE:
                if(tag_name==null)tag_name="MESSAGE_STATE";
            case OPT_DISPLAY_TIME:
                if(tag_name==null)tag_name="DISPLAY_TIME";
            case OPT_SMS_SIGNAL:
                if(tag_name==null)tag_name="SMS_SIGNAL";
            case OPT_MS_VALIDITY:
                if(tag_name==null)tag_name="MS_VALIDITY";
            case OPT_ITS_REPLY_TYPE:
                if(tag_name==null)tag_name="ITS_REPLY_TYPE";
            case OPT_CONGESTION_STATE:
                if(tag_name==null)tag_name="CONGESTION_STATE";
            case OPT_BROADCAST_CHANNEL_INDICATOR:
                if(tag_name==null)tag_name="BROADCAST_CHANNEL_INDICATOR";
            case OPT_BROADCAST_MESSAGE_CLASS:
                if(tag_name==null)tag_name="BROADCAST_MESSAGE_CLASS";
            case OPT_BROADCAST_REP_NUM:
                if(tag_name==null)tag_name="BROADCAST_REP_NUM";
            case OPT_BROADCAST_ERROR_STATUS:
                if(tag_name==null)tag_name="BROADCAST_ERROR_STATUS";
            case OPT_BROADCAST_AREA_SUCCESS:
                if(tag_name==null)tag_name="BROADCAST_AREA_SUCCESS";
            case OPT_DEST_ADDR_NP_RESOLUTION:
                if(tag_name==null)tag_name="DEST_ADDR_NP_RESOLUTION";
            case OPT_DEST_ADDR_NP_COUNTRY:
                if(tag_name==null)tag_name="DEST_ADDR_NP_COUNTRY";
            case OPT_USSD_SERVICE_OP:
                if(tag_name==null)tag_name="USSD_SERVICE_OP";
            case OPT_ALERT_ON_MESSAGE_DELIVERY:
                if(tag_name==null)tag_name="ALERT_ON_MESSAGE_DELIVERY";
                try
                {
                    value=ConvertLib.getByteArrayFromInt(Integer.decode(svalue));
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            case OPT_SOURCE_SUBADDRESS:
                if(tag_name==null)tag_name="SOURCE_SUBADDRESS";
                if(maxlength==-1)maxlength=23;
                if(minlength==-1)minlength=2;
            case OPT_DEST_SUBADDRESS:
                if(tag_name==null)tag_name="DEST_SUBADDRESS";
                if(maxlength==-1)maxlength=23;
                if(minlength==-1)minlength=2;
            case OPT_CALLBACK_NUM_ATAG:
                if(tag_name==null)tag_name="CALLBACK_NUM_ATAG";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=0;
            case OPT_CALLBACK_NUM:
                if(tag_name==null)tag_name="CALLBACK_NUM";
                if(maxlength==-1)maxlength=19;
                if(minlength==-1)minlength=4;
            case OPT_NETWORK_ERROR_CODE:
                if(tag_name==null)tag_name="NETWORK_ERROR_CODE";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_MESSAGE_PAYLOAD:
                if(tag_name==null)tag_name="MESSAGE_PAYLOAD";
                if(maxlength==-1)maxlength=Integer.MAX_VALUE;
                if(minlength==-1)minlength=0;
            case OPT_ITS_SESSION_INFO:
                if(tag_name==null)tag_name="ITS_SESSION_INFO";
                if(maxlength==-1)maxlength=2;
                if(minlength==-1)minlength=2;
            case OPT_BROADCAST_CONTENT_TYPE:
                if(tag_name==null)tag_name="BROADCAST_CONTENT_TYPE";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_BROADCAST_CONTENT_TYPE_INFO:
                if(tag_name==null)tag_name="BROADCAST_CONTENT_TYPE_INFO";
                if(maxlength==-1)maxlength=255;
                if(minlength==-1)minlength=1;
            case OPT_BROADCAST_FREQUENCY_INTERVAL:
                if(tag_name==null)tag_name="BROADCAST_FREQUENCY_INTERVAL";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_BROADCAST_AREA_IDENTIFIER:
                if(tag_name==null)tag_name="BROADCAST_AREA_IDENTIFIER";
                if(maxlength==-1)maxlength=101;
                if(minlength==-1)minlength=0;
            case OPT_BROADCAST_SERVICE_GROUP:
                if(tag_name==null)tag_name="BROADCAST_SERVICE_GROUP";
                if(maxlength==-1)maxlength=255;
                if(minlength==-1)minlength=1;
            case OPT_BILLING_IDENTIFICATION:
                if(tag_name==null)tag_name="BILLING_IDENTIFICATION";
                if(maxlength==-1)maxlength=1024;
                if(minlength==-1)minlength=1;
            case OPT_SOURCE_NODE_ID:
                if(tag_name==null)tag_name="SOURCE_NODE_ID";
                if(maxlength==-1)maxlength=6;
                if(minlength==-1)minlength=6;
            case OPT_DEST_NODE_ID:
                if(tag_name==null)tag_name="DEST_NODE_ID";
                if(maxlength==-1)maxlength=6;
                if(minlength==-1)minlength=6;
            case OPT_DEST_ADDR_NP_INFORMATION:
                if(tag_name==null)tag_name="DEST_ADDR_NP_INFORMATION";
                if(maxlength==-1)maxlength=10;
                if(minlength==-1)minlength=10;
                try
                {

                    value=ConvertLib.getBytesFromByteString(svalue);
                    if(value.length>maxlength||value.length<minlength)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" length "+value.length+" not between "+minlength+" and "+maxlength);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException("length "+value.length+" not between "+minlength+" and "+maxlength);
                    }
                } catch (IllegalArgumentException iex)
                {
                    throw iex;
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            case OPT_ADDITIONAL_STATUS_INFO_TEXT:
                if(tag_name==null)tag_name="ADDITIONAL_STATUS_INFO_TEXT";
                if(maxlength==-1)maxlength=256;
                if(minlength==-1)minlength=1;
            case OPT_RECEIPTED_MESSAGE_ID:
                if(tag_name==null)tag_name="RECEIPTED_MESSAGE_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=1;
            case OPT_BROADCAST_END_TIME:
                if(tag_name==null)tag_name="BROADCAST_END_TIME";
                if(maxlength==-1)maxlength=16;
                if(minlength==-1)minlength=16;
            case OPT_SOURCE_NETWORK_ID:
                if(tag_name==null)tag_name="SOURCE_NETWORK_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=7;
            case OPT_DEST_NETWORK_ID:
                if(tag_name==null)tag_name="DEST_NETWORK_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=7;
                try
                {

                    value=ConvertLib.getBytesFromByteString(svalue);
                    if(value.length>maxlength||value.length<minlength)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" length "+value.length+" not between "+minlength+" and "+maxlength);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException("length "+value.length+" not between "+minlength+" and "+maxlength);
                    }
                    if(value[value.length-1]!=0x00)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" musst be 0x00 terminated");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException(tag_name+" musst be 0x00 terminated");
                    }
                } catch (IllegalArgumentException iex)
                {
                    throw iex;
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            default:
                XTTProperties.printFail(funcname+": Unknown tag value "+parameter_tag);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                throw new IllegalArgumentException("Unknown tag value "+parameter_tag);
        }
        byte[] length=ConvertLib.getByteArrayFromInt(value.length,2);
        byte[] returnvalue=new byte[tag.length+length.length+value.length];
        int pointer=0;
        pointer=ConvertLib.addBytesToArray(returnvalue,pointer,tag   );
        pointer=ConvertLib.addBytesToArray(returnvalue,pointer,length);
        pointer=ConvertLib.addBytesToArray(returnvalue,pointer,value );
        if(doOutput)
        {
            XTTProperties.printInfo(funcname+": created TLV->"+tag_name);
        }
        return returnvalue;
    }

    public static int getTAGID(String stag)
    {
        if(stag.equals("DEST_ADDR_SUBUNIT"))            return OPT_DEST_ADDR_SUBUNIT           ;
        if(stag.equals("DEST_NETWORK_TYPE"))            return OPT_DEST_NETWORK_TYPE           ;
        if(stag.equals("DEST_BEARER_TYPE"))             return OPT_DEST_BEARER_TYPE            ;
        if(stag.equals("DEST_TELEMATICS_ID"))           return OPT_DEST_TELEMATICS_ID          ;
        if(stag.equals("SOURCE_ADDR_SUBUNIT"))          return OPT_SOURCE_ADDR_SUBUNIT         ;
        if(stag.equals("SOURCE_NETWORK_TYPE"))          return OPT_SOURCE_NETWORK_TYPE         ;
        if(stag.equals("SOURCE_BEARER_TYPE"))           return OPT_SOURCE_BEARER_TYPE          ;
        if(stag.equals("SOURCE_TELEMATICS_ID"))         return OPT_SOURCE_TELEMATICS_ID        ;
        if(stag.equals("QOS_TIME_TO_LIVE"))             return OPT_QOS_TIME_TO_LIVE            ;
        if(stag.equals("PAYLOAD_TYPE"))                 return OPT_PAYLOAD_TYPE                ;
        if(stag.equals("ADDITIONAL_STATUS_INFO_TEXT"))  return OPT_ADDITIONAL_STATUS_INFO_TEXT ;
        if(stag.equals("RECEIPTED_MESSAGE_ID"))         return OPT_RECEIPTED_MESSAGE_ID        ;
        if(stag.equals("MS_MSG_WAIT_FACILITIES"))       return OPT_MS_MSG_WAIT_FACILITIES      ;
        if(stag.equals("PRIVACY_INDICATOR"))            return OPT_PRIVACY_INDICATOR           ;
        if(stag.equals("SOURCE_SUBADDRESS"))            return OPT_SOURCE_SUBADDRESS           ;
        if(stag.equals("DEST_SUBADDRESS"))              return OPT_DEST_SUBADDRESS             ;
        if(stag.equals("USER_MESSAGE_REFERENCE"))       return OPT_USER_MESSAGE_REFERENCE      ;
        if(stag.equals("USER_RESPONSE_CODE"))           return OPT_USER_RESPONSE_CODE          ;
        if(stag.equals("SOURCE_PORT"))                  return OPT_SOURCE_PORT                 ;
        if(stag.equals("DESTINATION_PORT"))             return OPT_DESTINATION_PORT            ;
        if(stag.equals("SAR_MSG_REF_NUM"))              return OPT_SAR_MSG_REF_NUM             ;
        if(stag.equals("LANGUAGE_INDICATOR"))           return OPT_LANGUAGE_INDICATOR          ;
        if(stag.equals("SAR_TOTAL_SEGMENTS"))           return OPT_SAR_TOTAL_SEGMENTS          ;
        if(stag.equals("SAR_SEGMENT_SEQNUM"))           return OPT_SAR_SEGMENT_SEQNUM          ;
        if(stag.equals("SC_INTERFACE_VERSION"))         return OPT_SC_INTERFACE_VERSION        ;
        if(stag.equals("CALLBACK_NUM_PRES_IND"))        return OPT_CALLBACK_NUM_PRES_IND       ;
        if(stag.equals("CALLBACK_NUM_ATAG"))            return OPT_CALLBACK_NUM_ATAG           ;
        if(stag.equals("NUMBER_OF_MESSAGES"))           return OPT_NUMBER_OF_MESSAGES          ;
        if(stag.equals("CALLBACK_NUM"))                 return OPT_CALLBACK_NUM                ;
        if(stag.equals("DPF_RESULT"))                   return OPT_DPF_RESULT                  ;
        if(stag.equals("SET_DPF"))                      return OPT_SET_DPF                     ;
        if(stag.equals("MS_AVAILABILITY_STATUS"))       return OPT_MS_AVAILABILITY_STATUS      ;
        if(stag.equals("NETWORK_ERROR_CODE"))           return OPT_NETWORK_ERROR_CODE          ;
        if(stag.equals("MESSAGE_PAYLOAD"))              return OPT_MESSAGE_PAYLOAD             ;
        if(stag.equals("DELIVERY_FAILURE_REASON"))      return OPT_DELIVERY_FAILURE_REASON     ;
        if(stag.equals("MORE_MESSAGES_TO_SEND"))        return OPT_MORE_MESSAGES_TO_SEND       ;
        if(stag.equals("MESSAGE_STATE"))                return OPT_MESSAGE_STATE               ;
        if(stag.equals("USSD_SERVICE_OP"))              return OPT_USSD_SERVICE_OP             ;
        if(stag.equals("DISPLAY_TIME"))                 return OPT_DISPLAY_TIME                ;
        if(stag.equals("SMS_SIGNAL"))                   return OPT_SMS_SIGNAL                  ;
        if(stag.equals("MS_VALIDITY"))                  return OPT_MS_VALIDITY                 ;
        if(stag.equals("ALERT_ON_MESSAGE_DELIVERY"))    return OPT_ALERT_ON_MESSAGE_DELIVERY   ;
        if(stag.equals("ITS_REPLY_TYPE"))               return OPT_ITS_REPLY_TYPE              ;
        if(stag.equals("ITS_SESSION_INFO"))             return OPT_ITS_SESSION_INFO            ;
        if(stag.equals("CONGESTION_STATE"))             return OPT_CONGESTION_STATE            ;
        if(stag.equals("BROADCAST_CHANNEL_INDICATOR"))  return OPT_BROADCAST_CHANNEL_INDICATOR ;
        if(stag.equals("BROADCAST_CONTENT_TYPE"))       return OPT_BROADCAST_CONTENT_TYPE      ;
        if(stag.equals("BROADCAST_CONTENT_TYPE_INFO"))  return OPT_BROADCAST_CONTENT_TYPE_INFO ;
        if(stag.equals("BROADCAST_MESSAGE_CLASS"))      return OPT_BROADCAST_MESSAGE_CLASS     ;
        if(stag.equals("BROADCAST_REP_NUM"))            return OPT_BROADCAST_REP_NUM           ;
        if(stag.equals("BROADCAST_FREQUENCY_INTERVAL")) return OPT_BROADCAST_FREQUENCY_INTERVAL;
        if(stag.equals("BROADCAST_AREA_IDENTIFIER"))    return OPT_BROADCAST_AREA_IDENTIFIER   ;
        if(stag.equals("BROADCAST_ERROR_STATUS"))       return OPT_BROADCAST_ERROR_STATUS      ;
        if(stag.equals("BROADCAST_AREA_SUCCESS"))       return OPT_BROADCAST_AREA_SUCCESS      ;
        if(stag.equals("BROADCAST_END_TIME"))           return OPT_BROADCAST_END_TIME          ;
        if(stag.equals("BROADCAST_SERVICE_GROUP"))      return OPT_BROADCAST_SERVICE_GROUP     ;
        if(stag.equals("BILLING_IDENTIFICATION"))       return OPT_BILLING_IDENTIFICATION      ;
        if(stag.equals("SOURCE_NETWORK_ID"))            return OPT_SOURCE_NETWORK_ID           ;
        if(stag.equals("DEST_NETWORK_ID"))              return OPT_DEST_NETWORK_ID             ;
        if(stag.equals("SOURCE_NODE_ID"))               return OPT_SOURCE_NODE_ID              ;
        if(stag.equals("DEST_NODE_ID"))                 return OPT_DEST_NODE_ID                ;
        if(stag.equals("DEST_ADDR_NP_RESOLUTION"))      return OPT_DEST_ADDR_NP_RESOLUTION     ;
        if(stag.equals("DEST_ADDR_NP_INFORMATION"))     return OPT_DEST_ADDR_NP_INFORMATION    ;
        if(stag.equals("DEST_ADDR_NP_COUNTRY"))         return OPT_DEST_ADDR_NP_COUNTRY        ;
        return -1;
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
                XTTProperties.printFail("SMPPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            if(connected.isEmpty())
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for any SMPP Bind on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(connected.isEmpty())
                    {
                        XTTProperties.printFail("SMPPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for any SMPP Bind on SMSC no timeout");
                    conkey.wait();
                }
            } else
            {
                XTTProperties.printInfo("waitForBind: already connected");
            }

        }
    }

    /**
     * Wait on PDU_BIND from specific systemid
     */
    public static void waitForBind(String systemid) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(conkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("SMPPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connected.contains(systemid))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for SMPP Bind with systemid='"+systemid+"' on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(!connected.contains(systemid))
                    {
                        XTTProperties.printFail("SMPPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for SMPP Bind with systemid='"+systemid+"' on SMSC no timeout");
                    conkey.wait();
                }
            }
        }
    }

    /**
     * Wait on PDU_BIND from specific systemid
     */
    public static void waitForBind(InetAddress remoteAddress) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(conkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("SMPPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!connectedIp.contains(remoteAddress.toString()))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForBind: waiting for SMPP Bind with remoteAddress='"+remoteAddress+"' on SMSC timeout="+wait);
                    conkey.wait(wait);
                    if(!connectedIp.contains(remoteAddress.toString()))
                    {
                        XTTProperties.printFail("SMPPWorker.waitForBind: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    } else
                    {
                        XTTProperties.printDebug("waitForBind: SMPP connection with remoteAddress='"+remoteAddress+"' found!");
                    }
                } else
                {
                    XTTProperties.printInfo("waitForBind: waiting for SMPP Bind with remoteAddress='"+remoteAddress+"' on SMSC no timeout");
                    conkey.wait();
                }
            }
        }
    }
    private static boolean checkConnection()
    {
        synchronized(conkey)
        {
            return !connected.isEmpty();
        }
    }
    private static boolean checkConnection(String systemid)
    {
        synchronized(conkey)
        {
            return connected.contains(systemid);
        }
    }

    public static void waitForBinds(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("SMPPWorker.waitForBinds: no instance running!");
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
                XTTProperties.printInfo("SMPPWorker.waitForBinds: "+concount+"/"+number);
                if(wait>0)
                {
                    prevcount=concount;
                    conkey.wait(wait);
                    if(concount==prevcount)
                    {
                        XTTProperties.printFail("SMPPWorker.waitForBinds: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    conkey.wait();
                }
            }
            XTTProperties.printInfo("SMPPWorker.waitForBinds: "+concount+"/"+number);
        }
    }


    /**
     * Wait on MESSAGE from specific systemid
     */
    public static void waitForMessage(String systemid) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("SMSCSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMSCServer.DEFAULTTIMEOUT;
        synchronized(msgkey)
        {
            if(SMSCServer.checkSockets())
            {
                XTTProperties.printFail("SMPPWorker.waitForBind: no instance running!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            while(!checkConnection(systemid))
            {
                if(wait>0)
                {
                    XTTProperties.printInfo("waitForMessage: waiting for SMPP Message with systemid='"+systemid+"' on SMSC timeout:"+wait);
                    msgkey.wait(wait);
                } else
                {
                    XTTProperties.printInfo("waitForMessage: waiting for SMPP Message with systemid='"+systemid+"' on SMSC no timeout");
                    msgkey.wait();
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
            XTTProperties.printFail("SMPPWorker.waitForMessages: no instance running!");
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
                if(!checkConnection())
                {
                    XTTProperties.printFail("SMPPWorker.waitForMessages: not connected!");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                XTTProperties.printInfo("SMPPWorker.waitForMessages: "+msgcount+"/"+number);
                if(wait>0)
                {
                    prevcount=msgcount;
                    msgkey.wait(wait);
                    if(msgcount==prevcount)
                    {
                        XTTProperties.printFail("SMPPWorker.waitForMessages: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    msgkey.wait();
                }
            }
            XTTProperties.printInfo("SMPPWorker.waitForMessages: "+msgcount+"/"+number);
        }
    }
    public static void waitForTimeoutMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("SMPPWorker.waitForTimeoutMessages: no instance running!");
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
                        XTTProperties.printFail("SMPPWorker.waitForTimeoutMessages: not connected!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                XTTProperties.printInfo("SMPPWorker.waitForTimeoutMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=msgcount;
                msgkey.wait(wait);
                if(msgcount==prevcount)
                {
                    XTTProperties.printInfo("SMPPWorker.waitForTimeoutMessages: timed out with no messages!");
                    return;
                }
            }
            XTTProperties.printFail("SMPPWorker.waitForTimeoutMessages: Message received! "+wspcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    /**
     * Wait for a number of wsp messages
     */
    public static void waitForWSPMessages(int number) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("SMPPWorker.waitForWSPMessages: no instance running!");
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
                if(!checkConnection())
                {
                    XTTProperties.printFail("SMPPWorker.waitForWSPMessages: not connected!");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                XTTProperties.printInfo("SMPPWorker.waitForWSPMessages: "+wspcount+"/"+number);
                if(wait>0)
                {
                    prevcount=wspcount;
                    wspkey.wait(wait);
                    if(wspcount==prevcount)
                    {
                        XTTProperties.printFail("SMPPWorker.waitForWSPMessages: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    wspkey.wait();
                }
            }
            XTTProperties.printInfo("SMPPWorker.waitForWSPMessages: "+wspcount+"/"+number);
        }
    }

    /**
     * Wait for a number of wsp messages
     */
    public static void waitForTimeoutWSPMessages(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SMSCServer.checkSockets())
        {
            XTTProperties.printFail("SMPPWorker.waitForTimeoutWSPMessages: no instance running!");
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
                if(!checkConnection())
                {
                    XTTProperties.printFail("SMPPWorker.waitForTimeoutWSPMessages: not connected!");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                XTTProperties.printInfo("SMPPWorker.waitForTimeoutWSPMessages: "+wspcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=wspcount;
                wspkey.wait(wait);
                if(wspcount==prevcount)
                {
                    XTTProperties.printInfo("SMPPWorker.waitForTimeoutWSPMessages: timed out with no WSP messages!");
                    return;
                }
            }
            XTTProperties.printFail("SMPPWorker.waitForTimeoutWSPMessages: WSP message received! "+wspcount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static Socket getSocket(String workerId)
    {
        //System.out.println(workerId+"\n"+workerMap);
        SMPPWorker worker=(SMPPWorker)workerMap.get(workerId.trim());
        if(worker==null)
        {
            return null;
        } else
        {
            return worker.getSocket();
        }
    }
    public Socket getSocket()
    {
        return socket;
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

    public static void addResponseTLV(int cmdID, byte[] tlv)
    {
        ByteArrayWrapper oldtlvs=tlvMap.get(cmdID+"");
        byte[] oldtlv=new byte[0];
        if(oldtlvs!=null)
        {
            if(oldtlvs.getArray()!=null)
            {
                oldtlv=oldtlvs.getArray();
            }
        }
        int pointer=0;
        byte[] newtlvs=new byte[oldtlv.length+tlv.length];
        pointer=ConvertLib.addBytesToArray(newtlvs,pointer,oldtlv);
        pointer=ConvertLib.addBytesToArray(newtlvs,pointer,tlv);

        tlvMap.put(cmdID+"",new ByteArrayWrapper(newtlvs));
    }
    public static void setOverrideResponse(int cmdID, byte[] resp)
    {
        if(resp!=null)
        {
            overrideResponseMap.put(cmdID+"",new ByteArrayWrapper(resp));
        } else
        {
            overrideResponseMap.remove(cmdID+"");
        }
    }

    public static void clearOverrideResponse()
    {
        overrideResponseMap.clear();
    }
    public static void clearResponseTLV()
    {
        tlvMap.clear();
    }
    public static void clearResponseTLV(int cmdID)
    {
        tlvMap.remove(cmdID+"");
    }
    public static void setResponseStatus(int cmdID, int status)
    {
        Integer cmdR=new Integer(status);
        cmdStatusOverride.put(cmdID+"",cmdR);
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
        tlvMap.clear();
        cmdStatusOverride.clear();
        nextMessageDelay=0;
        overridePattern=null;
    }

    public static final String tantau_sccsid = "@(#)$Id: SMPPWorker.java,v 1.28 2011/01/28 06:05:10 rajesh Exp $";
}
