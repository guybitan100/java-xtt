package com.mobixell.xtt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.xml.sax.InputSource;


/**
 * FunctionModule_VASP provides MM7 via HTTP and SOAP functions.
 *
 * @author      Gavin Cattell, Roger Soder
 * @version     $Id: FunctionModule_VASP.java,v 1.27 2010/05/12 07:16:43 awadhai Exp $
 */
public class FunctionModule_VASP extends FunctionModule
{
    private Map<String,HTTPConnection> connections = Collections.synchronizedMap(new HashMap<String,HTTPConnection>());
    private HTTPConnection defaultConnection = null;
    //private Thread ws                    = null;
    private final static String CRLF     = "\r\n";
    private LinkedHashMap<String,String> sendHeader                   = new LinkedHashMap<String,String>();
    private static LinkedHashMap<String,Vector<String>> receiveHeader = new LinkedHashMap<String,Vector<String>>();
    private static byte[] serverResponse = null;
    //private static boolean proxySet      = false;
    private String serverResponseCode[]  = new String[]{"","not yet initialized"};
    private byte[] postData             = null;
    private String contentBoundary      = "000000000000000000000001-content";
    private String messageBoundary      = "000000000000000000000001-message";
    private String mulipartType         = "related";
    private String contentId            = "<xtt/mm7/submit>";

    private boolean isMM7 = true; //false means it's a TPI
    private boolean useContentID=false;

    private java.security.cert.Certificate certs[] = new java.security.cert.Certificate[0];

    private Document soap = null;

    //TODO: Make namespaces generic
    //Used for MM7
    private Namespace env = Namespace.getNamespace("env", "http://schemas.xmlsoap.org/soap/envelope/");
    private Namespace mm7 = Namespace.getNamespace("mm7", "http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-5-MM7-1-3");
    private Namespace mms = Namespace.getNamespace("http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-5-MM7-1-3");

    //Used for TPI
    private Namespace soapenv = null; //done in createTPISoap
    private Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    private Namespace xsd = Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
    private Namespace iserver2 = null;
    //private Namespace xmlns = Namespace.getNamespace("xmlns","http://www.w3.org/2000/xmlns");
    
    private String messageID = null;

    /**
     * clears and reinitializes all the variables. Does not reset the gateway.
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");

        defaultConnection=new HTTPConnection("DEFAULT","VASP");
        defaultConnection.readConfiguration();

        serverResponse       = null;
        serverResponseCode   = new String[]{"","not yet initialized"};
        sendHeader           = new LinkedHashMap<String,String>();
        receiveHeader        = new LinkedHashMap<String,Vector<String>>();
        postData             = null;

        createMM7Soap(null,false,null,null);

        certs = new java.security.cert.Certificate[0];

        sendHeader.put("content-type","multipart/related; boundary=\"" + messageBoundary +"\"; type=text/xml; start=\"" + contentId + "\"");
        sendHeader.put("SOAPAction","\"\"");
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module VASP -->"
            +"\n    <Vasp>"
            +"\n        <!-- IpAddress and Port to connect to for VASP module -->"
            +"\n        <RemoteIp>127.0.0.1</RemoteIp>"
            +"\n        <RemotePort>5555</RemotePort>"
            +"\n"
            +HTTPConnection.getConfigurationOptions()
            +"\n"
            +"\n        <!-- this is for supporting old VASP configuration files with NO MM7 or TPI tag -->"
            +"\n        <!--noProtocolTag/-->"
            +"\n        <VASPID>VASPID001</VASPID>"
            +"\n        <VASID>VASID002</VASID>"
            +"\n        <Number>0711113333</Number>"
            +"\n        <!-- Listening port for VASP, only used by tests -->"
            +"\n        <Port>3333</Port>"
            +"\n        <Directory>vaspdir</Directory>"
            +"\n        <TransactionId>xttvasp-0001</TransactionId>"
            +"\n"
            +"\n        <!-- schema definitions for MM7 and TPI -->"
            +"\n        <xmlnsenv>http://schemas.xmlsoap.org/soap/envelope/</xmlnsenv>"
            +"\n        <SOAPXSD>soapEnvelope.xsd</SOAPXSD>"
            +"\n"
            +"\n        <!-- schema definitions for MM7 -->"
            +"\n        <xmlns>http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-5-MM7-1-2</xmlns>"
            +"\n        <xmlnsmm7>http://www.3gpp.org/ftp/Specs/archive/23_series/23.140/schema/REL-5-MM7-1-2</xmlnsmm7>"
            +"\n        <MM7XSD>REL-5-MM7-1-2.xsd</MM7XSD>"
            +"\n        <MM7Version>5.3.0</MM7Version>"
            +"\n"
            +"\n        <!-- schema definitions for TPI -->"
            +"\n        <version>2.1.0</version>"
            +"\n        <xmlnsenvname>SOAP-ENV</xmlnsenvname>"
            +"\n        <xmlnsxsd>http://www.w3.org/2001/XMLSchema</xmlnsxsd>"
            +"\n        <xmlnsxsi>http://www.w3.org/2001/XMLSchema-instance</xmlnsxsi>"
            +"\n        <iserver2>soap.iserv2.sicap.ch</iserver2>"
            +"\n        <iserver2name>iserver2</iserver2name>"
            +"\n        <actor>MBS</actor>"
            +"\n     </Vasp>";
    }

    /**
     * constructor sets gateway.
     */
    public FunctionModule_VASP()
    {


    }


    /**
     * returns a byte array containing the body part of the last request.
     */
    public static byte[] getBody()
    {
        return serverResponse;
    }

    /**
     * returns a LinkedHashMap containing for each header a set with the values of the header. Does only contain the
     * values of the last request.
     */
    public static LinkedHashMap<String,Vector<String>> getHeader()
    {
        return receiveHeader;
    }

