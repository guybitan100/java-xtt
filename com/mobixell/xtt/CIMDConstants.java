/*
 * $RCSfile: CIMDConstants.java,v $
 * $Revision: 1.1 $
 * $Date: 2008/01/31 08:57:17 $
 * Mobixell Inc. Copyright = c; 2005
 */

package com.mobixell.xtt;

/**
 * CIMDConstants
 * <p>
 * Holds values used by the CIMDWorker class
 *
 * @author Roger Soder
 * @version 1.0
 */
public class CIMDConstants extends Thread
{
    public static final byte STX         = 0x02;
    public static final byte ETX         = 0x03;
    public static final byte COLON       = 0x3A; // 58=:
    public static final byte TAB         = 0x09; // 58=:
    public static final int  HEADLENGTH  = 7;

    // Message types
    public static final int LOGIN_REQUEST                   =  1;
    public static final int LOGIN_RESPONSE                  = 51;
    public static final int LOGOUT_REQUEST                  =  2;
    public static final int LOGOUT_RESPONSE                 = 52;
    public static final int SUBMIT_MESSAGE_REQUEST          =  3;
    public static final int SUBMIT_MESSAGE_RESPONSE         = 53;
    public static final int ENQUIRE_MESSAGE_STATUS_REQUEST  =  4;
    public static final int ENQUIRE_MESSAGE_STATUS_RESPONSE = 54;
    public static final int DELIVERY_REQUEST_REQUEST        =  5;
    public static final int DELIVERY_REQUEST_RESPONSE       = 55;
    public static final int CANCEL_MESSAGE_REQUEST          =  6;
    public static final int CANCEL_MESSAGE_RESPONSE         = 56;
    public static final int SET_MESSAGE_REQUEST             =  8;
    public static final int SET_MESSAGE_RESPONSE            = 58;
    public static final int GET_MESSAGE_REQUEST             =  9;
    public static final int GET_MESSAGE_RESPONSE            = 59;
    public static final int DELIVER_MESSAGE_REQUEST         = 20;
    public static final int DELIVER_MESSAGE_RESPONSE        = 70;
    public static final int DELIVER_STATUS_REPORT           = 23;
    public static final int DELIVER_STATUS_RESPONSE         = 73;
    public static final int ALIVE_REQUEST                   = 40;
    public static final int ALIVE_RESPONSE                  = 90;
    public static final int GENERAL_ERROR_RESPONSE          = 98;
    public static final int NACK                            = 99;


