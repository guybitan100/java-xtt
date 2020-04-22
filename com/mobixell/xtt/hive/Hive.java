package com.mobixell.xtt.hive;

import java.io.File;
import com.mobixell.xtt.*;

public class Hive extends Thread
{
    //Hive Information (and local Drone)
    private static java.net.InetAddress group = null;
    private static int port = 9882;

    private static Drone me = new Drone();
    private static long hiveStartTime = -1;
    
    private static boolean shouldTrace = false;

    //If set to false the while loop around the receive won't run.
    private static boolean enabled = true;

    //Keys to notify on.
    public static Object packetkey = new Object();
    public static int packetcount = 0;
    //Packets that aren't from this box.
    public static Object remotepacketkey = new Object();
    public static int remotepacketcount = 0;
    public static Object dronereqkey = new Object();
    public static int dronereqcount = 0;
    public static Object dronereskey = new Object();
    public static int dronerescount = 0;
    public static Object dronemsgkey = new Object();
    public static int dronemsgcount = 0;
    public static Object msgreplykey = new Object();
    public static int msgreplycount = 0;

    //Message types
    public static final int DRONELISTREQ =0x0001;
    public static final int DRONEMESSAGE =0x0002;
    public static final int MESSAGEREPLY =0x8002;
    public static final int DRONELISTRESP=0x8001;
    //Tag types
    public static final int IPADDRESSTAG  =0x0001;
    public static final int PORTTAG       =0x0002;
    public static final int HOSTNAMETAG   =0x0003;
    public static final int DRONETYPETAG  =0x0004;
    public static final int COMMENTTAG    =0x0005;
    public static final int OUTOFHIVETAG  =0x0006;
    public static final int VERSIONTAG    =0x0007;
    public static final int FORWARDEDTAG  =0x0008;
    public static final int EPOCHTIMETAG  =0x0009;
    public static final int BROADCASTTAG  =0x000A;
    public static final int UPTIMETAG     =0x000B;
    public static final int REMOTEPORTTAG =0x000C;

    //Message only Tag
    public static final int FILESENDREQ   =0x2001;

    /*
        Hive Socket
    */
    private static java.net.DatagramSocket s;

    public Hive(String group, int port, int droneType)
    {
        Log.initialize();
        this.setName("Hive");
        if(XTTProperties.getQuietProperty("SYSTEM/HIVE/ENABLETRACING").equals(""))
        {
            shouldTrace=true;
        }
        
        me.setPort(port);
        me.setDroneType(droneType);

        try
        {
            this.group = java.net.InetAddress.getByName(group);
            if( XTTProperties.getQuietProperty("SYSTEM/IP").equals("null") || XTTProperties.getQuietProperty("SYSTEM/IP").equals("127.0.0.1"))
            {
                me.setIp(java.net.InetAddress.getLocalHost().getHostAddress() );
            }
            else
            {
                me.setIp(XTTProperties.getQuietProperty("SYSTEM/IP"));
            }
            me.setHostname(me.getInetAddress().getCanonicalHostName());

            //Add the InetAddreses to HiveWorker, so it can work out if it's getting a packet from itself.
            java.net.InetAddress[] localIPs = java.net.InetAddress.getAllByName(java.net.InetAddress.getLocalHost().getHostName());
            for(int i=0;i<localIPs.length;i++)
            {
                HiveWorker.addInetAddress(localIPs[i]);
            }
        }
        catch(java.net.UnknownHostException uhe)
        {
            if(shouldTrace)
            {
                XTTProperties.printException(uhe);
            }
        }
    }

    public void sendMesssage(String message, Drone recipient)
    {
        send(makeDroneMessage(message,recipient),recipient.getInetAddress(),recipient.getPort());
    }

    public void sendFileRequest(String message, Drone recipient, File file)
    {
        send(makeDroneFileRequest(message,file.getAbsolutePath(),recipient),recipient.getInetAddress(),recipient.getPort());
    }

    public void sendGlobalMesssage(String message)
    {
        sendToGroup(makeDroneBroadcastMessage(message));
    }

    public void refreshDroneList()
    {
        HiveWorker.refreshDroneList();
    }

