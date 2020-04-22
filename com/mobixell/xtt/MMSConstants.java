/*
 * $RCSfile: MMSConstants.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/11/09 14:14:49 $
 * 724 Solutions Inc. Copyright (c) 2005
 */

package com.mobixell.xtt;

/**
 * MMSConstants
 * <p>
 * Holds values used by the MMSDecoder class
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface MMSConstants
{
    // MMS Headers
    public static final int MMS_BCC                         = 0x01;
    public static final int MMS_CC                          = 0x02;
    public static final int MMS_X_MMS_CONTENT_LOCATION      = 0x03;
    public static final int MMS_CONTENT_TYPE                = 0x04;
    public static final int MMS_DATE                        = 0x05;
    public static final int MMS_X_MMS_DELIVERY_REPORT       = 0x06;
    public static final int MMS_X_MMS_DELIVERY_TIME         = 0x07;
    public static final int MMS_X_MMS_EXPIRY                = 0x08;
    public static final int MMS_FROM                        = 0x09;
    public static final int MMS_X_MMS_MESSAGE_CLASS         = 0x0A;
    public static final int MMS_MESSAGE_ID                  = 0x0B;
    public static final int MMS_X_MMS_MESSAGE_TYPE          = 0x0C;
    public static final int MMS_X_MMS_MMS_VERSION           = 0x0D;
    public static final int MMS_X_MMS_MESSAGE_SIZE          = 0x0E;
    public static final int MMS_X_MMS_PRIORITY              = 0x0F;
    public static final int MMS_X_MMS_READ_REPORT           = 0x10;
    public static final int MMS_X_MMS_REPORT_ALLOWED        = 0x11;
    public static final int MMS_X_MMS_RESPONSE_STATUS       = 0x12;
    public static final int MMS_X_MMS_RESPONSE_TEXT         = 0x13;
    public static final int MMS_X_MMS_SENDER_VISIBILITY     = 0x14;
    public static final int MMS_X_MMS_STATUS                = 0x15;
    public static final int MMS_SUBJECT                     = 0x16;
    public static final int MMS_TO                          = 0x17;
    public static final int MMS_X_MMS_TRANSACTION_ID        = 0x18;
    public static final int MMS_X_MMS_RETRIEVE_STATUS       = 0x19;
    public static final int MMS_X_MMS_RETRIEVE_TEXT         = 0x1A;
    public static final int MMS_X_MMS_READ_STATUS           = 0x1B;
    public static final int MMS_X_MMS_REPLY_CHARGING        = 0x1C;
    public static final int MMS_X_MMS_REPLY_CHARGING_DEADLINE=0x1D;
    public static final int MMS_X_MMS_REPLY_CHARGING_ID     = 0x1E;
    public static final int MMS_X_MMS_REPLY_CHARGING_SIZE   = 0x1F;
    public static final int MMS_X_MMS_PREVIOUSLY_SENT_BY    = 0x20;
    public static final int MMS_X_MMS_PREVIOUSLY_SENT_DATE  = 0x21;
    // New 1.3
    public static final int MMS_X_MMS_STORE                 = 0x22;
    public static final int MMS_X_MMS_MM_STATE              = 0x23;
    public static final int MMS_X_MMS_MM_FLAGS              = 0x24;
    public static final int MMS_X_MMS_STORE_STATUS          = 0x25;
    public static final int MMS_X_MMS_STORE_STATUS_TEXT     = 0x26;
    public static final int MMS_X_MMS_STORED                = 0x27;
    public static final int MMS_X_MMS_ATTRIBUTES            = 0x28;
    public static final int MMS_X_MMS_TOTALS                = 0x29;
    public static final int MMS_X_MMS_MBOX_TOTALS           = 0x2A;
    public static final int MMS_X_MMS_QUOTAS                = 0x2B;
    public static final int MMS_X_MMS_MBOX_QUOTAS           = 0x2C;
    public static final int MMS_X_MMS_MESSAGE_COUNT         = 0x2D;
    public static final int MMS_CONTENT                     = 0x2E;
    public static final int MMS_X_MMS_START                 = 0x2F;
    public static final int MMS_ADDITIONAL_HEADERS          = 0x30;
    public static final int MMS_X_MMS_DISTRIBUTION_INDICATOR= 0x31;
    public static final int MMS_X_MMS_ELEMENT_DESCRIPTOR    = 0x32;
    public static final int MMS_X_MMS_LIMIT                 = 0x33;
    public static final int MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE     =  0x34;
    public static final int MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT=  0x35;
    public static final int MMS_X_MMS_STATUS_TEXT           = 0x36;
    public static final int MMS_X_MMS_APPLIC_ID             = 0x37;
    public static final int MMS_X_MMS_REPLY_APPLIC_ID       = 0x38;
    public static final int MMS_X_MMS_AUX_APPLIC_INFO       = 0x39;
    public static final int MMS_X_MMS_CONTENT_CLASS         = 0x3A;
    public static final int MMS_X_MMS_DRM_CONTENT           = 0x3B;
    public static final int MMS_X_MMS_ADAPTATION_ALLOWED    = 0x3C;
    public static final int MMS_X_MMS_REPLACE_ID            = 0x3D;
    public static final int MMS_X_MMS_CANCEL_ID             = 0x3E;
    public static final int MMS_X_MMS_CANCEL_STATUS         = 0x3F;


    // MMS Types
    public static final int M_SEND_REQ                      = 128;
    public static final int M_SEND_CONF                     = 129;
    public static final int M_NOTIFICATION_IND              = 130;
    public static final int M_NOTIFYRESP_IND                = 131;
    public static final int M_RETRIEVE_CONF                 = 132;
    public static final int M_ACKNOWLEDGE_IND               = 133;
    public static final int M_DELIVERY_IND                  = 134;
    public static final int M_READ_REC_IND                  = 135;
    public static final int M_READ_ORIG_IND                 = 136;
    public static final int M_FORWARD_REQ                   = 137;
    public static final int M_FORWARD_CONF                  = 138;
    // MMS 1.3
    public static final int M_MBOX_STORE_REQ                = 139;
    public static final int M_MBOX_STORE_CONF               = 140;
    public static final int M_MBOX_VIEW_REQ                 = 141;
    public static final int M_MBOX_VIEW_CONF                = 142;
    public static final int M_MBOX_UPLOAD_REQ               = 143;
    public static final int M_MBOX_UPLOAD_CONF              = 144;
    public static final int M_MBOX_DELETE_REQ               = 145;
    public static final int M_MBOX_DELETE_CONF              = 146;
    public static final int M_MBOX_DESCR                    = 147;
    public static final int M_DELETE_REQ                    = 148;
    public static final int M_DELETE_CONF                   = 149;
    public static final int M_CANCEL_REQ                    = 150;
    public static final int M_CANCEL_CONF                   = 151;
}