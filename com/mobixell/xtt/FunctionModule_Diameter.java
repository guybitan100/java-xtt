package com.mobixell.xtt;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import com.mobixell.xtt.diameter.DiameterConnection;
import com.mobixell.xtt.diameter.DiameterConstants;
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
/**
 * FunctionModule_Diameter provides Diameter and DiameterS GET functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_Diameter.java,v 1.17 2011/02/17 10:52:34 rajesh Exp $
 */
public class FunctionModule_Diameter extends FunctionModule implements DiameterConstants
{
    private Map<String,DiameterConnection> connections        = Collections.synchronizedMap(new HashMap<String,DiameterConnection>());
    private DiameterServer 		 		   s             	  = null;
    private Thread               		   ws                 = null;
    private DiameterConnection   		   defaultConnection  = null;
    private DiameterConnection   		   conn  			  = null;


    /**
     * clears and reinitializes all the variables. Does reset the Diameter.
     */
    public void initialize()
    {
        DiameterWorkerServer.init();
        DiameterServer.resetWorkerId();
        String enableExtendedOutput=XTTProperties.getQuietProperty("DIAMETERSERVER/ENABLEEXTENDEDOUTPUT");
        if(!enableExtendedOutput.equals("null"))
        {
            DiameterWorkerServer.setExtendedOutput(true);
        }
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        conn = new DiameterConnection();
        defaultConnection=new DiameterConnection("DEFAULT","DIAMETER");
        defaultConnection.readConfiguration();
    }


    /**
     * constructor sets Diameter.
     */
    public FunctionModule_Diameter()
    {
        //do not do this, parser will initialize!
        //initialize();
    }

