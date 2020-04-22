package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;

import net.sourceforge.jwap.wsp.IWSPUpperLayer2;
import net.sourceforge.jwap.wsp.CWSPSession;
import net.sourceforge.jwap.wsp.CWSPResult;
import net.sourceforge.jwap.wsp.CWSPSocketAddress;
import net.sourceforge.jwap.wsp.pdu.CWSPHeaders;

/**
 * FunctionModule_WAP provides connection-oriented unencrypted wap1 GET functions.
 * It uses the jwap-1.1.jar wap stack from jwap.sourceforge.net.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_WAP.java,v 1.11 2008/01/17 13:56:29 rsoder Exp $
 * @see         <A HREF="http://jwap.sourceforge.net/latest-api-docs/">jWAP</A>
 */
public class FunctionModule_WAP extends FunctionModule implements IWSPUpperLayer2
{
    /**
     * The actual WSP Session.
     * You need one instance of CWSPSession for each WAP session.
     */
    private CWSPSession session;

    //private final static String CRLF     = "\r\n";
    // stores the Headers to be sent to the server
    private LinkedHashMap<String,String> sendHeader = new LinkedHashMap<String,String>();
    // stores the Headers to be received from the server
    private static LinkedHashMap<String,Vector<String>> receiveHeader = new LinkedHashMap<String,Vector<String>>();
    // tha data received from the server
    private static byte[] serverResponse = null;
    //private static boolean proxySet      = false;
    // the http-response-code of the last server request
    private String serverResponseCode    = "";

    /**
     * variable value is true when we do not have a timout situation
     * if a timeout occures the value is still false, see where it's used
     */
    private boolean isConnected=false;

