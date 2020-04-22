package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTTXML;
import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.RemoteXTTClient;
import com.mobixell.xtt.RemoteXTTPacket;

import javax.swing.*;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: ViewRemoteXTT.java,v 1.1 2009/06/11 13:34:36 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class ViewRemoteXTT extends JFrame implements ActionListener, WindowListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String tantau_sccsid = "@(#)$Id: ViewRemoteXTT.java,v 1.1 2009/06/11 13:34:36 rsoder Exp $";
    private JPanel content=new JPanel(new BorderLayout());
    JTextArea textarea=new JTextArea(4,40);
    public ViewRemoteXTT(Drone target)
    {
        this.setTitle("View RemoteXTT Status");
        RemoteXTTPacket dataResponse = RemoteXTTClient.getStatus(new java.net.InetSocketAddress(target.getIp(),target.getRemoteXTTPort()),true);

        JPanel lastTest=new JPanel(new BorderLayout());
        lastTest.setBorder(new javax.swing.border.EtchedBorder(javax.swing.border.EtchedBorder.LOWERED ));
        JPanel lastUser=new JPanel(new BorderLayout());
        lastUser.setBorder(new javax.swing.border.EtchedBorder(javax.swing.border.EtchedBorder.LOWERED ));

        String todo=null;

        try
        {
            String lasttestname     = XTTXML.getElement("response/status/lasttest/name",dataResponse.getDocument()).getText();
            String lasttestnumber   = XTTXML.getElement("response/status/lasttest/number",dataResponse.getDocument()).getText();
            String lasttesttotal    = XTTXML.getElement("response/status/lasttest/total",dataResponse.getDocument()).getText();
            String lastclient       = XTTXML.getElement("response/status/user/client",dataResponse.getDocument()).getText();
            String lastip           = XTTXML.getElement("response/status/user/ip",dataResponse.getDocument()).getText();
            
            todo=XTTXML.getElement("response/status/todo",dataResponse.getDocument()).getText();

            JLabel name=new JLabel("Last Test Name: "+lasttestname);
            lastTest.add(name,BorderLayout.CENTER);
            JLabel executed=new JLabel(" Executed: "+lasttestnumber+"/"+lasttesttotal);
            lastTest.add(executed,BorderLayout.EAST);

            name=new JLabel("Last Client: "+lastclient);
            lastUser.add(name,BorderLayout.CENTER);
            executed=new JLabel(" IP: "+lastip);
            lastUser.add(executed,BorderLayout.EAST);
        
        }
        catch(NullPointerException npe)
        {
            if(dataResponse.getException()!=null)
            {
                Exception ex=dataResponse.getException();
                showError("RemoteXTT Error","Incorrect data was returned by RemoteXTT\n"+ex.getClass().getName()+"\n"+ex.getMessage());
            } else
            {
                showError("RemoteXTT Error","Incorrect data was returned by RemoteXTT");
            }
        }


        this.getContentPane().setLayout(new BorderLayout());
        //this.getContentPane().add(scroll,BorderLayout.CENTER);

        JPanel topInformation=new JPanel(new BorderLayout());
        topInformation.add(new GuiDrone(target),BorderLayout.NORTH);
        topInformation.add(lastTest,BorderLayout.SOUTH);
        content.add(topInformation,BorderLayout.NORTH);     
        textarea.setText(todo);
        JScrollPane textpane=new JScrollPane(textarea);
        JTextArea textarea2=new JTextArea(4,40);
        textarea2.setEditable(false);
        textarea2.setLineWrap(true);
        textarea2.setText("");
        JPanel texts=new JPanel(new BorderLayout());
        texts.add(textpane,BorderLayout.SOUTH);
        texts.add(lastUser,BorderLayout.NORTH);
        content.add(texts,BorderLayout.CENTER);
        
        JPanel buttons=new JPanel(new BorderLayout());

        JButton close=new JButton("Close");
        close.setActionCommand("CLOSE");
        close.addActionListener(this);

        buttons.add(close,BorderLayout.CENTER);
        
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
        } else if(e.getActionCommand().equals("BROADCAST"))
        {
        } else if(e.getActionCommand().equals("SENDFILE"))
        {
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