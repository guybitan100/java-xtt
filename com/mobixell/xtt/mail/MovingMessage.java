/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.mobixell.xtt.mail;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.mobixell.xtt.foedus.util.Resource;
import com.mobixell.xtt.foedus.util.Workspace;
import com.mobixell.xtt.mail.MailAddress;
import com.mobixell.xtt.util.GreenMailUtil;
import com.mobixell.xtt.util.InternetPrintWriter;


/**
 * Contains information for delivering a mime email.
 * <p/>
 * <p/>
 * Since a MovingMessage many be passed through many queues and
 * handlers before it can be safely deleted, destruction it handled
 * by reference counting. When an object first obtains a reference
 * to a MovingMessage, it should immediately call {@link #acquire()}.
 * As soon as it has finished processing, that object must call
 * {@link #releaseContent()}.  For example usage, see {@link
 * foedus.processing.OutgoingImpl}.
 * </p>
 */
public class MovingMessage {
    MailAddress returnPath;
    List<MailAddress> toAddresses = new LinkedList<MailAddress>();
    Workspace _workspace;
    Resource _content;
    MimeMessage message;
    int _references = 0;

    public List<MailAddress> getToAddresses() {
        return toAddresses;
    }

    public MovingMessage(Workspace workspace) {
        _workspace = workspace;
    }

    public MimeMessage getMessage() {
        return message;
    }

    public Reader getContent()
            throws IOException {

        return _content.getReader();
    }

    public void acquire() {
        _references++;
    }

    public void releaseContent() {
        if (_references > 0) {
            _references--;
        } else if (_content != null) {
            _workspace.release(_content);
            _content = null;
        }
    }

    public MailAddress getReturnPath() {

        return returnPath;
    }

    public void setReturnPath(MailAddress fromAddress) {
        this.returnPath = fromAddress;
    }

    public void addRecipient(MailAddress s) {
        toAddresses.add(s);
    }

    public void removeRecipient(MailAddress s) {
        toAddresses.remove(s);
    }

    public Iterator<MailAddress> getRecipientIterator() {

        return toAddresses.iterator();
    }

    /**
     * Reads the contents of the stream until
     * &lt;CRLF&gt;.&lt;CRLF&gt; is encountered.
     * <p/>
     * <p/>
     * It would be possible and prehaps desirable to prevent the
     * adding of an unnecessary CRLF at the end of the message, but
     * it hardly seems worth 30 seconds of effort.
     * </p>
     */
    public void readDotTerminatedContent(BufferedReader in)
            throws IOException {
        _content = _workspace.getTmpFile();
        Writer data = _content.getWriter();
        PrintWriter dataWriter = new InternetPrintWriter(data);

        while (true) {
            String line = in.readLine();
            if (line == null)
                throw new EOFException("Did not receive <CRLF>.<CRLF>");

            if (".".equals(line)) {
                dataWriter.close();

                break;
            } else {
                dataWriter.println(line);
            }
        }
        try {
            message = GreenMailUtil.newMimeMessage(_content.getAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}