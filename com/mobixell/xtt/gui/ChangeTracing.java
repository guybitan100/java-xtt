package com.mobixell.xtt.gui;

import javax.swing.*;

import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.awt.event.ActionEvent;



/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: ChangeTracing.java,v 1.2 2008/04/30 09:08:56 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class ChangeTracing extends JMenu implements ActionListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String tantau_sccsid = "@(#)$Id: ChangeTracing.java,v 1.2 2008/04/30 09:08:56 rsoder Exp $";

    private HashMap<Integer,JRadioButtonMenuItem> radios=new HashMap<Integer,JRadioButtonMenuItem>();
    
    JCheckBoxMenuItem external=null;
    JCheckBoxMenuItem transaction=null;

    public ChangeTracing(XTTGui xttgui, String text)
    {
        super(text);
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem radiomenu= new JRadioButtonMenuItem("Fail");
        radiomenu.setActionCommand("TRACE_FAIL");
        radiomenu.addActionListener(this);
        if(XTTProperties.getTracing()==XTTProperties.FAIL)
        {
            radiomenu.setSelected(true);
        }
        group.add(radiomenu);
        radios.put(XTTProperties.FAIL,radiomenu);
        this.add(radiomenu);
        radiomenu= new JRadioButtonMenuItem("Warn");
        radiomenu.setActionCommand("TRACE_WARN");
        radiomenu.addActionListener(this);
        if(XTTProperties.getTracing()==XTTProperties.WARN)
        {
            radiomenu.setSelected(true);
        }
        group.add(radiomenu);
        radios.put(XTTProperties.WARN,radiomenu);
        this.add(radiomenu);
        radiomenu= new JRadioButtonMenuItem("Info");
        radiomenu.setActionCommand("TRACE_INFO");
        radiomenu.addActionListener(this);
        if(XTTProperties.getTracing()==XTTProperties.INFO)
        {
            radiomenu.setSelected(true);
        }
        group.add(radiomenu);
        radios.put(XTTProperties.INFO,radiomenu);
        this.add(radiomenu);
        radiomenu= new JRadioButtonMenuItem("Verbose");
        radiomenu.setActionCommand("TRACE_VERBOSE");
        radiomenu.addActionListener(this);
        if(XTTProperties.getTracing()==XTTProperties.VERBOSE)
        {
            radiomenu.setSelected(true);
        }
        group.add(radiomenu);
        radios.put(XTTProperties.VERBOSE,radiomenu);
        this.add(radiomenu);
        radiomenu= new JRadioButtonMenuItem("Debug");
        radiomenu.setActionCommand("TRACE_DEBUG");
        radiomenu.addActionListener(this);
        if(XTTProperties.getTracing()==XTTProperties.DEBUG)
        {
            radiomenu.setSelected(true);
        }
        group.add(radiomenu);
        radios.put(XTTProperties.DEBUG,radiomenu);
        this.add(radiomenu);
        
        this.addSeparator();
        
        external=new JCheckBoxMenuItem("External");
        external.setActionCommand("EXTERNAL");
        external.addActionListener(this);
        external.setSelected(XTTProperties.getLogExternal());
        this.add(external);
        
        transaction=new JCheckBoxMenuItem("Transaction");
        transaction.setActionCommand("TRANSACTION");
        transaction.addActionListener(this);
        transaction.setSelected(XTTProperties.getLogTransactions());
        this.add(transaction);
        
    }
 
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("TRACE_FAIL"))
        {
            XTTProperties.setTracing(XTTProperties.FAIL);
        } else if(e.getActionCommand().equals("TRACE_WARN"))
        {
            XTTProperties.setTracing(XTTProperties.WARN);
        } else if(e.getActionCommand().equals("TRACE_INFO"))
        {
            XTTProperties.setTracing(XTTProperties.INFO);
        } else if(e.getActionCommand().equals("TRACE_VERBOSE"))
        {
            XTTProperties.setTracing(XTTProperties.VERBOSE);
        } else if(e.getActionCommand().equals("TRACE_DEBUG"))
        {
            XTTProperties.setTracing(XTTProperties.DEBUG);
        } else if(e.getActionCommand().equals("Transaction"))
        {
            XTTProperties.setLogTransactions(!XTTProperties.getLogTransactions());
        } else if(e.getActionCommand().equals("External"))
        {
            XTTProperties.setLogExternal(!XTTProperties.getLogExternal());
        }
    }
    
    public void setTracing(int tracing)
    {
        JRadioButtonMenuItem item=radios.get(tracing);
        item.setSelected(true);
    }
    public void setLogExternal(boolean t)
    {
        if(external!=null)
        {
            external.setSelected(t);
        }
    }
    public void setLogTransaction(boolean t)
    {
        if(transaction!=null)
        {
            transaction.setSelected(t);
        }
    }
}
