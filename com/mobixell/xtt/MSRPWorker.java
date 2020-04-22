
package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * <p>MSRPWorker</p>
 * <p>Processes a single MSRP request which has been received by the MSRPServer</p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: 724 Solutions Inc</p>
 *
 * Stores headers as<code><br>
 *  Response:<br>
 *  MSRP/[serverport]/RESPONSE/[transactionid]<br>
 *  MSRP/[serverport]/RESPONSE/[messageiid]<br>
 *  MSRP/[serverport]/RESPONSE<br>
 *  MSRP/RESPONSE/[transactionid]<br>
 *  MSRP/RESPONSE/[messageiid]<br>
 *  MSRP/RESPONSE<br>
 *  and:<br>
 *  /HEADER/[headername]<br>
 *  /CODE<br>
 *  /MESSAGE<br>
 *  /TRANSACTIONID
 *  <br>
 *  Request:<br>
 *  MSRP/[serverport]/[METHOD]/[transactionid]<br>
 *  MSRP/[serverport]/[METHOD]/[messageiid]<br>
 *  MSRP/[serverport]/[METHOD]<br>
 *  MSRP/[METHOD]/[transactionid]<br>
 *  MSRP/[METHOD]/[messageiid]<br>
 *  MSRP/[METHOD]<br>
 *  MSRP/REQUEST
 *  and:<br>
 *  /HEADER/[headername]<br>
 *  /BODY/BASE64<br>
 *  /BODY/PLAIN<br>
 *  /FIRSTLINE<br>
 *  /METHOD<br>
 *  /TRANSACTIONID<br>
 * <br>The Variable ending in /FINISHED/REQUEST is set to true after sending the response/finishing the response handling.
 *  </code><br>
 *
 *
 * @author Anil Wadhai
 * @version $Id: MSRPWorker.java,v 1.6 2009/08/07 13:50:42 awadhai Exp $
 */

public class MSRPWorker extends Thread implements MSRPConstants
{
    private Map<String, DataHandler>                     dataHandlers            = Collections.synchronizedMap(new HashMap<String, DataHandler>());
    private static LinkedHashMap<String, Vector<String>> receivedServerHeader    = new LinkedHashMap<String, Vector<String>>();
    private static LinkedHashMap<String,Vector<String>>  receivedHeader          = new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String, Vector<String>> sendServerHeader        = new LinkedHashMap<String, Vector<String>>();
    private LinkedHashMap<String, Vector<String>>        headersToSend           = new LinkedHashMap<String, Vector<String>>(); // Not
    private LinkedHashMap<String, Vector<String>>        myReceivedHeader        = null;
    private static final int                             MINLAGDELAY             = 100;
    private int                                          networklagdelay         = 0;
    private String[]                                     finishedNotifcation     = new String[0];
    private int                                          id;
    private boolean                                      stop                    = false;
    private static int                                   instances               = 0;
    private static int                                   overrideReturnCode      = 0;
    private static int                                   responsecount           = 0;
    private static int                                   requestcount            = 0;
    private static int                                   responseCode            = 0;
    private static String                                recievedURL             = "null";
    private static String                                overrideReturnMessage   = null;
    private static boolean                               skipResponse            = false;
    private static Object                                requestkey              = new Object();
    private static Object                                responsekey             = new Object();
    private static Object                                key                     = new Object();
    private static final byte[]                          CRLF                    = { (byte) '\r', (byte) '\n' };
    
    /*
     * Socket to client we're handling, which will be set by the MSRPServer when
     * dispatching the request to us
     */
    private Socket                                       tcpSocket               = null;
    private MSRPServer                                   myServer                = null;
    private int                                          myTimeout               = 600000;
    private int                                          myServerPort            = -1;
    private int                                          returnCode              = 500;
    private boolean                                      keep_alive              = true;
    private String                                       myIPAddress             = null;
    private String                                       firstCommand            = null;
    private String                                       myreceivedTransactionID = null;
    private String                                       messageID               = null;
    private byte                                         lastByte                = ENDMESSAGE;


    public void setTcpSocket(Socket socket){
        this.tcpSocket = socket;
    }

