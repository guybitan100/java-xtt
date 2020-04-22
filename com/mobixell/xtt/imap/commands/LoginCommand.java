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
import com.mobixell.xtt.user.GreenMailUser;

/**
 * Handles processeing for the LOGIN imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.2 $
 */
class LoginCommand extends NonAuthenticatedStateCommand {
    public static final String NAME = "LOGIN";
    public static final String ARGS = "<userid> <password>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException {
        String userid = parser.astring(request);
        String password = parser.astring(request);
        parser.endLine(request);
        
        // TODO: need to remove - START
     /*   if (userid.equalsIgnoreCase("anil.wadhai@xoriant.com"))
        {
            try
            {
                session.getUserManager().createUser("anil.wadhai@xoriant.com", "anil.wadhai@xoriant.com", "anil.wadhai@xoriant.com");
            } catch (UserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }*/
        // TODO: need to remove - END
        
        if (session.getUserManager().test(userid, password)) {
            GreenMailUser user = session.getUserManager().getUser(userid);
            session.setAuthenticated(user);
            response.commandComplete(this);

        } else {
            response.commandFailed(this, "Invalid login/password");
        }
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
6.2.2.  LOGIN Command

   Arguments:  user name
               password

   Responses:  no specific responses for this command

   Result:     OK - login completed, now in authenticated state
               NO - login failure: user name or password rejected
               BAD - command unknown or arguments invalid

      The LOGIN command identifies the client to the server and carries
      the plaintext password authenticating this user.

   Example:    C: a001 LOGIN SMITH SESAME
               S: a001 OK LOGIN completed
*/
