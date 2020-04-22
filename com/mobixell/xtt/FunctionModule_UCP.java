package com.mobixell.xtt;

import java.net.Socket;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * FunctionModule_UCP.
 * <p>
 * Functions for sending UCP packets.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.14 $
 */
public class FunctionModule_UCP extends FunctionModule implements UCPConstants
{
    private Map<String,UCPConnection> connections = Collections.synchronizedMap(new HashMap<String,UCPConnection>());
    private int readtimeout=0;

    public FunctionModule_UCP()
    {


    }

    private class UCPConnection
    {
        private String name = null;
        private Socket socket = null;
        private byte[] response = new byte[0];
        private int autoTRN=0;

        public UCPConnection(String name,Socket socket)
        {
            this.name=name;
            this.socket=socket;
        }
        public int getAutoTRN()
        {
            int trn=autoTRN++;
            autoTRN=autoTRN%100;
            return trn;
        }
        public byte[] getResponse()
        {
            return response;
        }
        public void setResponse(byte[] response)
        {
            this.response=response;
        }
        public Socket getSocket()
        {
            return socket;
        }
        public String getName()
        {
            return name;
        }
        public void clearResponse()
        {
            this.response = new byte[0];
        }

    }

    /**
     * Create and open a connection to a specified IPAddress and port.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument ip address to connect to
     *                     ,<br> <code>parameters[3]</code> argument is port to connect to)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void openConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": openConnection: name ip port");
            return;
        } else if (parameters.length !=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name ip port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            int port;
            String IP;

            String name = parameters[1];
            IP = parameters[2];

            try
            {
                port = Integer.parseInt(parameters[3]);
            } catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": can't set port, '"+parameters[3]+"' isn't a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }
            try
            {
                Socket socket = new Socket(IP,port);
                XTTProperties.printDebug("Client connected: "+socket.getRemoteSocketAddress()+"\n"+socket.getLocalSocketAddress()+"\n");
                UCPConnection c=new UCPConnection(name,socket);
                connections.put(name,c);
                XTTProperties.printInfo(parameters[0]+": Connection '"+name+"' opened to " + IP + ":" + port);
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting connection '"+name+"' to "+IP+":"+port);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
    }
    
    /**
     * Method to establish connection
     * @param parameters
     */
    public void getConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getConnection: name OAdC");
            return;
        } else if (parameters.length !=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name OAdC");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name = parameters[1];
            String OAdC = parameters[2];

            try
            {
                Socket socket = UCPWorker.getSocket(OAdC);
                if(socket==null)
                {
                    XTTProperties.printFail(parameters[0]+": error getting connection '"+name+"' for '"+OAdC+"'");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                XTTProperties.printDebug("Client connected: "+socket.getRemoteSocketAddress()+"\n"+socket.getLocalSocketAddress()+"\n");
                UCPConnection c=new UCPConnection(name,socket);
                connections.put(name,c);
                XTTProperties.printInfo(parameters[0]+": Connection '"+name+"' found for '"+OAdC+"'");
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting connection '"+name+"' for '"+OAdC+"'");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
    }


    /**
     * Close a connection.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void closeConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection: name");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();

            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": no previous connection to close");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            } else
            {
                try
                {
                    socket.close();
                    XTTProperties.printInfo(parameters[0]+": Connection closed");
                }
                catch(java.io.IOException ioe)
                {
                    XTTProperties.printFail(parameters[0] + ": Error closing socket");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ioe);
                    }
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            }
        }
    }

    /**
     * Check if a connection is open or closed by trying to read from it. If the conenction is open a socket timout will happen, if it is closed
     * it will also be recognized by the return value. Should only be used when absolutly necesarry like when checking if the server disconnected.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> status value can be closed or open
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkConnection: name status");
            return;
        } else if (parameters.length !=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name status");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();

            if(socket == null)
            {
                XTTProperties.printFail(parameters[0]+": no connection to check");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            } else
            {
                try
                {
                    InputStream in = null;
                    c.getSocket().setSoTimeout(500);
                    in = c.getSocket().getInputStream();
                    int b=in.read();
                    if(b==-1&&parameters[2].equalsIgnoreCase("closed"))
                    {
                        XTTProperties.printInfo(parameters[0]+": connection '"+parameters[1]+"' is CLOSED");
                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' is CLOSED");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } catch (java.net.SocketTimeoutException ste)
                {
                    if(parameters[2].equalsIgnoreCase("open"))
                    {
                        XTTProperties.printInfo(parameters[0]+": connection '"+parameters[1]+"' probably OPEN (timout)");
                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' probably OPEN (timout)");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } catch(java.lang.Exception ioe)
                {
                    XTTProperties.printFail(parameters[0] + ": Error with socket");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ioe);
                    }
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                } finally
                {
                    try{
                    c.getSocket().setSoTimeout(0);
                    }catch(Exception ex){}
                }
            }
        }
    }

    /**
     * Send an UCP message from the SMSC to the client.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the workerID of the worker which should send the request
     *                     ,<br> <code>parameters[3]</code> argument is the trn number (using a string switches to automatic trn generation)
     *                     ,<br> <code>parameters[4]</code> argument is the operation O or R
     *                     ,<br> <code>parameters[5]</code> argument is the operationType like 53 etc.
     *                     ,<br> <code>parameters[6]</code> argument is the data inlcuding the Slahes / after the operation time just before the checksum.
     *                     <br>If the workerID is ommited the corresponding worker of the connection is automatically found, but this only works if the
     *                     function module is directly connected to the SMSC with NO router/proxy inbetween or transparent routing used.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendSMSCUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSMSCUCPPacket: connectionName trn operation operationType data");
            XTTProperties.printFail(this.getClass().getName()+": sendSMSCUCPPacket: connectionName workerId trn operation operationType data");
            return;
        } else if (parameters.length !=7&&parameters.length !=6)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName workerId trn operation operationType data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();

            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            BufferedOutputStream out = null;
            //Send the byte array
            try
            {
                if(parameters.length ==7)
                {
                    out = UCPWorker.getOutputStream(parameters[2]);
                    if(out==null)
                    {
                        XTTProperties.printFail(parameters[0] + "("+c.getName()+"): output stream of SMSCWorker.id='"+parameters[2]+"' could not be found");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                    c.clearResponse();
                    sendUCPPacket(parameters,out,1,c);
                    c.setResponse(UCPWorker.getResponse(parameters[2]));
                } else
                {
                    out = UCPWorker.getOutputStream(socket.getLocalAddress(),socket.getLocalPort());
                    if(out==null)
                    {
                        XTTProperties.printFail(parameters[0] + "("+c.getName()+"): output stream of "+socket.getLocalAddress()+":"+socket.getLocalPort()+" could not be found");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                    c.clearResponse();
                    sendUCPPacket(parameters,out,0,c);
                    c.setResponse(UCPWorker.getResponse(socket.getInetAddress(),socket.getPort()));
                }

            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }
    
    /**
     * Method to send UCP packets
     * @param parameters
     */
    public void sendSMSCUCPPackets(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSMSCUCPPackets: connectionName trn operation operationType data");
            return;
        } else if (parameters.length !=7&&parameters.length !=6)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName trn operation operationType data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();

            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            BufferedOutputStream out = null;
            //Send the byte array
            try
            {
                out = UCPWorker.getOutputStream(socket.getInetAddress(),socket.getPort());
                if(out==null)
                {
                    XTTProperties.printFail(parameters[0] + "("+c.getName()+"): output stream of "+socket.getLocalAddress()+":"+socket.getLocalPort()+" could not be found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                c.clearResponse();
                sendUCPPacket(parameters,out,0,c);
                c.setResponse(UCPWorker.getResponse(socket.getInetAddress(),socket.getPort()));
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }

    /**
     * Wait for a SMSC injected UCP message. This is the correspondig read function to the sendSMSCUCPPacket fucntion.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void waitSMSCUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitSMSCUCPPacket: connectionName");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {

            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            Socket socket=c.getSocket();
            try
            {
                UCPWorker.awaitUCPMessage(0,new BufferedInputStream(socket.getInputStream()),new BufferedOutputStream(socket.getOutputStream()));
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error reading packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }

    /**
     * Send an UCP packet of an open connection.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the trn to use (a string switches to automatic trn)
     *                     ,<br> <code>parameters[3]</code> argument is operation O or R
     *                     ,<br> <code>parameters[4]</code> argument is operationType like ucp 51 and will influence the max number of / possible in the data
     *                     ,<br> <code>parameters[5]</code> argument is data starting and ending with / (everything between the operation type and the checksum)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendUCPPacket: connectionName trn operation operationType data");
            return;
        }
        else if (parameters.length != 6)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName trn operation operationType data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();
            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection "+c.getName()+" not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            BufferedOutputStream out = null;
            //Send the byte array
            try
            {
                out = new BufferedOutputStream(socket.getOutputStream());
                c.clearResponse();
                sendUCPPacket(parameters,out,0,c);
                readResponse(parameters[0]+ "("+c.getName()+")",c);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }

    private void sendUCPPacket(String parameters[],BufferedOutputStream out,int off, UCPConnection c)
    {
            String dataBody=parameters[5+off];
            if(!dataBody.startsWith("/")||!dataBody.endsWith("/"))
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): parameter"+(5+off)+" data must start with '/' and end with '/'");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }


            int trn=-1;
            try
            {
                trn = Integer.parseInt(parameters[2+off]);
            }
            catch(java.lang.NumberFormatException nfe)
            {
                trn=c.getAutoTRN();
                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): parameter trn is not a number using inernal: "+trn);
            }
            String operation=parameters[3+off].toUpperCase();
            if(!operation.equals("O")&&!operation.equals("R"))
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): parameter"+(3+off)+" (operation) may only be 'O' or 'R'");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }

            int operationType=-1;
            try
            {
                operationType = Integer.parseInt(parameters[4+off]);
                String[] checkBody=null;
                switch(operationType)
                {
                    case CALL_INPUT_OPERATION    :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=7)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): CALL_INPUT_OPERATION("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 6");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case MULTIADDRESS_CALL_INPUT :
                        checkBody=new String(dataBody+"00").split("/");
                        int num=0;
                        try
                        {
                            num=Integer.parseInt(checkBody[1]);
                        } catch (Exception ex)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): MULTIADDRESS_CALL_INPUT("+operationType+"): first parameter has to be number of following recipients");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        if(checkBody.length!=(7+num))
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): MULTIADDRESS_CALL_INPUT("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be "+(6+num));
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case SUPPSERVICE_CALL_INPUT  :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=18)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): SUPPSERVICE_CALL_INPUT("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 17");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case SMS_TRANSFER_OPERATION  :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=12)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): SMS_TRANSFER_OPERATION("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 11");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case SMT_ALERT_OPERATION     :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=4)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): SMT_ALERT_OPERATION("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 3");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case SUBMIT_SHORT_MESSAGE    :
                    case DELIVER_SHORT_MESSAGE   :
                    case DELIVER_NOTIFICATION    :
                    case MODIFY_MESSAGE          :
                    case INQUIRY_MESSAGE         :
                    case DELETE_MESSAGE          :
                    case RESPONSE_INQUIRY_MESSAGE:
                    case RESPONSE_DELETE_MESSAGE :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=35)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): TYPE_5X("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 34");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case SESSION_MANAGEMENT            :
                        checkBody=new String(dataBody+"00").split("/");
                        if(checkBody.length!=14)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): SESSION_MANAGEMENT("+operationType+"): the number of '/' in the parameter"+(5+off)+" (data) has to be 13");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return;
                        }
                        break;
                    case PROVISIONING_ACTIONS         :
                        break;
                    default:
                        XTTProperties.printFail(parameters[0]+"("+c.getName()+"): parameter"+(4+off)+" (operationType) is not an allowed UCP type");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                }
            }
            catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): parameter"+(4+off)+" (operationType) is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }


            //Send the byte array
            try
            {
                int command_length=HEADLENGTH+dataBody.length()+2;

                String outputString=ConvertLib.intToString(trn,2)
                            +"/"+ConvertLib.intToString(command_length,5)
                            +"/"+operation
                            +"/"+ConvertLib.intToString(operationType,2)
                            +dataBody;
                byte[] output=ConvertLib.getOctetByteArrayFromString(outputString);
                String checksum=ConvertLib.intToHex(ConvertLib.addBytes(output),2);

                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): sending: \n  UCP: stx"+outputString+checksum+"etx"
                    +"\n  trn            = "+trn
                    +"\n  length         = "+command_length
                    +"\n  operation      = "+operation
                    +"\n  operationType  = "+operationType
                    +"\n");
                synchronized(out)
                {
                    out.write(STX);
                    out.write(output);
                    out.write(ConvertLib.getOctetByteArrayFromString(checksum));
                    out.write(ETX);
                    out.flush();
                }

            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
    }

    private void readResponse(String functionname,UCPConnection c)
    {
        InputStream in = null;
        try
        {
            c.setResponse(new byte[0]);
            if(readtimeout>0)
            {
                c.getSocket().setSoTimeout(readtimeout);
            }
            //in = new BufferedInputStream(c.getSocket().getInputStream());
            in = c.getSocket().getInputStream();
            c.setResponse(UCPWorker.readResponse(functionname,in));
        } catch(Exception e)
        {
            XTTProperties.printFail(functionname+": Error reading packet");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        } finally
        {
            try
            {
                if(readtimeout>0)
                {
                    c.getSocket().setSoTimeout(0);
                }
            } catch(Exception ex){}
        }
    }

    /**
     * Send an UCP packet over an open connection with windowing enabled. This function does not automatically read the response.
     * You can send multiple packets before reading the responses with {@link FunctionModule_UCP#readWindowedUCPPacket(java.lang.String[]) FunctionModule_UCP.readWindowedUCPPacket(java.lang.String[])}
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the trn to use (a string switches to automatic trn)
     *                     ,<br> <code>parameters[3]</code> argument is operation O or R
     *                     ,<br> <code>parameters[4]</code> argument is operationType like ucp 51 and will influence the max number of / possible in the data
     *                     ,<br> <code>parameters[5]</code> argument is data starting and ending with / (everything between the operation type and the checksum)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendWindowedUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendWindowedUCPPacket: connectionName trn operation operationType data");
            return;
        }
        else if (parameters.length < 5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName trn operation operationType data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();
            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection "+c.getName()+" not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            BufferedOutputStream out = null;
            //Send the byte array
            try
            {
                out = new BufferedOutputStream(socket.getOutputStream());
                c.clearResponse();
                sendUCPPacket(parameters,out,0,c);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }

    /**
     * Read an UCP packet over an open connection with windowing enabled. Send with {@link #sendWindowedUCPPacket(java.lang.String[]) sendWindowedUCPPacket(java.lang.String[])}
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void readWindowedUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": readWindowedUCPPacket: connectionName");
            return;
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            Socket socket=c.getSocket();
            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection "+c.getName()+" not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            //Send the byte array
            try
            {
                readResponse(parameters[0],c);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error reading packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }


    /**
     * Send an free UCP packet over an open connection. 
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the stx to use (normal would be 0x02)
     *                     ,<br> <code>parameters[3]</code> argument is the etx to use (normal would be 0x03)
     *                     ,<br> <code>parameters[4]</code> argument append a correct checksum true/yes
     *                     ,<br> <code>parameters[5]</code> argument is everything between stx and etx with or without checksum
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendFreeUCPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendFreeUCPPacket: connectionName stx etx appendChecksum data");
            return;
        }
        else if (parameters.length !=6)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName stx etx appendChecksum data");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            Socket socket=c.getSocket();

            String dataBody=parameters[5];

            if((socket == null) || (!socket.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": connection "+c.getName()+" not open");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            byte stx=STX;
            try
            {
                stx = (Byte.decode(parameters[2])).byteValue();
            }
            catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): parameter2 (stx) is not a number: '"+parameters[2]+"'");
            }
            byte etx=ETX;
            boolean useETX=true;
            try
            {
                etx = (Byte.decode(parameters[3])).byteValue();
            }
            catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): parameter3 (etx) is not a number: "+parameters[3]+"'");
                useETX=false;
            }
            boolean useCheckSum=false;
            if(ConvertLib.textToBoolean(parameters[4]))
            {
                useCheckSum=true;
            }

            BufferedOutputStream out=null;
            InputStream in=null;

            c.clearResponse();
            //Send the byte array
            try
            {
                out = new BufferedOutputStream(socket.getOutputStream());
                in = socket.getInputStream();

                byte[] output=ConvertLib.getOctetByteArrayFromString(dataBody);
                String checksum="";
                if(useCheckSum)
                {
                    checksum=ConvertLib.intToHex(ConvertLib.addBytes(output),2);
                }

                String etxString="(0x"+ConvertLib.intToHex(etx,2)+")";
                if(!useETX)etxString="";
                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): sending: \n  UCP: (0x"+ConvertLib.intToHex(stx,2)+")"+dataBody+checksum+etxString+"\n");
                out.write(stx);
                out.write(output);
                if(useCheckSum)
                {
                    out.write(ConvertLib.getOctetByteArrayFromString(checksum));
                }
                if(useETX)
                {
                    out.write(etx);
                }
                out.flush();

            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            try
            {
                if(readtimeout>0)
                {
                    socket.setSoTimeout(readtimeout);
                }
                int firstByte=in.read();
                if(firstByte!=STX)
                {
                    throw new Exception(""+c.getName()+":STX not received: 0x"+ConvertLib.intToHex(firstByte));
                }

                byte[] head=new byte[HEADLENGTH];
                HTTPHelper.readBytes(in,head);

                XTTProperties.printVerbose(parameters[0]+": Received: 14 bytes");
                int command_trn             = ConvertLib.getIntFromStringBytes(head, 0,2);
                int command_length          = ConvertLib.getIntFromStringBytes(head, 3,5);
                char command_operation      = (char)head[9];
                int command_operation_type  = ConvertLib.getIntFromStringBytes(head, 11,2);

                int rchecksum=ConvertLib.addBytes(head);

                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): Received: \n  UCP: stx"+ConvertLib.createString(head)+""
                    +"\n  command_trn            = "+command_trn
                    +"\n  command_length         = "+command_length
                    +"\n  command_operation      = "+command_operation
                    +"\n  command_operation_type = "+command_operation_type
                    +"\n");

                byte[] response=new byte[command_length-HEADLENGTH];

                HTTPHelper.readBytes(in,response);

                byte[] cresponse=new byte[command_length];
                for(int i=0;i<head.length;i++)
                {
                    cresponse[i]=head[i];
                }
                for(int i=0;i<response.length;i++)
                {
                    cresponse[i+HEADLENGTH]=response[i];
                }

                c.setResponse(cresponse);

                rchecksum=rchecksum+ConvertLib.addBytes(response,0,response.length-2);

                String responseString=ConvertLib.getStringFromOctetByteArray(response, 0, response.length);
                String[] body=responseString.split("/");

                int lastByte=in.read();
                if(lastByte!=ETX)
                {
                    XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): Received: "+response.length+" bytes\n  UCP:"+responseString);
                    XTTProperties.printFail(parameters[0] +"("+c.getName()+"): Error: ETX not received: 0x"+ConvertLib.intToHex(lastByte));
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                } else if (!ConvertLib.intToHex(rchecksum,2).equals(body[body.length-1]))
                {
                    XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): Received: "+response.length+" bytes\n  UCP:"+responseString+"etx");
                    XTTProperties.printFail(parameters[0] +"("+c.getName()+"): Error: CheckSum not valid: calculated "+ConvertLib.intToHex(rchecksum,2)+"!= received "+body[body.length-1]);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                } else
                {
                    XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): Received: "+response.length+" bytes\n  UCP:"+responseString+"etx");
                }
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error reading packet");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                //XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            } finally
            {
                try
                {
                    if(readtimeout>0)
                    {
                        socket.setSoTimeout(0);
                    }
                } catch(Exception ex){}
            }
        }
    }



    /**
     * Query the response for a regular expression.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is variable to store the result
     *                     ,<br> <code>parameters[3]</code> argument is the regular expression to use
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public boolean queryTextResponse(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryTextResponse: connectionName variableName regularExpression");
            return false;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
            byte[] response=c.getResponse();

            if(response!=null&&response.length > 0)
            {

                String regex=parameters[3];
                XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): regex: '"+regex+"'");
                return ConvertLib.queryString(parameters[0]+"("+c.getName()+"): "+parameters[2]+"",ConvertLib.createString(response),regex,parameters[2]);
            } else
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+parameters[2]+": Nothing to check against");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    /**
     * Query the response does not contain a regular expression.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is variable to store the result
     *                     ,<br> <code>parameters[3]</code> argument is the regular expression to use
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public boolean queryTextResponseNegative(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryTextResponse: connectionName regularExpression");
            return false;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            UCPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }

            String regex=parameters[2];
            XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): regex: '"+regex+"'");
            return ConvertLib.queryStringNegative(parameters[0]+"("+c.getName()+")",ConvertLib.createString(c.getResponse()),regex);
        }
    }


    /**
     * Clears and reinitializes all the variables of the module. 
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        connections = Collections.synchronizedMap(new HashMap<String,UCPConnection>());
        readtimeout=XTTProperties.getIntProperty("SMSCSERVER/UCPREADTIMEOUT");
    }

    /**
     * Set the timeout on reading the response.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the readtimeout in ms
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setReadTimeout(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setReadTimeout: readtimeout");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": readtimeout");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
                readtimeout=timeout;
                XTTProperties.printInfo(parameters[0]+": readtimeout set to "+readtimeout+"ms");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": error setting readtimeout to "+parameters[1]+" - "+e.getClass().getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }



    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_UCP.java,v 1.14 2010/05/05 08:12:38 rajesh Exp $";
}