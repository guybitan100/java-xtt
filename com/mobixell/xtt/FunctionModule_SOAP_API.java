package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.net.URL;
import java.io.StringReader;
import org.jdom.Document;
import org.jdom.Element;
import org.xml.sax.InputSource;

/**
* FunctionModule_SOAP_API provides SOAPClient function.
*
* @author      Ketan Tank
* @version     $Id: FunctionModule_SOAP_API.java,v 1.14 2007/06/21 08:47:12 rsoder Exp $
*/

public class FunctionModule_SOAP_API extends FunctionModule
{
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SOAP_API.java,v 1.14 2007/06/21 08:47:12 rsoder Exp $";
    private HTTPConnection defaultConnection = null;
    private static String  pathTree = null;
    private static LinkedHashMap<String,Vector<String>> receiveHeader = new LinkedHashMap<String,Vector<String>>();
    private static String serverResponseCode[]  = new String[]{"","not yet initialized"};
    private static byte[] serverResponse = null;
    private String SOAPUrl      = null ;
    private String xmlFile2Send = null ;
    private String SOAPAction   = "";
    private org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
    private static Element dataElement = null;

    /**
    * clears and reinitializes all the internal variables.
    */
    public void initialize()
    {
        defaultConnection=new HTTPConnection("DEFAULT","SOAP_API");
        defaultConnection.readConfiguration();
        receiveHeader       = new LinkedHashMap<String,Vector<String>>();
        serverResponseCode  = new String[]{"","not yet initialized"};
        serverResponse      = null;
        SOAPUrl             = null ;
        xmlFile2Send        = null ;
        SOAPAction          = "";
        parser              = new org.jdom.input.SAXBuilder();
        dataElement         = null ;
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }
    public String getConfigurationOptions()
    {
        return "    <!-- function module SOAP_API -->"
        +"\n    <SOAP_API>"
        +HTTPConnection.getConfigurationOptions()
        +"\n    </SOAP_API>";
    }

    /**
    * constructor sets
    */
    public FunctionModule_SOAP_API()
    {
    }

    /**
    * do a http POST request.
    *
    * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
    * the method name, <code>parameters[1]</code>
    * argument and following are concatenated together to the url value.
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    */
    public void SOAPClient(String parameters[])
    {
        sendSOAPMessage(parameters);
    }


    private void sendSOAPMessage(String parameters[])
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSOAPMessage: SOAPUrl, xmlFile2Send, pathTree");
            return;
        }
        if(parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": SOAPUrl, xmlFile2Send, pathTree");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printDebug(parameters[0] + ": SOAPUrl is: "+ parameters[1] + "'");
            //XTTProperties.printDebug("xmlFile2Send  '\n"+parameters[2]+"'");
            SOAPUrl        =   parameters[1] ;
            xmlFile2Send   =   parameters[2] ;
            pathTree       =   parameters[3] ;
        }

        try
        {
            // Create the connection where we're going to send the file.
            URL url                     = new URL(SOAPUrl);
            HTTPConnection connection  = defaultConnection;

            byte[] postData =ConvertLib.createBytes(xmlFile2Send) ;
            connection.setPostDataBytes(postData);

            // Set the appropriate HTTP parameters.
            connection.getRequestHeader().clear();
            connection.getRequestHeader().put("Content-Type","text/xml; charset=utf-8");
            connection.getRequestHeader().put("SOAPAction",SOAPAction);

            // Everything's set up; send the XML that was read in to b.
            connection.sendPostRequest(parameters[0],url.toExternalForm());

            receiveHeader=connection.getResponseHeader();
            serverResponseCode=new String[]{connection.getResponseCode()+"",connection.getResponseMessage()};
            serverResponse=connection.getResponse();
            String body=ConvertLib.getStringFromOctetByteArray(serverResponse);

            try
            {
                Element root 	        = new Element("soapenv");
                Document responseXML  	= new Document(root);
                Element element 		= responseXML.getRootElement();

                responseXML = parser.build(new InputSource(new StringReader(body))) ;
                XTTProperties.printVerbose(parameters[0] + ": Response received ("+serverResponse.length+" bytes) START:\n" + XTTXML.stringXML(responseXML) + "\nResponse received END:");
                XTTProperties.printDebug(parameters[0] + ": Root element is  '" + element + "'");
                dataElement = XTTXML.getElement(pathTree, responseXML);
                XTTProperties.printDebug(parameters[0] + ": pathTree is '" + pathTree + "'");
            } catch(org.jdom.JDOMException je)
            {
                XTTProperties.printFail(parameters[0] + ": POST RESPONSE ERROR");
                XTTProperties.printException(je);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
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

    public static String getResponse()
    {
        String acctualdata = null;
        if(pathTree != null)
        {
            if(dataElement != null)
            {
                acctualdata = dataElement.getText();
            }
        }
        return acctualdata;
    }

    public boolean queryResponse(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": queryResponse: regularExpression variableName");
            return false;
        }
        if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": regularExpression variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            //XTTProperties.printDebug(parameters[0] + ": Text for Data Element " + dataElement +" is " + getResponse() + "'");
            XTTProperties.printDebug(parameters[0] + ": regex String is : '" + parameters[1] + "'");
            //XTTProperties.printInfo(parameters[0] +  ":" + " Actual Result is: " + getResponse()+ " AND " +" Expected Result is: " + parameters[1]);
            return ConvertLib.queryString(parameters[0], getResponse(), parameters[1], parameters[2]);
        }
    }

    /**
    * compare the http response code of the last POST/GET request with a value.
    *
    * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
    *                     the method name, <br><code>parameters[1]</code> and following are the allowed response codes.
    *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
    *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *
    */
    public void checkResponseCode(String parameters[])
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+ ": checkResponseCode: expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length < 2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            boolean found = false;
            StringBuffer checked = new StringBuffer();
            String divider = "";
            for(int i=1 ; i<parameters.length ; i++)
            {
                if(serverResponseCode[0].equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider = ",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": Found "+serverResponseCode[0]+" "+serverResponseCode[1]);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": Found "+serverResponseCode[0]+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
    * returns a byte array containing the body part of the last request
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
    * returns the getClass().getName() of this object. For debug reasons only.
    */
    public String toString()
    {
        return this.getClass().getName();
    }

}