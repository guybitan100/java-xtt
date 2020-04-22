package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.hive.Drone;
import com.mobixell.xtt.hive.Hive;

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
 * @version     $Id: ViewHive.java,v 1.3 2008/03/17 08:59:02 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class ViewHive extends JFrame implements ActionListener, WindowListener, Runnable
{
    public static final String tantau_sccsid = "@(#)$Id: ViewHive.java,v 1.3 2008/03/17 08:59:02 rsoder Exp $";
    private JPanel content=new JPanel(new VerticalFlowLayout());
    XTTGui xttgui=null;
    
    public ViewHive(XTTGui xttgui)
    {
        this.xttgui=xttgui;
        
        this.setTitle("View Hive");

        JScrollPane scroll=new JScrollPane(content);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(scroll,BorderLayout.CENTER);

        scroll.setPreferredSize(new Dimension(XTTGui.SUBWINDOWX,XTTGui.SUBWINDOWY));
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        fillContent();
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
        Thread t=new Thread(this,"ViewHive");
        t.start();
        //setVisible(true);
    }
    
    private void fillContent()
    {
        content.removeAll();
        java.util.Iterator<Drone> it=Hive.getDrones().iterator();
        String text=null;
        while(it.hasNext())
        {
            content.add(new GuiDrone(it.next(),this));
        }
        content.revalidate();
        content.repaint();
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("REFRESH"))
        {
            if(XTTProperties.getHive()==null)
            {
                xttgui.showError("Hive Error","You have not joined the Hive");
            } else
            {
                try
                {
                    XTTProperties.getHive().refreshDroneList();
                    //Thread.sleep(2000);
                    //fillContent();
                } catch (Exception ex)
                {
                    xttgui.showError("Hive Error",ex.getClass().getName()+"\n"+ex.getMessage());
                }
            }

        } else
        {
            this.setVisible(false);
        }
    }
    public void run()
    {
        while(true)
        {
            try
            {
                synchronized(Hive.dronereskey)
                {
                    Hive.dronereskey.wait();
                }
                Thread.sleep(2000);
                fillContent();
            } catch(Exception ex)
            {
                XTTProperties.printException(ex);
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

    public void windowActivated(WindowEvent e){}
    public void windowClosed(WindowEvent e)
    {
        this.setVisible(false);
    }
    public void windowClosing(WindowEvent e)
    {
    }
    public void windowDeactivated(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowIconified(WindowEvent e){}
    public void windowOpened(WindowEvent e){}
}