    public void run()
    {
        try
        {
            hiveStartTime = System.currentTimeMillis();
            HiveMonitor hiveMonitor = new HiveMonitor();
            hiveMonitor.start();
            s = new java.net.MulticastSocket(port);
            java.net.MulticastSocket ms = (java.net.MulticastSocket)s;
            ms.joinGroup(group);
        }
        catch(java.net.SocketException se)
        {
            printFail("You can't currently join that multicast group. Leaving the hive.");
            printDebugException(se);
        }
        catch(Exception e)
        {
            printDebugException(e);
        }

        byte[] buf = new byte[1024];
        java.net.DatagramPacket inPacket = new java.net.DatagramPacket(buf, buf.length);

        if(XTTProperties.getXTTGui() != null)
        {
            sendToGroup(makeDroneListRequest());
        }
        else
        {
            sendToGroup(makeDroneListResponse());
        }
        //TODO: Add the enabled stuff
        while(true)
        {
            try
            {
                if(!s.isClosed())
                {
                    buf = new byte[1024];
                    inPacket = new java.net.DatagramPacket(buf, buf.length);
                    s.receive(inPacket);
                    new HiveWorker(inPacket).start();
                }
                else
                {
                    this.yield();
                }
            }
            catch(Exception e)
            {
                if(shouldTrace)
                {
                    XTTProperties.printException(e);
                }
                //break;
            }
            //handlePacket(inPacket);
        }


    }

    protected static void send(byte[] data, java.net.InetAddress recipient, int port)
    {
        try
        {
            java.net.DatagramPacket packet = new java.net.DatagramPacket(data, data.length, recipient, port);
            s.send(packet);
        }
        catch(java.net.NoRouteToHostException nrthe)
        {
            if(shouldTrace)
            {
                XTTProperties.printWarn("Hive: Unable to find route to '" + recipient.getHostAddress() + "'");
            }
        }
        catch(java.io.IOException ioe)
        {
            if(shouldTrace)
            {
                XTTProperties.printException(ioe);
            }
        }
    }
    protected static void sendToGroup(byte[] data)
    {
        try
        {
            java.net.DatagramPacket packet = new java.net.DatagramPacket(data, data.length, group, port);
            s.send(packet);
        }
        catch(java.net.NoRouteToHostException nrthe)
        {
            if(shouldTrace)
            {
                if(enabled)
                {
                    XTTProperties.printWarn("Hive: Unable to send to Hive. Stopping Hive.");
                    stopHive();
                }
            }
        }
        catch(java.io.IOException ioe)
        {
            if(shouldTrace)
            {
                XTTProperties.printException(ioe);
            }
        }
    }

    protected static void setOutOfHive()
    {
        Hive.printDebug("Hive: Setting out of Hive");
        //Re-enabled.
        enabled = true;
        //Set the 'Out Of Hive' flag
        me.setIsInHive(false);
        //Shut any socket that might be open from the Multicast
        try
        {
            s.close();
        }
        catch(Exception e)
        {
            //
        }

        try
        {
            //Change the 'group' address to that of the RemoteXTT
            group = java.net.InetAddress.getByName(XTTProperties.getProperty("SYSTEM/REMOTEIP"));

            //Increase the port by one, incase you're running two on the same machine.
            me.setPort(me.getPort()+1);
            //Reopen a normal DatagramSocket.
            s = new java.net.DatagramSocket(me.getPort());
            //Resend a DroneListRequest
            sendToGroup(makeDroneListRequest());
        }
        catch(Exception e)
        {
            if (shouldTrace)
            {
                XTTProperties.printWarn("Hive: Error reconfiguring into Out Of Hive mode. Aborted Hive.");
                XTTProperties.printException(e);
            }
        }
    }

    private static void stopHive()
    {
        enabled = false;
        try
        {
            s.close();
        }
        catch(Exception e)
        {
            //
        }
    }

