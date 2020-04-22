package com.mobixell.xtt.hive;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;

import java.io.File;

public class Message
{
    //All Messages
    //private static java.util.Set<Message> messages = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<Message>());
    private static java.util.Vector<Message> messages = new java.util.Vector<Message>();
    //Local Message Stuff
    private long received = 0;

    private String message = null;
    //The packet sent or received.
    private byte[] messagePacket = new byte[0];

    private Drone sender = null;
    private Drone recipient = null;

    private boolean isBroadcast = false;
    //This is a message sent by us.
    private boolean sentByUs = false;
    //Have we already logged this message?
    private boolean alreadyLogged = false;

    //public static final int FOREIGN     =-1;
    public static final int PENDING     = 0;
    public static final int DELIVERED   = 1;
    public static final int EXPIRED     = 2;

    private int status = PENDING;
    public int getStatus(){return status;}
    public void setStatus(int stat){status=stat;}

    //File attached to the message
    private File file = null;
    //private FileTransferAgent fta = null;
    private boolean pendingFile = false;

    //Use this for synchronizing for waits, notifies.
    public static Object messagekey = new Object();

    public Message()
    {

    }

    public Message(String message, long receivedTime)
    {
        setMessage(message);
        setTime(receivedTime);
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
    public void setMessage(StringBuffer message)
    {
        this.message = message.toString();
    }
    public void setMessage(byte[] message)
    {
        this.message = com.mobixell.xtt.ConvertLib.getStringFromOctetByteArray(message);
    }

    public String getMessage()
    {
        return message;
    }

    public void setIsBroadcast(boolean isBroadcast)
    {
        this.isBroadcast = isBroadcast;
    }

    public boolean getIsBroadcast()
    {
        return isBroadcast;
    }

    public void setTime(long receivedTime)
    {
        received = receivedTime;
    }

    public long getTime()
    {
        return received;
    }

    public String getFormattedTime()
    {
        return getFormattedTime("EEE, d MMM yyyy HH:mm:ss");
    }
    public String getFormattedTime(String dateFormat)
    {
        String datestring = null;
        try
        {
            java.text.SimpleDateFormat format=new java.text.SimpleDateFormat(dateFormat,java.util.Locale.US);
            java.util.GregorianCalendar calendar=new java.util.GregorianCalendar(java.util.Locale.US);
            calendar.setTime(new java.util.Date(received));

            datestring = format.format(calendar.getTime());
        }
        catch(Exception e)
        {
            XTTProperties.printFail("Message: Failed to get formatted datetime");
        }
        return datestring;
    }

    public void setIsFromSelf(boolean sentByUs)
    {
        this.sentByUs = sentByUs;
    }

    public boolean isFromSelf()
    {
        return sentByUs;
    }

    public boolean isDelivered()
    {
        if(sentByUs)
            return status==DELIVERED;
        else
            return true;
    }

    public void setMessagePacket(byte[] messagePacket)
    {
        this.messagePacket = messagePacket;
    }
    public byte[] getMessagePacket()
    {
        return messagePacket;
    }

    public void setSender(Drone sender)
    {
        this.sender = sender;
    }

    public Drone getSender()
    {
        return sender;
    }

    public void setRecipient(Drone recipient)
    {
        this.recipient = recipient;
    }

    public Drone getRecipient()
    {
        return recipient;
    }

    public void setFile(String filename)
    {
        if(!sentByUs) pendingFile = true;
        this.file = new File(filename);
    }
    public String getFile()
    {
        if(file!=null)
            return file.getName();
        else
            return null;
    }
    public String getAbsoluteFilePath()
    {
        if(file!=null)
            return file.getAbsolutePath();
        else
            return null;
    }

    public long getFileSize()
    {
        if(file != null)
            return file.length();
        else
            return -1;
    }

    public void acceptFile(File saveTo) throws java.net.SocketException
    {
        if(file != null)
        {
            try
            {
                Hive.send(Hive.makeMessageReply(message, received, this, saveTo), sender.getInetAddress(), sender.getPort());
                pendingFile = false;
            }
            catch(java.util.EmptyStackException ese)
            {
                pendingFile = true;
                java.net.SocketException se = new java.net.SocketException("Too many current transfers. Try again later");
                se.initCause(ese);
                throw se;
            }
        }
        //return fta;
    }

    public boolean isAcceptPending()
    {
        return pendingFile;
    }

    /*public FileTransferAgent getFileTransferAgent()
    {
        return fta;
    }
    public void setFileTransferAgent(int port, File saveTo)
    {
        fta = new FileTransferAgent(port, saveTo);
        fta.start();
    }
    public void setFileTransferAgent(String filename, java.net.InetAddress recipient, int port)
    {
        fta = new FileTransferAgent(filename,recipient,port);
        fta.start();
    }
    public void setFileTransferAgent(FileTransferAgent fta)
    {
        this.fta = fta;
    }*/

    public void resend()
    {
        if((messagePacket.length > 0) && (!isDelivered()))
        {
            synchronized(messagekey)
            {
                status = PENDING;
                Hive.send(messagePacket,recipient.getInetAddress(),recipient.getPort());
                messagekey.notify();
            }
        }
    }

    public String toString()
    {
        return message;
    }

    public boolean equals(Object obj)
    {
        try
        {
            Message comp = (Message)obj;
            boolean isEqual = message.equals(comp.getMessage()) && sender.equals(comp.getSender()) && (received == comp.getTime());
            return isEqual;
        }
        catch(ClassCastException cce)
        {
            return false;
        }
    }

    /**
     * Creates a quick hashCode for a <code>Message</code>.
     *
     * This only creates the hashCode based on the text in the message.
     */
    public int hashCode()
    {
        if(message == null)
        {
            return new String("No message").hashCode();
        }

        return message.hashCode();
    }

    /**
     * Add this <code>Message</code> to the message list.
     */
    public void add()
    {
        synchronized(messagekey)
        {
            int index = messages.indexOf(this);
            if(index < 0)
            {
                messages.add(this);
                log();
            }
            else
            {
                Message tempMessage = messages.get(index);
                tempMessage.setStatus(DELIVERED);
                if((tempMessage.getFile() == null) && (file != null))
                {
                    tempMessage.setFile(getAbsoluteFilePath());
                }
                tempMessage.log();
            }
            messagekey.notify();
        }
    }
    /**
     * Remove this <code>Message</code> from the message list.
     */
    public void remove()
    {
        synchronized(messagekey)
        {
            messages.remove(this);
            messagekey.notify();
        }
    }

    public void log()
    {
        //if(isDelivered() && !alreadyLogged)
        if(!alreadyLogged)
        {
            logHtml();
        }
    }

    public void logHtml()
    {
        File logFile = null;
        if(isFromSelf())
            logFile = Log.getHtmlLogFileForSender(recipient);
        else
            logFile = Log.getHtmlLogFileForSender(sender);
        alreadyLogged = true;

        StringBuffer logOutput = new StringBuffer();
        String newLine = System.getProperty("line.separator");
        logOutput.append("<table>");
        logOutput.append("<tr>");
        logOutput.append("<td>From: " + sender.getHostname() + "(" + sender.getIp() + ":" + sender.getPort() + ")</td>" + newLine);
        logOutput.append("<td>To:   " + recipient.getHostname() + "(" + recipient.getIp() + ":" + recipient.getPort() + ")</td>" + newLine);
        logOutput.append("</tr><tr><td>Sent: " + getFormattedTime() + "</td>" + newLine);
        if(isBroadcast) logOutput.append("<td>Type: Broadcast</td>" + newLine);
        else            logOutput.append("<td>Type: Personal</td>" + newLine);
        logOutput.append("</tr><table>");
        logOutput.append("</table>");
        String suffix = "</body>" + newLine + "</html>";
        logOutput.append(message + newLine + newLine + suffix);
        Log.appendToLog(logFile, logOutput.toString(), suffix);
    }

    public void logPlainText()
    {
        File logFile = null;
        if(isFromSelf())
            logFile = Log.getPlainTextLogFileForSender(recipient);
        else
            logFile = Log.getPlainTextLogFileForSender(sender);
        alreadyLogged = true;
        Hive.printDebug("Message: Logging to: " + logFile.toString());

        StringBuffer logOutput = new StringBuffer();
        String newLine = System.getProperty("line.separator");
        logOutput.append("From: " + sender.getHostname() + "(" + sender.getIp() + ":" + sender.getPort() + ")" + newLine);
        logOutput.append("To:   " + recipient.getHostname() + "(" + recipient.getIp() + ":" + recipient.getPort() + ")" + newLine);
        logOutput.append("Sent: " + getFormattedTime() + newLine);
        if(isBroadcast) logOutput.append("Type: Broadcast" + newLine);
        else            logOutput.append("Type: Personal" + newLine);
        logOutput.append(message + newLine + newLine);
        Log.appendToLog(logFile, logOutput.toString());
    }

    /**
     * Get number of messages.
     */
    public static int getNumMessages()
    {
        return messages.size();
    }
    /**
     * Get latest message.
     */
    public static Message getLatestMessage()
    {
        Message message = null;
        try
        {
            message = messages.lastElement();
        }
        catch(java.util.NoSuchElementException nsee) {}
        return message;
    }

    /**
     * Get oldest message.
     */
    public static Message getFirstMessage()
    {
        Message message = null;
        try
        {
            message = messages.firstElement();
        }
        catch(java.util.NoSuchElementException nsee) {}
        return message;
    }

    /**
     * Get all the Messages.
     */
    public static Iterable<Message> getMessages()
    {
        return new Iterable<Message>()
        {
            public java.util.Iterator<Message> iterator()
            {
                return new java.util.Iterator<Message>()
                {
                    final java.util.Iterator messageIterator = new java.util.Vector<Message>(messages).iterator();
                    public boolean hasNext()
                    {
                        return messageIterator.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public Message next()
                    {
                        return (Message)messageIterator.next();
                    }
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static final String tantau_sccsid = "@(#)$Id: Message.java,v 1.32 2008/02/19 12:16:47 gcattell Exp $";
}