package com.mobixell.xtt;

/* An example of a very simple multi-threaded HTTP server.
 * Implementation notes are in MMSCServer.html, and also
 * as comments in the source code.
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * <p>MMSCWorker</p>/
 * <p>Processes a single HTTP request which has been received by the MMSCServer</p>
 * messages are stored in:
 *                      <br>MMSCSERVER/MM7/SOAP
 *                      <br>MMSCSERVER/myServerPort/MM7/SOAP
 * <br>sent messageids are stored in:
 *                      <br>MMSCSERVER/MM7/MESSAGEID
 *                      <br>MMSCSERVER/myServerPort/MM7/MESSAGEID
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Gavin Cattell & Roger Soder
 * @version $Id: MMSCWorker.java,v 1.32 2010/07/01 16:19:51 rajesh Exp $
 */
public class MMSCWorker extends Thread implements WebHttpConstants
{
    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    //private static LinkedHashMap<String,String> postData=null;
    private static LinkedHashMap<String,String> sendServerHeader=new LinkedHashMap<String,String>();
    private LinkedHashMap<String,String> headersToSend=new LinkedHashMap<String,String>(); //Not static, this has to be different for each request
    private String responseHeader = "";
    private byte[] responseBody = new byte[0];
    private String protocol = "";
    private String responseMode=null;
    private String operationID=null;

    private static String env="http://schemas.xmlsoap.org/soap/envelope/";
    private static String mm7="http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-6-MM7-1-4";
    public static String mm7version="6.5.0";

    private String transactionid="xttvasp-0001";

