/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.store.FolderException;

/**
 * Handles processeing for the UNSUBSCRIBE imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
class UnsubscribeCommand extends AuthenticatedStateCommand {
    public static final String NAME = "UNSUBSCRIBE";
    public static final String ARGS = "<mailbox>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        session.getHost().unsubscribe(session.getUser(), mailboxName);
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
