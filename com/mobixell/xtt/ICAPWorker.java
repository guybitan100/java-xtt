package com.mobixell.xtt;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * <p>ICAPWorker</p>
 * <p>Processes an ICAP request which has been received by the ICAPServer</p>
 * <p>Flush levels if flushLevel>0, Level is 2^(Level1-1)+2^(Level2-1)+2^2+...<code><pre>
 *                ICAP/1.0 200 OK(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (2)      Date:(7) Mon, 10 Jan 2000  09:55:21 GMT(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Server:(7) ICAP-Server-Software/1.0(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Connection:(7) close(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                ISTag:(7) "W3E4R7U9-L2E4-2"(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Encapsulated:(7) res-hdr=0, res-body=222(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (3)      (5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (1)      HTTP/1.1 200 OK(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (2)      Date:(7) Mon, 10 Jan 2000  09:55:21 GMT(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Via:(7) 1.0 icap.example.org (ICAP Example RespMod Service 1.1)(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Server:(7) Apache/1.3.6 (Unix)(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                ETag:(7) "63840-1ab7-378d415b"(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Content-Type:(7) text/html(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *                Content-Length:(7) 92(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (3)      (5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (123)    3c(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (2)      This is data that was returned by an origin server, but with(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (123)    1f(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (2)      value added by an ICAP server.(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (123)    0(5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (3)      (5)&lt;CR&gt;(6)&lt;LF&gt;(4)
 *       (1234567)
 *       </pre></code>
 * </p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Gavin Cattell & Roger Soder
 * @version $Id: ICAPWorker.java,v 1.12 2010/03/18 12:15:45 rajesh Exp $
 */
public class ICAPWorker extends Thread implements ICAPConstants
{
    private boolean stop = false;
    private int id;

    //private static LinkedHashMap<String,String> postData=null;
    private static LinkedHashMap<String,Vector<String>> receivedServerICAPHeader = new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,Vector<String>> receivedServerREQHeader  = new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,Vector<String>> receivedServerRESPHeader = new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,String> sendServerICAPHeader = new LinkedHashMap<String,String>();
    private static LinkedHashMap<String,String> sendServerREQHeader  = new LinkedHashMap<String,String>();
    private static LinkedHashMap<String,String> sendServerRESPHeader = new LinkedHashMap<String,String>();
    private static LinkedHashMap<String,String> sendServerOptions    = new LinkedHashMap<String,String>();
    private LinkedHashMap<String,String> optionsToSend = new LinkedHashMap<String,String>();
    private LinkedHashMap<String,String> headersToSend = new LinkedHashMap<String,String>(); //Not static, this has to be different for each request
    private String protocol = "";
    private String method=null;
    private static String recievedURL="null";
    private String myRecievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    private int returnCode = ICAP_SERVER_ERROR;
    private String returnMessage = "";
    private static int overrideReturnCode=0;
    private static String overrideReturnMessage="";
    private static String overrideType=null;
    private static byte[] overrideHTTPHeaders=null;
    private static Vector<ByteArrayWrapper> overrideHTTPBody=null;

    private static final String serverName="XTT JAVA ICAPServer $Revision: 1.12 $";

    static final byte[] CRLF = {0x0D,0x0A};
    static final byte[] CR = {0x0D};
    static final byte[] LF = {0x0A};

    private static Object requestkey=new Object();
    private static int requestcount=0;

    private static Object reqkey=new Object();
    private static int reqcount=0;

    private static Object respkey=new Object();
    private static int respcount=0;

    private static Object optkey=new Object();
    private static int optcount=0;

    byte[] receivedData         = null; //where we  store the bytes of the POST
    int    receivedBodyStart    = 0;    //tells us where the body starts
    int    receivedPayloadBodyStart = 0;    //tells us where the body to transcode starts
    int    receivedPayloadBodyStop  = 0;    //tells us where the body to transcode stops

    private static final int LEVEL1= 1;
    private static final int LEVEL2= 2;
    private static final int LEVEL3= 4;
    private static final int LEVEL4= 8;
    private static final int LEVEL5=16;
    private static final int LEVEL6=32;
    private static final int LEVEL7=64;
    private static       int flushLevel=0;


    /* Socket to client we're handling, which will be set by the ICAPServer
       when dispatching the request to us */
    private Socket s = null;
    private ICAPServer myServer=null;
    private int myTimeout=600000;
    private String myServerPort="-";
    private static String myHost=null;

    private static String defaultMethod="REQMOD";
    private static boolean enableFullStreaming=true;
    private boolean myEnableFullStreaming=enableFullStreaming;

    private static boolean default_keep_alive=false;
    private boolean keep_alive=false;
    private int MINLAGDELAY=100;

    /**
     * Creates a new ICAPWorker
     * @param id     ID number of this worker thread
     */
    public ICAPWorker(int id, Socket setSocket, ICAPServer server, int timeout,File root)
    {
        super("ICAPWorker-"+id);
        this.s = setSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=server;
        myHost=setSocket.getLocalAddress().getCanonicalHostName();
        if(myServer==null)
        {
            myServerPort="-";
        } else
        {
            myServer.addWorker();
            myServerPort=myServer.getPort()+"";
        }
    }

    public int getWorkerId() {
        return id;
    }

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        this.stop = true;
        XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        try
        {
            this.s.close();
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
            handleClient();
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
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
                XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
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
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
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
                XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
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
    public void handleClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instance "+instances);
            key.notify();
        }

        myEnableFullStreaming=enableFullStreaming;

        BufferedInputStream in = new BufferedInputStream(s.getInputStream(),65536);

        StreamSplit out=null;
        ByteArrayOutputStream outBuffer=new ByteArrayOutputStream();;
        OutputStream outDirect=s.getOutputStream();
        if(flushLevel>0)
        {
            outDirect=new BufferedOutputStream(outDirect,65536);
        }
        if(myEnableFullStreaming)
        {
            out=new StreamSplit(outDirect);
        } else
        {
            out=new StreamSplit(outBuffer);
        }
        String thisConnection="close";

        s.setSoTimeout(myTimeout);
        //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
        s.setTcpNoDelay(true);

        boolean runbefore=false;

        try
        {
        KEEPALIVE:do
        {
            headersToSend=new LinkedHashMap<String,String>();
            headersToSend.put("istag"           ,'"'+ConvertLib.outputBytes(RandomLib.getRandomMD5Hash(128))+'"');
            headersToSend.put("date"            ,HTTPHelper.createHTTPDate());
            headersToSend.put("connection"      ,thisConnection);
            optionsToSend.put("server"          ,serverName);

            returnCode = ICAP_OK;
            myRecievedURL="null";

            optionsToSend=new LinkedHashMap<String,String>();
            optionsToSend.put("methods"         ,defaultMethod);
            optionsToSend.put("service"         ,"XTT JAVA ICAPServer $Revision: 1.12 $");
            optionsToSend.put("istag"           ,'"'+ConvertLib.outputBytes(RandomLib.getRandomMD5Hash(128))+'"');
            optionsToSend.put("encapsulated"    ,"null-body=0");
            optionsToSend.put("max-connections" ,"20");
            optionsToSend.put("options-ttl"     ,"10");
            optionsToSend.put("allow"           ,"204");
            optionsToSend.put("preview"         ,"4096");
            optionsToSend.put("transfer-complete","asp, bat, exe, com");
            optionsToSend.put("transfer-ignore" ,"html");
            optionsToSend.put("transfer-preview","*");

            String encapsulated=null;
            int reqheaderstart=-1;
            int resheaderstart=-1;
            int bodystart=-1;
            boolean hasbody=false;

            int networklagdelay=XTTProperties.getNetworkLagDelay();
            if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;

            if(runbefore)
            {
                XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): Client keep-alive - receiving");
            } else
            {
                XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): Client connected - receiving");
            }

            LinkedHashMap<String,Vector<String>> serverHeader=HTTPHelper.readHTTPStreamHeaders("ICAPWorker("+myServerPort+"/"+getWorkerId()+")",in);

            if(serverHeader.get(null)==null)
            {
                XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): no header received, possible disconnect");
                return;
            }
            String methodLine=serverHeader.get(null).get(0);
            String firstLine[]=null;
            synchronized(receivedServerICAPHeader)
            {
                receivedServerICAPHeader.clear();
                receivedServerICAPHeader.putAll(serverHeader);
                firstLine=methodLine.split("\\s+",4);
                method=firstLine[0];
                myRecievedURL=firstLine[1];
                recievedURL=myRecievedURL;
                protocol = firstLine[2];
                // Lets figure out which method we have
                if(method.equals("REQMOD"))
                {
                    XTTProperties.printTransaction("ICAPSERVER/ICAP/REQMOD"+XTTProperties.DELIMITER+firstLine[1]);
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+method);
                } else if(method.equals("RESPMOD"))
                {
                    XTTProperties.printTransaction("ICAPSERVER/ICAP/RESPMOD"+XTTProperties.DELIMITER+firstLine[1]);
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+method);
                } else if(method.equals("OPTIONS"))
                {
                    XTTProperties.printTransaction("ICAPSERVER/ICAP/OPTIONS"+XTTProperties.DELIMITER+firstLine[1]);
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+method);
                } else
                {
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): Method '"+method+"' is NOT SUPPORTED");
                    returnCode = ICAP_METHOD_NOT_ALLOWED;
                }
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): fileName="+firstLine[1]);
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): protocol="+protocol);
                String keepReason="";
                // If we have a connection header change the keep-alive mode, else do it by the default
                if (serverHeader.get("connection")!=null&&serverHeader.get("connection").get(0).equalsIgnoreCase("keep-alive"))
                {
                    keep_alive=true;
                    keepReason="Connection=keep-alive";
                    thisConnection="keep-alive";
                } else if (serverHeader.get("connection")!=null&&serverHeader.get("connection").get(0).equalsIgnoreCase("close"))
                {
                    keep_alive=false;
                    keepReason="Connection=close";
                    thisConnection="close";
                } else if(default_keep_alive==true)
                {
                    keep_alive=true;
                    keepReason="default=keep-alive";
                    thisConnection="keep-alive";
                } else if(default_keep_alive==false)
                {
                    keep_alive=false;
                    keepReason="default=close";
                    thisConnection="close";
                }
                headersToSend.put("connection",thisConnection);
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+keepReason+" - Keep-Alive set to "+keep_alive);

                // Lets figure out if we have body and which ehaders are present by parsing the encapsulated header
                if(serverHeader.get("encapsulated")!=null)
                {
                    encapsulated=serverHeader.get("encapsulated").get(0);
                    String[] encs=encapsulated.split("[,+\\s]+");
                    String[] vals=null;
                    StringBuffer output=new StringBuffer("Encapsulated:");
                    for(int i=0;i<encs.length;i++)
                    {
                        vals=encs[i].split("=");
                        if(vals[0].equals("req-hdr"))
                        {
                            reqheaderstart=Integer.decode(vals[1]);
                            output.append(" REQ:"+reqheaderstart);
                        } else if (vals[0].equals("res-hdr"))
                        {
                            resheaderstart=Integer.decode(vals[1]);
                            output.append(" RES:"+resheaderstart);
                        } else if (vals[0].equals("req-body"))
                        {
                            bodystart=Integer.decode(vals[1]);
                            hasbody=true;
                            output.append(" REQBODY:"+bodystart+"/"+hasbody);
                        } else if (vals[0].equals("res-body"))
                        {
                            bodystart=Integer.decode(vals[1]);
                            hasbody=true;
                            output.append(" RESBODY:"+bodystart+"/"+hasbody);
                        } else if (vals[0].equals("opt-body"))
                        {
                            bodystart=Integer.decode(vals[1]);
                            hasbody=true;
                            output.append(" OPTBODY:"+bodystart+"/"+hasbody);
                        } else if (vals[0].equals("null-body"))
                        {
                            bodystart=Integer.decode(vals[1]);
                            hasbody=false;
                            output.append(" NULLBODY:"+bodystart+"/"+hasbody);
                        }
                        //System.out.println(encs[i]+" '"+vals[0]+"'");
                    }
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+output);
                } else
                {
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): NO encapsulated header present");
                    hasbody=false;
                }
            }
            if(overrideReturnCode>0)
            {
                returnCode=overrideReturnCode;
                returnMessage=overrideReturnMessage;
            }
            if(returnMessage.equals(""))returnMessage=getReturnMessage(returnCode);
            byte[] body=null;
            Vector<String> preview=null;
            // If we have a preview header enable the preview
            if(serverHeader.get("preview")!=null)
            {
                preview=new Vector<String>();
            }
            LinkedHashMap<String,Vector<String>> reqHeader=null;
            LinkedHashMap<String,Vector<String>> resHeader=null;
            LinkedHashMap<String,String>     sendHttpHeader=null;
            LinkedHashMap<String,Vector<String>> httpHeader=null;
            String bodytype="";
            String encBodytype="null-body";
            // Read resp or reqmod headers
            if(method.equals("REQMOD")||method.equals("RESPMOD"))
            {
                // REQMOD mode, read Request headers
                if(method.equals("REQMOD"))
                {
                    bodytype="REQ";
                    encBodytype="req-body";
                    reqHeader=HTTPHelper.readHTTPStreamHeaders("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"HEADER",in);
                    if(reqHeader.containsKey(""))
                    {
                        reqHeader.remove(reqHeader.get("").get(0));   
                    }
                    httpHeader=reqHeader;
                    sendHttpHeader=sendServerREQHeader;
                    synchronized(receivedServerREQHeader)
                    {
                        receivedServerREQHeader.clear();
                        receivedServerREQHeader.putAll(reqHeader);
                    }
                // RESPMOD mode, read resp and/or req headers
                } else if(method.equals("RESPMOD"))
                {
                    bodytype="RES";
                    encBodytype="res-body";
                    if(reqheaderstart>=0)
                    {
                        reqHeader=HTTPHelper.readHTTPStreamHeaders("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"HEADER",in);
                        if(reqHeader.containsKey(""))
                        {
                            reqHeader.remove(reqHeader.get("").get(0));
                        }
                        synchronized(receivedServerREQHeader)
                        {
                            receivedServerREQHeader.clear();
                            receivedServerREQHeader.putAll(reqHeader);
                        }
                    }
                    if(resheaderstart>=0)
                    {
                        resHeader=HTTPHelper.readHTTPStreamHeaders("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"HEADER",in);
                        if(resHeader.containsKey(""))
                        {
                            resHeader.remove(resHeader.get("").get(0));
                        }
                        httpHeader=resHeader;
                        sendHttpHeader=sendServerRESPHeader;
                        synchronized(receivedServerRESPHeader)
                        {
                            receivedServerRESPHeader.clear();
                            receivedServerRESPHeader.putAll(resHeader);
                        }
                    }
                }

                // Decide what to send back and how to read the body
                if(hasbody)
                {
                    // If we are in preview mode
                    if(preview!=null)
                    {
                        // sadly we need to read the WHOLE preview first before we can continue.
                        body=HTTPHelper.readChunkedBody("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY",in,reqHeader,preview);
                        // So lets decide if we do a 204 no modification or if we continue reading the rest
                        if(returnCode!=ICAP_NO_MODIFICATION&&returnCode<300)
                        {
                            // Client indicated there is no rest so send response and finish
                            if(preview!=null&&preview.contains("ieof"))
                            {
                                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: Preview "+body.length+" bytes ended with ieof");

                                // send headers and received body only
                                writeHeaders(out,bodytype,encBodytype,httpHeader,sendHttpHeader);

                                out.flush(LEVEL1+LEVEL2+LEVEL3);
                                out.print(ConvertLib.intToHex(body.length));
                                out.writeCRLF();
                                out.flush(LEVEL2);
                                out.write(body);
                                out.writeCRLF();
                                out.flush(LEVEL1+LEVEL2+LEVEL3);
                                out.print(ConvertLib.intToHex(0));
                                out.writeCRLF();
                                out.flush(LEVEL3);
                                out.writeCRLF();
                                if(myEnableFullStreaming)
                                {
                                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: written chunk, length: "+out.getOutput());
                                    out.clearOutput();
                                }
                            // Send what we have and then read the rest until finished
                            } else
                            {
                                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: Preview "+body.length+" bytes ended normally, 100 continue");

                                // SEND 100 continue.
                                outDirect.write(ConvertLib.createBytes("ICAP/1.0 "+ICAP_CONTINUE+" CONTINUE"));
                                if((flushLevel&LEVEL6)>0)
                                {
                                    outDirect.flush();
                                    Thread.yield();
                                }
                                // CRLF
                                if((flushLevel&LEVEL5)>0)
                                {
                                    outDirect.flush();
                                    Thread.yield();
                                }
                                if((flushLevel&LEVEL6)>0)
                                {
                                    outDirect.write(CR);
                                    outDirect.flush();
                                    Thread.yield();
                                    outDirect.write(LF);
                                } else
                                {
                                    outDirect.write(CRLF);
                                }
                                if((flushLevel&LEVEL4)>0)
                                {
                                    outDirect.flush();
                                    Thread.yield();
                                }
                                // CRLF
                                if((flushLevel&LEVEL5)>0)
                                {
                                    outDirect.flush();
                                    Thread.yield();
                                }
                                if((flushLevel&LEVEL6)>0)
                                {
                                    outDirect.write(CR);
                                    outDirect.flush();
                                    Thread.yield();
                                    outDirect.write(LF);
                                } else
                                {
                                    outDirect.write(CRLF);
                                }
                                if((flushLevel&LEVEL4)>0)
                                {
                                    outDirect.flush();
                                    Thread.yield();
                                }
                                // HEADERS
                                writeHeaders(out,bodytype,encBodytype,httpHeader,sendHttpHeader);
                                out.flush(LEVEL1+LEVEL2+LEVEL3);
                                out.print(ConvertLib.intToHex(body.length));
                                out.writeCRLF();
                                out.flush(LEVEL2);
                                out.write(body);
                                out.writeCRLF();
                                if(myEnableFullStreaming)
                                {
                                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: written chunk, length: "+out.getOutput());
                                    out.clearOutput();
                                }
                                readWriteBody(out,bodytype,in);
                            }
                        // We dio 204 no modification
                        } else
                        {
                            sendNoModification(out,bodytype);
                        }
                    // We are in no preview direct mode
                    } else
                    {
                        // If not 204 requested write the headers and read/write the body
                        if(returnCode!=ICAP_NO_MODIFICATION&&returnCode<300)
                        {
                            writeHeaders(out,bodytype,encBodytype,httpHeader,sendHttpHeader);
                            readWriteBody(out,bodytype,in);
                        } else
                        {
                            sendNoModification(out,bodytype);
                        }
                    }
                } else
                {
                    if(returnCode!=ICAP_NO_MODIFICATION&&returnCode<300)
                    {
                        encBodytype="null-body";
                        writeHeaders(out,bodytype,encBodytype,httpHeader,sendHttpHeader);
                    } else
                    {
                        sendNoModification(out,bodytype);
                    }
                }
            // We have options so send the response and options headers
            } else if(method.equals("OPTIONS"))
            {
                returnCode=ICAP_OK;
                returnMessage=getReturnMessage(returnCode);
                out.print("ICAP/1.0 "+returnCode+" "+returnMessage);
                out.flush(LEVEL2);
                out.writeCRLF();
                //headersToSend.putAll(sendServerICAPHeader);
                String method=myServer.getMethod(recievedURL);
                if(method!=null)
                {
                    optionsToSend.put("methods",method);
                } else
                {
                    optionsToSend.put("methods",defaultMethod);
                }
                headersToSend.putAll(optionsToSend);
                headersToSend.putAll(sendServerOptions);
                out.printFlatHeaders(headersToSend);
                if(myEnableFullStreaming)
                {
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): OPTIONS: written\n"+out.getOutput());
                    out.clearOutput();
                }
            }
            if(!myEnableFullStreaming)
            {
                if(overrideHTTPHeaders!=null&&!method.equals("OPTIONS"))
                {
                    ByteArrayOutputStream newBuffer=new ByteArrayOutputStream();;
                    out=new StreamSplit(newBuffer);
                    writeOverrideData(out,bodytype,encBodytype);
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): sent override response (view mixed: plain/hex view):\n"+out.getOutput());
                    outDirect.write(newBuffer.toByteArray());
                } else
                {
                    XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): sent response (view mixed: plain/hex view):\n"+out.getOutput());
                    outDirect.write(outBuffer.toByteArray());
                }
            }
            outDirect.flush();
            Thread.yield();
            synchronized (requestkey)
            {
                requestcount++;
                requestkey.notifyAll();
            }
            if(method.equals("REQMOD"))
            {
                synchronized (reqkey)
                {
                    reqcount++;
                    reqkey.notifyAll();
                }
            } else if(method.equals("RESPMOD"))
            {
                synchronized (respkey)
                {
                    respcount++;
                    respkey.notifyAll();
                }
            } else if(method.equals("OPTIONS"))
            {
                synchronized (optkey)
                {
                    optcount++;
                    optkey.notifyAll();
                }
            }
            runbefore=true;
            } while(keep_alive);
        } finally
        {
            s.close();
            XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    // Send the 204 no modification
    private void sendNoModification(StreamSplit out,String method)  throws java.io.IOException
    {
        headersToSend.put("encapsulated","null-body=0");
        out.print("ICAP/1.0 "+returnCode+" "+returnMessage);
        out.flush(LEVEL2);
        out.writeCRLF();
        headersToSend.putAll(sendServerICAPHeader);
        out.printFlatHeaders(headersToSend);
        if(myEnableFullStreaming)
        {
            XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+method+"HEADER: written "+returnMessage+" headers:\n"+out.getOutput());
            out.clearOutput();
        }
    }

    /*
    * Write the override headers and body to the stream, first ICAP, then HTTP either request or response headers closing with the body in chunks.
    * Creates the encapsulated headers correctly.
    */
    private void writeOverrideData(StreamSplit out,String method, String encBodytype) throws java.io.IOException
    {
        if(overrideType!=null)
        {
            method=overrideType;
            encBodytype=overrideType+"-body";
        }
        if(overrideHTTPBody==null)encBodytype="null-body";
        headersToSend.put("encapsulated",method.toLowerCase()+"-hdr=0, "+encBodytype+"="+overrideHTTPHeaders.length);

        out.print("ICAP/1.0 "+returnCode+" "+returnMessage);
        out.flush(LEVEL2);
        out.writeCRLF();
        headersToSend.putAll(sendServerICAPHeader);
        out.printFlatHeaders(headersToSend);
        out.write(overrideHTTPHeaders);
        if(overrideHTTPBody!=null)
        {
            byte[] chunk=null;
            Iterator<ByteArrayWrapper> it=overrideHTTPBody.iterator();
            while(it.hasNext())
            {
                chunk=it.next().getArray();
                out.flush(LEVEL1+LEVEL2+LEVEL3);
                out.print(ConvertLib.intToHex(chunk.length));
                out.writeCRLF();
                out.flush(LEVEL2);
                out.write(chunk);
                out.writeCRLF();
            }
            out.flush(LEVEL1+LEVEL2+LEVEL3);
            out.print(ConvertLib.intToHex(0));
            out.writeCRLF();
            out.flush(LEVEL3);
            out.writeCRLF();
        }

    }

    /*
    * Write the headers to the stream, first ICAP, then HTTP either request or response headers.
    * Creates the encapsulated headers correctly.
    */
    private void writeHeaders(StreamSplit out,String method, String encBodytype,LinkedHashMap<String,Vector<String>> httpheader,LinkedHashMap<String,String> sendHttpHeader)  throws java.io.IOException
    {
        ByteArrayOutputStream tempHeaderBuffer=new ByteArrayOutputStream();
        StreamSplit tempHeader=new StreamSplit(tempHeaderBuffer);

        mergeHeaders(httpheader,sendHttpHeader);
        alterViaHeader(httpheader);
        tempHeader.printDeepHeaders(httpheader);
        headersToSend.put("encapsulated",method.toLowerCase()+"-hdr=0, "+encBodytype+"="+tempHeaderBuffer.toByteArray().length);

        out.print("ICAP/1.0 "+returnCode+" "+returnMessage);
        out.flush(LEVEL2);
        out.writeCRLF();
        headersToSend.putAll(sendServerICAPHeader);
        out.printFlatHeaders(headersToSend);
        out.printDeepHeaders(httpheader);
        if(myEnableFullStreaming)
        {
            XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+method+"HEADER: written headers:\n"+out.getOutput()+"EOF headers");
            out.clearOutput();
        }
    }

    /*
    * Read a single chunk and then write it to the outgoing stream. When a chunk of size 0 is receifed close by writing the
    * chunk ending and trailer headers.
    */
    private void readWriteBody(StreamSplit out,String bodytype, BufferedInputStream in) throws java.io.IOException
    {
        byte[] chunk=null;
        Vector<String> chunkOptions=null;
        LinkedHashMap<String,Vector<String>> trailerHeader=new LinkedHashMap<String,Vector<String>>();
        do
        {
            chunkOptions=new Vector<String>();
            chunk=HTTPHelper.readSingleChunk("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY",in,trailerHeader,chunkOptions);
            out.flush(LEVEL1+LEVEL2+LEVEL3);
            out.print(ConvertLib.intToHex(chunk.length));
            if(chunkOptions.size()>0)
            {
                out.print("; "+chunkOptions.get(0));
            }
            out.writeCRLF();
            if(chunk.length>0)
            {
                out.flush(LEVEL2);
                out.write(chunk);
                out.writeCRLF();
            }
            if(myEnableFullStreaming)
            {
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: written chunk, length: "+out.getOutput());
                out.clearOutput();
            }
        } while(chunk.length!=0);
        //Trailer Headers
        out.printDeepHeaders(trailerHeader);
        if(myEnableFullStreaming)
        {
            XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): "+bodytype+"BODY: written final data:\n"+out.getOutput());
        }
    }

    /* Merge the Flat headers (second val) onto the Deep headers (first val). Replaces already present values.
     *
     */
    private void mergeHeaders(LinkedHashMap<String,Vector<String>> headerTo,LinkedHashMap<String,String> header)
    {
            Iterator<String> it=header.keySet().iterator();
            String headerKey=null;
            String headerValue=null;
            Vector<String> vals=null;
            while(it.hasNext())
            {
                headerKey=it.next();
                headerValue = header.get(headerKey);
                vals=new Vector<String>();
                vals.add(headerValue);
                headerTo.put(headerKey.toLowerCase(),vals);
            }
    }

    // Change/Add the via header of the HTTP Request/Response
    private void alterViaHeader(LinkedHashMap<String,Vector<String>> header)
    {
        Vector<String> vals=header.get("via");
        if(vals==null)
        {
            vals=new Vector<String>();
            vals.add("ICAP/1.0 "+myHost+" ("+serverName+")");
            header.put("via",vals);
        } else
        {
            String via=vals.get(0);
            via=via+", ICAP/1.0 "+myHost+" ("+serverName+")";
            vals.clear();
            vals.add(via);
            header.put("via",vals);
        }
    }

    // Class to handle debug output and writing of the stream
    private class StreamSplit
    {
        OutputStream outStream=null;
        StringBuffer output=new StringBuffer("");
        public StreamSplit(OutputStream outS)
        {
            outStream=outS;
        }
        public void flush(int level) throws java.io.IOException
        {
            if((flushLevel&(level))>0)
            {
                outStream.flush();
                Thread.yield();
            }
        }


        // Create bytes from the String and write to the stream
        public void print(String data) throws java.io.IOException
        {
            outStream.write(ConvertLib.createBytes(data));
            output.append(data);
        }

        /**
         * Writes The headers "key1: value1+CRLF+key2: value2+CRLF" etc. Addes empty line at the end.
         * Deep headers are key/Vector of values type
        * If there are no headers the EMPTY CR/LF line at the end will only be written
         */
        public void printDeepHeaders(LinkedHashMap<String,Vector<String>> header) throws java.io.IOException
        {
            Iterator<String> it=header.keySet().iterator();
            Iterator<String> vals=null;
            String headerKey=null;
            Vector<String> headerValues=null;
            String headerValue=null;
            while(it.hasNext())
            {
                headerKey=it.next();
                headerValues = header.get(headerKey);
                if(headerKey!=null)
                {
                    print(headerKey+":");
                    flush(LEVEL7);
                    vals=headerValues.iterator();
                    while(vals.hasNext())
                    {
                        headerValue=vals.next();
                        print(" "+headerValue);
                        writeCRLF();
                    }
                } else
                {
                    vals=headerValues.iterator();
                    while(vals.hasNext())
                    {
                        headerValue=vals.next();
                        print(headerValue);
                        writeCRLF();
                    }
                    flush(LEVEL2);
                }
            }
            flush(LEVEL3);
            writeCRLF();
            flush(LEVEL1);
        }

        /* Flat Headers are direct key/value hash maps pairs
        * If there are no headers the EMPTY CR/LF line at the end will only be written
        */
        public void printFlatHeaders(LinkedHashMap<String,String> header) throws java.io.IOException
        {
            Iterator<String> it=header.keySet().iterator();
            String headerKey=null;
            String headerValue=null;
            while(it.hasNext())
            {
                headerKey=it.next();
                headerValue = header.get(headerKey);
                if(headerKey!=null)
                {
                    print(headerKey+":");
                    flush(LEVEL7);
                    print(" "+headerValue);
                    writeCRLF();
                } else
                {
                    print(headerValue);
                    flush(LEVEL2);
                    writeCRLF();
                }
            }
            flush(LEVEL3);
            writeCRLF();
            flush(LEVEL1);
        }
        public void write(byte[] data) throws IOException
        {
            outStream.write(data);
            output.append(ConvertLib.getHexView(data));
        }
        public void writeCRLF() throws IOException
        {
            flush(LEVEL5);
            if((flushLevel&LEVEL6)>0)
            {
                outStream.write(CR);
                outStream.flush();
                Thread.yield();
                outStream.write(LF);
            } else
            {
                outStream.write(CRLF);
            }
            flush(LEVEL4);
            output.append("\n");
        }
        public String getOutput()
        {
            return output.toString();
        }
        public void clearOutput()
        {
            output=new StringBuffer("");
        }
    }

/* // Leftover from ICAPWorker //
    private byte[] sendFile(String fname)// throws IOException
    {
        try
        {
            File targ = new File(myRoot,fname);
            if (targ.isDirectory())
            {
                XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): directories not supported");
                returnCode=ICAP_SERVER_ERROR;
                return new byte[0];
            }

            ByteArrayWrapper wrap=(ByteArrayWrapper)fileCache.get(myRoot+"/"+fname);
            byte[] thefile=null;
            if(wrap==null)
            {
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from DISK");
                thefile=new byte[(int)targ.length()];
                FileInputStream is=null;
                try
                {
                    is = new FileInputStream(targ.getAbsolutePath());
                    HTTPHelper.readBytes(is,thefile);
                } finally
                {
                    is.close();
                }
                fileCache.put(myRoot+"/"+fname,new ByteArrayWrapper(thefile));
            }
            else
            {
                XTTProperties.printDebug("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from CACHE");
                thefile=wrap.getArray();
            }
            return thefile;
        } catch(Exception e)
        {
            XTTProperties.printFail("ICAPWorker("+myServerPort+"/"+getWorkerId()+"): exception when getting file");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            returnCode=ICAP_NOT_FOUND;
            return new byte[0];
        }
    }

    public static void setCacheFile(String root,String fname, String content)
    {
        if(root.endsWith("/"))root=root.substring(0,root.length()-1);
        try
        {
            URI url = new URI(fname);
            fname = url.getPath();
        } catch(Exception e){}
        fname = fname.replace('/', File.separatorChar);
        if (fname.startsWith(File.separator))
        {
            fname = fname.substring(1);
        }
        byte[] thefile=ConvertLib.getOctetByteArrayFromString(content);
        XTTProperties.printInfo("ICAPWorker.setCacheFile: store content to cache: '"+root+"/"+fname+"' "+thefile.length+" bytes");
        fileCache.put(root+"/"+fname,new ByteArrayWrapper(thefile));
    }
    public static void clearCache()
    {
        XTTProperties.printInfo("ICAPWorker.setCacheFile: clearing file cache");
        fileCache = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());
    }

    private int getFileLength(String fname, File targ)
    {
        ByteArrayWrapper wrap=(ByteArrayWrapper)fileCache.get(myRoot+"/"+fname);
        if(wrap==null)
        {
            return (int)targ.length();
        } else
        {
            return wrap.getArray().length;
        }
    }
*/
    public static LinkedHashMap<String,Vector<String>> getServerICAPHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerICAPHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerICAPHeader);
        }
        return returnMap;
    }
    public static LinkedHashMap<String,Vector<String>> getServerREQHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerREQHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerREQHeader);
        }
        return returnMap;
    }
    public static LinkedHashMap<String,Vector<String>> getServerRESPHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerRESPHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerRESPHeader);
        }
        return returnMap;
    }

    public static LinkedHashMap<String,String> getServerSendICAPHeader()
    {
        return sendServerICAPHeader;
    }
    public static void setServerSendICAPHeader(LinkedHashMap<String,String> serverHeader)
    {
        sendServerICAPHeader=serverHeader;
    }
    public static LinkedHashMap<String,String> getServerSendREQHeader()
    {
        return sendServerREQHeader;
    }
    public static void setServerSendREQHeader(LinkedHashMap<String,String> serverHeader)
    {
        sendServerREQHeader=serverHeader;
    }
    public static LinkedHashMap<String,String> getServerSendRESPHeader()
    {
        return sendServerRESPHeader;
    }
    public static void setServerSendRESPHeader(LinkedHashMap<String,String> serverHeader)
    {
        sendServerRESPHeader=serverHeader;
    }
    public static LinkedHashMap<String,String> getServerSendOptions()
    {
        return sendServerOptions;
    }
    public static void setServerSendOptions(LinkedHashMap<String,String> serverOptions)
    {
        sendServerOptions=serverOptions;
    }
    public static void setDefaultMethod(String method)
    {
        method=method.toUpperCase();
        if(method!=null&&(method.equals("REQMOD")||method.equals("RESPMOD")))
        {
            defaultMethod=method;
        } else
        {
            XTTProperties.printFail("ICAPWorker.setDefaultMethod: unsupported method: "+method);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    public static void setDefaultKeepAlive(boolean keep)
    {
        default_keep_alive=keep;
    }
    public static void setOverrideReturnCode(int code, String message)
    {
        overrideReturnCode=code;
        overrideReturnMessage=message;
    }

    public static void setOverrideHTTP(byte[] heads, Vector<ByteArrayWrapper> body,String type)
    {
        if(enableFullStreaming)
        {
            throw new IllegalStateException("Override not allowed when in full streaming mode!");
        }
        overrideHTTPHeaders=heads;
        overrideHTTPBody=body;
        overrideType=type;
    }

    public static void setEnableFullStreaming(boolean state)
    {
        enableFullStreaming=state;
    }

    public static void setFlushLevel(int level)
    {
        if(!enableFullStreaming)
        {
            throw new IllegalStateException("FlushLevel only allowed when in full streaming mode!");
        }
        flushLevel=level;
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
        synchronized (reqkey)
        {
            reqcount=0;
        }
        synchronized (respkey)
        {
            respcount=0;
        }
        synchronized (optkey)
        {
            optcount=0;
        }
        overrideReturnCode=0;
        overrideReturnMessage="";
        overrideHTTPHeaders=null;
        overrideHTTPBody=null;
        overrideType=null;
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("ICAPSERVER/WAITTIMEOUT");
        if(wait<0)wait=ICAPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("ICAPWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("ICAPWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForTimeoutRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(requestkey)
        {
            number=requestcount+1;
            while(requestcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("ICAPWorker.waitForTimeoutRequests: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("ICAPWorker.waitForTimeoutRequests: request received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void waitForREQMOD(int number) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForREQMOD: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("ICAPSERVER/WAITTIMEOUT");
        if(wait<0)wait=ICAPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(reqkey)
        {
            while(reqcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForREQMOD: "+reqcount+"/"+number);
                if(wait>0)
                {
                    prevcount=reqcount;
                    reqkey.wait(wait);
                    if(reqcount==prevcount)
                    {
                        XTTProperties.printFail("ICAPWorker.waitForREQMOD: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    reqkey.wait();
                }
            }
            XTTProperties.printInfo("ICAPWorker.waitForREQMOD: "+reqcount+"/"+number);
        }
    }
    public static void waitForTimeoutREQMOD(int timeouttime) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForTimeoutREQMOD: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(reqkey)
        {
            number=reqcount+1;
            while(reqcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForTimeoutREQMOD: "+reqcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=reqcount;
                reqkey.wait(wait);
                if(reqcount==prevcount)
                {
                    XTTProperties.printInfo("ICAPWorker.waitForTimeoutREQMOD: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("ICAPWorker.waitForTimeoutREQMOD: req received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    public static void waitForRESPMOD(int number) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForRESPMOD: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("ICAPSERVER/WAITTIMEOUT");
        if(wait<0)wait=ICAPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(respkey)
        {
            while(respcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForRESPMOD: "+respcount+"/"+number);
                if(wait>0)
                {
                    prevcount=respcount;
                    respkey.wait(wait);
                    if(respcount==prevcount)
                    {
                        XTTProperties.printFail("ICAPWorker.waitForRESPMOD: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    respkey.wait();
                }
            }
            XTTProperties.printInfo("ICAPWorker.waitForRESPMOD: "+respcount+"/"+number);
        }
    }
    public static void waitForTimeoutRESPMOD(int timeouttime) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForTimeoutRESPMOD: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(respkey)
        {
            number=respcount+1;
            while(respcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForTimeoutRESPMOD: "+respcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=respcount;
                respkey.wait(wait);
                if(respcount==prevcount)
                {
                    XTTProperties.printInfo("ICAPWorker.waitForTimeoutRESPMOD: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("ICAPWorker.waitForTimeoutRESPMOD: resp received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    public static void waitForOPTIONS(int number) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForOPTIONS: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("ICAPSERVER/WAITTIMEOUT");
        if(wait<0)wait=ICAPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(optkey)
        {
            while(optcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForOPTIONS: "+optcount+"/"+number);
                if(wait>0)
                {
                    prevcount=optcount;
                    optkey.wait(wait);
                    if(optcount==prevcount)
                    {
                        XTTProperties.printFail("ICAPWorker.waitForOPTIONS: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    optkey.wait();
                }
            }
            XTTProperties.printInfo("ICAPWorker.waitForOPTIONS: "+optcount+"/"+number);
        }
    }
    public static void waitForTimeoutOPTIONS(int timeouttime) throws java.lang.InterruptedException
    {
        if(ICAPServer.checkSockets())
        {
            XTTProperties.printFail("ICAPWorker.waitForTimeoutOPTIONS: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(optkey)
        {
            number=optcount+1;
            while(optcount<number)
            {
                XTTProperties.printInfo("ICAPWorker.waitForTimeoutOPTIONS: "+optcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=optcount;
                optkey.wait(wait);
                if(optcount==prevcount)
                {
                    XTTProperties.printInfo("ICAPWorker.waitForTimeoutOPTIONS: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("ICAPWorker.waitForTimeoutOPTIONS: opt received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    private String getReturnMessage(int code)
    {
        switch(code)
        {
            case ICAP_CONTINUE           : return "Continue";
            case ICAP_OK                 : return "OK";
            case ICAP_CREATED            : return "Created";
            case ICAP_ACCEPTED           : return "Accepted";
            case ICAP_NOT_AUTHORITATIVE  : return "Not Authoritative";
            case ICAP_NO_MODIFICATION    : return "No Modification";
            case ICAP_RESET              : return "Reset";
            case ICAP_PARTIAL            : return "Partial";
            case ICAP_MULT_CHOICE        : return "Multiple Choice";
            case ICAP_MOVED_PERM         : return "Moved Permanently";
            case ICAP_MOVED_TEMP         : return "Moved Temporary";
            case ICAP_SEE_OTHER          : return "See Other";
            case ICAP_NOT_MODIFIED       : return "Not Modified";
            case ICAP_USE_PROXY          : return "Use Proxy";
            case ICAP_BAD_REQUEST        : return "Bad Request";
            case ICAP_UNAUTHORIZED       : return "Unauthorized";
            case ICAP_PAYMENT_REQUIRED   : return "Payment Required";
            case ICAP_FORBIDDEN          : return "Forbidden";
            case ICAP_NOT_FOUND          : return "Not Found";
            case ICAP_METHOD_NOT_ALLOWED : return "Method Not Allowed";
            case ICAP_NOT_ACCEPTABLE     : return "Not Acceptable";
            case ICAP_PROXY_AUTH         : return "Proxy Authorisation";
            case ICAP_REQUEST_TIMEOUT    : return "Request Timeout";
            case ICAP_CONFLICT           : return "Conflict";
            case ICAP_GONE               : return "Gone";
            case ICAP_LENGTH_REQUIRED    : return "Length Required";
            case ICAP_PRECON_FAILED      : return "Precon Failed";
            case ICAP_ENTITY_TOO_LARGE   : return "Entitiy Too Large";
            case ICAP_REQ_TOO_LONG       : return "Req Too Long";
            case ICAP_UNSUPPORTED_TYPE   : return "Unsupported Type";
            case ICAP_BAD_COMPOSITION    : return "Bad Composition";
            case ICAP_SERVER_ERROR       : return "Server Error";
            case ICAP_NOT_IMPLEMENTED    : return "Not Implemented";
            case ICAP_BAD_GATEWAY        : return "Bad Gateway";
            case ICAP_OVERLOADED         : return "Overloaded";
            case ICAP_GATEWAY_TIMEOUT    : return "Gateway Timeout";
            case ICAP_VERSION_UNSUPPORTED: return "Version Unsupported";
            default:  return "custom message";
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: ICAPWorker.java,v 1.12 2010/03/18 12:15:45 rajesh Exp $";
}
