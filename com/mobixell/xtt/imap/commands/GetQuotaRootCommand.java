package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.store.FolderException;
import com.mobixell.xtt.store.MailFolder;

/**
 * Handles processeing for the GETQUOTAROOT imap command.
 * ref rfc2087
 * @author Anil Wadhai
 *
 */
public class GetQuotaRootCommand extends CommandTemplate {
	
    public static final String NAME = "GETQUOTAROOT";
    public static final String ARGS = "<mailboxName>";

	 /**
     * @see com.mobixell.xtt.imap.commands.CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException, FolderException {
    	 
    	
    	String mailboxName = parser.mailbox(request);
    	
    	parser.endLine(request);
    
    	MailFolder folder = session.getHost().getFolder(session.getUser(), mailboxName);
    	StringBuffer message = new StringBuffer("QUOTAROOT");
    	message.append(SP);
    	message.append(mailboxName);
    	message.append(SP).append("\"\"");
    
    	
    	response.untaggedResponse(message.toString());
    	
    	message = new StringBuffer("QUOTA");
    	message.append(SP).append("\"\"");
    	message.append(SP).append("(");
    	message.append("STORAGE");
    	message.append(SP);
    	
        long currentUsage = session.getHost().getFolder(session.getUser(), mailboxName).getCurrentUsage();
        
    	message.append(currentUsage); //TODO: 
       	message.append(SP);
       	long resourceLimits = session.getHost().getFolder(session.getUser(), mailboxName).getQuota();
       	message.append(resourceLimits);
       	message.append(")");
       	response.untaggedResponse(message.toString());

       	session.unsolicitedResponses(response);
        response.commandComplete(this);

    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    public String getArgSyntax() {
        return ARGS;
    }
}

/*

http://tools.ietf.org/html/rfc2087#page-2

4.3. GETQUOTAROOT Command


   Arguments:  mailbox name

   Data:       untagged responses: QUOTAROOT, QUOTA

   Result:     OK - getquota completed
               NO - getquota error: no such mailbox, permission denied
               BAD - command unknown or arguments invalid

   The GETQUOTAROOT command takes the name of a mailbox and returns the
   list of quota roots for the mailbox in an untagged QUOTAROOT
   response.  For each listed quota root, it also returns the quota
   root's resource usage and limits in an untagged QUOTA response.

   Example:    C: A003 GETQUOTAROOT INBOX
               S: * QUOTAROOT INBOX ""
               S: * QUOTA "" (STORAGE 10 512)
               S: A003 OK Getquota completed


5.2. QUOTAROOT Response


   Data:       mailbox name
               zero or more quota root names

      This response occurs as a result of a GETQUOTAROOT command.  The
      first string is the mailbox and the remaining strings are the
      names of the quota roots for the mailbox.

      Example:    S: * QUOTAROOT INBOX ""
                  S: * QUOTAROOT comp.mail.mime


*/