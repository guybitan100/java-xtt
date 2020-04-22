/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.mobixell.xtt.pop3.commands;

import com.mobixell.xtt.foedus.util.MsgRangeFilter;

import java.io.StringReader;
import java.util.List;

import com.mobixell.xtt.pop3.Pop3Connection;
import com.mobixell.xtt.pop3.Pop3State;
import com.mobixell.xtt.pop3.commands.Pop3Command;
import com.mobixell.xtt.store.MailFolder;
import com.mobixell.xtt.store.SimpleStoredMessage;
import com.mobixell.xtt.util.GreenMailUtil;

import javax.mail.Flags;


public class RetrCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            MailFolder inbox = state.getFolder();
            String[] cmdLine = cmd.split(" ");

            String msgNumStr = cmdLine[1];
            List msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false));
            if (msgList.size() != 1) {
                conn.println("-ERR no such message");

                return;
            }

            SimpleStoredMessage msg = (SimpleStoredMessage) msgList.get(0);
            String email = GreenMailUtil.getWholeMessage(msg.getMimeMessage());
            conn.println("+OK");
            conn.print(new StringReader(email));
            conn.println();
            conn.println(".");
            msg.getFlags().add(Flags.Flag.SEEN);
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}