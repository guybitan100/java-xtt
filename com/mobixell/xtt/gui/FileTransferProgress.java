package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.FileTransferAgent;
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
 * @version     $Id: FileTransferProgress.java,v 1.4 2008/03/17 08:59:02 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class FileTransferProgress extends JDialog implements Runnable, WindowListener, ActionListener
{
    public static final String tantau_sccsid = "@(#)$Id: FileTransferProgress.java,v 1.4 2008/03/17 08:59:02 rsoder Exp $";
    private FileTransferAgent agent=null;
    JProgressBar progressBar=null;
    JButton cancel=new JButton("Cancel");
    String filename=null;
    public FileTransferProgress(XTTGui xttgui, FileTransferAgent agent, String filename)
    {
        super(xttgui, "Transfering "+filename,false);
        this.filename=filename;
        JPanel content=new JPanel(new BorderLayout());
        this.agent=agent;
        progressBar = new JProgressBar(0,XTTProperties.getNumberOfTests());
        progressBar.setPreferredSize(new java.awt.Dimension(300,40));
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0% - ?/?");
        content.add(progressBar,BorderLayout.NORTH);
        content.add(cancel,BorderLayout.SOUTH);
        cancel.setActionCommand("CANCEL");
        cancel.addActionListener(this);
        this.getContentPane().add(content);

        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
        this.pack();
        this.setLocationRelativeTo(null); //center it
        this.repaint();
    }
    
    public void view()
    {
        Thread t=new Thread(this,"FileTransferProgress-"+filename);
        t.start();
        this.setVisible(true);
    }
    public void run()
    {
        int current=0;
        int max=100;
        while(!agent.isFinished())
        {
            // This should round to the full %
            current=(int)((agent.getCurrentSize()*1000/agent.getFileSize()+5)/10);
            progressBar.setMaximum(max);
            progressBar.setValue(current);
            progressBar.setString(current+"% - "+(agent.getCurrentSize()/1024)+"KB/"+(agent.getFileSize()/1024)+"KB");
            progressBar.repaint();
            try
            {
                synchronized(agent.progresskey)
                {
                    if(agent.isFinished())break;
                    agent.progresskey.wait();
                }
            } catch (Exception ex){}
        }
        current=(int)((agent.getCurrentSize()*1000/agent.getFileSize()+5)/10);
        progressBar.setValue(current);
        progressBar.setString(current+"% - "+agent.getCurrentSize()+"/"+agent.getFileSize());
        cancel.setText("Close");
        cancel.setActionCommand("CLOSE");
        this.repaint();
    }
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("CLOSE"))
        {
            this.dispose();
        } else if(e.getActionCommand().equals("CANCEL"))
        {
            agent.cancelTransfer();
        }
    }
    public void windowActivated(WindowEvent e){}
    public void windowClosed(WindowEvent e){}
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