package com.mobixell.xtt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.OutputStream;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
public class XTTXML
{
    public static final String tantau_sccsid = "@(#)$Id: XTTXML.java,v 1.27 2008/11/24 15:37:36 gcattell Exp $";

    /**
     * Returns an Format for XMLOutputter.
     * This should be used to avoid having to change multiple XML output functions when you want a format change.
     */
    private static Format getXmlFormat()
    {
        Format prettyFormat = org.jdom.output.Format.getPrettyFormat();
        prettyFormat.setTextMode(org.jdom.output.Format.TextMode.TRIM_FULL_WHITE);
        prettyFormat.setOmitEncoding(true);

        return prettyFormat;
    }
    public static void writeXML(Document xml,String fileName)
    {
        BufferedWriter fout;

        try
        {
            if(XTTProperties.getLogDirectory() != null)
            {
                fileName = XTTProperties.getLogDirectory() + File.separator + fileName;
            }
            fout = new BufferedWriter(new FileWriter(fileName));

            streamXML(xml,fout);

            fout.flush();
            fout.close();
        }
        catch (IOException ioe)
        {
            XTTProperties.printFail("writeXML: Error writing file");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ioe);
            }
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    public static void streamXML(Document xml, Writer stream)
    {
        XMLOutputter outputter;
        try
        {
            outputter = new XMLOutputter(getXmlFormat());

            if (xml != null)
            {
                outputter.output(xml,stream);
                stream.flush();
            }
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }
    public static void streamXML(Document xml, OutputStream stream)
    {
        XMLOutputter outputter;
        try
        {
            outputter = new XMLOutputter(getXmlFormat());

            if (xml != null)
            {
                outputter.output(xml,stream);
                stream.flush();
            }
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    public static String stringXML(Document xml)
    {
        XMLOutputter outputter;
        try
        {
            outputter = new XMLOutputter(getXmlFormat());

            if (xml != null)
            {
                return outputter.outputString(xml);
            }
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        return null;
    }

    /**
    * Sets a property in the DOM tree. This property is only valid for one test, and will be removed on the next test.
    * Several actions can take place in this function.
    * <p>
    * 1) If the name argument doesn't exist, and new item is created with the text in content argument.
    * 2) If the name argument does exist, then the value it currently stores is changed to content argument.
    * 3) Lastly if content argument is null or of length zero, the name argument is removed from the DOM tree.
    * The name argument can point to subnodes of nodes by adding the '/' character
    * This specifies the tree to search down, it can be as long as you want.
    *
    * @param name The node to set
    * @param content The content of the node to set
    */
    public static void setProperty(String name, String content)
    {
        //Check there's a main config, if we're running from XTT and not a stand alone feature
        if(XTTConfiguration.getNumberofPermanentLocalConfigurations() == 0)
        {
            XTTProperties.printWarn("setProperty: No main config loaded");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        if(XTTConfiguration.getNumberofTemporaryLocalConfigurations() == 0)
        {
            XTTConfigurationLocalTemporary blankTempConfig = new XTTConfigurationLocalTemporary();
            blankTempConfig.add();
        }
        setProperty(name, content, XTTConfigurationLocalTemporary.getFirstDocument());
    }

    /**
    * Like <code>setProperty</code> but sets a permantent property, valid until XTT is shutdown.
    * Avoid using this unless you are setting system wide properties.
    *
    */
    protected static void setGlobalProperty(String name, String content)
    {
        if(XTTConfiguration.getNumberofPermanentLocalConfigurations() > 0)
        {
            setProperty(name, content, XTTConfigurationLocalPermanent.getFirstDocument());
        }
        else
        {
            XTTProperties.printFail("setGlobalProperty: no main configs loaded");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    private static void setProperty(String name, String content, Document doc)
    {
        if(doc == null)
        {
            XTTProperties.printWarn("setProperty: Config doesn't exist");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }

        if( (name.startsWith("/")) || (name.endsWith("/")) )
        {
            name = name.substring(1,name.length());
            XTTProperties.printFail("getElements: " + name + " has a leading or trailing / this isn't allowed");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }

        //name = name.replaceAll("[^0-9a-zA-Z/]","");
        Element newElement = new Element("null");

        //Remove all the characters we don't want
        if((content != null) && (content.equals("null")))
        {
            XTTProperties.printFail("This will confuse me, don't store 'null' as a value");
            return;
        }

        String[] nameParts = name.split("/");
        Element parent = doc.getRootElement();
        Element child = null;
        int slashIndex = name.indexOf("/");

        // If you need to remove the node do it here
        if(content == null)
        {
            //System.err.println("removing");
            slashIndex = name.lastIndexOf("/");
            if ( (slashIndex == -1)&& (removeChild(name,parent)) )
            {
                //XTTProperties.XTTProperties.printInfo("setProperty: " + name + " was succesfully removed");
            }
            else if (slashIndex != -1)
            {
                parent = getElement(name.substring(0,slashIndex),doc);
                if((parent != null)&&(removeChild(name.substring(slashIndex+1,name.length()),parent)) )
                {
                    //XTTProperties.XTTProperties.printInfo("setProperty: " + name + " was succesfully removed");
                }
                else
                {
                    XTTProperties.printFail("setProperty: Error removing node " + name);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }

            }
            else
            {
                //XTTProperties.printDebug("setProperty: Error removing node " + name);
                //setTestStatus(XTTProperties.FAILED);
                return;
            }
        }
        else
        {
            for(int i=0;i<nameParts.length;i++)
            {
                if(slashIndex != -1)
                {
                    child = getElement(name.substring(0,slashIndex));
                }
                else
                {
                    child = getElement(name);
                }

                if(child==null)
                {
                    try
                    {
                        newElement = new Element(encapsulateName(nameParts[i]));
                    }
                    catch(org.jdom.IllegalNameException ine)
                    {
                        XTTProperties.printFail("setProperty: Illegal name '" +  encapsulateName(nameParts[i]) + "'");
                        return;
                    }

                    if(slashIndex == -1)
                    {
                        newElement.setText(content);
                        parent.addContent(newElement);
                        //XTTProperties.XTTProperties.printInfo("setProperty: " + name +" was succesfully added");
                        return;
                    }
                    else
                    {
                        parent.addContent(newElement);
                        parent = newElement;
                    }
                }
                else if (slashIndex == -1)
                {
                    //XTTProperties.XTTProperties.printInfo("setProperty: " + name +" was succesfully edited");
                    child.setText(content);
                }
                if (child != null)
                {
                    parent = child;
                }
                slashIndex = name.indexOf("/",slashIndex+1);
            }
        }
     }

    private static String encapsulateName(String name)
    {
        if(name.matches(".*\\[([0-9]+|first\\(\\)|last\\(\\))\\]$"))
        {
            name = name.replaceAll("\\[([0-9]+|first\\(\\)|last\\(\\))\\]$","");
            //XTTProperties.printDebug("encapsulateName: '" + name + "' contains a index, new name '" + newName + "'");
        }
        if(name.substring(0,1).matches("[0-9]"))
        {
            return "xttnumbernameextension"+name;
        } else
        {
            return name;
        }
    }

    /**
    * Returns the first <code>Element</code> matching <code>name</code>. The node is the first match, in the latest configuration loaded.
    *
    * @param name The path of the nodes wanted. Uses a pseudo xpath match (see {@link #getProperty}).
    */
    protected static Element getElement(String name)
    {
        Element[] elements = getElements(name);
        if((elements == null) || (elements.length == 0))
        {
            return null;
        }
        else
        {
            return elements[0];
        }
    }

    /**
    * Returns an array of type <code>Element</code> containing all the nodes matching <code>name</code>. The nodes are only selected from the last loaded configuration.
    *
    * @param name The path of the nodes wanted. Uses a pseudo xpath match (see {@link #getProperty}).
    */
    protected static Element[] getElements(String name)
    {
        Element[] elements = null;
        for (Document config: XTTConfiguration.getAllDocuments())
        {
            elements = getElements(name,config);
            if((elements != null) && (elements.length > 0))
            {
                return elements;
            }
        }
        return elements;
    }

    /**
    * Returns an <code>Element</code> matching the path given by name, in the <code>Document</code> <code>doc</code>
    *
    * @param name The path of the nodes wanted. Uses a pseudo xpath match (see {@link #getProperty}).
    * @param doc The <code>Docuement</code> in which to search.
    */
    public static Element getElement(String name, Document doc)
    {
        Element[] elements = getElements(name,doc);
        if((elements == null) || (elements.length == 0))
        {
            return null;
        }
        else
        {
            return elements[0];
        }
    }

    /**
    * Returns an array of type <code>Element</code> containing all the nodes matching <code>name</code> in the <code>Document</code> <code>doc</code>
    *
    * @param name The path of the nodes wanted. Uses a pseudo xpath match (see {@link #getProperty}).
    * @param doc The <code>Docuement</code> in which to search.
    */
    public static Element[] getElements(String name, Document doc)
    {
        //If you have no XML Document to test against, just stop
        if(doc == null)
        {
            return new Element[0];
        }

        Element root = null;
        Element[] elements = null;

        root = doc.getRootElement(); //Find the root element first
        elements = evaluatePseudoXPath(name,root);
        return elements;
    }

    private static Element[] evaluatePseudoXPath(String pseudoXPath, Element node)
    {
        //Since I've enchanced the XPath to be more like the real XPath we'll be nice and add a /
        if(!pseudoXPath.startsWith("/"))
        {
            pseudoXPath = "/" + pseudoXPath;
        }

        pseudoXPath = pseudoXPath.replaceAll("//","/@SKIP@/");
        //pseudoXPath = pseudoXPath.replaceAll("[^0-9a-zA-Z/@\\.-+=#%]",""); //Replace all non-allowed XML characters, leave only numbers and letters and the /, @, and .
        String[] nameParts = pseudoXPath.split("/"); //Split the who path into nodes
        Vector<Element> result = findNode(nameParts,1,node);
        if(result!=null)
            return result.toArray(new Element[0]);
        else
            return null;
    }

    private static Vector<Element> findNode(String[] pseudoXPath, int currentPart, Element node)
    {
        if(currentPart >= pseudoXPath.length)
        {
            return null;
        }
        Vector<Element> children = new Vector<Element>();

        //If the current part isn't // get the children with the correct current name
        if(!pseudoXPath[currentPart].equals("@SKIP@"))
        {
            children = getChildren(encapsulateName(pseudoXPath[currentPart]),node);
            //Do a check here for position, e.g. this[1]

            //There were no children with that name, so return null
            if(children.size() == 0)
            {
                return null;
            }
            //We're at the end of the path, so return this list, since it's the children we want.
            else if(currentPart == pseudoXPath.length-1)
            {
                return children;
            }
            //We're not at the end yet, so look for the next child under these children.
            else
            {
                Vector<Element> found=new Vector<Element>();
                for(Element child : children)
                {
                    Vector<Element> result = findNode(pseudoXPath,currentPart+1,child);
                    if(result!=null && result.size() != 0)
                    {
                        //Do a check here for position, e.g. this[1]
                        found.addAll(result);
                    }
                }
                return found;
            }
        }
        /*This is a // so find the next node down with the next part name.
          We have to do two things here:
            1)if we're at the end of the path after the // add the matching children and return them
            2)if we're not at the end, take the matching children and continue to match underneath
        */
        else
        {
            Element temp = null;
            children = new Vector<Element>();
            //Loop round ALL the descendants until we grab one that's the correct name of the next XPath part, after the //
            for(java.util.Iterator i = node.getDescendants(); i.hasNext(); )
            {
                //Since we're ClassCasting ignore anything that's not an ELEMENT
                try
                {
                    temp = (Element)i.next();
                    //Match the name of the Element against the next XPath part.
                    if(temp.getName().equalsIgnoreCase(encapsulateName(pseudoXPath[currentPart+1])))
                    {
                        //The next part after the // is the end of the path, so return this list.
                        if(currentPart == pseudoXPath.length-2)
                        {
                            children.add(temp);
                        }
                        //We're not at the end, so continue down the list
                        else
                        {
                            return  findNode(pseudoXPath,currentPart+2,temp);
                        }
                    }
                }
                //We get this if the obj isn't an Element
                catch(ClassCastException cce)
                {
                    //
                }
                catch(Exception e)
                {
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(e);
                    }
                }
            }
            //Children is only set when we're at the end of the path, so we need to return all the ones we found.
            if(children.size() > 0)
            {
                //Do a check here for position, this is more tricky since the # is relative to each parent...
                return children;
            }
        }
        return null;
    }

    /*private Vector<Element> checkContextPosition(Vector<Element> nodes, String name)
    {
        HashMap<Element,Integer> parentCheck = new HashMap<Element,Integer>();

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(".*\\[([0-9]+|first\\(\\)|last\\(\\))\\]$");
        java.util.regex.Matcher m = p.matcher(name);
        String position = null;
        if(m.matches())
        {
            position = m.group(1);
        }
        Integer count = null;
        for(Element node:nodes)
        {
            count = parentCheck.get(node.getParentElement());
            if(count == null)
                count = 0;
            parentCheck.put(node.getParentElement), ++count);
        }
    }*/

    /**
    * Returns the first <code>Element</code> matching <code>name</code>. The node is the first match, in the latest configuration loaded.
    * The <code>getElement<code> function should be used instead where possible, since the real xPath is case-sensitive.
    *
    * @param name The path of the nodes wanted. Uses a real xpath match.
    */
    protected static Element getElementViaXPath(String name)
    {
        Element[] elements = getElementsViaXPath(name);
        if((elements == null) || (elements.length == 0))
        {
            return null;
        }
        else
        {
            return elements[0];
        }
    }

    /**
    * Returns an array of type <code>Element</code> containing all the nodes matching <code>name</code>. The nodes are only selected from the last loaded configuration.
    * The <code>getElements<code> function should be used instead where possible, since the real xPath is case-sensitive.
    *
    * @param name The path of the nodes wanted. Uses a real xpath match.
    */
    protected static Element[] getElementsViaXPath(String name)
    {
        Element[] elements = new Element[0];
        for (Document config: XTTConfiguration.getAllDocuments())
        {
            elements = getElementsViaXPath(name,config);
            //XTTProperties.printDebug("getElements: checking temp config: " + (i+1) + " of " + tempConfigurations.size());
            if((elements != null) && (elements.length > 0))
            {
                return elements;
            }
        }
        return elements;
    }

    /**
    * Returns an <code>Element</code> matching the path given by name, in the <code>Document</code> <code>doc</code>
    * The <code>getElement<code> function should be used instead where possible, since the real xPath is case-sensitive.
    *
    * @param name The path of the nodes wanted. Uses a real xpath match (see {@link #getProperty}).
    * @param doc The <code>Docuement</code> in which to search.
    */
    public static Element getElementViaXPath(String name, Document doc)
    {
        Element node = null;
        try
        {
            Element root = doc.getRootElement();
            Object o = org.jdom.xpath.XPath.selectSingleNode(root,name);
            node = (Element)o;
        }
        catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        return node;
    }

    /**
    * Returns an array of type <code>Element</code> containing all the nodes matching <code>name</code> in the <code>Document</code> <code>doc</code>.
    * The <code>getElements<code> function should be used instead where possible, since the real xPath is case-sensitive.
    *
    * @param name The path of the nodes wanted. Uses a real xpath match
    * @param doc The <code>Docuement</code> in which to search.
    */
    public static Element[] getElementsViaXPath(String name, Document doc)
    {
        //If you have no XML Document to test against, just stop
        if(doc == null)
        {
            return new Element[0];
        }

        Element[] elements = new Element[0];
        try
        {
            /*You have to do all this extra garbage since JDom isn't using 1.5 Generics yet.
              And we really don't want to have unsafe warnings all the time!*/
            List selectedNodes = org.jdom.xpath.XPath.selectNodes(doc.getRootElement(),name);
            elements = new Element[selectedNodes.size()];
            int i = 0;
            for(Object element : selectedNodes)
            {
                elements[i++] = (Element)element;
            }
        }
        catch(ClassCastException cce)
        {
            XTTProperties.printFail("getElementsViaXPath: One of the nodes returned by " + name + " wasn't of type Element");
        }
        catch(Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }

        return elements;
    }

    /**
    * Returns the <code>Text</code> of the <code>Element</code> matching the path given by name, in the <code>Document</code> <code>doc</code>
    *
    * @param name The path of the nodes wanted. Uses a pseudo xpath match (see {@link #getProperty}).
    * @param root The <code>Document</code> in which to search.
    */
    public String getElementText(String name, Document root)
    {
        Element node = null;
        try
        {
            node = getElement(name,root);
            return node.getText();
        }
        catch(NullPointerException npe)
        {
            XTTProperties.printFail("getElementText: '" + name + "' doesn't exist.");
        }
        catch(Exception e)
        {
            XTTProperties.printFail("getElementText: Error while getting '" + name + "'.");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        return null;
    }

    /**
    * Case insensitive removal of first child matching name, under <code>root</code>..
    *
    */
    private static boolean removeChild(String name, Element root)
    {
        if(root == null)
        {
            return false;
        }
        List children = root.getChildren();
        Element temp;
        for (int j = 0; j<children.size();j++)
        {
            temp = (Element)children.get(j);
            if ( temp.getName().equalsIgnoreCase(encapsulateName(name)) )
            {
                root.removeChild( temp.getName() );
                return true;
            }
        }
        return false;
    }

    /**
    * Case insensitive removal of all children matching name, under <code>root</code>.
    *
    */
    private static boolean removeChildren(String name, Element root)
    {
        if(root == null)
        {
            return false;
        }
        List children = root.getChildren();
        Element temp;
        boolean wasRemoved = false;
        for (int j = 0; j<children.size();j++)
        {
            temp = (Element)children.get(j);
            if ( temp.getName().equalsIgnoreCase(encapsulateName(name)) )
            {
                root.removeChild( temp.getName() );
                wasRemoved = true;
            }
        }
        return wasRemoved;
    }

    /**
    * Case insensitive retrieval of the first child matching <code>name</code>, under <code>root</code>.
    */
    public static Element getChild(String name, Element root)
    {
        Vector children = getChildren(name,root);
        if((children != null) && (children.size() > 0))
        {
            return (Element)children.get(0);
        }
        else
        {
            return null;
        }
    }

    /**
    * Case insensitive retrieval of all children matching <code>name</code>, under <code>root</code>.
    */
    public static Vector<Element> getChildren(String name, Element root)
    {
        if(root == null)
        {
            return null;
        }

        List children = root.getChildren();
        Vector<Element> elements = new Vector<Element>();
        Element temp;
        for (int j = 0; j<children.size();j++)
        {
            temp = (Element)children.get(j);
            if ( temp.getName().equalsIgnoreCase(encapsulateName(name)) )
            {
                elements.add(temp);
            }
        }
        return elements;
    }

    /**
    * Case insensitive removal of all nodes matching name.
    *
    */
    public static void removeAll(String name)
    {
        Element root = null;
        for (Document config: XTTConfiguration.getAllDocuments())
        {
            root = config.getRootElement();
            removeAll(name,root);
        }
    }

    /**
    * Case insensitive removal of all nodes matching name, under <code>root</code>.
    *
    */
    private static void removeAll(String name, Element root)
    {
        removeChildren(name,root);
        List children = root.getChildren();
        for (int i=0;i<children.size();i++)
        {
            removeAll(name,(Element)children.get(i));
        }
    }

    /**
    * Returns a String value from an XML DOM Tree corresponding to the name argument.
    * <p>
    * The name argument can point to subnodes of nodes by adding the '/' character
    * This specifies the tree to search down, it can be as long as you want.
    * If just a single word is specified, then the first occurance of that node will be returned.
    *
    * Get property first searches through the test specific configuration, before checking the global one.
    * Returns "null" if not found
    * @param primaryName The node to get
    * @param deprecatedNames A list of nodes to search if the primaryName isn't found
    */
    private static String getProperty(String primaryName, String... deprecatedNames)
    {
        String property = getSingleQuietProperty(primaryName);

        if (property.equals("null"))
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietProperty(name);
                if(!property.equals("null"))
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }

        if (property.equals("null"))
        {
            XTTProperties.printDebug("Property '" + primaryName + "' wasn't found");
        }
        return property;
    }


    /**
    * getProperty, but with no warning.
    *
    */
    public static String getQuietProperty(String primaryName, String... deprecatedNames)
    {
        String property = getSingleQuietProperty(primaryName);

        if (property.equals("null"))
        {
            for(String name : deprecatedNames)
            {
                property = getSingleQuietProperty(name);
                if(!property.equals("null"))
                {
                    XTTProperties.printWarn("'" + property + "' found in deprecated node '" + name + "', use '" + primaryName +"' instead.");
                    return property;
                }
            }
        }

        return property;
    }

    /**
    * getProperty, but with no warning.
    *
    */
    public static String getSingleQuietProperty(String name)
    {
        if(XTTConfiguration.getNumberofPermanentLocalConfigurations() == 0)
        {
            //Don't care so much about configurations when not in XTT mode.
            if(XTTProperties.getXTT()!=null)
            {
                XTTProperties.printWarn("getProperty: No config loaded");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            return "null";
        }

        Element test = getElement(name);
        if (test == null)
        {
            return "null";
        }
        else
        {
            return test.getText();
        }
    }

    /**
    *
    * Return a <code>Document<code> of the current test XML file to be run.
    *
    * @see <A HREF="http://www.jdom.org/docs/apidocs/index.html">JDOM</A>
    */
    public static Document getCurrentTestDocument()
    {
        return readXML(XTTProperties.getCurrentTestFileName());
    }

    /**
    *
    * Return a JDOM Document from the file specified by fileName.
    * @param fileName The XML file to load
    * @see <A HREF="http://www.jdom.org/docs/apidocs/index.html">JDOM</A>
    */
    public static Document readXML(String fileName)
    {
        try
        {
            if(fileName.toLowerCase().endsWith(".xml"))
            {
                org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                org.jdom.Document test = parser.build(fileName);
                return test;
            }
            else
            {
                XTTProperties.printFail("'" + fileName +"' Invalid file type, must be .XML");
            }
        }
        catch (org.jdom.input.JDOMParseException jpe)
        {
            XTTProperties.printFail("Error while reading '" + fileName + "' XML:\n" + jpe);
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.printFail("Error while reading XML '" + fileName + "'");
        }
        return null;
    }

/**
    *
    * Return a JDOM Document from a String.
    * @param data The XML to load
    * @see <A HREF="http://www.jdom.org/docs/apidocs/index.html">JDOM</A>
    */
    public static Document readXMLFromString(String data)
    {
        try
        {
            java.io.StringReader reader = new java.io.StringReader(data);
            org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
            org.jdom.Document test = parser.build(reader);
            return test;
        }
        catch (org.jdom.input.JDOMParseException jpe)
        {
            XTTProperties.printFail("Error while reading XML String:\n" + jpe);
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            XTTProperties.printFail("Error while reading XML String");
        }
        return null;
    }

    public static void sort(Document doc)
    {
        sort(doc,1);
    }
    public static void sort(Document doc, int depth)
    {
        sort(doc.getRootElement(),depth);
    }

    public static void sort(Element node)
    {
        sort(node,1);
    }

    @SuppressWarnings("unchecked")
    public static void sort(Element node, int depth)
    {
        //No node to sort
        if(node == null)
        {
            return;
        }
        //Don't go deeper
        if(depth == 0)
        {
            return;
        }

        List<Element> list = node.getChildren();
        Element[] children = (Element[]) list.toArray(new Element[list.size()]);
        Arrays.sort(children, new sortByElementName());

        for (int i = 0; i < children.length; i++)
        {
            children[i].detach();
        }
        for (int i = 0; i < children.length; i++)
        {
            list.add(children[i]);
        }
    }

    private static class sortByElementName implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Element e1 = (Element)o1;
            Element e2 = (Element)o2;
            System.out.println("e1: " + e1.getName() + " e2: " + e2.getName());
            return e1.getName().compareTo(e2.getName());
        }
    }

}

