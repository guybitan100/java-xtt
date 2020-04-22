package com.mobixell.xtt;

/* An example of a very simple multi-threaded HTTP server.
 * Implementation notes are in STIServer.html, and also
 * as comments in the source code.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.URI;
import java.net.Socket;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>STIWorker</p>
 * <p>Processes a single HTTP request which has been received by the STIServer</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Gavin Cattell & Roger Soder
 * @version $Id: STIWorker.java,v 1.22 2008/11/20 09:19:44 rsoder Exp $
 */
public class STIWorker extends Thread implements WebHttpConstants
{
    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    //private static LinkedHashMap<String,String> postData=null;
    private static LinkedHashMap<String,String> sendServerHeader=new LinkedHashMap<String,String>();
    private LinkedHashMap<String,String> headersToSend=new LinkedHashMap<String,String>(); //Not static, this has to be different for each request
    private String responseHeader = "";
    private byte[] responseBody = new byte[0];
    private String protocol = "";
    private LinkedHashMap<String,TranscodingJob> transcodingJobMap=null;
    private Vector<TranscodingJob> transcodingJobList=null;
    private String originatorID=null;
    private String operationID=null;

    private static Map<String,ByteArrayWrapper> fileCache = Collections.synchronizedMap(new HashMap<String,ByteArrayWrapper>());

    private static String recievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    private int returnCode = HTTP_SERVER_ERROR;
    private int soapReturnCode = 2000;
    private static int overrideReturnCode=0;
    private static int overrideSoapReturnCode=0;

    static final byte[] EOL = {(byte)'\r', (byte)'\n' };

    private static Object requestkey=new Object();
    private static int requestcount=0;

    byte[] receivedData         = null; //where we  store the bytes of the POST
    int    receivedBodyStart    = 0;    //tells us where the body starts
    int    receivedPayloadBodyStart = 0;    //tells us where the body to transcode starts
    int    receivedPayloadBodyStop  = 0;    //tells us where the body to transcode stops
    
    
    /* Socket to client we're handling, which will be set by the STIServer
       when dispatching the request to us */
    private Socket s = null;
    private STIServer myServer=null;
    private int myTimeout=600000;
    private File myRoot=null;
    private String myServerPort="-";
    private STIWorker thisworker=this;
    private static boolean extendedOutput=true;