    /**
     * Overriden from superclass to add the DiameterServer and DiameterWorker version numbers.
     *
     * @see DiameterServer
     * @see DiameterWorkerServer
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": DiameterServer: "+parseVersion(DiameterServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": DiameterWorker: "+parseVersion(DiameterWorkerServer.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": DiameterServer: ",SHOWLENGTH) + parseVersion(DiameterServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": DiameterWorker: ",SHOWLENGTH) + parseVersion(DiameterWorkerServer.tantau_sccsid));
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module Diameter -->"
        +"\n    <DiameterServer>"
        +"\n        <!-- the udp/tcp listening port of the internal DiameterServer -->"
        +"\n        <Port>"+DiameterServer.DEFAULTPORT+"</Port>"
        +"\n        <!-- the listening port of the internal secure-DiameterServer -->"
        +"\n        <SecurePort>"+DiameterServer.DEFAULTSECUREPORT+"</SecurePort>"
        +"\n        <!-- The automatic response of the DiameterServer, use a filename or inline the document ass seen here -->"
        +"\n        <ResponseDocument>"
        +"\n            <!-- "
        +"\n            <Diameter>"
        +"\n                <response>"
        +"\n                    <ACCOUNTING_REQUEST proxiable=\"true\" error=\"false\" tretr=\"false\" matchAVP=\"USER_NAME\" matchREGEX=\"user1\" responsedelay=\"0\">"
        +"\n                        <SESSION_ID         request=\"SESSION_ID\"        mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <USER_NAME          request=\"USER_NAME\"         mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <RESULT_CODE        value=\"DIAMETER_SUCCESS\"    mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <CC_MONEY                                         mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\">"
        +"\n                            <CC_REQUEST_TYPE    value=\"EVENT_REQUEST\"   mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                            <CC_TIME            value=\"100\"             mandatory=\"false\" protected=\"false\" vendorspecific=\"true\"  vendorID=\"1000\"/>"
        +"\n                        </CC_MONEY>"
        +"\n                    </ACCOUNTING_REQUEST>"
        +"\n                    <ACCOUNTING_REQUEST proxiable=\"true\" error=\"true\" tretr=\"false\">"
        +"\n                        <SESSION_ID         request=\"SESSION_ID\"        mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <USER_NAME          request=\"USER_NAME\"         mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <TERMINATION_CAUSE  value=\"DIAMETER_USER_MOVED\" mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <RESULT_CODE        value=\"DIAMETER_AUTHORIZATION_REJECTED\"    mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ERROR_MESSAGE      value=\"He's not here, Doh!\" mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                        <CC_MONEY                                         mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\">"
        +"\n                            <CC_REQUEST_TYPE    value=\"EVENT_REQUEST\"   mandatory=\"true\"  protected=\"false\" vendorspecific=\"false\" vendorID=\"\"/>"
        +"\n                            <CC_TIME            value=\"200\"             mandatory=\"false\" protected=\"false\" vendorspecific=\"true\"  vendorID=\"1000\"/>"
        +"\n                        </CC_MONEY>"
        +"\n                    </ACCOUNTING_REQUEST>"
        +"\n                    <DEVICE_WATCHDOG_REQUEST proxiable=\"true\" error=\"false\" tretr=\"false\">"
        +"\n                        <RESULT_CODE            value=\"DIAMETER_SUCCESS\"    mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_HOST            value=\"diameter.xtt724.com\" mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_REALM           value=\"ORIGIN_REALM\"        mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                    </DEVICE_WATCHDOG_REQUEST>"
        +"\n                    <CAPABILITIES_EXCHANGE_REQUEST proxiable=\"true\" error=\"false\" tretr=\"false\">"
        +"\n                        <RESULT_CODE            value=\"DIAMETER_SUCCESS\"    mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_HOST            value=\"diameter.xtt724.com\" mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_REALM           value=\"ORIGIN_REALM\"        mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <VENDOR_ID              request=\"VENDOR_ID\" />"
        +"\n                        <PRODUCT_NAME           request=\"PRODUCT_NAME\" />"
        +"\n                        <SUPPORTED_VENDOR_ID    request=\"SUPPORTED_VENDOR_ID\" />"
        +"\n                        <AUTH_APPLICATION_ID    request=\"AUTH_APPLICATION_ID\" />"
        +"\n                    </CAPABILITIES_EXCHANGE_REQUEST>"
        +"\n                    <CREDIT_CONTROL_REQUEST proxiable=\"true\" error=\"false\" tretr=\"false\">"
        +"\n                        <SESSION_ID             request=\"SESSION_ID\"/>"
        +"\n                        <RESULT_CODE            value=\"DIAMETER_SUCCESS\"    mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_HOST            value=\"diameter.xtt724.com\" mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <ORIGIN_REALM           request=\"DESTINATION_REALM\" mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <AUTH_APPLICATION_ID    value=\"4\"                   mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <CC_REQUEST_TYPE        request=\"CC_REQUEST_TYPE\"   mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <CC_REQUEST_NUMBER      request=\"CC_REQUEST_NUMBER\" mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        <GRANTED_SERVICE_UNIT                                 mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\">"
        +"\n                            <CC_MONEY                                         mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\">"
        +"\n                                <UNIT_VALUE                                   mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\">"
        +"\n                                    <VALUE_DIGITS    value=\"1000\"           mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                                    <EXPONENT        value=\"-3\"             mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                                    <CURRENCY_CODE   value=\"756\"            mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                                </UNIT_VALUE>"
        +"\n                            </CC_MONEY>"
        +"\n                            <CC_SERVICE_SPECIFIC_UNITS value=\"1\"            mandatory=\"true\" protected=\"false\" vendorspecific=\"false\"  vendorID=\"\"/>"
        +"\n                        </GRANTED_SERVICE_UNIT>"
        +"\n                    </CREDIT_CONTROL_REQUEST>"
        +"\n                </response>"
        +"\n            </Diameter>"
        +"\n            -->"
        +"\n        </ResponseDocument>"
        +"\n        <!-- timeout on client connections to the DiameterServer -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
        +"\n        <waitTimeout>30000</waitTimeout>"
        +"\n        <!-- in case the default debug output is not enough enable this"
        +"\n        <enableExtendedOutput/>"
        +"\n        -->"
        +"\n    </DiameterServer>"
        ;
    }
    /**
     * Creates a new connection.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument the name for the connection, <br>
     *            <code>parameters[2]</code> argument is the ip address of the server the clients connects to, <br>
     *            <code>parameters[3]</code> argument is the port the client connects to on the server, <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see XTTProperties
     */
    public int createConnection(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createConnection: name serverIP serverPort");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name serverIP serverPort");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            String name=parameters[1];
            String ipAddress=parameters[2];
            String serverPort=parameters[3];

