/*
 * $RCSfile: RadiusWorkerParent.java,v $
 * $Revision: 1.3 $
 * $Date: 2010/03/18 05:30:39 $
 * Mobixell Inc. Copyright (c) 2005
 */

package com.mobixell.xtt.radius;

/**
 * UCPConstants
 * <p>
 * Holds values used by the RadiusWorker class
 *
 * @author Roger Soder
 * @version 1.0
 */
public abstract class RadiusWorkerParent extends Thread
{
    // Packet Type
    public static final byte ACCESS_REQUEST              = 1;
    public static final byte ACCESS_ACCEPT               = 2;
    public static final byte ACCESS_REJECT               = 3;
    public static final byte ACCOUNTING_REQUEST          = 4;
    public static final byte ACCOUNTING_RESPONSE         = 5;
    public static final byte ACCESS_CHALLENGE            = 11;
    public static final byte STATUS_SERVER               = 12;// (experimental)
    public static final byte STATUS_CLIENT               = 13;// (experimental)

    // Attributes
    public static final byte USER_NAME                   =  1;
    public static final byte USER_PASSWORD               =  2;
    public static final byte CHAP_PASSWORD               =  3;
    public static final byte NAS_IP_ADDRESS              =  4;
    public static final byte NAS_PORT                    =  5;
    public static final byte SERVICE_TYPE                =  6;
    public static final byte FRAMED_PROTOCOL             =  7;
    public static final byte FRAMED_IP_ADDRESS           =  8;
    public static final byte FRAMED_IP_NETMASK           =  9;
    public static final byte FRAMED_ROUTING              = 10;
    public static final byte FILTER_ID                   = 11;
    public static final byte FRAMED_MTU                  = 12;
    public static final byte FRAMED_COMPRESSION          = 13;
    public static final byte LOGIN_IP_HOST               = 14;
    public static final byte LOGIN_SERVICE               = 15;
    public static final byte LOGIN_TCP_PORT              = 16;
    //public static final byte (UNASSIGNED)                = 17;
    public static final byte REPLY_MESSAGE               = 18;
    public static final byte CALLBACK_NUMBER             = 19;
    public static final byte CALLBACK_ID                 = 20;
    //public static final byte (UNASSIGNED)                = 21;
    public static final byte FRAMED_ROUTE                = 22;
    public static final byte FRAMED_IPX_NETWORK          = 23;
    public static final byte STATE                       = 24;
    public static final byte CLASS                       = 25;
    public static final byte VENDOR_SPECIFIC             = 26;
    public static final byte SESSION_TIMEOUT             = 27;
    public static final byte IDLE_TIMEOUT                = 28;
    public static final byte TERMINATION_ACTION          = 29;
    public static final byte CALLED_STATION_ID           = 30;
    public static final byte CALLING_STATION_ID          = 31;
    public static final byte NAS_IDENTIFIER              = 32;
    public static final byte PROXY_STATE                 = 33;
    public static final byte LOGIN_LAT_SERVICE           = 34;
    public static final byte LOGIN_LAT_NODE              = 35;
    public static final byte LOGIN_LAT_GROUP             = 36;
    public static final byte FRAMED_APPLETALK_LINK       = 37;
    public static final byte FRAMED_APPLETALK_NETWORK    = 38;
    public static final byte FRAMED_APPLETALK_ZONE       = 39;
    // (reserved for accounting)   = 40_59  ;
    public static final byte ACCT_STATUS_TYPE            = 40;
    public static final byte ACCT_DELAY_TIME             = 41;
    public static final byte ACCT_INPUT_OCTETS           = 42;
    public static final byte ACCT_OUTPUT_OCTETS          = 43;
    public static final byte ACCT_SESSION_ID             = 44;
    public static final byte ACCT_AUTHENTIC              = 45;
    public static final byte ACCT_SESSION_TIME           = 46;
    public static final byte ACCT_INPUT_PACKETS          = 47;
    public static final byte ACCT_OUTPUT_PACKETS         = 48;
    public static final byte ACCT_TERMINATE_CAUSE        = 49;
    public static final byte ACCT_MULTI_SESSION_ID       = 50;
    public static final byte ACCT_LINK_COUNT             = 51;
    // (reserved for accounting)   = 40_59  ;
    public static final byte CHAP_CHALLENGE              = 60;
    public static final byte NAS_PORT_TYPE               = 61;
    public static final byte PORT_LIMIT                  = 62;
    public static final byte LOGIN_LAT_PORT              = 63;
    
