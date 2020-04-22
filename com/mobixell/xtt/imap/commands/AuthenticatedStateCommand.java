/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.ImapSessionState;

/**
 * A base class for ImapCommands only valid in AUTHENTICATED and SELECTED states.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
abstract class AuthenticatedStateCommand extends CommandTemplate {
    /**
     * Check that the state is {@link ImapSessionState#AUTHENTICATED } or
     * {@link ImapSessionState#SELECTED}
     */
    public boolean validForState(ImapSessionState state) {
        return (state == ImapSessionState.AUTHENTICATED
                || state == ImapSessionState.SELECTED);
    }
}
