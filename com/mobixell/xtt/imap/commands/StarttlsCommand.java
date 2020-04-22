package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
/**
 * Handles processeing for the STARTTLS imap command.
 * @author Anil Wadhai
 * @version $Revision: 1.1 $
 */

public class StarttlsCommand extends NonAuthenticatedStateCommand {

    public static final String NAME = "STARTTLS";
    public static final String ARGS = null;
    
    public static final String STARTTLS_RESPONSE = OK + SP + "Begin TLS negotiation now";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException {
        
        parser.endLine(request);
        response.taggedResponse(STARTTLS_RESPONSE);
        
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
    
   /*
      6.2.1. STARTTLS Command


   Arguments:  none

   Responses:  no specific response for this command

   Result:     OK - starttls completed, begin TLS negotiation
               BAD - command unknown or arguments invalid

      A [TLS] negotiation begins immediately after the CRLF at the end
      of the tagged OK response from the server.  Once a client issues a
      STARTTLS command, it MUST NOT issue further commands until a
      server response is seen and the [TLS] negotiation is complete.

      The server remains in the non-authenticated state, even if client
      credentials are supplied during the [TLS] negotiation.  This does
      not preclude an authentication mechanism such as EXTERNAL (defined
      in [SASL]) from using client identity determined by the [TLS]
      negotiation.

      Once [TLS] has been started, the client MUST discard cached
      information about server capabilities and SHOULD re-issue the
      CAPABILITY command.  This is necessary to protect against man-in-
      the-middle attacks which alter the capabilities list prior to
      STARTTLS.  The server MAY advertise different capabilities after
      STARTTLS.

   Example:    C: a001 CAPABILITY
               S: * CAPABILITY IMAP4rev1 STARTTLS LOGINDISABLED
               S: a001 OK CAPABILITY completed
               C: a002 STARTTLS
               S: a002 OK Begin TLS negotiation now
               <TLS negotiation, further commands are under [TLS] layer>
               C: a003 CAPABILITY
               S: * CAPABILITY IMAP4rev1 AUTH=PLAIN
               S: a003 OK CAPABILITY completed
               C: a004 LOGIN joe password
               S: a004 OK LOGIN completed

     */


}