    //ipv6 Attribute
    public static final byte NAS_IPV6_ADDRESS            = 95;
    public static final byte FRAMED_INTERFACE_ID         = 96;
    public static final byte FRAMED_IPV6_PREFIX          = 97;
    public static final byte LOGIN_IPV6_HOST             = 98;
    public static final byte FRAMED_IPV6_ROUTE           = 99;
    public static final byte FRAMED_IPV6_POOL            = 100;
   
    
    
    
    

    public RadiusWorkerParent(String name)
    {
        super(name);
    }

    public String getServiceType(int type)
    {
        switch(type)
        {
            case   1: return "Login";
            case   2: return "Framed";
            case   3: return "Callback Login";
            case   4: return "Callback Framed";
            case   5: return "Outbound";
            case   6: return "Administrative";
            case   7: return "NAS Prompt";
            case   8: return "Authenticate Only";
            case   9: return "Callback NAS Prompt";
            case  10: return "Call Check";
            case  11: return "Callback Administrative";
            default : return "Unknown";
        }
    }

    public String getFramedProtocol(int type)
    {
        switch(type)
        {
            case  1: return "PPP";
            case  2: return "SLIP";
            case  3: return "AppleTalk Remote Access Protocol (ARAP)";
            case  4: return "Gandalf proprietary SingleLink/MultiLink protocol";
            case  5: return "Xylogics proprietary IPX/SLIP";
            case  6: return "X.75 Synchronous";
            default: return "Unknown";
        }
    }
    public String getFramedCompression(int type)
    {
        switch(type)
        {
            case 0: return "None";
            case 1: return "VJ TCP/IP header compression [10]";
            case 2: return "IPX header compression";
            case 3: return "Stac-LZS compression";
            default: return "Unknown";
        }
    }
    public String getLoginService(int type)
    {
        switch(type)
        {
            case 0: return "Telnet";
            case 1: return "Rlogin";
            case 2: return "TCP Clear";
            case 3: return "PortMaster (proprietary)";
            case 4: return "LAT";
            case 5: return "X25-PAD";
            case 6: return "X25-T3POS";
            case 8: return "TCP Clear Quiet (suppresses any NAS-generated connect string)";
            default : return "Unknown";
        }
    }
    public String getTerminationAction(int type)
    {
        switch(type)
        {
            case 0: return "Default";
            case 1: return "RADIUS-Request";
            default: return "Unknown";
        }
    }
    public String getNASPortType(int type)
    {
        switch(type)
        {
            case 0 : return "Async";
            case 1 : return "Sync";
            case 2 : return "ISDN Sync";
            case 3 : return "ISDN Async V.120";
            case 4 : return "ISDN Async V.110";
            case 5 : return "Virtual";
            case 6 : return "PIAFS";
            case 7 : return "HDLC Clear Channel";
            case 8 : return "X.25";
            case 9 : return "X.75";
            case 10: return "G.3 Fax";
            case 11: return "SDSL - Symmetric DSL";
            case 12: return "ADSL-CAP - Asymmetric DSL, Carrierless Amplitude Phase Modulation";
            case 13: return "ADSL-DMT - Asymmetric DSL, Discrete Multi-Tone";
            case 14: return "IDSL - ISDN Digital Subscriber Line";
            case 15: return "Ethernet";
            case 16: return "xDSL - Digital Subscriber Line of unknown type";
            case 17: return "Cable";
            case 18: return "Wireless - Other";
            case 19: return "Wireless - IEEE 802.11";
            default: return "Unknown";
        }
    }
}