    protected static byte[] makeDroneListResponse()
    {
        byte[] response = new byte[0];

        byte[] respIp =             makeTag(IPADDRESSTAG,me.getInetAddress().getAddress());
        byte[] respPort =           makeTag(PORTTAG,ConvertLib.getByteArrayFromInt(me.getPort(),2));
        byte[] respHostname =       makeTag(HOSTNAMETAG,ConvertLib.createBytes(""+me.getHostname()));
        byte[] respDroneType =      makeTag(DRONETYPETAG,ConvertLib.getByteArrayFromInt(me.getDroneType(),1));
        byte[] respDroneVersion =   makeTag(VERSIONTAG,XTTProperties.getXTTBuildVersion().getBytes());
        byte[] respDroneUpTime =    makeTag(UPTIMETAG,ConvertLib.getByteArrayFromLong( (System.currentTimeMillis() - hiveStartTime)/1000) );
        byte[] respDroneOutOfHive = new byte[0];
        if(!me.isInHive()) respDroneOutOfHive = makeTag(OUTOFHIVETAG,new byte[0]);
        byte[] respCommentOne = new byte[0];
        if(!XTTProperties.getQuietProperty("SYSTEM/HIVE/COMMENT").equals("null"))
        {
            respCommentOne = makeTag(COMMENTTAG,XTTProperties.getProperty("SYSTEM/HIVE/COMMENT").getBytes());
        }
        byte[] respRemotePort =     new byte[0];
        if(XTTProperties.getRemoteXTTPort()>0)
        {
            respRemotePort=makeTag(REMOTEPORTTAG,ConvertLib.getByteArrayFromInt(XTTProperties.getRemoteXTTPort(),2));
        }


        response = new byte[4 + respIp.length + respPort.length + respHostname.length + respDroneType.length + respCommentOne.length + respDroneVersion.length + respDroneOutOfHive.length + respDroneUpTime.length+respRemotePort.length];

        int pointer = 0;
        pointer = ConvertLib.addBytesToArray(response,pointer,ConvertLib.getByteArrayFromInt(DRONELISTRESP,2));

        pointer = ConvertLib.addBytesToArray(response,pointer,ConvertLib.getByteArrayFromInt(response.length-4,2));

        pointer = ConvertLib.addBytesToArray(response,pointer,respIp);
        pointer = ConvertLib.addBytesToArray(response,pointer,respPort);
        pointer = ConvertLib.addBytesToArray(response,pointer,respHostname);
        pointer = ConvertLib.addBytesToArray(response,pointer,respDroneType);
        pointer = ConvertLib.addBytesToArray(response,pointer,respDroneVersion);
        pointer = ConvertLib.addBytesToArray(response,pointer,respDroneUpTime);
        pointer = ConvertLib.addBytesToArray(response,pointer,respCommentOne);
        pointer = ConvertLib.addBytesToArray(response,pointer,respDroneOutOfHive);
        pointer = ConvertLib.addBytesToArray(response,pointer,respRemotePort);

        if(Hive.getShouldTrace())
        {
            XTTProperties.printDebug("Sending Drone List Response:\n"+ConvertLib.getHexView(response));
        }

        return response;
    }

    protected static byte[] makeDroneListRequest()
    {
        byte[] request = new byte[4];
        ConvertLib.addBytesToArray(request,0,ConvertLib.getByteArrayFromInt(DRONELISTREQ,2));
        ConvertLib.addBytesToArray(request,2,ConvertLib.getByteArrayFromInt(0,2));

        if(Hive.getShouldTrace())
        {
            XTTProperties.printDebug("Sending Drone List Request:\n"+ConvertLib.getHexView(request));
        }

        return request;
    }

    protected static byte[] makeDroneBroadcastMessage(String message)
    {
        return makeDroneMessage(message, true, null, null);
    }
    protected static byte[] makeDroneMessage(String message, Drone recipient)
    {
        return makeDroneMessage(message, false, null, recipient);
    }
    protected static byte[] makeDroneFileRequest(String message, String filename, Drone recipient)
    {
        return makeDroneMessage(message, false, filename, recipient);
    }

