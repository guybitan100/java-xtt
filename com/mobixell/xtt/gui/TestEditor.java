package com.mobixell.xtt.gui;

import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTTTest;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream ;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;


/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: TestEditor.java,v 1.7 2008/03/17 14:01:41 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class TestEditor extends JFrame implements WindowListener,ActionListener
{
	private static final long serialVersionUID = 1L;
	public static final String tantau_sccsid = "@(#)$Id: TestEditor.java,v 1.7 2008/03/17 14:01:41 rsoder Exp $";

    private XTTGui xttgui          = null;
    private XTTTest testno         = null;
    private JEditTextArea ta       = new JEditTextArea();
    private String currentFileName = "";
    private String oldfilename     = "";

    public TestEditor(XTTGui xttgui,String fileName,XTTTest testno)
    {
        this.setTitle("Test Editor");
        this.addWindowListener(this);
        this.xttgui=xttgui;
        this.testno=testno;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initMenu();
        oldfilename=fileName;
        loadFile(fileName);
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
            if(contentS.equals(""))contentS="<test>\n</test>";
            ta.setText(contentS);
            ta.setCaretPosition(0);
            ta.scrollToCaret();
            this.currentFileName=filename;
            this.setTitle(currentFileName+" - Test Editor");
        } catch (Exception e)
        {
            xttgui.showConfigurationError(e.getMessage());
        }
    }
public void initMenu ()
{
	 JMenuBar menuBar=new JMenuBar();
     menuBar.setBackground(new Color(0xf6, 0xf6, 0xf6));
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

     menuItem=new JMenuItem("Save, Set & Exit");
     menuItem.setActionCommand("SET");
     filemenu.add(menuItem);
     menuItem.addActionListener(this);

     menuItem=new JMenuItem("Cancel & Exit");
     menuItem.setActionCommand("EXIT");
     filemenu.add(menuItem);
     menuItem.addActionListener(this);

     JPopupMenu popup = new JPopupMenu();

     menuItem=null;

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

     this.getContentPane().setLayout(new BorderLayout());
     this.getContentPane().add(ta,BorderLayout.CENTER);
     this.pack();
     this.setLocationRelativeTo(null); //center it
     this.setVisible(true);
}
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("EXIT"))
        {
            currentFileName="";
            this.dispose();
        } else if(e.getActionCommand().equals("SET"))
        {
            try
            {
                if(checkCurrentFile())return;
                writeFile(ta.getText());
                testno.setFileName(this.currentFileName);
                if(!this.oldfilename.equals(this.currentFileName))
                {
                    xttgui.setListStatus(XTTGui.LIST_EDITED);
                }
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
                if(checkCurrentFile())return;
                writeFile(ta.getText());
            } catch (Exception ex)
            {
                String error="Error:\n";
                showSaveError(error+ex);
            }
        } else if(e.getActionCommand().equals("SAVEAS"))
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
                    if(checkCurrentFile())return;
                    if(!file.getAbsolutePath().endsWith(".xml"))
                    {
                        file=new File(file.getAbsolutePath()+".xml");
                    }
                    file.createNewFile();
                    this.currentFileName=file.getAbsolutePath();
                    this.setTitle(currentFileName+" - Test Editor");
                    writeFile(ta.getText());
                    //loadFile(file.getAbsolutePath());
                }
            } catch (Exception ex)
            {
                String error="Error:\n";
                showSaveError(error+ex);
            }
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
    }
    public String getCurrentFile()
    {
        return this.currentFileName;
    }

    private boolean checkCurrentFile() throws Exception
    {
        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
        org.jdom.Document document=parser.build(new StringReader(ta.getText()));
        if(!document.getRootElement().getName().toLowerCase().equals("test"))
        {
            String error="The outer node in the test file isn't called test";
            showSaveError(error);
            return true;
        }
        return false;
    }

    private void showSaveError(String errortext)
    {
        JOptionPane.showMessageDialog(this,
            errortext+"\n\nTest NOT saved!",
            "Test XML parse error",
            JOptionPane.ERROR_MESSAGE);
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
}