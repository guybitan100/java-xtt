package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Message;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: MessageListener.java,v 1.3 2008/03/17 08:59:02 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class MessageListener extends JMenu implements ActionListener, Runnable
{
    public static final String tantau_sccsid = "@(#)$Id: MessageListener.java,v 1.3 2008/03/17 08:59:02 rsoder Exp $";
    private int lastMessages=0;
    private ViewMessages vm=new ViewMessages(this);
    private JCheckBoxMenuItem autoList =new JCheckBoxMenuItem("Popup List");
    private JCheckBoxMenuItem autoOpen =new JCheckBoxMenuItem("Popup Message");
    private JCheckBoxMenuItem autoFront=new JCheckBoxMenuItem("XTT window to Front");
    private XTTGui xttgui       = null;

    public MessageListener(XTTGui xttgui)
    {
        super(Message.getNumMessages()+" Messages");
        this.xttgui=xttgui;
        lastMessages=Message.getNumMessages();
        
        JMenuItem menuItem=null;/*new JMenuItem("Edit Configuration");*/
        menuItem=new JMenuItem("View Messages");
        menuItem.addActionListener(this);
        this.add(menuItem);
        autoList.setState(false);
        autoOpen.setState(false);
        autoFront.setState(false);
        Thread t=new Thread(this,"MessageListener");
        t.start();
        
    }
    public void setPopupList(boolean state)
    {
        autoList.setState(state);
    }
    public void setPopupMessage(boolean state)
    {
        autoOpen.setState(state);
    }
    public void setXttToFront(boolean state)
    {
        autoFront.setState(state);
    }
    public void finishMenu()
    {
        this.addSeparator();
        JMenuItem menuItem=new JMenuItem("On Message:");
        this.add(menuItem);
        this.add(autoList);
        this.add(autoOpen);
        this.add(autoFront);
    }
    
    public void run()
    {
        int currentMessages=0;
        Message message=null;
        int newMessages=0;
        synchronized(Message.messagekey)
        {
            while(true)
            {
                try
                {
                    Message.messagekey.wait();
                    message=Message.getLatestMessage();
                    currentMessages=Message.getNumMessages();
                    newMessages=currentMessages-lastMessages;
                    vm.viewMessages();
                    if(autoFront())
                    {
                        xttgui.toFront();
                    }
                    if(autoList())
                    {
                        //SendMessage s=new SendMessage(message.getSender(), message.getMessage());                            
                        vm.setVisible(true);
                        vm.toFront();
                        updateMessages();
                        vm.setScrollToMax();
                    }
                    if(autoPopup())
                    {
                        SendMessage s=new SendMessage(message.getSender(), message.getMessage());                            
                    }
                    if(currentMessages!=lastMessages)
                    {
                        this.setText(currentMessages+" Messages ("+newMessages+" New)");
                    }
                } catch(Exception ex)
                {
                    xttgui.showError("Message Exception","Error while recieving Message: "+ex.getClass().getName());
                    XTTProperties.printException(ex);
                }
            }
        }
    }
    public void actionPerformed(ActionEvent e)
    {
        updateMessages();
        vm.setVisible(true);
    }
    public void updateMessages()
    {
        lastMessages=Message.getNumMessages();
        this.setText(lastMessages+" Messages");
    }
    public boolean autoPopup()
    {
        return autoOpen.getState();
    }
    public boolean autoFront()
    {
        return autoFront.getState();
    }
    public boolean autoList()
    {
        return autoList.getState();
    }
}