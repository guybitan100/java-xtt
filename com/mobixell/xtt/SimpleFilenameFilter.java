package com.mobixell.xtt;

import java.io.File;

/**
 * A simple Filter on a filename. Used to filter directory listings in the <code>list</code> method of class <code>File</code>, and by the Abstract Window Toolkit's file dialog component.
 * <p>
 * This is a simple indexOf match of the pattern on the filename.
 */
public class SimpleFilenameFilter implements java.io.FilenameFilter
{
    String pattern=null; //post or pre string.
    String extension=null; //post string.

    public SimpleFilenameFilter(String pattern)
    {
        this.pattern = pattern;
        this.extension = "";
    }

    public SimpleFilenameFilter(String pattern, String extension)
    {
        this.pattern = pattern;
        this.extension = extension;
    }
    
    public boolean accept(File dir, String name)
    {
        return ((name.indexOf(pattern) != -1) && (name.endsWith(extension)));
    }
    public static final String tantau_sccsid = "@(#)$Id: SimpleFilenameFilter.java,v 1.3 2008/02/18 12:36:26 gcattell Exp $";
}