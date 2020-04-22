package com.mobixell.xtt.imap.commands;

import com.mobixell.xtt.imap.commands.ImapCommand;
import com.mobixell.xtt.imap.ImapRequestLineReader;
import com.mobixell.xtt.imap.ImapResponse;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.user.UserException;

public class CreateUserCommand extends NonAuthenticatedStateCommand
{
    public static final String NAME = "CREATEUSER";
    public static final String ARGS = "<userid> <password>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request, ImapResponse response, ImapSession session) throws ProtocolException
    {
        String userid = parser.astring(request);
        String password = parser.astring(request);
        parser.endLine(request);
        if(!session.getUserManager().test(userid, password))
        {
            try
            {
                session.getUserManager().createUser(userid, userid, password);
            } catch (UserException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            response.commandComplete(this);
        } else
        {
            response.commandFailed(this, "user already exist !");
        }

    }

    /**
     * @see ImapCommand#getName
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    public String getArgSyntax()
    {
        return ARGS;
    }
}