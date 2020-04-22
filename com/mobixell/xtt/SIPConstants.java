package com.mobixell.xtt;

/**
 * SIPConstants
 * <p>
 * Holds values used by the SIPServer/SIPWorker class
 */
public interface SIPConstants 
{

    // SIP response codes, class 1xx: Provisional
    public static final int SIP_TRYING                          = 100;
    public static final int SIP_RINGING                         = 180;
    public static final int SIP_CALL_IS_BEING_FORWARDED         = 181;
    public static final int SIP_QUEUED                          = 182;
    public static final int SIP_SESSION_PROGRESS                = 183;

    // SIP Response codes: class 2xx: Success 
    public static final int SIP_OK                              = 200; //OK
    public static final int SIP_ACCEPTED                        = 202; //accepted: Used for referrals
    
    // SIP response codes: class 3xx: Redirection 
    public static final int SIP_MULTIPLE_CHOICES                = 300; //Multiple Choices
    public static final int SIP_MOVED_PERMANENTLY               = 301; //Moved Permanently
    public static final int SIP_MOVED_TEMPORARILY               = 302; //Moved Temporarily
    public static final int SIP_USE_PROXY                       = 305; //Use Proxy
    public static final int SIP_ALTERNATIVE_SERVICE             = 380; //Alternative Service

    // SIP responses codes: class 4xx: Client Error  
    public static final int SIP_BAD_REQUEST                     = 400; //Bad Request
    public static final int SIP_UNAUTHORIZED                    = 401; //Unauthorized: Used only by registrars. Proxys should use proxy authorization 407
    public static final int SIP_PAYMENT_REQUIRED                = 402; //Payment Required (Reserved for future use)
    public static final int SIP_FORBIDDEN                       = 403; //Forbidden
    public static final int SIP_NOT_FOUND                       = 404; //Not Found: User not found
    public static final int SIP_METHOD_NOT_ALLOWED              = 405; //Method Not Allowed
    public static final int SIP_NOT_ACCEPTABLE                  = 406; //Not Acceptable
    public static final int SIP_PROXY_AUTHENTICATION_REQUIRED   = 407; //Proxy Authentication Required
    public static final int SIP_REQUEST_TIMEOUT                 = 408; //Request Timeout: Couldnot find the user in time
    public static final int SIP_GONE                            = 410; //Gone: The user existed once, but is not available here any more.
    public static final int SIP_REQUEST_ENTITY_TOO_LARGE        = 413; //Request Entity Too Large
    public static final int SIP_REQUEST_URI_TOO_LONG            = 414; //Request-URI Too Long
    public static final int SIP_UNSUPPORTED_MEDIA_TYPE          = 415; //Unsupported Media Type
    public static final int SIP_UNSUPPORTED_URI_SCHEME          = 416; //Unsupported URI Scheme
    public static final int SIP_BAD_EXTENSION                   = 420; //Bad Extension: Bad SIP Protocol Extension used, not understood by the server
    public static final int SIP_EXTENSION_REQUIRED              = 421; //Extension Required
    public static final int SIP_INTERVAL_TOO_BRIEF              = 423; //Interval Too Brief
    public static final int SIP_TEMPORARILY_UNAVAILABLE         = 480; //Temporarily Unavailable
    public static final int SIP_CALL_TRANSACTION_DOES_NOT_EXIST = 481; //Call/Transaction Does Not Exist
    public static final int SIP_LOOP_DETECTED                   = 482; //Loop Detected
    public static final int SIP_TOO_MANY_HOPS                   = 483; //Too Many Hops
    public static final int SIP_ADDRESS_INCOMPLETE              = 484; //Address Incomplete
    public static final int SIP_AMBIGUOUS                       = 485; //Ambiguous
    public static final int SIP_BUSY_HERE                       = 486; //Busy Here
    public static final int SIP_REQUEST_TERMINATED              = 487; //Request Terminated
    public static final int SIP_NOT_ACCEPTABLE_HERE             = 488; //Not Acceptable Here
    public static final int SIP_REQUEST_PENDING                 = 491; //Request Pending
    public static final int SIP_UNDECIPHERABLE                  = 493; //Undecipherable: Could not decrypt S/MIME body part

    // SIP responses codes: class 5xx: Server Error  
    public static final int SIP_SERVER_INTERNAL_ERROR           = 500; //Server Internal Error
    public static final int SIP_NOT_IMPLEMENTED                 = 501; //Not Implemented: The SIP request method is not implemented here
    public static final int SIP_BAD_GATEWAY                     = 502; //Bad Gateway
    public static final int SIP_SERVICE_UNAVAILABLE             = 503; //Service Unavailable
    public static final int SIP_SERVER_TIMEOUT                  = 504; //Server Time-out
    public static final int SIP_VERSION_NOT_SUPPORTED           = 505; //Version Not Supported: The server does not support this version of the SIP protocol
    public static final int SIP_MESSAGE_TOO_LARGE               = 513; //Message Too Large

    // SIP response codes: class 6xx: Global failures 
    public static final int SIP_BUSY_EVERYWHERE                 = 600; //Busy Everywhere
    public static final int SIP_DECLINE                         = 603; //Decline
    public static final int SIP_DOES_NOT_EXIST_ANYWHERE         = 604; //Does Not Exist Anywhere
    public static final int SIP_GLOBAL_NOT_ACCEPTABLE           = 606; //Not Acceptable


    public static final String tantau_sccsid = "@(#)$Id: SIPConstants.java,v 1.1 2007/10/01 05:45:22 rsoder Exp $";
}

