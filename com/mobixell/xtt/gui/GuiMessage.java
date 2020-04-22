package com.mobixell.xtt.gui;

import com.mobixell.xtt.*;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.hive.Hive;
import com.mobixell.xtt.hive.Message;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.awt.Color;
import java.io.File;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: GuiMessage.java,v 1.6 2008/02/19 12:16:44 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class GuiMessage extends JPanel implements ActionListener
{
    public static final String tantau_sccsid = "@(#)$Id: GuiMessage.java,v 1.6 2008/02/19 12:16:44 rsoder Exp $";
    private Message message=null;
    private ViewMessages vm=null;
    private JButton accept=new JButton("Accept");
    XTTGui xttgui=null;

    public GuiMessage(Message message, ViewMessages vm)
    {
        this.message=message;
        this.vm=vm;
        this.xttgui=xttgui;
        this.setLayout(new BorderLayout());
        JPanel center=new JPanel(new BorderLayout());
        center.setBorder(new javax.swing.border.EtchedBorder(javax.swing.border.EtchedBorder.LOWERED ));
        JPanel top=new JPanel(new BorderLayout());
        JPanel topTop=new JPanel(new BorderLayout());
        JPanel topBottom=new JPanel(new BorderLayout());
        JPanel topFrom=new JPanel(new BorderLayout());
        JPanel topTo=new JPanel(new BorderLayout());
        JPanel topFile=new JPanel(new BorderLayout());
        JPanel bottom=new JPanel(new BorderLayout());
        JPanel buttons=new JPanel(new BorderLayout());
        JPanel buttonsReply=new JPanel(new BorderLayout());

        JButton delete=new JButton("  Delete  ");
        delete.setActionCommand("DELETE");
        delete.addActionListener(this);
        JButton reply=new JButton("Reply");
        reply.setActionCommand("REPLY");
        reply.addActionListener(this);
        if(message.isFromSelf())
        {
            reply.setEnabled(false);
            if(message.getStatus()==Message.DELIVERED)
            {
                reply.setText("DONE");
                reply.setBackground(Color.GREEN);
            } else if(message.getStatus()==Message.EXPIRED&&message.getRecipient()!=null)
            {
                reply.setText("Resend");
                reply.setEnabled(true);
                reply.setBackground(Color.RED);
                reply.setActionCommand("RESEND");
            } else
            {
                reply.setText("Sending");
                reply.setBackground(Color.ORANGE);
            }
        }
        buttonsReply.add(reply,BorderLayout.NORTH);
        buttons.add(buttonsReply,BorderLayout.NORTH);
        buttons.add(delete,BorderLayout.SOUTH);
        this.add(buttons,BorderLayout.EAST);

        Drone drone=message.getSender();
        Drone droneTo=message.getRecipient();

        JLabel from=new JLabel("From: ");
        JLabel name=new JLabel(drone.getHostname()+" ("+drone.getIp()+":"+drone.getPort()+")");
        name.setForeground(Color.BLUE);
        topFrom.add(name,BorderLayout.CENTER);
        topFrom.add(from,BorderLayout.WEST);

        if(message.getFile()!=null)
        {
            accept.setEnabled(message.isAcceptPending());
            accept.setActionCommand("ACCEPTFILE");
            accept.addActionListener(this);
            buttonsReply.add(accept,BorderLayout.SOUTH);
            JLabel file=new JLabel("File: ");
            file.setPreferredSize(from.getPreferredSize());
            JLabel filename=new JLabel(message.getFile());
            filename.setForeground(Color.RED);
            topFile.add(filename,BorderLayout.CENTER);
            topFile.add(file,BorderLayout.WEST);
            JLabel filesize=new JLabel(" Size: "+(message.getFileSize()/1024)+"KB");
            topFile.add(filesize,BorderLayout.EAST);
            topBottom.add(topFile,BorderLayout.NORTH);
        }

        SimpleDateFormat format=new SimpleDateFormat();
        JLabel seen=new JLabel(" Sent: "+message.getFormattedTime());
        topFrom.add(seen,BorderLayout.EAST);

        String typeS="Personal";
        if(message.getIsBroadcast())
        {
            typeS="Broadcast";
        } else
        {
            JLabel to=new JLabel("To: ");
            JLabel nameTo=new JLabel(droneTo.getHostname()+" ("+droneTo.getIp()+":"+droneTo.getPort()+")");
            nameTo.setForeground(Color.BLUE);
            to.setPreferredSize(from.getPreferredSize());
            topTo.add(nameTo,BorderLayout.CENTER);
            topTo.add(to,BorderLayout.WEST);
        }

        JLabel type=new JLabel(" Type: "+typeS);
        topTo.add(type,BorderLayout.EAST);

        top.add(topTop,BorderLayout.NORTH);
        top.add(topBottom,BorderLayout.SOUTH);
        topTop.add(topFrom,BorderLayout.NORTH);
        topTop.add(topTo,BorderLayout.SOUTH);

        JTextArea textarea=new JTextArea(4,40);
        textarea.setEditable(false);
        textarea.setLineWrap(true);
        textarea.setText(message.getMessage());
        JScrollPane textpane=new JScrollPane(textarea);
        bottom.add(textpane,BorderLayout.CENTER);

        center.add(top,BorderLayout.CENTER);
        center.add(bottom,BorderLayout.SOUTH);
        this.add(center,BorderLayout.CENTER);

    }
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("DELETE"))
        {
            message.remove();
            vm.refreshMessages();
        } else if(e.getActionCommand().equals("REPLY"))
        {
            SendMessage s=new SendMessage(message.getSender(), message.getMessage());
        } else if(e.getActionCommand().equals("RESEND"))
        {
            try
            {
                message.resend();
                Hive.printDebug("XTTGui.GuiMessage: Re-Sent message to: "+message.getRecipient().getHostname()+" ("+message.getRecipient().getIp()+":"+message.getRecipient().getPort()+")");
            } catch (Exception ex)
            {
                showError("Re-Send Error",ex.getClass().getName()+"\n"+ex.getMessage());
            }
        } else if(e.getActionCommand().equals("ACCEPTFILE"))
        {
            JFileChooser fc = new JFileChooser(".");
            fc.setSelectedFile(new File(message.getFile()));
            //In response to a button click:
            int returnVal = fc.showSaveDialog(this);
            if(returnVal==fc.APPROVE_OPTION)
            {
                //fc.setSelectedFile(new File(message.getFile()));
                File file = fc.getSelectedFile();

                if(file.exists())
                {
                    try
                    {

                        Object[] options = {"Yes, overwrite","No, cancel"};
                        int n = JOptionPane.showOptionDialog(this,
                            "The file already exists!\n"
                            + "Are you sure you want to overwrite:\n"
                            + file.getCanonicalPath(),
                            "Confirme overwriting file",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[1]);
                        if(n!=JOptionPane.YES_OPTION)
                        {
                            return;
                        }
                    } catch (Exception ex)
                    {
                        showError("File Accept Error",ex.getClass().getName()+"\n"+ex.getMessage());
                    }
                }
                try
                {
                    accept.setEnabled(false);
                    XTTProperties.printDebug("SAVE: "+file.getCanonicalPath());
                    message.acceptFile(file);
                } catch (Exception ex)
                {
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ex);
                    }
                    showError("File Accept Error",ex.getClass().getName()+"\n"+ex.getMessage());
                } finally
                {
                    accept.setEnabled(message.isAcceptPending());
                }
            }
        }
    }

    public void showError(String title,String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext,
            title,
            JOptionPane.ERROR_MESSAGE);
    }

}