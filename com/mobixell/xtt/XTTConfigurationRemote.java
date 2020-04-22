package com.mobixell.xtt;

import java.util.Iterator;
import java.util.Vector;
import org.jdom.Document;

public class XTTConfigurationRemote extends XTTConfiguration
{
    private static Vector<XTTConfigurationRemote> remoteConfigurations = new Vector<XTTConfigurationRemote>();

    public static int getNumberOfConfigurations()
    {
        return remoteConfigurations.size();    
    }
    
    public static Iterable<Document> getDocuments()
    {
        return new Iterable<Document>()
        {
            public Iterator<Document> iterator() 
            {
                return new Iterator<Document>()
                {                
                    final Iterator configs = remoteConfigurations.iterator();
                    public boolean hasNext() 
                    {
                        return configs.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public Document next()
                    {                    
                        XTTConfigurationRemote config = (XTTConfigurationRemote)configs.next();
                        return config.getDocument();
                    }
                    public void remove() 
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    public static Iterable<XTTConfigurationRemote> getConfigurations()
    {
        return new Iterable<XTTConfigurationRemote>()
        {
            public Iterator<XTTConfigurationRemote> iterator() 
            {
                return new Iterator<XTTConfigurationRemote>()
                {                
                    final Iterator configs = remoteConfigurations.iterator();
                    public boolean hasNext() 
                    {
                        return configs.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public XTTConfigurationRemote next()
                    {                    
                        return (XTTConfigurationRemote) configs.next();
                    }
                    public void remove() 
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static Document getFirstDocument()
    {
        return remoteConfigurations.get(0).getDocument();
    }

    /**
    * Removes the remote config stuff saved previously.
    *
    */
    public static void removeAll()
    {
        remoteConfigurations.clear();
    }
    
    private org.jdom.Document configurationDocument = null;
    private String name = "";
    private String remoteFileName="";
    
 
    public boolean add()
    {
        
        if(loadConfiguration())
        {
            if(!remoteConfigurations.contains(this))
            {            
                remoteConfigurations.add(0,this);
            }
            return initializeConfiguration();
        }
        return false;
    } 
    public boolean remove()
    {
        super.remove();
        return remoteConfigurations.add(this);
    }

    public Document getDocument()
    {
        return configurationDocument;
    }
            
    private boolean loadConfiguration()
    {
        return true;    
    }
    
    private boolean initializeConfiguration()
    {
        return true;    
    }    

    //If we're using this class it can't change.
    public boolean needsUpdating()
    {
        return false;
    }    
    public void update()
    {
        //Do something here.
    }
    
    public String getName()
    {
        return name;    
    }   
    public String getFileName()
    {
        return null;
    }
    
    public void setDocument(Document document)
    {        
        throw new UnsupportedOperationException("Not yet implemented");     
    }        
}