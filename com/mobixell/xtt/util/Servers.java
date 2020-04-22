/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.mobixell.xtt.util;

import com.mobixell.xtt.imap.ImapServer;
import com.mobixell.xtt.imap.Managers;
import com.mobixell.xtt.pop3.Pop3Server;
import com.mobixell.xtt.smtp.SmtpManager;
import com.mobixell.xtt.smtp.SmtpServer;
import com.mobixell.xtt.store.SimpleStoredMessage;
import com.mobixell.xtt.user.GreenMailUser;
import com.mobixell.xtt.user.UserException;
import com.mobixell.xtt.util.GreenMail;
import com.mobixell.xtt.util.ServerSetup;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Wael Chatila
 * @version $Id: Servers.java,v 1.1 2010/05/14 13:00:46 awadhai Exp $
 * @since Jan 28, 2006
 * @deprecated Use GreenMail.java instead
 */
public class Servers extends GreenMail {

    public Servers() {
        super();
    }

    public Servers(ServerSetup config) {
        super(config);
    }

    public Servers(ServerSetup[] config) {
        super(config);
    }
}