    private int MINLAGDELAY=100;
    /**
     * Creates a new STIWorker
     * @param id     ID number of this worker thread
     */
    public STIWorker(int id, Socket setSocket, STIServer server, int timeout,File root)
    {
        super("STIWorker-"+id);
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
        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        try
        {
            this.s.close();
        } catch (Exception ex){}
        // if this worker thread entered a wait(), we need to notify()
        // and wake it up so that it can exit
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
            XTTProperties.printWarn("STIWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        }
        catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("STIWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        }
        catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("STIWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        }
        catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("STIWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        }
        catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("STIWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(se);
            }
        }
        catch (Exception e)
        {
            XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
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
            XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instance "+instances);
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
            int networklagdelay=XTTProperties.getNetworkLagDelay();
            if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;

            XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): Client connected - receiving");
            
            BufferedInputStream in=(BufferedInputStream)is;
            LinkedHashMap<String,Vector<String>> serverHeader=HTTPHelper.readHTTPStreamHeaders("STIWorker("+myServerPort+"/"+getWorkerId()+")",in);

            String storeHeaderString=serverHeader.get(null).get(0);

            String firstLine[]=null;
            synchronized(receivedServerHeader)
            {
                receivedServerHeader.clear();
                receivedServerHeader.putAll(serverHeader);
                firstLine=storeHeaderString.split("\\s+",4);
                if(firstLine[0].equals("POST"))
                {
                    XTTProperties.printTransaction("STISERVER/HTTP/POST"+XTTProperties.DELIMITER+firstLine[1]);
                    XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                } else
                {
                    XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): Method is NOT SUPPORTED");
                    returnCode = HTTP_BAD_METHOD;
                    soapReturnCode = 4000;
                }
                XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): fileName="+firstLine[1]);
                recievedURL=firstLine[1];
                protocol = firstLine[2];
                XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): protocol="+protocol);

                if(getServerHeader().get("transfer-encoding")!=null)
                {
                    if(getServerHeader().get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                    {
                        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): chunked body found, unchunking");
                        receivedData=HTTPHelper.readChunkedBody("STIWorker("+myServerPort+"/"+getWorkerId()+")",in,getServerHeader());
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
                        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body");
                    } else
                    {
                        receivedData=HTTPHelper.readStream("STIWorker("+myServerPort+"/"+getWorkerId()+")",in,networklagdelay);
                        receivedBodyStart=0;
                    }
                }
                
                if(extendedOutput)
                {
                    XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): Received Body START:\n"+ConvertLib.getHexView(receivedData,0,receivedData.length));
                }


                //System.out.println(ConvertLib.getHexView(receivedData,0,receivedData.length));



                    // get the boundary
                    String contenttype=getServerHeader().get("content-type").get(0);
                    if(contenttype.toLowerCase().startsWith("multipart/related"))
                    {
                        int boundarystart=contenttype.indexOf("boundary=\"")+10;
                        String boundary=contenttype.substring(boundarystart,contenttype.indexOf("\"",boundarystart));
                        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): boundary found:'--"+boundary+"'");
                        
                        // try to figure out where the soap starts and the payload starts
                        // there's always a CR/LF before each boundary, have to get rid of that too.
                        // Use US-ASCII charset so that the length of the string equals the length of the bytes
                        //String thebody=(new String(receivedData,"US-ASCII"));
                        //String thebody=ConvertLib.createString(receivedData,receivedBodyStart,receivedData.length-receivedBodyStart);
                        String[] splitbody=(new String(receivedData,"US-ASCII")).split("--"+boundary);
                        //System.out.println("\n"+ConvertLib.createString(receivedData,receivedBodyStart,receivedData.length-receivedBodyStart).length());
                        //System.out.println("\n"+thebody.length());
                        //System.out.println("\n"+(receivedData.length-receivedBodyStart));
                        //System.out.println("\n");
                        String soapPart=splitbody[1].split("\r\n\r\n")[1];
                        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): SOAP found STISERVER/STI/SOAP:\n"+soapPart);
                        XTTProperties.setVariable("STISERVER/STI/SOAP",soapPart);
                        // splitbody[0] should be of 0 length because the body should start with the boundary, but better be sure.
                        // we also have to add the CR/LF and -- which is 2 bytes for first boundary and 4 for second boundary.
                        receivedPayloadBodyStart=receivedBodyStart+splitbody[0].length()+boundary.length()+2+splitbody[1].length()+boundary.length()+4;
                        
                        Pattern p=Pattern.compile("<originatorID>(.*)</originatorID>",Pattern.DOTALL);
                        Matcher m=p.matcher(soapPart);
                        if(m.find())
                        {
                            originatorID=m.group(1);
                        }
                        p=Pattern.compile("<operationID>(.*)</operationID>",Pattern.DOTALL);
                        m=p.matcher(soapPart);
                        if(m.find())
                        {
                            operationID=m.group(1);
                        }
                        String globalProfileID=null;
                        p=Pattern.compile("<profileID>(.*)</profileID>\\s*<transcodingJob>");
                        m=p.matcher(soapPart);
                        if(m.find())globalProfileID=m.group(1);

                        p=Pattern.compile("<transcodingJob>(.*)</transcodingJob>",Pattern.DOTALL);
                        m=p.matcher(soapPart);
                        
                        transcodingJobMap=new LinkedHashMap<String,TranscodingJob>();
                        transcodingJobList=new Vector<TranscodingJob>();
                        
