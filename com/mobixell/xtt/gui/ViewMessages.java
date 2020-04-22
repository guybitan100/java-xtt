package com.mobixell.xtt.gui;

import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Message;

import javax.swing.*;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: ViewMessages.java,v 1.2 2008/02/19 09:08:22 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class ViewMessages extends JFrame implements ActionListener, WindowListener
{
    public static final String tantau_sccsid = "@(#)$Id: ViewMessages.java,v 1.2 2008/02/19 09:08:22 rsoder Exp $";
    private JPanel content=new JPanel(new VerticalFlowLayout());
    private MessageListener ml=null;
    JScrollPane scroll=null;

    public ViewMessages(MessageListener m)
    {
        this.setTitle("View Messages");
        ml=m;

        scroll=new JScrollPane(content);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(scroll,BorderLayout.CENTER);

        scroll.setPreferredSize(new Dimension(XTTGui.SUBWINDOWX,XTTGui.SUBWINDOWY));
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        viewMessages();
        JPanel buttons=new JPanel();
        buttons.setLayout(new BorderLayout());

        JButton close=new JButton("Close");
        close.setActionCommand("CLOSE");
        close.addActionListener(this);
        JButton refresh=new JButton("Refresh");
        refresh.setActionCommand("REFRESH");
        refresh.addActionListener(this);
        
        buttons.add(close,BorderLayout.EAST);
        buttons.add(refresh,BorderLayout.WEST);
        
        this.getContentPane().add(buttons,BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(null); //center it
        refresh.requestFocusInWindow();
        //setVisible(true);
    }
    public void setScrollToMax()
    {
        scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        synchronized(content)
        {
            content.revalidate();
            content.repaint();
        }
    }
    public void refreshMessages()
    {
        viewMessages();
        ml.updateMessages();
    }
    public void viewMessages()
    {
        synchronized(content)
        {
            content.removeAll();
            int i=0;
            for (Message message: Message.getMessages())
            {
                i++;
                content.add(new GuiMessage(message,this));
            }
            content.revalidate();
            content.repaint();
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("REFRESH"))
        {
            refreshMessages();
        } else
        {
            ml.updateMessages();
            this.setVisible(false);
        }
    }
    public void windowActivated(WindowEvent e){}
    public void windowClosed(WindowEvent e)
    {
        ml.updateMessages();
        this.setVisible(false);
    }
    public void windowClosing(WindowEvent e)
    {
    }
    public void windowDeactivated(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowIconified(WindowEvent e){}
    public void windowOpened(WindowEvent e){}

    public void showError(String title,String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext,
            title,
            JOptionPane.ERROR_MESSAGE);
    }

}
