package com.mobixell.xtt.diameter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import com.mobixell.xtt.XTTXML;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
/**
 * <p>DiameterConnection</p>
 * <p>send a single Diameter request which has been received by the DiameterServer</p>
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * Stores headers and AVP's as<code><br>
 *  Response:<br>
 *  DIAMETER/[connectionName]/HEADER/[headername]
 *  DIAMETER/[connectionName]/AVP/[avpname]
 *  DIAMETER/[connectionName]/AVP/[avpname]/vendorID
 *  DIAMETER/[connectionName]/AVP/[avpname]/avpcode
 *  DIAMETER/[connectionName]/AVP/[avpname]/avpSflags
 *  DIAMETER/[connectionName]/AVP/[avpname]/length   (in case of grouped avps the number of avps in this avp)
 *  and (if variableName present in request):<br>
 *  [vaiableName]/HEADER/[headername]
 *  [vaiableName]/AVP/[avpname]
 *  [vaiableName]/AVP/[avpname]/vendorID
 *  [vaiableName]/AVP/[avpname]/avpcode
 *  [vaiableName]/AVP/[avpname]/avpSflags
 *  [vaiableName]/AVP/[avpname]/length   (in case of grouped avps the number of avps in this avp)
 *
 * @author      Anil Wadhai
 * @version     $Id: DiameterConnection.java,v 1.4 2009/10/12 13:04:48 rsoder Exp $
 */

public class DiameterConnection implements DiameterConstants
{

