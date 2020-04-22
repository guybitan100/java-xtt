/*
 * $RCSfile: SMPPConstants.java,v $
 * $Revision: 1.4 $
 * $Date: 2007/05/23 11:43:11 $
 * 724 Solutions Inc. Copyright (c) 2005
 */

package com.mobixell.xtt;

/**
 * SMPPConstants
 * <p>
 * Holds values used by the SMPPWorker class
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface SMPPConstants
{

    // SMPP data types
    public static final int C_OCTET_STRING_TYPE     = 0;
    public static final int OCTET_STRING_TYPE       = 1;
    public static final int INTEGER_TYPE            = 2;

    // SUBMIT_MULTI destination type flags
    public static final int SME_ADDRESS             = 1;
    public static final int DISTRIBUTION_LIST_NAME  = 2;

    // SMPP command IDs 3.4
    public static final int BIND_RECEIVER           = 0x00000001;
    public static final int BIND_RECEIVER_RESP      = 0x80000001;
    public static final int BIND_TRANSMITTER        = 0x00000002;
    public static final int BIND_TRANSMITTER_RESP   = 0x80000002;
    public static final int BIND_TRANSCEIVER        = 0x00000009;
    public static final int BIND_TRANSCEIVER_RESP   = 0x80000009;
    public static final int UNBIND                  = 0x00000006;
    public static final int UNBIND_RESP             = 0x80000006;
    public static final int SUBMIT_SM               = 0x00000004;
    public static final int SUBMIT_SM_RESP          = 0x80000004;
    public static final int DELIVER_SM              = 0x00000005;
    public static final int DELIVER_SM_RESP         = 0x80000005;
    public static final int QUERY_SM                = 0x00000003;
    public static final int QUERY_SM_RESP           = 0x80000003;
    public static final int CANCEL_SM               = 0x00000008;
    public static final int CANCEL_SM_RESP          = 0x80000008;
    public static final int REPLACE_SM              = 0x00000007;
    public static final int REPLACE_SM_RESP         = 0x80000007;
    public static final int ENQUIRE_LINK            = 0x00000015;
    public static final int ENQUIRE_LINK_RESP       = 0x80000015;
    public static final int SUBMIT_MULTI            = 0x00000021;
    public static final int SUBMIT_MULTI_RESP       = 0x80000021;
    public static final int DATA_SM                 = 0x00000103;
    public static final int DATA_SM_RESP            = 0x80000103;
    public static final int GENERIC_NAK             = 0x80000000;
    // SMPP command IDs 5.0
    public static final int OUTBIND                 = 0x0000000B;
    public static final int ALERT_NOTIFICATION      = 0x00000102;
    public static final int BROADCAST_SM            = 0x00000111;
    public static final int BROADCAST_SM_RESP       = 0x80000111;
    public static final int QUERY_BROADCAST_SM      = 0x00000112;
    public static final int QUERY_BROADCAST_SM_RESP = 0x80000112;
    public static final int CANCEL_BROADCAST_SM     = 0x00000113;
    public static final int CANCEL_BROADCAST_SM_RESP= 0x80000113;


    // Query Status values
    public static final byte ENROUTE                = (byte) 0x01;
    public static final byte DELIVERED              = (byte) 0x02;
    public static final byte EXPIRED                = (byte) 0x03;
    public static final byte DELETED                = (byte) 0x04;
    public static final byte UNDELIVERABLE          = (byte) 0x05;
    public static final byte ACCEPTED               = (byte) 0x06;
    public static final byte UNKNOWN                = (byte) 0x07;
    public static final byte REJECTED               = (byte) 0x08;
    public static final byte SKIPPED                = (byte) 0x09;

    // SMSC Error Codes 3.4
    public static final int ESME_ROK                = 0x00000000; // No Error
    public static final int ESME_RINVMSGLEN         = 0x00000001; // Message Length is invalid
    public static final int ESME_RINVCMDLEN         = 0x00000002; // Command Length is invalid
    public static final int ESME_RINVCMDID          = 0x00000003; // Invalid Command ID
    public static final int ESME_RINVBNDSTS         = 0x00000004; // Incorrect BIND Status for given command
    public static final int ESME_RALYBND            = 0x00000005; // ESME Already in Bound State
    public static final int ESME_RINVPRTFLG         = 0x00000006; // Invalid Priority Flag
    public static final int ESME_RINVREGDLVFLG      = 0x00000007; // Invalid Registered Delivery Flag
    public static final int ESME_RSYSERR            = 0x00000008; // System Error
                                                                  // Reserved     = 0x00000009 Reserved
    public static final int ESME_RINVSRCADR         = 0x0000000A; // Invalid Source Address
    public static final int ESME_RINVDSTADR         = 0x0000000B; // Invalid Dest Addr
    public static final int ESME_RINVMSGID          = 0x0000000C; // Message ID is invalid
    public static final int ESME_RBINDFAIL          = 0x0000000D; // Bind Failed
    public static final int ESME_RINVPASWD          = 0x0000000E; // Invalid Password
    public static final int ESME_RINVSYSID          = 0x0000000F; // Invalid System ID
                                                                  // Reserved     = 0x00000010 Reserved
    public static final int ESME_RCANCELFAIL        = 0x00000011; // Cancel SM Failed
                                                                  // Reserved     = 0x00000012 Reserved
    public static final int ESME_RREPLACEFAIL       = 0x00000013; // Replace SM Failed
    public static final int ESME_RMSGQFUL           = 0x00000014; // Message Queue Full
    public static final int ESME_RINVSERTYP         = 0x00000015; // Invalid Service Type
                                                                  // Reserved     = 0x00000016-0x00000032
    public static final int ESME_RINVNUMDESTS       = 0x00000033; // Invalid number of destinations
    public static final int ESME_RINVDLNAME         = 0x00000034; // Invalid Distribution List name
                                                                  // Reserved     = 0x00000035-0x0000003F
    public static final int ESME_RINVDESTFLAG       = 0x00000040; // Destination flag is invalid (submit_multi)
                                                                  // Reserved     = 0x00000041 Reserved
    public static final int ESME_RINVSUBREP         = 0x00000042; // Invalid ‘submit with replace request
                                                                  // (i.e. submit_sm with replace_if_present_flag set)
    public static final int ESME_RINVESMCLASS       = 0x00000043; // Invalid esm_class field data
    public static final int ESME_RCNTSUBDL          = 0x00000044; // Cannot Submit to Distribution List
    public static final int ESME_RSUBMITFAIL        = 0x00000045; // submit_sm or submit_multi failed
                                                                  // Reserved     = 0x00000046-0x00000047 Reserved
    public static final int ESME_RINVSRCTON         = 0x00000048; // Invalid Source address TON
    public static final int ESME_RINVSRCNPI         = 0x00000049; // Invalid Source address NPI
    public static final int ESME_RINVDSTTON         = 0x00000050; // Invalid Destination address TON
    public static final int ESME_RINVDSTNPI         = 0x00000051; // Invalid Destination address NPI
                                                                  // Reserved     = 0x00000052 Reserved
    public static final int ESME_RINVSYSTYP         = 0x00000053; // Invalid system_type field
    public static final int ESME_RINVREPFLAG        = 0x00000054; // Invalid replace_if_present flag
    public static final int ESME_RINVNUMMSGS        = 0x00000055; // Invalid number of messages
                                                                  // Reserved     = 0x00000056-0x00000057 Reserved
    public static final int ESME_RTHROTTLED         = 0x00000058; // Throttling error (ESME has exceeded allowed message limits)
                                                                  // Reserved     = 0x00000059-0x00000060 Reserved
    public static final int ESME_RINVSCHED          = 0x00000061; // Invalid Scheduled Delivery Time
    public static final int ESME_RINVEXPIRY         = 0x00000062; // Invalid message validity period (Expiry time)
    public static final int ESME_RINVDFTMSGID       = 0x00000063; // Predefined Message Invalid or Not Found
    public static final int ESME_RX_T_APPN          = 0x00000064; // ESME Receiver Temporary App Error Code
    public static final int ESME_RX_P_APPN          = 0x00000065; // ESME Receiver Permanent App Error Code
    public static final int ESME_RX_R_APPN          = 0x00000066; // ESME Receiver Reject Message Error Code
    public static final int ESME_RQUERYFAIL         = 0x00000067; // query_sm request failed
                                                                  // Reserved     = 0x00000068-0x000000BF Reserved
    public static final int ESME_RINVOPTPARSTREAM   = 0x000000C0; // Error in the optional part of the PDU Body.
    public static final int ESME_ROPTPARNOTALLWD    = 0x000000C1; // Optional Parameter not allowed
    public static final int ESME_RINVPARLEN         = 0x000000C2; // Invalid Parameter Length.
    public static final int ESME_RMISSINGOPTPARAM   = 0x000000C3; // Expected Optional Parameter missing
    public static final int ESME_RINVOPTPARAMVAL    = 0x000000C4; // Invalid Optional Parameter Value
                                                                  // Reserved  = 0x000000C5-0x000000FD Reserved
    public static final int ESME_RDELIVERYFAILURE   = 0x000000FE; // Delivery Failure (used for data_sm_resp)
    public static final int ESME_RUNKNOWNERR        = 0x000000FF; // Unknown Error
    // SMSC Error Codes 5.0
    public static final int ESME_RSERTYPUNAUTH      = 0x00000100; //  ESME Not authorised to use specified
    public static final int ESME_RPROHIBITED        = 0x00000101; //  ESME Prohibited from using specified
    public static final int ESME_RSERTYPUNAVAIL     = 0x00000102; //  Specified service_type is unavailable.
    public static final int ESME_RSERTYPDENIED      = 0x00000103; //  Specified service_type is denied.
    public static final int ESME_RINVDCS            = 0x00000104; //  Invalid Data Coding Scheme.
    public static final int ESME_RINVSRCADDRSUBUNIT = 0x00000105; //  Source Address Sub unit is Invalid.
    public static final int ESME_RINVDSTADDRSUBUNIT = 0x00000106; //  Destination Address Sub unit is Invalid.
    public static final int ESME_RINVBCASTFREQINT   = 0x00000107; //  Broadcast Frequency Interval is invalid.
    public static final int ESME_RINVBCASTALIAS_NAME= 0x00000108; //  Broadcast Alias Name is invalid.
    public static final int ESME_RINVBCASTAREAFMT   = 0x00000109; //  Broadcast Area Format is invalid.
    public static final int ESME_RINVNUMBCAST_AREAS = 0x0000010A; //  Number of Broadcast Areas is invalid.
    public static final int ESME_RINVBCASTCNTTYPE   = 0x0000010B; //  Broadcast Content Type is invalid.
    public static final int ESME_RINVBCASTMSGCLASS  = 0x0000010C; //  Broadcast Message Class is invalid.
    public static final int ESME_RBCASTFAIL         = 0x0000010D; //  broadcast_sm operation failed.
    public static final int ESME_RBCASTQUERYFAIL    = 0x0000010E; //  query_broadcast_sm operation failed.
    public static final int ESME_RBCASTCANCELFAIL   = 0x0000010F; //  cancel_broadcast_sm operation failed.
    public static final int ESME_RINVBCAST_REP      = 0x00000110; //  Number of Repeated Broadcasts is
    public static final int ESME_RINVBCASTSRVGRP    = 0x00000111; //  Broadcast Service Group is invalid.
    public static final int ESME_RINVBCASTCHANIND   = 0x00000112; //  Broadcast Channel Indicator is invalid.


    // Optional SMPP Data Fields 3.4
    public static final int OPT_DEST_ADDR_SUBUNIT           = 0x0005; // GSM
    public static final int OPT_DEST_NETWORK_TYPE           = 0x0006; // Generic
    public static final int OPT_DEST_BEARER_TYPE            = 0x0007; // Generic
    public static final int OPT_DEST_TELEMATICS_ID          = 0x0008; // GSM
    public static final int OPT_SOURCE_ADDR_SUBUNIT         = 0x000D; // GSM
    public static final int OPT_SOURCE_NETWORK_TYPE         = 0x000E; // Generic
    public static final int OPT_SOURCE_BEARER_TYPE          = 0x000F; // Generic
    public static final int OPT_SOURCE_TELEMATICS_ID        = 0x0010; // GSM
    public static final int OPT_QOS_TIME_TO_LIVE            = 0x0017; // Generic
    public static final int OPT_PAYLOAD_TYPE                = 0x0019; // Generic
    public static final int OPT_ADDITIONAL_STATUS_INFO_TEXT = 0x001D; // Generic
    public static final int OPT_RECEIPTED_MESSAGE_ID        = 0x001E; // Generic
    public static final int OPT_MS_MSG_WAIT_FACILITIES      = 0x0030; // GSM
    public static final int OPT_PRIVACY_INDICATOR           = 0x0201; // CDMA, TDMA
    public static final int OPT_SOURCE_SUBADDRESS           = 0x0202; // CDMA, TDMA
    public static final int OPT_DEST_SUBADDRESS             = 0x0203; // CDMA, TDMA
    public static final int OPT_USER_MESSAGE_REFERENCE      = 0x0204; // Generic
    public static final int OPT_USER_RESPONSE_CODE          = 0x0205; // CDMA, TDMA
    public static final int OPT_SOURCE_PORT                 = 0x020A; // Generic
    public static final int OPT_DESTINATION_PORT            = 0x020B; // Generic
    public static final int OPT_SAR_MSG_REF_NUM             = 0x020C; // Generic
    public static final int OPT_LANGUAGE_INDICATOR          = 0x020D; // CDMA, TDMA
    public static final int OPT_SAR_TOTAL_SEGMENTS          = 0x020E; // Generic
    public static final int OPT_SAR_SEGMENT_SEQNUM          = 0x020F; // Generic
    public static final int OPT_SC_INTERFACE_VERSION        = 0x0210; // Generic
    public static final int OPT_CALLBACK_NUM_PRES_IND       = 0x0302; // TDMA
    public static final int OPT_CALLBACK_NUM_ATAG           = 0x0303; // TDMA
    public static final int OPT_NUMBER_OF_MESSAGES          = 0x0304; // CDMA
    public static final int OPT_CALLBACK_NUM                = 0x0381; // CDMA, TDMA, GSM, iDEN
    public static final int OPT_DPF_RESULT                  = 0x0420; // Generic
    public static final int OPT_SET_DPF                     = 0x0421; // Generic
    public static final int OPT_MS_AVAILABILITY_STATUS      = 0x0422; // Generic
    public static final int OPT_NETWORK_ERROR_CODE          = 0x0423; // Generic
    public static final int OPT_MESSAGE_PAYLOAD             = 0x0424; // Generic
    public static final int OPT_DELIVERY_FAILURE_REASON     = 0x0425; // Generic
    public static final int OPT_MORE_MESSAGES_TO_SEND       = 0x0426; // GSM
    public static final int OPT_MESSAGE_STATE               = 0x0427; // Generic
    public static final int OPT_USSD_SERVICE_OP             = 0x0501; // GSM (USSD)
    public static final int OPT_DISPLAY_TIME                = 0x1201; // CDMA, TDMA
    public static final int OPT_SMS_SIGNAL                  = 0x1203; // TDMA
    public static final int OPT_MS_VALIDITY                 = 0x1204; // CDMA, TDMA
    public static final int OPT_ALERT_ON_MESSAGE_DELIVERY   = 0x130C; // CDMA
    public static final int OPT_ITS_REPLY_TYPE              = 0x1380; // CDMA
    public static final int OPT_ITS_SESSION_INFO            = 0x1383; // CDMA
    // Optional SMPP Data Fields 5.0
    public static final int OPT_CONGESTION_STATE            = 0x0428; // Generic
    public static final int OPT_BROADCAST_CHANNEL_INDICATOR = 0x0600; // GSM
    public static final int OPT_BROADCAST_CONTENT_TYPE      = 0x0601; // CDMA, TDMA, GSM
    public static final int OPT_BROADCAST_CONTENT_TYPE_INFO = 0x0602; // CDMA, TDMA
    public static final int OPT_BROADCAST_MESSAGE_CLASS     = 0x0603; // GSM
    public static final int OPT_BROADCAST_REP_NUM           = 0x0604; // GSM
    public static final int OPT_BROADCAST_FREQUENCY_INTERVAL= 0x0605; // CDMA, TDMA, GSM
    public static final int OPT_BROADCAST_AREA_IDENTIFIER   = 0x0606; // CDMA, TDMA, GSM
    public static final int OPT_BROADCAST_ERROR_STATUS      = 0x0607; // CDMA, TDMA, GSM
    public static final int OPT_BROADCAST_AREA_SUCCESS      = 0x0608; // GSM
    public static final int OPT_BROADCAST_END_TIME          = 0x0609; // CDMA, TDMA, GSM
    public static final int OPT_BROADCAST_SERVICE_GROUP     = 0x060A; // CDMA, TDMA
    public static final int OPT_BILLING_IDENTIFICATION      = 0x060B; // Generic
    public static final int OPT_SOURCE_NETWORK_ID           = 0x060D; // Generic
    public static final int OPT_DEST_NETWORK_ID             = 0x060E; // Generic
    public static final int OPT_SOURCE_NODE_ID              = 0x060F; // Generic
    public static final int OPT_DEST_NODE_ID                = 0x0610; // Generic
    public static final int OPT_DEST_ADDR_NP_RESOLUTION     = 0x0611; // CDMA, TDMA (US Only)
    public static final int OPT_DEST_ADDR_NP_INFORMATION    = 0x0612; // CDMA, TDMA (US Only)
    public static final int OPT_DEST_ADDR_NP_COUNTRY        = 0x0613; // CDMA, TDMA (US Only)

}




















