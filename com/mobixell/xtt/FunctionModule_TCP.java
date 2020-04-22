package com.mobixell.xtt;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Vector;
import java.util.LinkedHashMap;

/**
 * FunctionModule_TCP.
 * <p>
 * Functions for sending TCP packets.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.20 $
 */
public class FunctionModule_TCP extends FunctionModule
{
    private Socket s = null;
    private byte[] response = new byte[0];
    private int soTimeout=30000;
    private int synchronizeDelay=2000;

    public FunctionModule_TCP()
    {
    }

    public String getConfigurationOptions()
    {
        return "    <!-- function module TCP -->"
            +"\n    <TCP>"
            +"\n        <!-- socket timeout on tcp connections -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <synchronize>"
            +"\n            <!-- when synchronizeNotify can't connect it will wait this amount of miliseconds before retry -->"
            +"\n            <delay>2000</delay>"
            +"\n            <!-- the wait/notify port to use for synchronization -->"
            +"\n            <!-- note: this value is not used by code, it is here for convenience -->"
            +"\n            <Port>6969</Port>"
            +"\n        </synchronize>"
            +"\n    </TCP>";
    }

    public int startTCPProxy(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startTCPProxy: listenPort endIp endPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0]+": startTCPProxy: listenPort endIp endPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                InetAddress endIp = DNSServer.resolveAddressToInetAddress(parameters[2]);
                int endPort = Integer.decode(parameters[3]);
                int localPort = Integer.decode(parameters[1]);
                TCPProxy s = new TCPProxy(localPort, new InetSocketAddress(endIp, endPort), 60000);
                Thread ws=(new Thread(s, "TCPProxy"));
                ws.start();
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": Error");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int stopTCPProxy(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopTCPProxy:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                TCPProxy.closeSockets();
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": Error");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return status;
    }
    /**
     * sets the action to be taken by the TCP Proxy.
     * <p>
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is the port of the TCP-Proxy to add the action to.
     *                      <br><code>parameters[2]</code> is the number of bytes to read before performing any action.
     *                      <br><code>parameters[3]</code> is the number of bytes to read before an action after the first wait (repeating).
     *                      <br><code>parameters[4]</code> is the action to perform:
     *                      <br>    DELAY: 1              -> delay in ms
     *                      <br>    RANDOMBYTES: 2        -> replaces bytes with any bytes, good for binary protocols, amount of bytes to replace
     *                      <br>    RANDOMALPHANUMERIC: 3 -> replaces bytes with 0-9A-Z, usefull when having text based protocol, , amount of bytes to replace
     *                      <br><code>parameters[5]</code> is the value the action uses (e.g. ms to delay, number of bytes to replace etc.)
     *                      <br><code>parameters[6]</code> is which stream to perform the action on, <0 means only on incoming stream, >0 means only on outgoing stream, 0 means on both.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setTCPProxyAction(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setTCPProxyAction: port firstwait wait action actionvalue whichside");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length != 7)
        {
            XTTProperties.printFail(parameters[0]+": setTCPProxyAction: port firstwait wait action actionvalue whichside");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            int action=0;
            try
            {
                action=TCPProxyWorker.getAction(parameters[4]);
            } catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": "+nfe.getMessage());
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            int sideno=0;
            try
            {
                sideno=TCPProxyWorker.getSideNumber(parameters[6]);
            } catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": "+nfe.getMessage());
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            try
            {
                TCPProxy.setProxyAction(parameters[1],Integer.decode(parameters[2]),Integer.decode(parameters[3]),action,Integer.decode(parameters[5]),sideno);
            } catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": One argument wasn't a number");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int openConnection(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": openConnection: ip port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+": openConnection: ip port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            int port;
            String IP;

            IP = parameters[1];

            try
            {
                port = Integer.parseInt(parameters[2]);
            }
            catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": openConnection: can't set port, it isn't a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
            try
            {
                s = new Socket(IP,port);
                s.setSoTimeout(soTimeout);
                s.setTcpNoDelay(true);
                XTTProperties.printInfo(parameters[0]+": openConnection: Connection opened to " + IP + ":" + port);
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": openConnection: error getting port");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                    return XTTProperties.FAILED_UNKNOWN;
                }
            }
        }
        return status;
    }

    public int closeConnection(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": closeConnection:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {

            if((s == null) || (!s.isConnected()))
            {
                XTTProperties.printFail(parameters[0]+": closeConnection: no previous connection to close");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            else
            {
                try
                {
                    s.close();
                    XTTProperties.printInfo(parameters[0]+": closeConnection: Connection closed");
                }
                catch(java.io.IOException ioe)
                {
                    XTTProperties.printFail(parameters[0] + ": Error closing socket");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ioe);
                    }
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            }
        }
        return status;
    }

    public int sendPacketFromFile(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPacketFromFile: filename ;(must pre-connect)");
            XTTProperties.printFail(this.getClass().getName()+": sendPacketFromFile: ip port filename");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length < 2||parameters.length==3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+": sendPacket: filename ;(must pre-connect)");
            XTTProperties.printFail(parameters[0]+": sendPacket: ip port filename");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            int port=-1;
            String IP="";
            String fileName = XTTProperties.getCurrentTestPath();

            if (parameters.length > 2)
            {
                IP = parameters[1];
                fileName += parameters[3];
                try
                {
                    port = Integer.parseInt(parameters[2]);
                }
                catch(java.lang.NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+": sendPacket: can't set port, it isn't a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                }
            }
            else
            {
                fileName += parameters[1];
                if((s == null) || (!s.isConnected()))
                {
                    XTTProperties.printFail(parameters[0]+": sendPacket: no previous connection and no IP/Port were provided");
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    XTTProperties.printInfo(parameters[0]+": sendPacket: Using previously opened connection");
                }
            }

            //Parse the string into a byte array
            //packet = string2Bytes(parameters[parameters.length-1]);

            BufferedInputStream in = null;
            int fileLength=-1;
            byte[] fileContent = new byte[0];

            try
            {
                in = new BufferedInputStream(new FileInputStream(fileName));
                fileLength = (int)(new File(fileName).length());
                fileContent = new byte[fileLength];

                HTTPHelper.readBytes(in,fileContent, 0, fileLength);
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": sendPacket: error getting file " + fileName);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }

            sendPacket(IP,port,fileContent);
        }
        return status;
    }

    public int sendPacket(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPacket: byte[,byte,...] (must pre-connect)");
            XTTProperties.printFail(this.getClass().getName()+": sendPacket: ip port byte[,byte,...]");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length < 2)
        {
            XTTProperties.printFail(parameters[0]+": sendPacket: byte,byte,... (must pre-connect)");
            XTTProperties.printFail(parameters[0]+": sendPacket: ip port byte,byte,...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            byte[] packet;
            int port=-1;
            String IP="";

            if (parameters.length > 2)
            {
                IP = parameters[1];

                try
                {
                    port = Integer.parseInt(parameters[2]);
                }
                catch(java.lang.NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+": sendPacket: can't set port, it isn't a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return XTTProperties.FAILED;
                }
            }
            else
            {
                if((s == null) || (!s.isConnected()))
                {
                    XTTProperties.printFail(parameters[0]+": sendPacket: no previous connection and no IP/Port were provided");
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
                else
                {
                    XTTProperties.printInfo(parameters[0]+": sendPacket: Using previously opened connection");
                }
            }

            //Parse the string into a byte array
            packet = string2Bytes(parameters[parameters.length-1]);

            sendPacket(IP,port,packet);
        }
        return status;
    }

    private int sendPacket (String address, int port, byte[] data)
    {
    	int status = XTTProperties.PASSED;
        if(port>0)
        {
            try
            {
                s = new Socket(address,port);
                s.setSoTimeout(soTimeout);
                s.setTcpNoDelay(true);
                XTTProperties.printInfo("sendPacket: Connection opened to " + address + ":" + port);
            }
            catch(Exception e)
            {
                XTTProperties.printFail("sendPacket: error getting port");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }

        XTTProperties.printDebug("TCP: TCP Output message:\n"+ConvertLib.getHexView(data,0,XTTProperties.getIntProperty("Bufferoutputsize")));

        //Send the byte array
        try
        {

            BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(s.getInputStream());
            out.write(data,0,data.length);
            out.flush();

            //Receive a response
            byte buffer[]=new byte[1024];
            ArrayList<ByteArrayWrapper> bufferSet=new ArrayList<ByteArrayWrapper>();
            int thebyte=0;
            int byteCounter=-1;
            try
            {
                while(thebyte!=-1)
                {
                    byteCounter++;
                    if((byteCounter%1024)==0)
                    {
                        XTTProperties.printDebug("sendPacket: Reading 1024 Bytes:");
                        buffer=new byte[1024];
                        bufferSet.add(new ByteArrayWrapper(buffer));
                    }
                    thebyte=in.read();

                    //After you start getting the first byte you don't have to wait as long.
                    s.setSoTimeout(soTimeout/10);

                    buffer[byteCounter%1024]=(new Integer(thebyte)).byteValue();
                }
            }
            //Use the timout to break out of receiving, since you don't know when it really ends
            catch(java.net.SocketTimeoutException ste)
            {
                //System.out.println("Status: " + s.isConnected());
                //byteCounter--;
            }
            response=new byte[byteCounter];
            Object theBytes[]=bufferSet.toArray();
            int end=1024;
            int count=0;

            for(int k=0;k<theBytes.length;k++)
            {
                if(k==(theBytes.length-1))end=byteCounter%1024;
                for(int j=0;j<end;j++)
                {
                    response[count++]=((ByteArrayWrapper)theBytes[k]).getArray()[j];

                }
            }

            XTTProperties.printDebug("TCP: TCP Input message:\n"+ConvertLib.getHexView(response,0,XTTProperties.getIntProperty("Bufferoutputsize")));

        }
        catch(Exception e)
        {
            XTTProperties.printFail("sendPacket: Error sending packet");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return XTTProperties.FAILED;
        }
        finally
        {
            if(port>0)
            {
                try
                {
                    s.close();
                    s = null;
                }
                catch(Exception e)
                {
                    XTTProperties.printFail("sendPacket: Error closing socket");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return XTTProperties.FAILED;
                }
            }
            else
            {
                XTTProperties.printWarn("sendPacket: Done, but keeping socket open");
            }
        }
        return status;
    }

    public int sendTextPacket(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendTextPacket: text; (must pre-connect)");
            XTTProperties.printFail(this.getClass().getName()+": sendTextPacket: ip port text");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if (parameters.length < 2)
        {
            XTTProperties.printFail(parameters[0]+": text; (must pre-connect)");
            XTTProperties.printFail(parameters[0]+": ip port text");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            sendTextPacket(parameters,true);
        }
        return status;
    }

    public int sendTextPacketPart(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendTextPacketPart: text; (must pre-connect)");
            XTTProperties.printFail(this.getClass().getName()+": sendTextPacketPart: ip port text");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if (parameters.length < 2)
        {
            XTTProperties.printFail(parameters[0]+": text; (must pre-connect)");
            XTTProperties.printFail(parameters[0]+": ip port text");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            sendTextPacket(parameters,false);
        }
        return status;
    }

    private int sendTextPacket(String parameters[], boolean doRead)
    {
        int port;
        String IP;
        boolean alreadyStarted = false; //To be safe, lets assume we want to close the port at the end
        int status = XTTProperties.PASSED;
        int start=1;
        if (parameters.length > 2)
        {
            start=3;
            IP = parameters[1];

            try
            {
                port = Integer.parseInt(parameters[2]);
                if(port<1||port>65535)
                {
                XTTProperties.printFail(parameters[0]+": can't set port, out of range: "+port);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                }
            } catch(java.lang.NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": can't set port, it isn't a number: "+parameters[2]);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
            try
            {
                alreadyStarted = false;
                s = new Socket(IP,port);
                s.setSoTimeout(soTimeout);
                s.setTcpNoDelay(true);
                XTTProperties.printInfo(parameters[0]+": Connection opened to " + IP + ":" + port);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": error getting port");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }

        } else
        {
            if((s == null) || (!s.isConnected()))
            {
                alreadyStarted = false;
                XTTProperties.printFail(parameters[0]+": no previous connection and no IP/Port were provided");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            else
            {
                alreadyStarted = true;
                XTTProperties.printInfo(parameters[0]+": Using previously opened connection");
            }
        }


        //Send the lines
        try
        {
            if(parameters[start].length()>0)
            {
                sendPacketTXT(parameters[0],s,parameters[start]);
            }

            if(doRead)
            {
                response=readResponseTXT(parameters[0],s);
            }
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": Error sending/reading TCP packet");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return XTTProperties.FAILED;
        } finally
        {
            if(!alreadyStarted)
            {
                try
                {
                    s.close();
                    s = null;
                } catch(Exception e)
                {
                    XTTProperties.printFail(parameters[0] + ": Error closing socket");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return XTTProperties.FAILED;
                }
            } else
            {
                XTTProperties.printInfo(parameters[0] + ": Done, keeping socket open");
            }
        }
        return status;
    }

    private int sendPacketTXT(String function, Socket s, String packet) throws Exception
    {
    	int status = XTTProperties.PASSED;
    	
        java.io.BufferedReader lines=new java.io.BufferedReader(new java.io.StringReader(packet));
        String line="";
        StringBuffer outPutB=new StringBuffer();
        while((line=lines.readLine())!=null)
        {
            outPutB.append(line+"\r\n");
        }

        String outPut=outPutB.toString();

        BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
        if(packet.length()>0)
        {
            int endBytes=2;
            if(packet.endsWith("\n"))endBytes=0;
            out.write(ConvertLib.createBytes(outPut.substring(0,outPut.length()-endBytes)));
            out.flush();
            XTTProperties.printDebug(function + ": sent data:\n"+outPut.substring(0,outPut.length()-endBytes)+"\nEOF sent data");
        }
        return status;
    }

    private byte[] readResponseTXT(String function, Socket s) throws Exception
    {
        byte[] response=null;
        //Receive a response
        BufferedInputStream in = new BufferedInputStream(s.getInputStream(),65536);
        try
        {
            XTTProperties.printDebug(function + ": reading response:");
            response=HTTPHelper.readStream(function,in,soTimeout/20);
        }
        //Use the timout to break out of receiving, since you don't know when it really ends
        catch(java.net.SocketTimeoutException ste)
        {
            //System.out.println("Status: " + s.isConnected());
            //byteCounter--;
        }
        XTTProperties.printDebug(function+": TCP Input message:\n"+ConvertLib.getHexView(response,0,XTTProperties.getIntProperty("Bufferoutputsize")));
        return response;
    }

    public int checkResponse(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponse: stringToMatch");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length < 2)
        {
            XTTProperties.printFail(parameters[0]+": checkResponse: stringToMatch");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
        }
        else if(response.length > 0)
        {
            byte[] key = string2Bytes(parameters[1]);

            if (key.length > response.length)
            {
                XTTProperties.printFail(parameters[0]+": checkResponse: Response is shorter than query, can't match");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            int j=0; //which key byte are we checking on
            //int k=-1; //last index we found a match at
            for (int i=0;i<response.length;i++)
            {
                if(response[i] == key[j])
                {
                    //System.out.println("Found " + key[j] + " at " + i);
                    j++;
                    if(j == key.length)
                    {
                        XTTProperties.printInfo(parameters[0]+": checkResponse: Requested data was found in the response");
                        return status;
                    }
                }
                else
                {
                    j = 0;
                }
            }
            XTTProperties.printFail(parameters[0]+": checkResponse: Couldn't find the requested data in the response");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        else
        {
            XTTProperties.printFail(parameters[0]+": checkResponse: Nothing to check against");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    public int queryTextResponse(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryTextResponse: variableName regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
        } else if(response.length > 0)
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            
            boolean bStatus = ConvertLib.queryString(parameters[0],ConvertLib.createString(response),parameters[2],parameters[1]);
			if (!bStatus) 
				status = XTTProperties.FAILED;
        } else
        {
            XTTProperties.printFail(parameters[0]+": queryTextResponse: Nothing to check against");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    public int queryTextResponseNegative(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryTextResponse: regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[1]+"'");
            
            boolean bStatus = ConvertLib.queryStringNegative(parameters[0],ConvertLib.createString(response),parameters[1]);
			if (!bStatus) 
				status = XTTProperties.FAILED;
        }
        return status;
    }

    private byte[] string2Bytes(String byteString)
    {
        return ConvertLib.getByteArrayFromHexString(byteString);
    }

    public int sendRandomPackets(String parameters[])
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendRandomPackets: ip port numPackets packetSize");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if (parameters.length != 5)
        {
            XTTProperties.printFail(parameters[0]+": ip port numPackets packetSize");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            byte[] packet;
            int port;
            int numPackets=1;
            try
            {
                numPackets=(Integer.decode(parameters[3])).intValue();
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[3]+"' is NOT a correct number");
                status= XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
            String IP;
            Socket sock=null;

            for(int forPacket=0;forPacket<numPackets;forPacket++)
            {
                XTTProperties.printInfo(parameters[0]+"("+forPacket+"): sending Packets");
                IP = parameters[1];

                try
                {
                    port = (Integer.decode(parameters[2])).intValue();
                }
                catch(java.lang.NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+"("+forPacket+"): can't set port, it isn't a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                }
                try
                {
                    XTTProperties.printInfo(parameters[0]+"("+forPacket+"): opening Connection to " + IP + ":" + port);
                    sock = new Socket(IP,port);
                    sock.setSoTimeout(soTimeout);
                    sock.setTcpNoDelay(true);
                } catch(Exception e)
                {
                    XTTProperties.printFail(parameters[0]+"("+forPacket+"): error getting port");
                    status= XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                    return XTTProperties.FAILED;
                }

                try
                {
                    //Parse the string into a byte array
                    packet = RandomLib.getRandomBytes((Integer.decode(parameters[4])).intValue());
                }
                catch(java.lang.NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+"("+forPacket+"): '"+parameters[4]+"' is NOT a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                }

                XTTProperties.printDebug(""+ConvertLib.getHexView(packet,0,XTTProperties.getIntProperty("Bufferoutputsize")));

                //Send the byte array
                try
                {
                    BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());
                    BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                    out.write(packet,0,packet.length);
                    out.flush();

                    //Receive a response
                    byte buffer[]=new byte[1024];
                    ArrayList<ByteArrayWrapper> bufferSet=new ArrayList<ByteArrayWrapper>();
                    int thebyte=0;
                    int byteCounter=-1;
                    try
                    {
                        while(thebyte!=-1)
                        {
                            byteCounter++;
                            if((byteCounter%1024)==0)
                            {
                                XTTProperties.printDebug(parameters[0] + ": Reading 1024 Bytes:");
                                buffer=new byte[1024];
                                bufferSet.add(new ByteArrayWrapper(buffer));
                            }
                            thebyte=in.read();

                            //After you start getting the first byte you don't have to wait as long.
                            sock.setSoTimeout(soTimeout/10);

                            buffer[byteCounter%1024]=(new Integer(thebyte)).byteValue();
                        }
                    }
                    //Use the timout to break out of receiving, since you don't know when it really ends
                    catch(java.net.SocketTimeoutException ste)
                    {
                        //System.out.println("Status: " + s.isConnected());
                        //byteCounter--;
                    }
                    response=new byte[byteCounter];
                    Object theBytes[]=bufferSet.toArray();
                    int end=1024;
                    int count=0;

                    for(int k=0;k<theBytes.length;k++)
                    {
                        if(k==(theBytes.length-1))end=byteCounter%1024;
                        for(int j=0;j<end;j++)
                        {
                            response[count++]=((ByteArrayWrapper)theBytes[k]).getArray()[j];

                        }
                    }

                    XTTProperties.printDebug("TCP: TCP Input message:\n"+ConvertLib.getHexView(response,0,XTTProperties.getIntProperty("Bufferoutputsize")));

                    try
                    {
                        sock = null;
                    }
                    catch(Exception e)
                    {
                        XTTProperties.printFail(parameters[0] + "("+forPacket+"): Error closing socket");
                        if(XTTProperties.printDebug(null))
                        {
                            XTTProperties.printException(e);
                        }
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return XTTProperties.FAILED;
                    }
                } catch(Exception e)
                {
                    XTTProperties.printFail(parameters[0] + "("+forPacket+"): Error sending packet");
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return XTTProperties.FAILED;
                }
            }
        }
        return status;
    }

    public int responseToVariable(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": responseToVariable: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length==2)
        {
            XTTProperties.setVariable(parameters[1],ConvertLib.createString(response));
            XTTProperties.printInfo(parameters[0]+": stored response in variable='"+parameters[1]+"'");
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }
    public int responseToBase64Variable(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": responseToBase64Variable: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length==2)
        {
            XTTProperties.setVariable(parameters[1],ConvertLib.base64Encode(response));
            XTTProperties.printInfo(parameters[0]+": stored response in base64 variable='"+parameters[1]+"'");
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }

   /**
     * Waits forever for a synchronizeNotify request on the specified port. Transmit the current test status to the notify client and recieves
     * the status of the notify client. Should the received status be any failed status the test status will be modified accordingly.
     * <p>
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is the port on which to wait.
     *                      <br><code>parameters[2]</code> is any data that can be sent back AS-IS (no encoding is done) to the notify client.
     *                      <br><code>parameters[3]</code> is the variable under which to store the data received by notify AS-IS (no encoding is done).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int synchronizeWait(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": synchronizeWait: port dataAsIs variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        ServerSocket servsock = null;
        Socket sock=null;
        if(parameters.length==4)
        {
            int port=0;
            try
            {
                port=Integer.decode(parameters[1]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
                
            try
            {
                servsock = new ServerSocket(port);
            }
            catch (java.io.IOException e)
            {
                XTTProperties.printFail(parameters[0]+": error establishing listening port="+port);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            boolean finished=false;
            try
            {
                while (!finished)
                {
                    try
                    {
                        XTTProperties.printInfo(parameters[0]+": Waiting for notify on port "+port);
                        sock=servsock.accept();
                        PrintStream out = new PrintStream( sock.getOutputStream() );
                        BufferedInputStream in = new BufferedInputStream(sock.getInputStream(),65536);
            
                        readSynchronizeData(parameters[0],parameters[3],in);      
        
                        XTTProperties.printVerbose(parameters[0]+":" +" Sending stream data.");
                        writeSynchronizeData("HTTP/1.0 200 Ok",ConvertLib.createBytes(parameters[2]),out);
                        finished=true;
                    }
                    catch (java.io.IOException e)
                    {
                        XTTProperties.printWarn(parameters[0]+": Error with connection: " + e);
                    }
                    catch (Exception e)
                    {
                        XTTProperties.printWarn(parameters[0]+": Error: " + e);
                        e.printStackTrace();
                    } finally
                    {
                        try{sock.close();}catch(Exception e){};
                    }
                }
            } finally
            {
                try{servsock.close();}catch(Exception e){};
            }
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port dataAsIs variableForBody");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }

    private void writeSynchronizeData(String firstLine,byte[] dataToSend, PrintStream out) throws Exception
    {
        String CRLF="\r\n";
        StringBuffer headers=new StringBuffer();
        headers.append(firstLine+CRLF);
        headers.append("date: "+HTTPHelper.createHTTPDate()+CRLF);
        headers.append("connection: close"+CRLF);
        headers.append("xtt-status-code: "+XTTProperties.getCurrentTestStatusCode()+CRLF);
        headers.append("xtt-status-text: "+XTTProperties.getCurrentTestStatus()+CRLF);
        headers.append("server: XTT/"+XTTProperties.getXTTBuildVersion()
            +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
            +System.getProperties().getProperty("os.name")+" "
            +System.getProperties().getProperty("os.arch")+" "
            +System.getProperties().getProperty("os.version")+"; "
            +System.getProperties().getProperty("user.name")+"; "
            +"SynchronizeXTT"+"; "
            +"$Revision: 1.20 $"
            +")"+CRLF);
        headers.append("content-type: application/octet-stream"+CRLF);
        headers.append("content-length: "+dataToSend.length+CRLF);
        headers.append(CRLF);
        
        byte[] headerToSend=ConvertLib.createBytes(headers.toString());
        out.write(headerToSend);
        out.write(dataToSend);
        out.flush();
    }    

   /**
     * Tries forever to send a notify request to the specified ip and port. Transmit the current test status to the wait client and recieves
     * the status of the wait client. Should the received status be any failed status the test status will be modified accordingly.
     * <p>
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is the ip to which to send.
     *                      <br><code>parameters[2]</code> is the port to which to send.
     *                      <br><code>parameters[3]</code> is any data that can be sent AS-IS (no encoding is done) to the wait client.
     *                      <br><code>parameters[4]</code> is the variable under which to store the data received from the wait AS-IS (no encoding is done).
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int synchronizeNotify(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": synchronizeNotify: ip port dataAsIs variableForBody");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        Socket sock=null;
        if(parameters.length==5)
        {
            int port=0;
            try
            {
                port=Integer.decode(parameters[2]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
            InetAddress remoteIP=null;
            try
            {
                remoteIP = DNSServer.resolveAddressToInetAddress(parameters[1]);
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0]+": unknown host "+parameters[1]);
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
            
            boolean finished=false;
            XTTProperties.printInfo(parameters[0]+": Attempting notify to "+remoteIP+":"+port);
            while (!finished)
            {
                try
                {
                    sock = new Socket(remoteIP,port);
                    sock.setKeepAlive(true);
                    PrintStream out = new PrintStream( sock.getOutputStream() );
                    BufferedInputStream in = new BufferedInputStream(sock.getInputStream(),65536);

                    XTTProperties.printVerbose(parameters[0]+":" +" Sending stream data.");
                    writeSynchronizeData("POST /synchronizeXTT HTTP/1.0",ConvertLib.createBytes(parameters[3]),out);

                    readSynchronizeData(parameters[0],parameters[4],in);      
                    finished=true;
                }
                catch (java.io.IOException e)
                {
                    XTTProperties.printWarn(parameters[0]+": Error with connection: "+remoteIP+":"+port+" "+ e);
                    try
                    {
                        Thread.sleep(synchronizeDelay);
                    } catch (Exception ex)
                    {}
                }
                catch (Exception e)
                {
                    XTTProperties.printWarn(parameters[0]+": Error: " + e);
                    e.printStackTrace();
                } finally
                {
                    try{sock.close();}catch(Exception e){};
                }
            }
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port dataAsIs variableForBody");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }
    
    private void readSynchronizeData(String functionName,String variableName, BufferedInputStream in) throws Exception
    {
        XTTProperties.printVerbose(functionName+":" +" Starting stream read.");
        LinkedHashMap<String,Vector<String>> header=HTTPHelper.readHTTPStreamHeaders(functionName,in);
        //End fo stream so break or invaild
        if(header.get(null)==null) throw new IOException("Invalid Protocol, no Method found");    
        byte[] content=null;
        if(header.get("transfer-encoding")!=null)
        {
            if(header.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
            {
                XTTProperties.printDebug(functionName+": chunked body found, unchunking");
                content=HTTPHelper.readChunkedBody(functionName+"",in,header);
            }
        } else
        {
            Vector<String> contentlengthVector=header.get("content-length");
            if(contentlengthVector!=null)
            {
                int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                content=new byte[contentlength];
                HTTPHelper.readBytes(in,content);
                XTTProperties.printDebug(functionName+": read additional "+contentlength+" bytes as body");
            } else
            {
                throw new IOException("Invalid Protocol, content-length not found");    
            }
        }

        XTTProperties.setVariable(variableName,ConvertLib.createString(content));
        
        int xtt_status_code=Integer.decode(header.get("xtt-status-code").get(0));
        String xtt_status_text=header.get("xtt-status-text").get(0);

        XTTProperties.printDebug(functionName+": XTT code="+xtt_status_code+"/"+xtt_status_text+" Body:\n"+ConvertLib.getHexView(content)+" stored in "+variableName);
        if(xtt_status_code>=XTTProperties.FAILED)
        {
            XTTProperties.printFail(functionName+": Remote reports test status: "+xtt_status_text);
            XTTProperties.setTestStatus(xtt_status_code);
        } else
        {
            XTTProperties.printInfo(functionName+": Remote reports test status: NOT FAILED");
        }
    }


    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        soTimeout=XTTProperties.getIntProperty("TCP/TIMEOUT");
        if(soTimeout<0)soTimeout=30000;
        synchronizeDelay=XTTProperties.getIntProperty("TCP/SYNCHRONIZE/DELAY");
        if(synchronizeDelay<0)synchronizeDelay=2000;
        response = new byte[0];
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_TCP.java,v 1.20 2010/05/05 08:12:37 rajesh Exp $";
}