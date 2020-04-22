package com.mobixell.xtt;

/**
 * MSRPConstants
 * <p>
 * Holds values used by the MSRPServer/MSRPWorker class
 */
public interface MSRPConstants
{
    // 2xx: Success - The action was successfully received, understood, and accepted
    public static final int MSRP_OK                                  =200;

    // 4xx: Client Error - The request contains bad syntax or cannot be fulfilled
    public static final int MSRP_BAD_REQUEST                         =400;
    public static final int MSRP_FORBIDDEN                           =403;
    public static final int MSRP_REQUEST_TIME_OUT                    =408;
    public static final int MSRP_REQUEST_ENTITY_TOO_LARGE            =413;
    public static final int MSRP_UNSUPPORTED_MEDIA_TYPE              =415;
    public static final int MSRP_PARAMETERS_OUT_OF_BOUNDS            =423;
    public static final int MSRP_INVALID_SESSION                     =481;

    // 5xx: Server Error - The server failed to fulfill an apparently valid request
    public static final int MSRP_NOT_IMPLEMENTED                     =501;
    public static final int MSRP_SESSION_ALREADY_BOUND               =506;

    /**
     * Value that represents the '$' char in usascii used to flag the end of a
     * message in a transaction 
     */
    public static final byte ENDMESSAGE                              =36;
  
    /**
     * Value that represents the + char in usascii used to flag the interruption
     * of a message in a transaction
     */
    public static final byte INTERRUPT                               =43;
    
    /**
     * Value that represents the # char in usascii used to flag the abort of a
     * message in a transaction
     */
    public static final byte ABORTMESSAGE                            =35;
    
    public static final String SEVENHYPHEN                           = "-------";
    public static final String LINESEPARATOR                         = "\r\n";
    public static final String DOLLER                                = "$";
    public static final String HASH                                  = "#";
    public static final String PLUS                                  = "+";
    public static final String SINGLEHYPHEN                          = "-";
    public static final String SLASH                                 = "/";
    public static final String ASTERISK                              = "*";
    public static final String BYTERANGE                             = "Byte-Range";
    public static final String TOPATH                                = "To-Path";
    public static final String FROMPATH                              = "From-Path";
    public static final String MESSAGEID                             = "Message-ID";
    public static final String STATUS                                = "Status";
    public static final String MSRP                                  = "MSRP";
    public static final String WHITESPACE                            = " ";
    public static final String REPORT                                = "REPORT";
    public static final String SEND                                  = "SEND";

    public static final String tantau_sccsid = "@(#)$Id: MSRPConstants.java,v 1.1 2009/05/05 11:36:11 awadhai Exp $";
}