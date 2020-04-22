/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.store;

import com.mobixell.xtt.foedus.util.MsgRangeFilter;
import com.mobixell.xtt.mail.MovingMessage;
import com.mobixell.xtt.store.FolderException;
import com.mobixell.xtt.store.FolderListener;
import com.mobixell.xtt.store.MailFolder;
import com.mobixell.xtt.store.SimpleStoredMessage;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.Date;
import java.util.List;

/**
 * Represents a mailbox within an {@link com.mobixell.xtt.store.Store}.
 * May provide storage for MovingMessage objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.2 $
 */
public interface MailFolder {
    String getName();

    String getFullName();

    Flags getPermanentFlags();

    int getMessageCount();

    int getRecentCount(boolean reset);

    long getUidValidity();

    int getFirstUnseen();

    int getUnseenCount();

    boolean isSelectable();

    long getUidNext();

    long appendMessage(MimeMessage message, Flags flags, Date internalDate);

    void deleteAllMessages();

    void expunge() throws FolderException;

    void addListener(FolderListener listener);

    void removeListener(FolderListener listener);

    void store(MovingMessage mail) throws Exception;
    void store(MimeMessage mail) throws Exception;

    SimpleStoredMessage getMessage(long uid);

    long[] getMessageUids();

    long[] search(SearchTerm searchTerm);

    void copyMessage(long uid, MailFolder toFolder)
            throws FolderException;

    void setFlags(Flags flags, boolean value, long uid, FolderListener silentListener, boolean addUid) throws FolderException;

    void replaceFlags(Flags flags, long uid, FolderListener silentListener, boolean addUid) throws FolderException;

    int getMsn(long uid) throws FolderException;

    void signalDeletion();

    @SuppressWarnings("unchecked")
    List getMessages(MsgRangeFilter msgRangeFilter);
    List<SimpleStoredMessage> getMessages();
    List<SimpleStoredMessage> getNonDeletedMessages();
    
    // Quota realated
    long setQuota (long size); 
    long getQuota (); 
    long getQuotaRoot(String foldername) ;
    long getCurrentUsage();
    String getRootName();
}
