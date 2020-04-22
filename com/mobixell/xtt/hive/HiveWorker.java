package com.mobixell.xtt.hive;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;

public class HiveWorker extends Thread
{
    private HivePacket packet = null;
    private static Hive theHive = null;
    private static java.util.Set<java.net.InetAddress> allMyInetAddresses = java.util.Collections.synchronizedSet(new java.util.HashSet<java.net.InetAddress>());

    private static java.util.Map<String,Drone> drones = java.util.Collections.synchronizedMap(new java.util.HashMap<String,Drone>());
    private static java.util.Map<String,Drone> outOfHiveDrones = java.util.Collections.synchronizedMap(new java.util.HashMap<String,Drone>());

    protected static void addInetAddress(java.net.InetAddress oneAddress)
    {
        allMyInetAddresses.add(oneAddress);
    }

    public static java.util.Map<String,Drone> getDrones()
    {
        return drones;
    }

    public static void refreshDroneList()
    {
        drones.clear();
        Hive.sendToGroup(Hive.makeDroneListResponse());
        Hive.sendToGroup(Hive.makeDroneListRequest());
    }

    public HiveWorker(java.net.DatagramPacket packet)
    {
        this.packet = new HivePacket(packet);
    }
    public void run()
    {
        handlePacket();
    }

/*
        Packet Format:

          0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |       PacketType      |     PacketLength      |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                       TLVs                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

        TLV Format:

          0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |       TagID           |     DataLength        |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        /                     DATA                      /
        /                                               /
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    */

