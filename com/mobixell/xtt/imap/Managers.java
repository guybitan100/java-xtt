/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.mobixell.xtt.imap;

import com.mobixell.xtt.smtp.SmtpManager;
import com.mobixell.xtt.store.InMemoryStore;
import com.mobixell.xtt.user.UserManager;

/**
 * @author Wael Chatila
 * @version $Id: Managers.java,v 1.1 2010/05/14 13:00:44 awadhai Exp $
 * @since Jan 27, 2006
 */
public class Managers {
    private ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
    private UserManager userManager = new UserManager(imapHostManager);
    private SmtpManager smtpManager = new SmtpManager(imapHostManager, userManager);

    public SmtpManager getSmtpManager() {
        return smtpManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public ImapHostManager getImapHostManager() {
        return imapHostManager;
    }
}
