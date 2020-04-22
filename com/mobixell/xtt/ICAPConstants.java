package com.mobixell.xtt;

/**
 * ICAPConstants
 * <p>
 * Holds values used by the WebServer class
 *
 */
public interface ICAPConstants 
{

    public static final int ICAP_CONTINUE = 100;

    /** 2XX: generally "OK" */
    public static final int ICAP_OK                 = 200;
    public static final int ICAP_CREATED            = 201;
    public static final int ICAP_ACCEPTED           = 202;
    public static final int ICAP_NOT_AUTHORITATIVE  = 203;
    public static final int ICAP_NO_MODIFICATION    = 204;
    public static final int ICAP_RESET              = 205;
    public static final int ICAP_PARTIAL            = 206;

    /** 3XX: relocation/redirect */
    public static final int ICAP_MULT_CHOICE        = 300;
    public static final int ICAP_MOVED_PERM         = 301;
    public static final int ICAP_MOVED_TEMP         = 302;
    public static final int ICAP_SEE_OTHER          = 303;
    public static final int ICAP_NOT_MODIFIED       = 304;
    public static final int ICAP_USE_PROXY          = 305;

    /** 4XX: client error */
    public static final int ICAP_BAD_REQUEST        = 400;
    public static final int ICAP_UNAUTHORIZED       = 401;
    public static final int ICAP_PAYMENT_REQUIRED   = 402;
    public static final int ICAP_FORBIDDEN          = 403;
    public static final int ICAP_NOT_FOUND          = 404;
    public static final int ICAP_METHOD_NOT_ALLOWED = 405;
    public static final int ICAP_NOT_ACCEPTABLE     = 406;
    public static final int ICAP_PROXY_AUTH         = 407;
    public static final int ICAP_REQUEST_TIMEOUT    = 408;
    public static final int ICAP_CONFLICT           = 409;
    public static final int ICAP_GONE               = 410;
    public static final int ICAP_LENGTH_REQUIRED    = 411;
    public static final int ICAP_PRECON_FAILED      = 412;
    public static final int ICAP_ENTITY_TOO_LARGE   = 413;
    public static final int ICAP_REQ_TOO_LONG       = 414;
    public static final int ICAP_UNSUPPORTED_TYPE   = 415;
    public static final int ICAP_BAD_COMPOSITION    = 418;

    /** 5XX: server error */
    public static final int ICAP_SERVER_ERROR       = 500;
    public static final int ICAP_NOT_IMPLEMENTED    = 501;
    public static final int ICAP_BAD_GATEWAY        = 502;
    public static final int ICAP_OVERLOADED         = 503;
    public static final int ICAP_GATEWAY_TIMEOUT    = 504;
    public static final int ICAP_VERSION_UNSUPPORTED= 505;

    public static final String tantau_sccsid = "@(#)$Id: ICAPConstants.java,v 1.1 2007/03/30 14:18:16 rsoder Exp $";
}

