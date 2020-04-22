package com.mobixell.xtt.gui;

import javax.swing.*;

import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTConfigurationLocalPermanent;
import com.mobixell.xtt.XTTConfiguration;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.XTTXML;
import com.mobixell.xtt.gui.main.XTTGui;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream ;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;


/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: ConfigurationEditor.java,v 1.14 2008/03/17 14:01:41 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class ConfigurationEditor extends JFrame implements WindowListener,ActionListener,KeyListener
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String tantau_sccsid = "@(#)$Id: ConfigurationEditor.java,v 1.14 2008/03/17 14:01:41 rsoder Exp $";
    private XTTGui xttgui=null;
    private JEditTextArea ta = new JEditTextArea(this);
    private String defaultConfig="";
    private String previousText="";
    private String currentFileName="";
    private XTTConfiguration config=null;
    public ConfigurationEditor(XTTGui xttgui,XTTConfiguration config, String configFile,boolean isDefaultConfig)
    {
        this.config=config;
        this.addWindowListener(this);
        this.xttgui=xttgui;
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        addKeyListener(this);
        
        if(configFile!=null && isDefaultConfig)
        {
            this.defaultConfig="<!-- EXAMPLE CONFIGURATION FILE      -->"
                            +"\n<!-- PLEASE CHANGE/ADD/REMOVE VALUES -->"
                            +"\n"+configFile+"\n";
        }
        else
        {
        	setTitle("Configuration File Editor");
        	this.defaultConfig=configFile;	
        }

        JMenuBar menuBar=new JMenuBar();
        this.setJMenuBar(menuBar);
        
        JMenu filemenu=new JMenu("File");;
        menuBar.add(filemenu);

        JMenuItem menuItem=new JMenuItem("New/Load File");
        menuItem.setActionCommand("LOAD");
        filemenu.add(menuItem);
        menuItem.addActionListener(this);

        filemenu.addSeparator();

        menuItem=new JMenuItem("Save");
        menuItem.setActionCommand("SAVE");
        filemenu.add(menuItem);
        menuItem.addActionListener(this);

        menuItem=new JMenuItem("Save as");
        menuItem.setActionCommand("SAVEAS");
        filemenu.add(menuItem);
        menuItem.addActionListener(this);

        filemenu.addSeparator();

        menuItem=new JMenuItem("Save, Set, Reload & Exit");
        menuItem.setActionCommand("SET");
        filemenu.add(menuItem);
        menuItem.addActionListener(this);

        menuItem=new JMenuItem("Cancel & Exit");
        menuItem.setActionCommand("EXIT");
        filemenu.add(menuItem);
        menuItem.addActionListener(this);

        JMenu searchMenu=new JMenu("Search");
      
        menuItem=new JMenuItem("Find");
        menuItem.setActionCommand("FIND");
        searchMenu.add(menuItem);
        menuItem.addActionListener(this);
        menuBar.add(searchMenu);
        
        JPopupMenu popup = new JPopupMenu();

        menuItem=null;
		menuItem = new JMenuItem("Select All");
		menuItem.setActionCommand("ALL");
		menuItem.addActionListener(this);
		popup.add(menuItem);
		
        menuItem=new JMenuItem("Copy to Clipboard");
        menuItem.setActionCommand("COPY");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem=new JMenuItem("Paste from Clipboard");
        menuItem.setActionCommand("PASTE");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
        ta.setRightClickPopup(popup);
        
        ta.setTokenMarker(new XMLTokenMarker());

        if(config!=null)
        {
            try
            {
                //Class c=Class.forName("com.mobixell.xtt.XTTConfigurationRemote");
                if(config.getFileName()==null)
                {
                    this.setTitle(config.getFileName()+" - Configuration Editor - Generated (Read Only)");
                    ta.setText(XTTXML.stringXML(config.getDocument()));
                    ta.setCaretPosition(0);
                    ta.scrollToCaret();
                    currentFileName="";
                    ta.setEditable(false);
                } else
                {
                    currentFileName=config.getFileName();
                    loadFile(currentFileName);
                }
            } catch(Exception ex)
            {
                ta.setText(this.defaultConfig);
                ta.setCaretPosition(0);
                ta.scrollToCaret();
                currentFileName="";
            }
        } else
        {
            ta.setText(this.defaultConfig);
            ta.setCaretPosition(0);
            ta.scrollToCaret();
            currentFileName="";
        }


        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(ta,BorderLayout.CENTER);

        this.pack();
        this.setLocationRelativeTo(xttgui); //center it
        this.setVisible(true);
    }
    private void loadFile(String filename)
    {
        try
        {
            File file=new File(filename);
            if(!file.exists())
            {
                XTTProperties.printDebug("Creating file name: "+filename);
                file.createNewFile();
            } else
            {
                XTTProperties.printDebug("Loading file name: "+filename);
            }
            int size=(int)file.length();
            char[] content=new char[size];
            InputStreamReader reader=new InputStreamReader(new FileInputStream(file),XTTProperties.getCharSet());
            int numchars=reader.read(content,0,size);
            String contentS=new String(content,0,numchars);
            previousText=contentS;
            if(contentS.equals(""))contentS=this.defaultConfig;
            ta.setText(contentS);
            ta.setCaretPosition(0);
            ta.scrollToCaret();
            this.currentFileName=xttgui.createShortFileName(file.getAbsolutePath());;
            this.setTitle(this.currentFileName+" - Configuration Editor - Local");
        } catch (Exception e)
        {
            xttgui.showConfigurationError(e.getClass().getName()+"\n"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("EXIT"))
        {
            this.dispose();
        } else if(e.getActionCommand().equals("SET"))
        {
            if(XTT.isTestRunning())
            {
                xttgui.showError("Run Error","Tests are running, can't save and set configuration!");
                return;
            }
            try
            {
                org.jdom.Document document=checkCurrentFile();
                if(document==null)return;
                if(currentFileName.equals(""))
                {
                    if(!saveAS())return;
                }
                if(!ta.getText().equals(previousText))
                {
                    writeFile(ta.getText());
                }
                if(config==null)
                {
                    config=new XTTConfigurationLocalPermanent(currentFileName);
                } else
                {
                    config.setDocument(document);
                }
                config.add();
                this.dispose();
                //xttgui.enable();
            } catch (Exception ex)
            {
                String error="Error:\n";
                showSaveError(error+ex);
            }
        } else if(e.getActionCommand().equals("SAVE"))
        {
            try
            {
                org.jdom.Document document=checkCurrentFile();
                if(document==null)return;
                if(currentFileName.equals(""))
                {
                    if(!saveAS())return;
                } else
                {
                    writeFile(ta.getText());
                }
                ta.setEditable(true);
                //loadFile(currentFileName);
            } catch (Exception ex)
            {
                String error="Error:\n";
                showSaveError(error+ex);
            }
        } else if(e.getActionCommand().equals("SAVEAS"))
        {
            saveAS();
            ta.setEditable(true);
        } else if(e.getActionCommand().equals("LOAD"))
        {
            JFileChooser fc = new JFileChooser(".");
            fc.setFileFilter(new XMLFileFilter());
            //In response to a button click:
            int returnVal = fc.showOpenDialog(this);
            if(returnVal==JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if(!file.exists()&&!file.getAbsolutePath().endsWith(".xml"))
                {
                    file=new File(file.getAbsolutePath()+".xml");
                }
                config=null;
                loadFile(file.getAbsolutePath());
            }
        } else if(e.getActionCommand().equals("COPY"))
        {
            java.awt.datatransfer.StringSelection contents=new java.awt.datatransfer.StringSelection(ta.getSelectedText());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
        } else if(e.getActionCommand().equals("PASTE"))
        {
            ta.paste();
        }
        else if(e.getActionCommand().equals("FIND"))
        {
        	ta.search();
        }
        else if(e.getActionCommand().equals("ALL"))
        {
            ta.selectAll();
        }
    }
    public JEditTextArea getTaConf()
   	{
   		return ta;
   	}
    private boolean saveAS()
    {
        try
        {
            //Create a file chooser
            JFileChooser fc = new JFileChooser(".");
            fc.setFileFilter(new XMLFileFilter());
            //In response to a button click:
            int returnVal = fc.showSaveDialog(this);
            if(returnVal==JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if(!file.getAbsolutePath().endsWith(".xml"))
                {
                    file=new File(file.getAbsolutePath()+".xml");
                }
                org.jdom.Document document=checkCurrentFile();
                if(document==null)return false;
                config=null;//new XTTConfigurationLocalPermanent(currentFileName);
                file.createNewFile();
                this.currentFileName=xttgui.createShortFileName(file.getAbsolutePath());
                this.setTitle(currentFileName+" - Configuration Editor");
                writeFile(ta.getText());
                //loadFile(currentFileName);
                return true;
            }
        } catch (Exception ex)
        {
            String error="Error:\n";
            showSaveError(error+ex);
        }
        return false;
    }
    private void writeFile(String text) throws Exception
    {
        BufferedOutputStream out=new BufferedOutputStream(new FileOutputStream(currentFileName));
        byte[] data=ConvertLib.createBytes(text);
        XTTProperties.printDebug("Writing file size: "+data.length+" bytes name: "+currentFileName);
        out.write(data, 0, data.length);
        out.flush();
        out.close();
    }

    private org.jdom.Document checkCurrentFile() throws Exception
    {
        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
        org.jdom.Document document=parser.build(new StringReader(ta.getText()));
        if(!document.getRootElement().getName().toLowerCase().equals("configuration")&&!document.getRootElement().getName().toLowerCase().equals("remoteconfiguration"))
        {
            String error="The outer node in the configuration file isn't called Configuration or RemoteConfiguration";
            showSaveError(error);
            return null;
        }
        return document;
    }

    private void showSaveError(String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext+"\n\nConfiguration NOT saved!",
            "Configuration XML parse error",
            JOptionPane.ERROR_MESSAGE);
    }
    public void windowActivated(WindowEvent e){}
    public void windowClosed(WindowEvent e)
    {
        ta.setText("");
        //xttgui.doEnable();
    }
    public void windowClosing(WindowEvent e)
    {
        ta.setText("");
        //xttgui.doEnable();
    }
    public void windowDeactivated(WindowEvent e){}
    public void windowDeiconified(WindowEvent e){}
    public void windowIconified(WindowEvent e){}
    public void windowOpened(WindowEvent e){}
    public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
		if ((e.getKeyCode() == KeyEvent.VK_F) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))
		{
			ta.search();
		}
	}
}