    // Parameters
    public static final int PARAM_USER_IDENTITY                   =  10;
    public static final int PARAM_PASSWORD                        =  11;
    public static final int PARAM_SUBADDR                         =  12;
    public static final int PARAM_WINDOW_SIZE                     =  19;
    public static final int PARAM_DESTINATION_ADDRESS             =  21;
    public static final int PARAM_ORIGINATING_ADDRESS             =  23;
    public static final int PARAM_ORIGINATING_IMSI                =  26;
    public static final int PARAM_ALPHANUMERIC_ORIGINATING_ADDRESS=  27;
    public static final int PARAM_ORIGINATED_VISITED_MSC_ADDRESS  =  28;
    public static final int PARAM_DATA_CODING_SCHEME              =  30;
    public static final int PARAM_USER_DATA_HEADER                =  32;
    public static final int PARAM_USER_DATA                       =  33;
    public static final int PARAM_USER_DATA_BINARY                =  34;
    public static final int PARAM_TRANSPORTTYPE                   =  41;
    public static final int PARAM_MESSAGE_TYPE                    =  42;
    public static final int PARAM_MORE_MESSAGES_TO_SEND           =  44;
    public static final int PARAM_OPERATION_TIMER                 =  45;
    public static final int PARAM_DIALOGUE_ID                     =  46;
    public static final int PARAM_USSD_PHASE                      =  47;
    public static final int PARAM_SERVICE_CODE                    =  48;
    public static final int PARAM_VALIDITY_PERIOD_RELATIVE        =  50;
    public static final int PARAM_VALIDITY_PERIOD_ABSOLUTE        =  51;
    public static final int PARAM_PROTOCOL_IDENTIFIER             =  52;
    public static final int PARAM_FIRST_DELIVERY_TIME_RELATIVE    =  53;
    public static final int PARAM_FIRST_DELIVERY_TIME_ABSOLUTE    =  54;
    public static final int PARAM_REPLY_PATH                      =  55;
    public static final int PARAM_STATUS_REPORT_REQUEST           =  56;
    public static final int PARAM_CANCEL_ENABLED                  =  58;
    public static final int PARAM_CANCEL_MODE                     =  59;
    public static final int PARAM_SERVICE_CENTRE_TIME_STAMP       =  60;
    public static final int PARAM_STATUS_CODE                     =  61;
    public static final int PARAM_STATUS_ERROR_CODE               =  62;
    public static final int PARAM_DISCHARGE_TIME                  =  63;
    public static final int PARAM_TARIFF_CLASS                    =  64;
    public static final int PARAM_SERVICE_DESCRIPTION             =  65;
    public static final int PARAM_MESSAGE_COUNT                   =  66;
    public static final int PARAM_PRIORITY                        =  67;
    public static final int PARAM_DELIVERY_REQUEST_MODE           =  68;
    public static final int PARAM_SERVICE_CENTER_ADDRESS          =  69;
    public static final int PARAM_GET_PARAMETER                   = 500;
    public static final int PARAM_MC_TIME                         = 501;
    public static final int PARAM_ERROR_CODE                      = 900;
    public static final int PARAM_ERROR_TEXT                      = 901;

    public String getErrorMessage(int code)
    {
        switch(code)
        {
            case   0: return "No error";
            case   1: return "Unexpected operation";
            case   2: return "Syntax error";
            case   3: return "Unsupported parameter error";
            case   4: return "Connection to MC lost";
            case   5: return "No response from MC";
            case   6: return "General system error";
            case   7: return "Cannot find information";
            case   8: return "Parameter formatting error";
            case   9: return "Requested operation failed";
                 // LOGIN error codes:
            case 100: return "Invalid login";
            case 101: return "Incorrect access type";
            case 102: return "Too many users with this login ID";
            case 103: return "Login refused by MC";
            case 104: return "Invalid window size";
            case 105: return "Windowing disabled";
            case 106: return "Virtual SMS Center-based barring";
            case 107: return "Invalid subaddr";
            case 108: return "Alias account, login refused";
                 //SUBMIT MESSAGE error codes:
            case 300: return "Incorrect destination address";
            case 301: return "Incorrect number of destination addresses";
            case 302: return "Syntax error in user data parameter";
            case 303: return "Incorrect bin/head/normal user data parameter combination";
            case 304: return "Incorrect dcs parameter usage";
            case 305: return "Incorrect validity period parameters usage";
            case 306: return "Incorrect originator address usage";
            case 307: return "Incorrect PID parameter usage";
            case 308: return "Incorrect first delivery parameter usage";
            case 309: return "Incorrect reply path usage";
            case 310: return "Incorrect status report request parameter usage";
            case 311: return "Incorrect cancel enabled parameter usage";
            case 312: return "Incorrect priority parameter usage";
            case 313: return "Incorrect tariff class parameter usage";
            case 314: return "Incorrect service description parameter usage";
            case 315: return "Incorrect transport type parameter usage";
            case 316: return "Incorrect message type parameter usage";
            case 318: return "Incorrect MMs parameter usage";
            case 319: return "Incorrect operation timer parameter usage";
            case 320: return "Incorrect dialogue ID parameter usage";
            case 321: return "Incorrect alpha originator address usage";
            case 322: return "Invalid data for alpha numeric originator";
                 //ENQUIRE MESSAGE STATUS error codes:
            case 400: return "Incorrect address parameter usage";
            case 401: return "Incorrect scts parameter usage";
                 //DELIVERY REQUEST error codes:
            case 500: return "Incorrect scts parameter usage";
            case 501: return "Incorrect mode parameter usage";
            case 502: return "Incorrect parameter combination";
                 //CANCEL MESSAGE error codes:
            case 600: return "Incorrect scts parameter usage";
            case 601: return "Incorrect address parameter usage";
            case 602: return "Incorrect mode parameter usage";
            case 603: return "Incorrect parameter combination";
                 //SET error codes:
            case 800: return "Changing password failed";
            case 801: return "Changing password not allowed";
                 //GET error codes:
            case 900: return "Unsupported item requested";
            default: return "unknown";
        }
    }

