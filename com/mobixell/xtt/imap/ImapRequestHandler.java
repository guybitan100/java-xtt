/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap;

import com.mobixell.xtt.imap.commands.CommandParser;
import com.mobixell.xtt.imap.commands.ImapCommand;
import com.mobixell.xtt.imap.commands.ImapCommandFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
public final class ImapRequestHandler {
    private ImapCommandFactory imapCommands = new ImapCommandFactory();
    private CommandParser parser = new CommandParser();
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    /**
     * This method parses POP3 commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @return whether additional commands are expected.
     */
    public boolean handleRequest(InputStream input,
                                 OutputStream output,
                                 ImapSession session)
            throws ProtocolException {
        ImapRequestLineReader request = new ImapRequestLineReader(input, output);
        try {
            request.nextChar();
        } catch (ProtocolException e) {
            return false;
        }
        ImapResponse response = new ImapResponse(output);
        doProcessRequest(request, response, session);

        // Consume the rest of the line, throwing away any extras. This allows us
        // to clean up after a protocol error.
        request.consumeLine();

        return true;
    }

    private void doProcessRequest(ImapRequestLineReader request,
                                  ImapResponse response,
                                  ImapSession session) {
        String tag = null;
        String commandName = null;

        try {
            tag = parser.tag(request);
        } catch (ProtocolException e) {
            response.badResponse(REQUEST_SYNTAX);
            return;
        }

        response.setTag(tag);
        try {
            commandName = parser.atom(request);
        } catch (ProtocolException e) {
            response.commandError(REQUEST_SYNTAX);
            return;
        }

        ImapCommand command = imapCommands.getCommand(commandName);
        if (command == null) {
            response.commandError("Invalid command.");
            return;
        }

        if (!command.validForState(session.getState())) {
            response.commandFailed(command, "Command not valid in this state");
            return;
        }

        command.process(request, response, session);
    }


}
