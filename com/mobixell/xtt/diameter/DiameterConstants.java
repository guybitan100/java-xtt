/*
 * $RCSfile: DiameterConstants.java,v $
 * $Revision: 1.5 $
 * $Date: 2011/04/18 12:17:18 $
 */

package com.mobixell.xtt.diameter;

/**
 * DiameterConstants
 * Holds values used by the DiameterWorker class
 * http://tools.ietf.org/html/rfc3588
 * http://tools.ietf.org/html/rfc4006
 * <pre><code>
 *
 * 3.  Diameter Header
 *
 * A summary of the Diameter header format is shown below.  The fields
 * are transmitted in network byte order.
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Version    |                 Message Length                |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | command flags |                  Command-Code                 |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Application-ID                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      Hop-by-Hop Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      End-to-End Identifier                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  AVPs ...
 * +-+-+-+-+-+-+-+-+-+-+-+-+-
 *
 * Command Flags
 *    The Command Flags field is eight bits.  The following bits are assigned:
 *
 *     0 1 2 3 4 5 6 7
 *    +-+-+-+-+-+-+-+-+
 *    |R P E T r r r r|
 *    +-+-+-+-+-+-+-+-+
 *
 * 4.1.  AVP Header
 *
 * The fields in the AVP header MUST be sent in network byte order.  The
 * format of the header is:
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           AVP Code                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V M P r r r r r|                  AVP Length                   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                        Vendor-ID (opt)                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Data ...
 * +-+-+-+-+-+-+-+-+
 *
 *  Unless otherwise noted, AVPs will have the following default AVP
 *    Flags field settings:
 *
 *       The 'M' bit MUST be set.  The 'V' bit MUST NOT be set.
 *
 * </code></pre>
 * @author Roger Soder
 * @version 1.0
 */
public interface DiameterConstants
{
    // Network Byte Order == Big Endian == Java

    // Packet Type
    public static final int DEFAULTPORT     = 3868;

    // Diameter Applications 32 bit
    public static final int DIAMETER_COMMON_MESSAGES        =0;// http://tools.ietf.org/html/rfc3588
    public static final int DIAMETER_NASREQ                 =1;// [NASREQ]
    public static final int DIAMETER_MOBILE_IP              =2;// [DIAMMIP]
    public static final int DIAMETER_BASE_ACCOUNTING        =3;
    public static final int DIAMETER_CREDIT_CONTROL         =4;// http://tools.ietf.org/html/rfc4006
    public static final int DIAMETER_RELAY                  =0xffffffff;

    // Diameter COMMAND flag masks 8 bit
    public static final int CFLAG_REQUEST      = 128; // Request
    public static final int CFLAG_PROXIABLE    =  64; // Proxiable
    public static final int CFLAG_ERROR        =  32; // Error
    public static final int CFLAG_TRETR        =  16;

    // Command Codes 24 bit
    public static final int ASR         = 274; //  8.5.1 Abort-Session-Request
    public static final int ASA         = 274; //  8.5.2 Abort-Session-Answer
    public static final int ACR         = 271; //  9.7.1 Accounting-Request
    public static final int ACA         = 271; //  9.7.2 Accounting-Answer
    public static final int CER         = 257; //  5.3.1 Capabilities-Exchange-Request
    public static final int CEA         = 257; //  5.3.2 Capabilities-Exchange-Answer
    public static final int DWR         = 280; //  5.5.1 Device-Watchdog-Request
    public static final int DWA         = 280; //  5.5.2 Device-Watchdog-Answer
    public static final int DPR         = 282; //  5.4.1 Disconnect-Peer-Request
    public static final int DPA         = 282; //  5.4.2 Disconnect-Peer-Answer
    public static final int RAR         = 258; //  8.3.1 Re-Auth-Request
    public static final int RAA         = 258; //  8.3.2 Re-Auth-Answer
    public static final int STR         = 275; //  8.4.1 Session-Termination-Request
    public static final int STA         = 275; //  8.4.2 Session-Termination-Answer
    //  Credit-Control
    public static final int CCR         = 272; //  3.1 Credit-Control-Request
    public static final int CCA         = 272; //  3.2 Credit-Control-Answer
    //  Diameter MIP
    public static final int AMR         = 260; //  5.1 AA-Mobile-Node-Request
    public static final int AMA         = 260; //  5.2 AA-Mobile-Node-Answer
    public static final int HAR         = 262; //  5.3 Home-Agent-MIP-Request
    public static final int HAA         = 262; //  5.4 Home-Agent-MIP-Answer
    // Diameter Network Access Server Application
    public static final int AAR         = 265; //  3.1  AA-Request
    public static final int AAA         = 265; //  3.2  AA-Answer
    // Diameter EAP Application
    public static final int DER         = 268; //  3.1  Diameter-EAP-Request
    public static final int DEA         = 268; //  3.2  Diameter-EAP-Answer
    // Diameter SIP Application
    public static final int UAR         = 283; //  8.1  User-Authorization-Request
    public static final int UAA         = 283; //  8.2  User-Authorization-Answer
    public static final int SAR         = 284; //  8.3  Server-Assignment-Request
    public static final int SAA         = 284; //  8.4  Server-Assignment-Answer
    public static final int LIR         = 285; //  8.5  Location-Info-Request
    public static final int LIA         = 285; //  8.6  Location-Info-Answer
    public static final int MAR         = 286; //  8.7  Multimedia-Auth-Request
    public static final int MAA         = 286; //  8.8  Multimedia-Auth-Answer
    public static final int RTR         = 287; //  8.9  Registration-Termination-Request
    public static final int RTA         = 287; //  8.10 Registration-Termination-Answer
    public static final int PPR         = 288; //  8.11 Push-Profile-Request
    public static final int PPA         = 288; //  8.12 Push-Profile-Answer


