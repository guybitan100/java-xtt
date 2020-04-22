/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
public class ProtocolException extends Exception {
	private static final long serialVersionUID = 7233591775059795506L;

	public ProtocolException(String s) {
        super(s);
    }
}
