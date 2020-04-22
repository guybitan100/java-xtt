package com.mobixell.xtt.gui;

import javax.swing.*;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.gui.main.XTTGui;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.awt.event.ActionEvent;

import java.io.File;


/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: SaveByteStringAsBinary.java,v 1.2 2008/09/18 13:40:13 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class SaveByteStringAsBinary extends JMenuItem implements ActionListener
{
    public static final String tantau_sccsid = "@(#)$Id: SaveByteStringAsBinary.java,v 1.2 2008/09/18 13:40:13 rsoder Exp $";

    private XTTGui xttgui       = null;
    
    private HashMap<Integer,JRadioButtonMenuItem> radios=new HashMap<Integer,JRadioButtonMenuItem>();
    
    JCheckBoxMenuItem external=null;
    JCheckBoxMenuItem transaction=null;

    public SaveByteStringAsBinary(XTTGui xttgui, String text)
    {
        super(text);
        this.xttgui=xttgui;
        this.addActionListener(this);
    }
 
    public void actionPerformed(ActionEvent e)
    {
        Object[] options = {"Save from ByteString","Save from Base64","Cancel"};
        JTextArea textarea=new JTextArea(20,40);
        JScrollPane scroll=new JScrollPane(textarea);
        Object[] message ={"Plese enter the encoded bytes",scroll};
        int n = JOptionPane.showOptionDialog(xttgui,
            message,
            "Save Encoded String as Binary",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]);
        if(n==0||n==1)
        {
            try
            {
                byte[] data=null;
                String values=null;
                values=XTTProperties.getVariable(textarea.getText());
                if(values==null|values.equals("null"))
                {
                    values=textarea.getText();
                }
                if(n==0)
                {
                    data=ConvertLib.getByteArrayFromHexString(values);
                } else
                {
                    data=ConvertLib.base64Decode(values);
                }
                File file=getExportFile();
                if(file!=null)
                {
                    XTTProperties.printDebug("Saving Binary: "+file.getCanonicalPath());
                    java.io.BufferedOutputStream out=new java.io.BufferedOutputStream(new java.io.FileOutputStream(file));
                    out.write(data, 0, data.length);
                    out.flush();
                    out.close();
                }
            } catch (Exception ex)
            {
                showError("Error writing file: "+ex.getMessage(),"Save binary error");
                XTTProperties.printDebugException(ex);
            }
        }
    }
    public void showError(String title,String errortext)
    {
        JOptionPane.showMessageDialog(xttgui,
            errortext,
            title,
            JOptionPane.ERROR_MESSAGE);
    }
    private File getExportFile()
    {
        JFileChooser fc = new JFileChooser(".");
        //fc.setFileFilter(new CSVFileFilter());
        //fc.setSelectedFile(new File("export.csv"));
        //In response to a button click:
        int returnVal = fc.showSaveDialog(xttgui);
        if(returnVal==fc.APPROVE_OPTION)
        {
            //fc.setSelectedFile(new File(message.getFile()));
            File file = fc.getSelectedFile();

            if(file.exists())
            {
                try
                {

                    Object[] options = {"Yes, overwrite","No, cancel"};
                    int n = JOptionPane.showOptionDialog(xttgui,
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
                        return null;
                    } else
                    {
                        return file;
                    }
                } catch (Exception ex)
                {
                    showError("Save Binary Error",ex.getClass().getName()+"\n"+ex.getMessage());
                }
            } else
            {
                return file;
            }
        }
        return null;
    }    
}