    public static final int RESERVEDE   = 0xFFFFFE;
    public static final int RESERVEDF   = 0xFFFFFF;

    // Diameter AVP flag masks 8 bit
    public static final int AFLAG_VENDOR       = 128; // Vendor-Specific bit
    public static final int AFLAG_MANDATORY    =  64; // Mandatory
    public static final int AFLAG_PROTECTED    =  32; // P' bit indicates the need for encryption for end-to-end security

    // Diameter AVPs
    //                                                                                    +---------------------+
    //                                                                                    |    AVP Flag rules   |
    //                                                                                    |----+-----+----+-----|----+
    //                                                       AVP      Section             |    |     |SHLD| MUST|MAY |
    //                      Attribute Name                   Code     Defined  Data Type  |MUST| MAY | NOT|  NOT|Encr|
    //                      --------------------------------------------------------------|----+-----+----+-----|----|
    public static final int ACCT_INTERIM_INTERVAL           =  85; //  9.8.2   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_REALTIME_REQUIRED    = 483; //  9.8.7   Enumerated | M  |  P  |    |  V  | Y  |
    public static final int ACCT_MULTI_SESSION_ID           =  50; //  9.8.5   UTF8String | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_RECORD_NUMBER        = 485; //  9.8.3   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_RECORD_TYPE          = 480; //  9.8.1   Enumerated | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_SESSION_ID           =  44; //  9.8.4   OctetString| M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_SUB_SESSION_ID       = 287; //  9.8.6   Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int ACCT_APPLICATION_ID             = 259; //  6.9     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int AUTH_APPLICATION_ID             = 258; //  6.8     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int AUTH_REQUEST_TYPE               = 274; //  8.7     Enumerated | M  |  P  |    |  V  | N  |
    public static final int AUTHORIZATION_LIFETIME          = 291; //  8.9     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int AUTH_GRACE_PERIOD               = 276; //  8.10    Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int AUTH_SESSION_STATE              = 277; //  8.11    Enumerated | M  |  P  |    |  V  | N  |
    public static final int RE_AUTH_REQUEST_TYPE            = 285; //  8.12    Enumerated | M  |  P  |    |  V  | N  |
    public static final int CLASS                           =  25; //  8.20    OctetString| M  |  P  |    |  V  | Y  |
    public static final int DESTINATION_HOST                = 293; //  6.5     DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int DESTINATION_REALM               = 283; //  6.6     DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int DISCONNECT_CAUSE                = 273; //  5.4.3   Enumerated | M  |  P  |    |  V  | N  |
    public static final int E2E_SEQUENCE                    = 300; //  6.15    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int ERROR_MESSAGE                   = 281; //  7.3     UTF8String |    |  P  |    | V,M | N  |
    public static final int ERROR_REPORTING_HOST            = 294; //  7.4     DiamIdent  |    |  P  |    | V,M | N  |
    public static final int EVENT_TIMESTAMP                 =  55; //  8.21    Time       | M  |  P  |    |  V  | N  |
    public static final int EXPERIMENTAL_RESULT             = 297; //  7.6     Grouped    | M  |  P  |    |  V  | N  |
    public static final int EXPERIMENTAL_RESULT_CODE        = 298; //  7.7     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int FAILED_AVP                      = 279; //  7.5     Grouped    | M  |  P  |    |  V  | N  |
    public static final int FIRMWARE_REVISION               = 267; //  5.3.4   Unsigned32 |    |     |    |P,V,M| N  |
    public static final int HOST_IP_ADDRESS                 = 257; //  5.3.5   Address    | M  |  P  |    |  V  | N  |
    public static final int INBAND_SECURITY_ID              = 299; //  6.10    Unsigned32 |    |     |    |     |    |
    public static final int MULTI_ROUND_TIME_OUT            = 272; //  8.19    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ORIGIN_HOST                     = 264; //  6.3     DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int ORIGIN_REALM                    = 296; //  6.4     DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int ORIGIN_STATE_ID                 = 278; //  8.16    Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int PRODUCT_NAME                    = 269; //  5.3.7   UTF8String |    |     |    |P,V,M| N  |
    public static final int PROXY_HOST                      = 280; //  6.7.3   DiamIdent  | M  |     |    | P,V | N  |
    public static final int PROXY_INFO                      = 284; //  6.7.2   Grouped    | M  |     |    | P,V | N  |
    public static final int PROXY_STATE                     =  33; //  6.7.4   OctetString| M  |     |    | P,V | N  |
    public static final int REDIRECT_HOST                   = 292; //  6.12    DiamURI    | M  |  P  |    |  V  | N  |
    public static final int REDIRECT_HOST_USAGE             = 261; //  6.13    Enumerated | M  |  P  |    |  V  | N  |
    public static final int REDIRECT_MAX_CACHE_TIME         = 262; //  6.14    Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int RESULT_CODE                     = 268; //  7.1     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int ROUTE_RECORD                    = 282; //  6.7.1   DiamIdent  | M  |     |    | P,V | N  |
    public static final int SESSION_ID                      = 263; //  8.8     UTF8String | M  |  P  |    |  V  | Y  |
    public static final int SESSION_TIMEOUT                 =  27; //  8.13    Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int SESSION_BINDING                 = 270; //  8.17    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int SESSION_SERVER_FAILOVER         = 271; //  8.18    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int SUPPORTED_VENDOR_ID             = 265; //  5.3.6   Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int TERMINATION_CAUSE               = 295; //  8.15    Enumerated | M  |  P  |    |  V  | N  |
    public static final int USER_NAME                       =   1; //  8.14    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int VENDOR_ID                       = 266; //  5.3.3   Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int VENDOR_SPECIFIC_APPLICATION_ID  = 260; //  6.11    Grouped    | M  |  P  |    |  V  | N  |
    //  Credit-Control AVPs                                                               |    |     |    |     |    |
    public static final int CC_CORRELATION_ID               = 411; //  8.1     OctetString|    | P,M |    |  V  | Y  |
    public static final int CC_INPUT_OCTETS                 = 412; //  8.24    Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int CC_MONEY                        = 413; //  8.22    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int CC_OUTPUT_OCTETS                = 414; //  8.25    Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int CC_REQUEST_NUMBER               = 415; //  8.2     Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int CC_REQUEST_TYPE                 = 416; //  8.3     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CC_SERVICE_SPECIFIC_UNITS       = 417; //  8.26    Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int CC_SESSION_FAILOVER             = 418; //  8.4     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CC_SUB_SESSION_ID               = 419; //  8.5     Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int CC_TIME                         = 420; //  8.21    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int CC_TOTAL_OCTETS                 = 421; //  8.23    Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int CC_UNIT_TYPE                    = 454; //  8.32    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CHECK_BALANCE_RESULT            = 422; //  8.6     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int COST_INFORMATION                = 423; //  8.7     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int COST_UNIT                       = 424; //  8.12    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CREDIT_CONTROL                  = 426; //  8.13    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CREDIT_CONTROL_FAILURE_HANDLING = 427; //  8.14    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CURRENCY_CODE                   = 425; //  8.11    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int DIRECT_DEBITING_FAILURE_HANDLING= 428; //  8.15    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int EXPONENT                        = 429; //  8.9     Integer32  | M  |  P  |    |  V  | Y  |
    public static final int FINAL_UNIT_ACTION               = 449; //  8.35    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int FINAL_UNIT_INDICATION           = 430; //  8.34    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int GRANTED_SERVICE_UNIT            = 431; //  8.17    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int G_S_U_POOL_IDENTIFIER           = 453; //  8.31    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int G_S_U_POOL_REFERENCE            = 457; //  8.30    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MULTIPLE_SERVICES_CREDIT_CONTROL= 456; //  8.16    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MULTIPLE_SERVICES_INDICATOR     = 455; //  8.40    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int RATING_GROUP                    = 432; //  8.29    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int REDIRECT_ADDRESS_TYPE           = 433; //  8.38    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int REDIRECT_SERVER                 = 434; //  8.37    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int REDIRECT_SERVER_ADDRESS         = 435; //  8.39    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int REDIRECT_MODE                   = 2001; //  8.39   UTF8String | M  |  P  |    |  V  | Y  |
    public static final int REQUESTED_ACTION                = 436; //  8.41    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int REQUESTED_SERVICE_UNIT          = 437; //  8.18    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int RESTRICTION_FILTER_RULE         = 438; //  8.36    IPFiltrRule| M  |  P  |    |  V  | Y  |
    public static final int SERVICE_CONTEXT_ID              = 461; //  8.42    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int SERVICE_IDENTIFIER              = 439; //  8.28    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int SERVICE_PARAMETER_INFO          = 440; //  8.43    Grouped    |    | P,M |    |  V  | Y  |
    public static final int SERVICE_PARAMETER_TYPE          = 441; //  8.44    Unsigned32 |    | P,M |    |  V  | Y  |
    public static final int SERVICE_PARAMETER_VALUE         = 442; //  8.45    OctetString|    | P,M |    |  V  | Y  |
    public static final int SUBSCRIPTION_ID                 = 443; //  8.46    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int SUBSCRIPTION_ID_DATA            = 444; //  8.48    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int SUBSCRIPTION_ID_TYPE            = 450; //  8.47    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int TARIFF_CHANGE_USAGE             = 452; //  8.27    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int TARIFF_TIME_CHANGE              = 451; //  8.20    Time       | M  |  P  |    |  V  | Y  |
    public static final int UNIT_VALUE                      = 445; //  8.8     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int USED_SERVICE_UNIT               = 446; //  8.19    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int USER_EQUIPMENT_INFO             = 458; //  8.49    Grouped    |    | P,M |    |  V  | Y  |
    public static final int USER_EQUIPMENT_INFO_TYPE        = 459; //  8.50    Enumerated |    | P,M |    |  V  | Y  |
    public static final int USER_EQUIPMENT_INFO_VALUE       = 460; //  8.51    OctetString|    | P,M |    |  V  | Y  |
    public static final int VALUE_DIGITS                    = 447; //  8.10    Integer64  | M  |  P  |    |  V  | Y  |
    public static final int VALIDITY_TIME                   = 448; //  8.33    Unsigned32 | M  |  P  |    |  V  | Y  |
    //  Diameter MIP                                                                      |    |     |    |     |    |
    public static final int MIP_REG_REQUEST                 = 320; //  7.1     OctetString| M  |  P  |    |  V  | Y  |
    public static final int MIP_REG_REPLY                   = 321; //  7.2     OctetString| M  |  P  |    |  V  | Y  |
    public static final int MIP_MN_AAA_AUTH                 = 322; //  7.6     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_MOBILE_NODE_ADDRESS         = 333; //  7.3     Address    | M  |  P  |    |  V  | Y  |
    public static final int MIP_HOME_AGENT_ADDRESS          = 334; //  7.4     Address    | M  |  P  |    |  V  | Y  |
    public static final int MIP_CANDIDATE_HOME_AGENT_HOST   = 336; //  7.9     DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int MIP_FEATURE_VECTOR              = 337; //  7.5     Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_AUTH_INPUT_DATA_LENGTH      = 338; //  7.6.2   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_AUTHENTICATOR_LENGTH        = 339; //  7.6.3   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_AUTHENTICATOR_OFFSET        = 340; //  7.6.4   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_MN_AAA_SPI                  = 341; //  7.6.1   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_FILTER_RULE                 = 342; //  7.8     IPFiltrRule| M  |  P  |    |  V  | Y  |
    public static final int MIP_FA_CHALLENGE                = 344; //  7.7     OctetString| M  |  P  |    |  V  | Y  |
    public static final int MIP_ORIGINATING_FOREIGN_AAA     = 347; //  7.10    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_HOME_AGENT_HOST             = 348; //  7.11    DiamIdent  | M  |  P  |    |  V  | N  |
    public static final int MIP_FA_TO_HA_SPI                = 318; //  9.11    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_FA_TO_MN_SPI                = 319; //  9.10    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_HA_TO_FA_SPI                = 323; //  9.14    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int MIP_MN_TO_FA_MSA                = 325; //  9.5     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_FA_TO_MN_MSA                = 326; //  9.1     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_FA_TO_HA_MSA                = 328; //  9.2     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_HA_TO_FA_MSA                = 329; //  9.3     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_MN_TO_HA_MSA                = 331; //  9.6     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_HA_TO_MN_MSA                = 332; //  9.4     Grouped    | M  |  P  |    |  V  | Y  |
    public static final int MIP_NONCE                       = 335; //  9.12    OctetString| M  |  P  |    |  V  | Y  |
    public static final int MIP_SESSION_KEY                 = 343; //  9.7     OctetString| M  |  P  |    |  V  | Y  |
    public static final int MIP_ALGORITHM_                  = 345; //  9.8     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int MIP_REPLAY_MODE                 = 346; //  9.9     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int MIP_MSA_LIFETIME                = 367; //  9.13    Unsigned32 | M  |  P  |    |  V  | Y  |
    // Diameter Network Access Server Application                                         |    |     |    |     |    |
    public static final int NAS_PORT                        =  5 ; //   4.2    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int NAS_PORT_ID                     =  87; //   4.3    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int NAS_PORT_TYPE                   =  61; //   4.4    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CALLED_STATION_ID               =  30; //   4.5    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CALLING_STATION_ID              =  31; //   4.6    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CONNECT_INFO                    =  77; //   4.7    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int ORIGINATING_LINE_INFO           =  94; //   4.8    OctetString|    | M,P |    |  V  | Y  |
    public static final int REPLY_MESSAGE                   =  18; //   4.9    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int USER_PASSWORD                   =   2; //   5.1    OctetString| M  |  P  |    |  V  | Y  |
    public static final int PASSWORD_RETRY                  =  75; //   5.2    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int PROMPT                          =  76; //   5.3    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CHAP_AUTH                       = 402; //   5.4    Grouped    | M  |  P  |    |  V  | Y  |
    public static final int CHAP_ALGORITHM                  = 403; //   5.5    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CHAP_IDENT                      = 404; //   5.6    OctetString| M  |  P  |    |  V  | Y  |
    public static final int CHAP_RESPONSE                   = 405; //   5.7    OctetString| M  |  P  |    |  V  | Y  |
    public static final int CHAP_CHALLENGE                  =  60; //   5.8    OctetString| M  |  P  |    |  V  | Y  |
    public static final int ARAP_PASSWORD                   =  70; //   5.9    OctetString| M  |  P  |    |  V  | Y  |
    public static final int ARAP_CHALLENGE_RESPONSE         =  84; //   5.10   OctetString| M  |  P  |    |  V  | Y  |
    public static final int ARAP_SECURITY                   =  73; //   5.11   Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ARAP_SECURITY_DATA              =  74; //   5.12   OctetString| M  |  P  |    |  V  | Y  |
    public static final int SERVICE_TYPE                    =   6; //   6.1    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CALLBACK_NUMBER                 =  19; //   6.2    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CALLBACK_ID                     =  20; //   6.3    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int IDLE_TIMEOUT                    =  28; //   6.4    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int PORT_LIMIT                      =  62; //   6.5    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int NAS_FILTER_RULE                 = 400; //   6.6    IPFltrRule | M  |  P  |    |  V  | Y  |
    public static final int FILTER_ID                       =  11; //   6.7    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CONFIGURATION_TOKEN             =  78; //   6.8    OctetString| M  |     |    | P,V |    |
    public static final int QOS_FILTER_RULE                 = 407; //   6.9    QoSFltrRule|    |     |    |     |    |
    public static final int FRAMED_PROTOCOL                 =   7; //  6.10.1  Enumerated | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_ROUTING                  =  10; //  6.10.2  Enumerated | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_MTU                      =  12; //  6.10.3  Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_COMPRESSION              =  13; //  6.10.4  Enumerated | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IP_ADDRESS               =   8; //  6.11.1  OctetString| M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IP_NETMASK               =   9; //  6.11.2  OctetString| M  |  P  |    |  V  | Y  |
    public static final int FRAMED_ROUTE                    =  22; //  6.11.3  UTF8String | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_POOL                     =  88; //  6.11.4  OctetString| M  |  P  |    |  V  | Y  |
    public static final int FRAMED_INTERFACE_ID             =  96; //  6.11.5  Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IPV6_PREFIX              =  97; //  6.11.6  OctetString| M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IPV6_ROUTE               =  99; //  6.11.7  UTF8String | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IPV6_POOL                = 100; //  6.11.8  OctetString| M  |  P  |    |  V  | Y  |
    public static final int FRAMED_IPX_NETWORK              =  23; //  6.12.1  UTF8String | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_APPLETALK_LINK           =  37; //  6.13.1  Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_APPLETALK_NETWORK        =  38; //  6.13.2  Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int FRAMED_APPLETALK_ZONE           =  39; //  6.13.3  OctetString| M  |  P  |    |  V  | Y  |
    public static final int ARAP_FEATURES                   =  71; //  6.14.1  OctetString| M  |  P  |    |  V  | Y  |
    public static final int ARAP_ZONE_ACCESS                =  72; //  6.14.2  Enumerated | M  |  P  |    |  V  | Y  |
    public static final int LOGIN_IP_HOST                   =  14; //  6.15.1  OctetString| M  |  P  |    |  V  | Y  |
    public static final int LOGIN_IPV6_HOST                 =  98; //  6.15.2  OctetString| M  |  P  |    |  V  | Y  |
    public static final int LOGIN_SERVICE                   =  15; //  6.15.3  Enumerated | M  |  P  |    |  V  | Y  |
    public static final int LOGIN_TCP_PORT                  =  16; //  6.16.1  Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int LOGIN_LAT_SERVICE               =  34; //  6.17.1  OctetString| M  |  P  |    |  V  | Y  |
    public static final int LOGIN_LAT_NODE                  =  35; //  6.17.2  OctetString| M  |  P  |    |  V  | Y  |
    public static final int LOGIN_LAT_GROUP                 =  36; //  6.17.3  OctetString| M  |  P  |    |  V  | Y  |
    public static final int LOGIN_LAT_PORT                  =  63; //  6.17.4  OctetString| M  |  P  |    |  V  | Y  |
    public static final int TUNNELING                       = 401; //   7.1    Grouped    | M  |  P  |    |  V  | N  |
    public static final int TUNNEL_TYPE                     =  64; //   7.2    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_MEDIUM_TYPE              =  65; //   7.3    Enumerated | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_CLIENT_ENDPOINT          =  66; //   7.4    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_SERVER_ENDPOINT          =  67; //   7.5    UTF8String | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_PASSWORD                 =  69; //   7.6    OctetString| M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_PRIVATE_GROUP_ID         =  81; //   7.7    OctetString| M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_ASSIGNMENT_ID            =  82; //   7.8    OctetString| M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_PREFERENCE               =  83; //   7.9    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_CLIENT_AUTH_ID           =  90; //   7.10   UTF8String | M  |  P  |    |  V  | Y  |
    public static final int TUNNEL_SERVER_AUTH_ID           =  91; //   7.11   UTF8String | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_INPUT_OCTETS         = 363; //  8.1     Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_OUTPUT_OCTETS        = 364; //  8.2     Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_INPUT_PACKETS        = 365; //  8.3     Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int ACCOUNTING_OUTPUT_PACKETS       = 366; //  8.4     Unsigned64 | M  |  P  |    |  V  | Y  |
    public static final int ACCT_SESSION_TIME               =  46; //  8.5     Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ACCT_AUTHENTIC                  =  45; //  8.6     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int ACOUNTING_AUTH_METHOD           = 406; //  8.7     Enumerated | M  |  P  |    |  V  | Y  |
    public static final int ACCT_DELAY_TIME                 =  41; //  8.8     Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ACCT_LINK_COUNT                 =  51; //  8.9     Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int ACCT_TUNNEL_CONNECTION          =  68; //  8.10    OctetString| M  |  P  |    |  V  | Y  |
    public static final int ACCT_TUNNEL_PACKETS_LOST        =  86; //  8.11    Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int NAS_IDENTIFIER                  =  32; //  9.3.1   UTF8String | M  |  P  |    |  V  | Y  |
    public static final int NAS_IP_ADDRESS                  =   4; //  9.3.2   OctetString| M  |  P  |    |  V  | Y  |
    public static final int NAS_IPV6_ADDRESS                =  95; //  9.3.3   OctetString| M  |  P  |    |  V  | Y  |
    public static final int STATE                           =  24; //  9.3.4   OctetString| M  |  P  |    |  V  | Y  |
    public static final int ORIGIN_AAA_PROTOCOL             = 408; //  9.3.6   Enumerated | M  |  P  |    |  V  | Y  |
    // Diameter SIP Application                                                           |    |     |    |     |    |
    public static final int SIP_ACCOUNTING_INFORMATION      = 368; //  9.1     Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_ACCOUNTING_SERVER_URI       = 369; //  9.1.1   DiamURI    | M  |  P  |    |  V  | N  |
    public static final int SIP_CREDIT_CONTROL_SERVER_URI   = 370; //  9.1.2   DiamURI    | M  |  P  |    |  V  | N  |
    public static final int SIP_SERVER_URI                  = 371; //  9.2     UTF8String | M  |  P  |    |  V  | N  |
    public static final int SIP_SERVER_CAPABILITIES         = 372; //  9.3     Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_MANDATORY_CAPABILITY        = 373; //  9.3.1   Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int SIP_OPTIONAL_CAPABILITY         = 374; //  9.3.2   Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int SIP_SERVER_ASSIGNMENT_TYPE      = 375; //  9.4     Enumerated | M  |  P  |    |  V  | N  |
    public static final int SIP_AUTH_DATA_ITEM              = 376; //  9.5     Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_AUTHENTICATION_SCHEME       = 377; //  9.5.1   Enumerated | M  |  P  |    |  V  | N  |
    public static final int SIP_ITEM_NUMBER                 = 378; //  9.5.2   Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int SIP_AUTHENTICATE                = 379; //  9.5.3   Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_AUTHORIZATION               = 380; //  9.5.4   Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_AUTHENTICATION_INFO         = 381; //  9.5.5   Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_NUMBER_AUTH_ITEMS           = 382; //  9.6     Unsigned32 | M  |  P  |    |  V  | N  |
    public static final int SIP_DEREGISTRATION_REASON       = 383; //  9.7     Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_REASON_CODE                 = 384; //  9.7.1   Enumerated | M  |  P  |    |  V  | N  |
    public static final int SIP_REASON_INFO                 = 385; //  9.7.2   UTF8String | M  |  P  |    |  V  | N  |
    public static final int SIP_VISITED_NETWORK_ID          = 386; //  9.9     UTF8String | M  |  P  |    |  V  | N  |
    public static final int SIP_USER_AUTHORIZATION_TYPE     = 387; //  9.10    Enumerated | M  |  P  |    |  V  | N  |
    public static final int SIP_SUPPORTED_USER_DATA_TYPE    = 388; //  9.11    UTF8String | M  |  P  |    |  V  | N  |
    public static final int SIP_USER_DATA                   = 389; //  9.12    Grouped    | M  |  P  |    |  V  | N  |
    public static final int SIP_USER_DATA_TYPE              = 390; //  9.12.1  UTF8String | M  |  P  |    |  V  | N  |
    public static final int SIP_USER_DATA_CONTENTS          = 391; //  9.12.2  OctetString| M  |  P  |    |  V  | N  |
    public static final int SIP_USER_DATA_ALREADY_AVAILABLE = 392; //  9.13    Enumerated | M  |  P  |    |  V  | N  |
    public static final int SIP_METHOD                      = 393; //  9.14    UTF8String | M  |  P  |    |  V  | N  |
    // Swisscom
    public static final int SERVICE_INFORMATION             = 873; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int SMS_INFORMATION                 = 600; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int MMS_INFORMATION                 = 877; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int ORIGINATOR_ADDRESS              = 886; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int ADDRESS_TYPE                    = 899; //          Enumerated | ?  |  ?  |    |  ?  | ?  |
    public static final int ADDRESS_DATA                    = 897; //          UTF8String | ?  |  ?  |    |  ?  | ?  |
    public static final int ADDRESS_DOMAIN                  = 898; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int DOMAIN_NAME                     =1200; //          UTF8String | ?  |  ?  |    |  ?  | ?  |
  //public static final int 3GPP_IMSI_MCC_MNC               =   8; //          UTF8String | ?  |  ?  |    |  ?  | ?  | // As you can clearly see this thing is flawed
    public static final int MESSAGE_ID                      =1210; //          UTF8String | ?  |  ?  |    |  ?  | ?  |
    public static final int MESSAGE_CLASS                   =1213; //          Grouped    | ?  |  ?  |    |  ?  | ?  |
    public static final int CLASS_IDENTIFIER                =1214; //          Enumerated | ?  |  ?  |    |  ?  | ?  |
    public static final int TOKEN_TEXT                      =1215; //          UTF8String | ?  |  ?  |    |  ?  | ?  |
    // XL -specific AVPs
    public static final int REMAINING_SERVICE_MONEY         =874; //           Grouped    | M  |  P  |    |  ?  | ?  |
    public static final int UMB_CHARGING_GROUP              =20300; //         Grouped    | M  |  P  |    |  ?  | ?  |
    public static final int VAS_TYPE                        =20303; //         UTF8String | M  |  P  |    |  ?  | ?  |
    public static final int MESSAGE_TYPE                    =20302; //         UTF8String | M  |  P  |    |  ?  | ?  |
    public static final int SDC                             =20301; //         UTF8String | M  |  P  |    |  ?  | ?  |
    // Gx specific AVPs
    public static final int BEARER_USAGE			        =1000; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int CHARGING_RULE_INSTALL		    =1001; //          Grouped    | M  |  P  |    |  V  | Y  |
    public static final int CHARGING_RULE_REMOVE		    =1002; //          Grouped    | M  |  P  |    |  V  | Y  |
    public static final int CHARGING_RULE_DEFINITION	    =1003; //          Grouped    | M  |  P  |    |  V  | Y  |
    public static final int CHARGING_RULE_BASE_NAME		    =1004; //          UTF8String | M  |  P  |    |  V  | Y  |
    public static final int CHARGING_RULE_NAME		        =1005; //          OctetString| M  |  P  |    |  V  | Y  |
    public static final int EVENT_TRIGGER			        =1006; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int METERING_METHOD			        =1007; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int OFFLINE				            =1008; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int ONLINE				            =1009; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int PRECEDENCE			            =1010; //          Unsigned32 | M  |  P  |    |  V  | Y  |
    public static final int REPORTING_LEVEL			        =1011; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int PDP_SESSION_OPERATION		    =1015; //          Enumerated | M  |  P  |    |  V  | Y  |
    public static final int TFT_FILTER			            =1012; //          IPFiltrRule| M  |  P  |    |  V  | Y  |
    public static final int TFT_PACKET_FILTER_INFORMATION	=1013; //          Grouped    | M  |  P  |    |  V  | Y  |
    public static final int TOS_TRAFFIC_CLASS		        =1014; //          OctetString| M  |  P  |    |  V  | Y  |
    public static final int SESSION_RELEASE_CAUSE	        =1045; //          Enumerated | M  |  P  |    |  V  | Y  |



