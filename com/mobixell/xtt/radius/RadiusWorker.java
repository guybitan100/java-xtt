package com.mobixell.xtt.radius;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.io.IOException;

import com.mobixell.xtt.ByteArrayWrapper;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTTProperties;

public class RadiusWorker extends RadiusWorkerParent
{
    private RadiusServer parent         = null;
    private DatagramPacket packet       = null;
    private DatagramSocket socket       = null;
    private int myServerPort            = 0;
    private int myId                    = -1;
    private SocketAddress remoteAddress = null;

    InetAddress ie = null;
    int port111 = 0;

    private static ByteArrayWrapper additionalAttributes = new ByteArrayWrapper(new byte[0]);;
    private static Object additionalAttributesKey = new Object();

    public RadiusWorker(int myId,DatagramPacket packet, DatagramSocket socket, RadiusServer parent)
    {
        super("RadiusWorker-"+myId);
        this.myId=myId;
        this.packet = packet;
        this.socket = socket;
        this.parent = parent;
        myServerPort=parent.getPort();
        parent.addWorker();
    }

    public void run()
    {
        try
        {
            byte[] data = packet.getData();
            remoteAddress=packet.getSocketAddress();
            //parent.addReceivedPacket(ConvertLib.base64Encode(data,0,packet.getLength()));
            XTTProperties.printDebug("RadiusWorker(" + myServerPort + "/" + myId + "): Received:\n"+ConvertLib.getHexView(data,0,packet.getLength()));

            byte code=data[0];
            switch(code)
            {
                case ACCESS_REQUEST:
                    decodeAccessRequest(data,0,packet.getLength());
                    break;
                default:
                    XTTProperties.printFail("RadiusWorker("+myServerPort+ "/" + myId +"): unsupported command operation type: "+code);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
            }
        } catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        } finally
        {
            parent.removeWorker(this);
        }
    }

    /*
    private byte[] checkPacketAuthenticator(byte[] packet, int start, int stop) throws java.security.GeneralSecurityException
    {
        byte[] secret=parent.getSecret();
        byte[] newPacket=new byte[stop-start];
        int pointer=0;
        for(int i=start;i<stop;i++)
        {
            newPacket[pointer++]=packet[i];
        }
        for(int i=4;i<16;i++)
        {
            newPacket[i]=0;
        }
        return createAuthenticator(newPacket,secret);

    }
    */

    private byte[] createAuthenticator(byte[] packet,int packetStart,int packetStop, byte[] secret) throws java.security.GeneralSecurityException
    {
         java.security.MessageDigest digest=java.security.MessageDigest.getInstance("MD5");
         digest.update(packet,packetStart,packetStop-packetStart);
         digest.update(secret);
         byte[] authenticator=new byte[16];
         digest.digest(authenticator,0,16);
         return authenticator;
    }
    private byte[] createAuthenticator(byte[] packet, byte[] secret) throws java.security.GeneralSecurityException
    {
         return createAuthenticator(packet,0,packet.length,secret);
    }

    private byte[] createResponse(byte code, byte identifier, byte[] authenticator, byte[] attributes) throws java.security.GeneralSecurityException
    {
        byte[] secret=parent.getSecret();
        int plen=20+attributes.length;
        byte[] packetLength=ConvertLib.getByteArrayFromInt(plen,2);
        byte[] packet=new byte[plen];
        packet[0]=code;
        packet[1]=identifier;
        packet[2]=packetLength[0];
        packet[3]=packetLength[1];
        for(int i=0;i<authenticator.length;i++)
        {
            packet[i+4]=authenticator[i];
        }
        for(int i=0;i<attributes.length;i++)
        {
            packet[i+20]=attributes[i];
        }
        byte[] newAuthenticator=createAuthenticator(packet,secret);
        for(int i=0;i<newAuthenticator.length;i++)
        {
            packet[i+4]=newAuthenticator[i];
        }
        return packet;

    }

    public static byte[] getAdditionalAttributes()
    {
        synchronized(additionalAttributesKey)
        {
            return additionalAttributes.getArray();
        }
    }
    public static void addAdditionalAttributes(byte[] attributeData)
    {
        synchronized(additionalAttributesKey)
        {
            byte[] current=additionalAttributes.getArray();
            int len=attributeData.length+current.length;
            int pointer=0;
            byte[] attributes=new byte[len];
            for(int i=0;i<current.length;i++)
            {
                attributes[pointer++]=current[i];
            }
            for(int i=0;i<attributeData.length;i++)
            {
                attributes[pointer++]=attributeData[i];
            }
            additionalAttributes=new ByteArrayWrapper(attributes);
        }
    }
    public static void clearAdditionalAttributes()
    {
        synchronized(additionalAttributesKey)
        {
            additionalAttributes=new ByteArrayWrapper(new byte[0]);
        }
    }
    private void decodeAccessRequest(byte[] data, int start, int stop)
    {
        StringBuffer output=new StringBuffer();
        String storeVar="RADIUSSERVER/"+myServerPort+"/ACCESSREQUEST";
        output.append("RADIUSSERVER/"+myServerPort+"/ACCESSREQUEST/");

        try
        {
            byte[] authenticator=null;
            byte identifier=-1;
            try
            {

                int pointer=start;
                byte code=data[pointer++];
                output.append("\n AccessRequest = "+code);

                identifier=data[pointer++];
                output.append("\n identifier    = 0x"+ConvertLib.outputBytes(new byte[]{identifier},0,1));
                XTTProperties.setVariable(storeVar+"/"+"identifier",""+identifier);

                int length=ConvertLib.getIntFromByteArray(data,pointer,2);
                pointer+=2;

                output.append("\n length        = "+length);
                XTTProperties.setVariable(storeVar+"/"+"length",""+length);

                authenticator=new byte[16];
                for(int i=0;i<16;i++)
                {
                    authenticator[i]=data[pointer++];
                }
                String authenticatorString=ConvertLib.outputBytes(authenticator,0,16);
                output.append("\n authenticator = "+authenticatorString);
                XTTProperties.setVariable(storeVar+"/"+"authenticator",""+authenticatorString);
                /*
                byte[] checkedAuth=checkPacketAuthenticator(data,start,stop);
                String checkedAuthString=ConvertLib.outputBytes(checkedAuth,0,16);;
                if(!authenticatorString.equals(checkedAuthString))
                {
                    output.append("\n checkedAuth   = "+ConvertLib.outputBytes(checkedAuth,0,16));
                    XTTProperties.printFail("RadiusWorker("+myServerPort+ "/" + myId +"): Authenticators do not match!");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }*/

                output.append("\n Attributes: ");
                storeVar="RADIUSSERVER/"+myServerPort+"/ACCESSREQUEST/ATTRIBUTES";
                while(pointer<stop && pointer<data.length)
                {
                    byte attributetype=data[pointer++];
                    int attributelength=ConvertLib.getIntFromByteArray(data,pointer++,1)-2;


                    switch(attributetype)
                    {
                        case USER_NAME               :
                            String username=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   User-Name         = "+username);
                            XTTProperties.setVariable(storeVar+"/"+"User-Name",""+username);
                            break;
                        case USER_PASSWORD           :
                            String userpassword=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   User-Password     = "+userpassword);
                            XTTProperties.setVariable(storeVar+"/"+"User-Password",""+userpassword);
                            break;
                        case CHAP_PASSWORD           :
                            byte chapident=data[pointer++];
                            String chappassword=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Chap-Password     = 0x"+ConvertLib.outputBytes(new byte[]{chapident},0,1)+" - "+chappassword);
                            XTTProperties.setVariable(storeVar+"/"+"Chap-Password",""+ConvertLib.outputBytes(new byte[]{chapident},0,1)+" - "+chappassword);
                            break;
                        case NAS_IP_ADDRESS          :
                            String nasipaddress=ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1);
                            output.append("\n   NAS-IP-Address    = "+nasipaddress);
                            XTTProperties.setVariable(storeVar+"/"+"NAS-IP-Address",""+nasipaddress);
                            break;
                        case NAS_PORT                :
                            int nasport=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   NAS-Port          = "+nasport);
                            XTTProperties.setVariable(storeVar+"/"+"NAS-Port",""+nasport);
                            break;
                        case SERVICE_TYPE            :
                            int servicetype=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Service-Type      = "+servicetype+" ("+getServiceType(servicetype)+")");
                            XTTProperties.setVariable(storeVar+"/"+"Service-Type",""+servicetype);
                            break;
                        case FRAMED_PROTOCOL         :
                            int framedprotocol=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Protocol   = "+framedprotocol+" ("+getFramedProtocol(framedprotocol)+")");
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Protocol",""+framedprotocol);
                            break;
                        case FRAMED_IP_ADDRESS       :
                            String framedipaddress=ConvertLib.getIntFromByteArray(data,pointer++,1)
                                              +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                              +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                              +"."+ConvertLib.getIntFromByteArray(data,pointer++,1);
                            output.append("\n   Framed-IP-Address = "+framedipaddress);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-IP-Address",""+framedipaddress);
                            break;
                        case FRAMED_IP_NETMASK       :
                            String nasipnetmask=ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1);
                            output.append("\n   Framed-IP-Netmask = "+nasipnetmask);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-IP-Netmask",""+nasipnetmask);
                            break;
                        case FRAMED_ROUTING          :
                            int framedrouting=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Routing    = "+framedrouting);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Routing",""+framedrouting);
                            break;
                        case FILTER_ID               :
                            String filterid=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Filter-ID         = "+filterid);
                            XTTProperties.setVariable(storeVar+"/"+"Filter-ID",""+filterid);
                            break;
                        case FRAMED_MTU              :
                            int framedmtu=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-MTU        = "+framedmtu);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-MTU",""+framedmtu);
                            break;
                        case FRAMED_COMPRESSION      :
                            int framedcompression=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Compression= "+framedcompression+" ("+getFramedCompression(framedcompression)+")");
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Compression",""+framedcompression);
                            break;
                        case LOGIN_IP_HOST           :
                            String loginiphost=ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1)
                                           +"."+ConvertLib.getIntFromByteArray(data,pointer++,1);
                            output.append("\n   Login-IP-Host     = "+loginiphost);
                            XTTProperties.setVariable(storeVar+"/"+"Login-IP-Host",""+loginiphost);
                            break;
                        case LOGIN_SERVICE           :
                            int loginservice=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-Service     = "+loginservice+" ("+getLoginService(loginservice)+")");
                            XTTProperties.setVariable(storeVar+"/"+"Login-Service",""+loginservice);
                            break;
                        case LOGIN_TCP_PORT          :
                            int logintcpport=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-TCP-Port    = "+logintcpport);
                            XTTProperties.setVariable(storeVar+"/"+"Login-TCP-Port",""+logintcpport);
                            break;
                        case REPLY_MESSAGE           :
                            String replymessage=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Reply-Message     = "+replymessage);
                            XTTProperties.setVariable(storeVar+"/"+"Reply-Message",""+replymessage);
                            break;
                        case CALLBACK_NUMBER         :
                            String callbacknumber=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Callback-Number   = "+callbacknumber);
                            XTTProperties.setVariable(storeVar+"/"+"Callback-Number",""+callbacknumber);
                            break;
                        case CALLBACK_ID             :
                            String callbackid=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Callback-ID       = "+callbackid);
                            XTTProperties.setVariable(storeVar+"/"+"Callback-ID",""+callbackid);
                            break;
                        case FRAMED_ROUTE            :
                            String framedroute=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Route      = "+framedroute);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Route",""+framedroute);
                            break;
                        case FRAMED_IPX_NETWORK      :
                            String framedipxnetwork=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed_IPX_Network= "+framedipxnetwork);
                            XTTProperties.setVariable(storeVar+"/"+"Framed_IPX_Network",""+framedipxnetwork);
                            break;
                        case STATE                   :
                            String radiusstate=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   State             = "+radiusstate);
                            XTTProperties.setVariable(storeVar+"/"+"State",""+radiusstate);
                            break;
                        case CLASS                   :
                            String radiusclass=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Class             = "+radiusclass);
                            XTTProperties.setVariable(storeVar+"/"+"Class",""+radiusclass);
                            break;
                        case VENDOR_SPECIFIC         :
                            int finalpointer=pointer+attributelength;
                            output.append("\n   Vendor-Specific:");
                            String vendorID=ConvertLib.outputBytes(data,pointer,4);
                            pointer=pointer+4;
                            output.append("\n     Vendor-ID       = "+vendorID);
                            XTTProperties.setVariable(storeVar+"/VENDOR-SPECIFIC/"+"Vendor-ID",""+vendorID);
                            int vtype=0;
                            int vlength=0;
                            String vbytes=null;
                            while(pointer<finalpointer)
                            {
                                vtype=ConvertLib.getIntFromByteArray(data,pointer++,1);
                                vlength=ConvertLib.getIntFromByteArray(data,pointer++,1)-2;
                                vbytes=ConvertLib.outputBytes(data,pointer,vlength);
                                pointer+=vlength;
                                output.append("\n     "+vtype+" = "+vbytes);
                                XTTProperties.setVariable(storeVar+"/VENDOR-SPECIFIC/"+""+vtype,""+vbytes);
                            }
                            pointer=finalpointer;
                            break;
                        case SESSION_TIMEOUT         :
                            int sessiontimeout=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Session-Timeout   = "+sessiontimeout);
                            XTTProperties.setVariable(storeVar+"/"+"Session-Timeout",""+sessiontimeout);
                            break;
                        case IDLE_TIMEOUT            :
                            int idletimeout=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Idle-Timeout      = "+idletimeout);
                            XTTProperties.setVariable(storeVar+"/"+"Idle-Timeout",""+idletimeout);
                            break;
                        case TERMINATION_ACTION      :
                            int terminationaction=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Termination-Action= "+terminationaction+" ("+getTerminationAction(terminationaction)+")");
                            XTTProperties.setVariable(storeVar+"/"+"Termination-Action",""+terminationaction);
                            break;
                        case CALLED_STATION_ID       :
                            String calledstationid=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Called-Station-ID = "+calledstationid);
                            XTTProperties.setVariable(storeVar+"/"+"Called-Station-ID",""+calledstationid);
                            break;
                        case CALLING_STATION_ID      :
                            String callingstationid=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Calling-Station-ID= "+callingstationid);
                            XTTProperties.setVariable(storeVar+"/"+"Calling-Station-ID",""+callingstationid);
                            break;
                        case NAS_IDENTIFIER          :
                            String nasidentifier=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   NAS-Identifier    = "+nasidentifier);
                            XTTProperties.setVariable(storeVar+"/"+"NAS-Identifier",""+nasidentifier);
                            break;
                        case PROXY_STATE             :
                            String proxystate=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Proxy-State       = "+proxystate);
                            XTTProperties.setVariable(storeVar+"/"+"Proxy-State",""+proxystate);
                            break;
                        case LOGIN_LAT_SERVICE       :
                            String loginlatservice=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-LAT-Service = "+loginlatservice);
                            XTTProperties.setVariable(storeVar+"/"+"Login-LAT-Service",""+loginlatservice);
                            break;
                        case LOGIN_LAT_NODE          :
                            String loginlatnode=ConvertLib.getStringFromOctetByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-LAT-Node    = "+loginlatnode);
                            XTTProperties.setVariable(storeVar+"/"+"Login-LAT-Node",""+loginlatnode);
                            break;
                        case LOGIN_LAT_GROUP         :
                            String loginlatgroup=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-LAT-Group   = "+loginlatgroup);
                            XTTProperties.setVariable(storeVar+"/"+"Login-LAT-Group",""+loginlatgroup);
                            break;
                        case FRAMED_APPLETALK_LINK   :
                            int framedappletalklink=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Appletalk-Link= "+framedappletalklink);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Appletalk-Link",""+framedappletalklink);
                            break;
                        case FRAMED_APPLETALK_NETWORK:
                            int framedappletalknetwork=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Appletalk-Network= "+framedappletalknetwork);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Appletalk-Network",""+framedappletalknetwork);
                            break;
                        case FRAMED_APPLETALK_ZONE   :
                            String framedappletalkzone=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Appletalk-Zone= "+framedappletalkzone);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Appletalk-Zone",""+framedappletalkzone);
                            break;
                        case CHAP_CHALLENGE          :
                            String chapchallenge=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   CHAP-Challenge    = "+chapchallenge);
                            XTTProperties.setVariable(storeVar+"/"+"CHAP-Challenge",""+chapchallenge);
                            break;
                        case NAS_PORT_TYPE           :
                            int nasporttype=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   NAS-Port-Type     = "+nasporttype+" ("+getNASPortType(nasporttype)+")");
                            XTTProperties.setVariable(storeVar+"/"+"NAS-Port-Type",""+nasporttype);
                            break;
                        case PORT_LIMIT              :
                            int portlimit=ConvertLib.getIntFromByteArray(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Port-Limit        = "+portlimit);
                            XTTProperties.setVariable(storeVar+"/"+"Port-Limit",""+portlimit);
                            break;
                        case LOGIN_LAT_PORT          :
                            String loginlatport=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Login-LAT-Port    = "+loginlatport);
                            XTTProperties.setVariable(storeVar+"/"+"Login-LAT-Port",""+loginlatport);
                            break;
                        case NAS_IPV6_ADDRESS         :
                      /*  String nasipv6address=
                        ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                        +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1);
                     */
                        String nasipv6address=
                            ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1)
                            +":"+ConvertLib.getHexStringFromByteArray(data,pointer++,1)+ ConvertLib.getHexStringFromByteArray(data,pointer++,1);

                            output.append("\n   NAS-IPv6-Address    = "+nasipv6address);
                            XTTProperties.setVariable(storeVar+"/"+"NAS-IPv6-Address",""+nasipv6address);
                            break;
                        case FRAMED_INTERFACE_ID          :
                            String framedinterfaceid=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-Interface-Id    = "+framedinterfaceid);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-Interface-Id",""+framedinterfaceid);
                            break;
                        case FRAMED_IPV6_PREFIX          :
                            String framedipv6prefix=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-IPv6-Prefix    = "+framedipv6prefix);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-IPv6-Prefix",""+framedipv6prefix);
                            break;
                        case LOGIN_IPV6_HOST          :
                            String loginipv6host=    ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1)
                            +":"+ConvertLib.getStringFromOctetByteArray(data,pointer++,1)+ ConvertLib.getStringFromOctetByteArray(data,pointer++,1);
                            pointer+=attributelength;
                            output.append("\n   Login-IPv6-Host    = "+loginipv6host);
                            XTTProperties.setVariable(storeVar+"/"+"Login-IPv6-Host",""+loginipv6host);
                            break;
                        case FRAMED_IPV6_ROUTE          :
                            String framedipv6route=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-IPv6-Route    = "+framedipv6route);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-IPv6-Route",""+framedipv6route);
                            break;
                        case FRAMED_IPV6_POOL          :
                            String framedipv6pool=ConvertLib.outputBytes(data,pointer,attributelength);
                            pointer+=attributelength;
                            output.append("\n   Framed-IPv6-Pool    = "+framedipv6pool);
                            XTTProperties.setVariable(storeVar+"/"+"Framed-IPv6-Pool",""+framedipv6pool);
                            break;
                        default:
                            XTTProperties.printFail("RadiusWorker("+myServerPort+ "/" + myId +"): unsupported attribute type: "+attributetype);
                            XTTProperties.setTestStatus(XTTProperties.FAILED);
                            return;
                    }
                }
                
            } finally
            {
                XTTProperties.printDebug("RadiusWorker("+myServerPort+ "/" + myId +"): "+output.toString());
            }
            byte[] response=createResponse(ACCESS_ACCEPT,identifier,authenticator,getAdditionalAttributes());
            
            //synchronized(socket)
            //{
                XTTProperties.printDebug("RadiusWorker(" + myServerPort + "/" + myId + "): Sending to: "+remoteAddress+"\n"+ConvertLib.getHexView(response,0,response.length));
                DatagramPacket p=new DatagramPacket(response,0,response.length,remoteAddress);
                socket.send(p);
            //}
                
        } catch (IOException ioe)
        {
            XTTProperties.printFail("RadiusWorker("+myServerPort+ "/" + myId +"): error sending response "+ioe.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ioe);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        } catch (java.security.GeneralSecurityException gse)
        {
            XTTProperties.printFail("RadiusWorker("+myServerPort+ "/" + myId +"): encryption error "+gse.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(gse);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
    }


    public static final String tantau_sccsid = "@(#)$Id: RadiusWorker.java,v 1.5 2010/03/18 05:30:39 rajesh Exp $";
}
