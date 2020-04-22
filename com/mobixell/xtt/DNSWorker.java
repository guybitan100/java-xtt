package com.mobixell.xtt;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.Vector;

/**
* DNSWorker takes the UDP packet and parses it.
* For more information on the packet format see RFC 1034 and 1035 and 3596 (for IPv6).
* For Enum DNS see RFC 2916 then for NAPTR see RFC 2915.
*
*/
public class DNSWorker extends Thread
{
    //QTYPE
    private static final int A = 1;
    private static final int AAAA = 28;
    private static final int NAPTR = 35;
    private static int responseDelay=0;
    public static void setResponseDelay(int delay){responseDelay=delay;}
    

    DatagramPacket data = null;
    DatagramSocket socket = null;

    public DNSWorker(DatagramPacket packet, DatagramSocket socket)
    {
        data = packet;
        this.socket = socket;
    }

    public void run()
    {
        try
        {
            byte[] query = data.getData();
            //We only read DNS packets up to 1024 bytes.
            byte[] response = new byte[1024];

            /*
                Packet Header Format:
                                                1  1  1  1  1  1
                  0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |                      ID                       |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |                    QDCOUNT                    |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |                    ANCOUNT                    |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |                    NSCOUNT                    |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                |                    ARCOUNT                    |
                +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
            */

            String ID = "";
            int qr = -1; //Query Response
            int opCode = -1;
            int aa = -1; //Is this server the authority for the domain
            int tc = -1;
            int rd = -1; //Recursion desired
            int ra = -1; //Recursion available
            int z = -1; //3 bits reserved


            int questions = -1;

            //String qname = "";
            Vector<String> qname = new Vector<String>();
            int qtype = -1;
            int qclass = -1;

            ID = byte2hex(query[0]) + byte2hex(query[1]);

            qr = (query[2] & 0x80) >> 7;

            opCode = (query[2] & 0x78) >> 3;

            aa = (query[2] & 0x04) >> 2;

            tc = (query[2] & 0x02) >> 1;

            rd = (query[2] & 0x01);

            ra = (query[3] & 0x80) >> 7;

            //ignore the z bits

            //4 and 5 have number of questions
            questions = getIntFromByteArray(query,4,2);
            //6 and 7 contain number of answers
            //8 and 9 contain number of name server records
            //10 and 11 contain number of additional resources


            if(qr == 0) //This is a request not a response
            {

                /*
                    Question Format:
                                                    1  1  1  1  1  1
                      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                                               |
                    /                     QNAME                     /
                    /                                               /
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     QTYPE                     |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     QCLASS                    |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                */
                int pos = 0;
                int length = 0;
                //Read the QNAME into a Vector<String> containing the parts.
                do
                {
                    length = query[12+pos];
                    qname.add(getStringFromOctetByteArray(query,13+pos,length));
                    pos += length + 1;
                }
                while(length != 0);
                //Remove the last part, since it's empty.
                qname.remove(qname.size()-1);

                if(XTTProperties.printDebug(null)&&DNSServer.notExcludedFromDebug(qname))
                {
                    XTTProperties.printDebug("DNSWorker: DNS Incoming message from '" + data.getSocketAddress().toString() + "':\n"+ConvertLib.getHexView(query,0,data.getLength()));
                }

                //Now we know the size of the request since we know the length of the QNAME
                int end = 12 + pos;

                /*
                    QTYPE           value and meaning
                    A               1 a host address
                    NS              2 an authoritative name server
                    MD              3 a mail destination (Obsolete - use MX)
                    MF              4 a mail forwarder (Obsolete - use MX)
                    CNAME           5 the canonical name for an alias
                    SOA             6 marks the start of a zone of authority
                    MB              7 a mailbox domain name (EXPERIMENTAL)
                    MG              8 a mail group member (EXPERIMENTAL)
                    MR              9 a mail rename domain name (EXPERIMENTAL)
                    NULL            10 a null RR (EXPERIMENTAL)
                    WKS             11 a well known service description
                    PTR             12 a domain name pointer
                    HINFO           13 host information
                    MINFO           14 mailbox or mail list information
                    MX              15 mail exchange
                    TXT             16 text strings
             (IPv6) AAAA            28 an Internet class that stores a single IPv6 address
                    NAPTR           35 Naming Authority Pointer (NAPTR)
                    AXFR            252 A request for a transfer of an entire zone
                    MAILB           253 A request for mailbox-related records (MB, MG or MR)
                    MAILA           254 A request for mail agent RRs (Obsolete - see MX)
                    *               255 A request for all records
                */
                qtype = getIntFromByteArray(query, end ,2);

                /*
                    QCLASS
                    IN              1 the Internet
                    CS              2 the CSNET class (Obsolete - used only for examples in some obsolete RFCs)
                    CH              3 the CHAOS class
                    HS              4 Hesiod [Dyer 87]

                    *               255 any class
                */
                qclass = getIntFromByteArray(query, end + 2,2);

                end = end + 4;


                /*
                    Resource Record Format:
                                                    1  1  1  1  1  1
                      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                                               |
                    /                                               /
                    /                      NAME                     /
                    |                                               |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                      TYPE                     |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     CLASS                     |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                      TTL                      |
                    |                                               |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                   RDLENGTH                    |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
                    /                     RDATA                     /
                    /                                               /
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                */

                //Turn our Vector<String> into one String.
                String qnameFull = "";
                for(int j=0;j<(qname.size()-1);j++)
                {
                    qnameFull+=(String)qname.get(j)+".";
                }
                qnameFull+=qname.get(qname.size()-1);

                int timeToLive = XTTProperties.getQuietIntProperty("DNSSERVER/TTL");;
                byte[] ttl = new byte[0];
                byte[] ip = new byte[0];

                switch(qtype)
                {
                    case A:
                        XTTProperties.printDebug("DNSWorker: Handling IPv4 Request");
                        response = query; //You need to send back most of the info anyway

                        response[2] = (byte)0x81; //Set QR and RD to 1 (or on).
                        response[3] = (byte)0x80; //Set RA to 1 (or on).

                        response[7] = (byte)0x01; //We need to show we're sending one answer back, set ANCOUNT to 1 (only the 2nd byte though, could expand to set response[6] if needed).

                        //Start setting the "Resource Record" stuff.
                        response[end] = (byte) 0xc0; //Set the name back to the QNAME via an Offset, so we don't need to re-write it.
                        response[end+1] = (byte) 0x0c; //To position 12 i.e. the domain name in the question

                        response[end+3] = (byte) 0x01; //TYPE: a host address

                        response[end+5] = (byte) 0x01; //CLASS: the Internet

                        //Set TTL
                        if(timeToLive < 0)timeToLive=0;
                        ttl = ConvertLib.getByteArrayFromInt(timeToLive,4);
                        response[end+6] = ttl[0];
                        response[end+7] = ttl[1];
                        response[end+8] = ttl[2];
                        response[end+9] = ttl[3];

                        //add the RDATA length for a class A IN address
                        response[end+11] = (byte) 0x04; // 4 bytes

                        try
                        {
                            //Only do the java lookup if recursion is desired.
                            if(rd == 1)
                                ip = DNSEntry.resolveAddressToIp4(qname);
                            else
                                ip = DNSEntry.getRawIp4Address(qname);

                            response[end+12] = ip[0];
                            response[end+13] = ip[1];
                            response[end+14] = ip[2];
                            response[end+15] = ip[3];

                            end = end + 16;
                        }
                        catch(UnknownHostException uhe)
                        {
                            XTTProperties.printWarn("DNSWorker: No IP address for '"+ qnameFull+"'. Sending \'not found\' response.");

                            response[7] = (byte)0x00; //The IP lookup failed, so send NO answers back.
                            response[3] |= (byte)0x03;
                        }
                        break;
                    case AAAA:
                        //For setting the part, see the 'A' case above.
                        XTTProperties.printDebug("DNSWorker: Handling IPv6 Request");
                        response = query; //You need to send back most of the info anyway

                        response[2] = (byte)0x81;
                        response[3] = (byte)0x80;

                        response[7] = (byte)0x01; //We need to show we're sending one answer back

                        response[end] = (byte) 0xc0; //Offset
                        response[end+1] = (byte) 0x0c; //To position 12 i.e. the domain name in the question

                        response[end+3] = (byte) 0x1C; //an IPv6 host address

                        response[end+5] = (byte) 0x01; //the Internet

                        //ttl: default 0

                        if(timeToLive < 0)timeToLive=0;
                        ttl = ConvertLib.getByteArrayFromInt(timeToLive,4);
                        response[end+6] = ttl[0];
                        response[end+7] = ttl[1];
                        response[end+8] = ttl[2];
                        response[end+9] = ttl[3];

                        //add the RDATA length for a class AAAA IN address
                        response[end+11] = (byte) 0x10; //16 bytes


                        try
                        {
                            if(rd == 1)
                                ip = DNSEntry.resolveAddressToIp6(qname);
                            else
                                ip = DNSEntry.getRawIp6Address(qname);

                            response[end+12] = ip[0];
                            response[end+13] = ip[1];
                            response[end+14] = ip[2];
                            response[end+15] = ip[3];
                            response[end+16] = ip[4];
                            response[end+17] = ip[5];
                            response[end+18] = ip[6];
                            response[end+19] = ip[7];
                            response[end+20] = ip[8];
                            response[end+21] = ip[9];
                            response[end+22] = ip[10];
                            response[end+23] = ip[11];
                            response[end+24] = ip[12];
                            response[end+25] = ip[13];
                            response[end+26] = ip[14];
                            response[end+27] = ip[15];
                            end = end + 28;
                        }
                        catch(UnknownHostException uhe)
                        {
                            XTTProperties.printWarn("DNSWorker: No IP address for '"+ qnameFull+"'. Sending \'not found\' response.");

                            response[7] = (byte)0x00; //The IP lookup failed, so send NO answers back.
                            response[3] |= (byte)0x03;
                        }
                        break;
                    case NAPTR:
                        XTTProperties.printDebug("DNSWorker: Handling NAPTR Request");
                        System.arraycopy(query,0,response,0,response.length); //You need to send back most of the info anyway

                        response[2] = (byte)0x81;
                        response[3] = (byte)0x80;

                        try
                        {
                            Vector<ByteArrayWrapper> naptrResourceRecods = DNSEntry.getNaptr(qname);
                            System.arraycopy(ConvertLib.getByteArrayFromInt(naptrResourceRecods.size(),1),0,response,7,1);//We need to show how many answers we're sending back

                            for(int i = 0;i<naptrResourceRecods.size();i++)
                            {
                                byte[] naptr = naptrResourceRecods.elementAt(i).getArray();

                                response[end] = (byte) 0xc0; //Offset
                                response[end+1] = (byte) 0x0c; //To position 12 i.e. the domain name in the question

                                response[end+3] = (byte) 0x23; //a NAPTR resource

                                response[end+5] = (byte) 0x01; //the Internet

                                //ttl: default 0

                                if(timeToLive < 0)timeToLive=0;
                                ttl = ConvertLib.getByteArrayFromInt(timeToLive,4);
                                response[end+6] = ttl[0];
                                response[end+7] = ttl[1];
                                response[end+8] = ttl[2];
                                response[end+9] = ttl[3];

                                //add the RDATA length for the NAPTR
                                System.arraycopy(ConvertLib.getByteArrayFromInt(naptr.length,2),0,response,end+10,2);

                                System.arraycopy(naptr,0,response,end+12,naptr.length);

                                end = end + 12 + naptr.length;
                            }
                        }
                        catch(UnknownHostException uhe)
                        {
                            XTTProperties.printWarn("DNSWorker: No NAPTR for '"+ qnameFull+"'. Sending \'not found\' response.");

                            response[7] = (byte)0x00; //The IP lookup failed, so send NO answers back.
                            response[3] |= (byte)0x03;
                        }
                        break;
                    default:
                        XTTProperties.printFail("DNSWorker: Can't do fancy lookups yet, pretending the address doesn't exist");

                        response = query; //You need to send back most of the info anyway

                        response[2] = (byte)0x81;
                        response[3] = (byte)0x80;

                        response[7] = (byte)0x01; //We need to show we're sending one answer back

                        response[end] = (byte) 0xc0; //Offset
                        response[end+1] = (byte) 0x0c; //To position 12 i.e. the domain name in the question

                        response[end+3] = (byte) 0x01; //a host address

                        response[end+5] = (byte) 0x01; //the Internet

                        //ttl 24 hours or 86400 seconds
                        response[end+7] = (byte) 0x01;
                        response[end+8] = (byte) 0x51;
                        response[end+9] = (byte) 0x80;

                        //add the RDATA length for a class A IN address
                        response[end+11] = (byte) 0x04;
                        response[7] = (byte)0x00; //The IP lookup failed, so send NO answers back.
                        response[3] |= (byte)0x03;
                        break;
                }

                //System.out.println("" + (end));
                if(responseDelay>0)
                {
                    if(XTTProperties.printDebug(null)&&DNSServer.notExcludedFromDebug(qname))
                    {
                        XTTProperties.printDebug("DNSWorker: delaying Outgoing message to '" + data.getSocketAddress().toString()+" by "+responseDelay+"ms");
                    }
                    Thread.sleep(responseDelay);
                }

                //System.out.println("" + (end));
                if(XTTProperties.printDebug(null)&&DNSServer.notExcludedFromDebug(qname))
                {
                    XTTProperties.printDebug("DNSWorker: DNS Outgoing message to '" + data.getSocketAddress().toString() + "':\n"+ConvertLib.getHexView(response,0,end));
                }

                DatagramPacket responsePacket = new DatagramPacket(response,end,data.getAddress(),data.getPort());

                socket.send(responsePacket);
            }
            else
            {
                //System.out.println(data.getLength());
                XTTProperties.printDebug("DNSWorker: DNS Incoming message (NOT A REQUEST):\n"+ConvertLib.getHexView(query,0,data.getLength()));
            }
       }
       catch (Exception e)
       {
           if(XTTProperties.printDebug(null))
           {
               XTTProperties.printException(e);
           }
       }

    }