    // 7.1.1.  Informational
    public static final int DIAMETER_MULTI_ROUND_AUTH          =1001;
    //7.1.2.  Success
    public static final int DIAMETER_SUCCESS                   =2001;
    public static final int DIAMETER_LIMITED_SUCCESS           =2002;
    //7.1.3.  Protocol Errors
    public static final int DIAMETER_COMMAND_UNSUPPORTED       =3001;
    public static final int DIAMETER_UNABLE_TO_DELIVER         =3002;
    public static final int DIAMETER_REALM_NOT_SERVED          =3003;
    public static final int DIAMETER_TOO_BUSY                  =3004;
    public static final int DIAMETER_LOOP_DETECTED             =3005;
    public static final int DIAMETER_REDIRECT_INDICATION       =3006;
    public static final int DIAMETER_APPLICATION_UNSUPPORTED   =3007;
    public static final int DIAMETER_INVALID_HDR_BITS          =3008;
    public static final int DIAMETER_INVALID_AVP_BITS          =3009;
    public static final int DIAMETER_UNKNOWN_PEER              =3010;
    //7.1.4.  Transient Failures
    public static final int DIAMETER_AUTHENTICATION_REJECTED   =4001;
    public static final int DIAMETER_OUT_OF_SPACE              =4002;
    public static final int ELECTION_LOST                      =4003;
    // Credit-Control 9.1.  Transient Failures
    public static final int DIAMETER_END_USER_SERVICE_DENIED      =4010;
    public static final int DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE=4011;
    public static final int DIAMETER_CREDIT_LIMIT_REACHED         =4012;
    //7.1.5.  Permanent Failures
    public static final int DIAMETER_AVP_UNSUPPORTED           =5001;
    public static final int DIAMETER_UNKNOWN_SESSION_ID        =5002;
    public static final int DIAMETER_AUTHORIZATION_REJECTED    =5003;
    public static final int DIAMETER_INVALID_AVP_VALUE         =5004;
    public static final int DIAMETER_MISSING_AVP               =5005;
    public static final int DIAMETER_RESOURCES_EXCEEDED        =5006;
    public static final int DIAMETER_CONTRADICTING_AVPS        =5007;
    public static final int DIAMETER_AVP_NOT_ALLOWED           =5008;
    public static final int DIAMETER_AVP_OCCURS_TOO_MANY_TIMES =5009;
    public static final int DIAMETER_NO_COMMON_APPLICATION     =5010;
    public static final int DIAMETER_UNSUPPORTED_VERSION       =5011;
    public static final int DIAMETER_UNABLE_TO_COMPLY          =5012;
    public static final int DIAMETER_INVALID_BIT_IN_HEADER     =5013;
    public static final int DIAMETER_INVALID_AVP_LENGTH        =5014;
    public static final int DIAMETER_INVALID_MESSAGE_LENGTH    =5015;
    public static final int DIAMETER_INVALID_AVP_BIT_COMBO     =5016;
    public static final int DIAMETER_NO_COMMON_SECURITY        =5017;
    // Credit-Control 9.2.  Permanent Failures
    public static final int DIAMETER_USER_UNKNOWN              =5030;
    public static final int DIAMETER_RATING_FAILED             =5031;
    
}