                        // we have to stop 2 bytes earlyer because of the cr/lf at the end which we have to add again in the next start
                        int lastStop=receivedPayloadBodyStart-2;
                        receivedPayloadBodyStop=lastStop;
                        int jobCounter=0;
                        TranscodingJob tJob=null;
                        while(m.find())
                        {
                            tJob=new TranscodingJob(m.group(0),globalProfileID);
                            XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): found transcoding job["+(jobCounter)+"]:\n"+tJob);
                            transcodingJobMap.put(tJob.getLocation(),tJob);
                            transcodingJobList.add(tJob);
                            jobCounter++;
                        }
                        
                        String tempBody=null;
                        String[] tempHead=null;
                        lastStop=receivedPayloadBodyStop;
                        // TODO: check if this works with multiple jobs. probably not
                        for(int jobLoop=0;jobLoop<jobCounter;jobLoop++)
                        {
                            receivedPayloadBodyStop=lastStop+splitbody[2+jobLoop].length()-2;//-2 for the 0x0D,0x0A before the next boundary which is not part of the split
                            receivedPayloadBodyStart=lastStop+2;

                            tempBody=new String(receivedData,receivedPayloadBodyStart,receivedPayloadBodyStop-receivedPayloadBodyStart,"US-ASCII");
                            tempHead=tempBody.split("\r\n\r\n");
                            //System.out.println(ConvertLib.getHexView(receivedData,receivedPayloadBodyStart,receivedPayloadBodyStop-receivedPayloadBodyStart));
                            
                            p=Pattern.compile("content-id: <?([^<>]+)>?",Pattern.CASE_INSENSITIVE);
                            m=p.matcher(tempHead[0]);
                            if(m.find())
                            {
                                tJob=transcodingJobMap.get("cid:"+m.group(1));
                                if(tJob!=null)
                                {
                                    //System.out.println(ConvertLib.getHexView(receivedData,receivedPayloadBodyStart+tempHead[0].length()+4,receivedPayloadBodyStop));
                                    tJob.setContent(tempHead[0],receivedData,receivedPayloadBodyStart+tempHead[0].length()+4,receivedPayloadBodyStop);
                                } else
                                {
                                    XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): no job defined for content-id:\n"+tempHead[0]);
                                    returnCode=HTTP_SERVER_ERROR;
                                }
                            } else
                            {
                                XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): content-id not found on part:\n"+tempHead[0]);
                                returnCode=HTTP_SERVER_ERROR;
                            }
                            lastStop=receivedPayloadBodyStop+2+2+boundary.length();//2 for CR/LF, 2 for --,
                        }
                    } else
                    {
                        XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): Unsupported POST data");
                        returnCode=HTTP_SERVER_ERROR;
                        soapReturnCode = 4000;
                    }
            /*    } else
                {
                    XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): no POST data found");
                    returnCode=HTTP_SERVER_ERROR;
                }*/
            }
                
                //overrideReturnCode=500;



                if(overrideReturnCode>0)returnCode=overrideReturnCode;


                //Vector<ByteArrayWrapper> responseParts=createResponse();


                if (returnCode != HTTP_OK)
                {
                    if(overrideSoapReturnCode>0)soapReturnCode=overrideSoapReturnCode;
                    byte[] data=createErrorSoap(soapReturnCode);
                    printHeaders();
                    printResponseHeaders(ps);
                    ps.write(EOL);
                    ps.write(data);
                    pst.flush();
                    ps.debugOutput();
                    XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent Body START:\n"+ConvertLib.getHexView(data,0,data.length));
                    XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent Body END");
                    
                } else
                {
                    if(overrideSoapReturnCode>0)soapReturnCode=overrideSoapReturnCode;
                    byte[] data=createValidSoap(soapReturnCode);
                    if (this.stop)
                    {
                        return;
                    }
                    printHeaders();
                    printResponseHeaders(ps);
                    ps.write(EOL);
                    ps.write(data);
                    pst.flush();
                    ps.debugOutput();
                    XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent Body START:\n"+ConvertLib.getHexView(data,0,data.length));
                    XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent Body END");
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
            XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
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
            XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent header START:\n"+output);
            XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): sent header END");
        }
    }

    /**
     * to be completed
     * @param targ
     * @param ps
     * @return
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

        headersToSend.put("server","XTT JAVA STIServer $Revision: 1.22 $");
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
        String dataS=(ConvertLib.createString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            +"<env:Envelope xmlns=\"http://www.openmobilealliance.org/schema/sti/v1_0\" xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            +"    <env:Body>\n"
            +"        <soap:Fault xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>\n"
            +"            <faultcode>soap:Client</faultcode>\n"
            +"            <faultstring>"+getErrorMessage(returnCodeSoap)+"</faultstring>\n"
            +"            <faultactor>http://www.xtt724.com/sti/v1_0</faultactor>\n"
            +"            <detail>\n"
            +"                <TranscodingResponse xmlns=\"http://www.openmobilealliance.org/schema/sti/v1_0\">\n"
            +"                    <originatorID>"+originatorID+"</originatorID>\n"
            +"                    <operationID>"+operationID+"</operationID>\n"
            +"                    <mainReturnResult>\n"
            +"                        <returnCode>"+returnCodeSoap+"</returnCode>\n"
            +"                        <returnString>"+getErrorMessage(returnCodeSoap)+"</returnString>\n"
            +"                    </mainReturnResult>\n"
            +"                    <totalDuration>0</totalDuration>\n"
            +"                </TranscodingResponse>\n"
            +"            </detail>\n"
            +"        </soap:Fault>\n"
            +"    </env:Body>\n"
            +"</env:Envelope>"));
            byte[] data=ConvertLib.createBytes(dataS);
        headersToSend.put("content-length",""+data.length);
        return data; 
    }

    private byte[] createValidSoap(int returnCodeSoap)
    {

        StringBuffer soap=new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+"\n"
            +"<env:Envelope xmlns=\"http://www.openmobilealliance.org/schema/sti/v1_0\" xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"+"\n"
            +"<env:Body>"+"\n"
            +"   <TranscodingResponse>"+"\n"
            +"       <originatorID>"+originatorID+"</originatorID>"+"\n"
            +"       <operationID>"+operationID+"</operationID>"+"\n"
            +"       <mainReturnResult>"+"\n"
            +"           <returnCode>"+returnCodeSoap+"</returnCode>"+"\n"
            +"           <returnString>"+getErrorMessage(returnCodeSoap)+"</returnString>"+"\n"
            +"       </mainReturnResult>"+"\n"
            +"       <totalDuration>1</totalDuration>"+"\n");
            
        Iterator<TranscodingJob> it=transcodingJobList.iterator();
        TranscodingJob job=null;
        int contentSizes=0;
        while(it.hasNext())
        {
            job=it.next();
            job.create();
            soap.append("       <jobResult>"+"\n");
            soap.append("           <jobID>"+job.getJobID()+"</jobID>"+"\n");
            soap.append("           <mainReturnResult>"+"\n");
            soap.append("               <returnCode>"+job.getReturnCode()+"</returnCode>"+"\n");
            soap.append("               <returnString>"+getErrorMessage(job.getReturnCode())+"</returnString>"+"\n");
            soap.append("           </mainReturnResult>"+"\n");
            soap.append("           <duration>1</duration>"+"\n");
            soap.append("           <output>"+"\n");
            soap.append("               <contentType>"+job.getContentType()+"</contentType>"+"\n");
            soap.append("              <location>"+job.getLocation()+"</location>"+"\n");
            soap.append("               <mediaSize>"+job.getContentLength()+"</mediaSize>"+"\n");
            soap.append("           </output>"+"\n");
            soap.append("       </jobResult>"+"\n");
            contentSizes=contentSizes+job.getTotalLength();
        }
        
        soap.append("   </TranscodingResponse>"+"\n");
        soap.append("</env:Body>"+"\n");
        soap.append("</env:Envelope>");
                
        String headers="Content-type: text/xml; charset=\"utf-8\""+"\r\n"
            +"Content-Length: "+soap.length()+"\r\n"
            +"Content-Id: <soap-part>"+"\r\n"
            +"Content-Transfer-Encoding: 8bit"+"\r\n"+"\r\n";
            
            String boundary="XTTBOUNDARY-01234567890-STI000000001";
            byte[] boundaryBytes=null;
            try
            {
                boundaryBytes=("\r\n--"+boundary).getBytes(XTTProperties.getCharSet());
            } catch (java.io.UnsupportedEncodingException uee)
            {
                uee.printStackTrace();
                boundaryBytes=("\r\n--"+boundary).getBytes();
            }
            headersToSend.put("content-type","multipart/related; boundary=\""+boundary+"\"; type=\"text/xml\"; start=\"soap-part\"");

        String compleateSoap="--"+boundary+"\r\n"+headers+soap+"\r\n"+"--"+boundary+"\r\n";
        
        byte[] data=new byte[compleateSoap.length()+contentSizes+transcodingJobList.size()*(boundaryBytes.length+2)+2];
        int pointer=0;
        byte[] temp=null;
        try
        {
            temp=compleateSoap.getBytes(XTTProperties.getCharSet());
        } catch (java.io.UnsupportedEncodingException uee)
        {
            uee.printStackTrace();
            temp=compleateSoap.getBytes();
        }
        for(int i=0;i<temp.length;i++)
        {
            data[pointer++]=temp[i];
        }
        
        it=transcodingJobList.iterator();
        job=null;
        while(it.hasNext())
        {
            job=it.next();
            temp=job.getTotalContent();
            for(int i=0;i<temp.length;i++)
            {
                data[pointer++]=temp[i];
            }
            for(int i=0;i<boundaryBytes.length;i++)
            {
                data[pointer++]=boundaryBytes[i];
            }
            //System.out.println(data.length+" -> "+pointer);
            if(pointer>=data.length-4)
            {
                data[pointer++]='-';
                data[pointer++]='-';
            }
            data[pointer++]=0x0D;
            data[pointer++]=0x0A;
        }
        //System.out.println(data.length+" -> "+pointer);
        
        
        headersToSend.put("content-length",""+data.length);
        return data; 
    }

    /**
     * to be completed
     * @param targ
     * @param ps
     * @throws IOException
     */
    private byte[] sendFile(String fname)// throws IOException
    {
        try
        {
            File targ = new File(myRoot,fname);
            if (targ.isDirectory())
            {
                XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): directories not supported");
                returnCode=HTTP_SERVER_ERROR;
                return new byte[0];
            }

            ByteArrayWrapper wrap=(ByteArrayWrapper)fileCache.get(myRoot+"/"+fname);
            byte[] thefile=null;
            if(wrap==null)
            {
                XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from DISK");
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
                XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): sendFile from CACHE");
                thefile=wrap.getArray();
            }
            return thefile;
        } catch(Exception e)
        {
            XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): exception when getting file");
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
        XTTProperties.printInfo("STIWorker.setCacheFile: store content to cache: '"+root+"/"+fname+"' "+thefile.length+" bytes");
        fileCache.put(root+"/"+fname,new ByteArrayWrapper(thefile));
    }
    public static void clearCache()
    {
        XTTProperties.printInfo("STIWorker.setCacheFile: clearing file cache");
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
        overrideReturnCode=0;
        overrideSoapReturnCode=0;
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        String connection=null;
        if(STIServer.checkSockets())
        {
            XTTProperties.printFail("STIWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("STISERVER/TIMEOUT");
        if(wait<0)wait=STIServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("STIWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("STIWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("STIWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(STIServer.checkSockets())
        {
            XTTProperties.printFail("STIWorker.waitForTimeoutRequests: no instance running!");
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
                XTTProperties.printInfo("STIWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("STIWorker.waitForTimeoutRequests: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("STIWorker.waitForTimeoutRequests: request received! "+requestcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    private class TranscodingJob
    {
        private String jobID        = null;public String getJobID()      {return jobID;}
        private String contentType  = null;public String getContentType(){return contentType;}
        private String location     = null;public String getLocation()   {return location;}
        private String profileID    = null;public String getProfileID()  {return profileID;}
        
        private byte[] content      = new byte[0];
        private String headers      = null;
        
        private int returnCode      = 2000;public int getReturnCode(){return returnCode;}
        
        private int contentLength     = 0;   public int getContentLength(){return contentLength;}
        
        public int getTotalLength(){return content.length+headers.length()+4;}
        
        public byte[] getTotalContent()
        {
            byte[] returnval=new byte[getTotalLength()];
            byte[] head=null;
            try
            {
                head=headers.getBytes(XTTProperties.getCharSet());
            } catch (java.io.UnsupportedEncodingException uee)
            {
                uee.printStackTrace();
                head=headers.getBytes();
            }
            int pointer=0;
            for(int i=0;i<head.length;i++)
            {
                returnval[pointer++]=head[i];
            }
            returnval[pointer++]=0x0D;
            returnval[pointer++]=0x0A;
            returnval[pointer++]=0x0D;
            returnval[pointer++]=0x0A;
            for(int i=0;i<content.length;i++)
            {
                returnval[pointer++]=content[i];
            }
            return returnval;
        }
        
        public void create()
        {
            XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): TranscodingJob: "+jobID+" create");
            try
            {
                Pattern p=Pattern.compile("content-length:\\s++(\\p{Digit}*)",Pattern.CASE_INSENSITIVE);
                Matcher m=p.matcher(ConvertLib.createString(content));
                if(m.find())
                {
                    contentLength=Integer.decode(m.group(1));
                }
            } catch (Exception e){e.printStackTrace();}
                
            if(profileID.startsWith("XTTSTI"))
            {
                String [] parameters=profileID.split(";");
                for(int i=0;i<parameters.length;i++)
                {
                    if(parameters[i].startsWith("delay="))
                    {
                        int delay=Integer.decode(parameters[i].split("=")[1].trim());
                        delay(delay);
                    } else if(parameters[i].startsWith("returncode="))
                    {
                        returnCode=Integer.decode(parameters[i].split("=")[1].trim());
                    } else if(parameters[i].startsWith("file="))
                    {
                        String filename=parameters[i].split("=")[1].trim();
                        File targ = new File(myRoot, filename);
                        if (!targ.exists()&&(fileCache.get(myRoot+"/"+filename)==null))
                        {
                            returnCode = 2007;
                            content=new byte[0];
                            contentLength=0;
                        }else
                        {
                            String originalcontentType=contentType;
                            int index = filename.lastIndexOf('.');
                            if (index > 0) 
                            {
                                contentType = HTTPHelper.getContentType(filename.substring(index).toLowerCase());
                                headers=headers.replaceAll("[Cc]ontent-[Tt]ype: "+originalcontentType,"content-type: "+contentType);
                            } 
                            if (contentType == null) 
                            {
                                contentType = originalcontentType;
                            }
                            content=sendFile(filename);
                            contentLength=content.length;
                        }
                        XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): TranscodingJob: "+jobID+" file="+targ);
                    }
                }
            }
        }
        
        public TranscodingJob(String xmlParts, String globalProfileID)
        {
            profileID=globalProfileID;
            Pattern p=null;
            Matcher m=null;
            
            p=Pattern.compile("<transcodingJob>[^<>]*<jobID>(.*)</jobID>",Pattern.DOTALL);
            m=p.matcher(xmlParts);
            if(m.find())jobID=m.group(1);
            
            String source=null;
            p=Pattern.compile("<transcodingJob>.*<source>(.*)</source>.*</transcodingJob>",Pattern.DOTALL);
            m=p.matcher(xmlParts);
            if(m.find())
            {
                source=m.group(1);
                p=Pattern.compile("<contentType>(.*)</contentType>");
                m=p.matcher(source);
                if(m.find())contentType=m.group(1);
                p=Pattern.compile("<location>(.*)</location>");
                m=p.matcher(source);
                if(m.find())location=m.group(1);
            }
            String target=null;
            p=Pattern.compile("<transcodingJob>.*<target>(.*)</target>.*</transcodingJob>",Pattern.DOTALL);
            m=p.matcher(xmlParts);
            if(m.find())
            {
                target=m.group(1);
                p=Pattern.compile("<profileID>(.*)</profileID>");
                m=p.matcher(target);
                if(m.find())profileID=m.group(1);
            }
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/profileID",profileID);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/jobID",jobID);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/contentType",contentType);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/location",location);

            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/"+jobID+"/profileID",profileID);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/"+jobID+"/jobID",jobID);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/"+jobID+"/contentType",contentType);
            XTTProperties.setVariable("STISERVER/STI/"+profileID+"/"+jobID+"/location",location);
        }
        
        public void setContent(String headers, byte[] data, int start, int stop)
        {
            this.headers=headers;
            this.content=new byte[stop-start];
            for(int i=start;i<stop;i++)
            {
                this.content[i-start]=data[i];
            }
            XTTProperties.printDebug("STIWorker("+myServerPort+"/"+getWorkerId()+"): TranscodingJob: "+jobID+" content set "+content.length+" bytes");
            if(contentType.equals("application/vnd.wap.mms-message"))
            {
                MMSDecoder d=new MMSDecoder(content,0,content.length,"STISERVER/"+profileID);
                try
                {
                    d.decode();
                } catch (Exception e)
                {
                    XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): error decoding application/vnd.wap.mms-message");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                    returnCode=HTTP_SERVER_ERROR;
                }
            }
        }

        public TranscodingJob(String jobID, String contentType, String location, String profileID)
        {
            this.jobID      = jobID;
            this.contentType= contentType;
            this.location   = location;
            this.profileID  = profileID;
        }
        public String toString()
        {
            return "jobID              = "+jobID+"\n"
                  +"source.contentType = "+contentType+"\n"
                  +"source.location    = "+location+"\n"
                  +"target.profileID   = "+profileID+"\n"
                  +"content-length     = "+content.length+" bytes";
        }
        
        private void delay(int delayms)
        {
            if(delayms>0) 
            {
                try
                {
                    XTTProperties.printVerbose("STIWorker("+myServerPort+"/"+getWorkerId()+"): "+jobID+": delay="+delayms+"ms");
                    synchronized(this)
                    {
                        thisworker.wait(delayms);
                    }
                } catch( java.lang.InterruptedException ex )
                {
                    XTTProperties.printFail("STIWorker("+myServerPort+"/"+getWorkerId()+"): "+jobID+": Delay interrupted: "+ex);
                }
            }
        }
    }
    
    private String getErrorMessage(int code)
    {
        switch(code)
        {
            case 1000: return "Info";
            case 1001: return "Info - Result content saved to an external location.";
            case 1002: return "Info - Default profile used.";
            case 1003: return "Info - No transcoding performed - original content returned.";
            case 1004: return "Info - The transcoding did not result in any content.";
            //case 1005-1499: return "STI - reserved for future use";
            //case 1500-1999: return "Other- Reserved for any other non-defined (or proprietary) info message.";
            case 2000: return "Success";
            case 2001: return "Warning - One or more transcoding jobs failed.";
            case 2002: return "Warning - Multiple Transcoding Jobs not supported, only one job processed successfully.";
            case 2003: return "Warning - Unsupported parameter ignored, not used by the Transcoding Platform.";
            case 2004: return "Warning - Unsupported parameter value ignored.";
            case 2005: return "Warning - Unknown parameter value ignored.";
            case 2006: return "Warning - Content truncated.";
            case 2007: return "Warning - Content removed.";
            case 2008: return "Warning - Unable to transcode DRM protected content.";
            case 2009: return "Warning - DRM protected content transcoded.";
            case 2010: return "Warning - Incompatibility between transcodingParams and profileID ignored.";
            case 2011: return "Warning - Neither profileID nor transcodingParams specified";
            //case 2012-2499: return "STI - reserved for future use";
            //case 2500-2999: return "Other - Reserved for any other non-defined (or proprietary) warning result.";
            case 4000: return "Client Error";
            case 4001: return "Client Error - Parsing error: Invalid STI Request (in SOAP message body)";
            case 4002: return "Client Error - Unknown parameter value.";
            case 4003: return "Client Error - Unauthorized request.";
            case 4004: return "Client Error - Unable to get the specified internal or external resource: Invalid URI";
            case 4005: return "Client Error - Unable to get the specified internal or external resource: Location forbidden.";
            case 4006: return "Client Error - Unable to get the specified internal or external resource: Location not found.";
            case 4007: return "Client Error - Error while reading the specified internal or external resource: IO problem.";
            case 4008: return "Client Error - Failed to save the resource to the specified target external location: Invalid URI";
            case 4009: return "Client Error - Failed to save the resource to the specified target external location: Location forbidden.";
            case 4010: return "Client Error - Failed to save the resource to the specified target external location: Location not found.";
            case 4011: return "Client Error - Failed to save the resource to the specified target external location: IO Problem";
            case 4012: return "Client Error - Adaptation not Allowed";
            case 4013: return "Client Error - Non Unique jobID";
            case 4014: return "Client Error - Neither profileID nor transcodingParams specified";
            case 4015: return "Client Error - Insufficient transcoding parameters provided.";
            case 4016: return "Client Error - Error reading a specified internal or external resource.";
            case 4017: return "Client Error - Error when accessing a specified external location for writing.";
            case 4018: return "Client Error - Error when processing a specified internal or external resource.";
            //case 4019-4499: return "STI - reserved for future use";
            //case 4500-4999: return "Other - Reserved for any other non-defined (or proprietary) client error result.";
            case 5000: return "Server Error - Internal Server Error.";
            case 5001: return "Server Error - Unsupported parameter.";
            case 5002: return "Server Error - Unsupported parameter value.";
            case 5003: return "Server Error - Transcoding service temporary unavailable.";
            case 5004: return "Server Error - Timeout.";
            case 5005: return "Server Error - STI Version not supported.";
            case 5006: return "Server Error - Unable to perform transcoding.";
            case 5007: return "Server Error - Cannot meet sizeLimit.";
            case 5008: return "Server Error - DRM content - No transcoding performed.";
            case 5009: return "Server Error - All the transcoding jobs failed.";
            case 5010: return "Server Error - License prohibits the request";
            case 5011: return "Server Error - Resource Limit Exceeded";
            case 5012: return "Server Error - Multiple Transcoding Jobs not supported.";
            case 5013: return "Server Error - Input media not supported";
            //case 5014-5499: return "STI - reserved for future use";
            //case 5500-5999: return "Other - Reserved for any other non-defined (or proprietary) server error result.";
            default: return null;
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: STIWorker.java,v 1.22 2008/11/20 09:19:44 rsoder Exp $";
}
