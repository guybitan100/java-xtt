/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap.commands;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 1.1 $
 */
public interface IdSet {
    boolean includes(long value);
}
