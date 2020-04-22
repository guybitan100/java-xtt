package com.mobixell.xtt;

/**
 * WSPConstants
 * <p>
 * Holds values used by the WSPDecoder class
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface WSPConstants
{

    // WSP Commands
    //public static final int WSP_Reserved                    = 0x00;
    public static final int WSP_CONNECT                     = 0x01;
    public static final int WSP_CONNECTREPLY                = 0x02;
    public static final int WSP_REDIRECT                    = 0x03;
    public static final int WSP_REPLY                       = 0x04;
    public static final int WSP_DISCONNECT                  = 0x05;
    public static final int WSP_PUSH                        = 0x06;
    public static final int WSP_CONFIRMEDPUSH               = 0x07;
    public static final int WSP_SUSPEND                     = 0x08;
    public static final int WSP_RESUME                      = 0x09;
    //public static final int WSP_UNASSIGNED                  = 0x10 - 0x3F
    public static final int WSP_GET                         = 0x40;
    public static final int WSP_OPTIONS                     = 0x41;
    public static final int WSP_HEAD                        = 0x42;
    public static final int WSP_DELETE                      = 0x43;
    public static final int WSP_TRACE                       = 0x44;
    //public static final int WSP_UNASSIGNED                  = 0x45 - 0x4F
    //public static final int WSP_EXTENDED METHOD             = 0x50 - 0x5F
    public static final int WSP_POST                        = 0x60;
    public static final int WSP_PUT                         = 0x61;
    //public static final int WSP_UNASSIGNED                  = 0x62 - 0x6F
    //public static final int WSP_EXTENDED                    = 0x70 - 0x7F
    public static final int WSP_DATA_FRAGMENT_PDU           = 0x80;
    //public static final int WSP_RESERVED                    = 0x81 - 0xFF


    // WAP Headers
    public static final int WAP_ACCEPT                      = 0x00;
    public static final int WAP_ACCEPT_CHARSET1             = 0x01;
    public static final int WAP_ACCEPT_ENCODING1            = 0x02;
    public static final int WAP_ACCEPT_LANGUAGE             = 0x03;
    public static final int WAP_ACCEPT_RANGES               = 0x04;
    public static final int WAP_AGE                         = 0x05;
    public static final int WAP_ALLOW                       = 0x06;
    public static final int WAP_AUTHORIZATION               = 0x07;
    public static final int WAP_CACHE_CONTROL1              = 0x08;
    public static final int WAP_CONNECTION                  = 0x09;
    public static final int WAP_CONTENT_BASE1               = 0x0A;
    public static final int WAP_CONTENT_ENCODING            = 0x0B;
    public static final int WAP_CONTENT_LANGUAGE            = 0x0C;
    public static final int WAP_CONTENT_LENGTH              = 0x0D;
    public static final int WAP_CONTENT_LOCATION            = 0x0E;
    public static final int WAP_CONTENT_MD5                 = 0x0F;
    public static final int WAP_CONTENT_RANGE1              = 0x10;
    public static final int WAP_CONTENT_TYPE                = 0x11;
    public static final int WAP_DATE                        = 0x12;
    public static final int WAP_ETAG                        = 0x13;
    public static final int WAP_EXPIRES                     = 0x14;
    public static final int WAP_FROM                        = 0x15;
    public static final int WAP_HOST                        = 0x16;
    public static final int WAP_IF_MODIFIED_SINCE           = 0x17;
    public static final int WAP_IF_MATCH                    = 0x18;
    public static final int WAP_IF_NONE_MATCH               = 0x19;
    public static final int WAP_IF_RANGE                    = 0x1A;
    public static final int WAP_IF_UNMODIFIED_SINCE         = 0x1B;
    public static final int WAP_LOCATION                    = 0x1C;
    public static final int WAP_LAST_MODIFIED               = 0x1D;
    public static final int WAP_MAX_FORWARDS                = 0x1E;
    public static final int WAP_PRAGMA                      = 0x1F;
    public static final int WAP_PROXY_AUTHENTICATE          = 0x20;
    public static final int WAP_PROXY_AUTHORIZATION         = 0x21;
    public static final int WAP_PUBLIC                      = 0x22;
    public static final int WAP_RANGE                       = 0x23;
    public static final int WAP_REFERER                     = 0x24;
    public static final int WAP_RETRY_AFTER                 = 0x25;
    public static final int WAP_SERVER                      = 0x26;
    public static final int WAP_TRANSFER_ENCODING           = 0x27;
    public static final int WAP_UPGRADE                     = 0x28;
    public static final int WAP_USER_AGENT                  = 0x29;
    public static final int WAP_VARY                        = 0x2A;
    public static final int WAP_VIA                         = 0x2B;
    public static final int WAP_WARNING                     = 0x2C;
    public static final int WAP_WWW_AUTHENTICATE            = 0x2D;
    public static final int WAP_CONTENT_DISPOSITION1        = 0x2E;
    public static final int WAP_X_WAP_APPLICATION_ID        = 0x2F;
    public static final int WAP_X_WAP_CONTENT_URI           = 0x30;
    public static final int WAP_X_WAP_INITIATOR_URI         = 0x31;
    public static final int WAP_ACCEPT_APPLICATION          = 0x32;
    public static final int WAP_BEARER_INDICATION           = 0x33;
    public static final int WAP_PUSH_FLAG                   = 0x34;
    public static final int WAP_PROFILE                     = 0x35;
    public static final int WAP_PROFILE_DIFF                = 0x36;
    public static final int WAP_PROFILE_WARNING1            = 0x37;
    public static final int WAP_EXPECT                      = 0x38;
    public static final int WAP_TE                          = 0x39;
    public static final int WAP_TRAILER                     = 0x3A;
    public static final int WAP_ACCEPT_CHARSET              = 0x3B;
    public static final int WAP_ACCEPT_ENCODING             = 0x3C;
    public static final int WAP_CACHE_CONTROL1_DEPRECATED   = 0x3D;
    public static final int WAP_CONTENT_RANGE               = 0x3E;
    public static final int WAP_X_WAP_TOD                   = 0x3F;
    public static final int WAP_CONTENT_ID                  = 0x40;
    public static final int WAP_SET_COOKIE                  = 0x41;
    public static final int WAP_COOKIE                      = 0x42;
    public static final int WAP_ENCODING_VERSION            = 0x43;
    public static final int WAP_PROFILE_WARNING             = 0x44;
    public static final int WAP_CONTENT_DISPOSITION         = 0x45;
    public static final int WAP_X_WAP_SECURITY              = 0x46;
    public static final int WAP_CACHE_CONTROL               = 0x47;
}