package com.mobixell.xtt.gui;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: CSVFileFilter.java,v 1.1 2008/07/02 12:58:04 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class CSVFileFilter extends javax.swing.filechooser.FileFilter
{
    public static final String tantau_sccsid = "@(#)$Id: CSVFileFilter.java,v 1.1 2008/07/02 12:58:04 rsoder Exp $";
    public boolean accept(java.io.File f)
    {
        if(f!=null)
        {
            if(f.isDirectory())return true;
            if(f.getName().toLowerCase().endsWith(".csv"))return true;
        }
        return false;
    }
    public String getDescription()
    {
        return "CSV File Filter";
    }
}
