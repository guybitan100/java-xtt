/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.mobixell.xtt.imap;

import com.mobixell.xtt.imap.commands.ImapCommand;
import com.mobixell.xtt.store.MessageFlags;
import com.mobixell.xtt.util.InternetPrintWriter;

import javax.mail.Flags;
import java.io.OutputStream;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ImapResponse implements ImapConstants {
    private InternetPrintWriter writer;
    private String tag = UNTAGGED;
    
    StringBuffer response_buffer = new StringBuffer();

    public ImapResponse(OutputStream output) {
        this.writer = new InternetPrintWriter(output, true);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Writes a standard tagged OK response on completion of a command.
     * Response is writen as:
     * <pre>     a01 OK COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     */
    public void commandComplete(ImapCommand command) {
        commandComplete(command, null);
    }

    /**
     * Writes a standard tagged OK response on completion of a command,
     * with a response code (eg READ-WRITE)
     * Response is writen as:
     * <pre>     a01 OK [responseCode] COMMAND_NAME completed.</pre>
     *
     * @param command      The ImapCommand which was completed.
     * @param responseCode A string response code to send to the client.
     */
    public void commandComplete(ImapCommand command, String responseCode) {
        response_buffer = new StringBuffer();
        tag();
        message(OK);
        responseCode(responseCode);
        commandName(command);
        message("completed");
        end();
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param reason  A message describing why the command failed.
     */
    public void commandFailed(ImapCommand command, String reason) {
        commandFailed(command, null, reason);
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 NO [responseCode] COMMAND_NAME failed. <reason></pre>
     *
     * @param command      The ImapCommand which failed.
     * @param responseCode The Imap response code to send.
     * @param reason       A message describing why the command failed.
     */
    public void commandFailed(ImapCommand command,
                              String responseCode,
                              String reason) {
        response_buffer = new StringBuffer();
        tag();
        message(NO);
        responseCode(responseCode);
        commandName(command);
        message("failed");
        message(reason);
        end();
    }

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message.
     * Response is writen as:
     * <pre>     a01 BAD <message></pre>
     *
     * @param message The descriptive error message.
     */
    public void commandError(String message) {
        response_buffer = new StringBuffer();
        tag();
        message(BAD);
        message(message);
        end();
    }

    /**
     * Writes a standard untagged BAD response, together with a descriptive message.
     */
    public void badResponse(String message) {
        response_buffer = new StringBuffer();
        untagged();
        message(BAD);
        message(message);
        end();
    }

    /**
     * Writes an untagged OK response, with the supplied response code,
     * and an optional message.
     *
     * @param responseCode The response code, included in [].
     * @param message      The message to follow the []
     */
    public void okResponse(String responseCode, String message) {
        response_buffer = new StringBuffer();
        untagged();
        message(OK);
        responseCode(responseCode);
        message(message);
        end();
    }

    public void flagsResponse(Flags flags) {
        response_buffer = new StringBuffer();
        untagged();
        message("FLAGS");
        message(MessageFlags.format(flags));
        end();
    }

    public void existsResponse(int count) {
        response_buffer = new StringBuffer();
        untagged();
        message(count);
        message("EXISTS");
        end();
    }

    public void recentResponse(int count) {
        response_buffer = new StringBuffer();
        untagged();
        message(count);
        message("RECENT");
        end();
    }

    public void expungeResponse(int msn) {
        response_buffer = new StringBuffer();
        untagged();
        message(msn);
        message("EXPUNGE");
        end();
    }

    public void fetchResponse(int msn, String msgData) {
        response_buffer = new StringBuffer();
        untagged();
        message(msn);
        message("FETCH");
        message("(" + msgData + ")");
        end();
    }

    public void commandResponse(ImapCommand command, String message) {
        response_buffer = new StringBuffer();
        untagged();
        commandName(command);
        message(message);
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * request tag.
     *
     * @param message The message to write to the client.
     */
    public void taggedResponse(String message) {
        response_buffer = new StringBuffer();
        tag();
        message(message);
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * untagged marker "*".
     *
     * @param message The message to write to the client.
     */
    public void untaggedResponse(String message) {
        response_buffer = new StringBuffer();
        untagged();
        message(message);
        end();
    }

    public void byeResponse(String message) {
        untaggedResponse(BYE + SP + message);
        response_buffer.append(BYE + SP + message);
    }

    private void untagged() {
        writer.print(UNTAGGED);
        response_buffer.append(UNTAGGED);
    }

    private void tag() {
        response_buffer.append(tag);
        writer.print(tag);
    }

    private void commandName(ImapCommand command) {
        String name = command.getName();
        writer.print(SP);
        writer.print(name);
        response_buffer.append(SP);
        response_buffer.append(name);
    }

    private void message(String message) {
        if (message != null) {
            writer.print(SP);
            writer.print(message);
            response_buffer.append(SP);
            response_buffer.append(message);
        }
    }

    private void message(int number) {
        writer.print(SP);
        writer.print(number);
        response_buffer.append(SP);
        response_buffer.append(number);
        
    }

    private void responseCode(String responseCode) {
        if (responseCode != null) {
            writer.print(" [");
            writer.print(responseCode);
            writer.print("]");
            response_buffer.append(" [");
            response_buffer.append(responseCode);
            response_buffer.append("]");
        }
    }

    private void end() {
        writer.println();
        writer.flush();
       // XTTProperties.printInfo(response_buffer.toString());
    }

    public void permanentFlagsResponse(Flags flags) {
        response_buffer = new StringBuffer();
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + MessageFlags.format(flags));
        end();
    }
}