    /**
     * set the http headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
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

    public void createConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createConnection: name");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1];
            try
            {
                HTTPConnection connection=defaultConnection.createConnection(name);
                connections.put(name.toLowerCase(),connection);
                HTTPHelper.getConnection(connections,defaultConnection,parameters);
                XTTProperties.printInfo(parameters[0] + ": created connection '"+name+"'");
            } catch (Exception uhe)
            {
                XTTProperties.printFail(parameters[0] + ":: Unable to set create connection '"+name+"': "+uhe.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
            }
        }
    }
    
    public void closeConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": closeConnection: connection");
            return;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": connection");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPHelper.getConnection(connections,defaultConnection,parameters).closeConnection();
            XTTProperties.printInfo(parameters[0] + ": closed connection.");
        }
    }
        
    /**
     * compare the http headers received by the client from the server with a value.
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
     * compare the http headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the header key,
     *                      <br><code>parameters[2]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNot: headerFieldKey expectedValue");
            return;
        }
        HTTPHelper.checkHeader(STIWorker.getServerHeader(),parameters,true);
    }

    /**
     * check a http header received by the client from the server does contain a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value part.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
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
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument is the header value part.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
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
     * clear the post data meant to be sent to the server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional arguments requeired.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearContent(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearContent:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing POST data");
            postData=null;
        }
    }

    /**
     * set the post data to be sent from the client to the server on a post request.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> contains the String to be used as Postdata.
     */
    public void addContent(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addContent: postData");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": postData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            postData = ConvertLib.createBytes(parameters[1]);
            XTTProperties.printInfo(parameters[0]+": setting " + postData.length + " bytes of POST data");
        }
    }

    /**
     * no idea.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> contains the String to be used as Postdata.
     */
    public void addSOAPContentID(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addSOAPContentID: fileName");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": fileName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String contentData = parameters[1];
            String regex = "Content-ID:\\s*+(.*+)(\\n|\\r)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(contentData);

            boolean gotSmil = false;
            boolean setAlready = false;

            while(matcher.find())
            {
                XTTProperties.printInfo(parameters[0] + ": Adding " + matcher.group(1).replaceAll("[<>]","") + " to SOAP");
                if(matcher.group(1).indexOf("smil") != -1)
                {
                    gotSmil=true;
                    setMessageBoundary(new String[] {"setContentId","",matcher.group(1),"related"});
                    addSoapData(new String[] {"addSoapData","//*[local-name()='MMSSubmitRequest']","smil",matcher.group(1).replaceAll("[<>]","")});
                }
                else
                {
                    if(!gotSmil&&!setAlready)
                    {
                        setAlready = true;
                        setMessageBoundary(new String[] {"setContentId","",matcher.group(1),"mixed"});
                    }
                    addSoapData(new String[] {"addSoapData","//*[local-name()='MMSSubmitRequest']","content",matcher.group(1).replaceAll("[<>]","")});
                }
            }
        }
    }

    /**
     * set the post data to be sent from the client to the server on a post request, encoded with base64.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> contains the String to be used as Postdata.
     */
    public void addBase64Content(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addBase64Content: postData");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": postData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            byte[] decodedBytes = ConvertLib.base64Decode(parameters[1]);
            XTTProperties.printInfo(parameters[0]+": setting " + decodedBytes.length + " bytes of POST data");

            postData = decodedBytes;
        }
    }

    public void setMessageBoundary(String parameters[])
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": setMessageBoundary: messageBoundary");
            XTTProperties.printFail(this.getClass().getName()+": setMessageBoundary: messageBoundary content-id");
            XTTProperties.printFail(this.getClass().getName()+": setMessageBoundary: messageBoundary content-id multipart-type]");
            return;
        }
        if(parameters.length<2||parameters.length > 4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": messageBoundary");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": messageBoundary content-id");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": messageBoundary content-id multipart-type]");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        useContentID=false;
        if((parameters[1] != null)&&(!parameters[1].equals("")))
        {
            if (!messageBoundary.equals(parameters[1]))
            {
                XTTProperties.printInfo(parameters[0] + ": changing message boundary to " + parameters[1]);
                messageBoundary = parameters[1];
            }
        }
        if((parameters.length>2)&&(parameters[2] != null)&&(!parameters[2].equals("")))
        {
            if (!contentId.equals(parameters[2]))
            {
                XTTProperties.printInfo(parameters[0] + ": changing content-ID to " + parameters[2]);
                contentId = parameters[2];
                useContentID=true;
            }
        }
        if((parameters.length>3)&&(parameters[3] != null)&&(!parameters[3].equals("")))
        {
            if (!mulipartType.equals(parameters[3]))
            {
                XTTProperties.printInfo(parameters[0] + ": changing multipart type to " + parameters[3]);
                mulipartType = parameters[3];
            }
        }

        sendHeader.put("content-type","multipart/"+mulipartType+"; boundary=\"" + messageBoundary +"\"; type=text/xml; start=\"" + contentId + "\"");
    }

    public void setSoapData(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setSoapData: parentNode node");
            XTTProperties.printFail(this.getClass().getName()+": setSoapData: parentNode node data");
            return;
        } else if (parameters.length < 3||parameters.length > 4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode node");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode node data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }

        try
        {
            if (XPath.selectSingleNode(soap,parameters[1]) == null)
            {
                XTTProperties.printFail(parameters[0] + ": parent node '"+parameters[1]+"'doesn't exist, aborting");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            else
            {
                Element node = (Element)XPath.selectSingleNode(soap,parameters[1]+"/"+parameters[2]);
                if((node==null)&&(parameters[2].indexOf(":")==-1))
                {
                    XTTProperties.printDebug(parameters[0] + ": Checking for node "+parameters[2]+" local name! Might cause issues with namespaces");
                    node = (Element)XPath.selectSingleNode(soap,parameters[1]+"/*[local-name()='"+parameters[2]+"']");
                }
                //It doesn't exist yet
                if (node == null)
                {
                    node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                    //We didn't give any data, but it doesn't exist, so add a blank node
                    if (parameters.length < 4)
                    {
                        XTTProperties.printInfo(parameters[0] + ": node '"+parameters[2]+"' doesn't exist, adding");
                        Element newNode = new Element(parameters[2]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);
                    }
                    else
                    {
                        XTTProperties.printInfo(parameters[0] + ": node '"+parameters[2]+"' doesn't exist, adding with text '"+parameters[3]+"'");
                        Element newNode = new Element(parameters[2]);
                        newNode.addContent(parameters[3]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);  
                        if(parameters[2].equalsIgnoreCase("DeliveryReport"))
                        {
                            XTTProperties.setProperty("VASP/DN", parameters[3]);
                        }
                        else if(parameters[2].equalsIgnoreCase("ReadReply"))
                        {
                            XTTProperties.setProperty("VASP/RR", parameters[3]);
                        }
                        }
                }
                //It already exists
                else
                {
                    //No data was given, and it exists, so we'll remove this node
                    if (parameters.length < 4)
                    {
                        Element child=node;
                        node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                        //Element child = (Element)XPath.selectSingleNode(soap,parameters[1] + "/" + parameters[2]);
                        XTTProperties.printInfo(parameters[0] + ": node '"+parameters[2]+"' exists, removing");
                        //System.out.println(child);
                        //System.out.println(child.getNamespace());
                        node.removeChild(parameters[2],child.getNamespace());
                    }
                    //Data was given, lets just edit the test instead
                    else
                    {
                        XTTProperties.printInfo(parameters[0] + ": node '"+parameters[2]+"' exists, editing with value '"+parameters[3]+"'");
                        node.setText(parameters[3]);
                    }
                }
            }
            /*if(XTTProperties.printDebug(null))
            {
                XTTXML.writeXML(soap,"test.xml");
            }*/
        }
        catch (Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": error writing soap data");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    public void setSoapAttribute(String parameters[])
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": setSoapAttribute: parentNode attribute");
            XTTProperties.printFail(this.getClass().getName()+": setSoapAttribute: parentNode attribute data");
            XTTProperties.printFail(this.getClass().getName()+": setSoapAttribute: parentNode attribute data useXSI");
            return;
        } else if (parameters.length < 3||parameters.length > 5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode attribute");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode attribute data");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode attribute data useXSI");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        try
        {
            if (XPath.selectSingleNode(soap,parameters[1]) == null)
            {
                XTTProperties.printFail(parameters[0] + ": node '"+parameters[1]+"' doesn't exist, aborting");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } else
            {
                Element node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                //No data was given, and it exists, so we'll remove this node
                if (parameters.length < 4)
                {
                    //node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                    XTTProperties.printDebug(parameters[0] + ": node exists, removing attribute '"+parameters[2]+"'");
                    node.removeAttribute(parameters[2]);
                }
                //Data was given, lets just edit the test instead
                else
                {
                    if(parameters.length > 4)
                    {
                        XTTProperties.printDebug(parameters[0] + ": node exists, adding attribute '"+parameters[2]+"' with xsi");
                        //XTTProperties.printWarn("I'm adding the datatype");
                        node.setAttribute(new Attribute(parameters[2],parameters[3],xsi));
                    }
                    else
                    {
                        XTTProperties.printDebug(parameters[0] + ": node exists, adding attribute '"+parameters[2]+"'");
                        node.setAttribute(parameters[2],parameters[3]);
                    }
                }
            }
            /*if(XTTProperties.printDebug(null))
            {
                XTTProperties.writeXML(soap,"test.xml");
            }*/
        }
        catch (Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": error writing soap data");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    public void addSoapData(String parameters[])
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": addSoapData: parentNode attribute");
            XTTProperties.printFail(this.getClass().getName()+": addSoapData: parentNode attribute data");
            return;
        }else if (parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode attribute");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": parentNode attribute data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        try
        {
            if (XPath.selectSingleNode(soap,parameters[1]) == null)
            {
                XTTProperties.printFail(parameters[0] + ": parent node doesn't exist, aborting");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            else
            {
                Element node = (Element)XPath.selectSingleNode(soap,parameters[1]+"/"+parameters[2]);
                if((node==null)&&(parameters[2].indexOf(":")==-1))
                {
                    XTTProperties.printDebug(parameters[0] + ": Checking for node local name! Might cause issues with namespaces");
                    node = (Element)XPath.selectSingleNode(soap,parameters[1]+"/*[local-name()='"+parameters[2]+"']");
                }
                //System.out.println("" + parameters.length);
                //It doesn't exist yet
                if (node == null)
                {
                    node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                    //We didn't give any data, but it doesn't exist, so add a blank node
                    if (parameters.length < 4)
                    {
                        XTTProperties.printDebug(parameters[0] + ": node doesn't exist, adding");
                        Element newNode = new Element(parameters[2]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);
                    } else
                    {
                        XTTProperties.printDebug(parameters[0] + ": node doesn't exist, adding with text");
                        Element newNode = new Element(parameters[2]);
                        newNode.addContent(parameters[3]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);
                    }
                }
                //It already exists
                else
                {
                    if (parameters.length < 4)
                    {
                        XTTProperties.printDebug(parameters[0] + ": node exists, adding new");
                        node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                        Element newNode = new Element(parameters[2]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);
                    } else
                    {
                        XTTProperties.printDebug(parameters[0] + ": node exists, adding new with text");
                        node = (Element)XPath.selectSingleNode(soap,parameters[1]);
                        Element newNode = new Element(parameters[2]);
                        newNode.addContent(parameters[3]);
                        newNode.setNamespace(node.getNamespace());
                        node.addContent(newNode);
                    }
                }
            }
            /*if(XTTProperties.printDebug(null))
            {
                XTTXML.writeXML(soap,"test.xml");
            }*/
        }
        catch (Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": error writing soap data");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    public void resetSoap(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": resetSoap:"+NO_ARGUMENTS);
            return;
        }
        initialize();
        XTTProperties.printInfo(parameters[0] + ": done");
    }

    public void createMM7Soap(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM7Soap:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": createMM7Soap: bodyType");
            XTTProperties.printFail(this.getClass().getName()+": createMM7Soap: bodyType vaspID");
            XTTProperties.printFail(this.getClass().getName()+": createMM7Soap: bodyType vaspID vasID");
            return;
        }
        if(parameters.length==1)
        {
            XTTProperties.printInfo(createMM7Soap(null,false,null,null));
        } else if(parameters.length==2)
        {
            XTTProperties.printInfo(createMM7Soap(parameters[1],false,null,null));
        } else if(parameters.length==3)
        {
            XTTProperties.printInfo(createMM7Soap(parameters[1],false,parameters[2],null));
        } else if(parameters.length==4)
        {
            XTTProperties.printInfo(createMM7Soap(parameters[1],false,parameters[2],parameters[3]));
        } else if(parameters.length==5)
        {
            XTTProperties.printInfo(createMM7Soap(parameters[1],false,parameters[2],parameters[3],false));
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": bodyType");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": bodyType vaspID");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": bodyType vaspID vasID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
    }

    public void createTPISoap(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createTPISoap:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": createTPISoap: bodyType");
            return;
        }
        if(parameters.length==1)
        {
            XTTProperties.printInfo(createTPISoap((String)null));
        }
        else if(parameters.length==2)
        {
            XTTProperties.printInfo(createTPISoap(parameters[1]));
        }
        else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": bodyType");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
    }

    //2nd argument is so you can overload the above function, and for future use.
    private String createTPISoap(String reqTypeContent)
    {
        if(isMM7)
        {
            isMM7 = false;
            setMessageBoundary(new String[] {"setContentId","","","mixed"});
        }

        String info="createTPISoap";

        String tmp=XTTProperties.getProperty("VASP/XMLNSENV");
        String tmp2=XTTProperties.getProperty("VASP/XMLNSENVNAME");
        if(tmp2.equals("null"))tmp2="soapenv";
        if(!tmp.equals("null"))
        {
            soapenv = Namespace.getNamespace(tmp2, tmp);
        } else
        {
            soapenv = Namespace.getNamespace(tmp2, "http://schemas.xmlsoap.org/soap/envelope/");
        }

        tmp=XTTProperties.getProperty("VASP/XMLNS");
        if(!tmp.equals("null"))
        {
            mms = Namespace.getNamespace(tmp);
        }
        tmp=XTTProperties.getProperty("VASP/XMLNSXSI");
        if(!tmp.equals("null"))
        {
            xsi = Namespace.getNamespace("xsi", tmp);
        }
        tmp=XTTProperties.getProperty("VASP/XMLNSXSD");
        if(!tmp.equals("null"))
        {
            xsd = Namespace.getNamespace("xsd", tmp);
        }

        //int tpiversion=XTTProperties.getIntProperty("VASP/TPIVERSION");

        tmp=XTTProperties.getProperty("VASP/ISERVER2");
        tmp2=XTTProperties.getProperty("VASP/ISERVER2NAME");
        if(tmp2.equals("null"))tmp2="iserver2";
        if(!tmp.equals("null"))
        {
            iserver2 = Namespace.getNamespace(tmp2, tmp);
        } else
        {
            iserver2=null;
        }

        String actorDef=XTTProperties.getProperty("VASP/ACTOR");
        if(actorDef.equals("null"))
        {
            actorDef = "xma";
        }

        soap = new Document();

        //Set main soap body up
        Element envelope = new Element("Envelope",soapenv);
        //Attribute xsiDef = new Attribute(xsi.getPrefix(),xsi.getURI());
        envelope.addNamespaceDeclaration(xsi);
        envelope.addNamespaceDeclaration(xsd);
        //envelope.setAttribute(xsiDef);


        soap.addContent(envelope);

        Element header = new Element("Header",soapenv);
        envelope.addContent(header);

        Element version = new Element("version");
        Attribute actor = new Attribute("actor",actorDef,soapenv); //What's the actor?
        version.setAttribute(actor);
        Attribute mustUnd = new Attribute("mustunderstand","0",soapenv);
        version.setAttribute(mustUnd);
        String versionContent = XTTProperties.getProperty("VASP/VERSION");
        if ((versionContent == null)||(versionContent.length() == 0) || (versionContent.equalsIgnoreCase("null")))
            versionContent = "1.0.0";
        version.addContent(versionContent);
        header.addContent(version);

        Element reqType = new Element("request-type");
        actor = new Attribute("actor",actorDef,soapenv); //What's the actor?
        reqType.setAttribute(actor);
        mustUnd = new Attribute("mustunderstand","0",soapenv);
        reqType.setAttribute(mustUnd);

        if ((reqTypeContent == null)||(reqTypeContent.length() == 0))
            reqTypeContent = "MMSSUBMIT.REQ";

        reqType.addContent(reqTypeContent);
        header.addContent(reqType);


        Element body = new Element("Body",soapenv);
        envelope.addContent(body);

        Element req = null;

        if(reqTypeContent.equals("MMSSUBMIT.REQ"))
        {
            req = new Element("MMSSubmitRequest");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("MMSSUBMIT.RESP"))
        {
            req = new Element("MMSSubmitResponse");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("MMSDELIVER.REQ"))
        {
            req = new Element("MMSDeliverRequest ");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("MMSDELIVER.RESP"))
        {
            req = new Element("MMSDeliverResponse");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("SMSDELIVER.REQ"))
        {
            req = new Element("SMSDeliverRequest");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("SMSDELIVER.RESP"))
        {
            req = new Element("SMSDeliverResponse");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("SMSSUBMIT.REQ"))
        {
            req = new Element("SMSSubmitRequest");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else if(reqTypeContent.equals("SMSSUBMIT.RESP"))
        {
            req = new Element("SMSSubmitResponse");
            if(iserver2!=null)req.addNamespaceDeclaration(iserver2);
        } else
        {
            XTTProperties.printFail(info+": not supported: "+reqTypeContent);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            throw new IllegalArgumentException("Unsupported parameter "+reqTypeContent);
        }

        body.addContent(req);

        /*Element mm7Version          = new Element("MM7Version",mms);
        mm7Version.addContent("5.6.0");
        submitReq.addContent(mm7Version);

        Element senderIdentification= new Element("SenderIdentification",mms);
        Element vaspIDElement       = null;
        Element vasIDElement        = null;
        if(vaspID!=null&&!vaspID.equals(""))
        {
            vaspIDElement = new Element("VASPID",mms);
            vaspIDElement.addContent(vaspID);
            senderIdentification.addContent(vaspIDElement);
            info=info+" VASPID='"+vaspID+"'";
        }
        if(vasID!=null&&!vasID.equals(""))
        {
            vasIDElement = new Element("VASID",mms);
            vasIDElement.addContent(vasID);
            senderIdentification.addContent(vasIDElement);
            info=info+" VASID='"+vasID+"'";
        }
        if(vaspIDElement!=null||vasIDElement!=null)submitReq.addContent(senderIdentification);

        Element recipients          = new Element("Recipients",mms);
        if(withRecipients)submitReq.addContent(recipients);*/

        return info;
    }

    private String createMM7Soap(String bodyType,boolean withRecipients, String vaspID, String vasID)
    {
        return createMM7Soap(bodyType,withRecipients,vaspID,vasID,true);
    }
    private String createMM7Soap(String bodyType,boolean withRecipients, String vaspID, String vasID, boolean withMustunderstand)
    {
        if(!isMM7)
        {
            isMM7=true;
            setMessageBoundary(new String[] {"setContentId","","","related"});
        }

        if(bodyType==null||bodyType.equals(""))bodyType="SubmitReq";
        String info="createMM7Soap: "+bodyType+":";

            String tmp=XTTProperties.getProperty("VASP/XMLNSENV");
            if(!tmp.equals("null"))
            {
                env = Namespace.getNamespace("env", tmp);
            }

            tmp=XTTProperties.getProperty("VASP/XMLNSMM7");
            if(!tmp.equals("null"))
            {
                mm7 = Namespace.getNamespace("mm7", tmp);
            }

            tmp=XTTProperties.getProperty("VASP/XMLNS");
            if(!tmp.equals("null"))
            {
                mms = Namespace.getNamespace(tmp);
            }

            soap                 = new Document();

            //Set main soap body up
            Element envelope            = new Element("Envelope",env);
            soap.addContent(envelope);

            Element header              = new Element("Header",env);
            envelope.addContent(header);

            Element transId             = new Element("TransactionID",mm7);
            if(withMustunderstand)
            {
                Attribute mustUnd       = new Attribute("mustUnderstand","1",env);
                transId.setAttribute(mustUnd);
            }
            transId.addContent(XTTProperties.getProperty("VASP/TRANSACTIONID"));
            header.addContent(transId);

            Element body                = new Element("Body",env);
            envelope.addContent(body);

            Element submitReq           = new Element(bodyType,mms);
            body.addContent(submitReq);

            Element mm7Version          = new Element("MM7Version",mms);
            tmp=XTTProperties.getProperty("VASP/MM7VERSION");
            if(!tmp.equals("null"))
            {
                mm7Version.addContent(tmp);
            } else
            {
                mm7Version.addContent("5.6.0");
            }
            submitReq.addContent(mm7Version);

            Element senderIdentification= new Element("SenderIdentification",mms);
            Element vaspIDElement       = null;
            Element vasIDElement        = null;
            if(vaspID!=null&&!vaspID.equals(""))
            {
                vaspIDElement = new Element("VASPID",mms);
                vaspIDElement.addContent(vaspID);
                senderIdentification.addContent(vaspIDElement);
                info=info+" VASPID='"+vaspID+"'";
            }
            if(vasID!=null&&!vasID.equals(""))
            {
                vasIDElement = new Element("VASID",mms);
                vasIDElement.addContent(vasID);
                senderIdentification.addContent(vasIDElement);
                info=info+" VASID='"+vasID+"'";
            }
            submitReq.addContent(senderIdentification);

            Element recipients          = new Element("Recipients",mms);
            if(withRecipients)submitReq.addContent(recipients);
            return info;
    }

    public void previewRequest(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": previewRequest:"+NO_ARGUMENTS);
            return;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }

        StringOutputStream buffer=new StringOutputStream();
        DataOutputStream outputStream=new DataOutputStream(buffer);
        try
        {
            outputStream.writeBytes("Headers:" + CRLF);

            Iterator<String> it=sendHeader.keySet().iterator();
            String hkey;
            String hval;
            String contentType=null;
            while(it.hasNext())
            {
                hkey=it.next();
                hval=sendHeader.get(hkey);
                if(hkey.equals("content-type"))contentType=hval;
                outputStream.writeBytes(hkey+": "+ hval + CRLF);
            }
            outputStream.writeBytes(CRLF+"Body:"+CRLF);

            streamBody(outputStream);
        }
        catch(Exception e)
        {
            XTTProperties.printFail("streamBody: Error streaming POST");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        XTTProperties.printInfo(parameters[0]+":\n"+buffer);
    }

    public void soapToVariable(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": soapToVariable: variableName");
            return;
        }
        if(parameters.length==2)
        {
            StringOutputStream sos=new StringOutputStream();
            XTTXML.streamXML(soap,sos);
            XTTProperties.setVariable(parameters[1],sos.toString());
            XTTProperties.printInfo(parameters[0]+": stored SOAP in variable='"+parameters[1]+"'");
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
    }


    /*public void addContent(String[] parameters)
    {
        if((parameters==null)||(parameters.length < 2))
        {
            XTTProperties.printFail(this.getClass().getName()+": addContent: file");
            //XTTProperties.printFail(this.getClass().getName()+": addContent: header header heade file");
            return;
        }
        setPostData(parameters);
    }*/

    /**
     * do a http POST request with the post data sent with header "content-type: application/x-www-form-urlencoded".
     * Set the header to  "content-type: multipart/form-data" to send as multipart.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code>
     *                     argument is the variable to store the body in value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendPostRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPostRequest:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": sendPostRequest: connectionName");
            XTTProperties.printFail(this.getClass().getName()+": sendPostRequest: connectionName url");
            return;
        }
        if((parameters.length>=1)&&(parameters.length<=3))
        {
            sendGetRequest(parameters,true);
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName url");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
    }

    private void sendGetRequest(String parameters[],boolean isPost)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest: connectionName");
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest: connectionName url");
            return;
        }
        else
        {
            String variableForBody = null;
            if(parameters.length > 1) variableForBody = parameters[1];

            //HTTPConnection connection=defaultConnection;
            HTTPConnection connection=HTTPHelper.getConnection(connections,defaultConnection,parameters); 
            try
            {
                String mm7proxyip=XTTProperties.getProperty("VASP/REMOTEPROXYIP");
                String mm7proxyport=XTTProperties.getProperty("VASP/REMOTEPROXYPORT");
                if(mm7proxyip.equals("null"))
                {
                    mm7proxyip=XTTProperties.getProperty("XMA/MM7PROXYIP");
                }
                if(mm7proxyport.equals("null"))
                {
                    mm7proxyport=XTTProperties.getProperty("XMA/MM7PROXYPORT");
                }

                if(!mm7proxyip.equals("null")&&!mm7proxyport.equals("null"))
                {
                    connection.setProxy(mm7proxyip,mm7proxyport);
                }
                if(isMM7)
                {
                    XTTProperties.printInfo(parameters[0]+": sending MM7 request");
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": sending TPI request");
                }
                URL url = null;

                if(parameters.length!=3)
                {
                    String remoteIP=XTTProperties.getProperty("VASP/REMOTEIP");
                    int remotePORT=XTTProperties.getIntProperty("VASP/REMOTEPORT");
                    if(remoteIP.equals("null"))
                    {
                        remoteIP=XTTProperties.getProperty("XMA/MM7IP");
                    }
                    if(remotePORT<=0)
                    {
                        remotePORT=XTTProperties.getIntProperty("XMA/MM7PORT");
                    }

                    url = new URL("http",remoteIP,remotePORT,"/");
                } else
                {
                    url = new URL(parameters[2]);
                }

                connection.getRequestHeader().clear();

                Iterator<String> it=sendHeader.keySet().iterator();
                String hkey;
                String hval;
                //String boundary="---------------------------114782935826962";

                String contentType=null;
                while(it.hasNext())
                {
                    hkey=it.next();
                    hval=sendHeader.get(hkey);
                    if(hkey.equalsIgnoreCase("content-type"))contentType=hval;
                    connection.getRequestHeader().put(hkey, hval);
                }

                if(isPost)
                {
                    if((postData==null)&&!contentType.equals("text/xml"))
                    {
                        XTTProperties.printFail(parameters[0] + ": POST data not set - please use setPostData first");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }

                    boolean hasPostData=true;
                    if(postData==null||postData.length==0)hasPostData=false;

                    try
                    {
                        StringBuffer postDataBuffer=new StringBuffer("");

                        ByteArrayOutputStream dataBuffer=new ByteArrayOutputStream();
                        streamBody(dataBuffer);
                        connection.setPostDataBytes(dataBuffer.toByteArray());

                        XTTProperties.printTransaction("VASP/HTTP/POST/SEND"+XTTProperties.DELIMITER+url+XTTProperties.DELIMITER+messageBoundary);
                    } catch (Exception e)
                    {
                        XTTProperties.printFail(parameters[0] + ": POST ERROR");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        XTTProperties.printException(e);
                        //e.printStackTrace();
                    }

                }

                connection.sendPostRequest(parameters[0],url.toExternalForm());

                receiveHeader=connection.getResponseHeader();
                serverResponseCode=new String[]{connection.getResponseCode()+"",connection.getResponseMessage()};

                serverResponse=connection.getResponse();

                if(variableForBody!=null)
                {
                    String body=ConvertLib.getStringFromOctetByteArray(serverResponse);
                    XTTProperties.printVerbose(parameters[0]+": response body stored in '"+variableForBody+"'");
                    XTTProperties.setVariable(variableForBody,body);
                }

                XTTProperties.printTransaction("VASP/HTTP/POST/RECEIVE"+XTTProperties.DELIMITER+url+XTTProperties.DELIMITER+messageBoundary+XTTProperties.DELIMITER+serverResponse.length);

            } catch (MalformedURLException mux)
            {
                XTTProperties.printFail(this.getClass().getName()+"."+parameters[0] + ": MalformedURLException");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    
    
    
   /**
    *  
     * do a http POST Delivery Report request with the post data sent with header "content-type: text/xml; charset=\"utf-8\"".
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is mmsStatus
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @param parameters
    */
    public void sendDRRequest(String parameters[])
    {

        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDRRequest: mmsStatus");
            return;
        }
        else if(parameters.length!=2)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDRRequest: mmsStatus");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }

        String drStatus = XTTProperties.getProperty("VASP/DN");
        if(null==drStatus||!drStatus.equalsIgnoreCase("true"))
        {
            XTTProperties.printFail(parameters[0]+": Delivery Report is not requested in the last received SubmitReq");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }

        String name = UUID.randomUUID().toString();
        String[] newparameters = { parameters[0], name };
        try
        {
            HTTPConnection connection = defaultConnection.createConnection(name);
            connections.put(name.toLowerCase(), connection);

            connection = HTTPHelper.getConnection(connections, defaultConnection, newparameters);
            XTTProperties.printInfo(parameters[0]+": created connection '"+name+"'");

            String remoteIP = XTTProperties.getProperty("VASP/REMOTEIP");
            int remotePORT = XTTProperties.getIntProperty("VASP/REMOTEPORT");
            URL url = new URL("http", remoteIP, remotePORT, "/");
            connection.getRequestHeader().clear();
            postData = createRequestSoap("drRequest",parameters[1]);
            Iterator<String> it = sendHeader.keySet().iterator();
            String hkey;
            String hval;
            String contentType = null;
            while (it.hasNext())
            {
                hkey = it.next();
                hval = sendHeader.get(hkey);
                if(hkey.equalsIgnoreCase("content-type"))
                    contentType = hval;
                connection.getRequestHeader().put(hkey, hval);
            }
            connection.setPostDataBytes(postData);
            XTTProperties.printTransaction("VASP/DR/POST/SEND"+XTTProperties.DELIMITER+url+XTTProperties.DELIMITER+messageBoundary);

            connection.sendPostRequest(parameters[0], url.toExternalForm());

            receiveHeader = connection.getResponseHeader();
            serverResponseCode = new String[] { connection.getResponseCode()+"", connection.getResponseMessage() };
            serverResponse = connection.getResponse();

        } catch (Exception e)
        {
            XTTProperties.printFail(parameters[0]+": POST ERROR");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printException(e);

        } finally
        {
            HTTPHelper.getConnection(connections, defaultConnection, newparameters).closeConnection();
            XTTProperties.printInfo(parameters[0]+": closed connection.");
        }

    }

    /**
     *  
      * do a http POST Read Reply  request with the post data sent with header "content-type: text/xml; charset=\"utf-8\"".
      *
      * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
      *                     the method name, <code>parameters[1]</code> argument is mmsStatus(accepted, rejected or deleted)
      *                     If null is used as <code>parameters</code> it sends the allowed parameters list
      *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
      * @param parameters
     */
     public void sendRRRequest(String parameters[])
     {

         if(parameters==null)
         {
             XTTProperties.printFail(this.getClass().getName()+": sendRRRequest: mmsStatus");
             return;
         }
         else if(parameters.length!=2)
         {
             XTTProperties.printFail(this.getClass().getName()+": sendRRRequest: mmsStatus");
             XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
             return;
         }
         String rrStatus = XTTProperties.getProperty("VASP/RR");
         if(null==rrStatus||!rrStatus.equalsIgnoreCase("true"))
         {
             XTTProperties.printFail(parameters[0]+": Read Reply is not requested in the last received SubmitReq");
             XTTProperties.setTestStatus(XTTProperties.FAILED);
             return;
         }

         String name = UUID.randomUUID().toString();
         String[] newparameters = { parameters[0], name };
         try
         {
             HTTPConnection connection = defaultConnection.createConnection(name);
             connections.put(name.toLowerCase(), connection);

             connection = HTTPHelper.getConnection(connections, defaultConnection, newparameters);
             XTTProperties.printInfo(parameters[0]+": created connection '"+name+"'");

             String remoteIP = XTTProperties.getProperty("VASP/REMOTEIP");
             int remotePORT = XTTProperties.getIntProperty("VASP/REMOTEPORT");
             URL url = new URL("http", remoteIP, remotePORT, "/");
             connection.getRequestHeader().clear();
             postData = createRequestSoap("rrRequest",parameters[1]);
             Iterator<String> it = sendHeader.keySet().iterator();
             String hkey;
             String hval;
             String contentType = null;
             while (it.hasNext())
             {
                 hkey = it.next();
                 hval = sendHeader.get(hkey);
                 if(hkey.equalsIgnoreCase("content-type"))
                     contentType = hval;
                 connection.getRequestHeader().put(hkey, hval);
             }
             connection.setPostDataBytes(postData);
             XTTProperties.printTransaction("VASP/DR/POST/SEND"+XTTProperties.DELIMITER+url+XTTProperties.DELIMITER+messageBoundary);

             connection.sendPostRequest(parameters[0], url.toExternalForm());

             receiveHeader = connection.getResponseHeader();
             serverResponseCode = new String[] { connection.getResponseCode()+"", connection.getResponseMessage() };
             serverResponse = connection.getResponse();

         } catch (Exception e)
         {
             XTTProperties.printFail(parameters[0]+": POST ERROR");
             XTTProperties.setTestStatus(XTTProperties.FAILED);
             XTTProperties.printException(e);

         } finally
         {
             HTTPHelper.getConnection(connections, defaultConnection, newparameters).closeConnection();
             XTTProperties.printInfo(parameters[0]+": closed connection.");
         }

     }
    private void streamBody(OutputStream buffer) throws java.io.IOException
    {
        boolean hasPostData=true;
        if(postData==null||postData.length==0)hasPostData=false;

        DataOutputStream outputStream=new DataOutputStream(buffer);
        if(hasPostData)
        {
            outputStream.writeBytes("--" + messageBoundary+CRLF);
            outputStream.writeBytes("content-type: text/xml; charset=\"utf-8\""+CRLF);
            if(isMM7||useContentID)
            {
                outputStream.writeBytes("Content-ID: "+ contentId +CRLF);
            }
            outputStream.writeBytes(CRLF);

            outputStream.flush();
        }
        XTTXML.streamXML(soap,outputStream);
        outputStream.writeBytes(CRLF);
        if(hasPostData)
        {
            if(isMM7)
            {
                outputStream.writeBytes("--" + messageBoundary+CRLF);
            }
            outputStream.write(postData);
            outputStream.writeBytes("--" + messageBoundary + "--"+CRLF);
        }
        outputStream.flush();
    }

    public void checkCertificate(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkCertificate:"+NO_ARGUMENTS);//+";no arguments, means check for no certificates]");
            XTTProperties.printFail(this.getClass().getName()+": checkCertificate: Issuer SerialNumber");
            return;
        }
        if(parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": sleepTimeInMilliseconds");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else if(parameters.length!=3)
        {
            XTTProperties.printInfo(parameters[0] + ": Checking no certificates were provided");
            if(certs.length != 0)
            {
                XTTProperties.printFail(parameters[0] + ":"+ certs.length +" certificate(s) were found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": Checking for certificate from " + parameters[1] + " with serial " +parameters[2]);
            XTTProperties.printInfo(parameters[0]+": HTTPS: "+certs.length+" certificates");
            java.security.cert.X509Certificate x509=null;
            for(int i=0;i<certs.length;i++)
            {
                if(certs[i].getType().equals("X.509"))
                {
                    x509=(java.security.cert.X509Certificate)certs[i];
                    if((parameters[1].equalsIgnoreCase(x509.getIssuerX500Principal().getName()))&&
                       (parameters[2].equalsIgnoreCase(""+x509.getSerialNumber())))
                    {
                        XTTProperties.printInfo(parameters[0]+": Certificate["+i+"]: found from Issuer: "+x509.getIssuerX500Principal().getName()+" with serial " + x509.getSerialNumber());
                        XTTProperties.printDebug(parameters[0]+": "+i+":serial: "+x509.getSerialNumber() );
                        XTTProperties.printDebug(parameters[0]+": "+i+":algorithm: "+x509.getSigAlgName());
                        XTTProperties.printDebug(parameters[0]+": "+i+":X500Principal: "+x509.getIssuerX500Principal().getName());
                        XTTProperties.printDebug(parameters[0]+": "+i+":DN: "+x509.getSubjectDN());
                        XTTProperties.printDebug(parameters[0]+": "+i+":NotAfter: " + x509.getNotAfter());
                        return;
                    }
                }
            }
            XTTProperties.printFail(parameters[0] + ": no matching certificate was found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument and following are the allowed response codes.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
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
                if(serverResponseCode[0].equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider=",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": found "+serverResponseCode[0]+" "+serverResponseCode[1]);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": found "+serverResponseCode[0]+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
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
            byte[] body=getBody();
            if(body==null)
            {
                XTTProperties.printFail(parameters[0]+": no body to check, is "+body);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
            return ConvertLib.queryString(parameters[0],ConvertLib.createString(body),parameters[2],parameters[1]);
        }
    }

    /**
     * Create a datestring with the current time and specified formating. The used Locale will be US.
     * Default timezone is system timezone. Default no seconds will be added to the time value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> is a time format allowed by java.text.SimpleDateFormat.
     *                     <code>parameters[2]</code> is the variable name to store the created datestring in.
     *                     <code>parameters[3]</code> (optional) numer of seconds to add to the time value. (3600, -3600 etc.)
     *                     <code>parameters[4]</code> (optional, requeires parameter[3]) Timezone to use in java.util.TimeZone.getTimeZone(parameter[4]) format.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void createDateString(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName secondsToAdd");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName secondsToAdd timezone");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName secondsToAdd timezone variableForEpochVal");
            return;
        }
        if(parameters.length<2||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName secondsToAdd");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName secondsToAdd timezone");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName secondsToAdd timezone variableForEpochVal");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String funcName     =parameters[0];
            String varName      =parameters[1];
            String epochvarName = null;
            String secondsToAdd = null;
            String timeZone     = null;
            long epochDate=-1;
                if(parameters.length==5)
                {
                    epochvarName=parameters[4];
                }
                if(parameters.length>=4)
                {
                    timeZone=parameters[3];
                    if(timeZone.equals(""))timeZone=null;
                }
                if(parameters.length>=3)
                {
                    secondsToAdd=parameters[2];
                }
            createDate(funcName,varName,secondsToAdd,timeZone,epochDate,epochvarName);
        }
    }
    public void createDateStringFromEpoch(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName epochDate");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: variableName epochDate secondsToAdd");
            return;
        }
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName epochDate");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName epochDate secondsToAdd");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String funcName     =parameters[0];
            String varName      =parameters[1];
            String epochvarName = null;
            String secondsToAdd = null;
            String timeZone     = null;
            long epochDate=Long.parseLong(parameters[2]);
            long maxSec=9999999999l;
            if(epochDate<maxSec)epochDate=epochDate*1000;
            if(parameters.length>=4)
            {
                secondsToAdd=parameters[3];
            }
            createDate(funcName,varName,secondsToAdd,timeZone,epochDate,epochvarName);
        }
    }
    private void createDate(String funcName,String varName, String secondsToAdd, String timeZone, long epochDate, String epochvarName)
    {
        String dateFormat="yyyy'-'MM'-'dd'T'HH':'mm':'ss";
        try
        {
            String datestring="";
            String timezone="";
            SimpleDateFormat format=new SimpleDateFormat(dateFormat,Locale.US);
            GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
            if(epochDate>0)
            {
                calendar.setTime(new java.util.Date(epochDate));
            }
            if(timeZone!=null)
            {
                TimeZone z=TimeZone.getTimeZone(timeZone);
                format.setTimeZone(z);
            }/* else
            {
                TimeZone z=TimeZone.getTimeZone("GMT");
                format.setTimeZone(z);
            }*/
            if(secondsToAdd!=null)
            {
                calendar.add(GregorianCalendar.SECOND,Integer.parseInt(secondsToAdd));
            }
            TimeZone z=format.getTimeZone();
            int zone=z.getOffset(calendar.getTimeInMillis())/1000/3600;
            String plus="";if(zone>=0)plus="+";
            if(timeZone!=null&&timeZone.equalsIgnoreCase("GMT"))
            {
                timezone="Z";
            } else if(zone<10&&zone>=0)
            {
                timezone=plus+"0"+zone+":00";
            } else if(zone>-10&&zone<0)
            {
                timezone="-"+"0"+Math.abs(zone)+":00";
            } else
            {
                timezone=plus+zone+":00";
            }

            datestring=format.format(calendar.getTime());
            XTTProperties.printInfo(funcName+": date '"+datestring+timezone+"' stored in variable "+varName);
            XTTProperties.setVariable(varName,datestring+timezone);
            if(epochvarName!=null)
            {
                XTTProperties.printInfo(funcName+": date '"+(calendar.getTimeInMillis()/1000)+"' stored in variable "+epochvarName);
                XTTProperties.setVariable(epochvarName,(calendar.getTimeInMillis()/1000)+"");
            }
        } catch (java.lang.NullPointerException npx)
        {
            XTTProperties.printFail(funcName+": '"+dateFormat+"' IS NOT a correct date format");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } catch (java.lang.IllegalArgumentException iax)
        {
            XTTProperties.printFail(funcName+": '"+dateFormat+"' IS NOT a correct date format");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        } catch (Exception ex)
        {
            XTTProperties.printFail(funcName+": '"+secondsToAdd+"' IS NOT a correct number");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    
    
    private byte[] createRequestSoap(String reqType, String mmsStatus)
    {       
        String msgid=null;
        String requestTag = null;
        try
        {
            
            if(reqType.equalsIgnoreCase("drRequest")) 
            {
                requestTag ="DeliveryReportReq";  
            }
            else if ((reqType.equalsIgnoreCase("rrRequest")))
            {
                requestTag ="ReadReplyReq";  
            }
            DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
            DocumentBuilder db =dbf.newDocumentBuilder();
            org.w3c.dom.Document  doc = db.parse(new InputSource(new StringReader(XTTProperties.getVariable("VASP/BODY/PLAIN"))));
           
            if(doc.getElementsByTagName("MessageID").getLength() >=1)
            {
             msgid = doc.getElementsByTagName("MessageID").item(0).getTextContent(); 
             messageID = msgid;
            }
            
            
        } catch (Exception e)
        { 
            XTTProperties.printFail(reqType+" : MessageID not found in SubmitRsp");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        StringBuffer soap=new StringBuffer("<?xml version=\"1.0\"?>"+"\n");     
        soap.append("<env:Envelope xmlns:env=\""+env.getURI()+"\" >"+"\n");
        soap.append("   <env:Header>"+"\n");
        soap.append("       <mm7:TransactionID xmlns:mm7=\""+mm7.getURI()+"\" env:mustUnderstand=\"1\">"+"xttvasp-0001"+"</mm7:TransactionID>"+"\n");
        soap.append("   </env:Header>"+"\n");
        soap.append("<env:Body>"+"\n");
       
        soap.append("   <"+requestTag+" xmlns=\""+mm7.getURI()+"\">"+"\n");
        soap.append("       <MM7Version>"+MMSCWorker.mm7version+"</MM7Version>"+"\n");
        soap.append("       <MessageID>"+messageID+"</MessageID>"+"\n");
        soap.append("       <Date>"+new Date()+"</Date>"+"\n");
        soap.append("       <Recipient>"+"\n");
        soap.append("           <To>"+XTTProperties.getProperty("VASP/NUMBER", "")+"</To>"+"\n");     
        soap.append("       </Recipient>"+"\n");
        soap.append("       <Sender>"+"\n");
        String retVar = null;  
        if (!"null".equalsIgnoreCase(XTTProperties.getVariable("MMSCSERVER/RECIPIENTS")))
        {
            retVar ="MMSCSERVER/RECIPIENTS";
        }
       else if(!"null".equalsIgnoreCase( XTTProperties.getVariable("MMSCSERVER/RECIPIENTS/TO")))
        {
            retVar ="MMSCSERVER/RECIPIENTS/TO";
        }
        if (!"null".equalsIgnoreCase( XTTProperties.getVariable(retVar+"/NUMBER")))
        {
            XTTProperties.printInfo(XTTProperties.getVariable(retVar+"/NUMBER"));
            soap.append("           <Number>"+XTTProperties.getVariable(retVar+"/NUMBER")+"</Number>"+"\n");  
        }
        if (!"null".equalsIgnoreCase(XTTProperties.getVariable(retVar+"/RFC822AD")))
        {
            soap.append("           <rfc822ad>"+XTTProperties.getVariable(retVar+"/RFC822AD")+"</rfc822ad>"+"\n");
        }
        if (!"null".equalsIgnoreCase( XTTProperties.getVariable(retVar+"/SHORTCODE")))
        {
            soap.append("           <ShortCode>"+XTTProperties.getVariable(retVar+"/SHORTCODE")+"</ShortCode>"+"\n");
        }
        soap.append("       </Sender>"+"\n");
        soap.append("       <MMStatus>"+mmsStatus+"</MMStatus>"+"\n");
        soap.append("   </"+requestTag+">"+"\n");        
        soap.append("</env:Body>"+"\n");
        soap.append("</env:Envelope>"+"\n");
        
        byte[] data=ConvertLib.createBytes(soap.toString());
        String[] header1 ={"","content-length",""+data.length} ; 
        HTTPHelper.setHeader(this.sendHeader,header1);
        
        String[] header2 ={"","content-type","text/xml; charset=\"utf-8\""} ;
        HTTPHelper.setHeader(this.sendHeader,header2);
        return data;
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_VASP.java,v 1.27 2010/05/12 07:16:43 awadhai Exp $";
}