    private static Map<String,ByteArrayWrapper> fileCache = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());

    private static String recievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    private int returnCode = HTTP_SERVER_ERROR;
    private int soapReturnCode = 1000;
    private static int overrideReturnCode=0;
    private static int overrideSoapReturnCode=0;
    private static int overrideAfterRequests=0;
    private static int perworkerdelay=0;

    static final byte[] EOL = {(byte)'\r', (byte)'\n' };

    private static Object requestkey=new Object();
    private static int requestcount=0;

    byte[] receivedData         = null; //where we  store the bytes of the POST
    int    receivedBodyStart    = 0;    //tells us where the body starts
    int    receivedPayloadBodyStart = 0;    //tells us where the body to transcode starts
    int    receivedPayloadBodyStop  = 0;    //tells us where the body to transcode stops


    /* Socket to client we're handling, which will be set by the MMSCServer
       when dispatching the request to us */
    private Socket s = null;
    private MMSCServer myServer=null;
    private int myTimeout=600000;
    private File myRoot=null;
    private String myServerPort="-";

    private int networklagdelay=100;
    /**
     * Creates a new MMSCWorker
     * @param id     ID number of this worker thread
     */
    public MMSCWorker(int id, Socket setSocket, MMSCServer server, int timeout,File root)
    {
        super("MMSCWorker-"+id);
        this.s = setSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=server;
        this.myRoot=root;
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
        XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        // this worker thread entered a wait(), so we need to notify()
        // and wake it up so that it can exit
        //if( IntegrationTest.isVerbose() )
        //    System.out.println("Stop request for thread " + id );
        notifyAll();
    }

    /**
     * Start the worker thread
     */
    public synchronized void run()
    {
        try
        {
            handleClient();
        }
        catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        }
        catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        }
        catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        }
        catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        }
        catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(se);
            }
        }
        catch (Exception e)
        {
            XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            //e.printStackTrace();
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
            XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instance "+instances);
            key.notify();
        }

        InputStream is = new BufferedInputStream(s.getInputStream(),65536);

        PrintStream pst = new PrintStream(s.getOutputStream());
        StreamSplit ps=new StreamSplit(pst);

        s.setSoTimeout(myTimeout);
        //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
        s.setTcpNoDelay(true);

        headersToSend=new LinkedHashMap<String,String>();

        returnCode = HTTP_OK;

        try
        {
            XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): Client connected - receiving");
            BufferedInputStream in=(BufferedInputStream)is;

            LinkedHashMap<String,Vector<String>> serverHeader=HTTPHelper.readHTTPStreamHeaders("MMSCWorker("+myServerPort+"/"+getWorkerId()+")",in);

            //End fo stream so break
            //if(serverHeader.get(null)==null)break;
            String storeHeaderString=serverHeader.get(null).get(0);

            String firstLine[]=null;
            synchronized(receivedServerHeader)
            {
                receivedServerHeader.clear();
                receivedServerHeader.putAll(serverHeader);
                // only set this if we are doing a POST else set to null
                firstLine=storeHeaderString.split("\\s+",4);
                if(firstLine[0].equals("POST"))
                {
                    XTTProperties.printTransaction("MMSCSERVER/HTTP/POST"+XTTProperties.DELIMITER+firstLine[1]);
                    XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                } else
                {
                    XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): Method is NOT SUPPORTED");
                    returnCode = HTTP_BAD_METHOD;
                    soapReturnCode = 4000;
                }
                XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): fileName="+firstLine[1]);
                recievedURL=firstLine[1];
                protocol = firstLine[2];
                XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): protocol="+protocol);

                String soapPart="";

                if(getServerHeader().get("transfer-encoding")!=null)
                {
                    if(getServerHeader().get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                    {
                        XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): chunked body found, unchunking");
                        receivedData=HTTPHelper.readChunkedBody("MMSCWorker("+myServerPort+"/"+getWorkerId()+")",in,getServerHeader());
                        receivedBodyStart=0;
                    }
                } else
                {
                    Vector contentlengthVector=receivedServerHeader.get("content-length");
                    if(contentlengthVector!=null)
                    {
                        int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                        receivedData=new byte[contentlength];
                        HTTPHelper.readBytes(in,receivedData);
                        receivedBodyStart=0;
                        XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body");
                    } else
                    {
                        receivedData=HTTPHelper.readStream("MMSCWorker("+myServerPort+"/"+getWorkerId()+")",in,networklagdelay);
                        receivedBodyStart=0;
                    }
                }
                
                XTTProperties.setVariable("MMSCSERVER/MM7/BODY/PLAIN",ConvertLib.createString(receivedData));
                XTTProperties.setVariable("MMSCSERVER/MM7/BODY/BASE64",ConvertLib.createString(receivedData));
                XTTProperties.setVariable("MMSCSERVER/"+myServerPort+"/"+getWorkerId()+"/MM7/BODY/PLAIN",ConvertLib.createString(receivedData));
                XTTProperties.setVariable("MMSCSERVER/"+myServerPort+"/"+getWorkerId()+"/MM7/BASE64/PLAIN",ConvertLib.createString(receivedData));

                try
                {
                    // get the boundary
                    String contenttype=getServerHeader().get("content-type").get(0);
                    if(contenttype.toLowerCase().startsWith("multipart/related"))
                    {
                        int boundarystart=contenttype.indexOf("boundary=\"")+10;
                        String boundary=contenttype.substring(boundarystart,contenttype.indexOf("\"",boundarystart));
                        XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): boundary found:'--"+boundary+"'");

                        // try to figure out where the soap starts and the payload starts
                        // there's always a CR/LF before each boundary, have to get rid of that too.
                        String[] splitbody=ConvertLib.createString(receivedData,receivedBodyStart,receivedData.length-receivedBodyStart).split("--"+boundary);
                        soapPart=splitbody[1].split("\r\n\r\n")[1];
                        if(splitbody.length>2)
                        {
                            XTTProperties.setVariable("MMSCSERVER/MM7/CONTENT",splitbody[2].trim());
                        }
                    } else if (contenttype.toLowerCase().startsWith("text/xml"))
                    {
                        soapPart=ConvertLib.createString(receivedData,receivedBodyStart,receivedData.length-receivedBodyStart);
                    } else
                    {
                        XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): Unsupported POST data");
                        returnCode=HTTP_SERVER_ERROR;
                        soapReturnCode = 2000;
                    }
                    if(!soapPart.equals(""))
                    {
                        String moreStore="";
                        String moreStore2="";
                        Pattern rp=Pattern.compile("<Recipient>\\s*?<(Number|RFC2822Address|ShortCode)>([\\w\\p{Punct}]*?)</(Number|RFC2822Address|ShortCode)>\\s*?</Recipient>\\s*?",Pattern.DOTALL);
                        Matcher rm=rp.matcher(soapPart);
                        if(rm.find())
                        {
                            String recipient=rm.group(2);
                            moreStore="MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP Stored in MMSCSERVER/MM7/"+recipient+"/SOAP"+"\n";
                            moreStore2="\nMMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP Stored in MMSCSERVER/"+myServerPort+"/MM7/"+recipient+"/SOAP"+"\n";
                            XTTProperties.setVariable("MMSCSERVER/MM7/"+recipient+"/SOAP",soapPart);
                            XTTProperties.setVariable("MMSCSERVER/"+myServerPort+"/MM7/"+recipient+"/SOAP",soapPart);
                        }
                        String urlStore=recievedURL;
                        if(recievedURL.startsWith("/"))
                        {
                            urlStore=recievedURL.substring(1,recievedURL.length());
                        }
                        XTTProperties.setVariable("MMSCSERVER/"+urlStore+"/MM7/SOAP",soapPart);
                        urlStore="MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP Stored in MMSCSERVER/"+urlStore+"/MM7/SOAP\n";
                        XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP found:\n"+soapPart+"\n"+"\n"
                            +"MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP Stored in MMSCSERVER/MM7/SOAP"+"\n"
                            +moreStore
                            +urlStore
                            +"MMSCWorker("+myServerPort+"/"+getWorkerId()+"): SOAP Stored in MMSCSERVER/"+myServerPort+"/MM7/SOAP"+moreStore2);
                        XTTProperties.setVariable("MMSCSERVER/MM7/SOAP",soapPart);
                        XTTProperties.setVariable("MMSCSERVER/"+myServerPort+"/MM7/SOAP",soapPart);
                    }
                    
            try
                {
                    DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
                    DocumentBuilder db =dbf.newDocumentBuilder();

                    Document  doc = db.parse(new InputSource(new StringReader(soapPart)));

                    transactionid = doc.getElementsByTagName("mm7:TransactionID").item(0).getChildNodes().item(0).getNodeValue();

                    //For DR Auto Request
                    String drStatus = XTTProperties.getProperty("VASP/DN");
                    String rrStatus = XTTProperties.getProperty("VASP/RR");
                    if((null != drStatus && drStatus.equalsIgnoreCase("true")) ||(null != rrStatus && rrStatus.equalsIgnoreCase("true")))
                    {
//                        try
//                        {
//                            DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
//                            DocumentBuilder db =dbf.newDocumentBuilder();
//
//                            Document  doc = db.parse(new InputSource(new StringReader(soapPart)));
//
//                            transactionid = doc.getElementsByTagName("mm7:TransactionID").item(0).getChildNodes().item(0).getNodeValue();

                            //Document doc=db.parse(soapPart);
                            NodeList nl = doc.getElementsByTagName("Recipients");
                            for(int i=0;i<nl.getLength();i++)
                            {
                                Node fstNode = nl.item(i);
                                Element fstElmnt = (Element) fstNode;
                                NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("To");
                                Node sndNode;
                                String vartostore ="MMSCSERVER/RECIPIENTS";
                                if(fstNmElmntLst.getLength()>0)
                                {
                                    sndNode = fstNmElmntLst.item(0);
                                    vartostore = vartostore+"/TO";
                                    XTTProperties.setVariable(vartostore,"true");
                                }
                                else
                                {
                                    sndNode =fstNode; 
                                }
                                if (sndNode.getNodeType() == Node.ELEMENT_NODE)
                                {
                                    Element sndElmnt = (Element) sndNode;
                                    NodeList number = sndElmnt.getElementsByTagName("Number");
                                    if(number.getLength()>0)
                                    {
                                        Element sndNmElmnt = (Element) number.item(0);
                                        NodeList sndNm = sndNmElmnt.getChildNodes();
                                        String numberValue = ((Node) sndNm.item(0)).getNodeValue();
                                        XTTProperties.setVariable(vartostore+"/NUMBER", numberValue);
                                    }
                                    NodeList rfc822ad = sndElmnt.getElementsByTagName("rfc822ad");
                                    if(rfc822ad.getLength()>0)
                                    {
                                        Element sndNmElmnt = (Element) rfc822ad.item(0);
                                        NodeList sndNm = sndNmElmnt.getChildNodes();
                                        String rfc822adValue = ((Node) sndNm.item(0)).getNodeValue();
                                        XTTProperties.setVariable(vartostore+"/RFC822AD", rfc822adValue);
                                    }

                                    NodeList ShortCode = sndElmnt.getElementsByTagName("ShortCode");
                                    if(ShortCode.getLength()>0)
                                    {
                                        Element sndNmElmnt = (Element) ShortCode.item(0);
                                        NodeList sndNm = sndNmElmnt.getChildNodes();
                                        String shortCodeValue = ((Node) sndNm.item(0)).getNodeValue();
                                        XTTProperties.setVariable(vartostore+"/SHORTCODE", shortCodeValue);
                                    }
                                }
                            }
                        }
                    }catch (Exception e)
                    {
                        XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): exception while parsing SubmitReq xml");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        returnCode=HTTP_SERVER_ERROR;
                        soapReturnCode = 2000;
                        return;
                    }
                 } catch (ArrayIndexOutOfBoundsException aioobe)
                {
                    XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): Unable to decode POST data:\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
                    returnCode=HTTP_SERVER_ERROR;
                    soapReturnCode = 2000;
                }
                Pattern p=Pattern.compile("<env:Body>.*<.*(Submit|Deliver|Cancel|Update|Replace|DeliveryReport|ReadReply)Req",Pattern.DOTALL);
                //Pattern p=Pattern.compile("<env:Body>.* <(.*:(Submit|Deliver|Cancel|Update|Replace|DeliveryReport|ReadReply)|(Submit|Deliver|Cancel|Update|Replace|DeliveryReport|ReadReply))Req",Pattern.DOTALL);
                Matcher m=p.matcher(soapPart);
                if(m.find())
                {
                    responseMode=m.group(1);
                    if(responseMode.contains(":"))
                    {
                        responseMode = responseMode.substring(responseMode.indexOf(":")+1); 
                    }
                    soapReturnCode = 1000;
                } else
                {
                    returnCode=HTTP_SERVER_ERROR;
                    soapReturnCode = 2000;
                }
            }

            if(perworkerdelay>0)
            {
                try
                {
                    Thread.sleep(perworkerdelay*(getWorkerId()+1));
                } catch(InterruptedException ex){};
            }

            // This doesn't really work with parallel requests but should be ok with one at a time
            synchronized (requestkey)
            {
                if(requestcount>=overrideAfterRequests)
                {
                    if(overrideReturnCode>0)returnCode=overrideReturnCode;
                    if(overrideSoapReturnCode>0)soapReturnCode=overrideSoapReturnCode;
                }
            }

            if (returnCode != HTTP_OK)
            {
                byte[] data=createErrorSoap(soapReturnCode);
                printHeaders();
                printResponseHeaders(ps);
                ps.write(EOL);
                ps.write(data);
                pst.flush();
                ps.debugOutput();
                XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent Body START:\n"+(ConvertLib.createString(data)));
                XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent Body END");
            } else
            {
                byte[] data=createValidSoap(soapReturnCode,responseMode);
                printHeaders();
                printResponseHeaders(ps);
                ps.write(EOL);
                ps.write(data);
                pst.flush();
                ps.debugOutput();
                XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent Body START:\n"+(ConvertLib.createString(data)));
                XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent Body END");
                //returnResponse(responseParts,ps);
            }

            synchronized (requestkey)
            {
                requestcount++;
                requestkey.notifyAll();
            }
        } finally
        {
            s.close();
            XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    private void printResponseHeaders(StreamSplit ps)
    {
        try
        {
            headersToSend.putAll(sendServerHeader);

            ps.print(responseHeader);
            ps.write(EOL);

            Iterator it=headersToSend.keySet().iterator();
            String headerKey=null;
            String headerValue=null;
            while(it.hasNext())
            {
                headerKey=(String)it.next();
                headerValue = (String)headersToSend.get(headerKey);
                if(!headerValue.equals("null"))
                {
                    ps.print(headerKey+": "+headerValue);
                    ps.write(EOL);
                }
            }
        } catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    private class StreamSplit
    {
        PrintStream ps=null;
        StringBuffer output=new StringBuffer("");
        public StreamSplit(PrintStream ps)
        {
            this.ps=ps;
        }
        public void print(String out)
        {
            ps.print(out);
            output.append(out);
        }
        public void write(byte[] out) throws IOException
        {
            ps.write(out);
            output.append("\n");
        }
        public void debugOutput()
        {
            XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent header START:\n"+output);
            XTTProperties.printVerbose("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sent header END");
        }
    }

    /**
     * Adds headers to headersToSend Map
     * @throws IOException
     */
    private boolean printHeaders() throws IOException
    {
        boolean ret = false;

        switch(returnCode)
        {
        //200
        case HTTP_OK:
            responseHeader = protocol + " " + HTTP_OK+" OK";
            ret = true;
            break;
        //201
        case HTTP_CREATED:
            responseHeader = protocol + " " + HTTP_CREATED+" Created Content";
            headersToSend.put("content-type","text/xml");
            ret = false;
            break;
        //401
        case HTTP_UNAUTHORIZED:
            responseHeader = protocol + " " + HTTP_UNAUTHORIZED + " Authorization Required";
            headersToSend.put("content-type","text/xml");
            ret = false;
            break;
        //404
        case HTTP_NOT_FOUND:
            responseHeader = protocol + " " + HTTP_NOT_FOUND + " Not Found";
            headersToSend.put("content-type","text/xml");
            ret = false;
            break;
        //405
        case HTTP_BAD_METHOD:
            responseHeader = protocol + HTTP_BAD_METHOD + " Method Not Allowed ";
            break;
        //500
        case HTTP_SERVER_ERROR:
            responseHeader = protocol + " " + HTTP_SERVER_ERROR + " Internal Server Error";
            headersToSend.put("content-type","text/xml");
            ret = false;
            break;
        default:
            responseHeader = protocol + " " + returnCode + " XTT Preset Error";
            headersToSend.put("content-type","text/xml");
            ret = false;
            break;
        }

        headersToSend.put("server","XTT JAVA MMSCServer $Revision: 1.32 $");
        headersToSend.put("date","" + createDate());
        return ret;
    }

    private String createDate()
    {
        SimpleDateFormat format=new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy kk':'mm':'ss z",Locale.US);
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(calendar.getTime());
    }

    private byte[] createErrorSoap(int returnCodeSoap)
    {
       StringBuffer soap=new StringBuffer("<?xml version=\"1.0\"?>"+"\n");
        soap.append("<env:Envelope xmlns:env=\""+env+"\">"+"\n");
        soap.append("   <env:Header>"+"\n");
        soap.append("       <mm7:TransactionID xmlns:mm7=\""+mm7+"\" env:mustUnderstand=\"1\">"+transactionid+"</mm7:TransactionID>"+"\n");
        soap.append("   </env:Header>"+"\n");
        soap.append("   <env:Fault>"+"\n");
        soap.append("       <faultcode>env:Client</faultcode>"+"\n");
        soap.append("       <faultstring>Client error</faultstring>"+"\n");
        soap.append("       <detail>"+"\n");
        soap.append("           <RSErrorRsp xmlns=\""+mm7+"\">"+"\n");
        soap.append("               <MM7Version>"+mm7version+"</MM7Version>"+"\n");
        soap.append("               <Status>"+"\n");
        soap.append("                   <StatusCode>"+returnCodeSoap+"</StatusCode>"+"\n");
        soap.append("                   <StatusText>"+getErrorMessage(returnCodeSoap)+"</StatusText>"+"\n");
        soap.append("               </Status>"+"\n");
        soap.append("           </RSErrorRsp>"+"\n");
        soap.append("       </detail>"+"\n");
        soap.append("   </env:Fault>"+"\n");
        soap.append("</env:Envelope>"+"\n");

        byte[] data=ConvertLib.createBytes(soap.toString());
        headersToSend.put("content-length",""+data.length);
        return data;
    }

    private byte[] createValidSoap(int returnCodeSoap, String mode)
    {

        StringBuffer soap=new StringBuffer("<?xml version=\"1.0\"?>"+"\n");
        soap.append("<env:Envelope xmlns:env=\""+env+"\">"+"\n");
        soap.append("   <env:Header>"+"\n");
        soap.append("       <mm7:TransactionID xmlns:mm7=\""+mm7+"\" env:mustUnderstand=\"1\">"+transactionid+"</mm7:TransactionID>"+"\n");
        soap.append("   </env:Header>"+"\n");
        soap.append("<env:Body>"+"\n");
        soap.append("   <"+mode+"Rsp xmlns=\""+mm7+"\">"+"\n");
        soap.append("       <MM7Version>"+mm7version+"</MM7Version>"+"\n");
        soap.append("       <Status>"+"\n");
        soap.append("           <StatusCode>"+returnCodeSoap+"</StatusCode>"+"\n");
        soap.append("           <StatusText>"+getErrorMessage(returnCodeSoap)+"</StatusText>"+"\n");
        soap.append("       </Status>"+"\n");
        if(mode.equals("Submit"))
        {
            String msgid="XTT"+(new Date()).getTime();
            soap.append("       <MessageID>"+msgid+"</MessageID>"+"\n");
            XTTProperties.setVariable("MMSCSERVER/MM7/MESSAGEID",msgid);
            XTTProperties.setVariable("MMSCSERVER/"+myServerPort+"/MM7/MESSAGEID",msgid);
        }
        soap.append("   </"+mode+"Rsp>"+"\n");
        soap.append("</env:Body>"+"\n");
        soap.append("</env:Envelope>"+"\n");


        byte[] data=ConvertLib.createBytes(soap.toString());
        headersToSend.put("content-length",""+data.length);
        headersToSend.put("content-type","text/xml; charset=\"utf-8\"");
        return data;
    }

    /**
     * to be completed
     * @param fname
     * @throws IOException
     */
    private byte[] sendFile(String fname)// throws IOException
    {
        try
        {
            File targ = new File(myRoot,fname);
            if (targ.isDirectory())
            {
                XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): directories not supported");
                returnCode=HTTP_SERVER_ERROR;
                return new byte[0];
            }

            ByteArrayWrapper wrap=(ByteArrayWrapper)fileCache.get(myRoot+"/"+fname);
            byte[] thefile=null;
            if(wrap==null)
            {
                XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from DISK");
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
                XTTProperties.printDebug("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from CACHE");
                thefile=wrap.getArray();
            }
            return thefile;
        } catch(Exception e)
        {
            XTTProperties.printFail("MMSCWorker("+myServerPort+"/"+getWorkerId()+"): exception when getting file");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            returnCode=HTTP_NOT_FOUND;
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
        XTTProperties.printInfo("MMSCWorker.setCacheFile: store content to cache: '"+root+"/"+fname+"' "+thefile.length+" bytes");
        fileCache.put(root+"/"+fname,new ByteArrayWrapper(thefile));
    }
    public static void clearCache()
    {
        XTTProperties.printInfo("MMSCWorker.setCacheFile: clearing file cache");
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

    private boolean stop = false;
    private int id;

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

    public static void setXMLNSENV(String val)
    {
        if(val==null|val.equals("null"))return;
        env=val;
    }
    public static void setXMLNSMM7(String val)
    {
        if(val==null|val.equals("null"))return;
        mm7=val;
    }
    public static void setMM7Version(String val)
    {
        if(val==null|val.equals("null"))return;
        mm7version=val;
    }

    public static LinkedHashMap<String,String> getServerSendHeader()
    {
        return sendServerHeader;
    }
    public static void setServerSendHeader(LinkedHashMap<String,String> serverHeader)
    {
        sendServerHeader=serverHeader;
    }
    public static void setOverrideReturnCode(int code)
    {
        overrideReturnCode=code;
    }
    public static void setOverrideSoapReturnCode(int code)
    {
        overrideSoapReturnCode=code;
    }
    
    public static void setOverrideAfterRequests(int num)
    {
        overrideAfterRequests=num;
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
        perworkerdelay=0;
        overrideReturnCode=0;
        overrideSoapReturnCode=0;
        overrideAfterRequests=0;
    }
    public static void setPerWorkerDelay(int workerdelay)
    {
        perworkerdelay=workerdelay;
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        String connection=null;
        if(MMSCServer.checkSockets())
        {
            XTTProperties.printFail("MMSCWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("MMSCSERVER/TIMEOUT");
        if(wait<0)wait=MMSCServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("MMSCWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("MMSCWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("MMSCWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        String connection=null;
        if(MMSCServer.checkSockets())
        {
            XTTProperties.printFail("MMSCWorker.waitForTimeoutRequests: no instance running!");
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
                XTTProperties.printInfo("MMSCWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("MMSCWorker.waitForTimeoutRequests: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("MMSCWorker.waitForTimeoutRequests: request received! "+requestcount+"/"+number+"");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    private String getErrorMessage(int code)
    {
        switch(code)
        {
            case 1000: return "Success";
            case 1100: return "Partial success";
            case 2000: return "Client error";
            case 2001: return "Operation restricted";
            case 2002: return "Address Error";
            case 2003: return "Address Not Found";
            case 2004: return "Multimedia content refused";
            case 2005: return "Message ID Not found";
            case 2006: return "LinkedID not found";
            case 2007: return "Message format corrupt";
            case 2008: return "Application ID not found";
            case 2009: return "Reply Application ID not found";
            case 3000: return "Server Error";
            case 3001: return "Not Possible";
            case 3002: return "Message rejected";
            case 3003: return "Multiple addresses not supported";
            case 3004: return "Application Addressing not supported";
            case 4000: return "General service error";
            case 4001: return "Improper identification";
            case 4002: return "Unsupported version";
            case 4003: return "Unsupported operation";
            case 4004: return "Validation error";
            case 4005: return "Service error";
            case 4006: return "Service unavailable";
            case 4007: return "Service denied";
            case 4008: return "Application denied";
            default: return null;
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: MMSCWorker.java,v 1.32 2010/07/01 16:19:51 rajesh Exp $";
}
