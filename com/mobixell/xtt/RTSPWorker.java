package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.net.URI;

/**
 * <p>RTSPWorker</p>
 * <p>Processes a single RTSP request which has been received by the RTSPServer</p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: 724 Solutions Inc</p>
 *
 * Stores headers as<code><br>
 *  Response:<br>
 *  RTSP/[port]/RESPONSE/[sessionid]/<br>
 *  RTSP/[port]/RESPONSE/<br>
 *  RTSP/[port]/TCP/RESPONSE/[sessionid]/<br>
 *  RTSP/[port]/TCP/RESPONSE/<br>
 *  RTSP/TCP/RESPONSE/[sessionid]/<br>
 *  RTSP/TCP/RESPONSE/<br>
 *  and:<br>
 *  /HEADER/[headername]<br>
 *  /BODY/BASE64<br>
 *  /BODY/PLAIN<br>
 *  <br>
 *  Request:<br>
 *  RTSP/[port]/[method]/[sessionid]/<br>
 *  RTSP/[port]/[method]/[url]/<br>
 *  RTSP/[port]/[method]/<br>
 *  RTSP/[port]/TCP/[method]/[sessionid]/<br>
 *  RTSP/[port]/TCP/[method]/[url]/<br>
 *  RTSP/[port]/TCP/[method]/<br>
 *  RTSP/TCP/[method]/[sessionid]/<br>
 *  RTSP/TCP/[method]/[url]/<br>
 *  RTSP/TCP/[method]/<br>
 *  and:<br>
 *  /HEADER/[headername]<br>
 *  /BODY/BASE64<br>
 *  /BODY/PLAIN<br>
 * <br>The Variable ending in /FINISHED/REQUEST is set to true after sending the response/finishing the response handling.
 *  </code><br>
 *
 *
 * @author Roger Soder
 * @version $Id: RTSPWorker.java,v 1.7 2010/03/25 10:18:30 rajesh Exp $
 */
public class RTSPWorker extends Thread implements RTSPConstants
{
    private static final int MINLAGDELAY=100;
    private static final String DEFAULTFILE="default.rtsp";

    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,Vector<String>> sendServerHeader=new LinkedHashMap<String,Vector<String>>();

    private static LinkedHashMap<String,Boolean> sessionIsInterleaved=new LinkedHashMap<String,Boolean>();

    private LinkedHashMap<String,Vector<String>> headersToSend=new LinkedHashMap<String,Vector<String>>(); //Not static, this has to be different for each request
    private LinkedHashMap<String,Vector<String>> myReceivedHeader=null;
    private Vector<ByteArrayWrapper> additionalPackets=new Vector<ByteArrayWrapper>();
    private String[] finishedNotifcation=new String[0];
    private byte[] responseBody = new byte[0];
    private byte[] sendBody = new byte[0];
    private int recievedResponseCode=-1;
    private String recievedResponseMessage=null;
    private String protocol = null;
    private boolean stop = false;
    private int id;

    //private static Map<String,ByteArrayWrapper> fileCache = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());
    private static HTTPCache fileCache = new HTTPCache(DEFAULTFILE);

    private static String recievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    private int returnCode = RTSP_INTERNAL_SERVER_ERROR;
    private static int overrideReturnCode=0;
    private static String overrideReturnMessage=null;

    private static final byte[] CRLF = {(byte)'\r', (byte)'\n' };
    private static final int DEFAULTEXPIRY = 120;//in seconds

    private static Object responsekey=new Object();
    private static int responsecount=0;

    private static Object requestkey=new Object();
    private static int requestcount=0;

    private static Object cSeqKey=new Object();
    private static int cSeqCount=0;
    public static int getCSeq(){synchronized(cSeqKey){return ++cSeqCount;}}

    /* Socket to client we're handling, which will be set by the RTSPServer
       when dispatching the request to us */
    private Socket tcpSocket = null;
    private DatagramPacket udpPacket = null;
    private DatagramSocket udpSocket = null;
    private RTSPServer myServer=null;
    private File myServerRootDir=null;
    private int myTimeout=600000;
    private int myServerPort=-1;
    private String myIPAddress=null;

    private boolean keep_alive=true;

    private static boolean disableAuthentication=true;
    private static boolean skipResponse=false;
    private static boolean skipAutoSessionID=false;
    private static int sessionID=0;
    private static Object sessionIDKey=new Object();

