package com.mobixell.xtt.gui;

/**
 *
 *
 * @author      Roger Soder
 * @version     $Id: XMLFileFilter.java,v 1.2 2006/07/21 17:04:31 cvsbuild Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class XMLFileFilter extends javax.swing.filechooser.FileFilter
{
    public static final String tantau_sccsid = "@(#)$Id: XMLFileFilter.java,v 1.2 2006/07/21 17:04:31 cvsbuild Exp $";
    public boolean accept(java.io.File f)
    {
        if(f!=null)
        {
            if(f.isDirectory())return true;
            if(f.getName().toLowerCase().endsWith(".xml"))return true;
        }
        return false;
    }
    public String getDescription()
    {
        return "XML File Filter";
    }
}
