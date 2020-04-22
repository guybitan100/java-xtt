package com.mobixell.xtt;

import org.jdom.Document;
import java.util.Iterator;

public abstract class XTTConfiguration
{
    private org.jdom.Document configurationDocument = null;

    public static int getNumberofPermanentLocalConfigurations()
    {
        return XTTConfigurationLocalPermanent.getNumberOfConfigurations();
    }
    public static int getNumberofTemporaryLocalConfigurations()
    {
        return XTTConfigurationLocalTemporary.getNumberOfConfigurations();
    }
    public static int getNumberofRemoteConfigurations()
    {
        return XTTConfigurationRemote.getNumberOfConfigurations();
    }


    /**
    * Removes the permanent config stuff saved previously.
    *
    */
    public static void removePermanentLocalConfigurations()
    {
        XTTConfigurationLocalTemporary.removeAll();
    }
    /**
    * Removes the temp config stuff saved previously.
    *
    */
    public static void removeTemporaryLocalConfigurations()
    {
        XTTConfigurationLocalTemporary.removeAll();
    }
    /**
    * Removes the remote config stuff saved previously.
    *
    */
    public static void removeRemoteConfigurations()
    {
        XTTConfigurationRemote.removeAll();
    }

    //Nice code to iterate around all the configs. Easy to use in the enhanced for loop.
    public static Iterable<XTTConfiguration> getAllConfigurations()
    {
        return new Iterable<XTTConfiguration>()
        {
            public Iterator<XTTConfiguration> iterator()
            {
                return new Iterator<XTTConfiguration>()
                {
                    final Iterator permanentLocalConfigs = XTTConfigurationLocalPermanent.getConfigurations().iterator();
                    final Iterator temoporaryLocalConfigs = XTTConfigurationLocalTemporary.getConfigurations().iterator();
                    final Iterator remoteConfigs = XTTConfigurationRemote.getConfigurations().iterator();
                    public boolean hasNext()
                    {
                        return (permanentLocalConfigs.hasNext() || temoporaryLocalConfigs.hasNext() || remoteConfigs.hasNext());
                    }
                    @SuppressWarnings({"unchecked"})
                    public XTTConfiguration next()
                    {
                        XTTConfiguration config = null;
                        try
                        {
                            return (XTTConfiguration) temoporaryLocalConfigs.next();
                        }
                        catch(java.util.NoSuchElementException nse){}
                        try
                        {
                            return (XTTConfiguration) remoteConfigs.next();
                        }
                        catch(java.util.NoSuchElementException nse){}
                        return (XTTConfiguration) permanentLocalConfigs.next();
                    }
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    //Nice code to iterate around all the configs. Easy to use in the enhanced for loop.
    public static Iterable<Document> getAllDocuments()
    {
        return new Iterable<Document>()
        {
            public Iterator<Document> iterator()
            {
                return new Iterator<Document>()
                {
                    final Iterator permanentLocalConfigs = XTTConfigurationLocalPermanent.getConfigurations().iterator();
                    final Iterator temoporaryLocalConfigs = XTTConfigurationLocalTemporary.getConfigurations().iterator();
                    final Iterator remoteConfigs = XTTConfigurationRemote.getConfigurations().iterator();
                    public boolean hasNext()
                    {
                        return (permanentLocalConfigs.hasNext() || temoporaryLocalConfigs.hasNext() || remoteConfigs.hasNext());
                    }
                    @SuppressWarnings({"unchecked"})
                    public Document next()
                    {
                        XTTConfiguration config = null;
                        try
                        {
                            config = (XTTConfiguration) temoporaryLocalConfigs.next();
                            return config.getDocument();
                        }
                        catch(java.util.NoSuchElementException nse){}
                        try
                        {
                            config = (XTTConfiguration) remoteConfigs.next();
                            return config.getDocument();
                        }
                        catch(java.util.NoSuchElementException nse){}
                        try
                        {
                            config = (XTTConfiguration) permanentLocalConfigs.next();
                            return config.getDocument();
                        }
                        catch(java.util.NoSuchElementException nse){}
                        throw new java.util.NoSuchElementException();
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
     * Returns true is any of the local permanent configurations need updating.
     *
     */
    public static boolean doAnyNeedUpdating()
    {
        //We only need to check the permanent local configs. Nothing else can be updated (possibly Remote configs, but we'll deal with that another time)
        for(XTTConfigurationLocalPermanent config:XTTConfigurationLocalPermanent.getConfigurations())
        {
            if(config.needsUpdating())
            {
                return true;
            }
        }
        return false;
    }

    public static void updateAll()
    {
        XTTConfigurationLocalTemporary.removeAll();
        for(XTTConfiguration config : getAllConfigurations())
        {
            config.update();
        }
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().refreshConfigurations();
        }
    }

    public static void dumpConfiguration()
    {
        //TODO
        throw new java.lang.UnsupportedOperationException("TODO, Not implemented");
    }

    public static void dumpConfiguration(String mainConfigFilenamePrefix, String tempConfigFilenamePrefix)
    {
        //TODO
        throw new java.lang.UnsupportedOperationException("TODO, Not implemented");
    }

    public abstract Document getDocument();
    public abstract String getFileName();
    public abstract String getName();
    public abstract void setDocument(Document document);
    public boolean add()
    {
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().refreshConfigurations();
        }
        return true;            
    }

    public boolean remove()
    {
        if(XTTProperties.getXTTGui()!=null)
        {
            XTTProperties.getXTTGui().refreshConfigurations();
        }
        return true;
    }

    //If we're using this class it can't change.
    public boolean needsUpdating()
    {
        return false;
    }

    public void update()
    {
        //Just a stub.
    }

    /*
    TODO: Rework this
    public static void dumpConfiguration()
    {
        dumpConfiguration("dumpedMainConfig","dumpedTempConfig");
    }

    public static void dumpConfiguration(String mainConfigFilenamePrefix, String tempConfigFilenamePrefix)
    {
        BufferedWriter fileOut = null;

        XMLOutputter outputter;
        try
        {
            if(XTTProperties.getLogDirectory() != null)
            {
                mainConfigFilenamePrefix = XTTProperties.getLogDirectory() + File.separator + mainConfigFilenamePrefix;
                tempConfigFilenamePrefix = XTTProperties.getLogDirectory() + File.separator + tempConfigFilenamePrefix;
            }

            outputter = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

            for (int i=0;i<mainConfigurations.size();i++)
            {
                fileOut = new BufferedWriter(new FileWriter( mainConfigFilenamePrefix + i + ".xml"));
                outputter.output(mainConfigurations.get(i),fileOut);
                fileOut.flush();
                fileOut.close();
            }
            for (int i=0;i<tempConfigurations.size();i++)
            {
                fileOut = new BufferedWriter(new FileWriter(tempConfigFilenamePrefix + i + ".xml"));
                outputter.output(tempConfigurations.get(i),fileOut);
                fileOut.flush();
                fileOut.close();
            }
        }
        catch (IOException ioe)
        {
            XTTProperties.printFail("dumpConfiguration: Error writing file");
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }*/
}