    /**
     * Re-initialize the internal variables. Deletes headers, data, responseCode, CWSPSession etc.
     *
     */
    public void initialize()
    {
        sendHeader           = new LinkedHashMap<String,String>();
        receiveHeader        = new LinkedHashMap<String,Vector<String>>();
        serverResponse       = null;
        isConnected          = false;
        serverResponseCode   = "";
        session              = null;
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * returns the document body of the last server request
     *
     * @return      byte array containing the last request body
     */
    public static byte[] getBody()
    {
        return serverResponse;
    }

    /**
     * returns a <code>java.util.LinkedHashMap</code> containing one or more <code>Collection</code> objects containing the headerfields as Strings
     *
     * @return      byte array containing the last request body
     */
    public static LinkedHashMap<String,Vector<String>> getHeader()
    {
        return receiveHeader;
    }

    /**
     * remove all the headers stored to be sent to the server
     *
     * @param parameters   array of String containing the parameters.
     *
     */
    public void clearHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            sendHeader=new LinkedHashMap<String,String>();
        }
    }

    /**
     * set the http headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters. 
     *                     <br><code>parameters[0]</code> argument is always the method name, 
     *                     <br><code>parameters[1]</code> argument is the header key, 
     *                     <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: headerFieldKey headerFieldValue");
            return;
        }
        HTTPHelper.setHeader(this.sendHeader,parameters);
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the header key, 
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(getHeader(),parameters,false);
    }
    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to, 
     *                      <br><code>parameters[2]</code> argument is the header key, 
     *                      <br><code>parameters[3]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void queryHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryHeader: variable headerFieldKey regularExpression");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variable headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPHelper.queryHeader(getHeader(),parameters,false);
        }
    }
    /**
     * compare the http headers received by the client from the server with a value is prohibited.
     *
     * @param parameters   array of String containing the parameters. 
     *                     <br><code>parameters[0]</code> argument is always the method name, 
     *                     <br><code>parameters[1]</code> argument is the header key, 
     *                     <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(getHeader(),parameters,true);
    }
    /**
     * check a http header received by the client from the server does contain a value.
     *
     * @param parameters   array of String containing the parameters. 
     *                     <br><code>parameters[0]</code> argument is always the method name, 
     *                     <br><code>parameters[1]</code> argument is the header key, 
     *                     <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeaderPart: headerFieldKey expectedValuePart");
            return;
        }
        HTTPHelper.checkHeaderPart(getHeader(),parameters,false);
    }
    /**
     * check a http header received by the client from the server does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters. 
     *                     <br><code>parameters[0]</code> argument is always the method name, 
     *                     <br><code>parameters[1]</code> argument is the header key, 
     *                     <br><code>parameters[2]</code> argument is the header value part.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeaderNotPart: headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        HTTPHelper.checkHeaderPart(getHeader(),parameters,true);
    }
    
    /**
     * connect to a WAP gateway.
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> argument is the gateway ip address, 
     *                      <br><code>parameters[2]</code> is the gateway connection oriented unencrypted port. 
     *                      <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void connectGateway(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": connectGateway:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": connectGateway: gatewayAddress gatewayPort");
            return;
        }
        if(parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": gatewayAddress gatewayPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        try
        {
            InetAddress wapGwAddress=null;
            int wapGwPort=-1;
            if(parameters.length==3)
            {
                XTTProperties.printInfo(parameters[0] + ": connecting to WAP-Gateway "+parameters[1]+":"+parameters[2]);
                wapGwAddress=DNSServer.resolveAddressToInetAddress(parameters[1]);
                wapGwPort=Integer.parseInt(parameters[2]);
            } else
            {
                String ip=XTTProperties.getProperty("WAP/IP");
                if(ip.equals("null"))
                {
                    ip=XTTProperties.getProperty("XMG/IP");
                }
                wapGwAddress=DNSServer.resolveAddressToInetAddress(ip);
                wapGwPort=XTTProperties.getIntProperty("WAP/CONNECTIONORIENTED");
                if(wapGwPort<=0)
                {
                    wapGwPort=XTTProperties.getIntProperty("XMG/WAP/CONNECTIONORIENTED");
                }
                XTTProperties.printInfo(parameters[0] + ": connecting to WAP-Gateway "+ip+":"+wapGwPort);
            }
            if(wapGwPort<=0)
            {
                XTTProperties.printFail(parameters[0] + ": incompatible argument: gatewayPort="+wapGwPort);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            session = new CWSPSession(wapGwAddress, // URI of WAP gateway
                    wapGwPort,                      // port of wap gateway
                    (IWSPUpperLayer2)this,          // WE would like to be informed of occuring events
                    true);                          // be verbose
            int timeout=XTTProperties.getIntProperty("WAP/CONNECTIONTIMEOUT");
            if(timeout<=0)
            {
                timeout=XTTProperties.getIntProperty("XMG/WAP/CONNECTIONTIMEOUT");
            }
            if(timeout<=0)
            {
                timeout=10000;
            }
            // we will get a s.connect.cnf, when we are connected.
            // @see #s_connect_cnf()
            synchronized(this)
            {
                isConnected=false;
                session.s_connect();
                this.wait(timeout);
            }
            if(isConnected==false)
            {
                session=null;
                XTTProperties.printFail(parameters[0] + ": timed out");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            XTTProperties.printDebug(parameters[0] + ": done connecting");
        } catch (NumberFormatException nfe)
        {
            XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
        } catch (java.lang.InterruptedException ie)
        {
            XTTProperties.printFail(parameters[0] + ": "+ie.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } catch (java.net.UnknownHostException uhe)
        {
            XTTProperties.printFail(parameters[0] + ": "+uhe.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } catch (SocketException se) {
            XTTProperties.printFail(parameters[0] + ": "+se.getClass().getName());
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(se);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
                //se.printStackTrace();
        }
    }
    /**
     * disconnect from a WAP gateway.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name. No other arguments required.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void disconnectGateway(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": disconnectGateway:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        XTTProperties.printInfo(parameters[0] + ": Disconnecting from WAP-Gateway");
        try
        {
            int timeout=XTTProperties.getIntProperty("WAP/CONNECTIONTIMEOUT");
            if(timeout<=0)
            {
                timeout=XTTProperties.getIntProperty("XMG/WAP/CONNECTIONTIMEOUT");
            }
            if(timeout<=0)
            {
                timeout=10000;
            }
            synchronized(this)
            {
                isConnected=false;
                // we will get a s.connect.cnf, when we are connected.
                // @see #s_disconnect_ind
                //XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway: session.s_disconnect()");
                session.s_disconnect();
                //XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway: session=null");
                session=null;
                if(isConnected==false)
                {
                    XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway: this.wait("+timeout+")");
                    this.wait(timeout);
                } else
                {
                    XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway: no need to wait("+timeout+")");
                    Thread.sleep(500);
                }
                //XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway synchronized END");
            }
            if(isConnected==false)
            {
                session=null;
                XTTProperties.printFail(parameters[0] + ": timed out");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } catch (java.lang.InterruptedException ie)
        {
            XTTProperties.printFail(parameters[0] + ": "+ie.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } catch (java.lang.NullPointerException npe)
        {
            XTTProperties.printFail(parameters[0] + ": "+npe.getClass().getName());
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(npe);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        XTTProperties.printDebug(parameters[0] + ": Disconnecting from WAP-Gateway END");
    }

    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the post field name, <code>parameters[2]</code>
     *                     argument and following are concatenated together to the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see WebWorker#getPostData
     */
    public void checkResponseCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode: expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            boolean found=false;
            StringBuffer checked=new StringBuffer();
            String divider="";
            for(int i=1;i<parameters.length;i++)
            {
                if(serverResponseCode.equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider=",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": found "+serverResponseCode);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": found "+serverResponseCode+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * do a WAP GET request.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code>
     *                     argument and following are concatenated together to the url value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendGetRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest: URL");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": URL");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(session==null)
            {
                XTTProperties.printFail(parameters[0] + ": not connected to WAP-Gateway");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                XTTProperties.printInfo(parameters[0]+": downloading URL: "+parameters[1]);
                URL url = new URL(parameters[1]);
                CWSPHeaders hdrs = new CWSPHeaders();
                Iterator it=sendHeader.keySet().iterator();
                String key;
                while(it.hasNext())
                {
                    key=(String)it.next();
                    hdrs.setHeader(key, (String)sendHeader.get(key));
                }
                int timeout=XTTProperties.getIntProperty("WAP/CONNECTIONTIMEOUT");
                if(timeout<=0)
                {
                    timeout=XTTProperties.getIntProperty("XMG/WAP/CONNECTIONTIMEOUT");
                }
                if(timeout<=0)
                {
                    timeout=10000;
                }
                // we will get a s_methodResult_ind, when we are connected.
                // @see #s_connect_cnf()
                synchronized(this)
                {
                    isConnected=false;
                    session.s_get(hdrs, parameters[1]);
                    this.wait(timeout);
                }
                if(isConnected==false)
                {
                    session.s_disconnect();
                    session=null;
                    XTTProperties.printFail(parameters[0] + ": timed out");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }

            } catch (Exception e)
            {
                XTTProperties.printException(e);
                //e.printStackTrace();
            }
        }
    }

    /**
     * do a WAP GET request.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code>
     *                     argument and following are concatenated together to the url value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendPostRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPostRequest: contentType data URL");
            return;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": contentType data URL");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(session==null)
            {
                XTTProperties.printFail(parameters[0] + ": not connected to WAP-Gateway");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                byte[] postData=ConvertLib.getOctetByteArrayFromString(parameters[2]);
                String postContentType=parameters[1];
                XTTProperties.printInfo(parameters[0]+": downloading URL: "+parameters[3]);
                String stringURL=parameters[3];
                URL url = new URL(stringURL);
                CWSPHeaders hdrs = new CWSPHeaders();
                Iterator it=sendHeader.keySet().iterator();
                String key;
                while(it.hasNext())
                {
                    key=(String)it.next();
                    hdrs.setHeader(key, (String)sendHeader.get(key));
                }
                int timeout=XTTProperties.getIntProperty("WAP/CONNECTIONTIMEOUT");
                if(timeout<=0)
                {
                    timeout=XTTProperties.getIntProperty("XMG/WAP/CONNECTIONTIMEOUT");
                }
                if(timeout<=0)
                {
                    timeout=10000;
                }
                // we will get a s_methodResult_ind, when we are connected.
                // @see #s_connect_cnf()
                synchronized(this)
                {
                    isConnected=false;
                    session.s_post(hdrs,postData,postContentType, stringURL);
                    this.wait(timeout);
                }
                if(isConnected==false)
                {
                    session.s_disconnect();
                    session=null;
                    XTTProperties.printFail(parameters[0] + ": timed out");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }

            } catch (Exception e)
            {
                XTTProperties.printException(e);
                //e.printStackTrace();
            }
        }
    }


    public boolean queryBody(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryBody: variableName regularExpression");
            return false;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            return ConvertLib.queryString(parameters[0],ConvertLib.createString(getBody()),parameters[2],parameters[1]);
        }
    }


    //------------------ from here on we implement IWSPUpperLayer ---------------

    /**
     * will be invoked by jwap stack when we are connectedwith the WAP gateway
     */
    public void s_connect_cnf()
    {
        synchronized (this)
        {
            XTTProperties.printInfo(this.getClass().getName()+": connected to WAP-Gateway");
            isConnected=true;
            this.notify();
        }
    }

    /**
     * will be invoked by jwap stack to announce a response to a GET/POST method invocation
     *
     * @param payload
     * @param contentType
     * @param moreData
     */
    public void s_methodResult_ind(byte[] payload, String contentType,
        boolean moreData)
    {
        // we use the other one
    }

    /**
     * will be invoked by jwap stack to announce a response to a GET/POST method invocation
     *
     * @param result
     */
    public void s_methodResult_ind(CWSPResult result)
    {
        synchronized (this)
        {
            CWSPHeaders headers = result.getHeaders();
            receiveHeader       = new LinkedHashMap<String,Vector<String>>();
            if (headers != null)
            {
                receiveHeader=createHeaderFields(headers);
            }
            
            if((!receiveHeader.containsKey("content-type"))&&(result.getContentType() != null))
            {
                Vector<String> ct = new Vector<String>();
                ct.add(result.getContentType());
                receiveHeader.put("content-type",ct);
            }
            
            XTTProperties.printDebug(this.getClass().getName()+": Response Header Fields START:\n"+receiveHeader);
            XTTProperties.printDebug(this.getClass().getName()+": Response Header Fields END:");
            serverResponse=result.getPayload();
            serverResponseCode=result.getStatus()+"";

            XTTProperties.printInfo(this.getClass().getName()+": "+serverResponse.length+" Bytes received from WAP-Gateway");
            XTTProperties.printDebug(this.getClass().getName()+": Content Type: " + result.getContentType() + "\n" + ConvertLib.getHexView(serverResponse,0,XTTProperties.getIntProperty("BUFFEROUTPUTSIZE")));

            isConnected=true;
            this.notify();
        }
    }
    
    /**
     * create the LinkedHashMap of Collections for compatibility with the { @link FunctionModule_HTTP#getHeader() HTTP modules getHeader()}  method
     *
     * @param headers
     */
    private LinkedHashMap<String,Vector<String>> createHeaderFields(CWSPHeaders headers)
    {
        LinkedHashMap<String,Vector<String>> fields=new LinkedHashMap<String,Vector<String>>();
        String okey=null;
        String nkey=null;
        Vector<String> headerValues=null;
        for(Enumeration okeys=headers.getHeaderNames(); okeys.hasMoreElements();)
        {
            okey = (String)okeys.nextElement();
            nkey=okey;
            if(nkey!=null)nkey=nkey.toLowerCase();
            headerValues=new Vector<String>();
            for(Enumeration headerValuesEnum=headers.getHeaders(okey);headerValuesEnum.hasMoreElements();)
            {
                String val = (String) headerValuesEnum.nextElement();
                headerValues.add(val);
            }
            fields.put(nkey,headerValues);
            XTTProperties.printDebug("HEADTEST: "+nkey+" "+headerValues);
        }

        return fields;
    }

    /**
     * will be incoked by jwap stack when we are disconnected from the WAP gateway
     * @param reason
     */
    public void s_disconnect_ind(short reason)
    {
        synchronized (this)
        {
            XTTProperties.printInfo(this.getClass().getName()+": disconnected from WAP-Gateway reason:"+reason);
            isConnected=true;
            this.notify();
        }
        XTTProperties.printDebug(this.getClass().getName()+": disconnected from WAP-Gateway END");
    }

    /**
     * will be invoked by jwap stack when we are disconnected by the WAP gateway
     * because it is redirected.
     *
     * @param redirectInfo
     */
    public void s_disconnect_ind(InetAddress[] redirectInfo)
    {
        // ignore.
        // in IWSPUpperLayer2 we are using:
        // public void s_disconnect_ind(InetSocketAddress[] redirectInfo)
    }

    /**
     * will be invoked by jwap stack when we are disconnected by the WAP gateway
     * because it is redirected.
     *
     * @param redirectInfo
     */
    public void s_disconnect_ind(CWSPSocketAddress[] redirectInfo)
    {
        synchronized (this)
        {
            XTTProperties.printInfo(this.getClass().getName()+": redirect: disconnected from WAP-Gateway");
            if (redirectInfo.length > 0)
            {
                try
                {
                    XTTProperties.printInfo(this.getClass().getName()+": redirected to: "+ redirectInfo[0]);
                    session = new CWSPSession(
                            redirectInfo[0], // port of wap gateway
                            (IWSPUpperLayer2)this, // WE would like to be informed of occuring events
                            true); // be verbose
                    XTTProperties.printInfo(this.getClass().getName()+": redirect: connecting to WAP-Gateway");
                    session.s_connect();
                } catch (SocketException e) {
                    // UDP Socket problem
                    XTTProperties.printException(e);
                    //e.printStackTrace();
                }
            }
            isConnected=true;
            this.notify();
        }
    }

    /**
     * will be invoked by jwap stack that the session is suspended
     * @param reason
     */
    public void s_suspend_ind(short reason)
    {
        XTTProperties.printInfo(this.getClass().getName()+": suspended from WAP-Gateway reason:"+reason);
    }

    /**
     * will be invoked by jwap stack when a suspended session will be resumed
     */
    public void s_resume_cnf()
    {
        XTTProperties.printInfo(this.getClass().getName()+": resumed from WAP-Gateway");
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_WAP.java,v 1.11 2008/01/17 13:56:29 rsoder Exp $";
}