package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.store.FolderException;

/**
 * Handles processeing for the GETQUOTA imap command.
 * ref rfc2087
 * @author Anil Wadhai
 *
 */
public class GetQuotaCammand extends CommandTemplate {
	
    public static final String NAME = "GETQUOTA";
    public static final String ARGS = "<mailboxName>";

	 /**
     * @see com.mobixell.xtt.imap.commands.CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session) throws ProtocolException, FolderException {
	String mailboxName = parser.mailbox(request);
    	
    	parser.endLine(request);
    	session.getHost().getFolder(session.getUser(), mailboxName).getQuota();
    	StringBuffer message = new StringBuffer("QUOTA");
    	message.append(SP);
    	message.append(mailboxName);
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

4.2. GETQUOTA Command


   Arguments:  quota root

   Data:       untagged responses: QUOTA

   Result:     OK - getquota completed
               NO - getquota  error:  no  such  quota  root,  permission
               denied
               BAD - command unknown or arguments invalid

   The GETQUOTA command takes the name of a quota root and returns the
   quota root's resource usage and limits in an untagged QUOTA response.

   Example:    C: A003 GETQUOTA ""
               S: * QUOTA "" (STORAGE 10 512)
               S: A003 OK Getquota completed

5.1. QUOTA Response


   Data:       quota root name
               list of resource names, usages, and limits

      This response occurs as a result of a GETQUOTA or GETQUOTAROOT
      command. The first string is the name of the quota root for which
      this quota applies.

      The name is followed by a S-expression format list of the resource
      usage and limits of the quota root.  The list contains zero or
      more triplets.  Each triplet conatins a resource name, the current
      usage of the resource, and the resource limit.

      Resources not named in the list are not limited in the quota root.
      Thus, an empty list means there are no administrative resource
      limits in the quota root.

      Example:    S: * QUOTA "" (STORAGE 10 512)



*/