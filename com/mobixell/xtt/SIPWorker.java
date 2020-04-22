package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import java.net.InetAddress;
/**
 * <p>SIPWorker</p>
 * <p>Processes a single HTTP request which has been received by the SIPServer</p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Roger Soder
 * @version $Id: SIPWorker.java,v 1.25 2010/03/18 05:30:40 rajesh Exp $
 */
public class SIPWorker extends Thread implements SIPConstants
{
    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,Vector<String>> sendServerHeader=new LinkedHashMap<String,Vector<String>>();

    private LinkedHashMap<String,Vector<String>> headersToSend=new LinkedHashMap<String,Vector<String>>(); //Not static, this has to be different for each request
    private LinkedHashMap<String,Vector<String>> myReceivedHeader=null;
    private Vector<ByteArrayWrapper> additionalPackets=new Vector<ByteArrayWrapper>();
    private String[] finishedNotifcation=new String[0];
    private byte[] sendBody = new byte[0];
    private int recievedResponseCode=-1;
    private String recievedResponseMessage=null;
    private String protocol = null;
    private boolean stop = false;
    private int id;

    private static Map<String,ByteArrayWrapper> fileCache = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());

    private static String recievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    private int returnCode = SIP_SERVER_INTERNAL_ERROR;
    private static int overrideReturnCode=0;
    private static String overrideReturnMessage=null;

    private static final byte[] CRLF = {(byte)'\r', (byte)'\n' };
    private static final int DEFAULTEXPIRY = 120;//in seconds

    private static Object postkey=new Object();
    private static int postcount=0;

    private static Object requestkey=new Object();
    private static int requestcount=0;

    private static Object cSeqKey=new Object();
    private static int cSeqCount=0;
    public static int getCSeq(){synchronized(cSeqKey){return ++cSeqCount;}}

    /* Socket to client we're handling, which will be set by the SIPServer
       when dispatching the request to us */
    private Socket tcpSocket = null;public Socket getSocket(){return tcpSocket;}
    private DatagramPacket udpPacket = null;
    private DatagramSocket udpSocket = null;
    private SIPServer myServer=null;
    private int myTimeout=600000;
    private int myServerPort=-1;
    private String myIPAddress=null;

    private boolean keep_alive=true;

    private static boolean disableAuthentication=true;
    private static boolean skipResponse=false;
    private static boolean skipAutoNotify=false;

    private static String password="password";
    
    private String myrecievedURL=null;
    private String firstCommand=null;
    private String myrecievedTo=null;
    private String myrecievedFrom=null;
    private String myrecievedCallID=null;

    private static final int MINLAGDELAY=100;
    /**
     * Creates a new SIPWorker
     * @param id     ID number of this worker thread
     */
    public SIPWorker(int id, Socket setSocket, SIPServer sserver, int timeout,int serverPort)
    {
        this(id, setSocket, sserver, timeout,serverPort,true);
    }
    /**
     * Creates a new SIPWorker
     * @param id - ID number of this worker thread
     * @param setSocket - socket to communicate to server
     * @param sserver - server object
     * @param timeout - timeout for thread
     * @param serverPort - server listening port
     * @param keep - value for keep alive
     */
    public SIPWorker(int id, Socket setSocket, SIPServer sserver, int timeout,int serverPort, boolean keep)
    {
        this.tcpSocket = setSocket;
        this.udpPacket = null;
        this.udpSocket = null;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=sserver;
        this.keep_alive=keep;
        myServerPort=serverPort;
        myIPAddress=tcpSocket.getLocalAddress().getHostAddress();
    }
    
    /**
     * Creates a new SIPWorker
     * @param id - ID number of this worker thread
     * @param setSocket - socket to communicate to server
     * @param setPacket - data gram packet
     * @param sserver -  server object
     * @param timeout - timeout for thread
     * @param serverPort - server listening port
     */
    public SIPWorker(int id, DatagramSocket setSocket,DatagramPacket setPacket, SIPServer sserver, int timeout,int serverPort)
    {
        this.tcpSocket = null;
        this.udpPacket = setPacket;
        this.udpSocket = setSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=sserver;
        myServerPort=serverPort;
        myIPAddress=udpSocket.getLocalAddress().getHostAddress();
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
        XTTProperties.printDebug("SIPWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        this.stop = true;
    }
    public synchronized void doStop()
    {
        this.stop = true;
        XTTProperties.printDebug("SIPWorker("+myServerPort+"/"+getWorkerId()+"): doing stop");
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
                XTTProperties.printFail("SIPWorker("+myServerPort+"/"+getWorkerId()+"): invalid state in run: "+tcpSocket+"/"+udpPacket);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
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
                XTTProperties.printFail("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
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
                XTTProperties.printDebug("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("SIPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
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
                XTTProperties.printFail("SIPWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("SIPWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }
    
    /**
     * Handles the HTTP request
     * @throws IOException
     */
    public void handleUDPClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): New Client handled by "+id+" instance "+instances);
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
                XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): receiving from "+udpPacket.getSocketAddress());
                if(!handleData("UDP",inputStream)||skipResponse)
                {
                    XTTProperties.printTransaction("SIPWORKER/UDP"+XTTProperties.DELIMITER
                        +firstCommand+XTTProperties.DELIMITER
                        +myrecievedURL+XTTProperties.DELIMITER
                        +myrecievedFrom+XTTProperties.DELIMITER
                        +myrecievedTo+XTTProperties.DELIMITER
                        +myrecievedCallID);
                    setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
                    return;//break;
                }
                if(sendBody==null)
                {
                    sendBody=new byte[0];
                }
                Vector<String> v=new Vector<String>();
                v.add(""+sendBody.length);
                headersToSend.put("content-length",v);

                if(overrideReturnCode>0)returnCode=overrideReturnCode;
                String returnMessage=" "+getResponseMessage(returnCode);
                if(overrideReturnMessage!=null)returnMessage=overrideReturnMessage;

                v=new Vector<String>();
                v.add("SIP/2.0 "+returnCode+returnMessage);
                headersToSend.put(null,v);

                mergeHeaders(headersToSend,sendServerHeader);
                String debug=printDeepHeaders("UDP",headersToSend,outputStream);
                XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): UDP sent Headers\n"+debug);
                if(sendBody.length>0)
                {
                    outputStream.write(sendBody);
                }
                outputStream.flush();

                byte[] data=dataStreamOut.toByteArray();
                //SIP/2.0/TCP 172.20.14.1:23370; or 
                String destination=myReceivedHeader.get("via").get(0);
                String[] temp=destination.split(";",2);
                //String[] ipPort=temp[0].split(" ")[1].split(":");
                                
                
                String host = null;
                String[] ipPort=temp[0].split(" ");
                int lstColon = ipPort[1].lastIndexOf(":");
                String prt = ipPort[1].substring(lstColon + 1);
                
                if((ipPort[1].contains("[")) & (ipPort[1].contains("]")))
                {
                     host = ipPort[1].substring(1, lstColon-1);
                }
                else
                {   
                    host = ipPort[1].substring(0, lstColon);
                }
                InetAddress ip=DNSServer.resolveAddressToInetAddress(host);
                int port=Integer.parseInt(prt);
               // InetAddress ip=DNSServer.resolveAddressToInetAddress(ipPort[0]);
              //  int port=Integer.parseInt(ipPort[1]);
                DatagramPacket packet=new DatagramPacket(data,data.length,ip,port);

                udpSocket.send(packet);
                XTTProperties.printTransaction("SIPWORKER/UDP"+XTTProperties.DELIMITER
                    +firstCommand+XTTProperties.DELIMITER
                    +myrecievedURL+XTTProperties.DELIMITER
                    +myrecievedFrom+XTTProperties.DELIMITER
                    +myrecievedTo+XTTProperties.DELIMITER
                    +myrecievedCallID);
                XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): packet sent: "+data.length+" bytes to: "+packet.getSocketAddress() );

                Iterator<ByteArrayWrapper> it=additionalPackets.iterator();
                ByteArrayWrapper wrapperpacket=null;
                while(it.hasNext())
                {
                    wrapperpacket=it.next();
                    packet=new DatagramPacket(wrapperpacket.getArray(),wrapperpacket.getArray().length,ip,port);
                    udpSocket.send(packet);
                    XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): Additional packet sent: "+wrapperpacket.getArray().length+" bytes to: "+packet.getSocketAddress()+"\n"+ConvertLib.createString(wrapperpacket.getArray()));//ConvertLib.getHexView(wrapperpacket.getArray(),0,wrapperpacket.getArray().length));
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
            XTTProperties.printDebug("SIPWorker.UDP("+myServerPort+"/"+getWorkerId()+"): packet handling finished");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    /**
     * Handles the HTTP request
     * @throws IOException
     */
    public void handleTCPClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("SIPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): New Client handled by "+id+" instance "+instances);
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
                XTTProperties.printDebug("SIPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): receiving from "+tcpSocket.getRemoteSocketAddress());
                if(!handleData("TCP",inputStream)||skipResponse)
                {
                    XTTProperties.printTransaction("SIPWORKER/UDP"+XTTProperties.DELIMITER
                        +firstCommand+XTTProperties.DELIMITER
                        +myrecievedURL+XTTProperties.DELIMITER
                        +myrecievedFrom+XTTProperties.DELIMITER
                        +myrecievedTo+XTTProperties.DELIMITER
                        +myrecievedCallID);
                    setMultipleVariables(finishedNotifcation,"/FINISHED/REQUEST","true");
                    if(returnCode==0)
                    {
                        break;
                    } else
                    {
                        continue KEEPALIVE;
                    }
                }
                if(sendBody==null)
                {
                    sendBody=new byte[0];
                }
                Vector<String> v=new Vector<String>();
                v.add(""+sendBody.length);
                headersToSend.put("content-length",v);

                if(overrideReturnCode>0)returnCode=overrideReturnCode;
                String returnMessage=" "+getResponseMessage(returnCode);
                if(overrideReturnMessage!=null)returnMessage=overrideReturnMessage;

                v=new Vector<String>();
                v.add("SIP/2.0 "+returnCode+returnMessage);
                headersToSend.put(null,v);

                mergeHeaders(headersToSend,sendServerHeader);
                synchronized(tcpSocket)
                {
                    String debug=printDeepHeaders("TCP",headersToSend,outputStream);
                    XTTProperties.printDebug("SIPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): TCP sent Headers\n"+debug);
                    if(sendBody.length>0)
                    {
                        outputStream.write(sendBody);
                    }
                    outputStream.flush();
                    XTTProperties.printTransaction("SIPWORKER/UDP"+XTTProperties.DELIMITER
                        +firstCommand+XTTProperties.DELIMITER
                        +myrecievedURL+XTTProperties.DELIMITER
                        +myrecievedFrom+XTTProperties.DELIMITER
                        +myrecievedTo+XTTProperties.DELIMITER
                        +myrecievedCallID);
    
                    Iterator<ByteArrayWrapper> it=additionalPackets.iterator();
                    ByteArrayWrapper wrapperpacket=null;
                    while(it.hasNext())
                    {
                        wrapperpacket=it.next();
                        outputStream.write(wrapperpacket.getArray());
                        outputStream.flush();
                        XTTProperties.printDebug("SIPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): Sent additional packet:\n"+ConvertLib.createString(wrapperpacket.getArray()));//ConvertLib.getHexView(wrapperpacket.getArray(),0,wrapperpacket.getArray().length));
                    }
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
                XTTProperties.printDebug("SIPWorker.TCP("+myServerPort+"/"+getWorkerId()+"): Connection closed");
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
     * Handles HTTP request
     * @param mode - "TCP" or "UDP"
     * @param inputStream - Stream with all the headers
     * @return - 
     * @throws IOException
     */
    private boolean handleData(String mode,BufferedInputStream  inputStream) throws IOException
    {
        returnCode=SIP_OK;
        finishedNotifcation=new String[0];
        headersToSend=new LinkedHashMap<String,Vector<String>>();
        headersToSend.put(null,new Vector<String>());
        byte[] receivedData=null;

        XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): decoding data");

        int networklagdelay=XTTProperties.getNetworkLagDelay();
        if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;


        myReceivedHeader=HTTPHelper.readHTTPStreamHeaders("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+")",inputStream);

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
        myrecievedTo=getAddress("to",myReceivedHeader);         
        myrecievedFrom=getAddress("from",myReceivedHeader);       
        myrecievedCallID=myReceivedHeader.get("call-id").get(0);     

        finishedNotifcation=new String[]{"SIP/"+mode+"/"+firstCommand+"/"+myrecievedFrom
                                        ,"SIP/"+mode+"/"+firstCommand+"/"+myrecievedTo        
                                        ,"SIP/"+mode+"/"+firstCommand+"/"+myrecievedCallID   
                                        ,"SIP/"+mode+"/"+firstCommand+"/"+myrecievedURL
                                        };

        if(firstCommand.startsWith("SIP/"))
        {
            recievedResponseCode = Integer.parseInt(firstLine[1]);
            recievedResponseMessage = firstLine[2];
            recievedURL = null;
            protocol = firstLine[0];
            firstCommand="RESPONSE "+recievedResponseCode;
            finishedNotifcation=new String[]{"SIP/"+mode+"/RESPONSE/"+myrecievedFrom  
                                            ,"SIP/"+mode+"/RESPONSE/"+myrecievedTo
                                            ,"SIP/"+mode+"/RESPONSE/"+myrecievedCallID
                                            };
            storeHeaders(mode,finishedNotifcation,"",myReceivedHeader);
            Vector contentlengthVector=myReceivedHeader.get("content-length");
            if(contentlengthVector!=null)
            {
                int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                receivedData=new byte[contentlength];
                HTTPHelper.readBytes(inputStream,receivedData);
                XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
            }
            String bodyPlain=ConvertLib.createString(receivedData);
            String bodyBase64=ConvertLib.base64Encode(receivedData);
            
            setMultipleVariables(finishedNotifcation,"/BODY/BASE64",bodyBase64);
            setMultipleVariables(finishedNotifcation,"/BODY/PLAIN",bodyPlain);
            return false;
        } else if(firstCommand.equals("REGISTER"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/REGISTER"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if(firstCommand.equals("SUBSCRIBE"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/SUBSCRIBE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if(firstCommand.equals("NOTIFY"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/NOTIFY"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("MESSAGE"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/MESSAGE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("INVITE"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/INVITE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("ACK"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/ACK"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("CANCEL"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/CANCEL"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("BYE"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else if (firstCommand.equals("PUBLISH"))
        {
            //XTTProperties.printTransaction("SIPSERVER/SIP/BYE"+XTTProperties.DELIMITER+firstLine[1]);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method is "+firstCommand);
        } else
        {
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): Method '"+firstCommand+"' is NOT SUPPORTED");
            returnCode = SIP_NOT_IMPLEMENTED;
            headersToSend.put("cseq",myReceivedHeader.get("cseq"));
            headersToSend.put("call-id",myReceivedHeader.get("call-id"));
            storeHeaders(mode,finishedNotifcation,"",myReceivedHeader);

            return true;
        }
        storeHeaders(mode,finishedNotifcation,"",myReceivedHeader);

        Vector contentlengthVector=myReceivedHeader.get("content-length");
        if(contentlengthVector!=null)
        {
            int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
            receivedData=new byte[contentlength];
            HTTPHelper.readBytes(inputStream,receivedData);
            XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
        }
        int expires=DEFAULTEXPIRY;

        String bodyPlain=ConvertLib.createString(receivedData);
        String bodyBase64=ConvertLib.base64Encode(receivedData);
        
        setMultipleVariables(finishedNotifcation,"/BODY/BASE64",bodyBase64);
        setMultipleVariables(finishedNotifcation,"/BODY/PLAIN",bodyPlain);
        
        Vector<String> v=null;
        returnCode=checkAuthentication(myReceivedHeader,headersToSend);
        if(returnCode!=SIP_OK)return true;

        v=new Vector<String>();
        v.add("XTT/SIP;build="+XTTProperties.getXTTBuildVersion());
        headersToSend.put("server",v);

        //Common for all
        headersToSend.put("cseq",myReceivedHeader.get("cseq"));
        headersToSend.put("call-id",myReceivedHeader.get("call-id"));
        headersToSend.put("to",myReceivedHeader.get("to"));
        headersToSend.put("from",myReceivedHeader.get("from"));
        headersToSend.put("via",myReceivedHeader.get("via"));

        if(skipResponse)
        {
            // Do not handle response generation when you're not sending it anyway
            return false;
        } else if(firstCommand.equals("REGISTER"))
        {
            returnCode=SIP_OK;
            headersToSend.put("contact",myReceivedHeader.get("contact"));
            /*v=new Vector<String>();
            v.add(""+expires);
            headersToSend.put("expires",v);*/
            v=new Vector<String>();
            v.add(HTTPHelper.createHTTPDate());
            headersToSend.put("date",v);
        } else if(firstCommand.equals("MESSAGE"))
        {
            // Nothing to do
        } else if(firstCommand.equals("ACK"))
        {
            // no responses for ACK
            return false;
        } else if(firstCommand.equals("PUBLISH"))
        {
            // Nothing to do
        } else if(firstCommand.equals("SUBSCRIBE"))
        {
            v=new Vector<String>();
            v.add(""+expires);
            headersToSend.put("expires",v);
            if(!skipAutoNotify)
            {
                ByteArrayWrapper notify=new ByteArrayWrapper(SIPContent.createNotify(myReceivedHeader, mode, myIPAddress, myServerPort));
                additionalPackets.add(notify);
            }
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
    
    /**
     * 
     * @param which - header  "to" or "from"
     * @param receivedHeader - header values
     * @return - address from the given headers
     */ 
    private String getAddress(String which,LinkedHashMap<String,Vector<String>> receivedHeader)
    {
        Vector<String> addresses=receivedHeader.get(which);
        if(addresses==null)return "";
        String address=addresses.get(0);
        if(address==null)return "";
        String[] addr1=address.split(">|;",2);
        String[] addr=addr1[0].split(":",2);
        return addr[1];
    }

    /**
     * 
     * @param mode - "TCP" or "UDP"
     * @param storeVar - String of response headers
     * @param where - Make sure "where" is an empty string if not used
     * @param header - Header values.
     */
    private void storeHeaders(String mode, String[] storeVar,String where,LinkedHashMap<String,Vector<String>> header)
    {
        HTTPHelper.storeHeaders("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): headers stored:",storeVar,where,header);
    }

    private static int authmethod=1;
    private static final int METHOD_DIGEST=1;
    public int checkAuthentication(LinkedHashMap<String,Vector<String>> receivedHeader, LinkedHashMap<String,Vector<String>> sendHeader)
    {
        if(disableAuthentication)return SIP_OK;
        boolean isAuthenticated=false;
        int code=SIP_OK;
        Vector<String> authheader=receivedHeader.get("authorization");
        if(authheader!=null)
        {
            String authhead=authheader.get(0);
            String[] temp=authhead.split(" ",2);
            String authmeth=temp[0];
            String paramstring=temp[1];
            LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            String[] rawparams=paramstring.split("\"\\s*,\\s*");
            for(int i=0;i<rawparams.length;i++)
            {
                temp=rawparams[i].split("=\\s*\"",2);
                if(temp[1].endsWith("\""))
                {
                    params.put(temp[0],temp[1].substring(0,temp[1].length()-1));
                } else
                {
                    params.put(temp[0],temp[1]);
                }
            }

            if(authmeth.equalsIgnoreCase("digest"))
            {
                authmethod=METHOD_DIGEST;
                String method=receivedHeader.get(null).get(0).split("\\s+",4)[0];
                String username=params.get("username");
                String uri=params.get("uri");
                String qop=params.get("qop");
                String cnonce=params.get("cnonce");
                String nc=params.get("nc");
                String response=params.get("response");
                String realm=params.get("realm");
                String nonce=params.get("nonce");
                String algorithm=params.get("algorithm");
                //System.out.println("method   :"+method);
                //System.out.println("username :"+username);
                //System.out.println("uri      :"+uri);
                //System.out.println("qop      :"+qop);
                //System.out.println("cnonce   :"+cnonce);
                //System.out.println("nc       :"+nc);
                //System.out.println("realm    :"+realm);
                //System.out.println("nonce    :"+nonce);
                //System.out.println("algorithm:"+algorithm);
                //System.out.println("response :"+response);
                try
                {
                    java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
                    byte[] h1digest=md5Hash.digest(ConvertLib.createBytes(username+":"+realm+":"+password));
                    String finalH1d=ConvertLib.outputBytes(h1digest).toLowerCase();
                    if (algorithm!=null&&algorithm.equals("md5-sess"))
                    {
                        md5Hash.reset();
                        h1digest=md5Hash.digest(ConvertLib.createBytes(finalH1d+":"+nonce+":"+cnonce));
                    }
                    //System.out.println("finalH1d :"+finalH1d);
                    md5Hash.reset();
                    byte[] h2digest=md5Hash.digest(ConvertLib.createBytes(method+":"+uri));
                    String finalH2d=ConvertLib.outputBytes(h2digest).toLowerCase();
                    //System.out.println("finalH2d :"+finalH2d);
                    md5Hash.reset();
                    md5Hash.update(ConvertLib.createBytes(finalH1d+":"+nonce+":"));
                    if(qop!=null)
                    {
                        md5Hash.update(ConvertLib.createBytes(nc+":"+cnonce+":"+qop+":"));
                    }
                    byte[] finaldigest=md5Hash.digest(ConvertLib.createBytes(finalH2d));
                    String finaldig=ConvertLib.outputBytes(finaldigest).toLowerCase();
                    //System.out.println("finaldig :"+finaldig);
                    if(finaldig.equals(response))
                    {
                        isAuthenticated=true;
                    }
                } catch (java.security.NoSuchAlgorithmException ex)
                {
                    XTTProperties.printFail("SIPWorker.checkAuthentication(): NoSuchAlgorithmException - check your java vm!");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ex);
                    }
                    return SIP_SERVER_INTERNAL_ERROR;
                }
            }
        }

        if(isAuthenticated)return code;
        if(authmethod==METHOD_DIGEST)
        {

            Vector<String> v=new Vector<String>();
            v.add("Digest realm=\"XTT SIP Authentication\", nonce=\""+ConvertLib.outputBytes(RandomLib.getRandomMD5Hash(128)).toLowerCase()+"\"");
            //v.add("NTLM realm=\"SIP Communications Service\", targetname=\"avalon.len.tantau.com\"");
            //v.add("Basic realm=\"XTT SIP Authentication\"");
            sendHeader.put("www-authenticate",v);
            sendHeader.put("via",receivedHeader.get("via"));
            sendHeader.put("from",receivedHeader.get("from"));
            sendHeader.put("to",receivedHeader.get("to"));
            sendHeader.put("cseq",receivedHeader.get("cseq"));
            sendHeader.put("call-id",receivedHeader.get("call-id"));
            //createAuth();
            code=SIP_UNAUTHORIZED;
        }
        return code;
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
        //XTTProperties.printDebug("SIPWorker."+mode+"("+myServerPort+"/"+getWorkerId()+"): "+mode+" sent Headers\n"+debug);
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

    public static void setAuthentication(boolean code)
    {
        disableAuthentication=!code;
    }
    public static void setSkipResponse(boolean code)
    {
        skipResponse=code;
    }
    public static void setSkipAutoNotify(boolean code)
    {
        skipAutoNotify=code;
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
        synchronized (cSeqKey)
        {
            cSeqCount=0;
        }
        overrideReturnCode=0;
        overrideReturnMessage=null;
        disableAuthentication=true;
        skipResponse=false;
        skipAutoNotify=false;
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(SIPServer.checkSockets())
        {
            XTTProperties.printFail("SIPWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("SIPSERVER/WAITTIMEOUT");
        if(wait<0)wait=SIPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("SIPWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("SIPWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("SIPWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(SIPServer.checkSockets())
        {
            XTTProperties.printFail("SIPWorker.waitForTimeoutRequests: no instance running!");
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
                XTTProperties.printInfo("SIPWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("SIPWorker.waitForTimeoutRequests: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("SIPWorker.waitForTimeoutRequests: request received! "+requestcount+"/"+number);
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
            case SIP_TRYING: return "Trying";
            case SIP_RINGING: return "Ringing";
            case SIP_CALL_IS_BEING_FORWARDED: return "Call is being forwarded";
            case SIP_QUEUED: return "Queued";
            case SIP_SESSION_PROGRESS: return "Session Progress";
            case SIP_OK: return "Ok";
            case SIP_ACCEPTED: return "Accepted";
            case SIP_MULTIPLE_CHOICES: return "Multiple Choices";
            case SIP_MOVED_PERMANENTLY: return "Moved Permanently";
            case SIP_MOVED_TEMPORARILY: return "Moved Temporarily";
            case SIP_USE_PROXY: return "Use Proxy";
            case SIP_ALTERNATIVE_SERVICE: return "Alternative Service";
            case SIP_BAD_REQUEST: return "Bad Request";
            case SIP_UNAUTHORIZED: return "Unauthorized";
            case SIP_PAYMENT_REQUIRED: return "Payment Required";
            case SIP_FORBIDDEN: return "Forbidden";
            case SIP_NOT_FOUND: return "Not Found";
            case SIP_METHOD_NOT_ALLOWED: return "Method Not Allowed";
            case SIP_NOT_ACCEPTABLE: return "Not Acceptable";
            case SIP_PROXY_AUTHENTICATION_REQUIRED: return "Proxy Authentication Required";
            case SIP_REQUEST_TIMEOUT: return "Request Timeout";
            case SIP_GONE: return "Gone";
            case SIP_REQUEST_ENTITY_TOO_LARGE: return "Request Entity Too Large";
            case SIP_REQUEST_URI_TOO_LONG: return "Request-URI Too Long";
            case SIP_UNSUPPORTED_MEDIA_TYPE: return "Unsupported Media Type";
            case SIP_UNSUPPORTED_URI_SCHEME: return "Unsupported URI Scheme";
            case SIP_BAD_EXTENSION: return "Bad Extension";
            case SIP_EXTENSION_REQUIRED: return "Extension Required";
            case SIP_INTERVAL_TOO_BRIEF: return "Interval Too Brief";
            case SIP_TEMPORARILY_UNAVAILABLE: return "Temporarily Unavailable";
            case SIP_CALL_TRANSACTION_DOES_NOT_EXIST: return "Call/Transaction Does Not Exist";
            case SIP_LOOP_DETECTED: return "Loop Detected";
            case SIP_TOO_MANY_HOPS: return "Too Many Hops";
            case SIP_ADDRESS_INCOMPLETE: return "Address Incomplete";
            case SIP_AMBIGUOUS: return "Ambiguous";
            case SIP_BUSY_HERE: return "Busy Here";
            case SIP_REQUEST_TERMINATED: return "Request Terminated";
            case SIP_NOT_ACCEPTABLE_HERE: return "Not Acceptable Here";
            case SIP_REQUEST_PENDING: return "Request Pending";
            case SIP_UNDECIPHERABLE: return "Undecipherable";
            case SIP_SERVER_INTERNAL_ERROR: return "Server Internal Error";
            case SIP_NOT_IMPLEMENTED: return "Not Implemented";
            case SIP_BAD_GATEWAY: return "Bad Gateway";
            case SIP_SERVICE_UNAVAILABLE: return "Service Unavailable";
            case SIP_SERVER_TIMEOUT: return "Server Time-out";
            case SIP_VERSION_NOT_SUPPORTED: return "Version Not Supported";
            case SIP_MESSAGE_TOO_LARGE: return "Message Too Large";
            case SIP_BUSY_EVERYWHERE: return "Busy Everywhere";
            case SIP_DECLINE: return "Decline";
            case SIP_DOES_NOT_EXIST_ANYWHERE: return "Does Not Exist Anywhere";
            case SIP_GLOBAL_NOT_ACCEPTABLE: return "Not Acceptable";
            default: return "Unknown";
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: SIPWorker.java,v 1.25 2010/03/18 05:30:40 rajesh Exp $";
}