            try
            {
                DiameterConnection connection=conn.createConnection(name,ipAddress,serverPort);
                connections.put(name.toLowerCase(),connection);
                DiameterConnection.getConnection(connections,defaultConnection,parameters);
                XTTProperties.printInfo(parameters[0] + ": created connection '"+name+"'");
            } catch (Exception uhe)
            {
                XTTProperties.printFail(parameters[0] + ":: Unable to create connection '"+name+"': "+uhe.getMessage());
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
	            XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
                return status;
            }
        }
        return status;
    }
    /**
     * Send a DIAMETER request.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> request xml document,
     *                     <br><code>parameters[3]</code> variable name to store response[optional],
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterConnection#sendDiameterRequest
     */
    public int sendDiameterRequest(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDiameterRequest: name reqDocument");
            XTTProperties.printFail(this.getClass().getName()+": sendDiameterRequest: name reqDocument variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length < 3 || parameters.length > 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name reqDocument");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name reqDocument variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            DiameterConnection connection = DiameterConnection.getConnection(connections,defaultConnection,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
            String funcname           = parameters[0];
            String connectionName     = parameters[1];
            String reqDocument        = parameters[2];
            String variableName       = null;
            if(parameters.length == 4)
            {
                 variableName       = parameters[3];
            }

            try
            {
            	status=connection.sendDiameterRequest(funcname, reqDocument,connectionName,variableName,true,true);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                connection.closeConnection();
                return status;
            }
        }
        return status;
    }

    /**
     * Send a DIAMETER response. Does not read from stream.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> response xml document,
     *                     <br><code>parameters[3]</code> variable name to store response[optional],
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterConnection#sendDiameterRequest
     */
    public int sendDiameterResponse(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDiameterRequest: name resDocument");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length < 3 || parameters.length > 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name resDocument");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            DiameterConnection connection = DiameterConnection.getConnection(connections,defaultConnection,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            String funcname           = parameters[0];
            String connectionName     = parameters[1];
            String resDocument        = parameters[2];
            String variableName       = null;

            try
            {
                connection.sendDiameterRequest(funcname, resDocument,connectionName,variableName,false,false);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
        return status;
    }
    /**
     * Read a DIAMETER request, does not send a response.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> variable name to store response[optional],
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterConnection#sendDiameterRequest
     */
    public int readDiameterRequest(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": readDiameterRequest: name");
            XTTProperties.printFail(this.getClass().getName()+": readDiameterRequest: name variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length < 2 || parameters.length > 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            DiameterConnection connection = DiameterConnection.getConnection(connections,defaultConnection,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            String funcname           = parameters[0];
            String connectionName     = parameters[1];
            String variableName       = null;
            if(parameters.length == 3)
            {
                 variableName       = parameters[2];
            }

            try
            {
                connection.readRequest(funcname, connectionName,variableName);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
        return status;
    }

   /**
     * starts the Diameterserver as an instance of DiameterServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the DiameterServer port,
     *                     <br><code>parameters[2]</code> is the DiameterServer root directory,
     *                     <br><code>parameters[3]</code> argument is the DiameterServer timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterServer
     * @see XTTProperties
     */
    public int startDiameterServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startDiameterServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startDiameterServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startDiameterServer: port rootDirectory timeOut");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return startDiameterServer(parameters,false);
    }

    /**
     * starts the Diameterserver as an instance of DiameterServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the DiameterServer port,
     *                     <br><code>parameters[2]</code> is the DiameterServer root directory,
     *                     <br><code>parameters[3]</code> argument is the DiameterServer timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterServer
     * @see XTTProperties
     */
    public int startSecureDiameterServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureDiameterServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureDiameterServer: port");
            XTTProperties.printFail(this.getClass().getName()+": startSecureDiameterServer: port responseDocument timeOut");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return startDiameterServer(parameters,true);
    }

    private int startDiameterServer(String parameters[],boolean secure)
    {
    	int status = XTTProperties.PASSED;
        String securetext="non-secure";
        if(secure)securetext="secure";
        if(parameters.length!=1&&parameters.length!=2&parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port responseDocument timeOut");
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        if(parameters.length == 1||parameters.length == 2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" Diameter Server");
                String porttype="";
                if(secure)porttype="SECURE";
                String respDocumentS=XTTProperties.getProperty("DIAMETERSERVER/RESPONSEDOCUMENT");
                java.io.File f=new java.io.File(respDocumentS);
                Document respDocument=null;
                if(f.exists())
                {
                    respDocument=XTTXML.readXML(respDocumentS);
                    if(respDocument==null)
                    {
                    	status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);
                        return status;
                    }
                } else
                {
                    Element element=XTTXML.getElement("DIAMETERSERVER/RESPONSEDOCUMENT/DIAMETER");

                    if(element==null)
                    {
                        XTTProperties.printFail(parameters[0]+": Diameter Response Document missing");
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);
                        return status;
                    }

                    element=(Element)element.clone();
                    element.detach();
                    respDocument=new Document(element);
                }

                int port=XTTProperties.getIntProperty("DIAMETERSERVER/"+porttype+"PORT");
                if(parameters.length ==2)
                {
                    port=Integer.decode(parameters[1]);
                }
              //Guy only one webserver can we run with the same ip/port
				if (s != null) 
				{
					if (s.getPort() == port) 
					{
						if (s.isAlive()) 
						{
							s.stopGracefully();
						}
					}
				}
                s = new DiameterServer(null,port,respDocument,XTTProperties.getIntProperty("DIAMETERSERVER/TIMEOUT"), secure);
                ws=(new Thread(s, "DiameterServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" Diameter Server");
                return status;
            } catch(FileNotFoundException fnfe)
            {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException "+fnfe.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(fnfe);
                }
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } 
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        } else if (parameters.length == 4)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting "+securetext+" Diameter Server");
                Document respDocument=null;
                respDocument=XTTXML.readXMLFromString(parameters[2]);
                if(respDocument==null)
                {
                	status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                    return status;
                }
              //Guy only one webserver can we run with the same ip/port
				if (s != null) 
				{
					if (s.getPort() == Integer.decode(parameters[1])) 
					{
						if (s.isAlive()) 
						{
							s.stopGracefully();
						}
					}
				}
                s = new DiameterServer(null, Integer.decode(parameters[1]), respDocument,Integer.decode(parameters[3]), secure);
                ws=(new Thread(s, "DiameterServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started "+securetext+" Diameter Server");
                return status;
            } catch(FileNotFoundException fnfe)
            {
                XTTProperties.printFail(parameters[0] + ": FileNotFoundException "+fnfe.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(fnfe);
                }
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port responseDocument timeOut");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        }
        return status;

    }


    /**
     * stops all/one Diameterservers and all it's threads.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired,
     *                     <br>the optional <code>parameters[1]</code> argument is the DiameterServer port of the DiameterServer to stop, if omitted all running servers are stopped.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterServer#closeSocket(String)
     * @see DiameterServer#closeSockets()
     */
    public int stopDiameterServer(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopDiameterServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopDiameterServer: port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0] + ": Stopping DiameterServer on port "+parameters[1]);
                DiameterServer.closeSocket(parameters[1]);
                return status;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all DiameterServers");
            try
            {
                DiameterServer.closeSockets();
                return status;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
    }
    /**
     * stops the Diameterserver and all it's threads.
     *
     * @see FunctionModule_Diameter#stopDiameterServer(java.lang.String[])
     */
    public int stopSecureDiameterServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSecureDiameterServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopSecureDiameterServer: port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return stopDiameterServer(parameters);
    }

    public int enableExtendedOutput(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": enableExtendedOutput:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status=XTTProperties.FAILED_NO_ARGUMENTS;
            return status;
        }
        XTTProperties.printInfo(parameters[0] + ": Enabling extended output");
        DiameterWorkerServer.setExtendedOutput(true);
        return status;
    }
    public int disableExtendedOutput(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": disableExtendedOutput:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status= XTTProperties.FAILED_NO_ARGUMENTS;
        }
        XTTProperties.printInfo(parameters[0] + ": Disabling extended output");
        DiameterWorkerServer.setExtendedOutput(false);
        return status;
    }

    /**
     * Since the connection does never get closed you can manually close a connection if desired, otherwise it should time out after the timeout time and remove the socket.
    */

    public int closeConnection(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": closeConnection: connection");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": connection");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            DiameterConnection.getConnection(connections,defaultConnection,parameters).closeConnection();
            XTTProperties.printInfo(parameters[0] + ": closed connection.");
        }
        return status;
    }



    /**
     * wait for a specified number of Diameter requests on the DiameterServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterWorkerServer#waitForRequests(int)
     */
    public int waitForRequests(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForRequests: numRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            try
            {
                int messages=Integer.decode(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" Diameter-Requests received on DiameterServer");
                DiameterWorkerServer.waitForRequests(messages);
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
    }
    /**
     * wait for a specified number of Diameter requests on the DiameterServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the port the server is running on.
     *                     <br><code>parameters[2]</code> argument is diameter command code to wait for.
     *                     <br><code>parameters[3]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterWorkerServer#waitForRequests(int)
     */
    public int waitForSpecificRequests(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForSpecificRequests: serverPort commandCode numRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": serverPort commandCode numRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            int port=0;
            try
            {
                port=Integer.decode(parameters[1]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            int commandcode=DiameterWorkerServer.getCommandCode(parameters[2]);
            if(commandcode==0)
            {
                XTTProperties.printFail(parameters[0]+": unrecognized command code '"+parameters[2]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            try
            {
                int messages=Integer.decode(parameters[3]);
                String msgs=""+messages;
                if(messages<=0)msgs="any";
                XTTProperties.printInfo(parameters[0] + ": waiting for "+msgs+" "+DiameterWorkerServer.getCommandFullName(commandcode,CFLAG_REQUEST)+" Requests received on DiameterServer("+port+")");
                status= DiameterServer.waitForRequests(""+port,commandcode,messages);
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[3]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
    }
    /**
     * wait for NO Diameter requests on the DiameterServer during specified time.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the timeout time.
     *                     <br><code>parameters[2]</code> optional argument is the number of previous requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterWorkerServer#waitForTimeoutRequests(int,int)
     */
    public int waitForTimeoutRequests(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRequests: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutRequests: timeoutms maxPreviousRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            try
            {
                int timeoutms=Integer.decode(parameters[1]);
                int maxnumber=-1;
                if(parameters.length==3)
                {
                    try
                    {
                        maxnumber=Integer.decode(parameters[2]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                        status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        return status;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO requests on DiameterServer");
                DiameterWorkerServer.waitForTimeoutRequests(timeoutms,maxnumber);
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
    }

    /**
     * wait for NO Diameter requests on the DiameterServer during specified time.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the port the server is running on.
     *                     <br><code>parameters[2]</code> argument is diameter command code to wait for.
     *                     <br><code>parameters[3]</code> argument is the timeout time.
     *                     <br><code>parameters[4]</code> optional argument is the number of prevous requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterWorkerServer#waitForTimeoutRequests(int,int)
     */
    public int waitForTimeoutSpecificRequests(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSpecificRequests: serverPort commandCode timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutSpecificRequests: serverPort commandCode timeoutms maxPreviousRequests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousRequests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            int port=0;
            try
            {
                port=Integer.decode(parameters[1]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            int commandcode=DiameterWorkerServer.getCommandCode(parameters[2]);
            if(commandcode==0)
            {
                XTTProperties.printFail(parameters[0]+": unrecognized command code '"+parameters[2]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            }
            try
            {
                int timeoutms=Integer.decode(parameters[3]);
                int maxnumber=-1;
                if(parameters.length>=5)
                {
                    try
                    {
                        maxnumber=Integer.decode(parameters[4]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0]+": '"+parameters[4]+"' is NOT a correct number");
                        status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                        XTTProperties.setTestStatus(status);
                        return status;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO "+DiameterWorkerServer.getCommandFullName(commandcode,CFLAG_REQUEST)+" requests on DiameterServer");
                DiameterServer.waitForTimeoutRequests(""+port,commandcode,timeoutms,maxnumber);
                return status;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
    }

    public int lastWorkerIdToVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName");
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName serverPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName serverPort");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int port=0;
            if(parameters.length != 3)
            {
                    port=XTTProperties.getIntProperty("DiameterSERVER/PORT");
            }
            else
            {
                try
                {
                    port=Integer.decode(parameters[2]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": '"+parameters[2]+"' is NOT a number");
                    status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                }
            }
            int numworkers=DiameterServer.getLastWorkerId(port+"");
            XTTProperties.printInfo(parameters[0]+": stored '"+numworkers+"' to "+parameters[1]);
            XTTProperties.setVariable(parameters[1],""+numworkers);
        }
        return status;
    }

    /**
     * Sets a delay which will delay the response by delay*workerid miliseconds, nice if you know parallel connections get opened in order so you can keep the responses in order
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is a delay which will delay the response by (delay*(workerid+1)) miliseconds.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see DiameterWorkerServer#setPerWorkerDelay(int)
     */
    public int setPerWorkerDelay(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setPerWorkerDelay: delayMsTimesWorkerID");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayMsTimesWorkerID");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                DiameterWorkerServer.setPerWorkerDelay(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting per Worker delay to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling per Worker delay");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                return status;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting per Worker delay");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
        return status;
    }

    public int addExtendedStoreVar(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addExtendedStoreVar: avpname");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": avpname");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                DiameterWorkerServer.addExtendedStoreVar(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": adding extended store variable "+parameters[1]);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error adding extended store variable");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                return status;
            }
        }
        return status;
    }

    /**
     * Called for selftest purposes to see if this FunctionModules resources are avaiable.
     *
     */
    public String checkResources()
    {
        int securePort = XTTProperties.getIntProperty("DiameterSERVER/SECUREPORT");
        int standardPort = XTTProperties.getIntProperty("DiameterSERVER/PORT");
        String resourceString = null;

        try
        {
            if(standardPort>0)
            {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(standardPort);
                stndrdPrt.close();
            }
        }
        catch(java.net.BindException be)
        {
            resourceString = "" + this.getClass().getName() + ": Unavailable port '" +  standardPort + "'";
        }
        catch(java.io.IOException ioe){}

        try
        {
            if(securePort>0)
            {
                java.net.ServerSocket scrPrt =  new java.net.ServerSocket(securePort);
                scrPrt.close();
            }
        }
        catch(java.net.BindException be)
        {
            if(resourceString==null)
            {
                resourceString = "" + this.getClass().getName() + ": Unavailable port  '" +  securePort + "'";
            }
            else
            {
                resourceString += ",'" +  securePort + "'";
            }
            
        }
        catch(java.io.IOException ioe){}

        if(resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ": OK";
        }

        return resourceString;
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Diameter.java,v 1.17 2011/02/17 10:52:34 rajesh Exp $";


}

