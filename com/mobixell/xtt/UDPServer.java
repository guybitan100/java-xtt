package com.mobixell.xtt;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;

public class UDPServer extends Thread
{
    private static java.util.Map<Integer,UDPServer> serverMap = java.util.Collections.synchronizedMap(new HashMap<Integer,UDPServer>());

    private DatagramSocket socket = null;
    private int port = -1;
    private Vector<String> receivedPackets = new Vector<String>();

    private int numberOfReceivedPackets = 0;
    private Object receivedKey = new Object();

    /**
     * Start the UDP server on specified port
     * @param port - UDP server port.
     * @throws SocketException - It can throws the Socket exception.
     */
    public UDPServer(int port) throws SocketException
    {
        this.port = port;
        socket = new DatagramSocket(port);

        synchronized(serverMap)
        {
            serverMap.put(port,this);
        }
    }

    public UDPServer() throws SocketException
    {
        socket = new DatagramSocket();
        port = socket.getLocalPort();

        synchronized(serverMap)
        {
            serverMap.put(port,this);
        }
    }

    /**
     * Send the datagram packet to specified port.
     * @param packet - Datagram packet.
     * @param port - port number on which datagram packet need to be send.
     * @throws java.io.IOException - It throws IO exception.
     */
    public static void send(DatagramPacket packet, int port) throws java.io.IOException
    {
        try
        {
            //Get the server from specified port and send the datagram packet.
        	serverMap.get(port).send(packet);
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: send: No such serverport: " + port);
        }
    }

    public void send(DatagramPacket packet) throws java.io.IOException
    {
        socket.send(packet);
    }

    public boolean queryPacket(String variableName, String regularExpression, int packetNumber)
    {
        String data = "";
        if(packetNumber < 0)
        {
            data = receivedPackets.get(receivedPackets.size() + packetNumber - 1);
        }
        else if (packetNumber > 0)
        {
            data = receivedPackets.get(packetNumber - 1);
        }
        else
        {
            data = receivedPackets.lastElement();
        }
        data = ConvertLib.getHexStringFromByteArray(ConvertLib.base64Decode(data));
        XTTProperties.printDebug("UDPServer: " + data);
        return ConvertLib.queryString("UDPServer",data,regularExpression,variableName);
    }

    /**
     * Query for any particular packet.
     * @param port - port on which specified packet need to queried.
     * @param variableName - Variable name.
     * @param regularExpression - regular expression need to ne looked.
     * @param packetNumber - Packet number.
     * @return - It returns boolean.
     */
    public static boolean queryPacket(int port, String variableName, String regularExpression, int packetNumber)
    {
        boolean status = false;
        try
        {
            status = serverMap.get(port).queryPacket(variableName, regularExpression, packetNumber);
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: queryPacket: No such serverport: " + port);
        }
        return status;
    }

    public void addReceivedPacket(String packet)
    {
        //If we're in standalone mode, save memory and don't add the packets.
        if(XTTProperties.getXTT() != null)
        {
            synchronized(receivedKey)
            {
                XTTProperties.printDebug("UDPServer("+ socket.getLocalPort() +"): adding packet:\n"+packet);
                receivedPackets.add(packet);
                numberOfReceivedPackets++;
                receivedKey.notifyAll();
            }
        }
    }

    public void waitForPackets(int numberOfPacket) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("UDP/WAITTIMEOUT");
        int prevcount=0;
        synchronized(receivedKey)
        {
            while(numberOfReceivedPackets<numberOfPacket)
            {
                XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +").waitForPackets: "+numberOfReceivedPackets+"/"+numberOfPacket);
                if(wait>0)
                {
                    prevcount=numberOfReceivedPackets;
                    receivedKey.wait(wait);
                    if(numberOfReceivedPackets==prevcount)
                    {
                        XTTProperties.printFail("UDPServer("+ socket.getLocalPort() +").waitForPackets: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                else
                {
                    receivedKey.wait();
                }
            }
            XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +").waitForPackets: "+numberOfReceivedPackets+"/"+numberOfPacket);
        }
    }

