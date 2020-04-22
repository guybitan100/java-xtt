/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.store.FolderException;

import java.util.Collection;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
class LsubCommand extends ListCommand {
    public static final String NAME = "LSUB";

    protected Collection doList(ImapSession session, String searchPattern)
            throws FolderException {
        return session.getHost().listSubscribedMailboxes(session.getUser(), searchPattern);
    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }
}
