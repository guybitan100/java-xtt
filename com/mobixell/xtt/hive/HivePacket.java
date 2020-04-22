package com.mobixell.xtt.hive;

import com.mobixell.xtt.ConvertLib;

import java.util.Vector;
import java.net.DatagramPacket;

public class HivePacket
{
    private int messageType = -1;
    private int packetLength = -1;
    private Vector<TLV> tags = new Vector<TLV>();

    private boolean packetFromSelf = false;
    private boolean packetForwarded = false;

    private byte[] data = new byte[0];
    private DatagramPacket origPacket = null;

    public HivePacket(DatagramPacket packet)
    {
        this.origPacket = packet;
        this.data = packet.getData();
        parsePacket();
    }

    private void parsePacket()
    {
        
        tags.clear();
        //This is the length specified in the packet, plus the first packet type and length values.
        messageType   = ConvertLib.getIntFromByteArray(data,0,2);
        packetLength = ConvertLib.getIntFromByteArray(data,2,2);

        int tagtype = 0;
        int taglength=0;
        byte[] tagdata = new byte[0];
        TLV tempTLV = null;
        //Don't read the first type, and length again.
        int pointer=4;

        //Loop round all the tags, and add them to the Vector.
        while(pointer < (packetLength+4))
        {
            tagtype = ConvertLib.getIntFromByteArray(data,pointer,2);
            pointer+=2;
            taglength = ConvertLib.getIntFromByteArray(data,pointer,2);
            pointer+=2;
            tagdata = new byte[taglength];
            ConvertLib.addBytesToArray(tagdata,0,data,pointer,taglength);
            tempTLV = new TLV();
            tempTLV.type = tagtype;
            tempTLV.length = taglength;
            tempTLV.value = tagdata;
            tags.add(tempTLV);

            if(tempTLV.type == Hive.FORWARDEDTAG)
                packetForwarded = true;

            pointer+=taglength;
        }

        //System.out.println(ConvertLib.getHexView(data,0,packetLength+4));
    
        try
        {
            //Get all the interfaces, then check the InetAddress against it.
            java.net.InetAddress[] allInterfaces = java.net.InetAddress.getAllByName(java.net.InetAddress.getLocalHost().getHostName());
            packetFromSelf = java.util.Arrays.asList(allInterfaces).contains(origPacket.getAddress());
        }
        catch(java.net.UnknownHostException uhe)
        {
            packetFromSelf = false;
        }
    }

    public int getMessageType()
    {
        return messageType;
    }

    public int getPacketLength()
    {
        return packetLength;
    }

    public boolean isPacketFromSelf()
    {
        return packetFromSelf;
    }

    public boolean isForwarded()
    {
        return packetForwarded;
    }

    public byte[] getData()
    {
        return data;
    }

    public byte[] getDataToForward()
    {
        byte[] toForward = new byte[0];
        byte[] forwardTag = Hive.makeTag(Hive.FORWARDEDTAG,new byte[0]);
        //Start with the 4 bytes for message type and length
        int length=4;
        for(TLV tag: tags)
        {
            if(tag.type != Hive.OUTOFHIVETAG)
                length += tag.length + 4;
        }

        toForward = new byte[length + forwardTag.length];
        
        ConvertLib.addBytesToArray(toForward,0,ConvertLib.getByteArrayFromInt(messageType,2));
        ConvertLib.addBytesToArray(toForward,2,ConvertLib.getByteArrayFromInt(toForward.length-4,2));

        int pointer=4;
        for(TLV tag: tags)
        {
            if(tag.type != Hive.OUTOFHIVETAG)
            {
                pointer = ConvertLib.addBytesToArray(toForward,pointer,Hive.makeTag(tag.type,tag.value));
            }
        }
        //System.out.println("toForward.length" + toForward.length + " pointer: " + pointer + " forwardTag.length: " + forwardTag.length);
        //System.out.println(ConvertLib.getHexView(toForward));
        pointer = ConvertLib.addBytesToArray(toForward,pointer,forwardTag);
        
        return toForward;
    }

    public Vector<TLV> getTLVs()
    {
        return tags;
    }

    public String toString()
    {
        return "'"+getAddress().getHostAddress()+"':\n"+ConvertLib.getHexView(data,0,packetLength+4);
    }

    //Copy DatagramPacket functionality
    public java.net.InetAddress getAddress()
    {
        return origPacket.getAddress();
    }
    public int getPort()
    {
        return origPacket.getPort();
    }
    
    public static final String tantau_sccsid = "@(#)$Id: HivePacket.java,v 1.4 2008/02/01 14:40:46 gcattell Exp $";
}