    private String                            name               = null;
    private String                            module             = "DIAMETER";
    private int                               port               = 0;
    private String                            ipAddress          = "127.0.0.1";
    private Socket                            socket             = null;
    private BufferedInputStream               socketin           = null;
    private byte[]                            hexByteReqDocument = null;
    private int                               timeout            = 30000;
    private boolean                           isFirstTime        = false;
    public String getName()
    {
        return name;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public DiameterConnection(String name, String module)
    {
        this.name = name;
        this.module = module.toUpperCase();
    }

    public DiameterConnection()
    {
    }

    public void setPorts(String sport)
    {
        this.port = Integer.parseInt(sport);
    }

    /**
     * Reads the configuration from the properties. Values are stored under
     * [MODULE]/[VALUE]
     */
    public void readConfiguration()
    {
        int connectionTimeout = XTTProperties.getIntProperty("DIAMETERSERVER/TIMEOUT");
        if(connectionTimeout<=0)connectionTimeout = 30000;
        this.setTimeout(connectionTimeout);
    }

    /**
     * Create a new connection with the given connection name and set the
     * parameters<br>
     * like port, timeout, created connection and return the same.
     *
     * @param newname
     * @param port
     * @return newConnection
     */
    public DiameterConnection createConnection(String newname, String ipAddress, String port)
    {
        XTTProperties.printInfo("DiameterConnection : createConnection with name '"+newname+"'");
        DiameterConnection newConnection = new DiameterConnection(newname, module);
        newConnection.setIpAddress(ipAddress);
        newConnection.setPorts(port);
        newConnection.setTimeout(timeout);
        return newConnection;
    }

    public static DiameterConnection getConnection(Map<String, DiameterConnection> connections, DiameterConnection defaultConnection, String parameters[])
    {
        XTTProperties.printInfo("DiameterConnection : getConnection '"+parameters[1]+"'");
        if(parameters.length==1) return defaultConnection;
        DiameterConnection connection = connections.get(parameters[1].toLowerCase());
        if(connection==null)
        {
            return defaultConnection;
        } else
        {
            parameters[0] = parameters[0]+"("+connection.getName()+")";
            return connection;
        }
    }

    /**
     * just in case the connection has to be closed client side manually.
     */
     public void closeConnection()
     {
         XTTProperties.printInfo("DiameterConnection("+this.name+"): closeConnection");
         try{socket.close();} catch (Exception e){}
         socket=null;
         socketin=null;
     }

    public int sendDiameterRequest(String function, String reqDocument, String connectionName, String variableName, boolean request, boolean doRead)
    {
    	int status = XTTProperties.PASSED;
        try
        {
            hexByteReqDocument                 = new byte[0];
            Element                 current    = null;
            Document                reqDoc     = null;

            reqDoc = XTTXML.readXMLFromString(reqDocument);

           //if reqDoc has more then one request without root node. readXMLFromString will throw parsing exeception reqDoc will get null
            if(reqDoc==null)
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }

            current = reqDoc.getRootElement();
            // if root name is xml then check for number of child & id number of child is more then one that is invalid request.
            if(current.getName().toLowerCase().equals("xml"))
            {
                List<?> numberOfRequest = current.getChildren();
                if(numberOfRequest.size()>1)
                {
                    XTTProperties.printFail(function+"("+connectionName+"): XML document has more then one request");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return XTTProperties.FAILED;
                } else
                {
                    current = (Element)numberOfRequest.get(0);
                }

            }

            Diameter_PDUBody request_body = new Diameter_PDUBody("DiameterConnection("+connectionName+"): REQUEST");
            Diameter_PDUHeader pdu_header = new Diameter_PDUHeader("DiameterConnection("+connectionName+")");

            String overrideCommandCode = getAttribute(current, "overridecommandcode");

            if(overrideCommandCode == null || overrideCommandCode.equals(""))
            {
                pdu_header.setCommandcode(DiameterWorkerServer.getCommandCode(current.getName()));
            } else
            {
                pdu_header.setCommandcode(Integer.decode(getAttribute(current, "overridecommandcode")));
                pdu_header.setCommandcode(Integer.decode(getAttribute(current, "overridecommandcode")));
            }

            pdu_header.setVersion(Integer.decode(getAttribute(current, "version")));
            pdu_header.setApplicationID(Integer.decode(getAttribute(current, "applicationid")));
            pdu_header.setHopbyhopID(Integer.decode(getAttribute(current, "hopbyhopid")));
            pdu_header.setEndtoendID(Integer.decode(getAttribute(current, "endtoendid")));


            pdu_header.setProxiable(ConvertLib.textToBoolean(getAttribute(current, "proxiable")));
            pdu_header.setError(ConvertLib.textToBoolean(getAttribute(current, "error")));
            pdu_header.setTretr(ConvertLib.textToBoolean(getAttribute(current, "tretr")));

            //default true since its request, set to not true for responses
            pdu_header.setRequest(request);
            String typeRequestOrResponse=getAttribute(current, "request");
            if(typeRequestOrResponse!=null&&!typeRequestOrResponse.equals("null"))
            {
                pdu_header.setRequest(ConvertLib.textToBoolean(typeRequestOrResponse));
            }

            // Assemble the request PDUs with the data of the found request XML document as root
            request_body.setAVPs(createAVPs(function,current));

            byte[] body = request_body.createPDUBody();

            pdu_header.setMessagelength(pdu_header.HEADLENGTH+body.length);

            // Create the binary header
            byte[] header = pdu_header.createPDUHeader();

            hexByteReqDocument = ConvertLib.concatByteArray(header, body);

            String cmdFullName = DiameterWorkerServer.getCommandFullName(pdu_header.getCommandcode(), pdu_header.getCommandflags() );

            XTTProperties.printInfo(function+"("+connectionName+"): sendDiameterRequest to '"+this.ipAddress+":"+this.port+"'"+" REQUEST TYPE '"+cmdFullName+"'");

            status=sendTCPRequest(function, hexByteReqDocument);

            if(doRead)
            {
                readDiameterStream(function,connectionName,variableName);
            }

        } catch (Exception e)
        {
            XTTProperties.printFail(function+": "+e.getClass().getName()+": "+e.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return XTTProperties.FAILED;
        }
        return status;
    }

    /*
     * Create the child AVPs from a Request XML root node
     */
    @SuppressWarnings("unchecked")
    private Vector<Diameter_AVP> createAVPs(String function, Element root) throws Exception
    {
        if(!isFirstTime)
        {
            XTTProperties.printInfo(function+"("+this.name+"): createAVPs");
            isFirstTime = true;
        }

        Vector<Diameter_AVP> returnValues = new Vector<Diameter_AVP>();
        if(root==null) return returnValues;

        // get the iterator over all AVP children of the root
        Iterator<Element> it = root.getChildren().iterator();
        // the current XML AVP
        Element currentE = null;
        // The current created AVP
        Diameter_AVP currentAVP = null;
        // A vector for storing the children
        Vector<Diameter_AVP> currentAVPs = new Vector<Diameter_AVP>();
        String temp = null;
        int avpcode = 0;
        int ioverrideavpcode=0;
        // For each defined child for this ROOT AVP
        while (it.hasNext())
        {
            currentE = it.next();
            String overrideavpcode = getAttribute(currentE, "overrideavpcode");
            avpcode  = DiameterWorkerServer.getAVPCode(currentE.getName());

            if(overrideavpcode == null || overrideavpcode.equals(""))
            {
                ioverrideavpcode=0;
            } else
            {
                ioverrideavpcode = Integer.decode(overrideavpcode);
            }


            if(avpcode<=0&&ioverrideavpcode<=0)
            {
                XTTProperties.printFail(function+"("+this.name+"): AVP not found "+currentE.getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception("AVP not found "+currentE.getName());
            }

            currentAVPs = new Vector<Diameter_AVP>();
            currentAVPs.add(new Diameter_AVP());
            // }

            Iterator<Diameter_AVP> cit = currentAVPs.iterator();
            // For each AVP we have, either a single one or a 1 to n from the request
            while (cit.hasNext())
            {
                currentAVP                  = cit.next();
                currentAVP.avpname          = currentE.getName();
                currentAVP.avpcode          = avpcode;
                currentAVP.overrideavpcode  = ioverrideavpcode;

                temp = getAttribute(currentE, "mandatory");
                if(temp!=null&&!temp.equals(""))
                    currentAVP.mandatory = ConvertLib.textToBoolean(temp);
                    temp = getAttribute(currentE, "protected");
                if(temp!=null&&!temp.equals(""))
                    currentAVP.protectedf = ConvertLib.textToBoolean(temp);
                    temp = getAttribute(currentE, "vendorspecific");
                if(temp!=null&&!temp.equals(""))
                    currentAVP.vendorspec = ConvertLib.textToBoolean(temp);
                    temp = getAttribute(currentE, "vendorid");
                if(currentAVP.vendorspec&&temp!=null&&!temp.equals(""))
                {
                    currentAVP.vendorID = Integer.decode(temp);
                }

                // if currrent AVP element is not grouped
                if(currentE.getChildren().size()==0)
                {
                    temp = getAttribute(currentE, "value");

                    if(temp==null)
                    {
                        //this indicates "value" attribute is not present for this avp, so check if "variable" is defined and read values from the defined variable
                        String variableName = getAttribute(currentE, "variable");
                        if(variableName!=null)temp = XTTProperties.getVariable(variableName);
                    }

                    if(temp==null)
                    {
                        //this indicates "value" attribute or "variable" attribute is not present for this avp, so check if "configuration" is defined and read values from the defined configuration
                        String configName = getAttribute(currentE, "configuration");
                        if(configName!=null)temp = XTTProperties.getProperty(configName);
                    }

                    if(temp==null)
                    {
                        XTTProperties.printFail(function+"("+this.name+"): Neither 'value' nor 'confiuration' nor 'variable' attribute is defined for AVP '"+currentE.getName()+"'");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        break;
                    }
                }

                if(temp!=null&&!temp.equals(""))currentAVP.data = temp;

                // Set the child AVPs for this AVP
                currentAVP.setGroupedAVPs(createAVPs(function,currentE));
                // Add the current AVP to the response list
                returnValues.add(currentAVP);
            }
        }

        return returnValues;
    }

    @SuppressWarnings("unchecked")
    private String getAttribute(Element element, String name)
    {
        Iterator<Attribute> it = element.getAttributes().iterator();
        Attribute attribute    = null;
        while (it.hasNext())
        {
            attribute = it.next();
            if(attribute.getName().toLowerCase().equals(name))
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    /**
     * Diameter Client
     * send request to server.
     *
     * @param function
     * @param hexByteReqDocument
     * @throws IOException
     */
    private int sendTCPRequest(String function, byte[] hexByteReqDocument) throws IOException
    {
        XTTProperties.printInfo(function+"("+this.name+"): sendTCPRequest");

        if(socket==null||socket.isClosed()||!socket.isConnected())
        {
            socket  = new Socket(this.ipAddress, this.port);
            socketin= new BufferedInputStream(socket.getInputStream(), 65536);
            XTTProperties.printVerbose(function+": opened "+socket);
        }
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(timeout);

        synchronized (socket)
        {
        	try 
        	{
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 65536);
            out.write(hexByteReqDocument);
            out.flush();
            XTTProperties.printDebug(function + ": written Header/Body:\n"+ConvertLib.getHexView(hexByteReqDocument));
        	}
        	catch (Exception e)
        	{
        		 XTTProperties.printException(e);
        		return XTTProperties.FAILED_UNKNOWN;
        	}
        }
        return  XTTProperties.PASSED;
    }

    /**
     * handle the response received by client.
     */
    private void readDiameterStream(String function, String connectionName, String variableName) throws Exception
    {
        XTTProperties.printInfo(function+"("+connectionName+"): reading from stream");
        boolean extendedOutput = false;
        Diameter_PDUHeader  pdu_header = new Diameter_PDUHeader("DiameterConnection("+this.name+")");
        Diameter_PDUBody    pdu_body   = new Diameter_PDUBody("DiameterConnection("+this.name+")");

        String[] storeVar     = new String[] {"DIAMETER/"+connectionName};
        if(variableName != null)
        {
            storeVar     = new String[] {"DIAMETER/"+connectionName, variableName};
        }

        synchronized(socketin)
        {
            if(!pdu_header.readPDUHeader(socketin))
            {
                XTTProperties.printVerbose(function+"("+connectionName+"): pdu empty, possible disconnect");
                return;
            }
            pdu_body.readPDUBody(socketin, pdu_header, extendedOutput, storeVar);
        }

    }

    /**
     * read a request/response received by client without sending data first. Can be used to wait for a request that is expected to arrive.
     */
    public void readRequest(String function, String connectionName, String variableName) throws Exception
    {
        readDiameterStream(function,connectionName,variableName);
    }

    public static final String tantau_sccsid = "@(#)$Id: DiameterConnection.java,v 1.4 2009/10/12 13:04:48 rsoder Exp $";

}
