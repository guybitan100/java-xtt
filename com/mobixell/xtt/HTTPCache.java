package com.mobixell.xtt;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

/**
 * <p>HTTPCache</p>
 * <p>Stores files in memory and makes sure the newer of either memory or disk is returned</p>
 * <p>Copyright: Copyright (c) 2009</p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Roger Soder
 * @version $Id: HTTPCache.java,v 1.3 2009/02/13 13:35:11 rsoder Exp $
 */
public class HTTPCache
{
    public static final String tantau_sccsid = "@(#)$Id: HTTPCache.java,v 1.3 2009/02/13 13:35:11 rsoder Exp $";
    private static final int MAXCACHESIZE=1000000;
    private HashMap<String,CacheItem> fileCache = new HashMap<String,CacheItem>();
    //private HashMap<String,ByteArrayWrapper> fileCache = new HashMap<String,ByteArrayWrapper>();
    //private HashMap<String,Long> fileDate = new HashMap<String,Long>();
    String defaultFile=null;
    
    private class CacheItem
    {
        public long lastModified=-1;
        public byte[] bytes=null;
        public CacheItem(byte[] bytes, long date)
        {
            this.bytes=bytes;
            this.lastModified=date;
        }
        public String toString()
        {
            return "(bytes="+bytes.length+", date="+lastModified+")";
        }
    }
    
    /**
     * Creates a new HTTPCache
     * @param defaultFile     default file name to use on directories. example: index.html
     */
    public HTTPCache(String defaultFile)
    {
        this.defaultFile=defaultFile;
    }

    /**
     * Returns a bytearray with the content of the file or null if no file found.
     * @param output    output to print inf front of EMPTY,CACHE and DISK, null if no output
     * @param root      the root directory of the disk where files come from
     * @param fname     the filename relative to the root directory, written in unix style with /
     */
    public byte[] getFile(String output, File root, String fname) throws java.io.IOException
    {
        return getFile(output,root,fname,"");
    }
    /**
     * Returns a bytearray with the content of the file or null if no file found.
     * @param output        output to print inf front of EMPTY,CACHE and DISK, null if no output
     * @param root          the root directory of the disk where files come from
     * @param fname         the filename relative to the root directory, written in unix style with /
     * @param directory     additional directory between the rootDirectory and the actual files 
     */
    public byte[] getFile(String output, File root, String fname, String directory) throws java.io.IOException
    {
        String fileName=getFileName(fname,directory);
        byte[] returnBody=null;
        CacheItem item=getItem(fileName);
        long lastModified=-1;
        if(item!=null)lastModified=item.lastModified;
        File returnFile=getFile(root,fileName);
        if(returnFile.exists())
        {
            if(returnFile.lastModified()>lastModified)
            {
                if(output!=null)XTTProperties.printDebug(output+"DISK");
                returnBody=new byte[(int)returnFile.length()];
                FileInputStream is=null;
                try
                {
                	is = new FileInputStream(returnFile.getAbsolutePath());   
                    HTTPHelper.readBytes(is,returnBody);
                } finally
                {
                    is.close();
                }
                if(returnBody.length<MAXCACHESIZE)
                {
                    put(fname,returnBody,directory,new Long(returnFile.lastModified()));
                }
                return returnBody;
            }
        }
        if(item==null)
        {
            returnBody=null;
            if(output!=null)XTTProperties.printDebug(output+"EMPTY");
        } else
        {
            returnBody=item.bytes;
            if(output!=null)XTTProperties.printDebug(output+"CACHE");
        }
        return returnBody;
    }

