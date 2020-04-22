package com.mobixell.xtt;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * FunctionModule_MSRP provides MSRP and MSRPS functions.
 * 
 * @author Anil Wadhai
 * @version $Id: FunctionModule_MSRP.java,v 1.1 2009/05/05 11:33:59 awadhai Exp $
 */
public class FunctionModule_MSRP extends FunctionModule
{

    private Map<String, MSRPConnection>           connections             = Collections.synchronizedMap(new HashMap<String, MSRPConnection>());
    private MSRPServer                            s                       = null;
    private Thread                                ws                      = null;
    private MSRPConnection                        conn                    = null;
  
    /**
     * clears and reinitializes all the variables.
     */
    protected void initialize()
    {
        conn = new MSRPConnection();
        conn.readConfiguration();
        MSRPWorker.setServerSendHeader(new LinkedHashMap<String, Vector<String>>());
        MSRPWorker.init();
        MSRPServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * Starts the MSRPServer as an instance of MSRPServer.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the MSRPServer port, <br>
     *            <code>parameters[2]</code> argument is the MSRPServer timeout. <br>
     *            If only <code>parameters[0]</code> is submitted the parameters
     *            will be taken from the configuration xml document in
     *            XTTProperties. <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPServer
     * @see XTTProperties
     */
    public void startMSRPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startMSRPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startMSRPServer: port timeOut");
            return;
        }
        startMSRPServer(parameters, false);
    }
    
    /**
     * Starts the MSRP server as an instance of MSRPServer with SSL-Encryption.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the MSRPServer port,
     *                     <br><code>parameters[2]</code> argument is the MSRPServer timeout.
     *                     <br>If only <code>parameters[0]</code> is submitted the parameters will be taken from the configuration xml document in XTTProperties.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPServer
     * @see XTTProperties
     */
    public void startSecureMSRPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSecureWebServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSecureWebServer: port timeOut");
            return;
        }
        startMSRPServer(parameters,true);
    }

    private void startMSRPServer(String parameters[], boolean isSecure)
    {
        String secure = "";
        String secures = "";
        if(isSecure)
            secure = "SECURE";
        if(isSecure)
            secures = "secure ";
        if(parameters.length!=3)
        {
            XTTProperties.printWarn(parameters[0]+": Using default settings");
            try
            {
                XTTProperties.printVerbose(parameters[0]+": Starting startMSRPServer");
                s  = new MSRPServer(XTTProperties.getIntProperty("MSRPSERVER/"+secure+"PORT"), isSecure, XTTProperties.getIntProperty("MSRPSERVER/TIMEOUT"));
                ws = (new Thread(s, "MSRPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0]+": Started "+secures+"MSRPServer");
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            try
            {
                XTTProperties.printVerbose(parameters[0]+": Starting "+secures+"MSRPServer");
                s = new MSRPServer(Integer.parseInt(parameters[1]), isSecure, Integer.parseInt(parameters[2]));
                ws = (new Thread(s, "MSRPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0]+": Started "+secures+"MSRPServer");
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Creates a new connection.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument the name for the connection, <br>
     *            <code>parameters[2]</code> argument is the is the MSRPServer
     *            port which handles the responses, <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see XTTProperties
     */
    public void createConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createConnection: name serverPort");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name serverPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name       = parameters[1];
            String serverPort = parameters[2];
            try
            {
                if(MSRPServer.checkSocket(serverPort))
                {
                    XTTProperties.printFail(parameters[0]+": No running server found on port "+serverPort);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                MSRPConnection connection = conn.createConnection(name, serverPort);
                connections.put(name.toLowerCase(), connection);
                XTTProperties.printInfo(parameters[0]+": created connection '"+name+"'");
            } catch (Exception uhe)
            {
                XTTProperties.printFail(parameters[0]+":: Unable to create connection '"+name+"': "+uhe.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
            }
        }
    }

    /**
     * Creates a new connection and assigns Worker's Socket to the connection instance class so that the same socket is used to send messages.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument the name for the connection, <br>
     *            <code>parameters[2]</code> argument is the is the MSRPServer
     *            port which handles the responses, <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see XTTProperties
     */
    public void getConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getConnection: name serverPort");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name serverPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name       = parameters[1];
            String serverPort = parameters[2];
            try
            {
                if(MSRPServer.checkSocket(serverPort))
                {
                    XTTProperties.printFail(parameters[0]+": No running server found on port "+serverPort);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                MSRPConnection connection = conn.createConnection(name, serverPort);
                connections.put(name.toLowerCase(), connection);
                XTTProperties.printInfo(parameters[0]+": using connection '"+name+"'");
                connection.setSocket(MSRPServer.getSocket(""+serverPort),true);
               
            } catch (Exception uhe)
            {
                XTTProperties.printFail(parameters[0]+":: Unable to get connection '"+name+"': "+uhe.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
            }
        }
    }

    /**
     * Send a free MSRP request.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name,
     *            no additional parameters are requeired. <br>
     *            <code>parameters[1]</code> connection name, <br>
     *            <code>parameters[2]</code> msrp method to use in UPPERCASE (SEND or REPORT), <br>
     *            <code>parameters[3]</code> remoteHost, <br>
     *            <code>parameters[4]</code> remotePort, <br>
     *            <code>parameters[5]</code> headers, <br>
     *            <code>parameters[6]</code> base64Body encoded body to send, <br>
     *            <code>parameters[7]</code> transactionId (or not present for autogenerate),
     *            transactionId is generated automatically <br>
     *            <code>parameters[8]</code> boundary indicator (or not present for autogenerate),
     *            boundary is generated automatically <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * 
     * @see MSRPConnection#sendRequest
     */
    public void sendFreeRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: name msrpMethod remoteHost remotePort headers base64Body");
            XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: name msrpMethod remoteHost remotePort headers base64Body transactionId");
            XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: name msrpMethod remoteHost remotePort headers base64Body transactionId boundary");
            return;
        }
        if(parameters.length<7||parameters.length>9)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name msrpMethod remoteHost remotePort headers base64Body");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name msrpMethod remoteHost remotePort headers base64Body transactionId");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name msrpMethod remoteHost remotePort headers base64Body transactionId boundary");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
           
            String funcname     = parameters[0];
            String method       = parameters[2];
            String remoteHost   = parameters[3];
            String transactionId= null;
            String boundary     = null;
            int    remotePort   = 8493;
            try
            {
                remotePort=Integer.decode(parameters[4]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[4]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }
            
            String headers  = parameters[5];
            byte[] body     = ConvertLib.base64Decode(parameters[6]);
            
            if(parameters.length == 8)
            {
                transactionId = parameters[7]; 
            }
            
            if(parameters.length == 9)
            {
                transactionId = parameters[7]; 
                if(!(parameters[8].equals("$")||parameters[8].equals("+")||parameters[8].equals("#")))
                {
                   XTTProperties.printFail(parameters[0]+": '"+parameters[8]+"' is NOT a '+' or '$' or '#'");  
                   return;
                }
                boundary = "-------"+transactionId+parameters[8];
            }
       
            try
            {
                connection.sendRequest(funcname,remotePort,remoteHost, method,headers, body,transactionId,boundary);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    
    
    /**
     * Send a MSRP request.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no additional parameters are requeired.
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> msrp method name to use in UPPERCASE(SEND or REPORT),
     *                     <br><code>parameters[3]</code> base64Body encoded body to send, 
     *                     <br><code>parameters[4]</code> transactionId (or not present for autogenerate),
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPConnection#sendMSRPRequest
     */
    public void sendMSRPRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMSRPRequest: name msrpMethod base64Body");
            XTTProperties.printFail(this.getClass().getName()+": sendMSRPRequest: name msrpMethod base64Body transactionId");
            return;
        }
        if(parameters.length<4||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name msrpMethod base64Body");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name msrpMethod base64Body transactionId");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            String funcname           = parameters[0];
            String method             = parameters[2];
            String transactionId      = null;
            byte[] body               = new byte[0];
            if(parameters[3]!=null && !parameters[3].equals(""))
            {    
                body = ConvertLib.base64Decode(parameters[3]);
            }
            if(parameters.length==5)
            {
                transactionId = parameters[4];
            }
            try
            {
                connection.sendMSRPRequest(funcname, method, body,transactionId);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * wait for a specified number of MSRP responses on the MSRPServer.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the number of responses.<br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#waitForResponses(int)
     */
    public boolean waitForResponses(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForResponses: numResponses");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": numResponses");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages = Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0]+": waiting for "+messages+" MSRP-Responses received on MSRPServer");
                MSRPWorker.waitForResponses(messages);
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }
    
    /**
     * wait for a specified number of MSRP requests on the MSRPServer.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the number of requests.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#waitForRequests(int)
     */
    public boolean waitForRequests(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForRequests: numRequests");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numRequests");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            { 
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" MSRP-Requests received on MSRPServer");
                MSRPWorker.waitForRequests(messages);
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    /**
     * Since the connection does never get closed you can manually close a
     * connection if desired, otherwise it should time out after the timeout
     * time and remove the socket.
     * 
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *  to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    
    public void closeConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection: connection");
            return;
        }
        if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connection");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            connection.closeConnection();
            XTTProperties.printInfo(parameters[0]+": closed connection.");
        }
    }

    /**
     * stops all/one MSRPServers and all it's threads.
     * 
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name,
     *            no additional parameters are requeired, <br>
     *            the optional <code>parameters[1]</code> argument is the
     *            MSRPServer port of the MSRPServer to stop, if omitted all
     *            running servers are stopped. <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPServer#closeSocket(String)
     * @see MSRPServer#closeSockets()
     */
    public void stopMSRPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopMSRPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopMSRPServer: port");
            return;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printInfo(parameters[0]+": Stopping MSRPServer on port "+parameters[1]);
                MSRPServer.closeSocket(parameters[1]);
                return;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0]+": Stopping all MSRPServers");
            try
            {
                MSRPServer.closeSockets();
                return;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return;

    }
    
   /**
     * Returns the Configuration Options as a String ready to copy/paste in a
     * configuration file
     * 
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module MSRP -->"
        +"\n    <MSRPServer>"
        +"\n        <!-- the tcp listening port of the internal MSRPServer -->"
        +"\n        <Port>493</Port>"
        +"\n        <!-- the tcp listening port for responses/requests to the client -->"
        +"\n        <clientPort>849</clientPort>"
        +"\n        <!-- the listening port of the internal secure-webserver -->"
        +"\n        <SecurePort>8493</SecurePort>"
        +"\n        <!-- the root directory for the responses, default is the test's directory -->"
        +"\n        <ClientSecurePort>8494</ClientSecurePort>"
        +"\n        <!-- timeout on client connections to the MSRPServer -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
        +"\n        <waitTimeout>30000</waitTimeout>"
        +"\n        <!--"
        +"\n        <enableCertCheck/>"
        +"\n        -->"
        +"\n    </MSRPServer>"
        +"\n    <!-- function module MSRP -->"
        +"\n    <MSRP>"
        +MSRPConnection.getConfigurationOptions()
        +"\n</MSRP>";
    }

    /**                                                                          
     * Called for selftest purposes to see if this FunctionModules resources are avaiable.
     */
    public String checkResources()
    {
        int clientPort        = XTTProperties.getIntProperty("MSRPSERVER/CLIENTPORT");
        int port              = XTTProperties.getIntProperty("MSRPSERVER/PORT");
        int securePort        = XTTProperties.getIntProperty("MSRPSERVER/SECUREPORT");
        int clientSecurePort  = XTTProperties.getIntProperty("MSRPSERVER/CLIENTSECUREPORT");
        String resourceString = null;
        try
        {
            if (port > 0)
            {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(port);
                stndrdPrt.close();
            }
        } catch (java.net.BindException be)
        {
            resourceString = "" + this.getClass().getName() + ": Unavailable port '" + port + "'";
        } catch (java.io.IOException ioe)
        {
        }

        try
        {
            if (clientPort > 0)
            {
                java.net.ServerSocket clientPrt = new java.net.ServerSocket(clientPort);
                clientPrt.close();
            }
        } catch (java.net.BindException be)
        {
            if (resourceString == null)
            {
                resourceString = "" + this.getClass().getName() + ": Unavailable port  '" + clientPort + "'";
            } else
            {
                resourceString += ",'" + clientPort + "'";
            }
        } catch (java.io.IOException ioe)
        {
        }

        try
        {
            if (securePort > 0)
            {
                java.net.ServerSocket scrPrt = new java.net.ServerSocket(securePort);
                scrPrt.close();
            }
        } catch (java.net.BindException be)
        {
            if (resourceString == null)
            {
                resourceString = "" + this.getClass().getName() + ": Unavailable port  '" + securePort + "'";
            } else
            {
                resourceString += ",'" + securePort + "'";
            }
        } catch (java.io.IOException ioe)
        {
        }

        try
        {
            if (clientSecurePort > 0)
            {
                java.net.ServerSocket clientScrPrt = new java.net.ServerSocket(clientSecurePort);
                clientScrPrt.close();
            }
        } catch (java.net.BindException be)
        {
            if (resourceString == null)
            {
                resourceString = "" + this.getClass().getName() + ": Unavailable port  '" + clientSecurePort + "'";
            } else
            {
                resourceString += ",'" + clientSecurePort + "'";
            }
        } catch (java.io.IOException ioe)
        {
        }

        if (resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ": OK";
        }

        return resourceString;
    }

    /**
     * set the MSRP headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the header value or not present removing.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: name headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": setHeader: name headerFieldKey headerFieldValue");
            return;
        }
        if (parameters.length < 3 || parameters.length > 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name headerFieldKey");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {   
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            
            MSRPConnection.setHeader(connection,parameters);
        }
    }

    /**
     * set the MSRP headers to be sent from the server to the client.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the header key,
     *                     <br><code>parameters[2]</code> argument is the header value or not present removing.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     * @see MSRPWorker#getServerSendHeader
     */
    public void setServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": setServerHeader: headerFieldKey headerFieldValue");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            LinkedHashMap<String,Vector<String>> headers=MSRPWorker.getServerSendHeader();
            if(parameters.length==3&&!parameters[2].equals("null"))
            {
                String newVal=parameters[2];
                // Actually set the Header Key and Value
                Vector<String> values=headers.get(parameters[1].trim());
                if(values==null)values=new Vector<String>();
                values.add(newVal);
                headers.put(parameters[1].trim(),values);
                XTTProperties.printInfo(parameters[0]+": setting HeaderField "+parameters[1]+" to: "+values);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": removing HeaderField "+parameters[1]);
                // Actually remove the Header Key and Value
                headers.remove(parameters[2].trim());
            }
        }
    }    

    /**
     * remove all the headers that are to be sent from the client to the server.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> connection name,
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearHeader(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": clearHeader: name");
            return;
        }
        if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if (connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }
            connection.getRequestHeader().clear();
            XTTProperties.printInfo(parameters[0] + ": clearing header");
        }
    }

    /**
     * remove all the headers that are to be sent from the server to the client.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setServerSendHeader
     */
    public void clearServerHeader(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": clearServerHeader:" + NO_ARGUMENTS);
            return;
        }
        if (parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            MSRPWorker.setServerSendHeader(new LinkedHashMap<String, Vector<String>>());
        }
    }

    /**
     * disable the automatic sending of server side responses.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setServerSendHeader
     */
    public void disableResponse(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": disableResponse:"+NO_ARGUMENTS);
            return;
        }
        if (parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            return;
        } else
        {
            MSRPWorker.setSkipResponse(true);
            XTTProperties.printInfo(parameters[0] + ": Disabling sending of Response to request");
            
        }
    }

    /**
     * enable the automatic sending of server side responses.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setServerSendHeader
     */
    public void enableResponse(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": enableResponse:"+NO_ARGUMENTS);
            return;
        }
        if (parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            return;
       } else
        {
            MSRPWorker.setSkipResponse(false);
            XTTProperties.printInfo(parameters[0] + ": Enabling sending of Response to request");
        }
      
    }

    /**
     * wait for timeout on specified number of MSRP responses on the MSRPServer.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the time in ms.
     *                   <br><code>parameters[2]</code> argument is the number of responses.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#waitForTimeoutResponses(int,int)
     */
    public boolean waitForTimeoutResponses(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": waitForTimeoutResponses: timeoutms");
            XTTProperties.printFail(this.getClass().getName() + ": waitForTimeoutResponses: timeoutms maxPreviousResponses");
            return false;
        }
        if (parameters.length < 2 || parameters.length > 3)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": timeoutms maxPreviousResponses");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeoutms = Integer.parseInt(parameters[1]);
                int maxnumber = -1;
                if (parameters.length == 3)
                {
                    try
                    {
                        maxnumber = Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0] + ": '" + parameters[2] + "' is NOT a correct number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return false;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for " + timeoutms + "ms and NO responses on MSRPServer");
                MSRPWorker.waitForTimeoutResponses(timeoutms, maxnumber);
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    /**
     * Overrides the normal MSRP Return code with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is an integer representing the MSRP code (0 or less means no override).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setOverrideReturnCode(int)
     */
    public void setServerReturnCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnCode: msrpReturnCode");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": msrpReturnCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                if(Integer.parseInt(parameters[1])>0)
                {
                    MSRPWorker.setOverrideReturnCode(Integer.parseInt(parameters[1]));
                    XTTProperties.printInfo(parameters[0] + ": setting server return code to "+parameters[1]);
                } else
                {
                    MSRPWorker.setOverrideReturnCode(0);
                    XTTProperties.printInfo(parameters[0] + ": disabling server return code");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting server return code");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Overrides the normal MSRP Return Message with a custom code.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the Message after the response code 
     *                     <br>including the whitespace between message and response code (for "MSRP 200 Ok" it would be " Ok").
     *                     <br> if <code>parameters[1]</code> is not present response message would be set to null
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setOverrideReturnCode(int)
     */
    public void setServerReturnMessage(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnMessage: msrpReturnMessage");
            return;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(this.getClass().getName()+": setServerReturnMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": msrpReturnMessage");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
                if(parameters.length==1)
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling server return message");
                    MSRPWorker.setOverrideReturnMessage(null);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": setting server return message to '"+parameters[1]+"'");
                    MSRPWorker.setOverrideReturnMessage(parameters[1]);
                }
        }
    }
    
    /**
     * disable certificate checking on msrps connections.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setServerSendHeader
     */
    public void disableCertCheck(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": disableCertCheck: name");
            return;
        }
        if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            connection.setEnableCertcheck(false);
            XTTProperties.printInfo(parameters[0] + ": Disabling default Java Certificate Trust Manager");
        }
    }
    
    /**
     * enable certificate checking on msrps connections.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name ,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MSRPWorker#setServerSendHeader
     */
    public void enableCertCheck(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": enableCertCheck: name");
            return;
        }
        if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            MSRPConnection connection = MSRPConnection.getConnection(connections,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            connection.setEnableCertcheck(true);
            XTTProperties.printInfo(parameters[0] + ": Enabling default Java Certificate Trust Manager");
        }
    }
    
    /* *//**
     * compare the msrp headers received by the server from the client with a value which is required.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the header value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *//*
    public void checkServerHeader(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader: headerFieldKey expectedValue");
            return;
        }
        if (parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);  
        } else
        {
            parameters = modifyArray(parameters);           
            HTTPHelper.checkHeader(MSRPWorker.getServerHeader(), parameters, false);
        }
    }*/
    
    
    
  /*  *//**
     * compare the msrp headers received by the client from the server with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the connection name,
     *                      <br><code>parameters[2]</code> argument is the header key,
     *                      <br><code>parameters[3]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *//*
    public void checkHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeader:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: connection headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: connection headerFieldKey expectedValue");
            return;
        }
        if (parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);  
        } else
        {
            //TODO: presently connection not been used, It may use in future scope
            MSRPConnection connection = MSRPConnection.getConnection(connections, parameters);
            if (connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }
            parameters = modifyArray(parameters);
            HTTPHelper.checkHeader(MSRPWorker.getHeader(),parameters,false);
        }
    }*/
    

 /*   *//**
     * query the MSRP headers received by the server from the client with a regular expression.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *//*
    public void queryServerHeader(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeader:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeader: variable headerFieldKey regularExpression");
            return;
        }
        if (parameters.length < 4)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": variable headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            parameters = modifyArray(parameters);           
            HTTPHelper.queryHeader(MSRPWorker.getServerHeader(), parameters, false);
        }
    }*/

 /*   *//**
     * query the MSRP headers received by the server from the client with a regular expression.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                   <br><code>parameters[2]</code> argument is the header key,
     *                   <br><code>parameters[3]</code> argument is the regular expression.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *//*
    public void queryServerHeaderNegative(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeaderNegative:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": queryServerHeaderNegative: headerFieldKey regularExpression");
            return;
        }
        if (parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            parameters = modifyArray(parameters);
            HTTPHelper.queryHeader(MSRPWorker.getServerHeader(), parameters, true);
        }
    }*/

   /* *//**
     * compare the MSRP headers received by the server from the client with a value which it is prohibited.
     *
     * @param parameters array of String containing the parameters.
     *                   <br><code>parameters[0]</code> argument is always the method name,
     *                   <br><code>parameters[1]</code> argument is the header key,
     *                   <br><code>parameters[2]</code> argument is the header value.
     *                   <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                   to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *//*
    public void checkServerHeaderNot(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeaderNot:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeaderNot: headerFieldKey");
            XTTProperties.printFail(this.getClass().getName() + ": checkServerHeaderNot: headerFieldKey expectedValue");
            return;
        }
        if (parameters.length<2 ||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            parameters = modifyArray(parameters);
            HTTPHelper.checkHeader(MSRPWorker.getServerHeader(), parameters, true);
            
        }
    }*/
    
  /*  *//**
     * compare the msrp response code of the last request with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the connection name,
     *                     <br><code>parameters[2]</code> are the allowed response codes.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     *//*
    public void checkResponseCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode:"+NO_ARGUMENTS);   
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode: connection expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": connection expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            //TODO: presently connection not been used, It may use in future scope
            MSRPConnection connection = MSRPConnection.getConnection(connections,parameters);
            if(connection == null)
            {
                XTTProperties.printFail(parameters[0] + ": connection '" + parameters[1] + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;   
            }
            
            boolean found=false;
            StringBuffer checked=new StringBuffer();
            String divider="";
            for(int i=2;i<parameters.length;i++)
            {
                try
                {
                    if(MSRPWorker.getResponseCode()==Integer.decode(parameters[i]))
                    {
                        found=true;
                    }
                    checked.append(divider+parameters[i]);
                    divider=",";
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[i]+"' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
            }
            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": found "+MSRPWorker.getResponseCode()+" "+MSRPWorker.getResponseMessage(MSRPWorker.getResponseCode()));
            } else
            {
                XTTProperties.printFail(parameters[0] + ": found "+MSRPWorker.getResponseCode()+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }*/
    
   /* // method to modify the array size and parameter order
    private String[] modifyArray(String[] parameters)
    {
        String[] newParameters = null;
        if(parameters.length==4)
        {
            newParameters    = new String[3]; 
            newParameters[0] = parameters[0];
            newParameters[1] = parameters[2];
            newParameters[2] = parameters[3];
        } else
        {
            newParameters    = new String[2];  
            newParameters[0] = parameters[0];
            newParameters[1] = parameters[2];
        }
        return newParameters;
    }*/
    
}
