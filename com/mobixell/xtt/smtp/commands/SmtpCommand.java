/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.mobixell.xtt.smtp.commands;

import com.mobixell.xtt.smtp.SmtpConnection;
import com.mobixell.xtt.smtp.SmtpManager;
import com.mobixell.xtt.smtp.SmtpState;

import java.io.IOException;

public abstract class SmtpCommand {

    public abstract void execute(SmtpConnection conn,
                                 SmtpState state,
                                 SmtpManager manager,
                                 String commandLine)
            throws IOException;
}
