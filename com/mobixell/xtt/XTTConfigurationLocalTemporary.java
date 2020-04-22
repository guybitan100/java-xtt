package com.mobixell.xtt;

import java.io.File;
import org.jdom.Document;
import org.jdom.Element;
import java.util.Iterator;
import java.util.Vector;

public class XTTConfigurationLocalTemporary extends XTTConfigurationLocal
{
    private static Vector<XTTConfigurationLocalTemporary> temporaryLocalConfigurations = new Vector<XTTConfigurationLocalTemporary>();

    public static int getNumberOfConfigurations()
    {
        return temporaryLocalConfigurations.size();    
    }
    
    public static Iterable<Document> getDocuments()
    {
        return new Iterable<Document>()
        {
            public Iterator<Document> iterator() 
            {
                return new Iterator<Document>()
                {                
                    final Iterator configs = temporaryLocalConfigurations.iterator();
                    public boolean hasNext() 
                    {
                        return configs.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public Document next()
                    {                    
                        XTTConfigurationLocalTemporary config = (XTTConfigurationLocalTemporary)configs.next();
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
    public static Iterable<XTTConfigurationLocalTemporary> getConfigurations()
    {
        return new Iterable<XTTConfigurationLocalTemporary>()
        {
            public Iterator<XTTConfigurationLocalTemporary> iterator() 
            {
                return new Iterator<XTTConfigurationLocalTemporary>()
                {                
                    final Iterator configs = temporaryLocalConfigurations.iterator();
                    public boolean hasNext() 
                    {
                        return configs.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public XTTConfigurationLocalTemporary next()
                    {                    
                        return (XTTConfigurationLocalTemporary) configs.next();
                    }
                    public void remove() 
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    
    /**
    * Removes the temp config stuff saved previously.
    *
    */
    public static void removeAll()
    {
        temporaryLocalConfigurations.clear();
    }
 
    public static Document getFirstDocument()
    {
        return temporaryLocalConfigurations.get(0).getDocument();
    } 
        
    private File configurationFile = null;
    private org.jdom.Document configurationDocument = null;
    private String name = "";
      
    public XTTConfigurationLocalTemporary(String filePath)
    {
        //Temporary files are loaded from the current test working directory.
        configurationFile = new File(filePath);
        if(!configurationFile.exists())
        {
            configurationFile = new File(XTTProperties.getCurrentTestPath()+filePath);
               
            if(!configurationFile.exists())
            {
                name = configurationFile.getName();
                XTTProperties.printFail("XTTConfiguration: Cannot find file: " + filePath);
                configurationFile = null;
            }
        }
    }
    
    public XTTConfigurationLocalTemporary()
    {
        configurationDocument = new Document(new Element("tempconfiguration"));
        temporaryLocalConfigurations.insertElementAt(this,0);
        XTTProperties.printDebug("loadTempConfiguration: Added blank config");
    }
 
    public boolean add()
    {
        //Don't try to load from a file if it's a generated file.
        if((configurationDocument!=null && configurationFile==null) || loadConfiguration())
        {
            //Grab the name from the config
            try
            {
                name = XTTXML.getElement("name",configurationDocument).getText();
            }
            catch(NullPointerException npe)
            {
                //Do nothing, no name found.    
            }
                        
            if(!temporaryLocalConfigurations.contains(this))
            {
                temporaryLocalConfigurations.add(0,this);
            }
            return initializeConfiguration();
        }
        return false;
    }  
    public boolean remove()
    {
        boolean status = temporaryLocalConfigurations.remove(this);
        super.remove();
        return status;
    }

    public Document getDocument()
    {
        return configurationDocument;
    }

    public static boolean updateConfigurations()
    {
        return true;
    }

    public String getName()
    {
        return name;    
    }    
    public String getFileName()
    {
        if(configurationFile!=null)
        {
            return configurationFile.getName();        
        }
        else
        {
            return null;    
        }
    }
    
    public void setDocument(Document document)
    {
        
    }
         
    /**
     * Loads a temporary configuration (one for each test) XML file at the start of a test.
     * <p>
     * This configuration is read before the main configuration,
     * so certain values can be set per test, and not overwrite the default.
     *
     * @param file the filename as a String of the file to be read in
     */    
    private boolean loadConfiguration()
    {
        if(configurationFile == null)
        {
            return false;    
        }        
        try
        {
            org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
            configurationDocument = parser.build(configurationFile);
            temporaryLocalConfigurations.insertElementAt(this,0);

            XTTProperties.printDebug("loadTempConfiguration: loading: " + configurationFile.getName());

            String configuration;
            if(!configurationDocument.getRootElement().getName().equalsIgnoreCase("configuration"))
            {
                XTTProperties.printFail("The outer node in the test configuration file isn't called configuration\n");
                return false;
            }

            String variables = XTTProperties.getProperty("variables");
            if (!variables.equals("null"))
            {
                XTTXML.setProperty("variables",null);
                XTTProperties.printWarn("'variables' node in your test configuration is reserved for Internal use only;\n     Information stored here will be deleted");
            }
        }
        catch (org.jdom.JDOMException jpe)
        {
            XTTProperties.printFail("Error while reading test configuration XML:\n" + jpe);
            return false;
        }
        catch (Exception e)
        {

            XTTProperties.printException(e);
            XTTProperties.printFail("Error while reading configuration XML");
            return false;
        }
        
        return true;
    }
    
    private boolean initializeConfiguration()
    {
        return true;    
    }   
}