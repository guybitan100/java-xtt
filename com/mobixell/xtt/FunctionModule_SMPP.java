package com.mobixell.xtt;

import java.net.Socket;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * FunctionModule_SMPP.
 * <p>
 * Functions for sending SMPP packets. See SMPPWorker for more details.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.12 $
 * @see         SMPPWorker
 */
public class FunctionModule_SMPP extends FunctionModule implements SMPPConstants
{
    private Map<String,SMPPConnection> connections = Collections.synchronizedMap(new HashMap<String,SMPPConnection>());
    private int readtimeout=0;
    private boolean sendOnly=false;
    private Object workeridKey=new Object();
    private int workerid=0;

    public FunctionModule_SMPP()
    {
    }

    private class SMPPConnection
    {
        private String name = null;
        private Socket socket = null;
        private byte[] response = new byte[0];
        private byte[] tlvs = new byte[0];
        private int autoSequence=0;
        private boolean sendOnly=false;
        private SMPPWorker worker=null;

        public SMPPConnection(String name,Socket socket, int workerId)
        {
            this.name=name;
            this.socket=socket;
            sendOnly=true;
            worker=new SMPPWorker(workerId,socket,null,-1);
            worker.start();
        }
        public SMPPConnection(String name,Socket socket, boolean so)
        {
            this.name=name;
            this.socket=socket;
            sendOnly=so;
        }
        public SMPPConnection(String name,Socket socket)
        {
            this.name=name;
            this.socket=socket;
        }
        public int getAutoSequence()
        {
            int sequence=autoSequence++;
            return sequence;
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
        public void close() throws java.io.IOException
        {
            if(worker==null)
            {
                socket.close();
            } else
            {
                worker.setStop();
            }
        }
        public void clearResponse()
        {
            this.response = new byte[0];
        }
        public byte[] getTLVs()
        {
            return tlvs;
        }
        public void clearTLVs()
        {
            tlvs = new byte[0];
        }
        public boolean isSendOnly()
        {
            return sendOnly;
        }
        public void setSendOnly(boolean setSendOnly)
        {
            sendOnly=setSendOnly;
        }
        public void addTLV(byte[] tlv)
        {
            int pointer=0;
            byte[] newtlvs=new byte[tlvs.length+tlv.length];
            pointer=addBytesToArray(newtlvs,pointer,tlvs);
            pointer=addBytesToArray(newtlvs,pointer,tlv);
            tlvs=newtlvs;
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
                SMPPConnection c=new SMPPConnection(name,socket);
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
     * Create worker which connects to a specified IPAddress and port. The worker than can be used as a normal connection and has
     * all the features of a normal SMPPWorker.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument ip address to connect to
     *                     ,<br> <code>parameters[3]</code> argument is port to connect to)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void createServerConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createServerConnection: name ip port");
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
                XTTProperties.printDebug("Server connected: "+socket.getRemoteSocketAddress()+"\n"+socket.getLocalSocketAddress()+"\n");
                int workerID=-1;
                synchronized(workeridKey)
                {
                    workerID=workerid++;
                }
                SMPPConnection c=new SMPPConnection(name,socket,workerID);
                connections.put(name,c);
                XTTProperties.printInfo(parameters[0]+": Server Connection '"+name+"' opened to " + IP + ":" + port);
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
     * Get a connection already established to a SMPPWorker. The workerID is needed. In a controlled scenario workerID allways starts at 0.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> is the worker id
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void getServerConnection(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getServerConnection: name workerid");
            return;
        } else if (parameters.length !=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name workerid");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name = parameters[1];
            try
            {
                Socket socket = SMPPWorker.getSocket(parameters[2]);
                XTTProperties.printDebug("Connection found: "+socket.getRemoteSocketAddress()+"\n"+socket.getLocalSocketAddress()+"\n");
                SMPPConnection c=new SMPPConnection(name,socket,true);
                connections.put(name,c);
                XTTProperties.printInfo(parameters[0]+": Connection '"+name+"' found");
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting connection '"+name+"' ");
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
            SMPPConnection c=connections.get(parameters[1]);
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
                    c.close();
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
            SMPPConnection c=connections.get(parameters[1]);
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
     * Removes the TLVs stored in the connection.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void clearTLVs(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearTLVs: connectionName");
            return;
        } else if (parameters.length !=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            SMPPConnection c=connections.get(parameters[1]);
            if(c == null)
            {
                XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            } else
            {
                c.clearTLVs();
            }
        }
    }

    /**
     * Send an SMPP packet over an open connection. Responses are stored in SMSC/SMPP/[connectionName]/[field]. See the debug output for field names.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the command ID either as number or string
     *                     ,<br> <code>parameters[3]</code> argument is sequence number to use, if not a number it will be generated automatically
     *                     ,<br> <code>parameters[4]</code> argument and following are the additional parameters of the choosen command id. Either as integers or a Byte-String (like A1FF032E)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendSMPPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSMPPPacket: connectionName commandId sequence additionalParameters...");
            return;
        } else if (parameters.length < 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectionName commandId sequenceNumber additionalParameters...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            SMPPConnection c=connections.get(parameters[1]);
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
                if(sendSMPPPacket(parameters,out,c))
                {
                    readResponse(parameters[0]+ "("+c.getName()+")",c);
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
     * 
     * @param function
     * @param commandIDString - command name
     * @return - integer value of command id
     */
    private int getCommandIDFromString(String function, String commandIDString)
    {
        int commandID=0;
        if(commandIDString==null)return commandID;
        try
        {
            commandID = Integer.decode(commandIDString);
        } catch (NumberFormatException ex)
        {
            commandIDString=commandIDString.toUpperCase();
            if(commandIDString.equals("BIND_RECEIVER"))
            {
                commandID=BIND_RECEIVER;
            } else if(commandIDString.equals("BIND_TRANSMITTER"))
            {
                commandID=BIND_TRANSMITTER;
            } else if(commandIDString.equals("BIND_TRANSCEIVER"))
            {
                commandID=BIND_TRANSCEIVER;
            } else if(commandIDString.equals("UNBIND"))
            {
                commandID=UNBIND;
            } else if(commandIDString.equals("SUBMIT_SM"))
            {
                commandID=SUBMIT_SM;
            } else if(commandIDString.equals("DELIVER_SM"))
            {
                commandID=DELIVER_SM;
            } else if(commandIDString.equals("QUERY_SM"))
            {
                commandID=QUERY_SM;
            } else if(commandIDString.equals("CANCEL_SM"))
            {
                commandID=CANCEL_SM;
            } else if(commandIDString.equals("REPLACE_SM"))
            {
                commandID=REPLACE_SM;
            } else if(commandIDString.equals("ENQUIRE_LINK"))
            {
                commandID=ENQUIRE_LINK;
            } else if(commandIDString.equals("SUBMIT_MULTI"))
            {
                commandID=SUBMIT_MULTI;
            } else if(commandIDString.equals("DATA_SM"))
            {
                commandID=DATA_SM;
            } else if(commandIDString.equals("OUTBIND"))
            {
                commandID=OUTBIND;
            } else if(commandIDString.equals("ALERT_NOTIFICATION"))
            {
                commandID=ALERT_NOTIFICATION;
            } else if(commandIDString.equals("BROADCAST_SM"))
            {
                commandID=BROADCAST_SM;
            } else if(commandIDString.equals("QUERY_BROADCAST_SM"))
            {
                commandID=QUERY_BROADCAST_SM;
            } else if(commandIDString.equals("CANCEL_BROADCAST_SM"))
            {
                commandID=CANCEL_BROADCAST_SM;
            } else
            {
                XTTProperties.printFail(function+": commandID '"+commandIDString+"' is not recognized");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }
        }
        return commandID;
    }
    
    /**
     * Method to send SMPP packet
     * @param parameters - array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code>connection name
     *                     <br> <code>parameter[2]</code>system id <br> <code>parameters[3]</code>password
     *                     <br> <code>parameter[4]</code>password
     * @param out 
     * @param c - connection object
     * @return
     */
    private boolean sendSMPPPacket(String parameters[],BufferedOutputStream out,SMPPConnection c)
    {
        int sequence=-1;
        try
        {
            sequence = Integer.parseInt(parameters[3]);
        } catch(java.lang.NumberFormatException nfe)
        {
            sequence=c.getAutoSequence();
            XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): parameter sequence is not a number using internal: "+sequence);
        }

        int commandID = getCommandIDFromString(parameters[0]+"("+c.getName()+")",parameters[2]);
        byte[] packet=null;
        int pointer=0;
        String commandName=null;
        switch(commandID)
        {
            // SMPP 3.4
            case BIND_RECEIVER:
            {
                if(commandName==null)commandName="BIND_RECEIVER";
            }
            case BIND_TRANSMITTER:
            {
                if(commandName==null)commandName="BIND_TRANSMITTER";
            }
            case BIND_TRANSCEIVER:
            {
                if(commandName==null)commandName="BIND_TRANSCEIVER";
                if(parameters.length!=12)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 8 additional parameters required");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] system_id=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(system_id.length>16)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): system_id '"+parameters[4]+"' max length=15 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] password=ConvertLib.getCOctetByteArrayFromString(parameters[5]);
                if(password.length>9)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): password '"+parameters[5]+"' max length=8 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] system_type=ConvertLib.getCOctetByteArrayFromString(parameters[6]);
                if(system_type.length>13)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): system_type '"+parameters[6]+"' max length=12 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int version=Integer.decode(parameters[7]);
                if(version!=0x50&&version>0x34)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): version '"+parameters[7]+"' allowed 0x00-0x34 and 0x50");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] interface_version=ConvertLib.getByteArrayFromInt(version,1);
                int ton=Integer.decode(parameters[8]);
                if(ton>0xFF||ton<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): addr_ton '"+parameters[8]+"' set to 0x00");
                }
                byte[] addr_ton=ConvertLib.getByteArrayFromInt(ton,1);
                int npi=Integer.decode(parameters[9]);
                if(npi>0xFF||npi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): addr_npi '"+parameters[9]+"' set to 0x00");
                }
                byte[] addr_npi=ConvertLib.getByteArrayFromInt(npi,1);
                byte[] address_range=ConvertLib.getCOctetByteArrayFromString(parameters[10]);
                if(address_range.length>41)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): address_range '"+parameters[10]+"' max length=40 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16+system_id.length+password.length+system_type.length+interface_version.length+addr_ton.length+addr_npi.length+address_range.length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,system_id);
                pointer=addBytesToArray(packet,pointer,password);
                pointer=addBytesToArray(packet,pointer,system_type);
                pointer=addBytesToArray(packet,pointer,interface_version);
                pointer=addBytesToArray(packet,pointer,addr_ton);
                pointer=addBytesToArray(packet,pointer,addr_npi);
                pointer=addBytesToArray(packet,pointer,address_range);
                break;
            }
            case UNBIND:
            {
                if(commandName==null)commandName="UNBIND";
                if(parameters.length>4)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): no additinal parameters allowed");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16];
                break;
            }
            case SUBMIT_SM:
            {
                if(commandName==null)commandName="SUBMIT_SM";
            }
            case DELIVER_SM:
            {
                if(commandName==null)commandName="DELIVER_SM";
                if(parameters.length!=21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 17 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int dton=Integer.decode(parameters[8]);
                if(dton>0xFF||dton<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_ton '"+parameters[8]+"' set to 0x00");
                }
                byte[] dest_addr_ton=ConvertLib.getByteArrayFromInt(dton,1);
                int dnpi=Integer.decode(parameters[9]);
                if(dnpi>0xFF||dnpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_npi '"+parameters[9]+"' set to 0x00");
                }
                byte[] dest_addr_npi=ConvertLib.getByteArrayFromInt(dnpi,1);
                byte[] destination_addr=ConvertLib.getCOctetByteArrayFromString(parameters[10]);
                if(destination_addr.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): destination_addr '"+parameters[10]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int esmclass=Integer.decode(parameters[11]);
                if(esmclass>0xFF||esmclass<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esm_class '"+parameters[11]+"' set to 0x00");
                }
                byte[] esm_class=ConvertLib.getByteArrayFromInt(esmclass,1);
                int protocolid=Integer.decode(parameters[12]);
                if(protocolid>0xFF||protocolid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): protocol_id '"+parameters[12]+"' set to 0x00");
                }
                byte[] protocol_id=ConvertLib.getByteArrayFromInt(protocolid,1);
                int priorityflag=Integer.decode(parameters[13]);
                if(priorityflag>0xFF||priorityflag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): priority_flag '"+parameters[13]+"' set to 0x00");
                }
                byte[] priority_flag=ConvertLib.getByteArrayFromInt(priorityflag,1);
                byte[] schedule_delivery_time=ConvertLib.getCOctetByteArrayFromString(parameters[14]);
                if(schedule_delivery_time.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): schedule_delivery_time '"+parameters[14]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] validity_period=ConvertLib.getCOctetByteArrayFromString(parameters[15]);
                if(validity_period.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): validity_period '"+parameters[15]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int registereddelivery=Integer.decode(parameters[16]);
                if(registereddelivery>0xFF||registereddelivery<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): registered_delivery '"+parameters[16]+"' set to 0x00");
                }
                byte[] registered_delivery=ConvertLib.getByteArrayFromInt(registereddelivery,1);
                int replace_ifpresent_flag=Integer.decode(parameters[17]);
                if(replace_ifpresent_flag>0xFF||replace_ifpresent_flag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): replace_if_present_flag '"+parameters[17]+"' set to 0x00");
                }
                byte[] replace_if_present_flag=ConvertLib.getByteArrayFromInt(replace_ifpresent_flag,1);
                int datacoding=Integer.decode(parameters[18]);
                if(datacoding>0xFF||datacoding<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): data_coding '"+parameters[18]+"' set to 0x00");
                }
                byte[] data_coding=ConvertLib.getByteArrayFromInt(datacoding,1);
                int sm_default_msgid=Integer.decode(parameters[19]);
                if(sm_default_msgid>0xFF||sm_default_msgid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): sm_default_msg_id '"+parameters[19]+"' set to 0x00");
                }
                byte[] sm_default_msg_id=ConvertLib.getByteArrayFromInt(sm_default_msgid,1);
                byte[] short_message=ConvertLib.getOctetByteArrayFromString(parameters[20]);
                if(short_message.length>255)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): short_message '"+parameters[20]+"' max length=255 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] sm_length=ConvertLib.getByteArrayFromInt(short_message.length,1);

                packet=new byte[16+service_type.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +dest_addr_ton.length
                                  +dest_addr_npi.length
                                  +destination_addr.length
                                  +esm_class.length
                                  +protocol_id.length
                                  +priority_flag.length
                                  +schedule_delivery_time.length
                                  +validity_period.length
                                  +registered_delivery.length
                                  +replace_if_present_flag.length
                                  +data_coding.length
                                  +sm_default_msg_id.length
                                  +short_message.length
                                  +sm_length.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,dest_addr_ton           );
                pointer=addBytesToArray(packet,pointer,dest_addr_npi           );
                pointer=addBytesToArray(packet,pointer,destination_addr        );
                pointer=addBytesToArray(packet,pointer,esm_class               );
                pointer=addBytesToArray(packet,pointer,protocol_id             );
                pointer=addBytesToArray(packet,pointer,priority_flag           );
                pointer=addBytesToArray(packet,pointer,schedule_delivery_time  );
                pointer=addBytesToArray(packet,pointer,validity_period         );
                pointer=addBytesToArray(packet,pointer,registered_delivery     );
                pointer=addBytesToArray(packet,pointer,replace_if_present_flag );
                pointer=addBytesToArray(packet,pointer,data_coding             );
                pointer=addBytesToArray(packet,pointer,sm_default_msg_id       );
                pointer=addBytesToArray(packet,pointer,sm_length               );
                pointer=addBytesToArray(packet,pointer,short_message           );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case QUERY_SM:
            {
                if(commandName==null)commandName="QUERY_SM";
                if(parameters.length!=8)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 4 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[4]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16+message_id.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                break;
            }
            case CANCEL_SM:
            {
                if(commandName==null)commandName="DELIVER_SM";
                if(parameters.length!=12)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 8 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[5]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[5]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[6]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[7]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[7]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[8]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[8]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int dton=Integer.decode(parameters[9]);
                if(dton>0xFF||dton<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_ton '"+parameters[9]+"' set to 0x00");
                }
                byte[] dest_addr_ton=ConvertLib.getByteArrayFromInt(dton,1);
                int dnpi=Integer.decode(parameters[10]);
                if(dnpi>0xFF||dnpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_npi '"+parameters[10]+"' set to 0x00");
                }
                byte[] dest_addr_npi=ConvertLib.getByteArrayFromInt(dnpi,1);
                byte[] destination_addr=ConvertLib.getCOctetByteArrayFromString(parameters[11]);
                if(destination_addr.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): destination_addr '"+parameters[11]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16+service_type.length
                                  +message_id.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +dest_addr_ton.length
                                  +dest_addr_npi.length
                                  +destination_addr.length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,dest_addr_ton           );
                pointer=addBytesToArray(packet,pointer,dest_addr_npi           );
                pointer=addBytesToArray(packet,pointer,destination_addr        );
                break;
            }
            case REPLACE_SM:
            {
                if(commandName==null)commandName="REPLACE_SM";
                if(parameters.length!=13)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 9 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[5]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[4]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] schedule_delivery_time=ConvertLib.getCOctetByteArrayFromString(parameters[8]);
                if(schedule_delivery_time.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): schedule_delivery_time '"+parameters[8]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] validity_period=ConvertLib.getCOctetByteArrayFromString(parameters[9]);
                if(validity_period.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): validity_period '"+parameters[9]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int registereddelivery=Integer.decode(parameters[10]);
                if(registereddelivery>0xFF||registereddelivery<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): registered_delivery '"+parameters[10]+"' set to 0x00");
                }
                byte[] registered_delivery=ConvertLib.getByteArrayFromInt(registereddelivery,1);
                int sm_default_msgid=Integer.decode(parameters[11]);
                if(sm_default_msgid>0xFF||sm_default_msgid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): sm_default_msg_id '"+parameters[11]+"' set to 0x00");
                }
                byte[] sm_default_msg_id=ConvertLib.getByteArrayFromInt(sm_default_msgid,1);
                byte[] short_message=ConvertLib.getOctetByteArrayFromString(parameters[12]);
                if(short_message.length>255)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): short_message '"+parameters[12]+"' max length=255 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] sm_length=ConvertLib.getByteArrayFromInt(short_message.length,1);

                packet=new byte[16+message_id.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +schedule_delivery_time.length
                                  +validity_period.length
                                  +registered_delivery.length
                                  +sm_default_msg_id.length
                                  +short_message.length
                                  +sm_length.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,schedule_delivery_time  );
                pointer=addBytesToArray(packet,pointer,validity_period         );
                pointer=addBytesToArray(packet,pointer,registered_delivery     );
                pointer=addBytesToArray(packet,pointer,sm_default_msg_id       );
                pointer=addBytesToArray(packet,pointer,sm_length               );
                pointer=addBytesToArray(packet,pointer,short_message           );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case ENQUIRE_LINK:
            {
                if(commandName==null)commandName="ENQUIRE_LINK";
                if(parameters.length>4)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): no additinal parameters allowed");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16];
                break;
            }
            case SUBMIT_MULTI:
            {
                if(commandName==null)commandName="SUBMIT_MULTI";
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int numberofdests=Integer.decode(parameters[8]);
                if(numberofdests>0xFF||numberofdests<1)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): number_of_dests '"+parameters[8]+"' allowed 1 to 255");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] number_of_dests=ConvertLib.getByteArrayFromInt(numberofdests,1);

                byte[][] dest_address=new byte[numberofdests][0];

                int paramcount=8;
                for(int i=0;i<numberofdests;i++)
                {
                    int destflag=Integer.decode(parameters[++paramcount]);
                    if(destflag>0x02||destflag<0x01)
                    {
                        XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): destflag '"+parameters[paramcount]+"' allowed 1 or 2");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return false;
                    } else if(destflag==1)
                    {
                        byte[] dest_flag=ConvertLib.getByteArrayFromInt(destflag,1);
                        int dton=Integer.decode(parameters[++paramcount]);
                        if(dton>0xFF||dton<0)
                        {
                            XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_ton '"+parameters[paramcount]+"' set to 0x00");
                        }
                        byte[] dest_addr_ton=ConvertLib.getByteArrayFromInt(dton,1);
                        int dnpi=Integer.decode(parameters[++paramcount]);
                        if(dnpi>0xFF||dnpi<0)
                        {
                            XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_npi '"+parameters[paramcount]+"' set to 0x00");
                        }
                        byte[] dest_addr_npi=ConvertLib.getByteArrayFromInt(dnpi,1);
                        byte[] destination_addr=ConvertLib.getCOctetByteArrayFromString(parameters[++paramcount]);
                        if(destination_addr.length>21)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): destination_addr '"+parameters[paramcount]+"' max length=20 bytes");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return false;
                        }
                        dest_address[i]=new byte[dest_flag.length+dest_addr_ton.length+dest_addr_npi.length+destination_addr.length];
                        pointer=0;
                        pointer=addBytesToArray(dest_address[i],pointer,dest_flag);
                        pointer=addBytesToArray(dest_address[i],pointer,dest_addr_ton);
                        pointer=addBytesToArray(dest_address[i],pointer,dest_addr_npi);
                        pointer=addBytesToArray(dest_address[i],pointer,destination_addr);
                    } else
                    {
                        byte[] dest_flag=ConvertLib.getByteArrayFromInt(destflag,1);
                        byte[] dl_name=ConvertLib.getCOctetByteArrayFromString(parameters[++paramcount]);
                        if(dl_name.length>21)
                        {
                            XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dl_name '"+parameters[paramcount]+"' max length=20 bytes");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                            return false;
                        }
                        dest_address[i]=new byte[dest_flag.length+dl_name.length];
                        pointer=0;
                        pointer=addBytesToArray(dest_address[i],pointer,dest_flag);
                        pointer=addBytesToArray(dest_address[i],pointer,dl_name);
                    }
                }

                int esmclass=Integer.decode(parameters[++paramcount]);
                if(esmclass>0xFF||esmclass<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esm_class '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] esm_class=ConvertLib.getByteArrayFromInt(esmclass,1);
                int protocolid=Integer.decode(parameters[++paramcount]);
                if(protocolid>0xFF||protocolid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): protocol_id '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] protocol_id=ConvertLib.getByteArrayFromInt(protocolid,1);
                int priorityflag=Integer.decode(parameters[++paramcount]);
                if(priorityflag>0xFF||priorityflag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): priority_flag '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] priority_flag=ConvertLib.getByteArrayFromInt(priorityflag,1);
                byte[] schedule_delivery_time=ConvertLib.getCOctetByteArrayFromString(parameters[++paramcount]);
                if(schedule_delivery_time.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): schedule_delivery_time '"+parameters[paramcount]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] validity_period=ConvertLib.getCOctetByteArrayFromString(parameters[++paramcount]);
                if(validity_period.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): validity_period '"+parameters[paramcount]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int registereddelivery=Integer.decode(parameters[++paramcount]);
                if(registereddelivery>0xFF||registereddelivery<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): registered_delivery '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] registered_delivery=ConvertLib.getByteArrayFromInt(registereddelivery,1);
                int replace_ifpresent_flag=Integer.decode(parameters[++paramcount]);
                if(replace_ifpresent_flag>0xFF||replace_ifpresent_flag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): replace_if_present_flag '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] replace_if_present_flag=ConvertLib.getByteArrayFromInt(replace_ifpresent_flag,1);
                int datacoding=Integer.decode(parameters[++paramcount]);
                if(datacoding>0xFF||datacoding<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): data_coding '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] data_coding=ConvertLib.getByteArrayFromInt(datacoding,1);
                int sm_default_msgid=Integer.decode(parameters[++paramcount]);
                if(sm_default_msgid>0xFF||sm_default_msgid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): sm_default_msg_id '"+parameters[paramcount]+"' set to 0x00");
                }
                byte[] sm_default_msg_id=ConvertLib.getByteArrayFromInt(sm_default_msgid,1);
                byte[] short_message=ConvertLib.getOctetByteArrayFromString(parameters[++paramcount]);
                if(short_message.length>255)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): short_message '"+parameters[paramcount]+"' max length=255 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] sm_length=ConvertLib.getByteArrayFromInt(short_message.length,1);

                int destlength=0;
                for(int i=0;i<numberofdests;i++)
                {
                    destlength=destlength+dest_address[i].length;
                }
                packet=new byte[16+service_type.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +number_of_dests.length
                                  +destlength
                                  +esm_class.length
                                  +protocol_id.length
                                  +priority_flag.length
                                  +schedule_delivery_time.length
                                  +validity_period.length
                                  +registered_delivery.length
                                  +replace_if_present_flag.length
                                  +data_coding.length
                                  +sm_default_msg_id.length
                                  +short_message.length
                                  +sm_length.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,number_of_dests         );
                for(int i=0;i<numberofdests;i++)
                {
                    pointer=addBytesToArray(packet,pointer,dest_address[i]     );
                }
                pointer=addBytesToArray(packet,pointer,esm_class               );
                pointer=addBytesToArray(packet,pointer,protocol_id             );
                pointer=addBytesToArray(packet,pointer,priority_flag           );
                pointer=addBytesToArray(packet,pointer,schedule_delivery_time  );
                pointer=addBytesToArray(packet,pointer,validity_period         );
                pointer=addBytesToArray(packet,pointer,registered_delivery     );
                pointer=addBytesToArray(packet,pointer,replace_if_present_flag );
                pointer=addBytesToArray(packet,pointer,data_coding             );
                pointer=addBytesToArray(packet,pointer,sm_default_msg_id       );
                pointer=addBytesToArray(packet,pointer,sm_length               );
                pointer=addBytesToArray(packet,pointer,short_message           );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case DATA_SM:
            {
                if(commandName==null)commandName="DATA_SM";
                if(parameters.length!=14)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 10 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int dton=Integer.decode(parameters[8]);
                if(dton>0xFF||dton<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_ton '"+parameters[8]+"' set to 0x00");
                }
                byte[] dest_addr_ton=ConvertLib.getByteArrayFromInt(dton,1);
                int dnpi=Integer.decode(parameters[9]);
                if(dnpi>0xFF||dnpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): dest_addr_npi '"+parameters[9]+"' set to 0x00");
                }
                byte[] dest_addr_npi=ConvertLib.getByteArrayFromInt(dnpi,1);
                byte[] destination_addr=ConvertLib.getCOctetByteArrayFromString(parameters[10]);
                if(destination_addr.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): destination_addr '"+parameters[10]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int esmclass=Integer.decode(parameters[11]);
                if(esmclass>0xFF||esmclass<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esm_class '"+parameters[11]+"' set to 0x00");
                }
                byte[] esm_class=ConvertLib.getByteArrayFromInt(esmclass,1);
                int registereddelivery=Integer.decode(parameters[12]);
                if(registereddelivery>0xFF||registereddelivery<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): registered_delivery '"+parameters[12]+"' set to 0x00");
                }
                byte[] registered_delivery=ConvertLib.getByteArrayFromInt(registereddelivery,1);
                int datacoding=Integer.decode(parameters[13]);
                if(datacoding>0xFF||datacoding<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): data_coding '"+parameters[13]+"' set to 0x00");
                }
                byte[] data_coding=ConvertLib.getByteArrayFromInt(datacoding,1);

                packet=new byte[16+service_type.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +dest_addr_ton.length
                                  +dest_addr_npi.length
                                  +destination_addr.length
                                  +esm_class.length
                                  +registered_delivery.length
                                  +data_coding.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,dest_addr_ton           );
                pointer=addBytesToArray(packet,pointer,dest_addr_npi           );
                pointer=addBytesToArray(packet,pointer,destination_addr        );
                pointer=addBytesToArray(packet,pointer,esm_class               );
                pointer=addBytesToArray(packet,pointer,registered_delivery     );
                pointer=addBytesToArray(packet,pointer,data_coding             );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            // SMPP 5.0
            case OUTBIND:
            {
                if(commandName==null)commandName="OUTBIND";
                if(parameters.length!=6)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 2 additional parameters required");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            byte[] system_id=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
            if(system_id.length>16)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): system_id '"+parameters[4]+"' max length=15 bytes");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            byte[] password=ConvertLib.getCOctetByteArrayFromString(parameters[5]);
            if(password.length>9)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): password '"+parameters[5]+"' max length=8 bytes");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            packet=new byte[16+system_id.length+password.length];
            pointer=16;
            pointer=addBytesToArray(packet,pointer,system_id);
            pointer=addBytesToArray(packet,pointer,password);
            break;
        }
        case ALERT_NOTIFICATION:
        {
            if(commandName==null)commandName="ALERT_NOTIFICATION";
            if(parameters.length!=10)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 6 additional parameters required (omit sm_length)");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            int ston=Integer.decode(parameters[4]);
            if(ston>0xFF||ston<0)
            {
                XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[4]+"' set to 0x00");
            }
            byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
            int snpi=Integer.decode(parameters[5]);
            if(snpi>0xFF||snpi<0)
            {
                XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[5]+"' set to 0x00");
            }
            byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
            byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[6]);
            if(source_address.length>21)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[6]+"' max length=20 bytes");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            int eton=Integer.decode(parameters[7]);
            if(eton>0xFF||eton<0)
            {
                XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esme_addr_ton '"+parameters[7]+"' set to 0x00");
            }
            byte[] esme_addr_ton=ConvertLib.getByteArrayFromInt(eton,1);
            int enpi=Integer.decode(parameters[8]);
            if(enpi>0xFF||enpi<0)
            {
                XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esme_addr_npi '"+parameters[8]+"' set to 0x00");
            }
            byte[] esme_addr_npi=ConvertLib.getByteArrayFromInt(enpi,1);
            byte[] esme_addr=ConvertLib.getCOctetByteArrayFromString(parameters[9]);
            if(esme_addr.length>21)
            {
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): esme_addr '"+parameters[9]+"' max length=20 bytes");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }
            packet=new byte[16+source_addr_ton.length
                              +source_addr_npi.length
                              +source_address.length
                              +esme_addr_ton.length
                              +esme_addr_npi.length
                              +esme_addr.length
                              +c.getTLVs().length];
            pointer=16;
            pointer=addBytesToArray(packet,pointer,source_addr_ton         );
            pointer=addBytesToArray(packet,pointer,source_addr_npi         );
            pointer=addBytesToArray(packet,pointer,source_address          );
            pointer=addBytesToArray(packet,pointer,esme_addr_ton           );
            pointer=addBytesToArray(packet,pointer,esme_addr_npi           );
                pointer=addBytesToArray(packet,pointer,esme_addr               );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case BROADCAST_SM:
            {
                if(commandName==null)commandName="BROADCAST_SM";
                if(parameters.length!=15)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 11 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[8]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[8]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int priorityflag=Integer.decode(parameters[9]);
                if(priorityflag>0xFF||priorityflag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): priority_flag '"+parameters[9]+"' set to 0x00");
                }
                byte[] priority_flag=ConvertLib.getByteArrayFromInt(priorityflag,1);
                byte[] schedule_delivery_time=ConvertLib.getCOctetByteArrayFromString(parameters[10]);
                if(schedule_delivery_time.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): schedule_delivery_time '"+parameters[10]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] validity_period=ConvertLib.getCOctetByteArrayFromString(parameters[11]);
                if(validity_period.length>17)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): validity_period '"+parameters[11]+"' max length=16 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int replace_ifpresent_flag=Integer.decode(parameters[12]);
                if(replace_ifpresent_flag>0xFF||replace_ifpresent_flag<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): replace_if_present_flag '"+parameters[12]+"' set to 0x00");
                }
                byte[] replace_if_present_flag=ConvertLib.getByteArrayFromInt(replace_ifpresent_flag,1);
                int datacoding=Integer.decode(parameters[13]);
                if(datacoding>0xFF||datacoding<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): data_coding '"+parameters[13]+"' set to 0x00");
                }
                byte[] data_coding=ConvertLib.getByteArrayFromInt(datacoding,1);
                int sm_default_msgid=Integer.decode(parameters[14]);
                if(sm_default_msgid>0xFF||sm_default_msgid<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): sm_default_msg_id '"+parameters[14]+"' set to 0x00");
                }
                byte[] sm_default_msg_id=ConvertLib.getByteArrayFromInt(sm_default_msgid,1);

                packet=new byte[16+service_type.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +message_id.length
                                  +priority_flag.length
                                  +schedule_delivery_time.length
                                  +validity_period.length
                                  +replace_if_present_flag.length
                                  +data_coding.length
                                  +sm_default_msg_id.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,priority_flag           );
                pointer=addBytesToArray(packet,pointer,schedule_delivery_time  );
                pointer=addBytesToArray(packet,pointer,validity_period         );
                pointer=addBytesToArray(packet,pointer,replace_if_present_flag );
                pointer=addBytesToArray(packet,pointer,data_coding             );
                pointer=addBytesToArray(packet,pointer,sm_default_msg_id       );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case QUERY_BROADCAST_SM:
            {
                if(commandName==null)commandName="QUERY_BROADCAST_SM";
                if(parameters.length!=8)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 4 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[4]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[5]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[5]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[6]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[7]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[7]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                packet=new byte[16+message_id.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            case CANCEL_BROADCAST_SM:
            {
                if(commandName==null)commandName="CANCEL_BROADCAST_SM";
                if(parameters.length!=9)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): 5 additional parameters required (omit sm_length)");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] service_type=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
                if(service_type.length>5)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): service_type '"+parameters[4]+"' max length=4 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                byte[] message_id=ConvertLib.getCOctetByteArrayFromString(parameters[5]);
                if(message_id.length>65)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): message_id '"+parameters[5]+"' max length=64 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
                int ston=Integer.decode(parameters[6]);
                if(ston>0xFF||ston<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_ton '"+parameters[6]+"' set to 0x00");
                }
                byte[] source_addr_ton=ConvertLib.getByteArrayFromInt(ston,1);
                int snpi=Integer.decode(parameters[7]);
                if(snpi>0xFF||snpi<0)
                {
                    XTTProperties.printWarn(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_addr_npi '"+parameters[7]+"' set to 0x00");
                }
                byte[] source_addr_npi=ConvertLib.getByteArrayFromInt(snpi,1);
                byte[] source_address=ConvertLib.getCOctetByteArrayFromString(parameters[8]);
                if(source_address.length>21)
                {
                    XTTProperties.printFail(parameters[0]+"("+c.getName()+"): "+commandName+"("+commandID+"): source_address '"+parameters[8]+"' max length=20 bytes");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }

                packet=new byte[16+service_type.length
                                  +message_id.length
                                  +source_addr_ton.length
                                  +source_addr_npi.length
                                  +source_address.length
                                  +c.getTLVs().length];
                pointer=16;
                pointer=addBytesToArray(packet,pointer,service_type            );
                pointer=addBytesToArray(packet,pointer,message_id              );
                pointer=addBytesToArray(packet,pointer,source_addr_ton         );
                pointer=addBytesToArray(packet,pointer,source_addr_npi         );
                pointer=addBytesToArray(packet,pointer,source_address          );
                pointer=addBytesToArray(packet,pointer,c.getTLVs()             );
                break;
            }
            default:
                XTTProperties.printFail(parameters[0]+"("+c.getName()+"): commandID '"+commandID+"' is not supported/allowed SMPP type");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
        }
        pointer=0;
        pointer=addBytesToArray(packet,pointer,ConvertLib.getByteArrayFromInt(packet.length,4));
        pointer=addBytesToArray(packet,pointer,ConvertLib.getByteArrayFromInt(commandID,4));
        pointer=addBytesToArray(packet,pointer,ConvertLib.getByteArrayFromInt(0,4));
        pointer=addBytesToArray(packet,pointer,ConvertLib.getByteArrayFromInt(sequence,4));

        //Send the byte array
        try
        {
            XTTProperties.printDebug(parameters[0]+"("+c.getName()+"): sending SMPP:\n"+ConvertLib.getHexView(packet)
                +"\n  command_length   = 0x"+ConvertLib.intToHex(packet.length,8)
                +"\n  command_id       = 0x"+ConvertLib.intToHex(commandID,8)
                +"\n  command_status   = 0x00000000"
                +"\n  command_sequence = 0x"+ConvertLib.intToHex(sequence,8)
                +"\n");
            synchronized(c.getSocket())
            {
                out.write(packet);
                out.flush();
            }
            return true;
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + "("+c.getName()+"): Error sending packet");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        }
    }

    private int addBytesToArray(byte[] base, int basePointer, byte[] data)
    {
        return ConvertLib.addBytesToArray(base,basePointer,data);
    }
    
    /**
     * Reads response
     * @param functionname
     * @param c - connection object
     */
    private void readResponse(String functionname,SMPPConnection c)
    {
        if(c.isSendOnly())return;
        BufferedInputStream in = null;
        try
        {
            c.setResponse(new byte[0]);
            if(readtimeout>0)
            {
                c.getSocket().setSoTimeout(readtimeout);
            }
            in = new BufferedInputStream(c.getSocket().getInputStream());
            //in = c.getSocket().getInputStream();
            c.setResponse(SMPPWorker.readResponse(functionname,in,new String[]{"SMSC/SMPP/"+c.getName()}));
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
     * Clears and reinitializes all the variables of the module.
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        connections = Collections.synchronizedMap(new HashMap<String,SMPPConnection>());
        readtimeout=XTTProperties.getIntProperty("SMSCSERVER/SMPPREADTIMEOUT");
        workerid=0;
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


    public void setResponseStatus(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setResponseStatus: commandID responseCode");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": commandID responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                int commandID=getCommandIDFromString(parameters[0],parameters[1]);
                if(commandID<=0)
                {
                    XTTProperties.printFail(parameters[0]+": commandID '"+commandID+"' is not supported/allowed SMPP type");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                int status=Integer.decode(parameters[2]);
                XTTProperties.printInfo(parameters[0]+": set response status for '"+commandID+"' to '"+status+"'");
                SMPPWorker.setResponseStatus(commandID,status);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": error setting response status "+e.getClass().getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }


    /**
     * Add a TLV to a connection. The TLVs will be sent with each packet which allows TLVs unless cleared.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the connectionName
     *                     ,<br> <code>parameters[2]</code> argument is the tag id either as number or string
     *                     ,<br> <code>parameters[3]</code> argument is the value either as integer or String representing the bytes (F467B201A1) etc.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void addTLV(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addTLV: connectionName tagID value");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": connectionName tagID value");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                SMPPConnection c=connections.get(parameters[1]);
                if(c == null)
                {
                    XTTProperties.printFail(parameters[0]+": connection '"+parameters[1]+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                c.addTLV(createTLV(parameters[0],parameters[2],parameters[3]));
            } catch (IllegalArgumentException iae)
            {
                // they should all have been catched/created inside createTLV
                //iae.printStackTrace();
            } catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": error adding TLV "+e.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }

    /**
     * Add a response TLV to a specific commandID. The TLVs will be sent with each response packet which allows TLVs unless cleared. Currently only DATA_SM is working.
     *
     * @param parameters   array of String containing the parameters. <br><code>parameters[0]</code> argument is always
     *                     the method name,<br> <code>parameters[1]</code> argument is the command id
     *                     ,<br> <code>parameters[2]</code> argument is the tag id either as number or string
     *                     ,<br> <code>parameters[3]</code> argument is the value either as integer or String representing the bytes (F467B201A1) etc.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void addResponseTLV(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addResponseTLV: commandID tagID value");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": commandID tagID value");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                int commandID=getCommandIDFromString(parameters[0],parameters[1]);
                if(commandID<=0)
                {
                    XTTProperties.printFail(parameters[0]+": commandID '"+commandID+"' is not supported/allowed SMPP type");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                SMPPWorker.addResponseTLV(commandID,createTLV(parameters[0],parameters[2],parameters[3]));
            } catch (IllegalArgumentException iae)
            {
                // they should all have been catched/created inside createTLV
                //iae.printStackTrace();
            } catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": error adding TLV "+e.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }
    public void setOverrideResponse(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setOverrideResponse:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setOverrideResponse: commandID");
            XTTProperties.printFail(this.getClass().getName()+": setOverrideResponse: commandID value");
            return;
        }
        if(parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": commandID");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": commandID value");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                if(parameters.length==1)
                {
                    SMPPWorker.clearOverrideResponse();
                } else
                {
                    byte[] value=null;
                    if(parameters.length>2)value=ConvertLib.getByteArrayFromHexString(parameters[2]);
                    int commandID=getCommandIDFromString(parameters[0],parameters[1]);
                    if(commandID<=0)
                    {
                        XTTProperties.printFail(parameters[0]+": commandID '"+commandID+"' is not supported/allowed SMPP type");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    }
                    SMPPWorker.setOverrideResponse(commandID,value);
                }
            } catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": error setting override response "+e.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
    }
    
    /**
     * Clears response TLV
     * @param parameters
     */
    public void clearResponseTLV(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearResponseTLV:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": clearResponseTLV: commandID");
            return;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": commandID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(parameters.length>1)
            {
                int commandID=getCommandIDFromString(parameters[0],parameters[1]);
                if(commandID<=0)
                {
                    XTTProperties.printFail(parameters[0]+": commandID '"+commandID+"' is not supported/allowed SMPP type");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                SMPPWorker.clearResponseTLV(commandID);
            } else
            {
                SMPPWorker.clearResponseTLV();
            }
        }
    }

    private int getTAGID(String stag)
    {
        return SMPPWorker.getTAGID(stag);
        /*
        if(stag.equals("DEST_ADDR_SUBUNIT"))            return OPT_DEST_ADDR_SUBUNIT           ;
        if(stag.equals("DEST_NETWORK_TYPE"))            return OPT_DEST_NETWORK_TYPE           ;
        if(stag.equals("DEST_BEARER_TYPE"))             return OPT_DEST_BEARER_TYPE            ;
        if(stag.equals("DEST_TELEMATICS_ID"))           return OPT_DEST_TELEMATICS_ID          ;
        if(stag.equals("SOURCE_ADDR_SUBUNIT"))          return OPT_SOURCE_ADDR_SUBUNIT         ;
        if(stag.equals("SOURCE_NETWORK_TYPE"))          return OPT_SOURCE_NETWORK_TYPE         ;
        if(stag.equals("SOURCE_BEARER_TYPE"))           return OPT_SOURCE_BEARER_TYPE          ;
        if(stag.equals("SOURCE_TELEMATICS_ID"))         return OPT_SOURCE_TELEMATICS_ID        ;
        if(stag.equals("QOS_TIME_TO_LIVE"))             return OPT_QOS_TIME_TO_LIVE            ;
        if(stag.equals("PAYLOAD_TYPE"))                 return OPT_PAYLOAD_TYPE                ;
        if(stag.equals("ADDITIONAL_STATUS_INFO_TEXT"))  return OPT_ADDITIONAL_STATUS_INFO_TEXT ;
        if(stag.equals("RECEIPTED_MESSAGE_ID"))         return OPT_RECEIPTED_MESSAGE_ID        ;
        if(stag.equals("MS_MSG_WAIT_FACILITIES"))       return OPT_MS_MSG_WAIT_FACILITIES      ;
        if(stag.equals("PRIVACY_INDICATOR"))            return OPT_PRIVACY_INDICATOR           ;
        if(stag.equals("SOURCE_SUBADDRESS"))            return OPT_SOURCE_SUBADDRESS           ;
        if(stag.equals("DEST_SUBADDRESS"))              return OPT_DEST_SUBADDRESS             ;
        if(stag.equals("USER_MESSAGE_REFERENCE"))       return OPT_USER_MESSAGE_REFERENCE      ;
        if(stag.equals("USER_RESPONSE_CODE"))           return OPT_USER_RESPONSE_CODE          ;
        if(stag.equals("SOURCE_PORT"))                  return OPT_SOURCE_PORT                 ;
        if(stag.equals("DESTINATION_PORT"))             return OPT_DESTINATION_PORT            ;
        if(stag.equals("SAR_MSG_REF_NUM"))              return OPT_SAR_MSG_REF_NUM             ;
        if(stag.equals("LANGUAGE_INDICATOR"))           return OPT_LANGUAGE_INDICATOR          ;
        if(stag.equals("SAR_TOTAL_SEGMENTS"))           return OPT_SAR_TOTAL_SEGMENTS          ;
        if(stag.equals("SAR_SEGMENT_SEQNUM"))           return OPT_SAR_SEGMENT_SEQNUM          ;
        if(stag.equals("SC_INTERFACE_VERSION"))         return OPT_SC_INTERFACE_VERSION        ;
        if(stag.equals("CALLBACK_NUM_PRES_IND"))        return OPT_CALLBACK_NUM_PRES_IND       ;
        if(stag.equals("CALLBACK_NUM_ATAG"))            return OPT_CALLBACK_NUM_ATAG           ;
        if(stag.equals("NUMBER_OF_MESSAGES"))           return OPT_NUMBER_OF_MESSAGES          ;
        if(stag.equals("CALLBACK_NUM"))                 return OPT_CALLBACK_NUM                ;
        if(stag.equals("DPF_RESULT"))                   return OPT_DPF_RESULT                  ;
        if(stag.equals("SET_DPF"))                      return OPT_SET_DPF                     ;
        if(stag.equals("MS_AVAILABILITY_STATUS"))       return OPT_MS_AVAILABILITY_STATUS      ;
        if(stag.equals("NETWORK_ERROR_CODE"))           return OPT_NETWORK_ERROR_CODE          ;
        if(stag.equals("MESSAGE_PAYLOAD"))              return OPT_MESSAGE_PAYLOAD             ;
        if(stag.equals("DELIVERY_FAILURE_REASON"))      return OPT_DELIVERY_FAILURE_REASON     ;
        if(stag.equals("MORE_MESSAGES_TO_SEND"))        return OPT_MORE_MESSAGES_TO_SEND       ;
        if(stag.equals("MESSAGE_STATE"))                return OPT_MESSAGE_STATE               ;
        if(stag.equals("USSD_SERVICE_OP"))              return OPT_USSD_SERVICE_OP             ;
        if(stag.equals("DISPLAY_TIME"))                 return OPT_DISPLAY_TIME                ;
        if(stag.equals("SMS_SIGNAL"))                   return OPT_SMS_SIGNAL                  ;
        if(stag.equals("MS_VALIDITY"))                  return OPT_MS_VALIDITY                 ;
        if(stag.equals("ALERT_ON_MESSAGE_DELIVERY"))    return OPT_ALERT_ON_MESSAGE_DELIVERY   ;
        if(stag.equals("ITS_REPLY_TYPE"))               return OPT_ITS_REPLY_TYPE              ;
        if(stag.equals("ITS_SESSION_INFO"))             return OPT_ITS_SESSION_INFO            ;
        if(stag.equals("CONGESTION_STATE"))             return OPT_CONGESTION_STATE            ;
        if(stag.equals("BROADCAST_CHANNEL_INDICATOR"))  return OPT_BROADCAST_CHANNEL_INDICATOR ;
        if(stag.equals("BROADCAST_CONTENT_TYPE"))       return OPT_BROADCAST_CONTENT_TYPE      ;
        if(stag.equals("BROADCAST_CONTENT_TYPE_INFO"))  return OPT_BROADCAST_CONTENT_TYPE_INFO ;
        if(stag.equals("BROADCAST_MESSAGE_CLASS"))      return OPT_BROADCAST_MESSAGE_CLASS     ;
        if(stag.equals("BROADCAST_REP_NUM"))            return OPT_BROADCAST_REP_NUM           ;
        if(stag.equals("BROADCAST_FREQUENCY_INTERVAL")) return OPT_BROADCAST_FREQUENCY_INTERVAL;
        if(stag.equals("BROADCAST_AREA_IDENTIFIER"))    return OPT_BROADCAST_AREA_IDENTIFIER   ;
        if(stag.equals("BROADCAST_ERROR_STATUS"))       return OPT_BROADCAST_ERROR_STATUS      ;
        if(stag.equals("BROADCAST_AREA_SUCCESS"))       return OPT_BROADCAST_AREA_SUCCESS      ;
        if(stag.equals("BROADCAST_END_TIME"))           return OPT_BROADCAST_END_TIME          ;
        if(stag.equals("BROADCAST_SERVICE_GROUP"))      return OPT_BROADCAST_SERVICE_GROUP     ;
        if(stag.equals("BILLING_IDENTIFICATION"))       return OPT_BILLING_IDENTIFICATION      ;
        if(stag.equals("SOURCE_NETWORK_ID"))            return OPT_SOURCE_NETWORK_ID           ;
        if(stag.equals("DEST_NETWORK_ID"))              return OPT_DEST_NETWORK_ID             ;
        if(stag.equals("SOURCE_NODE_ID"))               return OPT_SOURCE_NODE_ID              ;
        if(stag.equals("DEST_NODE_ID"))                 return OPT_DEST_NODE_ID                ;
        if(stag.equals("DEST_ADDR_NP_RESOLUTION"))      return OPT_DEST_ADDR_NP_RESOLUTION     ;
        if(stag.equals("DEST_ADDR_NP_INFORMATION"))     return OPT_DEST_ADDR_NP_INFORMATION    ;
        if(stag.equals("DEST_ADDR_NP_COUNTRY"))         return OPT_DEST_ADDR_NP_COUNTRY        ;
        return -1;
        */
    }

    private byte[] createTLV(String funcname, String sparameter_tag, String svalue) throws Exception
    {
        return SMPPWorker.createTLV(funcname,sparameter_tag,svalue,true);
        /*
        int parameter_tag=-1;
        try
        {
            parameter_tag=Integer.decode(sparameter_tag);
        } catch (NumberFormatException nfe)
        {
            parameter_tag=getTAGID(sparameter_tag.toUpperCase().trim());
            if(parameter_tag==-1)
            {
                XTTProperties.printFail(funcname+": '"+sparameter_tag+"' not supported/found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                throw new IllegalArgumentException("'"+sparameter_tag+"' not supported/found");
            }
        }
        byte[] tag=ConvertLib.getByteArrayFromInt(parameter_tag,2);

        int maxlength=-1;
        int minlength=-1;

        byte[] value=null;

        String tag_name=null;

        switch(parameter_tag)
        {
            case OPT_DEST_ADDR_SUBUNIT:
                if(tag_name==null)tag_name="DEST_ADDR_SUBUNIT";
            case OPT_DEST_NETWORK_TYPE:
                if(tag_name==null)tag_name="DEST_NETWORK_TYPE";
            case OPT_DEST_BEARER_TYPE:
                if(tag_name==null)tag_name="DEST_BEARER_TYPE";
            case OPT_DEST_TELEMATICS_ID:
                if(tag_name==null)tag_name="DEST_TELEMATICS_ID";
            case OPT_SOURCE_ADDR_SUBUNIT:
                if(tag_name==null)tag_name="SOURCE_ADDR_SUBUNIT";
            case OPT_SOURCE_NETWORK_TYPE:
                if(tag_name==null)tag_name="SOURCE_NETWORK_TYPE";
            case OPT_SOURCE_BEARER_TYPE:
                if(tag_name==null)tag_name="SOURCE_BEARER_TYPE";
            case OPT_SOURCE_TELEMATICS_ID:
                if(tag_name==null)tag_name="SOURCE_TELEMATICS_ID";
            case OPT_QOS_TIME_TO_LIVE:
                if(tag_name==null)tag_name="QOS_TIME_TO_LIVE";
            case OPT_PAYLOAD_TYPE:
                if(tag_name==null)tag_name="PAYLOAD_TYPE";
            case OPT_MS_MSG_WAIT_FACILITIES:
                if(tag_name==null)tag_name="MS_MSG_WAIT_FACILITIES";
            case OPT_PRIVACY_INDICATOR:
                if(tag_name==null)tag_name="PRIVACY_INDICATOR";
            case OPT_USER_MESSAGE_REFERENCE:
                if(tag_name==null)tag_name="USER_MESSAGE_REFERENCE";
            case OPT_USER_RESPONSE_CODE:
                if(tag_name==null)tag_name="USER_RESPONSE_CODE";
            case OPT_SOURCE_PORT:
                if(tag_name==null)tag_name="SOURCE_PORT";
            case OPT_DESTINATION_PORT:
                if(tag_name==null)tag_name="DESTINATION_PORT";
            case OPT_SAR_MSG_REF_NUM:
                if(tag_name==null)tag_name="SAR_MSG_REF_NUM";
            case OPT_LANGUAGE_INDICATOR:
                if(tag_name==null)tag_name="LANGUAGE_INDICATOR";
            case OPT_SAR_TOTAL_SEGMENTS:
                if(tag_name==null)tag_name="SAR_TOTAL_SEGMENTS";
            case OPT_SAR_SEGMENT_SEQNUM:
                if(tag_name==null)tag_name="SAR_SEGMENT_SEQNUM";
            case OPT_SC_INTERFACE_VERSION:
                if(tag_name==null)tag_name="SC_INTERFACE_VERSION";
            case OPT_CALLBACK_NUM_PRES_IND:
                if(tag_name==null)tag_name="CALLBACK_NUM_PRES_IND";
            case OPT_NUMBER_OF_MESSAGES:
                if(tag_name==null)tag_name="NUMBER_OF_MESSAGES";
            case OPT_DPF_RESULT:
                if(tag_name==null)tag_name="DPF_RESULT";
            case OPT_SET_DPF:
                if(tag_name==null)tag_name="SET_DPF";
            case OPT_MS_AVAILABILITY_STATUS:
                if(tag_name==null)tag_name="MS_AVAILABILITY_STATUS";
            case OPT_DELIVERY_FAILURE_REASON:
                if(tag_name==null)tag_name="DELIVERY_FAILURE_REASON";
            case OPT_MORE_MESSAGES_TO_SEND:
                if(tag_name==null)tag_name="MORE_MESSAGES_TO_SEND";
            case OPT_MESSAGE_STATE:
                if(tag_name==null)tag_name="MESSAGE_STATE";
            case OPT_DISPLAY_TIME:
                if(tag_name==null)tag_name="DISPLAY_TIME";
            case OPT_SMS_SIGNAL:
                if(tag_name==null)tag_name="SMS_SIGNAL";
            case OPT_MS_VALIDITY:
                if(tag_name==null)tag_name="MS_VALIDITY";
            case OPT_ITS_REPLY_TYPE:
                if(tag_name==null)tag_name="ITS_REPLY_TYPE";
            case OPT_CONGESTION_STATE:
                if(tag_name==null)tag_name="CONGESTION_STATE";
            case OPT_BROADCAST_CHANNEL_INDICATOR:
                if(tag_name==null)tag_name="BROADCAST_CHANNEL_INDICATOR";
            case OPT_BROADCAST_MESSAGE_CLASS:
                if(tag_name==null)tag_name="BROADCAST_MESSAGE_CLASS";
            case OPT_BROADCAST_REP_NUM:
                if(tag_name==null)tag_name="BROADCAST_REP_NUM";
            case OPT_BROADCAST_ERROR_STATUS:
                if(tag_name==null)tag_name="BROADCAST_ERROR_STATUS";
            case OPT_BROADCAST_AREA_SUCCESS:
                if(tag_name==null)tag_name="BROADCAST_AREA_SUCCESS";
            case OPT_DEST_ADDR_NP_RESOLUTION:
                if(tag_name==null)tag_name="DEST_ADDR_NP_RESOLUTION";
            case OPT_DEST_ADDR_NP_COUNTRY:
                if(tag_name==null)tag_name="DEST_ADDR_NP_COUNTRY";
            case OPT_USSD_SERVICE_OP:
                if(tag_name==null)tag_name="USSD_SERVICE_OP";
            case OPT_ALERT_ON_MESSAGE_DELIVERY:
                if(tag_name==null)tag_name="ALERT_ON_MESSAGE_DELIVERY";
                try
                {
                    value=ConvertLib.getByteArrayFromInt(Integer.decode(svalue));
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            case OPT_SOURCE_SUBADDRESS:
                if(tag_name==null)tag_name="SOURCE_SUBADDRESS";
                if(maxlength==-1)maxlength=23;
                if(minlength==-1)minlength=2;
            case OPT_DEST_SUBADDRESS:
                if(tag_name==null)tag_name="DEST_SUBADDRESS";
                if(maxlength==-1)maxlength=23;
                if(minlength==-1)minlength=2;
            case OPT_CALLBACK_NUM_ATAG:
                if(tag_name==null)tag_name="CALLBACK_NUM_ATAG";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=0;
            case OPT_CALLBACK_NUM:
                if(tag_name==null)tag_name="CALLBACK_NUM";
                if(maxlength==-1)maxlength=19;
                if(minlength==-1)minlength=4;
            case OPT_NETWORK_ERROR_CODE:
                if(tag_name==null)tag_name="NETWORK_ERROR_CODE";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_MESSAGE_PAYLOAD:
                if(tag_name==null)tag_name="MESSAGE_PAYLOAD";
                if(maxlength==-1)maxlength=Integer.MAX_VALUE;
                if(minlength==-1)minlength=0;
            case OPT_ITS_SESSION_INFO:
                if(tag_name==null)tag_name="ITS_SESSION_INFO";
                if(maxlength==-1)maxlength=2;
                if(minlength==-1)minlength=2;
            case OPT_BROADCAST_CONTENT_TYPE:
                if(tag_name==null)tag_name="BROADCAST_CONTENT_TYPE";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_BROADCAST_CONTENT_TYPE_INFO:
                if(tag_name==null)tag_name="BROADCAST_CONTENT_TYPE_INFO";
                if(maxlength==-1)maxlength=255;
                if(minlength==-1)minlength=1;
            case OPT_BROADCAST_FREQUENCY_INTERVAL:
                if(tag_name==null)tag_name="BROADCAST_FREQUENCY_INTERVAL";
                if(maxlength==-1)maxlength=3;
                if(minlength==-1)minlength=3;
            case OPT_BROADCAST_AREA_IDENTIFIER:
                if(tag_name==null)tag_name="BROADCAST_AREA_IDENTIFIER";
                if(maxlength==-1)maxlength=101;
                if(minlength==-1)minlength=0;
            case OPT_BROADCAST_SERVICE_GROUP:
                if(tag_name==null)tag_name="BROADCAST_SERVICE_GROUP";
                if(maxlength==-1)maxlength=255;
                if(minlength==-1)minlength=1;
            case OPT_BILLING_IDENTIFICATION:
                if(tag_name==null)tag_name="BILLING_IDENTIFICATION";
                if(maxlength==-1)maxlength=1024;
                if(minlength==-1)minlength=1;
            case OPT_SOURCE_NODE_ID:
                if(tag_name==null)tag_name="SOURCE_NODE_ID";
                if(maxlength==-1)maxlength=6;
                if(minlength==-1)minlength=6;
            case OPT_DEST_NODE_ID:
                if(tag_name==null)tag_name="DEST_NODE_ID";
                if(maxlength==-1)maxlength=6;
                if(minlength==-1)minlength=6;
            case OPT_DEST_ADDR_NP_INFORMATION:
                if(tag_name==null)tag_name="DEST_ADDR_NP_INFORMATION";
                if(maxlength==-1)maxlength=10;
                if(minlength==-1)minlength=10;
                try
                {

                    value=ConvertLib.getBytesFromByteString(svalue);
                    if(value.length>maxlength||value.length<minlength)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" length "+value.length+" not between "+minlength+" and "+maxlength);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException("length "+value.length+" not between "+minlength+" and "+maxlength);
                    }
                } catch (IllegalArgumentException iex)
                {
                    throw iex;
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            case OPT_ADDITIONAL_STATUS_INFO_TEXT:
                if(tag_name==null)tag_name="ADDITIONAL_STATUS_INFO_TEXT";
                if(maxlength==-1)maxlength=256;
                if(minlength==-1)minlength=1;
            case OPT_RECEIPTED_MESSAGE_ID:
                if(tag_name==null)tag_name="RECEIPTED_MESSAGE_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=1;
            case OPT_BROADCAST_END_TIME:
                if(tag_name==null)tag_name="BROADCAST_END_TIME";
                if(maxlength==-1)maxlength=16;
                if(minlength==-1)minlength=16;
            case OPT_SOURCE_NETWORK_ID:
                if(tag_name==null)tag_name="SOURCE_NETWORK_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=7;
            case OPT_DEST_NETWORK_ID:
                if(tag_name==null)tag_name="DEST_NETWORK_ID";
                if(maxlength==-1)maxlength=65;
                if(minlength==-1)minlength=7;
                try
                {

                    value=ConvertLib.getBytesFromByteString(svalue);
                    if(value.length>maxlength||value.length<minlength)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" length "+value.length+" not between "+minlength+" and "+maxlength);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException("length "+value.length+" not between "+minlength+" and "+maxlength);
                    }
                    if(value[value.length-1]!=0x00)
                    {
                        XTTProperties.printFail(funcname+": error "+tag_name+" musst be 0x00 terminated");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        throw new IllegalArgumentException(tag_name+" musst be 0x00 terminated");
                    }
                } catch (IllegalArgumentException iex)
                {
                    throw iex;
                } catch (Exception ex)
                {
                    //XTTProperties.printFail(funcname+": error creating "+tag_name+" from '"+svalue+"'");
                    //XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    throw new Exception("error creating "+tag_name+" from '"+svalue+"'",ex);
                }
                break;
            default:
                XTTProperties.printFail(funcname+": Unknown tag value "+parameter_tag);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                throw new IllegalArgumentException("Unknown tag value "+parameter_tag);
        }
        byte[] length=ConvertLib.getByteArrayFromInt(value.length,2);
        byte[] returnvalue=new byte[tag.length+length.length+value.length];
        int pointer=0;
        pointer=addBytesToArray(returnvalue,pointer,tag   );
        pointer=addBytesToArray(returnvalue,pointer,length);
        pointer=addBytesToArray(returnvalue,pointer,value );
        XTTProperties.printInfo(funcname+": created TLV->"+tag_name);
        return returnvalue;
        */
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SMPP.java,v 1.12 2010/12/13 06:59:09 rajesh Exp $";
}