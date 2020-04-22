package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.store.FolderException;
import com.mobixell.xtt.store.MailFolder;

/**
 * Handles processeing for the SETQUOTA imap command.
 * ref rfc2087
 * @author Anil Wadhai
 *
 */
public class SetQuotaCammand extends CommandTemplate {
	
    public static final String NAME = "SETQUOTA";
    public static final String ARGS = "<mailboxName> <resourceLimits>";

	 /**
     * @see com.mobixell.xtt.imap.commands.CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException, FolderException {
    	 
    	
    	String mailboxName = parser.mailbox(request);
    	MailFolder folder = session.getHost().getFolder(session.getUser(), mailboxName);
    	long  resourceLimits = parser.number(request);
    	parser.endLine(request);
    	session.getHost().getFolder(session.getUser(), mailboxName).setQuota(resourceLimits);
    	StringBuffer message = new StringBuffer("QUOTA");
    	message.append(SP);
    	message.append(mailboxName);
    	message.append(SP);
    	long currentUsage = session.getHost().getFolder(session.getUser(), mailboxName).getCurrentUsage();
        
    	
    	message.append(currentUsage); 
       	message.append(SP);
    	message.append(resourceLimits);
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
4.1. SETQUOTA Command


   Arguments:  quota root
               list of resource limits

   Data:       untagged responses: QUOTA

   Result:     OK - setquota completed
               NO - setquota error: can't set that data
               BAD - command unknown or arguments invalid

   The SETQUOTA command takes the name of a mailbox quota root and a
   list of resource limits. The resource limits for the named quota root
   are changed to be the specified limits.  Any previous resource limits
   for the named quota root are discarded.

   If the named quota root did not previously exist, an implementation
   may optionally create it and change the quota roots for any number of
   existing mailboxes in an implementation-defined manner.

   Example:    C: A001 SETQUOTA "" (STORAGE 512)
               S: * QUOTA "" (STORAGE 10 512)
               S: A001 OK Setquota completed

*/