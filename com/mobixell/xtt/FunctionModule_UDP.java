package com.mobixell.xtt;

import java.net.DatagramPacket;

/**
 * FunctionModule_UDP.
 * <p>
 * Functions for sending UDP packets.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.13 $
 */
public class FunctionModule_UDP extends FunctionModule
{
    //private byte[] response = new byte[0];

	/**
	 * Starts the UDP server on specified port in parameter.
	 */
   	public int startUDPServer(String parameters[])
    {
   		int status = XTTProperties.PASSED;
   		
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startUDPServer: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startUDPServer: LocalPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {   UDPServer socket = null;
            try
            {
                if(parameters.length != 2)
                {
                    //Start the UDP server.
                	socket = new UDPServer(XTTProperties.getIntProperty("UDP/PORT"));
                }
                else
                {
                	//Start the UDP server on specified port.
                	socket = new UDPServer(Integer.parseInt(parameters[1]));
                }
            }
            catch (java.net.SocketException se)
            {
            	status=XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(se);
                }
            }
            socket.start();
        }
        return status;
    }

    /**
     * Stop UDP Server on specified port..
     * @param parameters
     */
   	public int stopUDPServer(String parameters[])
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopUDPServer: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopUDPServer: LocalPort");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length != 1) && (parameters.length != 2))
        {
            XTTProperties.printFail(parameters[0]+": "+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+": LocalPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }        
        else
        {
            try
            {
                int port = -1;
                if(parameters.length == 2)
                {
                    port = Integer.parseInt(parameters[1]);
                    //Stop the server
                    UDPServer.stopGracefully(port);
                }
                else
                {
                    //Stop the server.
                	UDPServer.stopAllGracefully();
                }
            }
            catch (Exception e)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
        return status;
    }

    /**
     * Send packet in base 64 format.
     * @param parameters
     */
   	public int sendPacketFromBase64(String parameters[])
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPacketFromBase64: ip port base64");
            XTTProperties.printFail(this.getClass().getName()+": sendPacketFromBase64: localport ip remoteport base64");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length != 4) && (parameters.length != 5))
        {
            XTTProperties.printFail(parameters[0]+": ip port base64");
            XTTProperties.printFail(parameters[0]+": localport ip remoteport base64");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                byte[] data = new byte[0];
                if(parameters.length==5)
                {
                    //Convert the data in base64 format.
                	data = ConvertLib.base64Decode(parameters[4]);
                    //Create the packet.
                	DatagramPacket packet = new DatagramPacket(data,data.length);
                    //Set the address.
                	packet.setAddress(DNSServer.resolveAddressToInetAddress(parameters[2]));
                    //set the port.
                	packet.setPort(Integer.parseInt(parameters[3]));
                    
                    XTTProperties.printInfo(parameters[0] + ": Sending data to: " + parameters[1]);
                    //Send the packet.
                    UDPServer.send(packet,Integer.parseInt(parameters[1]));
                }
                else if(parameters.length==4)
                {
                	//Convert the data in base64 format.
                	data = ConvertLib.base64Decode(parameters[3]);
                	//Create the packet.
                    DatagramPacket packet = new DatagramPacket(data,data.length); 
                    //Set the address.
                    packet.setAddress(DNSServer.resolveAddressToInetAddress(parameters[1]));
                    //set the port.
                    packet.setPort(Integer.parseInt(parameters[2]));
                    
                    XTTProperties.printInfo(parameters[0] + ": Sending data to: " +XTTProperties.getIntProperty("UDP/PORT"));
                    //Send the packet.
                    UDPServer.send(packet,XTTProperties.getIntProperty("UDP/PORT"));  
                }
            }
            catch (Exception e)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
        return status;
    }

    /**
     * Send the packet.
     * @param parameters
     */
   	public int sendPacket(String parameters[])
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPacket: ip port hexString");
            XTTProperties.printFail(this.getClass().getName()+": sendPacket: localport ip remoteport hexString");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length != 4) && (parameters.length != 5))
        {
            XTTProperties.printFail(parameters[0]+": ip port hexString");
            XTTProperties.printFail(parameters[0]+": localport ip remoteport hexString");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                byte[] data = new byte[0];
                if(parameters.length==5)
                {
                    //Convert the data.
                	data = ConvertLib.getByteArrayFromHexString(parameters[4]);
                	//Create the packet.
                    DatagramPacket packet = new DatagramPacket(data,data.length);
                    //Set the address.
                    packet.setAddress(DNSServer.resolveAddressToInetAddress(parameters[2]));
                    //set the port.
                    packet.setPort(Integer.parseInt(parameters[3]));
                    
                    XTTProperties.printInfo(parameters[0] + ": Sending data to: " + parameters[1]);
                    XTTProperties.printDebug(parameters[0] + ": data:\n" + ConvertLib.getHexView(data));
                    //Send the packet.
                    UDPServer.send(packet,Integer.parseInt(parameters[1]));
                }
                else if(parameters.length==4)
                {
                    //Convert the data.
                	data = ConvertLib.getByteArrayFromHexString(parameters[3]);
                	//Create the packet.
                    DatagramPacket packet = new DatagramPacket(data,data.length);
                    //Set the address.
                    packet.setAddress(DNSServer.resolveAddressToInetAddress(parameters[1]));
                    //set the port.
                    packet.setPort(Integer.parseInt(parameters[2]));
                    
                    XTTProperties.printInfo(parameters[0] + ": Sending data to: " +XTTProperties.getIntProperty("UDP/PORT"));
                    XTTProperties.printDebug(parameters[0] + ": data:\n" + ConvertLib.getHexView(data));
                    //Send the packet.
                    UDPServer.send(packet,XTTProperties.getIntProperty("UDP/PORT"));  
                }
            }
            catch (Exception e)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
        return status;
    }

    /**
     * Query for any particular packet.
     * @param parameters
     */
   	public int queryPacket(String[] parameters)
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryPacket: variableName regularExpression");
            XTTProperties.printFail(this.getClass().getName()+": queryPacket: variableName regularExpression port");
            XTTProperties.printFail(this.getClass().getName()+": queryPacket: variableName regularExpression port packetnumber");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length < 3)||(parameters.length > 5))
        {
            XTTProperties.printFail(parameters[0]+": variableName regularExpression");
            XTTProperties.printFail(parameters[0]+": variableName regularExpression port");
            XTTProperties.printFail(parameters[0]+": variableName regularExpression port packetnumber");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            if(parameters.length == 3)
            {
                //Get the specified packet.
            	UDPServer.queryPacket(XTTProperties.getIntProperty("UDP/PORT"),parameters[1],parameters[2],0);        
            }
            else if (parameters.length == 4)
            {
                //Get the specified packet.
            	UDPServer.queryPacket(Integer.parseInt(parameters[3]),parameters[1],parameters[2],0);        
            }
            else if (parameters.length == 5)
            {
                //Get the specified packet.
            	UDPServer.queryPacket(Integer.parseInt(parameters[3]),parameters[1],parameters[2],Integer.parseInt(parameters[4]));        
            }            
        }
        return status;
    }
    
    /**
     * Wait for the packet.
     * @param parameters
     */
   	public int waitForPackets(String[] parameters)
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForPackets: numberOfPackets");
            XTTProperties.printFail(this.getClass().getName()+": waitForPackets: numberOfPackets port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length < 2)||(parameters.length > 3))
        {
            XTTProperties.printFail(parameters[0]+": numberOfPackets");
            XTTProperties.printFail(parameters[0]+": numberOfPackets port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                if(parameters.length == 2)
                {
                    //Wait for the packet.
                	UDPServer.waitForPackets(Integer.parseInt(parameters[1]),XTTProperties.getIntProperty("UDP/PORT"));
                }
                else if(parameters.length == 3)
                {
                	//Wait for the packet.
                    UDPServer.waitForPackets(Integer.parseInt(parameters[1]),Integer.parseInt(parameters[2]));
                }
            }
            catch (Exception e)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }            
        }
        return status;
    }
    
    /**
     * Wait for specified packet.
     * @param parameters
     */
   	public int waitForThisPacket(String[] parameters)
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForThisPacket: regex");
            XTTProperties.printFail(this.getClass().getName()+": waitForThisPacket: regex port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length < 2)||(parameters.length > 3))
        {
            XTTProperties.printFail(parameters[0]+": regex");
            XTTProperties.printFail(parameters[0]+": regex port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                if(parameters.length == 2)
                {
                    //Wait for given packet.
                	UDPServer.waitForThisPacket(parameters[1],XTTProperties.getIntProperty("UDP/PORT"));
                }
                else if(parameters.length == 3)
                {
                	//Wait for given packet.
                    UDPServer.waitForThisPacket(parameters[1],Integer.parseInt(parameters[2]));
                }
            }
            catch (Exception e)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }            
        }
        return status;
    }    

    /**
     * 
     * @param parameters
     */
   	public int waitForTimeoutPackets(String[] parameters)
    {
   		int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutPackets: timeouttime");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutPackets: timeouttime port");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ((parameters.length < 2)||(parameters.length > 3))
        {
            XTTProperties.printFail(parameters[0]+": timeouttime");
            XTTProperties.printFail(parameters[0]+": timeouttime port");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        else
        {
            try
            {
                int timeout=Integer.parseInt(parameters[1]);
                if(parameters.length == 2)
                {
                    UDPServer.waitForTimeoutPackets(timeout,XTTProperties.getIntProperty("UDP/PORT"));
                }
                else if(parameters.length == 3)
                {
                    UDPServer.waitForTimeoutPackets(timeout,Integer.parseInt(parameters[2]));
                }
            }
            catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[1] + "' isn't a number.");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(nfe);
                }                    
            }
            catch (Exception e)
            {
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }            
        }
        return status;
    }

    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    public String checkResources()
    {
        int port = XTTProperties.getIntProperty("UDP/PORT");
        String resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        if(port>0)
        {
            try
            {
                java.net.DatagramSocket s=new java.net.DatagramSocket(port);
                s.close();
            } catch(Exception e)
            {
                resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  port+"'";
            }
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
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_UDP.java,v 1.13 2010/03/18 05:30:39 rajesh Exp $";
}