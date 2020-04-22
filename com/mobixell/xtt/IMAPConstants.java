package com.mobixell.xtt;

/**
 * IMAPConstants
 * <p>
 * Holds values used by the IMAPWorker class
 *
 * @author Anil Wadhai
 * @version 1.0
 */

public interface IMAPConstants
{
    
    // Basic response types
    public static final String OK = "OK";
    public static final String NO = "NO";
    public static final String BAD = "BAD";
    public static final String BYE = "BYE";
    public static final String UNTAGGED = "*";
  
    /** 2XX: Positive Completion reply */
    public static final String IMAP_CONNECT = "256";
   /* public static final String SMTP_QUIT    = "221";
    public static final String SMTP_OK      = "250";
    public static final String SMTP_OKOK    = "250 OK";*/
    public static final String SP = " ";
    public static final String VERSION = "IMAP4rev1";
    public static final String CAPABILITIES = "LITERAL+";

    public static final String USER_NAMESPACE = "#mail";

    char HIERARCHY_DELIMITER_CHAR = '.';
    char NAMESPACE_PREFIX_CHAR = '#';
    public static final String HIERARCHY_DELIMITER = String.valueOf(HIERARCHY_DELIMITER_CHAR);
    public static final String NAMESPACE_PREFIX = String.valueOf(NAMESPACE_PREFIX_CHAR);

    public static final String INBOX_NAME = "INBOX";
    
    
    // Client Commands - Any State
    public static final String CAPABILITY = "CAPABILITY";
    public static final String NOOP       = "NOOP";
    public static final String LOGOUT     = "LOGOUT";
    
    // Client Commands - Not Authenticated State
    public static final String STARTTLS     = "STARTTLS";
    public static final String AUTHENTICATE = "AUTHENTICATE";
    public static final String LOGIN        = "LOGIN";
    
    // Client Commands - Authenticated State
    
    public static final String SELECT      = "SELECT";
    public static final String EXAMINE     = "EXAMINE";
    public static final String CREATE      = "CREATE";
    public static final String DELETE      = "DELETE";
    public static final String RENAME      = "RENAME";
    public static final String SUBSCRIBE   = "SUBSCRIBE";
    public static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String LIST        = "LIST";
    public static final String LSUB        = "LSUB";
    public static final String STATUS      = "STATUS";
    public static final String APPEND      = "APPEND";
    
    // Quota Cammands
    public static final String SETQUOTA    = "SETQUOTA";
    public static final String GETQUOTA    = "GETQUOTA";
    public static final String GETQUOTAROOT= "GETQUOTAROOT";
    
    // Client Commands - Selected State   
 
    public static final String CHECK   = "CHECK";
    public static final String CLOSE   = "CLOSE";
    public static final String EXPUNGE = "EXPUNGE";
    public static final String SEARCH  = "SEARCH";
    public static final String FETCH   = "FETCH";
    public static final String STORE   = "STORE";
    public static final String COPY    = "COPY";
    public static final String UID     = "UID";
    
    
    // Its not command just to create user at server  for testing
    public static final String CREATEUSER= "CREATEUSER";
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
    
    
    

    
   

}