    /**
     * Creates a new MSRPWorker
     *
     * @param id ID number of this worker thread
     */
    public MSRPWorker(int id, Socket setSocket, MSRPServer sserver, int timeout, int serverPort)
    {
        this(id, setSocket, sserver, timeout, serverPort, true);
    }

    public MSRPWorker(int id, Socket setSocket, MSRPServer sserver, int timeout, int serverPort, boolean keep)
    {
        this.tcpSocket       = setSocket;
        this.id              = id;
        this.myTimeout       = timeout;
        this.myServer        = sserver;
        this.keep_alive      = keep;
        this.myServerPort    = serverPort;
        this.myIPAddress     = tcpSocket.getLocalAddress().getHostAddress();
    }

    public static LinkedHashMap<String, Vector<String>> getServerHeader()
    {
        LinkedHashMap<String, Vector<String>> returnMap = null;
        synchronized (receivedServerHeader)
        {
            returnMap = new LinkedHashMap<String, Vector<String>>();
            returnMap.putAll(receivedServerHeader);
        }
        return returnMap;
    }
    
    public static LinkedHashMap<String, Vector<String>> getHeader()
    {
        LinkedHashMap<String, Vector<String>> returnMap = null;
        synchronized (receivedHeader)
        {
            returnMap = new LinkedHashMap<String, Vector<String>>();
            returnMap.putAll(receivedHeader);
        }
        return returnMap;
    }

    public static LinkedHashMap<String, Vector<String>> getServerSendHeader()
    {
        return sendServerHeader;
    }
    
    public static int getResponseCode()
    { 
        return responseCode;
    }
    
    public void setResponseCode(int returnCode)
    {
        this.responseCode = returnCode;
    }


    public static void setServerSendHeader(LinkedHashMap<String, Vector<String>> sendserverHeader)
    {
        sendServerHeader = sendserverHeader;
    }

    public static void setOverrideReturnCode(int code)
    {
        overrideReturnCode = code;
    }

    public static void setOverrideReturnMessage(String msg)
    {
        overrideReturnMessage = msg;
    }

    public static String getServerRecievedURL()
    {
        return recievedURL;
    }

    public static void init()
    {
        
        synchronized (requestkey)
        {
            requestcount = 0;
        }
        synchronized (responsekey)
        {
            responsecount = 0;
        }

        overrideReturnCode = 0;
        overrideReturnMessage = null;
        skipResponse = false;
    }

    public int getWorkerId()
    {
        return id;
    }

    public int getMyServerPort()
    {
        return myServerPort;
    }

    public void setMyServerPort(int port)
    {
        myServerPort = port;
    }

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        XTTProperties.printVerbose("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        this.stop = true;
    }

    public synchronized void doStop()
    {
        this.stop = true;
        XTTProperties.printVerbose("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): doing stop");
        try
        {
            this.tcpSocket.close();
        } catch (Exception ex)
        {
        }
        // Notify anyone waiting on this object
        notifyAll();
    }
    
    

