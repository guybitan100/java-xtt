/*
 * $RCSfile: SMTPConstants.java,v $
 * $Revision: 1.2 $
 * $Date: 2006/07/21 17:04:28 $
 * 724 Solutions Inc. Copyright (c) 2005
 */

package com.mobixell.xtt;

/**
 * SMTPConstants
 * <p>
 * Holds values used by the SMTPWorker class
 *
 * @author Roger Soder
 * @version 1.0
 */
public interface SMTPConstants {

    /** 1XX: Positive Preliminary reply */

    /** 2XX: Positive Completion reply */
    public static final String SMTP_CONNECT = "220";
    public static final String SMTP_QUIT    = "221";
    public static final String SMTP_OK      = "250";
    public static final String SMTP_OKOK    = "250 OK";

    /** 3XX: Positive Intermediate reply */
    public static final String SMTP_BEGINDATA = "354 Start mail input; end with <CRLF>.<CRLF>";

    /** 4XX: Transient Negative Completion reply */
    public static final String SMTP_NEGATIVE_SYSERROR = "451 Requested action aborted: local error in processing of address";

    /** 5XX: Permanent Negative Completion reply */
    public static final String SMTP_UNKNOWN             = "500 Syntax error, command unrecognized";
    public static final String SMTP_SYNTAXERROR         = "501 Syntax error in parameters or arguments";
    public static final String SMTP_SYNTAXERROR_FORWARD = "501 Syntax error in forward path";
    public static final String SMTP_BADSEQUENCE         = "503 Bad sequence of commands";
    public static final String SMTP_NORECIPIENTS        = "503 No recipient(s).";

	public static final String tantau_sccsid = "@(#)$Id: SMTPConstants.java,v 1.2 2006/07/21 17:04:28 cvsbuild Exp $";
}