    private void handlePacket()
    {
        try
        {
            synchronized(Hive.packetkey)
            {
                Hive.packetcount++;
                Hive.packetkey.notify();
            }
            if(!packet.isPacketFromSelf())
            {
                synchronized(Hive.remotepacketkey)
                {
                    Hive.remotepacketcount++;
                    Hive.remotepacketkey.notify();
                }
            }
            
            switch(packet.getMessageType())
            {
                case Hive.DRONELISTREQ:
                    handleDroneListReq();
                    break;
                case Hive.DRONELISTRESP:
                    handleDroneListResp();
                    break;
                case Hive.DRONEMESSAGE:
                    handleDroneMessage();
                    break;
                case Hive.MESSAGEREPLY:
                    handleMessageReply();
                    break;
                default:
                    if(Hive.getShouldTrace())
                    {
                        XTTProperties.printDebug("Got Unknown Request: "+ packet.toString());
                    }
            }

            Drone sender = drones.get(packet.getAddress().getHostAddress());
            boolean packetFromOutOfHive = outOfHiveDrones.containsValue(sender);

            //If the message was from outside the Hive, and hasn't already been forwarded, send to the hive.
            if((!packet.isForwarded())&&(packetFromOutOfHive))
            {
                    if(Hive.getShouldTrace())
                    {
                        XTTProperties.printDebug("Forwarding packet to hive");
                    }                
                    Hive.sendToGroup(packet.getDataToForward());
            }
            //If the message from inside the hive, send to everyone outside the hive.
            else if(!packetFromOutOfHive)
            {
                //Resend to all Drones not in Hive
                for(Drone outOfHiveDrone: outOfHiveDrones.values())
                {
                    if((sender != outOfHiveDrone)||(!packet.isForwarded()))
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printDebug("Forwarding packet to " + outOfHiveDrone.toString());
                        }
                        Hive.send(packet.getDataToForward(),outOfHiveDrone.getInetAddress(),outOfHiveDrone.getPort());
                    }
                }
            }
        }
        catch(Exception e)
        {
            if(Hive.getShouldTrace())
            {
                XTTProperties.printException(e);
            }
        }
    }

    private void handleDroneListReq()
    {
        //Avoid printing output when an XTT test is running to avoid spam.
        if((!packet.isPacketFromSelf())&&(Hive.getShouldTrace()))
        {
            XTTProperties.printDebug("Got Drone List Request "+ packet.toString());
        }
        synchronized(Hive.dronereqkey)
        {
            Hive.dronereqcount++;
            Hive.dronereqkey.notify();
        }
        Hive.sendToGroup(Hive.makeDroneListResponse());
    }

    private void handleDroneListResp()
    {
        //Avoid printing output when an XTT test is running to avoid spam.
        if((!packet.isPacketFromSelf())&&(Hive.getShouldTrace()))
        {
            XTTProperties.printDebug("Got Drone List Response: "+ packet.toString());
        }
        synchronized(Hive.dronereskey)
        {
            Hive.dronerescount++;
            Hive.dronereskey.notify();
        }


        Drone tempDrone = new Drone();

        for (TLV tag: packet.getTLVs())
        {
            switch(tag.type)
            {
                case Hive.IPADDRESSTAG:
                    try
                    {
                        tempDrone.setIp(java.net.InetAddress.getByAddress(tag.value).getHostAddress());
                    }
                    catch(java.net.UnknownHostException uhe)
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printException(uhe);
                        }
                    }
                    break;
                case Hive.PORTTAG:
                    tempDrone.setPort(ConvertLib.getIntFromByteArray(tag.value,0,2));
                    break;
                case Hive.HOSTNAMETAG:
                    tempDrone.setHostname(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                case Hive.DRONETYPETAG:
                    tempDrone.setDroneType(ConvertLib.getIntFromByteArray(tag.value,0,1));
                    break;
                case Hive.COMMENTTAG:
                    tempDrone.addComment(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                case Hive.OUTOFHIVETAG:
                    tempDrone.setIsInHive(false);
                    break;
                case Hive.VERSIONTAG:
                    tempDrone.setVersion(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                case Hive.UPTIMETAG:
                    tempDrone.setUptime(ConvertLib.getLongFromByteArray(tag.value,0,tag.length));
                    break;                    
                case Hive.REMOTEPORTTAG:
                    tempDrone.setRemoteXTTPort(ConvertLib.getIntFromByteArray(tag.value,0,2));
                    break;                    
                case Hive.FORWARDEDTAG:
                    //No need to to anything.
                    break;
                default:
                    if(Hive.getShouldTrace())
                    {
                        XTTProperties.printFail("Unknown tag(" + tag.type +"):\n" + ConvertLib.getHexView(tag.value));
                    }
                    break;
            }
        }

        drones.put(tempDrone.getIp(),tempDrone);
        //Easier to access list of Drones which we need to broadcast to
        if(!tempDrone.isInHive())
        {
            if(Hive.getShouldTrace())
            {
                XTTProperties.printDebug("HiveWorker: " + tempDrone.getIp() + " is out of the hive.");
            }
            outOfHiveDrones.put(tempDrone.getIp(),tempDrone);
        }
    }

    private void handleDroneMessage()
    {
        Message newmessage = new Message();
        
        synchronized(Hive.dronemsgkey)
        {
            Hive.dronemsgcount++;
            Hive.dronemsgkey.notify();
        }

        //Support for Hives that don't sent the IPADDRESSTAG
        Drone sender = drones.get(packet.getAddress().getHostAddress());
        
        for (TLV tag: packet.getTLVs())
        {
            switch(tag.type)
            {
                case Hive.COMMENTTAG:
                    newmessage.setMessage(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                case Hive.IPADDRESSTAG:
                    try
                    {
                        sender = drones.get(java.net.InetAddress.getByAddress(tag.value).getHostAddress());
                    }
                    catch(java.net.UnknownHostException uhe)
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printException(uhe);
                        }
                    }
                    break;
                case Hive.BROADCASTTAG:
                    newmessage.setIsBroadcast(true);
                    break;
                case Hive.EPOCHTIMETAG:
                    newmessage.setTime(ConvertLib.getLongFromByteArray(tag.value,0,tag.length));
                    break;
                case Hive.FILESENDREQ:
                    newmessage.setFile(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                default:
                    if(Hive.getShouldTrace())
                    {
                        XTTProperties.printFail("Skipping tag(" + tag.type +") for DroneMessage:\n" + ConvertLib.getHexView(tag.value));
                    }
                    break;
            }
        }

        //If the sender is null here, then we don't know the sender of the packet either (this is for alpha support where the IP isn't included).
        if(sender == null)
        {
            sender = new Drone();
            sender.setIp(packet.getAddress().getHostAddress());
            sender.setPort(packet.getPort());
            sender.setDroneType(Drone.XTTDRONE);
            drones.put(sender.getIp(),sender);
        }
        
        //Send a reply to let the sender know you got the message.
        if(!newmessage.getIsBroadcast())
        {
            Hive.send(Hive.makeMessageReply(newmessage.getMessage(),newmessage.getTime()),sender.getInetAddress(),sender.getPort());
        }
        
        
        newmessage.setSender(sender);
        //If the message is a Broadcast you can't be sure who the message is to.
        if(!newmessage.getIsBroadcast())
        {
            newmessage.setRecipient(Hive.getLocalDrone());
        }

        newmessage.add();
    }

    private void handleMessageReply()
    {
        Message newmessage = new Message();
        
        int filePort = -1;
        String filename = null;

        synchronized(Hive.msgreplykey)
        {
            Hive.msgreplycount++;
            Hive.msgreplykey.notify();
        }

        for (TLV tag: packet.getTLVs())
        {
            switch(tag.type)
            {
                case Hive.COMMENTTAG:
                    newmessage.setMessage(ConvertLib.getStringFromOctetByteArray(tag.value));
                    break;
                case Hive.IPADDRESSTAG:
                    try
                    {
                        newmessage.setRecipient(drones.get(java.net.InetAddress.getByAddress(tag.value).getHostAddress()));
                    }
                    catch(java.net.UnknownHostException uhe)
                    {
                        if(Hive.getShouldTrace())
                        {
                            XTTProperties.printException(uhe);
                        }
                    }
                    break;
                case Hive.EPOCHTIMETAG:
                    newmessage.setTime(ConvertLib.getLongFromByteArray(tag.value,0,tag.length));
                    break;
                case Hive.PORTTAG:
                    filePort = ConvertLib.getIntFromByteArray(tag.value,0,2);
                    break;
                case Hive.FILESENDREQ:
                    filename = ConvertLib.getStringFromOctetByteArray(tag.value);
                    break;
                default:
                    if(Hive.getShouldTrace())
                    {
                        XTTProperties.printFail("Skipping tag(" + tag.type +") for MessageReply:\n" + ConvertLib.getHexView(tag.value));
                    }
                    break;
            }
        }
        
        newmessage.setSender(Hive.getLocalDrone());
        newmessage.setIsFromSelf(true);
        newmessage.setStatus(Message.DELIVERED);

        if((filePort > 0)&&(filename != null))
        {
            FileTransferAgent fta = new FileTransferAgent(filename,newmessage.getRecipient().getInetAddress(),filePort);
            fta.start();
            newmessage.setFile(filename);
        }
        newmessage.add();
    }
}