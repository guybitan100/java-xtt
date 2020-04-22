package com.mobixell.xtt;

import java.io.File;

import org.jdom.Document;
import java.util.Iterator;
import java.util.Vector;

public class XTTConfigurationLocalPermanent extends XTTConfigurationLocal
{
    private static Vector<XTTConfigurationLocalPermanent> permanentLocalConfigurations = new Vector<XTTConfigurationLocalPermanent>();

    public static int getNumberOfConfigurations()
    {
        return permanentLocalConfigurations.size();
    }

    public static Iterable<Document> getDocuments()
    {
        return new Iterable<Document>()
        {
            public Iterator<Document> iterator()
            {
                return new Iterator<Document>()
                {
                    final Iterator configs = permanentLocalConfigurations.iterator();
                    public boolean hasNext()
                    {
                        return configs.hasNext();
                    }
                    @SuppressWarnings({"unchecked"})
                    public Document next()
                    {
                        XTTConfigurationLocalPermanent config = (XTTConfigurationLocalPermanent)configs.next();
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
    public static Iterable<XTTConfigurationLocalPermanent> getConfigurations()
    {
        return new Iterable<XTTConfigurationLocalPermanent>()
        {
            public Iterator<XTTConfigurationLocalPermanent> iterator()
            {
                return new Iterator<XTTConfigurationLocalPermanent>()
                {
                    final Iterator configs = permanentLocalConfigurations.iterator();
                    public boolean hasNext()
                    {
                        return configs.hasNext();
                    }

                    @SuppressWarnings({"unchecked"})
                    public XTTConfigurationLocalPermanent next()
                    {
                        return (XTTConfigurationLocalPermanent) configs.next();
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
        return permanentLocalConfigurations.get(0).getDocument();
    }

    /**
    * Removes the main config stuff saved previously.
    *
    */
    public static void removeAll()
    {
        permanentLocalConfigurations.clear();
    }

    /*
    Single configuration functions.
    */

    private File configurationFile = null;

    private org.jdom.Document configurationDocument = null;
    private String configurationDocumentMd5Digest = null;
    private long lastLoadTime = 0; //We haven't loaded the file yet.
    private String name = "";

    public XTTConfigurationLocalPermanent()
    {

    }
    public XTTConfigurationLocalPermanent(String filePath)
    {
        configurationFile = new File(filePath);
        if(!configurationFile.exists())
        {
            XTTProperties.printWarn("XTTConfiguration: Cannot find file: " + filePath);
            name = configurationFile.getName();
            configurationFile = null;
        }
    }

    public boolean add()
    {
        //Don't try to load the configuration from a file if it's a generated configuration
        if((configurationDocument!=null && configurationFile==null) || (loadConfiguration()))
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

            if(!permanentLocalConfigurations.contains(this))
            {
                permanentLocalConfigurations.add(0,this);
            }
            XTTProperties.initializeMainConfigurations();
            super.add();
            return true;
        }
        return false;
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
        try
        {
            java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
            String md5Digest = ConvertLib.getHexStringFromByteArray(md5Hash.digest(ConvertLib.createBytes(XTTXML.stringXML(document))));

            if(configurationDocumentMd5Digest.equals(md5Digest))
            {
                XTTProperties.printVerbose("XTTConfiguration: setDocument: No changes were detected.");
            }
            else
            {
                lastLoadTime = -1;
            }
            configurationDocument = document;
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }
    public void setDocument(String string)
    {
        try
        {
            Document document = XTTXML.readXMLFromString(string);

            if(document == null)
            {
                throw new NullPointerException("document not allowed to be null");
            }
            java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
            //Do the MD5 Digest on the Document, not just the String we already have, just incase.
            String md5Digest = ConvertLib.getHexStringFromByteArray(md5Hash.digest(ConvertLib.createBytes(XTTXML.stringXML(document))));

            if(md5Digest.equals(configurationDocumentMd5Digest))
            {
                XTTProperties.printVerbose("XTTConfiguration: setDocument: No changes were detected.");
            }
            else
            {
                lastLoadTime = -1;
            }
            configurationDocument = document;
        }
        catch(Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    public boolean remove()
    {
        boolean status = permanentLocalConfigurations.remove(this);
        XTTProperties.initializeMainConfigurations();
        super.remove();
        return status;
    }

    public Document getDocument()
    {
        if(configurationDocument == null)
        {
            loadConfiguration();
        }
        return configurationDocument;
    }

    public boolean needsUpdating()
    {
        //We don't have a file to read from, so we can't update.
        if(configurationFile==null)
        {
            return false;
        }

        //Does the file we should read from exist? We can't update.
        if(!configurationFile.exists())
        {
            return false;
        }

        //If the lastModified time we have stored isn't the same as the current lastModified it has changed.
        if(lastLoadTime < configurationFile.lastModified())
        {
            return true;
        }

        //String rootElement = XTTXML.loadMainConfiguration(config);
        return false;
    }

    public void update()
    {
        loadConfiguration();
        XTTProperties.initializeMainConfigurations();
    }

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

            lastLoadTime = configurationFile.lastModified();
            java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
            configurationDocumentMd5Digest = ConvertLib.getHexStringFromByteArray(md5Hash.digest(ConvertLib.createBytes(XTTXML.stringXML(configurationDocument))));
        }
        catch (org.jdom.input.JDOMParseException jpe)
        {
            String error="Configuration XML wasn't complete:\n" + jpe;
            if(XTTProperties.getXTTGui()==null)
            {
                System.err.println(error);
                System.exit(XTTProperties.FAILED_WITH_INVALID_CONFIG_VALUE);
            } else
            {
                XTTProperties.getXTTGui().showConfigurationError(error);
                return false;
            }
        }
        //This handles the case of the file not being found.
        catch (Exception e)
        {
            XTTProperties.printException(e);
            String error="Error while reading XML";
            if(XTTProperties.getXTTGui()==null)
            {
                System.err.println(error);
                System.exit(XTTProperties.FAILED_WITH_INVALID_CONFIG_VALUE);
            } else
            {
                XTTProperties.getXTTGui().showConfigurationError(error+"\n"+e);
                return false;
            }
        }
        if(configurationDocument.getRootElement().getName().equalsIgnoreCase("configuration"))
        {
            //Do nothing, it's just nicer to check this way instead of with negatives.
        }
        else if (configurationDocument.getRootElement().getName().equalsIgnoreCase("remoteconfiguration"))
        {
            //Do nothing, it's just nicer to check this way instead of with negatives.
        }
        else
        {
            String error="Wrong root node '" + configurationDocument.getRootElement().getName() + "'";
            if(XTTProperties.getXTTGui()==null)
            {
                System.err.println(error);
                System.exit(XTTProperties.FAILED_WITH_INVALID_CONFIG_VALUE);
            } else
            {
                XTTProperties.getXTTGui().showConfigurationError(error);
                return false;
            }
            return false;
        }

        return true;
    }
}