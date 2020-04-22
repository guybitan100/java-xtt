package com.mobixell.xtt.diameter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.ByteArrayWrapper;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
/**
 * <p>Diameter_AVP</p>
 * @author Roger Soder
 * @version $Id: Diameter_AVP.java,v 1.8 2011/04/18 12:17:18 rajesh Exp $
 */
public class Diameter_AVP implements DiameterConstants
{
    public int overrideavpcode  = 0;
    public int avpcode          = 0;
    public int datalength       = 0;
    public int avpflags         = 0;
    public int vendorID         = 0;
    public int avpNumber        = 0;
    public boolean mandatory    = false;
    public boolean protectedf   = false;
    public boolean vendorspec   = false;

    public String avpname       = null;
    public String avpSflags     = "";

    public String output=null;

    private Vector<Diameter_AVP> groupedAVPs=new Vector<Diameter_AVP>();
    public Vector<Diameter_AVP> getGroupedAVPs()
    {
        return groupedAVPs;
    }
    public void setGroupedAVPs(Vector<Diameter_AVP> groupedAVPs)
    {
        this.groupedAVPs=groupedAVPs;
    }

    public String data="";
    long number=0;

    public int decode(byte[] body, int startpointer, int avpNumber, Set<String> extendedStoreVar, Vector<String> extendedStoreVarValue, HashMap<Integer, Vector<Diameter_AVP>> avpMap)
    {
        this.avpNumber=avpNumber;

        StringBuffer output=new StringBuffer("");
        int pointer=startpointer;
        try
        {
            avpcode         =ConvertLib.getIntFromByteArray(body, pointer,4);
            avpname=DiameterWorkerServer.getAVPName(avpcode);
            output.append("\navpcode         ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(body, pointer,4,2),8," ") +"="
                +ConvertLib.addPrefixToString(avpcode+"",3," ")+" ("+avpname+")");
            pointer+=4;

            avpflags    =ConvertLib.getIntFromByteArray(body, pointer,1);
            mandatory   =((avpflags&AFLAG_MANDATORY)==AFLAG_MANDATORY);
            if(mandatory)avpSflags=avpSflags+"M";
            protectedf  =((avpflags&AFLAG_PROTECTED)==AFLAG_PROTECTED);
            if(protectedf)avpSflags=avpSflags+"P";
            vendorspec  =((avpflags&AFLAG_VENDOR)==AFLAG_VENDOR);
            if(vendorspec)
            {
                avpSflags=avpSflags+"V";
            }
            output.append("\navpflags        ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(body, pointer,1,2),8," ") +"="
                +ConvertLib.addPrefixToString(avpflags+"",3," ")+" ("+avpSflags+")");
            pointer+=1;


            datalength      =ConvertLib.getIntFromByteArray(body, pointer,3);
            output.append("\ndatalength      ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(body, pointer,3,2),8," ") +"="+datalength);
            pointer+=3;

            if(vendorspec)
            {
                vendorID      =ConvertLib.getIntFromByteArray(body,pointer,4);
                output.append("\nvendorID        ="+ ConvertLib.outputBytes(body, pointer,4,2)+"="+vendorID);
                pointer+=4;
            }
            int avpheadlength=(pointer-startpointer);

            switch(avpcode)
            {
                case HOST_IP_ADDRESS         : // Address
                case MIP_MOBILE_NODE_ADDRESS : // Address
                case MIP_HOME_AGENT_ADDRESS  : // Address
                    data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    break;
                case DESTINATION_HOST              : // DiamIdent
                case DESTINATION_REALM             : // DiamIdent
                case ERROR_REPORTING_HOST          : // DiamIdent
                case ORIGIN_HOST                   : // DiamIdent
                case ORIGIN_REALM                  : // DiamIdent
                case PROXY_HOST                    : // DiamIdent
                case ROUTE_RECORD                  : // DiamIdent
                case MIP_CANDIDATE_HOME_AGENT_HOST : // DiamIdent
                case MIP_HOME_AGENT_HOST           : // DiamIdent
                    data=ConvertLib.createString(body,pointer,datalength-avpheadlength);
                    break;
                case REDIRECT_HOST                  : //    DiamURI
                case SIP_ACCOUNTING_SERVER_URI      : //    DiamURI
                case SIP_CREDIT_CONTROL_SERVER_URI  : //    DiamURI
                    data=ConvertLib.createString(body,pointer,datalength-avpheadlength);
                    break;
                case DISCONNECT_CAUSE                   : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="REBOOTING";break;
                        case 1 :data="BUSY";break;
                        case 2 :data="DO_NOT_WANT_TO_TALK_TO_YOU";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case REDIRECT_HOST_USAGE                : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DONT_CACHE";break;
                        case 1 :data="ALL_SESSION";break;
                        case 2 :data="ALL_REALM";break;
                        case 3 :data="REALM_AND_APPLICATION";break;
                        case 4 :data="ALL_APPLICATION";break;
                        case 5 :data="ALL_HOST";break;
                        case 6 :data="ALL_USER";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case AUTH_REQUEST_TYPE                  : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="AUTHENTICATE_ONLY";break;
                        case 2 :data="AUTHORIZE_ONLY";break;
                        case 3 :data="AUTHORIZE_AUTHENTICATE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case AUTH_SESSION_STATE                 : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="STATE_MAINTAINED";break;
                        case 1 :data="NO_STATE_MAINTAINED";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case RE_AUTH_REQUEST_TYPE               : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="AUTHORIZE_ONLY";break;
                        case 1 :data="AUTHORIZE_AUTHENTICATE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case TERMINATION_CAUSE                  : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="DIAMETER_LOGOUT";break;
                        case 2 :data="DIAMETER_SERVICE_NOT_PROVIDED";break;
                        case 3 :data="DIAMETER_BAD_ANSWER";break;
                        case 4 :data="DIAMETER_ADMINISTRATIVE";break;
                        case 5 :data="DIAMETER_LINK_BROKEN";break;
                        case 6 :data="DIAMETER_AUTH_EXPIRED";break;
                        case 7 :data="DIAMETER_USER_MOVED";break;
                        case 8 :data="DIAMETER_SESSION_TIMEOUT";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SESSION_SERVER_FAILOVER            : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="REFUSE_SERVICE";break;
                        case 1 :data="TRY_AGAIN ";break;
                        case 2 :data="ALLOW_SERVICE";break;
                        case 3 :data="TRY_AGAIN_ALLOW_SERVICE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ACCOUNTING_RECORD_TYPE             : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="EVENT_RECORD";break;
                        case 2 :data="START_RECORD ";break;
                        case 3 :data="START_RECORD";break;
                        case 4 :data="STOP_RECORD";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ACCOUNTING_REALTIME_REQUIRED       : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="DELIVER_AND_GRANT";break;
                        case 2 :data="GRANT_AND_STORE ";break;
                        case 3 :data="GRANT_AND_LOSE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CC_REQUEST_TYPE                    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="INITIAL_REQUEST";break;
                        case 2 :data="UPDATE_REQUEST";break;
                        case 3 :data="TERMINATION_REQUEST";break;
                        case 4 :data="EVENT_REQUEST";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CC_SESSION_FAILOVER                : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="FAILOVER_NOT_SUPPORTED";break;
                        case 1 :data="FAILOVER_SUPPORTED ";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CHECK_BALANCE_RESULT               : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="ENOUGH_CREDIT";break;
                        case 1 :data="NO_CREDIT ";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CREDIT_CONTROL                     : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="CREDIT_AUTHORIZATION";break;
                        case 1 :data="RE_AUTHORIZATION ";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CREDIT_CONTROL_FAILURE_HANDLING    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="TERMINATE";break;
                        case 1 :data="CONTINUE ";break;
                        case 2 :data="RETRY_AND_TERMINATE ";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case DIRECT_DEBITING_FAILURE_HANDLING   : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="TERMINATE_OR_BUFFER";break;
                        case 1 :data="CONTINUE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case TARIFF_CHANGE_USAGE                : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="UNIT_BEFORE_TARIFF_CHANGE";break;
                        case 1 :data="UNIT_AFTER_TARIFF_CHANGE";break;
                        case 2 :data="UNIT_INDETERMINATE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CC_UNIT_TYPE                       : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="TIME";break;
                        case 1 :data="MONEY";break;
                        case 2 :data="TOTAL-OCTETS";break;
                        case 3 :data="INPUT-OCTETS";break;
                        case 4 :data="OUTPUT-OCTETS";break;
                        case 5 :data="SERVICE-SPECIFIC-UNITS";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case FINAL_UNIT_ACTION                  : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="TERMINATE";break;
                        case 1 :data="REDIRECT";break;
                        case 2 :data="RESTRICT_ACCESS";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case REDIRECT_ADDRESS_TYPE              : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="IPv4 Address";break;
                        case 1 :data="IPv6 Address";break;
                        case 2 :data="URL";break;
                        case 3 :data="SIP URI";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case MULTIPLE_SERVICES_INDICATOR        : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="MULTIPLE_SERVICES_NOT_SUPPORTED";break;
                        case 1 :data="MULTIPLE_SERVICES_SUPPORTED";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case REQUESTED_ACTION                   : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DIRECT_DEBITING";break;
                        case 1 :data="REFUND_ACCOUNT";break;
                        case 2 :data="CHECK_BALANCE";break;
                        case 3 :data="PRICE_ENQUIRY";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SUBSCRIPTION_ID_TYPE               : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="END_USER_E164";break;
                        case 1 :data="END_USER_IMSI";break;
                        case 2 :data="END_USER_SIP_URI";break;
                        case 3 :data="END_USER_NAI";break;
                        case 4 :data="END_USER_PRIVATE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case USER_EQUIPMENT_INFO_TYPE           : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="IMEISV";break;
                        case 1 :data="MAC";break;
                        case 2 :data="EUI64";break;
                        case 3 :data="MODIFIED_EUI64";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case MIP_ALGORITHM_                     : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 2 :data="HMAC-SHA-1";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case MIP_REPLAY_MODE                    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="None";break;
                        case 2 :data="Timestamps";break;
                        case 3 :data="Nonces";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case NAS_PORT_TYPE                      : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="Async";break;
                        case 1 :data="Sync";break;
                        case 2 :data="ISDN Sync";break;
                        case 3 :data="ISDN Async V.120";break;
                        case 4 :data="ISDN Async V.110";break;
                        case 5 :data="Virtual";break;
                        case 6 :data="PIAFS";break;
                        case 7 :data="HDLC Clear Channel";break;
                        case 8 :data="X.25";break;
                        case 9 :data="X.75";break;
                        case 10:data="G.3 Fax";break;
                        case 11:data="SDSL - Symmetric DSL";break;
                        case 12:data="ADSL-CAP - Asymmetric DSL, Carrierless Amplitude Phase Modulation";break;
                        case 13:data="ADSL-DMT - Asymmetric DSL, Discrete Multi-Tone";break;
                        case 14:data="IDSL - ISDN Digital Subscriber Line";break;
                        case 15:data="Ethernet";break;
                        case 16:data="xDSL - Digital Subscriber Line of unknown type";break;
                        case 17:data="Cable";break;
                        case 18:data="Wireless - Other";break;
                        case 19:data="Wireless - IEEE 802.11";break;
                        case 20:data="Token-Ring";break;
                        case 21:data="FDDI";break;
                        case 22:data="Wireless - CDMA2000";break;
                        case 23:data="Wireless - UMTS";break;
                        case 24:data="Wireless - 1X-EV";break;
                        case 25:data="IAPP";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case PROMPT                             : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="No Echo";break;
                        case 1 :data="Echo";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CHAP_ALGORITHM                     : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="None";break;
                        case 2 :data="Timestamps";break;
                        case 3 :data="Nonces";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SERVICE_TYPE                       : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="Login";break;
                        case 2 :data="Framed";break;
                        case 3 :data="Callback Login";break;
                        case 4 :data="Callback Framed";break;
                        case 5 :data="Outbound";break;
                        case 6 :data="Administrative";break;
                        case 7 :data="NAS Prompt";break;
                        case 8 :data="Authenticate Only";break;
                        case 9 :data="Callback NAS Prompt";break;
                        case 10:data="Call Check";break;
                        case 11:data="Callback Administrative";break;
                        case 12:data="Voice";break;
                        case 13:data="Fax";break;
                        case 14:data="Modem Relay";break;
                        case 15:data="IAPP-Register";break;
                        case 16:data="IAPP-AP-Check";break;
                        case 17:data="Authorize Only";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case FRAMED_PROTOCOL                    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="PPP";break;
                        case 2 :data="SLIP";break;
                        case 3 :data="AppleTalk Remote Access Protocol (ARAP)";break;
                        case 4 :data="Gandalf proprietary SingleLink/MultiLink protocol";break;
                        case 5 :data="Xylogics proprietary IPX/SLIP";break;
                        case 6 :data="X.75 Synchronous";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;

                    break;
                }
                case FRAMED_ROUTING                     : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="None";break;
                        case 1 :data="Send routing packets";break;
                        case 2 :data="Listen for routing packets";break;
                        case 3 :data="Send and Listen";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case FRAMED_COMPRESSION                 : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="None";break;
                        case 1 :data="VJ TCP/IP header compression";break;
                        case 2 :data="IPX header compression";break;
                        case 3 :data="Stac-LZS compression";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ARAP_ZONE_ACCESS                   : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case     1  :   data="User-Name";break;
                        case     2  :   data="User-Password";break;
                        case     3  :   data="CHAP-Password";break;
                        case     4  :   data="NAS-IP-Address";break;
                        case     5  :   data="NAS-Port";break;
                        case     6  :   data="Service-Type";break;
                        case     7  :   data="Framed-Protocol";break;
                        case     8  :   data="Framed-IP-Address";break;
                        case     9  :   data="Framed-IP-Netmask";break;
                        case    10  :   data="Framed-Routing";break;
                        case    11  :   data="Filter-Id";break;
                        case    12  :   data="Framed-MTU";break;
                        case    13  :   data="Framed-Compression";break;
                        case    14  :   data="Login-IP-Host";break;
                        case    15  :   data="Login-Service";break;
                        case    16  :   data="Login-TCP-Port";break;
                        case    17  :   data="(unassigned)";break;
                        case    18  :   data="Reply-Message";break;
                        case    19  :   data="Callback-Number";break;
                        case    20  :   data="Callback-Id";break;
                        case    21  :   data="(unassigned)";break;
                        case    22  :   data="Framed-Route";break;
                        case    23  :   data="Framed-IPX-Network";break;
                        case    24  :   data="State";break;
                        case    25  :   data="Class";break;
                        case    26  :   data="Vendor-Specific";break;
                        case    27  :   data="Session-Timeout";break;
                        case    28  :   data="Idle-Timeout";break;
                        case    29  :   data="Termination-Action";break;
                        case    30  :   data="Called-Station-Id";break;
                        case    31  :   data="Calling-Station-Id";break;
                        case    32  :   data="NAS-Identifier";break;
                        case    33  :   data="Proxy-State";break;
                        case    34  :   data="Login-LAT-Service";break;
                        case    35  :   data="Login-LAT-Node";break;
                        case    36  :   data="Login-LAT-Group";break;
                        case    37  :   data="Framed-AppleTalk-Link";break;
                        case    38  :   data="Framed-AppleTalk-Network";break;
                        case    39  :   data="Framed-AppleTalk-Zone";break;
                        case    40  :   data="Acct-Status-Type";break;
                        case    41  :   data="Acct-Delay-Time";break;
                        case    42  :   data="Acct-Input-Octets";break;
                        case    43  :   data="Acct-Output-Octets";break;
                        case    44  :   data="Acct-Session-Id";break;
                        case    45  :   data="Acct-Authentic";break;
                        case    46  :   data="Acct-Session-Time";break;
                        case    47  :   data="Acct-Input-Packets";break;
                        case    48  :   data="Acct-Output-Packets";break;
                        case    49  :   data="Acct-Terminate-Cause";break;
                        case    50  :   data="Acct-Multi-Session-Id";break;
                        case    51  :   data="Acct-Link-Count";break;
                        case    52  :   data="Acct-Input-Gigawords";break;
                        case    53  :   data="Acct-Output-Gigawords";break;
                        case    54  :   data="(unassigned)";break;
                        case    55  :   data="Event-Timestamp";break;
                        case    56  :   data="Egress-VLANID";break;
                        case    57  :   data="Ingress-Filters";break;
                        case    58  :   data="Egress-VLAN-Name";break;
                        case    59  :   data="User-Priority-Table";break;
                        case    60  :   data="CHAP-Challenge";break;
                        case    61  :   data="NAS-Port-Type";break;
                        case    62  :   data="Port-Limit";break;
                        case    63  :   data="Login-LAT-Port";break;
                        case    64  :   data="Tunnel-Type";break;
                        case    65  :   data="Tunnel-Medium-Type";break;
                        case    66  :   data="Tunnel-Client-Endpoint";break;
                        case    67  :   data="Tunnel-Server-Endpoint";break;
                        case    68  :   data="Acct-Tunnel-Connection";break;
                        case    69  :   data="Tunnel-Password";break;
                        case    70  :   data="ARAP-Password";break;
                        case    71  :   data="ARAP-Features";break;
                        case    72  :   data="ARAP-Zone-Access";break;
                        case    73  :   data="ARAP-Security";break;
                        case    74  :   data="ARAP-Security-Data";break;
                        case    75  :   data="Password-Retry";break;
                        case    76  :   data="Prompt";break;
                        case    77  :   data="Connect-Info";break;
                        case    78  :   data="Configuration-Token";break;
                        case    79  :   data="EAP-Message";break;
                        case    80  :   data="Message-Authenticator";break;
                        case    81  :   data="Tunnel-Private-Group-ID";break;
                        case    82  :   data="Tunnel-Assignment-ID";break;
                        case    83  :   data="Tunnel-Preference";break;
                        case    84  :   data="ARAP-Challenge-Response";break;
                        case    85  :   data="Acct-Interim-Interval";break;
                        case    86  :   data="Acct-Tunnel-Packets-Lost";break;
                        case    87  :   data="NAS-Port-Id";break;
                        case    88  :   data="Framed-Pool";break;
                        case    89  :   data="CUI";break;
                        case    90  :   data="Tunnel-Client-Auth-ID";break;
                        case    91  :   data="Tunnel-Server-Auth-ID";break;
                        case    92  :   data="NAS-Filter-Rule";break;
                        case    93  :   data="(Unassigned)";break;
                        case    94  :   data="Originating-Line-Info";break;
                        case    95  :   data="NAS-IPv6-Address";break;
                        case    96  :   data="Framed-Interface-Id";break;
                        case    97  :   data="Framed-IPv6-Prefix";break;
                        case    98  :   data="Login-IPv6-Host";break;
                        case    99  :   data="Framed-IPv6-Route";break;
                        case   100  :   data="Framed-IPv6-Pool";break;
                        case   101  :   data="Error-Cause Attribute";break;
                        case   102  :   data="EAP-Key-Name";break;
                        case   103  :   data="Digest-Response";break;
                        case   104  :   data="Digest-Realm";break;
                        case   105  :   data="Digest-Nonce";break;
                        case   106  :   data="Digest-Response-Auth";break;
                        case   107  :   data="Digest-Nextnonce";break;
                        case   108  :   data="Digest-Method";break;
                        case   109  :   data="Digest-URI";break;
                        case   110  :   data="Digest-Qop";break;
                        case   111  :   data="Digest-Algorithm";break;
                        case   112  :   data="Digest-Entity-Body-Hash";break;
                        case   113  :   data="Digest-CNonce";break;
                        case   114  :   data="Digest-Nonce-Count";break;
                        case   115  :   data="Digest-Username";break;
                        case   116  :   data="Digest-Opaque";break;
                        case   117  :   data="Digest-Auth-Param";break;
                        case   118  :   data="Digest-AKA-Auts";break;
                        case   119  :   data="Digest-Domain";break;
                        case   120  :   data="Digest-Stale";break;
                        case   121  :   data="Digest-HA1";break;
                        case   122  :   data="SIP-AOR";break;
                        case   123  :   data="Delegated-IPv6-Prefix";break;
                        case 124-191:   data="(unassigned)";break;
                        case 192-223:   data="Experimental Use";break;
                        case 224-240:   data="Implementation Specific";break;
                        case 241-255:   data="Reserved";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case LOGIN_SERVICE                      : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="Telnet";break;
                        case 1 :data="Rlogin";break;
                        case 2 :data="Timestamps";break;
                        case 3 :data="TCP Clear";break;
                        case 4 :data="PortMaster";break;
                        case 5 :data="LAT";break;
                        case 6 :data="X25-PAD";break;
                        case 7 :data="X25-T3POS";break;
                        case 8 :data="TCP Clear Quiet";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case TUNNEL_TYPE                        : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="Point-to-Point Tunneling Protocol (PPTP)";break;
                        case 2 :data="Layer Two Forwarding (L2F)";break;
                        case 3 :data="Layer Two Tunneling Protocol (L2TP)";break;
                        case 4 :data="Ascend Tunnel Management Protocol (ATMP)";break;
                        case 5 :data="Virtual Tunneling Protocol (VTP)";break;
                        case 6 :data="IP Authentication Header in the Tunnel-mode (AH)";break;
                        case 7 :data="IP-in-IP Encapsulation (IP-IP)";break;
                        case 8 :data="Minimal IP-in-IP Encapsulation (MIN-IP-IP)";break;
                        case 9 :data="IP Encapsulating Security Payload in the Tunnel-mode (ESP)";break;
                        case 10:data="Generic Route Encapsulation (GRE)";break;
                        case 11:data="Bay Dial Virtual Services (DVS)";break;
                        case 12:data="IP-in-IP Tunneling";break;
                        case 13:data="Virtual LANs (VLAN)";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case TUNNEL_MEDIUM_TYPE                 : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="IPv4 (IP version 4)";break;
                        case 2 :data="IPv6 (IP version 6)";break;
                        case 3 :data="NSAP";break;
                        case 4 :data="HDLC (8-bit multidrop)";break;
                        case 5 :data="BBN 1822";break;
                        case 6 :data="802 (includes all 802 media plus 'Ethernet canonicalformat')";break;
                        case 7 :data="E.163 (POTS)";break;
                        case 8 :data="E.164 (SMDS, Frame Relay, ATM)";break;
                        case 9 :data="F.69 (Telex)";break;
                        case 10:data="X.121 (X.25, Frame Relay)";break;
                        case 11:data="IPX";break;
                        case 12:data="Appletalk";break;
                        case 13:data="Decnet IV";break;
                        case 14:data="Banyan Vines";break;
                        case 15:data="E.164 with NSAP format subaddress";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ACCT_AUTHENTIC                     : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="RADIUS";break;
                        case 2 :data="Local";break;
                        case 3 :data="Remote";break;
                        case 4 :data="Diameter";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ACOUNTING_AUTH_METHOD              : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="PAP";break;
                        case 2 :data="CHAP";break;
                        case 3 :data="MS-CHAP-1";break;
                        case 4 :data="MS-CHAP-2";break;
                        case 5 :data="EAP";break;
                        case 7 :data="None";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ORIGIN_AAA_PROTOCOL                : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 1 :data="RADIUS";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SIP_SERVER_ASSIGNMENT_TYPE         : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="NO_ASSIGNMENT";break;
                        case 1 :data="REGISTRATION";break;
                        case 2 :data="RE_REGISTRATION";break;
                        case 3 :data="UNREGISTERED_USER";break;
                        case 4 :data="TIMEOUT_DEREGISTRATION";break;
                        case 5 :data="USER_DEREGISTRATION";break;
                        case 6 :data="TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME";break;
                        case 7 :data="USER_DEREGISTRATION_STORE_SERVER_NAME";break;
                        case 8 :data="ADMINISTRATIVE_DEREGISTRATION";break;
                        case 9 :data="AUTHENTICATION_FAILURE";break;
                        case 10:data="AUTHENTICATION_TIMEOUT";break;
                        case 11:data="DEREGISTRATION_TOO_MUCH_DATA";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SIP_AUTHENTICATION_SCHEME          : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DIGEST";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SIP_REASON_CODE                    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="PERMANENT_TERMINATION";break;
                        case 1 :data="NEW_SIP_SERVER_ASSIGNED";break;
                        case 2 :data="SIP_SERVER_CHANGE";break;
                        case 3 :data="REMOVE_SIP_SERVER";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SIP_USER_AUTHORIZATION_TYPE        : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="REGISTRATION";break;
                        case 1 :data="DEREGISTRATION";break;
                        case 2 :data="REGISTRATION_AND_CAPABILITIES";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SIP_USER_DATA_ALREADY_AVAILABLE    : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="USER_DATA_NOT_AVAILABLE";break;
                        case 1 :data="USER_DATA_ALREADY_AVAILABLE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                // Swisscom:
                case ADDRESS_TYPE                       : //    Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="e-mail address";break;
                        case 1 :data="MSISDN";break;
                        case 2 :data="IPv4 Address";break;
                        case 3 :data="IPv6 Address";break;
                        case 4 :data="Numeric Shortcode";break;
                        case 5 :data="Alphanumeric Shortcode";break;
                        case 6 :data="Other";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case CLASS_IDENTIFIER                   : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="Personal";break;
                        case 1 :data="Advertisement";break;
                        case 2 :data="Informational";break;
                        case 3 :data="Auto";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                // Gx specific AVPs
                case BEARER_USAGE                       : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="GENERAL";break;
                        case 1 :data="IMS_SIGNALLING";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case EVENT_TRIGGER                      : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="SGSN_CHANGE";break;
                        case 1 :data="QOS_CHANGE";break;
                        case 2 :data="RAT_CHANGE";break;
                        case 3 :data="TFT_CHANGE";break;
                        case 4 :data="PLMN_CHANGE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case METERING_METHOD                    : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DURATION";break;
                        case 1 :data="VOLUME";break;
                        case 2 :data="DURATION_VOLUME";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case OFFLINE                            : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DISABLE_OFFLINE";break;
                        case 1 :data="ENABLE_OFFLINE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case ONLINE                             : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="DISABLE_ONLINE";break;
                        case 1 :data="ENABLE_ONLINE";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case REPORTING_LEVEL                    : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="CHARGING_RULE_LEVEL";break;
                        case 1 :data="RATING_GROUP_LEVEL";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case PDP_SESSION_OPERATION              : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="PDP-SESSION-TERMINATION";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case SESSION_RELEASE_CAUSE              : //   Enumerated
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        case 0 :data="UNSPECIFIED_REASON";break;
                        case 1 :data="UE_SUBSCRIPTION_REASON";break;
                        case 2 :data="INSUFFICIENT_SERVER_RESOURCES";break;
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    break;
                }
                case E2E_SEQUENCE                       : //    Grouped
                case EXPERIMENTAL_RESULT                : //    Grouped
                case FAILED_AVP                         : //    Grouped
                case PROXY_INFO                         : //    Grouped
                case VENDOR_SPECIFIC_APPLICATION_ID     : //    Grouped
                case CC_MONEY                           : //    Grouped
                case COST_INFORMATION                   : //    Grouped
                case FINAL_UNIT_INDICATION              : //    Grouped
                case GRANTED_SERVICE_UNIT               : //    Grouped
                case G_S_U_POOL_REFERENCE               : //    Grouped
                case MULTIPLE_SERVICES_CREDIT_CONTROL   : //    Grouped
                case REDIRECT_SERVER                    : //    Grouped
                case REQUESTED_SERVICE_UNIT             : //    Grouped
                case SERVICE_PARAMETER_INFO             : //    Grouped
                case SUBSCRIPTION_ID                    : //    Grouped
                case UNIT_VALUE                         : //    Grouped
                case USED_SERVICE_UNIT                  : //    Grouped
                case USER_EQUIPMENT_INFO                : //    Grouped
                case MIP_MN_AAA_AUTH                    : //    Grouped
                case MIP_ORIGINATING_FOREIGN_AAA        : //    Grouped
                case MIP_MN_TO_FA_MSA                   : //    Grouped
                case MIP_FA_TO_MN_MSA                   : //    Grouped
                case MIP_FA_TO_HA_MSA                   : //    Grouped
                case MIP_HA_TO_FA_MSA                   : //    Grouped
                case MIP_MN_TO_HA_MSA                   : //    Grouped
                case MIP_HA_TO_MN_MSA                   : //    Grouped
                case CHAP_AUTH					        : //    Grouped
                case TUNNELING					        : //    Grouped
                case SIP_ACCOUNTING_INFORMATION         : //    Grouped
                case SIP_SERVER_CAPABILITIES            : //    Grouped
                case SIP_AUTH_DATA_ITEM                 : //    Grouped
                case SIP_AUTHENTICATE                   : //    Grouped
                case SIP_AUTHORIZATION                  : //    Grouped
                case SIP_AUTHENTICATION_INFO            : //    Grouped
                case SIP_DEREGISTRATION_REASON          : //    Grouped
                case SIP_USER_DATA                      : //    Grouped
                // Swisscom:
                case SERVICE_INFORMATION                : //    Grouped
                case SMS_INFORMATION                    : //    Grouped
                case MMS_INFORMATION                    : //    Grouped
                case ORIGINATOR_ADDRESS                 : //    Grouped
                case ADDRESS_DOMAIN                     : //    Grouped
                case MESSAGE_CLASS                      : //    Grouped
                // XL
                case REMAINING_SERVICE_MONEY            : //    Grouped
                case UMB_CHARGING_GROUP                 : //    Grouped
                // Gx specific AVPs
                case CHARGING_RULE_INSTALL              : //    Grouped
                case CHARGING_RULE_REMOVE               : //    Grouped
                case CHARGING_RULE_DEFINITION           : //    Grouped
                case TFT_PACKET_FILTER_INFORMATION      : //    Grouped
                {
                        int i=0;
                        Diameter_AVP avp=null;
                        while(pointer<startpointer+datalength)
                        {
                            avp=new Diameter_AVP();
                            pointer=avp.decode(body,pointer,i,extendedStoreVar,extendedStoreVarValue,avpMap);
                            output.append("\nGrouped AVP number "+i+" pointer="+pointer);
                            output.append(avp.output);
                            groupedAVPs.add(avp);
                            //output.append("\n");
                            i++;
                        }
                    }
                    break;
                case EXPONENT                   : //    Integer32
                    number=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    data=""+number;
                    break;
                case VALUE_DIGITS               : //    Integer64
                    number=ConvertLib.getLongFromByteArray(body,pointer,datalength-avpheadlength);
                    data=""+number;
                    break;
                case RESTRICTION_FILTER_RULE    : //    IPFiltrRule
                case MIP_FILTER_RULE            : //    IPFiltrRule
                case NAS_FILTER_RULE            : //    IPFiltrRule
                // Gx specific AVPs
                case TFT_FILTER                 : //    IPFiltrRule
                    data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    break;
                case QOS_FILTER_RULE            : //    QoSFltrRule
                    data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    break;
                case FRAMED_IP_ADDRESS          : //    OctetString
                    // Swisscom
                    if(vendorspec&&vendorID==10415)
                    {
                        avpname="3GPP_IMSI_MCC_MNC";
                        data=ConvertLib.createString(body,pointer,datalength-avpheadlength);
                    } else
                    // Diameter
                    {
                        data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    }
                    break;
                case ACCOUNTING_SESSION_ID      : //    OctetString
                case CLASS                      : //    OctetString
                case PROXY_STATE                : //    OctetString
                case CC_CORRELATION_ID          : //    OctetString
                case SERVICE_PARAMETER_VALUE    : //    OctetString
                case USER_EQUIPMENT_INFO_VALUE  : //    OctetString
                case MIP_REG_REQUEST            : //    OctetString
                case MIP_REG_REPLY              : //    OctetString
                case MIP_FA_CHALLENGE           : //    OctetString
                case MIP_NONCE                  : //    OctetString
                case MIP_SESSION_KEY            : //    OctetString
                case ORIGINATING_LINE_INFO      : //    OctetString
                case USER_PASSWORD              : //    OctetString
                case CHAP_IDENT                 : //    OctetString
                case CHAP_RESPONSE              : //    OctetString
                case CHAP_CHALLENGE             : //    OctetString
                case ARAP_PASSWORD              : //    OctetString
                case ARAP_CHALLENGE_RESPONSE    : //    OctetString
                case ARAP_SECURITY_DATA         : //    OctetString
                case CONFIGURATION_TOKEN        : //    OctetString
                case FRAMED_IP_NETMASK          : //    OctetString
                case FRAMED_POOL                : //    OctetString
                case FRAMED_IPV6_PREFIX         : //    OctetString
                case FRAMED_IPV6_POOL           : //    OctetString
                case FRAMED_APPLETALK_ZONE      : //    OctetString
                case ARAP_FEATURES              : //    OctetString
                case LOGIN_IP_HOST              : //    OctetString
                case LOGIN_IPV6_HOST            : //    OctetString
                case LOGIN_LAT_SERVICE          : //    OctetString
                case LOGIN_LAT_NODE             : //    OctetString
                case LOGIN_LAT_GROUP            : //    OctetString
                case LOGIN_LAT_PORT             : //    OctetString
                case TUNNEL_PASSWORD            : //    OctetString
                case TUNNEL_PRIVATE_GROUP_ID    : //    OctetString
                case TUNNEL_ASSIGNMENT_ID       : //    OctetString
                case ACCT_TUNNEL_CONNECTION     : //    OctetString
                case NAS_IP_ADDRESS             : //    OctetString
                case NAS_IPV6_ADDRESS           : //    OctetString
                case STATE                      : //    OctetString
                case SIP_USER_DATA_CONTENTS     : //    OctetString
                // Gx specific AVPs
                case TOS_TRAFFIC_CLASS          : //    OctetString
                case CHARGING_RULE_NAME         : //    OctetString
                    data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    break;
                case EVENT_TIMESTAMP            : //    Time
                case TARIFF_TIME_CHANGE         : //    Time
                    number=ConvertLib.getLongFromByteArray(body,pointer,datalength-avpheadlength);
                    data=""+number;
                    break;
                case RESULT_CODE                : //    Unsigned32
                {
                    int cause=ConvertLib.getIntFromByteArray(body,pointer,datalength-avpheadlength);
                    switch(cause)
                    {
                        // Diameter
                        case DIAMETER_MULTI_ROUND_AUTH          :data="DIAMETER_MULTI_ROUND_AUTH"          ;break;
                        case DIAMETER_SUCCESS                   :data="DIAMETER_SUCCESS"                   ;break;
                        case DIAMETER_LIMITED_SUCCESS           :data="DIAMETER_LIMITED_SUCCESS"           ;break;
                        case DIAMETER_COMMAND_UNSUPPORTED       :data="DIAMETER_COMMAND_UNSUPPORTED"       ;break;
                        case DIAMETER_UNABLE_TO_DELIVER         :data="DIAMETER_UNABLE_TO_DELIVER"         ;break;
                        case DIAMETER_REALM_NOT_SERVED          :data="DIAMETER_REALM_NOT_SERVED"          ;break;
                        case DIAMETER_TOO_BUSY                  :data="DIAMETER_TOO_BUSY"                  ;break;
                        case DIAMETER_LOOP_DETECTED             :data="DIAMETER_LOOP_DETECTED"             ;break;
                        case DIAMETER_REDIRECT_INDICATION       :data="DIAMETER_REDIRECT_INDICATION"       ;break;
                        case DIAMETER_APPLICATION_UNSUPPORTED   :data="DIAMETER_APPLICATION_UNSUPPORTED"   ;break;
                        case DIAMETER_INVALID_HDR_BITS          :data="DIAMETER_INVALID_HDR_BITS"          ;break;
                        case DIAMETER_INVALID_AVP_BITS          :data="DIAMETER_INVALID_AVP_BITS"          ;break;
                        case DIAMETER_UNKNOWN_PEER              :data="DIAMETER_UNKNOWN_PEER"              ;break;
                        case DIAMETER_AUTHENTICATION_REJECTED   :data="DIAMETER_AUTHENTICATION_REJECTED"   ;break;
                        case DIAMETER_OUT_OF_SPACE              :data="DIAMETER_OUT_OF_SPACE"              ;break;
                        case ELECTION_LOST                      :data="ELECTION_LOST"                      ;break;
                        case DIAMETER_AVP_UNSUPPORTED           :data="DIAMETER_AVP_UNSUPPORTED"           ;break;
                        case DIAMETER_UNKNOWN_SESSION_ID        :data="DIAMETER_UNKNOWN_SESSION_ID"        ;break;
                        case DIAMETER_AUTHORIZATION_REJECTED    :data="DIAMETER_AUTHORIZATION_REJECTED"    ;break;
                        case DIAMETER_INVALID_AVP_VALUE         :data="DIAMETER_INVALID_AVP_VALUE"         ;break;
                        case DIAMETER_MISSING_AVP               :data="DIAMETER_MISSING_AVP"               ;break;
                        case DIAMETER_RESOURCES_EXCEEDED        :data="DIAMETER_RESOURCES_EXCEEDED"        ;break;
                        case DIAMETER_CONTRADICTING_AVPS        :data="DIAMETER_CONTRADICTING_AVPS"        ;break;
                        case DIAMETER_AVP_NOT_ALLOWED           :data="DIAMETER_AVP_NOT_ALLOWED"           ;break;
                        case DIAMETER_AVP_OCCURS_TOO_MANY_TIMES :data="DIAMETER_AVP_OCCURS_TOO_MANY_TIMES" ;break;
                        case DIAMETER_NO_COMMON_APPLICATION     :data="DIAMETER_NO_COMMON_APPLICATION"     ;break;
                        case DIAMETER_UNSUPPORTED_VERSION       :data="DIAMETER_UNSUPPORTED_VERSION"       ;break;
                        case DIAMETER_UNABLE_TO_COMPLY          :data="DIAMETER_UNABLE_TO_COMPLY"          ;break;
                        case DIAMETER_INVALID_BIT_IN_HEADER     :data="DIAMETER_INVALID_BIT_IN_HEADER"     ;break;
                        case DIAMETER_INVALID_AVP_LENGTH        :data="DIAMETER_INVALID_AVP_LENGTH"        ;break;
                        case DIAMETER_INVALID_MESSAGE_LENGTH    :data="DIAMETER_INVALID_MESSAGE_LENGTH"    ;break;
                        case DIAMETER_INVALID_AVP_BIT_COMBO     :data="DIAMETER_INVALID_AVP_BIT_COMBO"     ;break;
                        case DIAMETER_NO_COMMON_SECURITY        :data="DIAMETER_NO_COMMON_SECURITY"        ;break;
                        // Credit-Control
                        case DIAMETER_END_USER_SERVICE_DENIED       :data="DIAMETER_END_USER_SERVICE_DENIED"        ;break;
                        case DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE :data="DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE"  ;break;
                        case DIAMETER_CREDIT_LIMIT_REACHED          :data="DIAMETER_CREDIT_LIMIT_REACHED"           ;break;
                        case DIAMETER_USER_UNKNOWN                  :data="DIAMETER_USER_UNKNOWN"                   ;break;
                        case DIAMETER_RATING_FAILED                 :data="DIAMETER_RATING_FAILED"              ;break;
                        // default
                        default:data="Unknown("+cause+")";break;
                    }
                    number=cause;
                    data=""+number;
                    break;
                }
                case ACCT_INTERIM_INTERVAL      : //    Unsigned32
                case ACCOUNTING_RECORD_NUMBER   : //    Unsigned32
                case ACCT_APPLICATION_ID        : //    Unsigned32
                case AUTH_APPLICATION_ID        : //    Unsigned32
                case AUTHORIZATION_LIFETIME     : //    Unsigned32
                case AUTH_GRACE_PERIOD          : //    Unsigned32
                case EXPERIMENTAL_RESULT_CODE   : //    Unsigned32
                case FIRMWARE_REVISION          : //    Unsigned32
                case INBAND_SECURITY_ID         : //    Unsigned32
                case MULTI_ROUND_TIME_OUT       : //    Unsigned32
                case ORIGIN_STATE_ID            : //    Unsigned32
                case REDIRECT_MAX_CACHE_TIME    : //    Unsigned32
                case SESSION_TIMEOUT            : //    Unsigned32
                case SESSION_BINDING            : //    Unsigned32
                case SUPPORTED_VENDOR_ID        : //    Unsigned32
                case VENDOR_ID                  : //    Unsigned32
                case CC_REQUEST_NUMBER          : //    Unsigned32
                case CC_TIME                    : //    Unsigned32
                case CURRENCY_CODE              : //    Unsigned32
                case G_S_U_POOL_IDENTIFIER      : //    Unsigned32
                case RATING_GROUP               : //    Unsigned32
                case SERVICE_IDENTIFIER         : //    Unsigned32
                case SERVICE_PARAMETER_TYPE     : //    Unsigned32
                case VALIDITY_TIME              : //    Unsigned32
                case MIP_FEATURE_VECTOR         : //    Unsigned32
                case MIP_AUTH_INPUT_DATA_LENGTH : //    Unsigned32
                case MIP_AUTHENTICATOR_LENGTH   : //    Unsigned32
                case MIP_AUTHENTICATOR_OFFSET   : //    Unsigned32
                case MIP_MN_AAA_SPI             : //    Unsigned32
                case MIP_FA_TO_HA_SPI           : //    Unsigned32
                case MIP_FA_TO_MN_SPI           : //    Unsigned32
                case MIP_HA_TO_FA_SPI           : //    Unsigned32
                case MIP_MSA_LIFETIME           : //    Unsigned32
                case NAS_PORT                   : //    Unsigned32
                case PASSWORD_RETRY             : //    Unsigned32
                case ARAP_SECURITY              : //    Unsigned32
                case IDLE_TIMEOUT               : //    Unsigned32
                case PORT_LIMIT                 : //    Unsigned32
                case FRAMED_MTU                 : //    Unsigned32
                case FRAMED_APPLETALK_LINK      : //    Unsigned32
                case FRAMED_APPLETALK_NETWORK   : //    Unsigned32
                case LOGIN_TCP_PORT             : //    Unsigned32
                case TUNNEL_PREFERENCE          : //    Unsigned32
                case ACCT_SESSION_TIME          : //    Unsigned32
                case ACCT_DELAY_TIME            : //    Unsigned32
                case ACCT_LINK_COUNT            : //    Unsigned32
                case ACCT_TUNNEL_PACKETS_LOST   : //    Unsigned32
                case SIP_MANDATORY_CAPABILITY   : //    Unsigned32
                case SIP_OPTIONAL_CAPABILITY    : //    Unsigned32
                case SIP_ITEM_NUMBER            : //    Unsigned32
                case SIP_NUMBER_AUTH_ITEMS      : //    Unsigned32
                // Gx specific AVPs
                case PRECEDENCE                 : //    Unsigned32
                    number=ConvertLib.getLongFromByteArray(body,pointer,datalength-avpheadlength);
                    data=""+number;
                    break;
                case ACCOUNTING_SUB_SESSION_ID  : //    Unsigned64
                case CC_INPUT_OCTETS            : //    Unsigned64
                case CC_OUTPUT_OCTETS           : //    Unsigned64
                case CC_SERVICE_SPECIFIC_UNITS  : //    Unsigned64
                case CC_SUB_SESSION_ID          : //    Unsigned64
                case CC_TOTAL_OCTETS            : //    Unsigned64
                case FRAMED_INTERFACE_ID        : //    Unsigned64
                case ACCOUNTING_INPUT_OCTETS    : //    Unsigned64
                case ACCOUNTING_OUTPUT_OCTETS   : //    Unsigned64
                case ACCOUNTING_INPUT_PACKETS   : //    Unsigned64
                case ACCOUNTING_OUTPUT_PACKETS  : //    Unsigned64
                    number=ConvertLib.getLongFromByteArray(body,pointer,datalength-avpheadlength);
                    data=""+number;
                    break;
                case ACCT_MULTI_SESSION_ID        : //   UTF8String
                case ERROR_MESSAGE                : //   UTF8String
                case PRODUCT_NAME                 : //   UTF8String
                case SESSION_ID                   : //   UTF8String
                case USER_NAME                    : //   UTF8String
                case COST_UNIT                    : //   UTF8String
                case REDIRECT_SERVER_ADDRESS      : //   UTF8String
                case SERVICE_CONTEXT_ID           : //   UTF8String
                case SUBSCRIPTION_ID_DATA         : //   UTF8String
                case NAS_PORT_ID                  : //   UTF8String
                case CALLED_STATION_ID            : //   UTF8String
                case CALLING_STATION_ID           : //   UTF8String
                case CONNECT_INFO                 : //   UTF8String
                case REPLY_MESSAGE                : //   UTF8String
                case CALLBACK_NUMBER              : //   UTF8String
                case CALLBACK_ID                  : //   UTF8String
                case FILTER_ID                    : //   UTF8String
                case FRAMED_ROUTE                 : //   UTF8String
                case FRAMED_IPV6_ROUTE            : //   UTF8String
                case FRAMED_IPX_NETWORK           : //   UTF8String
                case TUNNEL_CLIENT_ENDPOINT       : //   UTF8String
                case TUNNEL_SERVER_ENDPOINT       : //   UTF8String
                case TUNNEL_CLIENT_AUTH_ID        : //   UTF8String
                case TUNNEL_SERVER_AUTH_ID        : //   UTF8String
                case NAS_IDENTIFIER               : //   UTF8String
                case SIP_SERVER_URI               : //   UTF8String
                case SIP_REASON_INFO              : //   UTF8String
                case SIP_VISITED_NETWORK_ID       : //   UTF8String
                case SIP_SUPPORTED_USER_DATA_TYPE : //   UTF8String
                case SIP_USER_DATA_TYPE           : //   UTF8String
                case SIP_METHOD                   : //   UTF8String
                // Swisscom:
                case ADDRESS_DATA                 : //   UTF8String
                case DOMAIN_NAME                  : //   UTF8String
                case MESSAGE_ID                   : //   UTF8String
                case TOKEN_TEXT                   : //   UTF8String
                // XL
                case VAS_TYPE                     : //   UTF8String
                case MESSAGE_TYPE                 : //   UTF8String
                case SDC                          : //   UTF8String
                // Gx specific AVPs
                case CHARGING_RULE_BASE_NAME      : //   UTF8String
                    data=ConvertLib.createString(body,pointer,datalength-avpheadlength);
                    break;
                default:
                    data=ConvertLib.outputBytes(body,pointer,datalength-avpheadlength);
                    break;
            }
            output.append("\ndata            ="+ data);

            if(extendedStoreVar!=null&&extendedStoreVar.contains(avpname.toLowerCase()))
            {
                extendedStoreVarValue.add(data);
            }

            Vector<Diameter_AVP> temp=avpMap.get(avpcode);
            if(temp==null)temp=new Vector<Diameter_AVP>();
            temp.add(this);
            avpMap.put(avpcode,temp);

            pointer=startpointer+datalength;

        } finally
        {
            this.output=output.toString();//+"\n";
        }
        if(pointer%4!=0)
        {
            pointer=pointer-(pointer%4)+4;
        }
        return pointer;
    }

    public byte[] encode()
    {
        byte[] payload=new byte[0];
        switch(avpcode)
        {
            case HOST_IP_ADDRESS         : // Address
            case MIP_MOBILE_NODE_ADDRESS : // Address
            case MIP_HOME_AGENT_ADDRESS  : // Address
                // 2 bytes from http://www.iana.org/assignments/address-family-numbers/ and following the address.
                payload=ConvertLib.getBytesFromByteString(data);
                break;
            case DESTINATION_HOST              : // DiamIdent
            case DESTINATION_REALM             : // DiamIdent
            case ERROR_REPORTING_HOST          : // DiamIdent
            case ORIGIN_HOST                   : // DiamIdent
            case ORIGIN_REALM                  : // DiamIdent
            case PROXY_HOST                    : // DiamIdent
            case ROUTE_RECORD                  : // DiamIdent
            case MIP_CANDIDATE_HOME_AGENT_HOST : // DiamIdent
            case MIP_HOME_AGENT_HOST           : // DiamIdent
                payload=ConvertLib.createBytes(data);
                break;
            case REDIRECT_HOST                  : //    DiamURI
            case SIP_ACCOUNTING_SERVER_URI      : //    DiamURI
            case SIP_CREDIT_CONTROL_SERVER_URI  : //    DiamURI
                payload=ConvertLib.createBytes(data);
                break;
            case DISCONNECT_CAUSE                   : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("REBOOTING"                   )){number=0;}
                    else if(data.equalsIgnoreCase("BUSY"                        )){number=1;}
                    else if(data.equalsIgnoreCase("DO_NOT_WANT_TO_TALK_TO_YOU"  )){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
               break;
            }
            case REDIRECT_HOST_USAGE                : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DONT_CACHE"            )){number=0;}
                    else if(data.equalsIgnoreCase("ALL_SESSION"           )){number=1;}
                    else if(data.equalsIgnoreCase("ALL_REALM"             )){number=2;}
                    else if(data.equalsIgnoreCase("REALM_AND_APPLICATION" )){number=3;}
                    else if(data.equalsIgnoreCase("ALL_APPLICATION"       )){number=4;}
                    else if(data.equalsIgnoreCase("ALL_HOST"              )){number=5;}
                    else if(data.equalsIgnoreCase("ALL_USER"              )){number=6;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
               break;
            }
            case AUTH_REQUEST_TYPE                  : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("AUTHENTICATE_ONLY"      )){number=1;}
                    else if(data.equalsIgnoreCase("AUTHORIZE_ONLY"         )){number=2;}
                    else if(data.equalsIgnoreCase("AUTHORIZE_AUTHENTICATE" )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case AUTH_SESSION_STATE                 : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("STATE_MAINTAINED"    )){number=0;}
                    else if(data.equalsIgnoreCase("NO_STATE_MAINTAINED" )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case RE_AUTH_REQUEST_TYPE               : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("AUTHORIZE_ONLY"         )){number=0;}
                    else if(data.equalsIgnoreCase("AUTHORIZE_AUTHENTICATE" )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case TERMINATION_CAUSE                  : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DIAMETER_LOGOUT"              )){number=1;}
                    else if(data.equalsIgnoreCase("DIAMETER_SERVICE_NOT_PROVIDED")){number=2;}
                    else if(data.equalsIgnoreCase("DIAMETER_BAD_ANSWER"          )){number=3;}
                    else if(data.equalsIgnoreCase("DIAMETER_ADMINISTRATIVE"      )){number=4;}
                    else if(data.equalsIgnoreCase("DIAMETER_LINK_BROKEN"         )){number=5;}
                    else if(data.equalsIgnoreCase("DIAMETER_AUTH_EXPIRED"        )){number=6;}
                    else if(data.equalsIgnoreCase("DIAMETER_USER_MOVED"          )){number=7;}
                    else if(data.equalsIgnoreCase("DIAMETER_SESSION_TIMEOUT"     )){number=8;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SESSION_SERVER_FAILOVER            : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("REFUSE_SERVICE"          )){number=0;}
                    else if(data.equalsIgnoreCase("TRY_AGAIN "              )){number=1;}
                    else if(data.equalsIgnoreCase("ALLOW_SERVICE"           )){number=2;}
                    else if(data.equalsIgnoreCase("TRY_AGAIN_ALLOW_SERVICE" )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ACCOUNTING_RECORD_TYPE             : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("EVENT_RECORD"  )){number=1;}
                    else if(data.equalsIgnoreCase("START_RECORD " )){number=2;}
                    else if(data.equalsIgnoreCase("START_RECORD"  )){number=3;}
                    else if(data.equalsIgnoreCase("STOP_RECORD"   )){number=4;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ACCOUNTING_REALTIME_REQUIRED       : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DELIVER_AND_GRANT")){number=1;}
                    else if(data.equalsIgnoreCase("GRANT_AND_STORE " )){number=2;}
                    else if(data.equalsIgnoreCase("GRANT_AND_LOSE"   )){number=3;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CC_REQUEST_TYPE                    : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("INITIAL_REQUEST"    )){number=1;}
                    else if(data.equalsIgnoreCase("UPDATE_REQUEST"    )){number=2;}
                    else if(data.equalsIgnoreCase("TERMINATION_REQUEST")){number=3;}
                    else if(data.equalsIgnoreCase("EVENT_REQUEST"      )){number=4;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CC_SESSION_FAILOVER                : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("FAILOVER_NOT_SUPPORTED")){number=0;}
                    else if(data.equalsIgnoreCase("FAILOVER_SUPPORTED "   )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CHECK_BALANCE_RESULT               : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("ENOUGH_CREDIT")){number=0;}
                    else if(data.equalsIgnoreCase("NO_CREDIT "   )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CREDIT_CONTROL                     : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("CREDIT_AUTHORIZATION")){number=0;}
                    else if(data.equalsIgnoreCase("RE_AUTHORIZATION "   )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CREDIT_CONTROL_FAILURE_HANDLING    : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("TERMINATE"           )){number=0;}
                    else if(data.equalsIgnoreCase("CONTINUE "           )){number=1;}
                    else if(data.equalsIgnoreCase("RETRY_AND_TERMINATE ")){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case DIRECT_DEBITING_FAILURE_HANDLING   : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("TERMINATE_OR_BUFFER")){number=0;}
                    else if(data.equalsIgnoreCase("CONTINUE"           )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case TARIFF_CHANGE_USAGE                : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("UNIT_BEFORE_TARIFF_CHANGE")){number=0;}
                    else if(data.equalsIgnoreCase("UNIT_AFTER_TARIFF_CHANGE" )){number=1;}
                    else if(data.equalsIgnoreCase("UNIT_INDETERMINATE"       )){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CC_UNIT_TYPE                       : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("TIME"                  )){number=0;}
                    else if(data.equalsIgnoreCase("MONEY"                 )){number=1;}
                    else if(data.equalsIgnoreCase("TOTAL-OCTETS"          )){number=2;}
                    else if(data.equalsIgnoreCase("INPUT-OCTETS"          )){number=3;}
                    else if(data.equalsIgnoreCase("OUTPUT-OCTETS"         )){number=4;}
                    else if(data.equalsIgnoreCase("SERVICE-SPECIFIC-UNITS")){number=5;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case FINAL_UNIT_ACTION                  : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("TERMINATE"      )){number=0;}
                    else if(data.equalsIgnoreCase("REDIRECT"       )){number=1;}
                    else if(data.equalsIgnoreCase("RESTRICT_ACCESS")){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case REDIRECT_ADDRESS_TYPE              : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("IPv4 Address")){number=0;}
                    else if(data.equalsIgnoreCase("IPv6 Address")){number=1;}
                    else if(data.equalsIgnoreCase("URL"         )){number=2;}
                    else if(data.equalsIgnoreCase("SIP URI"     )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case MULTIPLE_SERVICES_INDICATOR        : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("MULTIPLE_SERVICES_NOT_SUPPORTED")){number=0;}
                    else if(data.equalsIgnoreCase("MULTIPLE_SERVICES_SUPPORTED"    )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case REQUESTED_ACTION                   : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DIRECT_DEBITING")){number=0;}
                    else if(data.equalsIgnoreCase("REFUND_ACCOUNT" )){number=1;}
                    else if(data.equalsIgnoreCase("CHECK_BALANCE"  )){number=2;}
                    else if(data.equalsIgnoreCase("PRICE_ENQUIRY"  )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SUBSCRIPTION_ID_TYPE               : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("END_USER_E164"   )){number=0;}
                    else if(data.equalsIgnoreCase("END_USER_IMSI"   )){number=1;}
                    else if(data.equalsIgnoreCase("END_USER_SIP_URI")){number=2;}
                    else if(data.equalsIgnoreCase("END_USER_NAI"    )){number=3;}
                    else if(data.equalsIgnoreCase("END_USER_PRIVATE")){number=4;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case USER_EQUIPMENT_INFO_TYPE           : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("IMEISV"        )){number=0;}
                    else if(data.equalsIgnoreCase("MAC"           )){number=1;}
                    else if(data.equalsIgnoreCase("EUI64"         )){number=2;}
                    else if(data.equalsIgnoreCase("MODIFIED_EUI64")){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case MIP_ALGORITHM_                     : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("HMAC-SHA-1")){number=2;}
                    else{number=2;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case MIP_REPLAY_MODE                    : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("None"      )){number=1;}
                    else if(data.equalsIgnoreCase("Timestamps")){number=2;}
                    else if(data.equalsIgnoreCase("Nonces"    )){number=3;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case NAS_PORT_TYPE                      : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("Async"                                                            )){number=0 ;}
                    else if(data.equalsIgnoreCase("Sync"                                                             )){number=1 ;}
                    else if(data.equalsIgnoreCase("ISDN Sync"                                                        )){number=2 ;}
                    else if(data.equalsIgnoreCase("ISDN Async V.120"                                                 )){number=3 ;}
                    else if(data.equalsIgnoreCase("ISDN Async V.110"                                                 )){number=4 ;}
                    else if(data.equalsIgnoreCase("Virtual"                                                          )){number=5 ;}
                    else if(data.equalsIgnoreCase("PIAFS"                                                            )){number=6 ;}
                    else if(data.equalsIgnoreCase("HDLC Clear Channel"                                               )){number=7 ;}
                    else if(data.equalsIgnoreCase("X.25"                                                             )){number=8 ;}
                    else if(data.equalsIgnoreCase("X.75"                                                             )){number=9 ;}
                    else if(data.equalsIgnoreCase("G.3 Fax"                                                          )){number=10;}
                    else if(data.equalsIgnoreCase("SDSL - Symmetric DSL"                                             )){number=11;}
                    else if(data.equalsIgnoreCase("ADSL-CAP - Asymmetric DSL, Carrierless Amplitude Phase Modulation")){number=12;}
                    else if(data.equalsIgnoreCase("ADSL-DMT - Asymmetric DSL, Discrete Multi-Tone"                   )){number=13;}
                    else if(data.equalsIgnoreCase("IDSL - ISDN Digital Subscriber Line"                              )){number=14;}
                    else if(data.equalsIgnoreCase("Ethernet"                                                         )){number=15;}
                    else if(data.equalsIgnoreCase("xDSL - Digital Subscriber Line of unknown type"                   )){number=16;}
                    else if(data.equalsIgnoreCase("Cable"                                                            )){number=17;}
                    else if(data.equalsIgnoreCase("Wireless - Other"                                                 )){number=18;}
                    else if(data.equalsIgnoreCase("Wireless - IEEE 802.11"                                           )){number=19;}
                    else if(data.equalsIgnoreCase("Token-Ring"                                                       )){number=20;}
                    else if(data.equalsIgnoreCase("FDDI"                                                             )){number=21;}
                    else if(data.equalsIgnoreCase("Wireless - CDMA2000"                                              )){number=22;}
                    else if(data.equalsIgnoreCase("Wireless - UMTS"                                                  )){number=23;}
                    else if(data.equalsIgnoreCase("Wireless - 1X-EV"                                                 )){number=24;}
                    else if(data.equalsIgnoreCase("IAPP"                                                             )){number=25;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case PROMPT                             : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("No Echo")){number=0;}
                    else if(data.equalsIgnoreCase("Echo"   )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CHAP_ALGORITHM                     : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("None"      )){number=1;}
                    else if(data.equalsIgnoreCase("Timestamps")){number=2;}
                    else if(data.equalsIgnoreCase("Nonces"    )){number=3;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SERVICE_TYPE                       : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("Login"                  )){number=1 ;}
                    else if(data.equalsIgnoreCase("Framed"                 )){number=2 ;}
                    else if(data.equalsIgnoreCase("Callback Login"         )){number=3 ;}
                    else if(data.equalsIgnoreCase("Callback Framed"        )){number=4 ;}
                    else if(data.equalsIgnoreCase("Outbound"               )){number=5 ;}
                    else if(data.equalsIgnoreCase("Administrative"         )){number=6 ;}
                    else if(data.equalsIgnoreCase("NAS Prompt"             )){number=7 ;}
                    else if(data.equalsIgnoreCase("Authenticate Only"      )){number=8 ;}
                    else if(data.equalsIgnoreCase("Callback NAS Prompt"    )){number=9 ;}
                    else if(data.equalsIgnoreCase("Call Check"             )){number=10;}
                    else if(data.equalsIgnoreCase("Callback Administrative")){number=11;}
                    else if(data.equalsIgnoreCase("Voice"                  )){number=12;}
                    else if(data.equalsIgnoreCase("Fax"                    )){number=13;}
                    else if(data.equalsIgnoreCase("Modem Relay"            )){number=14;}
                    else if(data.equalsIgnoreCase("IAPP-Register"          )){number=15;}
                    else if(data.equalsIgnoreCase("IAPP-AP-Check"          )){number=16;}
                    else if(data.equalsIgnoreCase("Authorize Only"         )){number=17;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case FRAMED_PROTOCOL                    : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("PPP"                                              )){number=1;}
                    else if(data.equalsIgnoreCase("SLIP"                                             )){number=2;}
                    else if(data.equalsIgnoreCase("AppleTalk Remote Access Protocol (ARAP)"          )){number=3;}
                    else if(data.equalsIgnoreCase("Gandalf proprietary SingleLink/MultiLink protocol")){number=4;}
                    else if(data.equalsIgnoreCase("Xylogics proprietary IPX/SLIP"                    )){number=5;}
                    else if(data.equalsIgnoreCase("X.75 Synchronous"                                 )){number=6;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case FRAMED_ROUTING                     : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("None"                      )){number=0;}
                    else if(data.equalsIgnoreCase("Send routing packets"      )){number=1;}
                    else if(data.equalsIgnoreCase("Listen for routing packets")){number=2;}
                    else if(data.equalsIgnoreCase("Send and Listen"           )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case FRAMED_COMPRESSION                 : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("None"                        )){number=0;}
                    else if(data.equalsIgnoreCase("VJ TCP/IP header compression")){number=1;}
                    else if(data.equalsIgnoreCase("IPX header compression"      )){number=2;}
                    else if(data.equalsIgnoreCase("Stac-LZS compression"        )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ARAP_ZONE_ACCESS                   : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("User-Name"                    )){number=  1;}
                    else if(data.equalsIgnoreCase("User-Password"                )){number=  2;}
                    else if(data.equalsIgnoreCase("CHAP-Password"                )){number=  3;}
                    else if(data.equalsIgnoreCase("NAS-IP-Address"               )){number=  4;}
                    else if(data.equalsIgnoreCase("NAS-Port"                     )){number=  5;}
                    else if(data.equalsIgnoreCase("Service-Type"                 )){number=  6;}
                    else if(data.equalsIgnoreCase("Framed-Protocol"              )){number=  7;}
                    else if(data.equalsIgnoreCase("Framed-IP-Address"            )){number=  8;}
                    else if(data.equalsIgnoreCase("Framed-IP-Netmask"            )){number=  9;}
                    else if(data.equalsIgnoreCase("Framed-Routing"               )){number= 10;}
                    else if(data.equalsIgnoreCase("Filter-Id"                    )){number= 11;}
                    else if(data.equalsIgnoreCase("Framed-MTU"                   )){number= 12;}
                    else if(data.equalsIgnoreCase("Framed-Compression"           )){number= 13;}
                    else if(data.equalsIgnoreCase("Login-IP-Host"                )){number= 14;}
                    else if(data.equalsIgnoreCase("Login-Service"                )){number= 15;}
                    else if(data.equalsIgnoreCase("Login-TCP-Port"               )){number= 16;}
                    else if(data.equalsIgnoreCase("(unassigned)"                 )){number= 17;}
                    else if(data.equalsIgnoreCase("Reply-Message"                )){number= 18;}
                    else if(data.equalsIgnoreCase("Callback-Number"              )){number= 19;}
                    else if(data.equalsIgnoreCase("Callback-Id"                  )){number= 20;}
                    else if(data.equalsIgnoreCase("(unassigned)"                 )){number= 21;}
                    else if(data.equalsIgnoreCase("Framed-Route"                 )){number= 22;}
                    else if(data.equalsIgnoreCase("Framed-IPX-Network"           )){number= 23;}
                    else if(data.equalsIgnoreCase("State"                        )){number= 24;}
                    else if(data.equalsIgnoreCase("Class"                        )){number= 25;}
                    else if(data.equalsIgnoreCase("Vendor-Specific"              )){number= 26;}
                    else if(data.equalsIgnoreCase("Session-Timeout"              )){number= 27;}
                    else if(data.equalsIgnoreCase("Idle-Timeout"                 )){number= 28;}
                    else if(data.equalsIgnoreCase("Termination-Action"           )){number= 29;}
                    else if(data.equalsIgnoreCase("Called-Station-Id"            )){number= 30;}
                    else if(data.equalsIgnoreCase("Calling-Station-Id"           )){number= 31;}
                    else if(data.equalsIgnoreCase("NAS-Identifier"               )){number= 32;}
                    else if(data.equalsIgnoreCase("Proxy-State"                  )){number= 33;}
                    else if(data.equalsIgnoreCase("Login-LAT-Service"            )){number= 34;}
                    else if(data.equalsIgnoreCase("Login-LAT-Node"               )){number= 35;}
                    else if(data.equalsIgnoreCase("Login-LAT-Group"              )){number= 36;}
                    else if(data.equalsIgnoreCase("Framed-AppleTalk-Link"        )){number= 37;}
                    else if(data.equalsIgnoreCase("Framed-AppleTalk-Network"     )){number= 38;}
                    else if(data.equalsIgnoreCase("Framed-AppleTalk-Zone"        )){number= 39;}
                    else if(data.equalsIgnoreCase("Acct-Status-Type"             )){number= 40;}
                    else if(data.equalsIgnoreCase("Acct-Delay-Time"              )){number= 41;}
                    else if(data.equalsIgnoreCase("Acct-Input-Octets"            )){number= 42;}
                    else if(data.equalsIgnoreCase("Acct-Output-Octets"           )){number= 43;}
                    else if(data.equalsIgnoreCase("Acct-Session-Id"              )){number= 44;}
                    else if(data.equalsIgnoreCase("Acct-Authentic"               )){number= 45;}
                    else if(data.equalsIgnoreCase("Acct-Session-Time"            )){number= 46;}
                    else if(data.equalsIgnoreCase("Acct-Input-Packets"           )){number= 47;}
                    else if(data.equalsIgnoreCase("Acct-Output-Packets"          )){number= 48;}
                    else if(data.equalsIgnoreCase("Acct-Terminate-Cause"         )){number= 49;}
                    else if(data.equalsIgnoreCase("Acct-Multi-Session-Id"        )){number= 50;}
                    else if(data.equalsIgnoreCase("Acct-Link-Count"              )){number= 51;}
                    else if(data.equalsIgnoreCase("Acct-Input-Gigawords"         )){number= 52;}
                    else if(data.equalsIgnoreCase("Acct-Output-Gigawords"        )){number= 53;}
                    else if(data.equalsIgnoreCase("(unassigned)"                 )){number= 54;}
                    else if(data.equalsIgnoreCase("Event-Timestamp"              )){number= 55;}
                    else if(data.equalsIgnoreCase("Egress-VLANID"                )){number= 56;}
                    else if(data.equalsIgnoreCase("Ingress-Filters"              )){number= 57;}
                    else if(data.equalsIgnoreCase("Egress-VLAN-Name"             )){number= 58;}
                    else if(data.equalsIgnoreCase("User-Priority-Table"          )){number= 59;}
                    else if(data.equalsIgnoreCase("CHAP-Challenge"               )){number= 60;}
                    else if(data.equalsIgnoreCase("NAS-Port-Type"                )){number= 61;}
                    else if(data.equalsIgnoreCase("Port-Limit"                   )){number= 62;}
                    else if(data.equalsIgnoreCase("Login-LAT-Port"               )){number= 63;}
                    else if(data.equalsIgnoreCase("Tunnel-Type"                  )){number= 64;}
                    else if(data.equalsIgnoreCase("Tunnel-Medium-Type"           )){number= 65;}
                    else if(data.equalsIgnoreCase("Tunnel-Client-Endpoint"       )){number= 66;}
                    else if(data.equalsIgnoreCase("Tunnel-Server-Endpoint"       )){number= 67;}
                    else if(data.equalsIgnoreCase("Acct-Tunnel-Connection"       )){number= 68;}
                    else if(data.equalsIgnoreCase("Tunnel-Password"              )){number= 69;}
                    else if(data.equalsIgnoreCase("ARAP-Password"                )){number= 70;}
                    else if(data.equalsIgnoreCase("ARAP-Features"                )){number= 71;}
                    else if(data.equalsIgnoreCase("ARAP-Zone-Access"             )){number= 72;}
                    else if(data.equalsIgnoreCase("ARAP-Security"                )){number= 73;}
                    else if(data.equalsIgnoreCase("ARAP-Security-Data"           )){number= 74;}
                    else if(data.equalsIgnoreCase("Password-Retry"               )){number= 75;}
                    else if(data.equalsIgnoreCase("Prompt"                       )){number= 76;}
                    else if(data.equalsIgnoreCase("Connect-Info"                 )){number= 77;}
                    else if(data.equalsIgnoreCase("Configuration-Token"          )){number= 78;}
                    else if(data.equalsIgnoreCase("EAP-Message"                  )){number= 79;}
                    else if(data.equalsIgnoreCase("Message-Authenticator"        )){number= 80;}
                    else if(data.equalsIgnoreCase("Tunnel-Private-Group-ID"      )){number= 81;}
                    else if(data.equalsIgnoreCase("Tunnel-Assignment-ID"         )){number= 82;}
                    else if(data.equalsIgnoreCase("Tunnel-Preference"            )){number= 83;}
                    else if(data.equalsIgnoreCase("ARAP-Challenge-Response"      )){number= 84;}
                    else if(data.equalsIgnoreCase("Acct-Interim-Interval"        )){number= 85;}
                    else if(data.equalsIgnoreCase("Acct-Tunnel-Packets-Lost"     )){number= 86;}
                    else if(data.equalsIgnoreCase("NAS-Port-Id"                  )){number= 87;}
                    else if(data.equalsIgnoreCase("Framed-Pool"                  )){number= 88;}
                    else if(data.equalsIgnoreCase("CUI"                          )){number= 89;}
                    else if(data.equalsIgnoreCase("Tunnel-Client-Auth-ID"        )){number= 90;}
                    else if(data.equalsIgnoreCase("Tunnel-Server-Auth-ID"        )){number= 91;}
                    else if(data.equalsIgnoreCase("NAS-Filter-Rule"              )){number= 92;}
                    else if(data.equalsIgnoreCase("(Unassigned)"                 )){number= 93;}
                    else if(data.equalsIgnoreCase("Originating-Line-Info"        )){number= 94;}
                    else if(data.equalsIgnoreCase("NAS-IPv6-Address"             )){number= 95;}
                    else if(data.equalsIgnoreCase("Framed-Interface-Id"          )){number= 96;}
                    else if(data.equalsIgnoreCase("Framed-IPv6-Prefix"           )){number= 97;}
                    else if(data.equalsIgnoreCase("Login-IPv6-Host"              )){number= 98;}
                    else if(data.equalsIgnoreCase("Framed-IPv6-Route"            )){number= 99;}
                    else if(data.equalsIgnoreCase("Framed-IPv6-Pool"             )){number=100;}
                    else if(data.equalsIgnoreCase("Error-Cause Attribute"        )){number=101;}
                    else if(data.equalsIgnoreCase("EAP-Key-Name"                 )){number=102;}
                    else if(data.equalsIgnoreCase("Digest-Response"              )){number=103;}
                    else if(data.equalsIgnoreCase("Digest-Realm"                 )){number=104;}
                    else if(data.equalsIgnoreCase("Digest-Nonce"                 )){number=105;}
                    else if(data.equalsIgnoreCase("Digest-Response-Auth"         )){number=106;}
                    else if(data.equalsIgnoreCase("Digest-Nextnonce"             )){number=107;}
                    else if(data.equalsIgnoreCase("Digest-Method"                )){number=108;}
                    else if(data.equalsIgnoreCase("Digest-URI"                   )){number=109;}
                    else if(data.equalsIgnoreCase("Digest-Qop"                   )){number=110;}
                    else if(data.equalsIgnoreCase("Digest-Algorithm"             )){number=111;}
                    else if(data.equalsIgnoreCase("Digest-Entity-Body-Hash"      )){number=112;}
                    else if(data.equalsIgnoreCase("Digest-CNonce"                )){number=113;}
                    else if(data.equalsIgnoreCase("Digest-Nonce-Count"           )){number=114;}
                    else if(data.equalsIgnoreCase("Digest-Username"              )){number=115;}
                    else if(data.equalsIgnoreCase("Digest-Opaque"                )){number=116;}
                    else if(data.equalsIgnoreCase("Digest-Auth-Param"            )){number=117;}
                    else if(data.equalsIgnoreCase("Digest-AKA-Auts"              )){number=118;}
                    else if(data.equalsIgnoreCase("Digest-Domain"                )){number=119;}
                    else if(data.equalsIgnoreCase("Digest-Stale"                 )){number=120;}
                    else if(data.equalsIgnoreCase("Digest-HA1"                   )){number=121;}
                    else if(data.equalsIgnoreCase("SIP-AOR"                      )){number=122;}
                    else if(data.equalsIgnoreCase("Delegated-IPv6-Prefix"        )){number=123;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case LOGIN_SERVICE                      : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("Telnet"         )){number=0;}
                    else if(data.equalsIgnoreCase("Rlogin"         )){number=1;}
                    else if(data.equalsIgnoreCase("Timestamps"     )){number=2;}
                    else if(data.equalsIgnoreCase("TCP Clear"      )){number=3;}
                    else if(data.equalsIgnoreCase("PortMaster"     )){number=4;}
                    else if(data.equalsIgnoreCase("LAT"            )){number=5;}
                    else if(data.equalsIgnoreCase("X25-PAD"        )){number=6;}
                    else if(data.equalsIgnoreCase("X25-T3POS"      )){number=7;}
                    else if(data.equalsIgnoreCase("TCP Clear Quiet")){number=8;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case TUNNEL_TYPE                        : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("Point-to-Point Tunneling Protocol (PPTP)"                  )){number=1 ;}
                    else if(data.equalsIgnoreCase("Layer Two Forwarding (L2F)"                                )){number=2 ;}
                    else if(data.equalsIgnoreCase("Layer Two Tunneling Protocol (L2TP)"                       )){number=3 ;}
                    else if(data.equalsIgnoreCase("Ascend Tunnel Management Protocol (ATMP)"                  )){number=4 ;}
                    else if(data.equalsIgnoreCase("Virtual Tunneling Protocol (VTP)"                          )){number=5 ;}
                    else if(data.equalsIgnoreCase("IP Authentication Header in the Tunnel-mode (AH)"          )){number=6 ;}
                    else if(data.equalsIgnoreCase("IP-in-IP Encapsulation (IP-IP)"                            )){number=7 ;}
                    else if(data.equalsIgnoreCase("Minimal IP-in-IP Encapsulation (MIN-IP-IP)"                )){number=8 ;}
                    else if(data.equalsIgnoreCase("IP Encapsulating Security Payload in the Tunnel-mode (ESP)")){number=9 ;}
                    else if(data.equalsIgnoreCase("Generic Route Encapsulation (GRE)"                         )){number=10;}
                    else if(data.equalsIgnoreCase("Bay Dial Virtual Services (DVS)"                           )){number=11;}
                    else if(data.equalsIgnoreCase("IP-in-IP Tunneling"                                        )){number=12;}
                    else if(data.equalsIgnoreCase("Virtual LANs (VLAN)"                                       )){number=13;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case TUNNEL_MEDIUM_TYPE                 : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("IPv4 (IP version 4)"                                         )){number=1 ;}
                    else if(data.equalsIgnoreCase("IPv6 (IP version 6)"                                         )){number=2 ;}
                    else if(data.equalsIgnoreCase("NSAP"                                                        )){number=3 ;}
                    else if(data.equalsIgnoreCase("HDLC (8-bit multidrop)"                                      )){number=4 ;}
                    else if(data.equalsIgnoreCase("BBN 1822"                                                    )){number=5 ;}
                    else if(data.equalsIgnoreCase("802 (includes all 802 media plus 'Ethernet canonicalformat')")){number=6 ;}
                    else if(data.equalsIgnoreCase("E.163 (POTS)"                                                )){number=7 ;}
                    else if(data.equalsIgnoreCase("E.164 (SMDS, Frame Relay, ATM)"                              )){number=8 ;}
                    else if(data.equalsIgnoreCase("F.69 (Telex)"                                                )){number=9 ;}
                    else if(data.equalsIgnoreCase("X.121 (X.25, Frame Relay)"                                   )){number=10;}
                    else if(data.equalsIgnoreCase("IPX"                                                         )){number=11;}
                    else if(data.equalsIgnoreCase("Appletalk"                                                   )){number=12;}
                    else if(data.equalsIgnoreCase("Decnet IV"                                                   )){number=13;}
                    else if(data.equalsIgnoreCase("Banyan Vines"                                                )){number=14;}
                    else if(data.equalsIgnoreCase("E.164 with NSAP format subaddress"                           )){number=15;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ACCT_AUTHENTIC                     : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("RADIUS"  )){number=1;}
                    else if(data.equalsIgnoreCase("Local"   )){number=2;}
                    else if(data.equalsIgnoreCase("Remote"  )){number=3;}
                    else if(data.equalsIgnoreCase("Diameter")){number=4;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ACOUNTING_AUTH_METHOD              : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("PAP"      )){number=1;}
                    else if(data.equalsIgnoreCase("CHAP"     )){number=2;}
                    else if(data.equalsIgnoreCase("MS-CHAP-1")){number=3;}
                    else if(data.equalsIgnoreCase("MS-CHAP-2")){number=4;}
                    else if(data.equalsIgnoreCase("EAP"      )){number=5;}
                    else if(data.equalsIgnoreCase("None"     )){number=7;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ORIGIN_AAA_PROTOCOL                : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(data.equalsIgnoreCase("RADIUS")){number=1 ;}
                    else{number=1;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SIP_SERVER_ASSIGNMENT_TYPE         : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("NO_ASSIGNMENT"                           )){number=0 ;}
                    else if(data.equalsIgnoreCase("REGISTRATION"                            )){number=1 ;}
                    else if(data.equalsIgnoreCase("RE_REGISTRATION"                         )){number=2 ;}
                    else if(data.equalsIgnoreCase("UNREGISTERED_USER"                       )){number=3 ;}
                    else if(data.equalsIgnoreCase("TIMEOUT_DEREGISTRATION"                  )){number=4 ;}
                    else if(data.equalsIgnoreCase("USER_DEREGISTRATION"                     )){number=5 ;}
                    else if(data.equalsIgnoreCase("TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME")){number=6 ;}
                    else if(data.equalsIgnoreCase("USER_DEREGISTRATION_STORE_SERVER_NAME"   )){number=7 ;}
                    else if(data.equalsIgnoreCase("ADMINISTRATIVE_DEREGISTRATION"           )){number=8 ;}
                    else if(data.equalsIgnoreCase("AUTHENTICATION_FAILURE"                  )){number=9 ;}
                    else if(data.equalsIgnoreCase("AUTHENTICATION_TIMEOUT"                  )){number=10;}
                    else if(data.equalsIgnoreCase("DEREGISTRATION_TOO_MUCH_DATA"            )){number=11;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SIP_AUTHENTICATION_SCHEME          : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(data.equalsIgnoreCase("DIGEST")){number=0;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SIP_REASON_CODE                    : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("PERMANENT_TERMINATION"  )){number=0;}
                    else if(data.equalsIgnoreCase("NEW_SIP_SERVER_ASSIGNED")){number=1;}
                    else if(data.equalsIgnoreCase("SIP_SERVER_CHANGE"      )){number=2;}
                    else if(data.equalsIgnoreCase("REMOVE_SIP_SERVER"      )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SIP_USER_AUTHORIZATION_TYPE        : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("REGISTRATION"                  )){number=0;}
                    else if(data.equalsIgnoreCase("DEREGISTRATION"                )){number=1;}
                    else if(data.equalsIgnoreCase("REGISTRATION_AND_CAPABILITIES" )){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SIP_USER_DATA_ALREADY_AVAILABLE    : //    Enumerated
            {
                 try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("USER_DATA_NOT_AVAILABLE"       )){number=0;}
                    else if(data.equalsIgnoreCase("USER_DATA_ALREADY_AVAILABLE"   )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            // Swisscom:
            case ADDRESS_TYPE                       : //    Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("e-mail address"          )){number=0;}
                    else if(data.equalsIgnoreCase("MSISDN"                  )){number=1;}
                    else if(data.equalsIgnoreCase("IPv4 Address"            )){number=2;}
                    else if(data.equalsIgnoreCase("IPv6 Address"            )){number=3;}
                    else if(data.equalsIgnoreCase("Numeric Shortcode"       )){number=4;}
                    else if(data.equalsIgnoreCase("Alphanumeric Shortcode"  )){number=5;}
                    else if(data.equalsIgnoreCase("Other"                   )){number=6;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case CLASS_IDENTIFIER                   : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("Personal"            )){number=0;}
                    else if(data.equalsIgnoreCase("Advertisement"       )){number=1;}
                    else if(data.equalsIgnoreCase("Informational"       )){number=2;}
                    else if(data.equalsIgnoreCase("Auto"                )){number=3;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            // Gx specific AVPs
            case BEARER_USAGE                        : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("GENERAL"            )){number=0;}
                    else if(data.equalsIgnoreCase("IMS_SIGNALLING"     )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case EVENT_TRIGGER                       : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("SGSN_CHANGE"            )){number=0;}
                    else if(data.equalsIgnoreCase("QOS_CHANGE"             )){number=1;}
                    else if(data.equalsIgnoreCase("RAT_CHANGE"             )){number=2;}
                    else if(data.equalsIgnoreCase("TFT_CHANGE"             )){number=3;}
                    else if(data.equalsIgnoreCase("PLMN_CHANGE"            )){number=4;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case METERING_METHOD                     : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DURATION"         )){number=0;}
                    else if(data.equalsIgnoreCase("VOLUME"           )){number=1;}
                    else if(data.equalsIgnoreCase("DURATION_VOLUME"  )){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case OFFLINE                             : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DISABLE_OFFLINE"         )){number=0;}
                    else if(data.equalsIgnoreCase("ENABLE_OFFLINE"          )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ONLINE                              : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("DISABLE_ONLINE"         )){number=0;}
                    else if(data.equalsIgnoreCase("ENABLE_ONLINE"          )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case REPORTING_LEVEL                     : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("CHARGING_RULE_LEVEL"         )){number=0;}
                    else if(data.equalsIgnoreCase("RATING_GROUP_LEVEL"          )){number=1;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case PDP_SESSION_OPERATION               : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("PDP-SESSION-TERMINATION"      )){number=0;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case SESSION_RELEASE_CAUSE               : //   Enumerated
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {
                    if(     data.equalsIgnoreCase("UNSPECIFIED_REASON"             )){number=0;}
                    if(     data.equalsIgnoreCase("UE_SUBSCRIPTION_REASON"         )){number=1;}
                    if(     data.equalsIgnoreCase("INSUFFICIENT_SERVER_RESOURCES"  )){number=2;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case E2E_SEQUENCE                       : //    Grouped
            case EXPERIMENTAL_RESULT                : //    Grouped
            case FAILED_AVP                         : //    Grouped
            case PROXY_INFO                         : //    Grouped
            case VENDOR_SPECIFIC_APPLICATION_ID     : //    Grouped
            case CC_MONEY                           : //    Grouped
            case COST_INFORMATION                   : //    Grouped
            case FINAL_UNIT_INDICATION              : //    Grouped
            case GRANTED_SERVICE_UNIT               : //    Grouped
            case G_S_U_POOL_REFERENCE               : //    Grouped
            case MULTIPLE_SERVICES_CREDIT_CONTROL   : //    Grouped
            case REDIRECT_SERVER                    : //    Grouped
            case REQUESTED_SERVICE_UNIT             : //    Grouped
            case SERVICE_PARAMETER_INFO             : //    Grouped
            case SUBSCRIPTION_ID                    : //    Grouped
            case UNIT_VALUE                         : //    Grouped
            case USED_SERVICE_UNIT                  : //    Grouped
            case USER_EQUIPMENT_INFO                : //    Grouped
            case MIP_MN_AAA_AUTH                    : //    Grouped
            case MIP_ORIGINATING_FOREIGN_AAA        : //    Grouped
            case MIP_MN_TO_FA_MSA                   : //    Grouped
            case MIP_FA_TO_MN_MSA                   : //    Grouped
            case MIP_FA_TO_HA_MSA                   : //    Grouped
            case MIP_HA_TO_FA_MSA                   : //    Grouped
            case MIP_MN_TO_HA_MSA                   : //    Grouped
            case MIP_HA_TO_MN_MSA                   : //    Grouped
            case CHAP_AUTH					        : //    Grouped
            case TUNNELING					        : //    Grouped
            case SIP_ACCOUNTING_INFORMATION         : //    Grouped
            case SIP_SERVER_CAPABILITIES            : //    Grouped
            case SIP_AUTH_DATA_ITEM                 : //    Grouped
            case SIP_AUTHENTICATE                   : //    Grouped
            case SIP_AUTHORIZATION                  : //    Grouped
            case SIP_AUTHENTICATION_INFO            : //    Grouped
            case SIP_DEREGISTRATION_REASON          : //    Grouped
            case SIP_USER_DATA                      : //    Grouped
            // Swisscom:
            case SERVICE_INFORMATION                : //    Grouped
            case SMS_INFORMATION                    : //    Grouped
            case MMS_INFORMATION                    : //    Grouped
            case ORIGINATOR_ADDRESS                 : //    Grouped
            case ADDRESS_DOMAIN                     : //    Grouped
            case MESSAGE_CLASS                      : //    Grouped
            // XL
            case REMAINING_SERVICE_MONEY            : //    Grouped
            case UMB_CHARGING_GROUP                 : //    Grouped
            // Gx specific AVPs
            case CHARGING_RULE_INSTALL              : //    Grouped
            case CHARGING_RULE_REMOVE               : //    Grouped
            case CHARGING_RULE_DEFINITION           : //    Grouped
            case TFT_PACKET_FILTER_INFORMATION      : //    Grouped
            {
                Iterator<Diameter_AVP> avps=groupedAVPs.iterator();
                Vector<ByteArrayWrapper> avpBytes=new Vector<ByteArrayWrapper>();
                Diameter_AVP avp=null;
                byte[] avpEncoded=null;
                int length=0;
                while(avps.hasNext())
                {
                    avp=avps.next();
                    avpEncoded=avp.encode();
                    length=avpEncoded.length+length;
                    avpBytes.add(new ByteArrayWrapper(avpEncoded));
                }
                payload=new byte[length];
                Iterator<ByteArrayWrapper> it=avpBytes.iterator();
                int currentpointer=0;
                ByteArrayWrapper current=null;
                while(it.hasNext())
                {
                    current=it.next();
                    currentpointer=ConvertLib.addBytesToArray(payload,currentpointer,current.getArray());
                }
                break;
            }
            case EXPONENT                   : //    Integer32
            case REDIRECT_MODE 				: 	
                number=Long.decode(data);
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            case VALUE_DIGITS               : //    Integer64
                number=Long.decode(data);
                payload=ConvertLib.getByteArrayFromLong(number,8);
                break;
            case RESTRICTION_FILTER_RULE    : //    IPFiltrRule
            case MIP_FILTER_RULE            : //    IPFiltrRule
            case NAS_FILTER_RULE            : //    IPFiltrRule
            // Gx specific AVPs
            case TFT_FILTER                 : //    IPFiltrRule
                payload=ConvertLib.getBytesFromByteString(data);
                break;
            case QOS_FILTER_RULE            : //    QoSFltrRule
                payload=ConvertLib.getBytesFromByteString(data);
                break;
            case ACCOUNTING_SESSION_ID      : //    OctetString
            case CLASS                      : //    OctetString
            case PROXY_STATE                : //    OctetString
            case CC_CORRELATION_ID          : //    OctetString
            case SERVICE_PARAMETER_VALUE    : //    OctetString
            case USER_EQUIPMENT_INFO_VALUE  : //    OctetString
            case MIP_REG_REQUEST            : //    OctetString
            case MIP_REG_REPLY              : //    OctetString
            case MIP_FA_CHALLENGE           : //    OctetString
            case MIP_NONCE                  : //    OctetString
            case MIP_SESSION_KEY            : //    OctetString
            case ORIGINATING_LINE_INFO      : //    OctetString
            case USER_PASSWORD              : //    OctetString
            case CHAP_IDENT                 : //    OctetString
            case CHAP_RESPONSE              : //    OctetString
            case CHAP_CHALLENGE             : //    OctetString
            case ARAP_PASSWORD              : //    OctetString
            case ARAP_CHALLENGE_RESPONSE    : //    OctetString
            case ARAP_SECURITY_DATA         : //    OctetString
            case CONFIGURATION_TOKEN        : //    OctetString
            case FRAMED_IP_ADDRESS          : //    OctetString
            case FRAMED_IP_NETMASK          : //    OctetString
            case FRAMED_POOL                : //    OctetString
            case FRAMED_IPV6_PREFIX         : //    OctetString
            case FRAMED_IPV6_POOL           : //    OctetString
            case FRAMED_APPLETALK_ZONE      : //    OctetString
            case ARAP_FEATURES              : //    OctetString
            case LOGIN_IP_HOST              : //    OctetString
            case LOGIN_IPV6_HOST            : //    OctetString
            case LOGIN_LAT_SERVICE          : //    OctetString
            case LOGIN_LAT_NODE             : //    OctetString
            case LOGIN_LAT_GROUP            : //    OctetString
            case LOGIN_LAT_PORT             : //    OctetString
            case TUNNEL_PASSWORD            : //    OctetString
            case TUNNEL_PRIVATE_GROUP_ID    : //    OctetString
            case TUNNEL_ASSIGNMENT_ID       : //    OctetString
            case ACCT_TUNNEL_CONNECTION     : //    OctetString
            case NAS_IP_ADDRESS             : //    OctetString
            case NAS_IPV6_ADDRESS           : //    OctetString
            case STATE                      : //    OctetString
            case SIP_USER_DATA_CONTENTS     : //    OctetString
            // Gx specific AVPs
            case CHARGING_RULE_NAME         : //    OctetString
            case TOS_TRAFFIC_CLASS          : //    OctetString
                payload=ConvertLib.getBytesFromByteString(data);
                break;
            case EVENT_TIMESTAMP            : //    Time
            case TARIFF_TIME_CHANGE         : //    Time
                number=Long.decode(data);
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            case RESULT_CODE                : //    Unsigned32
            {
                try
                {
                    number=Integer.decode(data);
                } catch(NumberFormatException nex)
                {

                    if(     data.equalsIgnoreCase("DIAMETER_MULTI_ROUND_AUTH"              )){number=DIAMETER_MULTI_ROUND_AUTH              ;}
                    else if(data.equalsIgnoreCase("DIAMETER_SUCCESS"                       )){number=DIAMETER_SUCCESS                       ;}
                    else if(data.equalsIgnoreCase("DIAMETER_LIMITED_SUCCESS"               )){number=DIAMETER_LIMITED_SUCCESS               ;}
                    else if(data.equalsIgnoreCase("DIAMETER_COMMAND_UNSUPPORTED"           )){number=DIAMETER_COMMAND_UNSUPPORTED           ;}
                    else if(data.equalsIgnoreCase("DIAMETER_UNABLE_TO_DELIVER"             )){number=DIAMETER_UNABLE_TO_DELIVER             ;}
                    else if(data.equalsIgnoreCase("DIAMETER_REALM_NOT_SERVED"              )){number=DIAMETER_REALM_NOT_SERVED              ;}
                    else if(data.equalsIgnoreCase("DIAMETER_TOO_BUSY"                      )){number=DIAMETER_TOO_BUSY                      ;}
                    else if(data.equalsIgnoreCase("DIAMETER_LOOP_DETECTED"                 )){number=DIAMETER_LOOP_DETECTED                 ;}
                    else if(data.equalsIgnoreCase("DIAMETER_REDIRECT_INDICATION"           )){number=DIAMETER_REDIRECT_INDICATION           ;}
                    else if(data.equalsIgnoreCase("DIAMETER_APPLICATION_UNSUPPORTED"       )){number=DIAMETER_APPLICATION_UNSUPPORTED       ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_HDR_BITS"              )){number=DIAMETER_INVALID_HDR_BITS              ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_AVP_BITS"              )){number=DIAMETER_INVALID_AVP_BITS              ;}
                    else if(data.equalsIgnoreCase("DIAMETER_UNKNOWN_PEER"                  )){number=DIAMETER_UNKNOWN_PEER                  ;}
                    else if(data.equalsIgnoreCase("DIAMETER_AUTHENTICATION_REJECTED"       )){number=DIAMETER_AUTHENTICATION_REJECTED       ;}
                    else if(data.equalsIgnoreCase("DIAMETER_OUT_OF_SPACE"                  )){number=DIAMETER_OUT_OF_SPACE                  ;}
                    else if(data.equalsIgnoreCase("ELECTION_LOST"                          )){number=ELECTION_LOST                          ;}
                    else if(data.equalsIgnoreCase("DIAMETER_AVP_UNSUPPORTED"               )){number=DIAMETER_AVP_UNSUPPORTED               ;}
                    else if(data.equalsIgnoreCase("DIAMETER_UNKNOWN_SESSION_ID"            )){number=DIAMETER_UNKNOWN_SESSION_ID            ;}
                    else if(data.equalsIgnoreCase("DIAMETER_AUTHORIZATION_REJECTED"        )){number=DIAMETER_AUTHORIZATION_REJECTED        ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_AVP_VALUE"             )){number=DIAMETER_INVALID_AVP_VALUE             ;}
                    else if(data.equalsIgnoreCase("DIAMETER_MISSING_AVP"                   )){number=DIAMETER_MISSING_AVP                   ;}
                    else if(data.equalsIgnoreCase("DIAMETER_RESOURCES_EXCEEDED"            )){number=DIAMETER_RESOURCES_EXCEEDED            ;}
                    else if(data.equalsIgnoreCase("DIAMETER_CONTRADICTING_AVPS"            )){number=DIAMETER_CONTRADICTING_AVPS            ;}
                    else if(data.equalsIgnoreCase("DIAMETER_AVP_NOT_ALLOWED"               )){number=DIAMETER_AVP_NOT_ALLOWED               ;}
                    else if(data.equalsIgnoreCase("DIAMETER_AVP_OCCURS_TOO_MANY_TIMES"     )){number=DIAMETER_AVP_OCCURS_TOO_MANY_TIMES     ;}
                    else if(data.equalsIgnoreCase("DIAMETER_NO_COMMON_APPLICATION"         )){number=DIAMETER_NO_COMMON_APPLICATION         ;}
                    else if(data.equalsIgnoreCase("DIAMETER_UNSUPPORTED_VERSION"           )){number=DIAMETER_UNSUPPORTED_VERSION           ;}
                    else if(data.equalsIgnoreCase("DIAMETER_UNABLE_TO_COMPLY"              )){number=DIAMETER_UNABLE_TO_COMPLY              ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_BIT_IN_HEADER"         )){number=DIAMETER_INVALID_BIT_IN_HEADER         ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_AVP_LENGTH"            )){number=DIAMETER_INVALID_AVP_LENGTH            ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_MESSAGE_LENGTH"        )){number=DIAMETER_INVALID_MESSAGE_LENGTH        ;}
                    else if(data.equalsIgnoreCase("DIAMETER_INVALID_AVP_BIT_COMBO"         )){number=DIAMETER_INVALID_AVP_BIT_COMBO         ;}
                    else if(data.equalsIgnoreCase("DIAMETER_NO_COMMON_SECURITY"            )){number=DIAMETER_NO_COMMON_SECURITY            ;}
                    // Credit control
                    else if(data.equalsIgnoreCase("DIAMETER_END_USER_SERVICE_DENIED"       )){number=DIAMETER_END_USER_SERVICE_DENIED       ;}
                    else if(data.equalsIgnoreCase("DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE" )){number=DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE ;}
                    else if(data.equalsIgnoreCase("DIAMETER_CREDIT_LIMIT_REACHED"          )){number=DIAMETER_CREDIT_LIMIT_REACHED          ;}
                    else if(data.equalsIgnoreCase("DIAMETER_USER_UNKNOWN"                  )){number=DIAMETER_USER_UNKNOWN                  ;}
                    else if(data.equalsIgnoreCase("DIAMETER_RATING_FAILED"                 )){number=DIAMETER_RATING_FAILED                 ;}
                    else{number=0;}
                }
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            }
            case ACCT_INTERIM_INTERVAL      : //    Unsigned32
            case ACCOUNTING_RECORD_NUMBER   : //    Unsigned32
            case ACCT_APPLICATION_ID        : //    Unsigned32
            case AUTH_APPLICATION_ID        : //    Unsigned32
            case AUTHORIZATION_LIFETIME     : //    Unsigned32
            case AUTH_GRACE_PERIOD          : //    Unsigned32
            case EXPERIMENTAL_RESULT_CODE   : //    Unsigned32
            case FIRMWARE_REVISION          : //    Unsigned32
            case INBAND_SECURITY_ID         : //    Unsigned32
            case MULTI_ROUND_TIME_OUT       : //    Unsigned32
            case ORIGIN_STATE_ID            : //    Unsigned32
            case REDIRECT_MAX_CACHE_TIME    : //    Unsigned32
            case SESSION_TIMEOUT            : //    Unsigned32
            case SESSION_BINDING            : //    Unsigned32
            case SUPPORTED_VENDOR_ID        : //    Unsigned32
            case VENDOR_ID                  : //    Unsigned32
            case CC_REQUEST_NUMBER          : //    Unsigned32
            case CC_TIME                    : //    Unsigned32
            case CURRENCY_CODE              : //    Unsigned32
            case G_S_U_POOL_IDENTIFIER      : //    Unsigned32
            case RATING_GROUP               : //    Unsigned32
            case SERVICE_IDENTIFIER         : //    Unsigned32
            case SERVICE_PARAMETER_TYPE     : //    Unsigned32
            case VALIDITY_TIME              : //    Unsigned32
            case MIP_FEATURE_VECTOR         : //    Unsigned32
            case MIP_AUTH_INPUT_DATA_LENGTH : //    Unsigned32
            case MIP_AUTHENTICATOR_LENGTH   : //    Unsigned32
            case MIP_AUTHENTICATOR_OFFSET   : //    Unsigned32
            case MIP_MN_AAA_SPI             : //    Unsigned32
            case MIP_FA_TO_HA_SPI           : //    Unsigned32
            case MIP_FA_TO_MN_SPI           : //    Unsigned32
            case MIP_HA_TO_FA_SPI           : //    Unsigned32
            case MIP_MSA_LIFETIME           : //    Unsigned32
            case NAS_PORT                   : //    Unsigned32
            case PASSWORD_RETRY             : //    Unsigned32
            case ARAP_SECURITY              : //    Unsigned32
            case IDLE_TIMEOUT               : //    Unsigned32
            case PORT_LIMIT                 : //    Unsigned32
            case FRAMED_MTU                 : //    Unsigned32
            case FRAMED_APPLETALK_LINK      : //    Unsigned32
            case FRAMED_APPLETALK_NETWORK   : //    Unsigned32
            case LOGIN_TCP_PORT             : //    Unsigned32
            case TUNNEL_PREFERENCE          : //    Unsigned32
            case ACCT_SESSION_TIME          : //    Unsigned32
            case ACCT_DELAY_TIME            : //    Unsigned32
            case ACCT_LINK_COUNT            : //    Unsigned32
            case ACCT_TUNNEL_PACKETS_LOST   : //    Unsigned32
            case SIP_MANDATORY_CAPABILITY   : //    Unsigned32
            case SIP_OPTIONAL_CAPABILITY    : //    Unsigned32
            case SIP_ITEM_NUMBER            : //    Unsigned32
            case SIP_NUMBER_AUTH_ITEMS      : //    Unsigned32
            // Gx specific AVPs
            case PRECEDENCE                 : //    Unsigned32
                number=Long.decode(data);
                payload=ConvertLib.getByteArrayFromLong(number,4);
                break;
            case ACCOUNTING_SUB_SESSION_ID  : //    Unsigned64
            case CC_INPUT_OCTETS            : //    Unsigned64
            case CC_OUTPUT_OCTETS           : //    Unsigned64
            case CC_SERVICE_SPECIFIC_UNITS  : //    Unsigned64
            case CC_SUB_SESSION_ID          : //    Unsigned64
            case CC_TOTAL_OCTETS            : //    Unsigned64
            case FRAMED_INTERFACE_ID        : //    Unsigned64
            case ACCOUNTING_INPUT_OCTETS    : //    Unsigned64
            case ACCOUNTING_OUTPUT_OCTETS   : //    Unsigned64
            case ACCOUNTING_INPUT_PACKETS   : //    Unsigned64
            case ACCOUNTING_OUTPUT_PACKETS  : //    Unsigned64
                number=Long.decode(data);
                payload=ConvertLib.getByteArrayFromLong(number,8);
                break;
            case ACCT_MULTI_SESSION_ID        : //   UTF8String
            case ERROR_MESSAGE                : //   UTF8String
            case PRODUCT_NAME                 : //   UTF8String
            case SESSION_ID                   : //   UTF8String
            case USER_NAME                    : //   UTF8String
            case COST_UNIT                    : //   UTF8String
            case REDIRECT_SERVER_ADDRESS      : //   UTF8String
            case SERVICE_CONTEXT_ID           : //   UTF8String
            case SUBSCRIPTION_ID_DATA         : //   UTF8String
            case NAS_PORT_ID                  : //   UTF8String
            case CALLED_STATION_ID            : //   UTF8String
            case CALLING_STATION_ID           : //   UTF8String
            case CONNECT_INFO                 : //   UTF8String
            case REPLY_MESSAGE                : //   UTF8String
            case CALLBACK_NUMBER              : //   UTF8String
            case CALLBACK_ID                  : //   UTF8String
            case FILTER_ID                    : //   UTF8String
            case FRAMED_ROUTE                 : //   UTF8String
            case FRAMED_IPV6_ROUTE            : //   UTF8String
            case FRAMED_IPX_NETWORK           : //   UTF8String
            case TUNNEL_CLIENT_ENDPOINT       : //   UTF8String
            case TUNNEL_SERVER_ENDPOINT       : //   UTF8String
            case TUNNEL_CLIENT_AUTH_ID        : //   UTF8String
            case TUNNEL_SERVER_AUTH_ID        : //   UTF8String
            case NAS_IDENTIFIER               : //   UTF8String
            case SIP_SERVER_URI               : //   UTF8String
            case SIP_REASON_INFO              : //   UTF8String
            case SIP_VISITED_NETWORK_ID       : //   UTF8String
            case SIP_SUPPORTED_USER_DATA_TYPE : //   UTF8String
            case SIP_USER_DATA_TYPE           : //   UTF8String
            case SIP_METHOD                   : //   UTF8String
            // Swisscom:
            case ADDRESS_DATA                 : //   UTF8String
            case DOMAIN_NAME                  : //   UTF8String
            case MESSAGE_ID                   : //   UTF8String
            case TOKEN_TEXT                   : //   UTF8String
            // XL
            case VAS_TYPE                     : //   UTF8String
            case MESSAGE_TYPE                 : //   UTF8String
            case SDC                          : //   UTF8String
            // Gx specific AVPs
            case CHARGING_RULE_BASE_NAME      : //   UTF8String
                payload=ConvertLib.createBytes(data);
                break;
            default:
                payload=ConvertLib.getBytesFromByteString(data);
                break;
        }
        datalength=8+payload.length;

        if(vendorID>0)
        {
            avpflags=avpflags|AFLAG_VENDOR;
            datalength=datalength+4;
        }
        if(mandatory)
        {
            avpflags=avpflags|AFLAG_MANDATORY;
        }
        if(protectedf)
        {
            avpflags=avpflags|AFLAG_PROTECTED;
        }

        int padding=4-(datalength%4);
        if(datalength%4==0)padding=0;

        byte[] returnValue=new byte[datalength+padding];

        int pointer=0;
        int useavpcode=avpcode;
        if(overrideavpcode>0)useavpcode=overrideavpcode;
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(useavpcode,4));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(avpflags,1));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(datalength,3));
        if(vendorID>0)
        {
            pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(vendorID,4));
        }
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,payload);
        return returnValue;
    }

    public String toString()
    {
        return avpcode+"/"+avpname+"/"+mandatory+"/"+protectedf+"/"+vendorspec;
    }
}