    private static byte[] makeDroneMessage(String message, boolean isBroadcast, String filename, Drone recipient)
    {
        byte[] ipTag =  makeTag(IPADDRESSTAG,me.getInetAddress().getAddress());
        byte[] commentTag = makeTag(COMMENTTAG,ConvertLib.createBytes(message));
        byte[] broadcastTag = new byte[0];
        if(isBroadcast)broadcastTag = makeTag(BROADCASTTAG,new byte[0]);

        long sentTime = System.currentTimeMillis();

        byte[] epochTag = makeTag(EPOCHTIMETAG,ConvertLib.getByteArrayFromLong(sentTime));
        byte[] fileReqTag = new byte[0];
        if(filename!=null)fileReqTag = makeTag(FILESENDREQ, ConvertLib.createBytes(filename));

        byte[] messagePacket = new byte[4 + commentTag.length + ipTag.length + broadcastTag.length + epochTag.length + fileReqTag.length];

        int pointer = 0;
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,ConvertLib.getByteArrayFromInt(DRONEMESSAGE,2));
        //length
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,ConvertLib.getByteArrayFromInt(messagePacket.length - 4,2));
        //IP tag
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,ipTag);
        //Broadcast tag (if set)
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,broadcastTag);
        //Epoch tag
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,epochTag);
        //Send file req tag
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,fileReqTag);
        //The message tag
        pointer = ConvertLib.addBytesToArray(messagePacket,pointer,commentTag);
        if(Hive.getShouldTrace())
        {
            XTTProperties.printDebug("Sending Message:\n"+ConvertLib.getHexView(messagePacket));
        }

        //Save the sent message
        Message outMessage = new Message();
        outMessage.setTime(sentTime);
        outMessage.setSender(me);
        if(recipient != null) outMessage.setRecipient(recipient);
        outMessage.setMessage(message);
        outMessage.setMessagePacket(messagePacket);
        outMessage.setIsBroadcast(isBroadcast);
        outMessage.setIsFromSelf(true);
        if(filename!=null) outMessage.setFile(filename);
        outMessage.add();
        if(recipient != null)
        {
            new MessageMonitor(outMessage).start();
        }

        return messagePacket;
    }

    public static byte[] makeMessageReply(String message, long epochTime)
    {
        return makeMessageReply( message, epochTime, null, null);
    }
    //The EmptyStackException is thrown by the FileTransferAgent constructor.
    public static byte[] makeMessageReply(String message, long epochTime, Message acceptFileMessage, File saveTo) throws java.util.EmptyStackException
    {
        byte[] ipTag =  makeTag(IPADDRESSTAG,me.getInetAddress().getAddress());
        byte[] commentTag = makeTag(COMMENTTAG,ConvertLib.createBytes(message));
        byte[] epochTag = makeTag(EPOCHTIMETAG,ConvertLib.getByteArrayFromLong(epochTime));
        byte[] fileTag = new byte[0];
        byte[] portTag = new byte[0];
        if((acceptFileMessage != null)&&(acceptFileMessage.getFile() != null))
        {
            //acceptFileMessage.setFileTransferAgent(9884,saveTo);
            FileTransferAgent fta = new FileTransferAgent(saveTo);
            int ftaPort = fta.getPort();
            fta.start();
            fileTag = makeTag(FILESENDREQ,ConvertLib.createBytes(acceptFileMessage.getAbsoluteFilePath()));
            portTag = makeTag(PORTTAG,ConvertLib.getByteArrayFromInt(ftaPort,2));
        }
        byte[] replyPacket = new byte[4 + commentTag.length + ipTag.length + epochTag.length + fileTag.length + portTag.length];

        int pointer = 0;
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,ConvertLib.getByteArrayFromInt(MESSAGEREPLY,2));
        //length
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,ConvertLib.getByteArrayFromInt(replyPacket.length - 4,2));
        //IP tag
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,ipTag);
        //Epoch tag
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,epochTag);
        //filename tag
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,fileTag);
        //file port tag
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,portTag);
        //The message tag
        pointer = ConvertLib.addBytesToArray(replyPacket,pointer,commentTag);
        if(Hive.getShouldTrace())
        {
            XTTProperties.printDebug("Sending Message Reply:\n"+ConvertLib.getHexView(replyPacket));
        }

        return replyPacket;
    }

    protected static byte[] makeTag(int tag, byte[] data)
    {
        byte[] newTag = new byte[data.length + 4];
        ConvertLib.addBytesToArray(newTag,0,ConvertLib.getByteArrayFromInt(tag,2));
        ConvertLib.addBytesToArray(newTag,2,ConvertLib.getByteArrayFromInt(data.length,2));
        ConvertLib.addBytesToArray(newTag,4,data);
        return newTag;
    }

    public static Drone getLocalDrone()
    {
        return me;
    }

    public static java.util.Set<Drone> getDrones()
    {
        java.util.TreeSet<Drone> sortedSet = new java.util.TreeSet<Drone>(HiveWorker.getDrones().values());
        return sortedSet;
    }
    public static boolean getShouldTrace()
    {
        return shouldTrace;
    }
    public static void setShouldTrace(boolean should)
    {
        shouldTrace=should;
    }

    public static boolean printDebug(String message)
    {
        if(shouldTrace)
        {
            return XTTProperties.printDebug(message);
        }
        else
        {
            return false;
        }
    }

    public static boolean printFail(String message)
    {
        if(shouldTrace)
        {
            return XTTProperties.printFail(message);
        }
        else
        {
            return false;
        }
    }

    public static void printDebugException(Exception e)
    {
        if(shouldTrace)
        {
            XTTProperties.printDebugException(e);
        }
    }

    public static void printException(Exception e)
    {
        XTTProperties.printException(e);
    }

    public static final String tantau_sccsid = "@(#)$Id: Hive.java,v 1.36 2009/06/11 13:35:12 rsoder Exp $";
}