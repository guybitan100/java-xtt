package com.mobixell.xtt;

/**
 * RTSPConstants
 * <p>
 * Holds values used by the RTSPServer/RTSPWorker class
 */
public interface RTSPConstants
{
    // 1xx: Informational - Request received, continuing process
    public static final int RTSP_CONTINUE                            =100;
    
    // 2xx: Success - The action was successfully received, understood, and accepted
    public static final int RTSP_OK                                  =200;
    public static final int RTSP_CREATED                             =201;
    public static final int RTSP_LOW_ON_STORAGE_SPACE                =250;
    
    // 3xx: Redirection - Further action must be taken in order to complete the request
    public static final int RTSP_MULTIPLE_CHOICES                    =300;
    public static final int RTSP_MOVED_PERMANENTLY                   =301;
    public static final int RTSP_MOVED_TEMPORARILY                   =302;
    public static final int RTSP_SEE_OTHER                           =303;
    public static final int RTSP_NOT_MODIFIED                        =304;
    public static final int RTSP_USE_PROXY                           =305;
    
    // 4xx: Client Error - The request contains bad syntax or cannot be fulfilled
    public static final int RTSP_BAD_REQUEST                         =400;
    public static final int RTSP_UNAUTHORIZED                        =401;
    public static final int RTSP_PAYMENT_REQUIRED                    =402;
    public static final int RTSP_FORBIDDEN                           =403;
    public static final int RTSP_NOT_FOUND                           =404;
    public static final int RTSP_METHOD_NOT_ALLOWED                  =405;
    public static final int RTSP_NOT_ACCEPTABLE                      =406;
    public static final int RTSP_PROXY_AUTHENTICATION_REQUIRED       =407;
    public static final int RTSP_REQUEST_TIME_OUT                    =408;
    public static final int RTSP_GONE                                =410;
    public static final int RTSP_LENGTH_REQUIRED                     =411;
    public static final int RTSP_PRECONDITION_FAILED                 =412;
    public static final int RTSP_REQUEST_ENTITY_TOO_LARGE            =413;
    public static final int RTSP_REQUEST_URI_TOO_LARGE               =414;
    public static final int RTSP_UNSUPPORTED_MEDIA_TYPE              =415;
    public static final int RTSP_PARAMETER_NOT_UNDERSTOOD            =451;
    public static final int RTSP_CONFERENCE_NOT_FOUND                =452;
    public static final int RTSP_NOT_ENOUGH_BANDWIDTH                =453;
    public static final int RTSP_SESSION_NOT_FOUND                   =454;
    public static final int RTSP_METHOD_NOT_VALID_IN_THIS_STATE      =455;
    public static final int RTSP_HEADER_FIELD_NOT_VALID_FOR_RESOURCE =456;
    public static final int RTSP_INVALID_RANGE                       =457;
    public static final int RTSP_PARAMETER_IS_READ_ONLY              =458;
    public static final int RTSP_AGGREGATE_OPERATION_NOT_ALLOWED     =459;
    public static final int RTSP_ONLY_AGGREGATE_OPERATION_ALLOWED    =460;
    public static final int RTSP_UNSUPPORTED_TRANSPORT               =461;
    public static final int RTSP_DESTINATION_UNREACHABLE             =462;
    
    // 5xx: Server Error - The server failed to fulfill an apparently valid request
    public static final int RTSP_INTERNAL_SERVER_ERROR               =500;
    public static final int RTSP_NOT_IMPLEMENTED                     =501;
    public static final int RTSP_BAD_GATEWAY                         =502;
    public static final int RTSP_SERVICE_UNAVAILABLE                 =503;
    public static final int RTSP_GATEWAY_TIME_OUT                    =504;
    public static final int RTSP_RTSP_VERSION_NOT_SUPPORTED          =505;
    public static final int RTSP_OPTION_NOT_SUPPORTED                =551;



    public static final String tantau_sccsid = "@(#)$Id: RTSPConstants.java,v 1.1 2009/02/06 14:24:14 rsoder Exp $";
}

