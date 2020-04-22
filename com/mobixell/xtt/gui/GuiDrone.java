package com.mobixell.xtt.gui;

import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.hive.Drone;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.awt.Color;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: GuiDrone.java,v 1.5 2009/06/11 13:34:36 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class GuiDrone extends JPanel implements ActionListener
{
    public static final String tantau_sccsid = "@(#)$Id: GuiDrone.java,v 1.5 2009/06/11 13:34:36 rsoder Exp $";
    private Drone drone=null;
    private ViewHive hive=null;
    public GuiDrone(Drone drone)
    {
        this(drone,null,true);
    }
    public GuiDrone(Drone drone, ViewHive hive)
    {
        this(drone,hive,false);
    }
    public GuiDrone(Drone drone, ViewHive hive, boolean viewOnly)
    {
        this.drone=drone;
        this.hive=hive;
        this.setLayout(new BorderLayout());
        JPanel center=new JPanel(new BorderLayout());
        center.setBorder(new javax.swing.border.EtchedBorder(javax.swing.border.EtchedBorder.LOWERED ));
        JPanel top=new JPanel(new BorderLayout());
        JPanel bottom=new JPanel(new BorderLayout());
        
        JButton open=null;
        if(!viewOnly)
        {
            open=new JButton("Open");
            open.setActionCommand("OPEN");
            open.addActionListener(this);
        }            
        String remotePort="";
        if(drone.getRemoteXTTPort()>0)remotePort="/"+drone.getRemoteXTTPort();
        JLabel name=new JLabel(drone.getHostname()+" ("+drone.getIp()+":"+drone.getPort()+remotePort+")");
        name.setForeground(Color.BLUE);
        top.add(name,BorderLayout.CENTER);
        
        long uptime=drone.getUptime();
        String uptimeS="";
        if(uptime>=24*60*60)
        {
            uptimeS=(uptime/(24*60*60))+"d"+ConvertLib.longToString((uptime%(24*60*60))/(60*60),2)+"h";
        } else if(uptime>=60*60)
        {
            uptimeS=(uptime/(60*60))+"h"+ConvertLib.longToString((uptime%(60*60))/60,2)+"m";
        } else if(uptime>=0)
        {
            uptimeS=(uptime/(60))+"m"+ConvertLib.longToString(uptime%60,2)+"s";
        } else
        {
            uptimeS="unknown";
        }
        
        SimpleDateFormat format=new SimpleDateFormat();
        JLabel seen=new JLabel(" Seen: "+format.format(drone.getLastSeen().getTime())+" Uptime: "+uptimeS);
        bottom.add(seen,BorderLayout.EAST);
        
        /*java.util.Iterator<String> it=drone.getComment().iterator();
        StringBuffer comments=new StringBuffer("Comment: ");
        while(it.hasNext())
        {
            comments.append(it.next()+"\n");
        }*/
        JLabel comment=new JLabel("Comment: "+drone.getComments());
        bottom.add(comment,BorderLayout.CENTER);
        
        String type=null;
        String version=drone.getVersion();
        if(version==null)version="OBSOLETE";
        switch(drone.getDroneType())
        {
            case Drone.XTTDRONE:
                type="Local XTT ("+version+")";
                break;
            case Drone.REMOTEDRONE:
                type="Remote XTT ("+version+")";
                break;
            default:
                type="Unknown ("+version+")";
                break;
            
        }
        JLabel dtype=new JLabel(" Type: "+type);
        top.add(dtype,BorderLayout.EAST);

        center.add(top,BorderLayout.CENTER);
        center.add(bottom,BorderLayout.SOUTH);
        this.add(center,BorderLayout.CENTER);
        if(!viewOnly)
        {
            this.add(open,BorderLayout.EAST);
        }
    }
    public void actionPerformed(ActionEvent e)
    {
        if(drone.getDroneType()==Drone.REMOTEDRONE&&drone.getRemoteXTTPort()>0)
        {
            ViewRemoteXTT v=new ViewRemoteXTT(drone);
        }else if(drone.getDroneType()==Drone.REMOTEDRONE)
        {
            showError("Send Error","Unsupported version of RemoteXTT!");
        } else
        {
            SendMessage s=new SendMessage(drone);        
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