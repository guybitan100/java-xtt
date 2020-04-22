/*
 * Copyright (C) 2006 Wael Chatila. All Rights Reserved.
 * Permission to use, read, view, run, copy, compile, link or any
 * other way use this code without prior permission
 * of the copyright holder is not permitted.
 */
package com.mobixell.xtt.user;

import com.mobixell.xtt.mail.MovingMessage;
import com.mobixell.xtt.user.UserException;

import javax.mail.internet.MimeMessage;


public interface GreenMailUser {
    String getEmail();
    String getLogin();

    void deliver(MovingMessage msg) throws UserException;
    void deliver(MimeMessage msg) throws UserException;

    void create() throws UserException;

    void delete()
            throws UserException;

    String getPassword();

    void setPassword(String password);

    void authenticate(String password)
            throws UserException;

    String getQualifiedMailboxName();

}