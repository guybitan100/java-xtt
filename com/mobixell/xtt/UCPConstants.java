/*
 * $RCSfile: UCPConstants.java,v $
 * $Revision: 1.2 $
 * $Date: 2006/07/21 17:04:29 $
 * 724 Solutions Inc. Copyright (c) 2005
 */

package com.mobixell.xtt;

/**
 * UCPConstants
 * <p>
 * Holds values used by the UCPWorker class
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface UCPConstants
{
    public static final byte STX         = 0x02;
    public static final byte ETX         = 0x03;
    public static final int  HEADLENGTH  = 13;


    public static final int   CALL_INPUT_OPERATION    = 01; //Call input operation
    public static final int   MULTIADDRESS_CALL_INPUT = 02; //Multiple address call input operation
    public static final int   SUPPSERVICE_CALL_INPUT  = 03; //Call input with supplementary services operation
    public static final int   SMS_TRANSFER_OPERATION  = 30; //SMS message transfer operation
    public static final int   SMT_ALERT_OPERATION     = 31; // Alert the SMSC to start delivering buffered messages
    //public static final int                         = 32; //(reserved)
    //public static final int                         = 33; //(reserved)
    //public static final int                         = 34; //(reserved)
    //public static final int                         = 36; //(reserved)
    //public static final int                         = 38; //(reserved)
    //public static final int                         = 40; //(reserved)
    //public static final int                         = 41; //(reserved)
    //public static final int                         = 42; //(reserved)
    //public static final int                         = 43; //(reserved)
    public static final int   SUBMIT_SHORT_MESSAGE    = 51; //Submit a new message to someone.
    public static final int   DELIVER_SHORT_MESSAGE   = 52; //The SMSC delivers a message that someone hassent
    public static final int   DELIVER_NOTIFICATION    = 53; //Informs you of the delivery status of a message
    public static final int   MODIFY_MESSAGE          = 54; //Modify the message parameters of a buffered message.
    public static final int   INQUIRY_MESSAGE         = 55; //Verify if a message is still in the SMSC.
    public static final int   DELETE_MESSAGE          = 56; //Delete a buffered message if it is still in the SMSC.
    public static final int   RESPONSE_INQUIRY_MESSAGE= 57; //Informs you of the outcome of the inquiry to a buffered
    public static final int   RESPONSE_DELETE_MESSAGE = 58; //Informs you of the outcome of the delete request for a
    public static final int   SESSION_MANAGEMENT      = 60; //Session management Authenticate yourself after making the connection to the
    public static final int   PROVISIONING_ACTIONS    = 61; //List management Manage your own mobile originated and mobile terminated


    public static final int   CHECKSUM_ERROR                                 = 1;
    public static final int   SYNTAX_ERROR                                   = 2;
    public static final int   OPERATION_NOT_SUPPORTED_BY_SYSTEM              = 3;
    public static final int   OPERATION_NOT_ALLOWED                          = 4;
    public static final int   CALL_BARRING_ACTIVE                            = 5;
    public static final int   ADC_INVALID                                    = 6;
    public static final int   AUTHENTICATION_FAILURE                         = 7;
    public static final int   LEGITIMISATION_CODE_FOR_ALL_CALLS_FAILURE      = 8;
    public static final int   GA_NOT_VALID                                   = 9;
    public static final int   REPETITION_NOT_ALLOWED                         =10;
    public static final int   LEGITIMISATION_CODE_FOR_REPETITION_FAILURE     =11;
    public static final int   PRIORITY_CALL_NOT_ALLOWED                      =12;
    public static final int   LEGITIMISATION_CODE_FOR_PRIORITY_CALL_FAILURE  =13;
    public static final int   URGENT_MESSAGE_NOT_ALLOWED                     =14;
    public static final int   LEGITIMISATION_CODE_FOR_URGENT_MESSAGE_FAILURE =15;
    public static final int   REVERSE_CHARGING_NOT_ALLOWED                   =16;
    public static final int   LEGITIMISATION_CODE_FOR_REV_CHARGING_FAILURE   =17;
    public static final int   DEFERRED_DELIVERY_NOT_ALLOWED                  =18;
    public static final int   NEW_AC_NOT_VALID                               =19;
    public static final int   NEW_LEGITIMISATION_CODE_NOT_VALID              =20;
    public static final int   STANDARD_TEXT_NOT_VALID                        =21;
    public static final int   TIME_PERIOD_NOT_VALID                          =22;
    public static final int   MESSAGE_TYPE_NOT_SUPPORTED_BY_SYSTEM           =23;
    public static final int   MESSAGE_TOO_LONG                               =24;
    public static final int   REQUESTED_STANDARD_TEXT_NOT_VALID              =25;
    public static final int   MESSAGE_TYPE_NOT_VALID_FOR_THE_PAGER_TYPE      =26;
    public static final int   MESSAGE_NOT_FOUND_IN_SMSC                      =27;
    public static final int   SUBSCRIBER_HANGUP                              =30;
    public static final int   FAX_GROUP_NOT_SUPPORTED                        =31;
    public static final int   FAX_MESSAGE_TYPE_NOT_SUPPORTED                 =32;
    public static final int   ADDRESS_ALREADY_IN_LIST                        =33;
    public static final int   ADDRESS_NOT_IN_LIST                            =34;
    public static final int   LIST_FULL_CANNOT_ADD_ADDRESS_TO_LIST           =35;
    public static final int   RPID_ALREADY_IN_USE                            =36;
    public static final int   DELIVERY_IN_PROGRESS                           =37;
    public static final int   MESSAGE_FORWARDED                              =38;




}




















