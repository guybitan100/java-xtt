/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.mobixell.xtt.imap;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Vector;
import com.mobixell.xtt.util.DummySSLServerSocketFactory;
import com.mobixell.xtt.util.ServerSetup;
import com.mobixell.xtt.util.Service;

/**
 * @author Wael Chatila
 * @version $Id: AbstractServer.java,v 1.3 2010/07/09 10:50:33 mlichtin Exp $
 * @since Feb 2, 2006
 */
public abstract class AbstractServer extends Service {
    protected final InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected Vector<Object> handlers = null;
    protected Managers managers;
    protected ServerSetup setup;

    protected AbstractServer(ServerSetup setup, Managers managers) {
        try {
            this.setup = setup;
            bindTo = (setup.getBindAddress() == null) ? InetAddress.getByName("0.0.0.0") : InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.managers = managers;
        handlers = new Vector<Object>();
    }

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret = null;
        IOException retEx = null;
        for (int i=0;i<25 && (null == ret);i++) {
            try {
                if (setup.isSecure()) {
                    ret = DummySSLServerSocketFactory.getDefault().createServerSocket();
                } else {
                    ret = new ServerSocket();
                }
                ret.setReuseAddress(true);
                ret.bind(new java.net.InetSocketAddress(bindTo, setup.getPort()));
            } catch (BindException e) {
                try {
                    retEx = e;
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }
        if (null == ret && null != retEx) {
            throw retEx;
        }
        return ret;
    }

    public String getBindTo() {
        return bindTo.getHostAddress();
    }

    public int getPort() {
        return setup.getPort();
    }

    public String getProtocol() {
        return setup.getProtocol();
    }

    public ServerSetup getServerSetup() {
        return setup;
    }

    public String toString() {
        return null!=setup? setup.getProtocol()+':'+setup.getPort() : super.toString();
    }
}