    public String getParameterName(int code)
    {
        switch(code)
        {
            case PARAM_USER_IDENTITY                   :return "user_identity";
            case PARAM_PASSWORD                        :return "password";
            case PARAM_SUBADDR                         :return "subaddr";
            case PARAM_WINDOW_SIZE                     :return "window_size";
            case PARAM_DESTINATION_ADDRESS             :return "destination_address";
            case PARAM_ORIGINATING_ADDRESS             :return "originating_address";
            case PARAM_ORIGINATING_IMSI                :return "originating_imsi";
            case PARAM_ALPHANUMERIC_ORIGINATING_ADDRESS:return "alphanumeric_originating_address";
            case PARAM_ORIGINATED_VISITED_MSC_ADDRESS  :return "originated_visited_msc_address";
            case PARAM_DATA_CODING_SCHEME              :return "data_coding_scheme";
            case PARAM_USER_DATA_HEADER                :return "user_data_header";
            case PARAM_USER_DATA                       :return "user_data";
            case PARAM_USER_DATA_BINARY                :return "user_data_binary";
            case PARAM_TRANSPORTTYPE                   :return "transporttype";
            case PARAM_MESSAGE_TYPE                    :return "message_type";
            case PARAM_MORE_MESSAGES_TO_SEND           :return "more_messages_to_send";
            case PARAM_OPERATION_TIMER                 :return "operation_timer";
            case PARAM_DIALOGUE_ID                     :return "dialogue_id";
            case PARAM_USSD_PHASE                      :return "ussd_phase";
            case PARAM_SERVICE_CODE                    :return "service_code";
            case PARAM_VALIDITY_PERIOD_RELATIVE        :return "validity_period_relative";
            case PARAM_VALIDITY_PERIOD_ABSOLUTE        :return "validity_period_absolute";
            case PARAM_PROTOCOL_IDENTIFIER             :return "protocol_identifier";
            case PARAM_FIRST_DELIVERY_TIME_RELATIVE    :return "first_delivery_time_relative";
            case PARAM_FIRST_DELIVERY_TIME_ABSOLUTE    :return "first_delivery_time_absolute";
            case PARAM_REPLY_PATH                      :return "reply_path";
            case PARAM_STATUS_REPORT_REQUEST           :return "status_report_request";
            case PARAM_CANCEL_ENABLED                  :return "cancel_enabled";
            case PARAM_CANCEL_MODE                     :return "cancel_mode";
            case PARAM_SERVICE_CENTRE_TIME_STAMP       :return "service_centre_time_stamp";
            case PARAM_STATUS_CODE                     :return "status_code";
            case PARAM_STATUS_ERROR_CODE               :return "status_error_code";
            case PARAM_DISCHARGE_TIME                  :return "discharge_time";
            case PARAM_TARIFF_CLASS                    :return "tariff_class";
            case PARAM_SERVICE_DESCRIPTION             :return "service_description";
            case PARAM_MESSAGE_COUNT                   :return "message_count";
            case PARAM_PRIORITY                        :return "priority";
            case PARAM_DELIVERY_REQUEST_MODE           :return "delivery_request_mode";
            case PARAM_SERVICE_CENTER_ADDRESS          :return "service_center_address";
            case PARAM_GET_PARAMETER                   :return "get_parameter";
            case PARAM_MC_TIME                         :return "mc_time";
            case PARAM_ERROR_CODE                      :return "error_code";
            case PARAM_ERROR_TEXT                      :return "error_text";
            default: return "unknown";
       }
    }
}