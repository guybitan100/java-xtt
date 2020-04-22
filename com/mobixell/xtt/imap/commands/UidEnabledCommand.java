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
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
public interface UidEnabledCommand {
    void doProcess(ImapRequestLineReader request,
                   ImapResponse response,
                   ImapSession session,
                   boolean useUids)
            throws ProtocolException, FolderException;
}