    public static String byte2hex(byte b)
    {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
        '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        //The magic bit!
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        return "" + hexChars[high]+hexChars[low];
    }

    public static int getIntFromByteArray(byte[] bytes, int start, int len) throws Exception
    {
        if (len > 4)
        {
            throw new ArrayIndexOutOfBoundsException(len + " to long for integer conversion");
        }
        byte[] barray = {0,0,0,0};
        int retVal    = 0x00000000;

        for (int i = len-1; i >= 0; i--)
        {
            //System.out.println(i+"+4-"+len+"="+(i+3-len)+" s:"+start+" i:"+i+" l:"+bytes.length);
            barray[i+4-len] = bytes[start + i];
        }

        retVal |= ((((int) barray[0]) << 24) & 0xff000000);
        retVal |= ((((int) barray[1]) << 16) & 0x00ff0000);
        retVal |= ((((int) barray[2]) << 8)  & 0x0000ff00);
        retVal |= ( ((int) barray[3])        & 0x000000ff);

        return retVal;
    }

    public static String getStringFromOctetByteArray(byte[] bytes, int start, int datalength)
    {
        StringBuffer tmp=new StringBuffer();
        for(int i=start;i<bytes.length&&(i-start<datalength);i++)
        {
            tmp.append((char)bytes[i]);
        }
        return tmp.toString();
    }
    public static final String tantau_sccsid = "@(#)$Id: DNSWorker.java,v 1.14 2009/10/22 13:08:59 rsoder Exp $";
}