    private static String password="password";

    private String myrecievedURL=null;
    private String firstCommand=null;
    private String myreceivedSessionID=null;
    private boolean skipContentLength=false;

    /**
     * Creates a new RTSPWorker
     * @param id     ID number of this worker thread
     */
    public RTSPWorker(int id, Socket setSocket, RTSPServer sserver, int timeout,int serverPort, File rootDir)
    {
        this(id, setSocket, sserver, timeout,serverPort,true, rootDir);
    }
    
    /**
     * Creates a new RTSPWorker
     * @param id  ID number of this worker thread
     * @param setSocket - socket to communicate to server
     * @param sserver - server object
     * @param timeout - timeout for thread
     * @param serverPort - server listening port
     * @param keep - value for keep alive
     * @param rootDir - value of server root directory
     */
    
    public RTSPWorker(int id, Socket setSocket, RTSPServer sserver, int timeout,int serverPort, boolean keep, File rootDir)
    {
        this.tcpSocket = setSocket;
        this.udpPacket = null;
        this.udpSocket = null;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=sserver;
        this.keep_alive=keep;
        this.myServerRootDir=rootDir;
        this.myServerPort=serverPort;
        this.myIPAddress=tcpSocket.getLocalAddress().getHostAddress();
    }
    
    /**
     * @param id ID number of this worker thread
     * @param setSocket - socket to communicate to server
     * @param setPacket - set datagram UDP packet
     * @param sserver - server object
     * @param timeout - timeout for thread
     * @param serverPort - server listening port 
     * @param rootDir - value of server root directory
     * 
     */
    
    public RTSPWorker(int id, DatagramSocket setSocket,DatagramPacket setPacket, RTSPServer sserver, int timeout,int serverPort, File rootDir)
    {
        this.tcpSocket = null;
        this.udpPacket = setPacket;
        this.udpSocket = setSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=sserver;
        this.myServerRootDir=rootDir;
        this.myServerPort=serverPort;
        this.myIPAddress=udpSocket.getLocalAddress().getHostAddress();
    }

    /**
     * Handles id for thread
     * @return - id associated with thread
     */
    public int getWorkerId()
    {
        return id;
    }
    