    /**
     * Returns the size of a file or -1 if no file found
     * @param root          the root directory of the disk where files come from
     * @param fname         the filename relative to the root directory, written in unix style with /
     */
    public int getFileSize(File root, String fname)
    {
        return getFileSize(root,fname,"");
    }
    /**
     * Returns the size of a file or -1 if no file found
     * @param root          the root directory of the disk where files come from
     * @param fname         the filename relative to the root directory, written in unix style with /
     * @param directory     additional directory between the rootDirectory and the actual files 
     */
    public int getFileSize(File root, String fname, String directory)
    {
        String fileName=getFileName(fname,directory);
        CacheItem item=getItem(fileName);
        long lastModified=-1;
        if(item!=null)lastModified=item.lastModified;
        File returnFile=getFile(root,fileName);
        if(returnFile.exists())
        {
            if(returnFile.lastModified()>lastModified)
            {
                return (int)returnFile.length();
            }
        }
        if(item==null)
        {
            return -1;
        } else
        {
            return item.bytes.length;
        }
    }
    /**
     * Returns the last modifed date in miliseconds since 1st January 1970 or -1 if no file found.
     * @param root          the root directory of the disk where files come from
     * @param fname         the filename relative to the root directory, written in unix style with /
     */
    public long getLastModified(File root, String fname)
    {
        return getLastModified(root,fname,"");
    }
    /**
     * Returns the last modifed date in miliseconds since 1st January 1970 or -1 if no file found.
     * @param root          the root directory of the disk where files come from
     * @param fname         the filename relative to the root directory, written in unix style with /
     * @param directory     additional directory between the rootDirectory and the actual files 
     */
    public long getLastModified(File root, String fname, String directory)
    {
        String fileName=getFileName(fname,directory);
        CacheItem item=getItem(fileName);
        long lastModified=-1;
        if(item!=null)lastModified=item.lastModified;
        File returnFile=getFile(root,fileName);
        if(returnFile.exists())
        {
            if(returnFile.lastModified()>lastModified)
            {
                return (int)returnFile.lastModified();
            }
        }
        if(item==null)
        {
            return -1;
        } else
        {
            return item.lastModified;
        }
    }    
    
    private String getFileName(String fname, String directory)
    {
        if(!directory.equals("")&&!directory.endsWith("/")){directory=directory+"/";}
        String fileName=directory+fname;
        fileName=fileName.replace('/',File.separatorChar);
        return fileName;
    }
    
    private File getFile(File root, String fileName)
    {
        File returnFile=new File(root.getAbsolutePath()+File.separator+fileName);
        if(returnFile.isDirectory())returnFile=new File(root.getAbsolutePath()+File.separator+fileName+File.separator+defaultFile);
        return returnFile;
    }
    
    private CacheItem getItem(String fileName)
    {
        CacheItem item=null;
        synchronized(fileCache)
        {
            if(fileName.endsWith(File.separator))
            {
                item=fileCache.get(fileName+defaultFile);
            }
            if(item==null)
            {
                item=fileCache.get(fileName);
            }
        }
        return item;
    }
    
    /**
     * Store a file in the cache, last modifed date is the time of storage.
     * @param fname         the filename relative to the root directory, written in unix style with /
     * @param data          the byte[] of data
     * @param directory     additional directory between the rootDirectory and the actual files 
     * @param date          the last modified date 
     */
    private void put(String fname,byte[] data, String directory, Long date)
    {
        String fileName=getFileName(fname,directory);
        synchronized(fileCache)
        {
            fileCache.put(fileName,new CacheItem(data,date));
        }
    }
    /**
     * Store a file in the cache, last modifed date is the time of storage.
     * @param fname         the filename relative to the root directory, written in unix style with /
     */
    public void put(String fname,byte[] data)
    {
        put(fname,data,"");
    }
    /**
     * Store a file in the cache, last modifed date is the time of storage.
     * @param fname         the filename relative to the root directory, written in unix style with /
     * @param data          the byte[] of data
     * @param directory     additional directory between the rootDirectory and the actual files 
     */
    public void put(String fname,byte[] data,String directory)
    {
        put(fname,data,directory,new Long(System.currentTimeMillis()));
    }
    /**
     * Clear all files out of the cache
     */
    public void clear()
    {
        synchronized(fileCache)
        {
            fileCache.clear();
        }
    }
    public String toString()
    {
        synchronized(fileCache)
        {
            return fileCache.toString();
        }
    }
}