package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.hive.Hive;
import javax.swing.*;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: SendMessage.java,v 1.3 2008/02/19 12:05:10 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class SendMessage extends JFrame implements ActionListener, WindowListener
{
    public static final String tantau_sccsid = "@(#)$Id: SendMessage.java,v 1.3 2008/02/19 12:05:10 rsoder Exp $";
    private JPanel content=new JPanel(new BorderLayout());
    JTextArea textarea=new JTextArea(4,40);
    private Drone target=null;

    public SendMessage(Drone target)
    {
        this(target,null);
    }
    public SendMessage(Drone target, String recieved)
    {
        this.setTitle("Send Message");
        this.target=target;

        this.getContentPane().setLayout(new BorderLayout());
        //this.getContentPane().add(scroll,BorderLayout.CENTER);

        content.add(new GuiDrone(target),BorderLayout.NORTH);
        //content.setPreferredSize(new Dimension(XTTGui.SUBWINDOWX,XTTGui.SUBWINDOWY));
        //JTextArea textarea=new JTextArea(4,40);
        textarea.setEditable(true);
        textarea.setLineWrap(true);
        textarea.setText("");
        JScrollPane textpane=new JScrollPane(textarea);
        JTextArea textarea2=new JTextArea(4,40);
        textarea2.setEditable(false);
        textarea2.setLineWrap(true);
        textarea2.setText(recieved);
        JScrollPane textpane2=new JScrollPane(textarea2);
        
        JPanel texts=new JPanel(new BorderLayout());
        if(recieved!=null)
        {
            texts.add(textpane2,BorderLayout.NORTH);
        }
        texts.add(textpane,BorderLayout.SOUTH);
        content.add(texts,BorderLayout.CENTER);
        
        JPanel buttons=new JPanel(new BorderLayout());
        JPanel buttonsLeft=new JPanel(new FlowLayout());

        JButton close=new JButton("Close");
        close.setActionCommand("CLOSE");
        close.addActionListener(this);
        JButton send=new JButton("Send");
        send.setActionCommand("SEND");
        send.addActionListener(this);
        JButton broadcast=new JButton("Broadcast");
        broadcast.setActionCommand("BROADCAST");
        broadcast.addActionListener(this);
        JButton sendfile=new JButton("Send File");
        sendfile.setActionCommand("SENDFILE");
        sendfile.addActionListener(this);

        buttonsLeft.add(send);
        buttonsLeft.add(broadcast);
        buttonsLeft.add(sendfile);
        
        buttons.add(close,BorderLayout.EAST);
        buttons.add(buttonsLeft,BorderLayout.WEST);
        
        this.add(content,BorderLayout.NORTH);
        this.add(buttons,BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo(null); //center it
        textarea.requestFocusInWindow();
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("SEND"))
        {
            try
            {
                if(textarea.getText().equals(""))return;
                XTTProperties.getHive().sendMesssage(textarea.getText(),target);
                Hive.printDebug("XTTGui.SendMessage: Sent message to: "+target.getHostname()+" ("+target.getIp()+":"+target.getPort()+")");
                this.dispose();
            } catch (Exception ex)
            {
                showError("Send Error",ex.getClass().getName()+"\n"+ex.getMessage());
            }
        } else if(e.getActionCommand().equals("BROADCAST"))
        {
            try
            {
                if(textarea.getText().equals(""))return;
                XTTProperties.getHive().sendGlobalMesssage(textarea.getText());
                Hive.printDebug("XTTGui.SendMessage: Sent message broadcast");
                this.dispose();
            } catch (Exception ex)
            {
                showError("Send Error",ex.getClass().getName()+"\n"+ex.getMessage());
            }

        } else if(e.getActionCommand().equals("SENDFILE"))
        {
            JFileChooser fc = new JFileChooser(".");
            //fc.setFileFilter(new LISTFileFilter());
            //In response to a button click:
            int returnVal = fc.showOpenDialog(this);
            if(returnVal==fc.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                
                if(!file.exists())
                {
                    showError("File Send Error","File '"+file+"' not found");
                } else if(file.isDirectory())
                {
                    showError("File Send Error","Can not send a directory!");
                } else if(file.length()<=0)
                {
                    showError("File Send Error","File length is "+file.length()+"!");
                } else
                {
                    XTTProperties.getHive().sendFileRequest(textarea.getText(),target,file);
                    Hive.printDebug("XTTGui.SendMessage: Sent file request to: "+target.getHostname()+" ("+target.getIp()+":"+target.getPort()+")");
                    this.dispose();
                }    
            }
        } else
        {
            this.dispose();
        }
    }
    public void windowActivated(WindowEvent e){}
    public void windowClosed(WindowEvent e)
    {
        this.dispose();
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