    public void waitForThisPacket(String regex) throws java.lang.InterruptedException
    {
        int wait=XTTProperties.getIntProperty("UDP/WAITTIMEOUT");
        int prevcount=0;
        long startTime = System.currentTimeMillis();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher("");

        synchronized(receivedKey)
        {
            while((numberOfReceivedPackets<1)||(!matcher.find()))
            {
                if(wait>0)
                {
                    prevcount=numberOfReceivedPackets;
                    receivedKey.wait(wait);
                    if((System.currentTimeMillis()-startTime)>wait)
                    {
                        XTTProperties.printFail("UDPServer("+ socket.getLocalPort() +").waitForThisPacket: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                }
                else
                {
                    receivedKey.wait();
                }

                if(numberOfReceivedPackets>0)
                {
                    matcher = pattern.matcher(ConvertLib.getHexStringFromByteArray(ConvertLib.base64Decode(receivedPackets.lastElement())));
                }
            }
            XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +").waitForThisPacket: Matched '"+regex+"'");
        }
    }

    public void waitForTimeoutPackets(int timeoutTime) throws java.lang.InterruptedException
    {
        int prevcount=0;
        synchronized(receivedKey)
        {
            XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +").waitForTimeoutPackets time: "+timeoutTime+"ms");

            prevcount=numberOfReceivedPackets;
            receivedKey.wait(timeoutTime);
            if(numberOfReceivedPackets==prevcount)
            {
                XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +").waitForTimeoutPackets: timed out!");                
                return;
            }            
            XTTProperties.printFail("UDPServer("+ socket.getLocalPort() +").waitForTimeoutPackets: "+numberOfReceivedPackets+"/"+prevcount);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    /**
     * Wait for packet.
     * @param numberOfPacket - Wait for specified packet number.
     * @param port - Wait for the packet on this port.
     * @throws java.lang.InterruptedException
     */
    public static void waitForPackets(int numberOfPacket, int port) throws java.lang.InterruptedException
    {
        try
        {
            serverMap.get(port).waitForPackets(numberOfPacket);
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: waitForPackets: No such serverport: " + port);
        }
    }

    /**
     * Wait for given packet.
     * @param regex - The regex in packet need to be looked.
     * @param port - Wait for a packet containing regex to come in to server port
     * @throws java.lang.InterruptedException
     */
    public static void waitForThisPacket(String regex, int port) throws java.lang.InterruptedException
    {
        try
        {
            serverMap.get(port).waitForThisPacket(regex);
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: waitForThisPacket: No such serverport: " + port);
        }
    }

    public static void waitForTimeoutPackets(int timeoutTime, int port) throws java.lang.InterruptedException
    {
        try
        {
            serverMap.get(port).waitForTimeoutPackets(timeoutTime);
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: waitForTimeoutPackets: No such serverport: " + port);
        }
    }

    public void run()
    {
        XTTProperties.printInfo("UDPServer("+ socket.getLocalPort() +"): listening");

        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try
        {
            while(true)
            {
                socket.receive(packet);
                new UDPWorker(packet,socket,this).start();

                buf = new byte[1024];
                packet = new DatagramPacket(buf, buf.length);
            }
        }
        catch(SocketException e)
        {
            //
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    public static void main(String args[])
    {
        try
        {
            UDPServer thisServer = null;
            if(args.length > 0)
            {
                thisServer = new UDPServer(Integer.parseInt(args[0]));
            }
            else
            {
                thisServer = new UDPServer();
            }
            thisServer.start();
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    //Stop THIS instance of the UDPServer running
    public void stopGracefully() throws java.lang.Exception
    {
        XTTProperties.printDebug("UDPServer("+ socket.getLocalPort() +"): close socket");
        socket.close();
        this.join();
    }

    //Try to stop the UDPServer with the 'port'
    public static void stopGracefully(int port) throws java.lang.Exception
    {
        try
        {
            //Get the server from server map and stop it.
        	serverMap.get(port).stopGracefully();
        }
        catch(NullPointerException npe)
        {
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printFail("UDPServer: stopGracefully: No such serverport: " + port);
        }
        synchronized(serverMap)
        {
            //Remove the server port from map after it stopped.
        	serverMap.remove(port);
        }
    }

    //Try to stop ALL UDPServers
    public static void stopAllGracefully() throws java.lang.Exception
    {
        Iterator<Integer> i = serverMap.keySet().iterator();
        Integer port = null;
        while(i.hasNext())
        {
            port = i.next();
            try
            {
                serverMap.get(port).stopGracefully();
            }
            catch(NullPointerException npe){}
        }

        synchronized(serverMap)
        {
            serverMap.clear();
        }
    }

    public int getLocalPort()
    {
        return port;
    }

    public boolean checkSocket()
    {
        return (socket==null);
    }

    public static final String tantau_sccsid = "@(#)$Id: UDPServer.java,v 1.8 2010/03/18 05:30:40 rajesh Exp $";
}