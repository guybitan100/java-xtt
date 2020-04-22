package com.mobixell.xtt;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class UDPWorker extends Thread
{
    UDPServer parent = null;
    DatagramPacket packet = null;
    DatagramSocket socket = null;

    public UDPWorker(DatagramPacket packet, DatagramSocket socket, UDPServer parent)
    {
        this.packet = packet;
        this.socket = socket;
        this.parent = parent;
    }

    public void run()
    {
        try
        {
            byte[] data = packet.getData();
            parent.addReceivedPacket(ConvertLib.base64Encode(data,0,packet.getLength()));
            XTTProperties.printDebug("UDPWorkder(" + socket.getLocalPort() + "): Received:\n"+ConvertLib.getHexView(data,0,packet.getLength()));
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }
    
    public static final String tantau_sccsid = "@(#)$Id: UDPWorker.java,v 1.2 2006/11/21 08:55:55 gcattell Exp $";
}