    /**
     * Start the worker thread
     */
    public void run()
    {
        try
        {
            handleTCPClient();
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(se);
            }
        } catch (java.net.SocketException soe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(soe);
                }
            }
        } catch (java.net.SocketTimeoutException ste)
        {
            if(keep_alive)
            {
                XTTProperties.printDebug("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ste);
                }
            }
        } catch (java.io.IOException ioe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    /**
     * Handles the TCP request
     *
     * @throws IOException
     */
    public void handleTCPClient() throws IOException
    {
        
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by "+id+" instance "+instances);
            key.notify();
        }
        BufferedInputStream inputStream = new BufferedInputStream(tcpSocket.getInputStream(), 65536);
        BufferedOutputStream outputStream = new BufferedOutputStream(tcpSocket.getOutputStream());
        tcpSocket.setSoTimeout(myTimeout);
        // the Nagle algorithm is used to automatically concatenate a number of
        // small buffer messages (see RC896, RFC1122)
        tcpSocket.setTcpNoDelay(true);
        
        networklagdelay=XTTProperties.getNetworkLagDelay();
        if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;
        
        try
        {
            KEEPALIVE: do
            {
                XTTProperties.printDebug("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): receiving from "+tcpSocket.getRemoteSocketAddress());
                headersToSend                 = new LinkedHashMap<String, Vector<String>>();
                String[] firstLine            = null;
                String returnMessage          = null;
                String methodName             = "";
    
                String fline=HTTPHelper.readCRLFLine(inputStream);
                if(fline==null)
                {
                    returnCode = 0;
                    break;
                }
                // get first line i.e. MSRP tID 200 OK or MSRP tID SEND/REPORT
                firstLine               = fline.split("\\s+", 4);
                firstCommand            = firstLine[0];
                myreceivedTransactionID = firstLine[1];
                methodName              = firstLine[2];
    
                // read header at server side only to construct response & at client side read the response.
                StringBuffer plainHeaderOutput=new StringBuffer("MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")"+": Received Header START:\n");
                StringBuffer decodedHeaderOutput=new StringBuffer("");
                try
                {
                    XTTProperties.printDebug("MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")"+": reading Headers");
                    myReceivedHeader=new LinkedHashMap<String,Vector<String>>();
                    HTTPHelper.readHeaders(inputStream,myReceivedHeader,plainHeaderOutput,decodedHeaderOutput,"^$|"+SEVENHYPHEN+myreceivedTransactionID+"\\$");
                } finally
                {
                    XTTProperties.printDebug(plainHeaderOutput.toString());
                    XTTProperties.printDebug("MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")"+": Decoding:"+decodedHeaderOutput);
                }

                messageID = null;
                if(myReceivedHeader.containsKey(MESSAGEID.toLowerCase()))
                {
                    messageID           = myReceivedHeader.get(MESSAGEID.toLowerCase()).get(0);    
                }

                // if first line does not contain SEND or REPORT means it is response.
                if(!methodName.equals(SEND)&&!methodName.equals(REPORT))
                {
                    finishedNotifcation = new String[] { "MSRP/"+myServerPort+"/RESPONSE/"+myreceivedTransactionID,
                    								     "MSRP/"+myServerPort+"/RESPONSE/"+messageID,
                    									 "MSRP/"+myServerPort+"/RESPONSE",
                    									 "MSRP/RESPONSE/"+myreceivedTransactionID,
                    									 "MSRP/RESPONSE/"+messageID,
                    									 "MSRP/RESPONSE"};
                    // Store the header
                    storeHeaders(finishedNotifcation, "/HEADER", myReceivedHeader);
                    setMultipleVariables(finishedNotifcation, "/CODE", firstLine[2]);
                    setMultipleVariables(finishedNotifcation, "/MESSAGE", firstLine[3]);
                    setMultipleVariables(finishedNotifcation, "/TRANSACTIONID", myreceivedTransactionID); 
                    setMultipleVariables(finishedNotifcation, "/FINISHED/REQUEST", "true");
                    synchronized (receivedHeader)
                    {
                        receivedHeader.clear();
                        receivedHeader.putAll(myReceivedHeader);
                    }

                    synchronized (responsekey)
                    {
                        responsecount++;
                        responsekey.notifyAll();
                    }
                    continue KEEPALIVE;
                }

                if(skipResponse)
                {
                    // Do not handle response generation when you're not sending it anyway
                    XTTProperties.printTransaction("MSRPWORKER/"+XTTProperties.DELIMITER+firstCommand+XTTProperties.DELIMITER+methodName+XTTProperties.DELIMITER+myreceivedTransactionID);
                    continue KEEPALIVE;
                }

                synchronized (receivedServerHeader)
                {
                    receivedServerHeader.clear();
                    receivedServerHeader.putAll(myReceivedHeader);
                }
                
                finishedNotifcation = new String[] { "MSRP/"+myServerPort+"/"+methodName+"/"+myreceivedTransactionID,
                                                     "MSRP/"+myServerPort+"/"+methodName+"/"+messageID,
                                                     "MSRP/"+myServerPort+"/"+methodName, 
                                                     "MSRP/"+methodName+"/"+myreceivedTransactionID,
                                                     "MSRP/"+methodName+"/"+messageID,
                                                     "MSRP/"+methodName,
                                                     "MSRP/REQUEST"};
                
                setMultipleVariables(finishedNotifcation, "/FIRSTLINE", fline); // firstLine received in request
                setMultipleVariables(finishedNotifcation, "/METHOD", methodName); // methodName received in request
                setMultipleVariables(finishedNotifcation, "/TRANSACTIONID", myreceivedTransactionID); // transactionID received in request
              
                if(messageID != null && dataHandlers.containsKey(messageID))
                {
                    DataHandler dataHandler = dataHandlers.get(messageID);
                    dataHandler.handleData( inputStream,  methodName);
                    
                } else if(messageID != null)
                {
                    DataHandler dataHandler = new DataHandler();
                    dataHandlers.put(messageID, dataHandler);
                    dataHandler.handleData( inputStream,  methodName);
                }else
                {
                    byte[]   receivedData = new byte[0];
                    if(inputStream.available()>0)
                    {
                        receivedData = HTTPHelper.readStream("MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")", inputStream, networklagdelay);       
                    }
                 
                    storeHeaders(finishedNotifcation, "/HEADER", myReceivedHeader);
                    String bodyPlain  = ConvertLib.createString(receivedData);
                    String bodyBase64 = ConvertLib.base64Encode(receivedData);
                    setMultipleVariables(finishedNotifcation, "/BODY/BASE64", bodyPlain);
                    setMultipleVariables(finishedNotifcation, "/BODY/PLAIN", bodyBase64);
                    returnCode = MSRP_BAD_REQUEST;
                }

                headersToSend.put(null, new Vector<String>());
                headersToSend.put(TOPATH, myReceivedHeader.get(FROMPATH.toLowerCase()));
                headersToSend.put(FROMPATH, myReceivedHeader.get(TOPATH.toLowerCase()));
                headersToSend.put(BYTERANGE, myReceivedHeader.get(BYTERANGE.toLowerCase()));
                if(messageID!=null)
                {
                    headersToSend.put(MESSAGEID, myReceivedHeader.get(MESSAGEID.toLowerCase()));    
                }
                // if request is type REPORT check for status header & response code in status header value.    
                String statusReportValue = "";
                boolean isStatus = myReceivedHeader.containsKey(STATUS.toLowerCase());
                if(isStatus)
                {
                    statusReportValue = myReceivedHeader.get(STATUS.toLowerCase()).get(0);
                    if(statusReportValue!=null)
                    {
                        statusReportValue = statusReportValue.split("\\s+", 3)[1];
                        returnCode        = Integer.parseInt(statusReportValue);
                    } else
                    {
                        returnCode        = MSRP_BAD_REQUEST;
                    }

                } else if(methodName.equals(REPORT))
                {
                    returnCode = MSRP_BAD_REQUEST;
                }

                Vector<String> v = new Vector<String>();
                if(overrideReturnCode>0)returnCode = overrideReturnCode;
                returnMessage = " "+getResponseMessage(returnCode);
                if(overrideReturnMessage!=null)returnMessage = overrideReturnMessage;
                v.add(MSRP+WHITESPACE+myreceivedTransactionID+WHITESPACE+returnCode+returnMessage);
                headersToSend.put(null, v);
                headersToSend.putAll(sendServerHeader);

                synchronized (tcpSocket)
                {
                    String debug = printDeepHeaders("TCP", headersToSend, outputStream);
                    XTTProperties.printDebug("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): TCP sent Headers\n"+debug);
                    outputStream.flush();
                }

              
                    synchronized (requestkey)
                    {
                        requestcount++;
                        requestkey.notifyAll();
                    }
                    setMultipleVariables(finishedNotifcation, "/FINISHED/REQUEST", "true");
                    continue KEEPALIVE;
                
            } while (keep_alive);
        } finally
        {
            if(keep_alive)
            {
                tcpSocket.close();
                XTTProperties.printVerbose("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
            }
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    /**
     * @param mode
     * @param header
     * @param out
     * @return string
     * @throws java.io.IOException
     */
    public String printDeepHeaders(String mode, LinkedHashMap<String, Vector<String>> header, BufferedOutputStream out) throws java.io.IOException
    {
        StringBuffer debug            = new StringBuffer();
        Iterator<String> it           = header.keySet().iterator();
        Iterator<String> vals         = null;
        String headerKey              = null;
        Vector<String> headerValues   = null;
        String headerValue            = null;
        while (it.hasNext())
        {
            headerKey = it.next();
            headerValues = header.get(headerKey);

            if(headerKey!=null)
            {
                vals = headerValues.iterator();
                while (vals.hasNext())
                {
                    out.write(ConvertLib.createBytes(headerKey+":"));
                    debug.append(headerKey+":");
                    headerValue = vals.next();
                    out.write(ConvertLib.createBytes(" "+headerValue));
                    out.write(CRLF);
                    debug.append(" "+headerValue+LINESEPARATOR);
                }
            } else
            {
                vals = headerValues.iterator();
                while (vals.hasNext())
                {
                    headerValue = vals.next();
                    out.write(ConvertLib.createBytes(headerValue));
                    out.write(CRLF);
                    debug.append(headerValue+LINESEPARATOR);
                }
            }
        }
        out.write(ConvertLib.createBytes(SEVENHYPHEN+myreceivedTransactionID+DOLLER));
        debug.append(SEVENHYPHEN+myreceivedTransactionID+DOLLER);

        out.write(CRLF);
        debug.append(LINESEPARATOR);
        return debug.toString();
    }

    /**
     *  wait for 'n' responses as specified in the 'number' parameter
     * @param number  - number of responses to wait
     * @throws java.lang.InterruptedException
     */

    public static void waitForResponses(int number) throws java.lang.InterruptedException
    {
        if(MSRPServer.checkSockets())
        {
            XTTProperties.printFail("MSRPWorker.waitForResponses: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait = XTTProperties.getIntProperty("MSRPSERVER/WAITTIMEOUT");
        if(wait<0)
            wait = MSRPServer.DEFAULTTIMEOUT;
        int prevcount = 0;
        synchronized (responsekey)
        {
            while (responsecount<number)
            {
                XTTProperties.printInfo("MSRPWorker.waitForResponses: "+responsecount+"/"+number);
                if(wait>0)
                {
                    prevcount = responsecount;
                    responsekey.wait(wait);
                    if(responsecount==prevcount)
                    {
                        XTTProperties.printFail("MSRPWorker.waitForResponses: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    responsekey.wait();
                }
            }
            XTTProperties.printInfo("MSRPWorker.waitForResponses: "+responsecount+"/"+number);
        }
    }

    /**
     * wait for 'n' requests as specified in the 'number' parameter
     * @param number  - number of requests to wait
     * @throws java.lang.InterruptedException
     */

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(MSRPServer.checkSockets())
        {
            XTTProperties.printFail("MSRPWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }

        int wait=XTTProperties.getIntProperty("MSRPSERVER/WAITTIMEOUT");
        if(wait<0)wait=MSRPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("MSRPWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("MSRPWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("MSRPWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }

    /**
     * Store the header(s)
     *
     * @param storeVar
     * @param where
     * @param header
     */
    private void storeHeaders(String[] storeVar, String where, LinkedHashMap<String, Vector<String>> header)
    {
        HTTPHelper.storeHeaders("MSRPWorker("+myServerPort+"/"+getWorkerId()+"): headers stored:",storeVar,where,header);
    }

    /**
     * This is setter function for variables.
     *
     * @param store
     * @param where
     * @param what
     */
    private void setMultipleVariables(String[] store, String where, String what)
    {
        for (int i = 0; i<store.length; i++)
        {
            XTTProperties.setVariable(store[i]+where, what);
        }
    }

    public static void setSkipResponse(boolean code)
    {
        skipResponse = code;
    }

    public Socket getCurrentTCPSocket()
    {
        return this.tcpSocket;
    }

    /**
     * method use to return response message w.r.t. response code, Ref:RFC4975[10]
     *
     * @param code
     * @return string
     */
    public static String getResponseMessage(int code)
    {
        switch (code)
        {
        case MSRP_OK:                                  return "OK";
        case MSRP_BAD_REQUEST:                         return "Bad Request";
        case MSRP_FORBIDDEN:                           return "Forbidden";
        case MSRP_REQUEST_TIME_OUT:                    return "Request Timeout";
        case MSRP_REQUEST_ENTITY_TOO_LARGE:            return "Request Entity Too Large";
        case MSRP_UNSUPPORTED_MEDIA_TYPE:              return "Unsupported Media Type";
        case MSRP_NOT_IMPLEMENTED:                     return "Not Implemented";
        case MSRP_PARAMETERS_OUT_OF_BOUNDS:            return "Parameter Out Of Bound";
        case MSRP_INVALID_SESSION:                     return "Session Does Not Exist";
        case MSRP_SESSION_ALREADY_BOUND:               return "Session Already Bound";
        default:                                       return "Unknown";
        }
    }

    public static void waitForTimeoutResponses(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if (MSRPServer.checkSockets())
        {
            XTTProperties.printFail("MSRPWorker.waitForTimeoutResponses: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait      = timeouttime;
        int prevcount = 0;
        int number    = 0;

        synchronized (responsekey)
        {
            if (maxnumber >= 0)
            {
                number = maxnumber + 1;
            } else
            {
                number = responsecount + 1;
            }
            while (responsecount < number)
            {
                XTTProperties.printInfo("MSRPWorker.waitForTimeoutResponses: " + responsecount + "/" + number + " time: " + timeouttime + "ms");
                prevcount = responsecount;
                responsekey.wait(wait);
                if (responsecount == prevcount)
                {
                    XTTProperties.printInfo("MSRPWorker.waitForTimeoutResponses: timed out with no responses!");
                    return;
                }
            }
            XTTProperties.printFail("MSRPWorker.waitForTimeoutResponses: response received! " + responsecount + "/" + number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    
    
    private class DataHandler
    {
        public DataHandler()
        {
        }

        private boolean  isFirtsTime             = false;
        private int      datalength              = 0;
        private int      offset                  = 0;
        private int      actualChunkedBodyLength = 0;
        private int      startByteRangeNumber    = 0;
        private int      totalByteRangeNumber    = 0;
        private String   byteRange               = null;
        private String[] arrayByteRange          = null;
        private String[] arrayOutOfTotalBytes    = null;
        private byte[]   receivedBody            = null;
        private byte[]   receivedData            = new byte[0];
        private byte[]   body                    = new byte[0];

        private void handleData(BufferedInputStream inputStream, String methodName) throws IOException
        {
            int endByteRangeNumber = 0;
            if(!methodName.equals(REPORT))
            {
                byteRange            = myReceivedHeader.get(BYTERANGE.toLowerCase()).get(0);
                arrayByteRange       = byteRange.split(SINGLEHYPHEN);
                arrayOutOfTotalBytes = arrayByteRange[1].split(SLASH);

                try
                {
                    startByteRangeNumber = Integer.parseInt(arrayByteRange[0]);
                    
                    if(!arrayOutOfTotalBytes[1].equals(ASTERISK))
                    {
                    	totalByteRangeNumber = Integer.parseInt(arrayOutOfTotalBytes[1]);
                    	if(totalByteRangeNumber == 0)
                    	{
                    		returnCode = MSRP_OK;
                    		storeHeaders(finishedNotifcation, "/HEADER", myReceivedHeader);
                    		return;
                    	}
                    	
                    }
                    if(!arrayOutOfTotalBytes[0].equals(ASTERISK))
                    {
                        endByteRangeNumber = Integer.parseInt(arrayOutOfTotalBytes[0]);
                    }
                } catch (NumberFormatException e)
                {
                    XTTProperties.printFail("MSRPWorker :"+arrayByteRange[0]+SLASH+arrayOutOfTotalBytes[1]+SLASH+arrayOutOfTotalBytes[0]+" is NOT a correct number."+e.getMessage());
                }

                if(!isFirtsTime && totalByteRangeNumber!=0&&!byteRange.startsWith("1-0"))
                {
                    receivedBody = new byte[totalByteRangeNumber];
                    isFirtsTime = true;
                }

                if(endByteRangeNumber==0)
                {
                    //receivedData = HTTPHelper.readStream("MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")", inputStream, networklagdelay);
                    receivedData =readBytes(inputStream,"MSRPWorker"+"("+myServerPort+"/"+getWorkerId()+")");
                } else
                {
                    // +1  = if byte range is like 5-10/20 then endByteRangeNumber-startByteRangeNumber = 5 but actually it its need 6 to read data from byte array.
                    // +12 = $/+/#(1) + CRLF(2) + seven hypen(7) + CRLF(2)
                    datalength   = endByteRangeNumber-startByteRangeNumber+1+12+myreceivedTransactionID.getBytes().length;
                    receivedData = new byte[datalength];
                    HTTPHelper.readBytes(inputStream, receivedData);
                }
                offset                   = startByteRangeNumber-1;
                lastByte                 = receivedData[receivedData.length-3];
                byte[] actualChunkedBody = new byte[0];
                if(!byteRange.startsWith("1-0"))
                {
                    actualChunkedBody = ConvertLib.subByteArray(receivedData, 0, receivedData.length-myreceivedTransactionID.getBytes().length-12);
                    actualChunkedBodyLength  = actualChunkedBodyLength+actualChunkedBody.length;
                 }
                if(lastByte==INTERRUPT && totalByteRangeNumber!=0)
                {
                    ConvertLib.addBytesToArray(receivedBody, offset, actualChunkedBody);
                    returnCode = MSRP_OK;
                } else if(lastByte==INTERRUPT && totalByteRangeNumber==0)
                {
                    insertByteArray(actualChunkedBodyLength, offset, actualChunkedBody);
                    returnCode = MSRP_OK;
                } else if(lastByte==ABORTMESSAGE)
                {
                    returnCode = MSRP_FORBIDDEN;
                } else if(lastByte==ENDMESSAGE)
                {
                	if(!isFirtsTime)
                	{
                	    if(totalByteRangeNumber==0)
                	    {
                	        totalByteRangeNumber =  startByteRangeNumber + actualChunkedBody.length - 1;
                	    }
                		receivedBody = new byte[totalByteRangeNumber];
                    	ConvertLib.addBytesToArray(receivedBody, 0, body);	
                    	isFirtsTime = true;
                	}
                	
                    ConvertLib.addBytesToArray(receivedBody, offset, actualChunkedBody);
                   
                    returnCode = MSRP_OK;
                } else
                {
                    returnCode = MSRP_BAD_REQUEST;
                }

            }

           
            //Store the header
            storeHeaders(finishedNotifcation, "/HEADER", myReceivedHeader);

            if(totalByteRangeNumber==actualChunkedBodyLength&&!methodName.equals(REPORT))
            {
                String bodyPlain  = ConvertLib.createString(receivedBody);
                String bodyBase64 = ConvertLib.base64Encode(receivedBody);
                setMultipleVariables(finishedNotifcation, "/BODY/BASE64", bodyBase64);
                setMultipleVariables(finishedNotifcation, "/BODY/PLAIN", bodyPlain);
                actualChunkedBodyLength = 0;
                isFirtsTime = false;
            }
        }
        
        /**
         * returns a byte array with the content of the given array starting from the specified start index.<br>
         * <br>
         * Examples:<br>
         * input : <br>
         *  bytes1 = {.,.,.,7,8,9}<br>
         *   <br>
         *  byte2 : {1,2,3} & off = 0 <br>
         * 
         * output bytes1 = {1,2,3,.,.,.,7,8,9}
         * @return a part of the byte array starting from specified start index
         */
        private byte[] insertByteArray(int actualChunkedBodylength, int offset, byte[] actualChunkedBody)
        {
            byte[] temp = null;
            if(offset>=body.length)
            {
                temp = new byte[body.length];
                ConvertLib.addBytesToArray(temp, 0, body);
                if(offset>actualChunkedBodylength)
                {
                    body = new byte[offset+actualChunkedBodylength];   
                } else
                {
                    body = new byte[actualChunkedBodylength];   
                }
                ConvertLib.addBytesToArray(body, 0, temp);
                ConvertLib.addBytesToArray(body, offset, actualChunkedBody);
                return body;
            } else
            {
                ConvertLib.addBytesToArray(body, offset, actualChunkedBody);
                return body;
            }
        }
       /**
        * read byte by byte from input untill the bondary. 
        * @param inputStream
        * @param function
        * @return
        * @throws IOException
        */
               
        private byte[] readBytes(BufferedInputStream inputStream, String function) throws IOException
        {
            
            byte[] body           = null;
            String boundary       = LINESEPARATOR+SEVENHYPHEN+myreceivedTransactionID;
            byte[] boundaryBytes  = boundary.getBytes();
            int boundarylength    = boundaryBytes.length;
            MSRPBuffer msrpBuffer = new MSRPBuffer(36768+boundarylength+4,inputStream); 
            if(function!=null)XTTProperties.printDebug(function+": Reading Bytes from inputStream:");
            while(true)
            {
                msrpBuffer.loadBytesFromInputStream(function);
                body = msrpBuffer.getBytes();
                // to read extra CRLF which is in body
                if (byteRange.startsWith("1-0"))
                {
                    msrpBuffer.loadBytesFromInputStream(function);
                    body = msrpBuffer.getBytes();
                    msrpBuffer.loadBytesFromInputStream(function);
                    body = msrpBuffer.getBytes();
                }
                //body.length-3 =  do not compare last three byte (i.e. $ or + or # and CRLF) 
                boolean flag = compareBytes(body, body.length-3, boundaryBytes, boundarylength, boundarylength);
                if(flag)
                {
                    return body;
                }  
            }
        }
    }  
    /**
     * compare the bytes in a2 with same number of last bytes in a1 if equeal return true othrewise return false.
     * @param a1
     * @param offs1
     * @param a2
     * @param offs2
     * @param len
     * @return
     */
    private boolean compareBytes(byte[] a1, int offs1, byte[] a2, int offs2, int len)
    {
        while (--len>0)
        {
            if(a1[--offs1]!=a2[--offs2])
            {
                return false;
            }
        }
        return true;
    }
    
    private class MSRPBuffer
    {

        private byte                ringBuff[];
        private byte[]              retrunRingBuff;
        private int                 ringStart;
        private int                 ringEnd;
        private Object              ringLock;
        private BufferedInputStream inputStream;
        private Thread              loaderThread = null;

        /* constructor */
        public MSRPBuffer(int bufferSize, BufferedInputStream input)
        {
            ringBuff    = new byte[bufferSize];
            ringStart   = 0;
            ringEnd     = ringStart;
            ringLock    = new Object();
            inputStream = input;
        }

        public void loadBytesFromInputStream(String function) throws IOException
        {
            this.loadBytesFromInputStream(false, function);
        }

        /* load bytes from InputStream */
        // if 'block' is true, this method will not return unless an IOException
        // is thrown during the 'read'.
        private void loadBytesFromInputStream(boolean block, String function) throws IOException
        {
            int lastbyte = 0;
            int CR       = 0x0D;
            int LF       = 0x0A;
            /* initialize loader thread */
            // The 'loaderThread' initialization isn't locked.  Since the value
            // doesn't change once initialized, this should be ok.
            if(loaderThread==null)
            {
                loaderThread = Thread.currentThread();
            }
            /* read all available bytes */
            while (block||(inputStream.available()>0))
            {
                int b = inputStream.read(); 
                if(lastbyte!=CR&&b!=LF&&b>=0)
                {
                    synchronized (ringLock)
                    {
                        this.putByte(b,function);
                    }
                    lastbyte = b;
                } else
                {
                    lastbyte = b;
                    this.putByte(b,function);
                    break;
                }
            }
        }

        public boolean putByte(int val, String function)
        {
            boolean ok = false;
            synchronized (ringLock)
            {
                int next = (ringEnd+1)%ringBuff.length;
                if(next!=ringStart)
                {
                    ringBuff[this.ringEnd] = (byte) (val&0xFF);
                    ringEnd = next;
                    ringLock.notify();
                    ok = true;
                } else
                {
	                // if buffer overrun increase the size of buffer
                    byte tempringBuff[] = new byte[ringBuff.length+2048];
                    ConvertLib.addBytesToArray(tempringBuff, 0, ringBuff);
                    ringBuff    = new byte[ringBuff.length+2048];
                    ConvertLib.addBytesToArray(ringBuff, 0, tempringBuff);
                    next = (ringEnd+1)%ringBuff.length;
                    ringBuff[this.ringEnd] = (byte) (val&0xFF);
                    ringEnd = next;
                    ringLock.notify();
                    ok = true;
                }
            }
            return ok;
        }

        public byte[] getBytes() throws IOException
        {
            retrunRingBuff = new byte[this.ringEnd];
            System.arraycopy(ringBuff, 0, retrunRingBuff, 0, retrunRingBuff.length);
            return this.retrunRingBuff;
        }

    }
    
    public static final String tantau_sccsid = "@(#)$Id: MSRPWorker.java,v 1.6 2009/08/07 13:50:42 awadhai Exp $";

}
