/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.mobixell.xtt.pop3.commands;

import java.util.HashMap;
import java.util.Map;

import com.mobixell.xtt.pop3.commands.ApopCommand;
import com.mobixell.xtt.pop3.commands.DeleCommand;
import com.mobixell.xtt.pop3.commands.ListCommand;
import com.mobixell.xtt.pop3.commands.NoopCommand;
import com.mobixell.xtt.pop3.commands.PassCommand;
import com.mobixell.xtt.pop3.commands.Pop3Command;
import com.mobixell.xtt.pop3.commands.QuitCommand;
import com.mobixell.xtt.pop3.commands.RetrCommand;
import com.mobixell.xtt.pop3.commands.RsetCommand;
import com.mobixell.xtt.pop3.commands.StatCommand;
import com.mobixell.xtt.pop3.commands.TopCommand;
import com.mobixell.xtt.pop3.commands.UidlCommand;
import com.mobixell.xtt.pop3.commands.UserCommand;


public class Pop3CommandRegistry {
    private static Map<String, Pop3Command> commands = new HashMap<String, Pop3Command>();
    private static Object[][] COMMANDS = new Object[][]
    {
        {"QUIT", QuitCommand.class}, {"STAT", StatCommand.class},
        {"APOP", ApopCommand.class}, {"USER", UserCommand.class},
        {"PASS", PassCommand.class}, {"LIST", ListCommand.class},
        {"UIDL", UidlCommand.class}, {"TOP", TopCommand.class},
        {"RETR", RetrCommand.class}, {"DELE", DeleCommand.class}
        , {"NOOP", NoopCommand.class}, {"RSET", RsetCommand.class}

    };

    public void load()
            throws Exception {
        for (int i = 0; i < COMMANDS.length; i++) {
            String name = COMMANDS[i][0].toString();

            if (commands.containsKey(name))

                continue;

            try {
                Class type = (Class) COMMANDS[i][1];
                Pop3Command command = (Pop3Command) type.newInstance();
                registerCommand(name, command);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    private void registerCommand(String name, Pop3Command command) {
        commands.put(name, command);
    }

    public Pop3Command getCommand(String name) {
        if (commands.size() == 0) {
            try {
                load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return (Pop3Command) commands.get(name);
    }
}