    /**
     * Handles current server port
     * @return -  server port number
     */
    public int getMyServerPort()
    {
        return myServerPort;
    }

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        XTTProperties.printDebug("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        this.stop = true;
    }
    public synchronized void doStop()
    {
        this.stop = true;
        XTTProperties.printDebug("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): doing stop");
        try
        {
            this.tcpSocket.close();
        } catch (Exception ex){}
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
            if(tcpSocket!=null&&udpPacket==null)
            {
                handleTCPClient();
            } else if(tcpSocket==null&&udpPacket!=null)
            {
                handleUDPClient();
            } else
            {
                XTTProperties.printFail("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): invalid state in run: "+tcpSocket+"/"+udpPacket);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
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
                XTTProperties.printFail("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
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
                XTTProperties.printDebug("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
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
                XTTProperties.printFail("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    /**
     * Handles the RTSP request
     * @throws IOException
     */
    public void handleUDPClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): New Client handled by "+id+" instance "+instances);
            key.notify();
        }
        ByteArrayInputStream dataStreamIn=new ByteArrayInputStream(udpPacket.getData(),udpPacket.getOffset(),udpPacket.getLength() );
        BufferedInputStream  inputStream  = new BufferedInputStream(dataStreamIn);
        ByteArrayOutputStream dataStreamOut=new ByteArrayOutputStream(65535);
        BufferedOutputStream outputStream = new BufferedOutputStream(dataStreamOut);

        try
        {
            //KEEPALIVE:do
            {
                additionalPackets.clear();
                XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): receiving from "+udpPacket.getSocketAddress());
                if(!handleData("UDP",inputStream)||skipResponse)
                {
                    XTTProperties.printTransaction("RTSPWORKER/UDP"+XTTProperties.DELIMITER
                        +firstCommand+XTTProperties.DELIMITER
                        +myrecievedURL+XTTProperties.DELIMITER
                        +myreceivedSessionID);
                    setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
                    return;//break;
                }
                Vector<String> v=null;
                if(sendBody==null||sendBody.length==0)
                {
                    sendBody=new byte[0];
                } else if(!skipContentLength)
                {
                    v=new Vector<String>();
                    v.add(""+sendBody.length);
                    headersToSend.put("content-length",v);
                }

                if(overrideReturnCode>0)returnCode=overrideReturnCode;
                String returnMessage=" "+getResponseMessage(returnCode);
                if(overrideReturnMessage!=null)returnMessage=overrideReturnMessage;


                v=new Vector<String>();
                v.add("RTSP/1.0 "+returnCode+returnMessage);
                headersToSend.put(null,v);

                mergeHeaders(headersToSend,sendServerHeader);
                String debug=printDeepHeaders("UDP",headersToSend,outputStream);
                XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): UDP sent Headers\n"+debug);
                if(sendBody.length>0)
                {
                    outputStream.write(sendBody);
                }
                outputStream.flush();

                byte[] data=dataStreamOut.toByteArray();
                DatagramPacket packet=new DatagramPacket(data,data.length,udpPacket.getSocketAddress());

                udpSocket.send(packet);
                XTTProperties.printTransaction("RTSPWORKER/UDP"+XTTProperties.DELIMITER
                    +firstCommand+XTTProperties.DELIMITER
                    +myrecievedURL+XTTProperties.DELIMITER
                    +myreceivedSessionID);
                XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): packet sent: "+data.length+" bytes to: "+packet.getSocketAddress() );

                Iterator<ByteArrayWrapper> it=additionalPackets.iterator();
                ByteArrayWrapper wrapperpacket=null;
                while(it.hasNext())
                {
                    wrapperpacket=it.next();
                    //packet=new DatagramPacket(wrapperpacket.getArray(),wrapperpacket.getArray().length,ip,port);
                    packet=new DatagramPacket(wrapperpacket.getArray(),wrapperpacket.getArray().length,udpPacket.getSocketAddress());
                    udpSocket.send(packet);
                    XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): Additional packet sent: "+wrapperpacket.getArray().length+" bytes to: "+packet.getSocketAddress()+"\n"+ConvertLib.createString(wrapperpacket.getArray()));//ConvertLib.getHexView(wrapperpacket.getArray(),0,wrapperpacket.getArray().length));
                }

                synchronized (requestkey)
                {
                    requestcount++;
                    requestkey.notifyAll();
                }
                setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
            } //while(keep_alive);
        } finally
        {
            //tcpSocket.close();
            XTTProperties.printDebug("RTSPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): packet handling finished");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    /**
     * Handles the RTSP request
     * @throws IOException
     */
    public void handleTCPClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("RTSPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): New Client handled by "+id+" instance "+instances);
            key.notify();
        }

        BufferedInputStream  inputStream  = new BufferedInputStream(tcpSocket.getInputStream(),65536);
        BufferedOutputStream outputStream = new BufferedOutputStream(tcpSocket.getOutputStream());
        tcpSocket.setSoTimeout(myTimeout);
        //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
        tcpSocket.setTcpNoDelay(true);

        try
        {
            KEEPALIVE:do
            {
                additionalPackets.clear();
                XTTProperties.printDebug("RTSPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): receiving from "+tcpSocket.getRemoteSocketAddress());
                if(!handleData("TCP",inputStream)||skipResponse)
                {
                    XTTProperties.printTransaction("RTSPWORKER/UDP"+XTTProperties.DELIMITER
                        +firstCommand+XTTProperties.DELIMITER
                        +myrecievedURL+XTTProperties.DELIMITER
                        +myreceivedSessionID);
                    setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
                    if(returnCode==0)
                    {
                        break;
                    } else
                    {
                        continue KEEPALIVE;
                    }
                }
                Vector<String> v=null;
                if(sendBody==null||sendBody.length==0)
                {
                    sendBody=new byte[0];
                } else if(!skipContentLength)
                {
                    v=new Vector<String>();
                    v.add(""+sendBody.length);
                    headersToSend.put("content-length",v);
                }

                if(overrideReturnCode>0)returnCode=overrideReturnCode;
                String returnMessage=" "+getResponseMessage(returnCode);
                if(overrideReturnMessage!=null)returnMessage=overrideReturnMessage;

                v=new Vector<String>();
                v.add("RTSP/1.0 "+returnCode+returnMessage);
                headersToSend.put(null,v);

                mergeHeaders(headersToSend,sendServerHeader);
                String debug=printDeepHeaders("TCP",headersToSend,outputStream);
                XTTProperties.printDebug("RTSPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): TCP sent Headers\n"+debug);
                if(sendBody.length>0)
                {
                    outputStream.write(sendBody);
                }
                outputStream.flush();
                XTTProperties.printTransaction("RTSPWORKER/TCP"+XTTProperties.DELIMITER
                    +firstCommand+XTTProperties.DELIMITER
                    +myrecievedURL+XTTProperties.DELIMITER
                    +myreceivedSessionID);

                Iterator<ByteArrayWrapper> it=additionalPackets.iterator();
                ByteArrayWrapper wrapperpacket=null;
                while(it.hasNext())
                {
                    wrapperpacket=it.next();
                    outputStream.write(wrapperpacket.getArray());
                    outputStream.flush();
                    XTTProperties.printDebug("RTSPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): Sent additional packet:\n"+ConvertLib.createString(wrapperpacket.getArray()));//ConvertLib.getHexView(wrapperpacket.getArray(),0,wrapperpacket.getArray().length));
                }

                synchronized (requestkey)
                {
                    requestcount++;
                    requestkey.notifyAll();
                }
                setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
            } while(keep_alive);
        } finally
        {
            if(keep_alive)
            {
                tcpSocket.close();
                XTTProperties.printDebug("RTSPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): Connection closed");
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
     * Handles RTSP request
     * @param mode - "TCP" or "UDP"
     * @param inputStream - Stream with all the headers
     * @return - 
     * @throws IOException
     */
    private boolean handleData(String mode,BufferedInputStream  inputStream) throws IOException
    {
        finishedNotifcation=new String[0];
        headersToSend=new LinkedHashMap<String,Vector<String>>();
        headersToSend.put(null,new Vector<String>());
        byte[] receivedData=new byte[0];
        returnCode=RTSP_OK;

        XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): decoding data");

        int networklagdelay=XTTProperties.getNetworkLagDelay();
        if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;


        myReceivedHeader=HTTPHelper.readHTTPStreamHeaders("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+")",inputStream);

        //End fo stream so break, use return code 0 to exit the keep-alive loop
        if(myReceivedHeader.get(null)==null){returnCode=0;return false;}

        String firstLine[]=myReceivedHeader.get(null).get(0).split("\\s+",4);
        synchronized(receivedServerHeader)
        {
            receivedServerHeader.clear();
            receivedServerHeader.putAll(myReceivedHeader);
        }

        firstCommand=firstLine[0];

        recievedResponseCode=-1;
        recievedResponseMessage=null;
        myrecievedURL=firstLine[1];
        recievedURL=myrecievedURL;
        protocol = firstLine[2];
        if(myReceivedHeader.get("session")!=null)
        {
            myreceivedSessionID=myReceivedHeader.get("session").get(0);
        } else
        {
            myreceivedSessionID=null;
        }

        finishedNotifcation=new String[]{"RTSP/"+myServerPort+"/"+firstCommand+"/"+myreceivedSessionID
                                        ,"RTSP/"+myServerPort+"/"+firstCommand+"/"+myrecievedURL
                                        ,"RTSP/"+myServerPort+"/"+firstCommand
                                        ,"RTSP/"+myServerPort+"/"+mode+"/"+firstCommand+"/"+myreceivedSessionID
                                        ,"RTSP/"+myServerPort+"/"+mode+"/"+firstCommand+"/"+myrecievedURL
                                        ,"RTSP/"+myServerPort+"/"+mode+"/"+firstCommand
                                        ,"RTSP/"+mode+"/"+firstCommand+"/"+myreceivedSessionID
                                        ,"RTSP/"+mode+"/"+firstCommand+"/"+myrecievedURL
                                        ,"RTSP/"+mode+"/"+firstCommand
                                        };

        if(firstCommand.startsWith("RTSP/"))
        {
            recievedResponseCode = Integer.parseInt(firstLine[1]);
            recievedResponseMessage = firstLine[2];
            recievedURL = null;
            protocol = firstLine[0];
            firstCommand="RESPONSE "+recievedResponseCode;
            finishedNotifcation=new String[]{"RTSP/"+myServerPort+"/RESPONSE/"+myreceivedSessionID
                                            ,"RTSP/"+myServerPort+"/RESPONSE"
                                            ,"RTSP/"+myServerPort+"/"+mode+"/RESPONSE/"+myreceivedSessionID
                                            ,"RTSP/"+myServerPort+"/"+mode+"/RESPONSE"
                                            ,"RTSP/"+mode+"/RESPONSE/"+myreceivedSessionID
                                            ,"RTSP/"+mode+"/RESPONSE"
                                            };
            storeHeaders(mode,finishedNotifcation,"/HEADER",myReceivedHeader);
            Vector contentlengthVector=myReceivedHeader.get("content-length");
            if(contentlengthVector!=null)
            {
                int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                receivedData=new byte[contentlength];
                HTTPHelper.readBytes(inputStream,receivedData);
                XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
            }
            String bodyPlain=ConvertLib.createString(receivedData);
            String bodyBase64=ConvertLib.base64Encode(receivedData);
            setMultipleVariables(finishedNotifcation,"/BODY/BASE64",bodyBase64);
            setMultipleVariables(finishedNotifcation,"/BODY/PLAIN",bodyPlain);
            synchronized (responsekey)
            {
                responsecount++;
                responsekey.notifyAll();
            }
            return false;
        } else if(firstCommand.equals("OPTIONS"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/REGISTER"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if(firstCommand.equals("DESCRIBE"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/SUBSCRIBE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if(firstCommand.equals("ANNOUNCE"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/NOTIFY"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("SETUP"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/MESSAGE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("PLAY"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/INVITE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("PAUSE"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/ACK"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("TEARDOWN"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/CANCEL"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("GET_PARAMETER"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("SET_PARAMETER"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("REDIRECT"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("RECORD"))
        {
            //XTTProperties.printTransaction("RTSPSERVER/RTSP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else
        {
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method '"+firstCommand+"' is NOT SUPPORTED");
            returnCode = RTSP_NOT_IMPLEMENTED;
            headersToSend.put("cseq",myReceivedHeader.get("cseq"));
            //headersToSend.put("call-id",myReceivedHeader.get("call-id"));
            storeHeaders(mode,finishedNotifcation,"/HEADER",myReceivedHeader);
            return true;
        }
        storeHeaders(mode,finishedNotifcation,"/HEADER",myReceivedHeader);

        Vector contentlengthVector=myReceivedHeader.get("content-length");
        if(contentlengthVector!=null)
        {
            int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
            receivedData=new byte[contentlength];
            HTTPHelper.readBytes(inputStream,receivedData);
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
        }

        String bodyPlain=ConvertLib.createString(receivedData);
        String bodyBase64=ConvertLib.base64Encode(receivedData);

        setMultipleVariables(finishedNotifcation,"/BODY/BASE64",bodyBase64);
        setMultipleVariables(finishedNotifcation,"/BODY/PLAIN",bodyPlain);

        Vector<String> v=null;

        v=new Vector<String>();
        v.add("XTT/RTSP;build="+XTTProperties.getXTTBuildVersion()+";worker="+FunctionModule.parseVersion(tantau_sccsid));
        headersToSend.put("server",v);

        //Common for all
        headersToSend.put("cseq",myReceivedHeader.get("cseq"));

        v=new Vector<String>();
        v.add(HTTPHelper.createHTTPDate());
        headersToSend.put("date",v);

        String fname = "";
        String queryString = "";
        try 
        {
            URI url = new URI(myrecievedURL);
            fname = url.getPath();
            if( fname == null ) fname = "";
            queryString = url.getQuery();
            if( queryString == null ) queryString = "";
        } catch (Exception ex) 
        {
            XTTProperties.printDebugException(ex);
        }
            String query[] = queryString.split( ";|&" );
            String sreturnCode = "returnCode=";
            for( int i=0; i<query.length; i++ )
            {
                if( query[i].startsWith(sreturnCode) )
                {
                    returnCode = Integer.parseInt(query[i].substring(sreturnCode.length()));
                    if(returnCode!=RTSP_OK)return true;
                }
            }
        // Handle sending of the response body
        sendBody=getResponseBody(firstCommand,fname);

        if(skipResponse)
        {
            // Do not handle response generation when you're not sending it anyway
            return false;
        } else if(firstCommand.equals("SETUP"))
        {
            if(!skipAutoSessionID&&myReceivedHeader.get("session")==null)
            {
                v=new Vector<String>();
                String ssid=createSessionID();
                v.add(ssid);
                headersToSend.put("session",v);
                v=myReceivedHeader.get("transport");
                boolean isI=false;
                for (java.util.Enumeration<String> e = v.elements() ; e.hasMoreElements() ;) 
                {
                    if(e.nextElement().matches("interleaved=")&&mode.equals("TCP"))
                    {
                        isI=true;
                        sessionIsInterleaved.put(ssid,new Boolean(isI));
                    }
                }
                XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): session interleaved="+isI);
                
            }
            if(myReceivedHeader.get("session")!=null)
            {
                headersToSend.put("session",myReceivedHeader.get("session"));
            }
        } else if(firstCommand.equals("PLAY"))
        {
            Boolean isI=sessionIsInterleaved.get(myreceivedSessionID);
            if(isI!=null&&isI.booleanValue())
            {
                skipContentLength=true;
            } else
            {
                skipContentLength=false;
            }
            XTTProperties.printDebug("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): session interleaved="+skipContentLength);
        }
        return true;
    }

    /**
     * Stores the variables
     * @param store
     * @param where
     * @param what
     */
    private void setMultipleVariables(String[] store, String where, String what)
    {
        for(int i=0;i<store.length;i++)
        {
            XTTProperties.setVariable(store[i]+where,what);
        }
    }

    private static String createSessionID()
    {
        //return ConvertLib.outputBytes(RandomLib.getRandomMD5Hash(128));
        synchronized(sessionIDKey)
        {
            sessionID++;
            return "XTTSID-"+ConvertLib.addPrefixToString(sessionID+"",10,"0");
        }
    }

    private byte[] getResponseBody(String command, String fname) throws java.io.IOException
    {
        if (fname.startsWith("/"))
        {
            fname = fname.substring(1);
        }
        byte[] returnFile=fileCache.getFile("RTSPWorker("+myServerPort+"/"+getWorkerId()+"): response body source: ",myServerRootDir,fname,command);
        if(returnFile==null)returnFile=new byte[0];
        return returnFile;
    }
    
    /**
     * Make sure "where" is an empty string if not used
     * 
     * @param mode - "TCP" or "UDP"
     * @param storeVar - String of response headers
     * @param where - Make sure "where" is an empty string if not used
     * @param header - Header values.
     */    
    private void storeHeaders(String mode, String[] storeVar,String where,LinkedHashMap<String,Vector<String>> header)
    {
        HTTPHelper.storeHeaders("RTSPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): headers stored:",storeVar,where,header);
    }

    /**
     * @param mode - "TCP" or "UDP"
     * @param header - header values
     * @param out - print value
     * @return - address from the given headers
     * @throws java.io.IOException
     */
    public static String printDeepHeaders(String mode, LinkedHashMap<String,Vector<String>> header, BufferedOutputStream out) throws java.io.IOException
    {
        StringBuffer debug=new StringBuffer();
        Iterator<String> it=header.keySet().iterator();
        Iterator<String> vals=null;
        String headerKey=null;
        Vector<String> headerValues=null;
        String headerValue=null;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValues=header.get(headerKey);
            if(headerKey!=null)
            {
                vals=headerValues.iterator();
                while(vals.hasNext())
                {
                    out.write(ConvertLib.createBytes(headerKey+":"));
                    debug.append(headerKey+":");
                    headerValue=vals.next();
                    out.write(ConvertLib.createBytes(" "+headerValue));
                    out.write(CRLF);
                    debug.append(" "+headerValue+"\r\n");
                }
            } else
            {
                vals=headerValues.iterator();
                while(vals.hasNext())
                {
                    headerValue=vals.next();
                    out.write(ConvertLib.createBytes(headerValue));
                    out.write(CRLF);
                    debug.append(headerValue+"\r\n");
                }
            }
        }
        out.write(CRLF);
        debug.append("\r\n");
        return debug.toString();
    }

    public static void mergeHeaders(LinkedHashMap<String,Vector<String>> headerTo,LinkedHashMap<String,Vector<String>> header)
    {
        Iterator<String> it=header.keySet().iterator();
        Iterator<String> vals=null;
        String headerKey=null;
        Vector<String> headerValues=null;
        String headerValue=null;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValues=header.get(headerKey);
            if(headerKey!=null)
            {
                headerTo.put(headerKey,headerValues);
            }
        }
    }

    public static void setCacheFileBase64(String method,String fname, String content)
    {
        try
        {
            URI url = new URI(fname);
            fname = url.getPath();
        } catch(Exception e){}
        if (fname.startsWith("/"))
        {
            fname = fname.substring(1);
        }
        byte[] thefile=ConvertLib.base64Decode(content);
        XTTProperties.printInfo("RTSPWorker.setCacheFile: base64 decoded to cache: '"+method+": /"+fname+"' "+thefile.length+" bytes");
        fileCache.put(fname,thefile,method);
    }
    public static void clearCache()
    {
        XTTProperties.printInfo("RTSPWorker.setCacheFile: clearing file cache");
        fileCache.clear();// = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());
    }

    public static void setSkipResponse(boolean code)
    {
        skipResponse=code;
    }
    public static void setSkipAutoSessionID(boolean code)
    {
        skipAutoSessionID=code;
    }
    public static LinkedHashMap<String,Vector<String>> getServerHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerHeader);
        }
        return returnMap;
    }
    public static LinkedHashMap<String,Vector<String>> getServerSendHeader()
    {
        return sendServerHeader;
    }
    public static void setServerSendHeader(LinkedHashMap<String,Vector<String>> sendserverHeader)
    {
        sendServerHeader=sendserverHeader;
    }
    public static void setOverrideReturnCode(int code)
    {
        overrideReturnCode=code;
    }
    public static void setOverrideReturnMessage(String msg)
    {
        overrideReturnMessage=msg;
    }
    public static String getServerRecievedURL()
    {
        return recievedURL;
    }
    public static void init()
    {
        synchronized (requestkey)
        {
            requestcount=0;
        }
        synchronized (responsekey)
        {
            responsecount=0;
        }
        synchronized (cSeqKey)
        {
            cSeqCount=0;
        }
        synchronized(sessionIDKey)
        {
            sessionID=0;
        }
        overrideReturnCode=0;
        overrideReturnMessage=null;
        disableAuthentication=true;
        skipResponse=false;
        skipAutoSessionID=false;
        fileCache.clear();
        sessionIsInterleaved.clear();
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(RTSPServer.checkSockets())
        {
            XTTProperties.printFail("RTSPWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("RTSPSERVER/WAITTIMEOUT");
        if(wait<0)wait=RTSPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("RTSPWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("RTSPWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("RTSPWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(RTSPServer.checkSockets())
        {
            XTTProperties.printFail("RTSPWorker.waitForTimeoutRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(requestkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=requestcount+1;
            }
            while(requestcount<number)
            {
                XTTProperties.printInfo("RTSPWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("RTSPWorker.waitForTimeoutRequests: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("RTSPWorker.waitForTimeoutRequests: request received! "+requestcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void waitForResponses(int number) throws java.lang.InterruptedException
    {
        if(RTSPServer.checkSockets())
        {
            XTTProperties.printFail("RTSPWorker.waitForResponses: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("RTSPSERVER/WAITTIMEOUT");
        if(wait<0)wait=RTSPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(responsekey)
        {
            while(responsecount<number)
            {
                XTTProperties.printInfo("RTSPWorker.waitForResponses: "+responsecount+"/"+number);
                if(wait>0)
                {
                    prevcount=responsecount;
                    responsekey.wait(wait);
                    if(responsecount==prevcount)
                    {
                        XTTProperties.printFail("RTSPWorker.waitForResponses: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    responsekey.wait();
                }
            }
            XTTProperties.printInfo("RTSPWorker.waitForResponses: "+responsecount+"/"+number);
        }
    }
    public static void waitForTimeoutResponses(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(RTSPServer.checkSockets())
        {
            XTTProperties.printFail("RTSPWorker.waitForTimeoutResponses: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(responsekey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=responsecount+1;
            }
            while(responsecount<number)
            {
                XTTProperties.printInfo("RTSPWorker.waitForTimeoutResponses: "+responsecount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=responsecount;
                responsekey.wait(wait);
                if(responsecount==prevcount)
                {
                    XTTProperties.printInfo("RTSPWorker.waitForTimeoutResponses: timed out with no responses!");
                    return;
                }
            }
            XTTProperties.printFail("RTSPWorker.waitForTimeoutResponses: response received! "+responsecount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    /**
     * @param code - code number
     * @return - response message for particular code
     */
    public static String getResponseMessage(int code)
    {
        switch(code)
        {
            case RTSP_CONTINUE                            : return "Continue";
            case RTSP_OK                                  : return "OK";
            case RTSP_CREATED                             : return "Created";
            case RTSP_LOW_ON_STORAGE_SPACE                : return "Low on Storage Space";
            case RTSP_MULTIPLE_CHOICES                    : return "Multiple Choices";
            case RTSP_MOVED_PERMANENTLY                   : return "Moved Permanently";
            case RTSP_MOVED_TEMPORARILY                   : return "Moved Temporarily";
            case RTSP_SEE_OTHER                           : return "See Other";
            case RTSP_NOT_MODIFIED                        : return "Not Modified";
            case RTSP_USE_PROXY                           : return "Use Proxy";
            case RTSP_BAD_REQUEST                         : return "Bad Request";
            case RTSP_UNAUTHORIZED                        : return "Unauthorized";
            case RTSP_PAYMENT_REQUIRED                    : return "Payment Required";
            case RTSP_FORBIDDEN                           : return "Forbidden";
            case RTSP_NOT_FOUND                           : return "Not Found";
            case RTSP_METHOD_NOT_ALLOWED                  : return "Method Not Allowed";
            case RTSP_NOT_ACCEPTABLE                      : return "Not Acceptable";
            case RTSP_PROXY_AUTHENTICATION_REQUIRED       : return "Proxy Authentication Required";
            case RTSP_REQUEST_TIME_OUT                    : return "Request Timeout";
            case RTSP_GONE                                : return "Gone";
            case RTSP_LENGTH_REQUIRED                     : return "Length Required";
            case RTSP_PRECONDITION_FAILED                 : return "Precondition Failed";
            case RTSP_REQUEST_ENTITY_TOO_LARGE            : return "Request Entity Too Large";
            case RTSP_REQUEST_URI_TOO_LARGE               : return "Request-URI Too Long";
            case RTSP_UNSUPPORTED_MEDIA_TYPE              : return "Unsupported Media Type";
            case RTSP_PARAMETER_NOT_UNDERSTOOD            : return "Invalid parameter";
            case RTSP_CONFERENCE_NOT_FOUND                : return "Illegal Conference Identifier";
            case RTSP_NOT_ENOUGH_BANDWIDTH                : return "Not Enough Bandwidth";
            case RTSP_SESSION_NOT_FOUND                   : return "Session Not Found";
            case RTSP_METHOD_NOT_VALID_IN_THIS_STATE      : return "Method Not Valid In This State";
            case RTSP_HEADER_FIELD_NOT_VALID_FOR_RESOURCE : return "Header Field Not Valid";
            case RTSP_INVALID_RANGE                       : return "Invalid Range";
            case RTSP_PARAMETER_IS_READ_ONLY              : return "Parameter Is Read-Only";
            case RTSP_AGGREGATE_OPERATION_NOT_ALLOWED     : return "Aggregate Operation Not Allowed";
            case RTSP_ONLY_AGGREGATE_OPERATION_ALLOWED    : return "Only Aggregate Operation Allowed";
            case RTSP_UNSUPPORTED_TRANSPORT               : return "Unsupported Transport";
            case RTSP_DESTINATION_UNREACHABLE             : return "Destination Unreachable";
            case RTSP_INTERNAL_SERVER_ERROR               : return "Internal Server Error";
            case RTSP_NOT_IMPLEMENTED                     : return "Not Implemented";
            case RTSP_BAD_GATEWAY                         : return "Bad Gateway";
            case RTSP_SERVICE_UNAVAILABLE                 : return "Service Unavailable";
            case RTSP_GATEWAY_TIME_OUT                    : return "Gateway Timeout";
            case RTSP_RTSP_VERSION_NOT_SUPPORTED          : return "RTSP Version Not Supported";
            case RTSP_OPTION_NOT_SUPPORTED                : return "Option not support";
            default: return "Unknown";
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: RTSPWorker.java,v 1.7 2010/03/25 10:18:30 rajesh Exp $";
}
