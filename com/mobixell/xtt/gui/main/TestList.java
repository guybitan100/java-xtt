package com.mobixell.xtt.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.StringOutputStream;
import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.XTTTest;
import com.mobixell.xtt.gui.CSVFileFilter;
import com.mobixell.xtt.gui.DropTransferHandler;
import com.mobixell.xtt.gui.TestEditor;
import com.mobixell.xtt.gui.XMLFileFilter;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;
import com.mobixell.xtt.log.LogViewer;

/**
 *
 *
 * @author      Roger Soder & Guy Bitan
 * @version     $Id: TestList.java,v 1.35 2008/08/22 08:58:33 rsoder Exp $
 * @see         com.mobixell.xtt.gui.main.XTTGui
 */
public class TestList extends JPanel implements ActionListener, MouseListener
{
    public static final String tantau_sccsid = "@(#)$Id: TestList.java,v 1.35 2008/08/22 08:58:33 rsoder Exp $";
    private XTTGui xttgui                    = null;
    private TestList thisclass               = this;
    private Vector<Test> testlist            = new Vector<Test>();
    private JPopupMenu popup                 = null;
    private JPopupMenu popup2                = null;
    private String currentFilePath           = "";
    private String currentFileName           = "";
    private JProgressBar progressBar         = null;
    private JDialog progressWindow           = null;
    private JButton memory                   = null;
    private ListTable testTable              = null;
    private ListTableModel listtablemodel    = new ListTableModel();
    private TableButton tableButton          = new TableButton();
    private JMenu menuItemRemove =null;
    private JScrollPane scrollport           = null;

    private static int tableButtonwidth      = 90;
    private Vector<Test> selectedtestlist = null;

    private boolean modality=true;public void setRunModality(boolean mod){modality=mod;}

    private JMenu exportMenu=null;

    public void setPreferedFilenameWidth(int width)
    {
        testTable.getColumnModel().getColumn(listtablemodel.FILENAME).setPreferredWidth(width);
        testTable.getColumnModel().getColumn(listtablemodel.FILENAME).setWidth(width);
    }
    public void setPreferedDescriptionWidth(int width)
    {
        testTable.getColumnModel().getColumn(listtablemodel.DESC).setPreferredWidth(width);
        testTable.getColumnModel().getColumn(listtablemodel.DESC).setWidth(width);
    }

    public TestList(XTTGui xttgui)
    {
        this.xttgui=xttgui;
        this.setLayout(new BorderLayout());

        testTable = new ListTable(listtablemodel);
        setOrderList(false);
        scrollport=SwingUtils.getJScrollPaneWithWaterMark(ImageCenter.getInstance().getAwtImage(
        		ImageCenter.ICON_SCENARIO_TREE_BG), testTable);
        scrollport.setAutoscrolls(true);
        
        DropTransferHandler dth=new DropTransferHandler(this);
        testTable.setTransferHandler(dth);
        scrollport.setTransferHandler(dth);

        testTable.addMouseListener(this);
        testTable.setDragEnabled(true);

        TableButton brend=tableButton;
        testTable.getColumnModel().getColumn(listtablemodel.BUTTON).setCellEditor(brend);
        testTable.getColumnModel().getColumn(listtablemodel.BUTTON).setCellRenderer(brend);
        testTable.getColumnModel().getColumn(listtablemodel.BUTTON).setMaxWidth(tableButtonwidth);
        testTable.getColumnModel().getColumn(listtablemodel.BUTTON).setMinWidth(tableButtonwidth);
        testTable.getColumnModel().getColumn(listtablemodel.FOLDER).setMinWidth(tableButtonwidth);
        testTable.getColumnModel().getColumn(listtablemodel.FOLDER).setPreferredWidth(150);
        testTable.getColumnModel().getColumn(listtablemodel.FOLDER).setMaxWidth(200);
        testTable.getColumnModel().getColumn(listtablemodel.FILENAME).setMinWidth(tableButtonwidth);
        testTable.getColumnModel().getColumn(listtablemodel.FILENAME).setPreferredWidth(180);
        testTable.getColumnModel().getColumn(listtablemodel.FILENAME).setMaxWidth(350);
        
        testTable.getColumnModel().getColumn(listtablemodel.LEVEL).setMinWidth(tableButtonwidth);
        testTable.getColumnModel().getColumn(listtablemodel.LEVEL).setPreferredWidth(100);
        testTable.getColumnModel().getColumn(listtablemodel.LEVEL).setMaxWidth(150);
        
        this.add(scrollport,BorderLayout.CENTER);

        popup = new JPopupMenu();

        memory=new JButton();
        memory.setActionCommand("MEMORY");
        memory.addActionListener(this);

        JMenuItem menuItem=null;

        menuItem=new JMenuItem("Add Test");
        menuItem.setActionCommand("ADDTEST");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        
        menuItemRemove=new JMenu("Remove");
        popup.add(menuItem);
        
        menuItem=new JMenuItem("Selected");
        menuItem.setActionCommand("REMOVESELECTED");
        menuItem.addActionListener(this);
        menuItemRemove.add(menuItem);
        
        menuItem=new JMenuItem("Passed");
        menuItem.setActionCommand("REMOVEPASSED");
        menuItem.addActionListener(this);
        menuItemRemove.add(menuItem);
        
        menuItem=new JMenuItem("Failed");
        menuItem.setActionCommand("REMOVEFAILED");
        menuItem.addActionListener(this);
        menuItemRemove.add(menuItem);
        
        popup.add(menuItemRemove);
        popup.addSeparator();

        menuItem=new JMenuItem("Run List");
        menuItem.setActionCommand("RUNALL");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem=new JMenuItem("Run Selected");
        menuItem.setActionCommand("RUNSELECTED");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
        menuItem=new JMenuItem("Log");
        menuItem.setActionCommand("LOG");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();

        menuItem=new JMenuItem("Load");
        menuItem.setActionCommand("LOAD");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem=new JMenuItem("Save As");
        menuItem.setActionCommand("SAVEAS");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem=new JMenuItem("Save");
        menuItem.setActionCommand("SAVE");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        // Right mouse popup
        popup2 = new JPopupMenu();
        menuItem=new JMenuItem("Run Selected");
        menuItem.setActionCommand("RUNSELECTED");
        menuItem.addActionListener(this);
        popup2.add(menuItem);
        menuItem=new JMenuItem("Run List");
        menuItem.setActionCommand("RUNALL");
        menuItem.addActionListener(this);
        popup2.add(menuItem);
        popup2.addSeparator();
        menuItem=new JMenuItem("Remove Selected");
        menuItem.setActionCommand("REMOVESELECTED");
        menuItem.addActionListener(this);
        popup2.add(menuItem);

        // exportMenu
        exportMenu=new JMenu("Export");
        menuItem=new JMenuItem("All Test Results");
        menuItem.setActionCommand("EXPORTALL");
        menuItem.addActionListener(this);
        exportMenu.add(menuItem);

        menuItem=new JMenuItem("Selected Test Results");
        menuItem.setActionCommand("EXPORTSELECTED");
        menuItem.addActionListener(this);
        exportMenu.add(menuItem);

        currentFilePath = XTTProperties.getXTTFilePath();
        setMenuEnabled();
    }


    public void mouseClicked(MouseEvent e){}
    public  void mouseEntered(MouseEvent e){}
    public  void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e)
    {
        maybeShowPopup(e);
    }
    public void mouseReleased(MouseEvent e)
    {
        maybeShowPopup(e);
    }
    private void maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger())
        {
            if(e.getComponent()==testTable)
            {
                popup2.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    public void setOrderList (boolean isOrder)
    {
    	if (isOrder)
    		testTable.setRowSorter(new TableRowSorter(listtablemodel));
    	else
    		testTable.setRowSorter(null);
    }
    public boolean checkDragSource(JComponent comp)
    {
        return (comp==testTable);
    }

    public JMenu getExportMenu()
    {
        return exportMenu;
    }

    public int[] getSelectedRows()
    {
        return testTable.getSelectedRows();
    }

    public boolean moveEntries(int[] selected)
    {
        Test target=testlist.elementAt(testTable.getSelectedRow());
        //System.out.println("MOVEENTRIES");
        Vector<Test> entries=new Vector<Test>();
        Test t=null;
        for(int i=0;i<selected.length;i++)
        {
            entries.add(testlist.elementAt(selected[i]));
        }
        int targetOldIndex=testlist.indexOf(target);
        testlist.removeAll(entries);
        int targetIndex=testlist.indexOf(target);
        if(targetIndex<0)targetIndex=targetOldIndex;
        if(targetIndex>testlist.size())targetIndex=testlist.size();
        testlist.addAll(targetIndex,entries);
        listtablemodel.fireTableDataChanged();
        //System.out.println(entries);
        xttgui.setListStatus(xttgui.LIST_EDITED);
        return true;
    }

    public java.util.List<File> getSelectedFileList()
    {
        Vector<File> files=new Vector<File>();
        int[] selected=testTable.getSelectedRows();
        Test t=null;
        for(int i=0;i<selected.length;i++)
        {
            t=testlist.elementAt(selected[i]);
            try
            {
                File f=new File(t.getFileName());
                files.add(f);
            } catch (Exception e){}
        }
        return files;
    }

    private File getExportFile()
    {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new CSVFileFilter());
        fc.setSelectedFile(new File("export.csv"));
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
                    showError("Export Error",ex.getClass().getName()+"\n"+ex.getMessage());
                }
            } else
            {
                return file;
            }
        }
        return null;
    }

    
    public void setMenuEnabled()
    {
    	  if (testTable.getRowCount()==0)
    		  menuItemRemove.setEnabled(false);
    	  else
    		  menuItemRemove.setEnabled(true);
    }
    public void actionPerformed(ActionEvent e)
    {
    	setMenuEnabled();
        if(e.getActionCommand().equals("CONFIG"))
        {
            int x=((java.awt.Component)e.getSource()).getX()+4;//- (int)((java.awt.Component)e.getSource()).getSize().getWidth();
            int y=((java.awt.Component)e.getSource()).getY()+4;
            popup.show((java.awt.Component)e.getSource(),x,y);
        } else if(e.getActionCommand().equals("LOAD"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are running, can't remove until they finished!");
                return;
            }
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir") + "/tests/XMP");
            fc.setFileFilter(new LISTFileFilter());
            //In response to a button click:
            int returnVal = fc.showOpenDialog(xttgui);
            if(returnVal==fc.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();

                if(!file.exists()&&!file.getAbsolutePath().endsWith(".list"))
                {
                    file=new File(file.getAbsolutePath()+".list");
                }

                //globalLog.clear();
                loadFile(file.getAbsolutePath());

            }
        } else if(e.getActionCommand().equals("RUNALL"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are already running, can't start until they finished!");
                return;
            }
            xttgui.resetStatus();
            Vector<XTTTest> list=new Vector<XTTTest>();
            Iterator<Test> it=testlist.iterator();
            Test t=null;
            while(it.hasNext())
            {
                t=it.next();
                t.resetTest();
                t.doQueue();
                list.add(t);
            }
            XTTProperties.setTestList(list);
            runTests();
        } else if(e.getActionCommand().equals("RUNSELECTED"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are already running, can't start until they finished!");
                return;
            }
            xttgui.resetStatus();
            int[] runselected=testTable.getSelectedRows();
            Vector<XTTTest> list=new Vector<XTTTest>();
            selectedtestlist=new Vector<Test>();
            Test t=null;

            for(int i=0;i<runselected.length;i++)
            {
                t=(Test)testlist.elementAt(runselected[i]);
                t.resetTest();
                t.doQueue();
                list.add(t);
                selectedtestlist.add(t);
            }
            if(runselected.length>0)
            {
                //selectedtestlist=list;
                XTTProperties.setTestList(list);
                runTests();
            } else
            {
                selectedtestlist=null;
            }
        } else if(e.getActionCommand().equals("EXPORTALL"))
        {
            try
            {
                File file=getExportFile();
                if(file!=null)
                {
                    XTTProperties.printDebug("Exporting: "+file.getCanonicalPath());
                    Vector<Test> list=new Vector<Test>();
                    Iterator<Test> it=testlist.iterator();
                    while(it.hasNext())
                    {
                        list.add(it.next());
                    }
                    exportTests(list,file);
                }
            } catch (Exception ex)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                showError("Export Error",ex.getClass().getName()+"\n"+ex.getMessage());
            }
        } else if(e.getActionCommand().equals("EXPORTSELECTED"))
        {
            try
            {
                File file=getExportFile();
                if(file!=null)
                {
                    XTTProperties.printDebug("Exporting: "+file.getCanonicalPath());
                    int[] runselected=testTable.getSelectedRows();
                    Vector<Test> list=new Vector<Test>();
                    Test t=null;
                    for(int i=0;i<runselected.length;i++)
                    {
                        t=(Test)testlist.elementAt(runselected[i]);
                        list.add(t);
                    }
                    exportTests(list,file);
                }
            } catch (Exception ex)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                showError("Export Error",ex.getClass().getName()+"\n"+ex.getMessage());
            }
		} else if (e.getActionCommand().equals("REMOVESELECTED")) {
			if (XTT.isTestRunning()) {
				showError("Run Error","Tests are running, can't remove until they finished!");
				return;
			}
			for (int i = testTable.getSelectedRowCount() - 1; i >= 0; i--) {
				testlist.removeElementAt(testTable.getSelectedRows()[i]);
			}
			listtablemodel.fireTableDataChanged();
			xttgui.setListStatus(xttgui.LIST_EDITED);

		}
        else if(e.getActionCommand().equals("REMOVEPASSED"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are running, can't remove until they finished!");
                return;
            }
            for(int i=testTable.getRowCount()-1;i>=0;i--)
            {
            	if (((JButton) (testTable.getValueAt(i, listtablemodel.BUTTON))).getText().equals(XTTProperties.getStatusDescription(XTTProperties.PASSED)))
            	{
            		testlist.removeElementAt(i);
            	}
             
            }
            listtablemodel.fireTableDataChanged();
            xttgui.setListStatus(xttgui.LIST_EDITED);
        }
        else if(e.getActionCommand().equals("REMOVEFAILED"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are running, can't remove until they finished!");
                return;
            }
            for(int i=testTable.getRowCount()-1;i>=0;i--)
            {
            	if (((JButton) (testTable.getValueAt(i, listtablemodel.BUTTON))).getText().equals(XTTProperties.getStatusDescription(XTTProperties.FAILED)))
            	{
            		testlist.removeElementAt(i);
            	}
             
            }
            listtablemodel.fireTableDataChanged();
            xttgui.setListStatus(xttgui.LIST_EDITED);

        }
        else if(e.getActionCommand().equals("LOG"))
        {
            try
            {
                LogViewer ed=new LogViewer(xttgui,XTTProperties.getLog(),true);
            } catch (java.lang.OutOfMemoryError ex)
            {
                showError("Out of Memory","java.lang.OutOfMemoryError");
            }
        } else if(e.getActionCommand().equals("SAVE")&&!currentFileName.equals(""))
        {
            try
            {
                XTTProperties.printDebug("Writing file name: "+currentFileName);
                PrintWriter out=new PrintWriter(new FileWriter(currentFileName));
                writeFile(out);
                xttgui.setListStatus(xttgui.LIST_SAVED);
            } catch (Exception ex)
            {
                showError("LIST Save Error",ex.getClass().getName()+"\nList file '"+currentFileName+"' can't be written!");
            }
        } else if(e.getActionCommand().equals("SAVEAS")||(e.getActionCommand().equals("SAVE")&&currentFileName.equals("")))
        {
            try
            {
                JFileChooser fc = new JFileChooser(".");
                fc.setFileFilter(new LISTFileFilter());
                //In response to a button click:
                int returnVal = fc.showSaveDialog(xttgui);
                if(returnVal==fc.APPROVE_OPTION)
                {
                    File file = fc.getSelectedFile();
                    if(!file.getAbsolutePath().endsWith(".list"))
                    {
                        file=new File(file.getAbsolutePath()+".list");
                    }

                    PrintWriter out=new PrintWriter(new FileWriter(file));
                    xttgui.setListFileName(file.getAbsolutePath());
                    XTTProperties.printDebug("Writing file name: "+currentFileName);
                    writeFile(out);
                    xttgui.setListStatus(xttgui.LIST_SAVED);
                }
            } catch (Exception ex)
            {
                showError("LIST Save Error",ex.getClass().getName()+"\nList file '"+currentFileName+"' can't be written!");
            }
        } else if(e.getActionCommand().equals("ADDTEST"))
        {
            if(XTT.isTestRunning())
            {
                showError("Run Error","Tests are running, can't add until they finished!");
                return;
            }
                JFileChooser fc = new JFileChooser(System.getProperty("user.dir")+"/tests/XMP/XMP_XFW006");
                fc.setMultiSelectionEnabled(true);
                fc.setFileFilter(new XMLFileFilter());
                //In response to a button click:
                int returnVal = fc.showOpenDialog(this);
                if(returnVal==fc.APPROVE_OPTION)
                {
                   prepareAndAddTest(fc.getSelectedFiles());
                }
        } else if(e.getActionCommand().equals("MEMORY"))
        {
            Runtime.getRuntime().runFinalization();
            System.gc();
            setMemoryBar();
        }
    }
    public void prepareAndAddTest (File []files)
    {
         try
         {
         for (int i=0;i<files.length;i++)
         {
         org.jdom.output.XMLOutputter outputter=new org.jdom.output.XMLOutputter();
         org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
         org.jdom.Document document=parser.build(files[i].getAbsolutePath());
         if(!document.getRootElement().getName().toLowerCase().equals("test"))
         {
             String error="The outer node in the test file isn't called test";
             showError("Test File Set Error",error);
             return;
         }
         addTest(files[i].getAbsolutePath(),"",getFileDescription(document),getFileLevel(document));
         xttgui.resetStatus();
         xttgui.setListStatus(xttgui.LIST_EDITED);
         }
         } catch (Exception e)
         {
        	 showError("Test File Set Error",e.getClass().getName()+"\n"+e.getMessage());	 
         }
    }
    private void writeFile(PrintWriter out) throws Exception
    {
        Test t=null;
        for(int i=0;i<testlist.size();i++)
        {
            t=(Test)testlist.elementAt(i);
            out.print(t.getComments());
            out.println(shortFileName(t.getFileName()));
        }
        out.flush();
        out.close();
    }
    private void exportTests(Vector<Test> list, File file) throws Exception
    {
        PrintWriter out=new PrintWriter(new FileWriter(file));
        Test t=null;
        //out.print("\"i\""+XTTProperties.getExportDelimiter());
        out.print("\"teststatus\""+XTTProperties.getExportDelimiter());
        out.print("\"short filename\""+XTTProperties.getExportDelimiter());
        out.print("\"test name\""+XTTProperties.getExportDelimiter());
        out.print("\"description\""+XTTProperties.getExportDelimiter());
        out.print("\"elapsed time\""+XTTProperties.getExportDelimiter());
        out.print("\"start time\""+XTTProperties.getExportDelimiter());
        out.println("\"end time\"");
        for(int i=0;i<list.size();i++)
        {
            t=(Test)list.elementAt(i);
            //out.print("\""+i+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+XTTProperties.getStatusDescription(t.getTestStatus())+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+shortFileName(t.getFileName())+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getTestName().replaceAll("\"","\"\"")+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getDescription().replaceAll("\"","\"\"")+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getElapsedTime()+"\""+XTTProperties.getExportDelimiter());
            out.print("\""+t.getStartTime()+"\""+XTTProperties.getExportDelimiter());
            out.println("\""+t.getEndTime()+"\"");
        }
        out.flush();
        out.close();
    }
    public void runTests()
    {
        //globalLog.clear();
        progressWindow=new JDialog(xttgui, "Running tests...", modality);
        progressBar = new JProgressBar(0,XTTProperties.getNumberOfTests());
        progressBar.setPreferredSize(new java.awt.Dimension(200,20));
        progressBar.setMaximum(XTTProperties.getNumberOfTests());
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0% - ?/?");
        progressWindow.getContentPane().add(progressBar);
        progressWindow.setDefaultCloseOperation(progressWindow.DO_NOTHING_ON_CLOSE);

        RunTheList runList=new RunTheList();
        progressWindow.addWindowListener(runList);
        progressWindow.pack();
        progressWindow.setLocationRelativeTo(null); //center it
        this.revalidate();
        this.repaint();
        Thread t=new Thread(runList,"TestList");
        t.start();
        progressWindow.setVisible(true);
    }

    private void showError(String errortitle,String errortext)
    {
        JOptionPane.showMessageDialog(xttgui,
            errortext,
            errortitle,
            JOptionPane.ERROR_MESSAGE);
    }

    public void loadFile(String filePath)
    {
        File fileName=new File(filePath);
        filePath=fileName.getAbsolutePath();

        testlist=new Vector<Test>();
        listtablemodel.fireTableDataChanged();
        xttgui.setListFileName("");

        StringBuffer lineComments = new StringBuffer(""); //Store all the information from the file
        try
        {
            if(!fileName.exists())
            {
                XTTProperties.printDebug("Creating file name: "+fileName);
                fileName.createNewFile();
            } else
            {
                XTTProperties.printDebug("Loading file name: "+fileName);
            }
            BufferedReader fileIn = new BufferedReader(new FileReader(fileName));
            xttgui.setListFileName(filePath);
            String temp = null; //Used each time to check if the line is null
            // Read the file in, while there's file left to read
            String currentFile="";
            String error="";
            int errorcount=0;
            String description="";
            String level="";
            while ((temp = fileIn.readLine())!=null)
            {
                if((temp.trim().length() > 0)&&(!temp.trim().startsWith("//")))
                {
                    if(new File(currentFilePath + temp).exists())
                    {
                        currentFile = new File(currentFilePath + temp).getAbsolutePath();
                    } else if(new File(temp).exists())
                    {
                        currentFile = new File(temp).getAbsolutePath();
                    } else
                    {
                        if(++errorcount<=20)
                        {
                            error=error+"Couldn't find file '"+temp+"'\n";
                        }
                        continue;
                    }
                    try
                    {
                        org.jdom.output.XMLOutputter outputter=new org.jdom.output.XMLOutputter();
                        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                        org.jdom.Document document=parser.build(new File(currentFile).getAbsolutePath());
                        if(!document.getRootElement().getName().toLowerCase().equals("test"))
                        {
                            if(++errorcount<=20)
                            {
                                error=error+"The outer node in the test file '"+temp+"' isn't called test\n";
                            }
                            continue;
                        }
                        description=getFileDescription(document);
                        level = getFileLevel(document);
                    } catch (Exception e)
                    {
                        if(++errorcount<=20)
                        {
                            error=error+"The file '"+temp+"' has an XML error: "+e.getMessage()+"\n";
                        }
                        continue;
                    }

                    addTest(currentFile,lineComments.toString(),description,level);
                    lineComments = new StringBuffer("");
                } else
                {
                    lineComments.append(temp+"\n");
                }
            }
            if(filePath.toLowerCase().endsWith(".xml"))
            {
                showError("LIST Load Error","You selected an XML document as a list, did you mean to load a test instead?\nTry using 'Add Test' instead, or just drag & drop your tests.\n\n" + error+""+(errorcount-20)+" more errors...");
            } else if(errorcount>20)
            {
                showError("LIST Load Error",error+""+(errorcount-20)+" more errors...");
            } else if(errorcount>0)
            {
                showError("LIST Load Error",error);
            }
            xttgui.setListStatus(xttgui.LIST_LOADED);
        } catch (Exception e)
        {
            showError("LIST Load Error",e.getClass().getName()+"\n"+e.getMessage());
            xttgui.setListStatus(xttgui.LIST_FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            return;
        }
    }

    public void setFileName(String filePath)
    {
        currentFileName=filePath;
        if(filePath.lastIndexOf(File.separator) != -1)
        {
            currentFilePath = filePath.substring(0,filePath.lastIndexOf(File.separator)+1);
        }
        else
        {
            currentFilePath = (new File("")).getAbsolutePath()+File.separator;
        }

    }

    private class RunTheList implements Runnable, WindowListener
    {
        public void run()
        {
            try
            {
                XTTProperties.getXTT().runTests(false);
            } catch (java.lang.OutOfMemoryError oom)
            {
                showError("Out of Memory","java.lang.OutOfMemoryError");
            } catch (Exception ex)
            {
                showError("Uncaught Exception",ex.getClass().getName()+"\n"+ex.getMessage());
                XTTProperties.printDebugException(ex);
            } finally
            {
                progressWindow.setVisible(false);

                if(selectedtestlist!=null)
                {
                    Iterator<Test> it=selectedtestlist.iterator();
                    while(it.hasNext())
                    {
                        it.next().unQueue();
                    }
                } else
                {
                    Iterator<Test> it=testlist.iterator();
                    while(it.hasNext())
                    {
                        it.next().unQueue();
                    }
                }
              
                selectedtestlist=null;
                setMemoryBar();
            }
        }
        public void windowActivated(WindowEvent e){}
        public void windowClosed(WindowEvent e)
        {
            //xttgui.setDoAbort();
        }
        public void windowClosing(WindowEvent e)
        {
            if(!XTT.isTestRunning())
            {
                progressWindow.setVisible(false);
            }
            progressWindow.setTitle("Aborting...");
            xttgui.setDoAbort();
        }
        public void windowDeactivated(WindowEvent e){}
        public void windowDeiconified(WindowEvent e){}
        public void windowIconified(WindowEvent e){}
        public void windowOpened(WindowEvent e){}
    }

    public void addTest(String filename,String comments,String description,String level)
    {
        Test newTest=new Test(filename,comments,description,level);
        testlist.add(newTest);
        listtablemodel.fireTableDataChanged();
    }

    public void moveUp() 
	{
		if (XTT.isTestRunning()) 
		{
			showError("Run Error",
					"Tests are running, can't remove until they finished!");
			return;
		}
		int start = 0 ;
		int selectedIndex=0;
		for (int ind =0; ind<testTable.getSelectedRowCount(); ind++) 
		{
			if (ind ==0) start = getSelectedRows()[ind];
				
			 selectedIndex = getSelectedRows()[ind]; 
			 if( selectedIndex > 0 ) 
	           {
				 Test testTemp =testlist.get(selectedIndex);
				 testlist.set(selectedIndex,testlist.get(selectedIndex-1));
				 testlist.set(selectedIndex-1,testTemp);
	           }
		}
		//listtablemodel.fireTableDataChanged();
		ListSelectionModel selection = testTable.getSelectionModel();
		int anchor = selection.getAnchorSelectionIndex();
        int lead = selection.getLeadSelectionIndex();
		//testTable.getSelectionModel().setSelectionInterval(2,);
		//testTable.getSelectionModel().setSelectionInterval(start-1,start);
		xttgui.setListStatus(xttgui.LIST_EDITED);
	}
    public void addTests(File[] files, boolean isDrop)
    {
        String error="";
        String description="";
        String level="";
        int errorcount=0;
        int dropPosition=testlist.size();
        String errortype="load";
        if(isDrop)
        {
            dropPosition=testTable.getSelectedRow()+1;
            if(dropPosition==-1)dropPosition=testlist.size();
            if(dropPosition>testlist.size())dropPosition=testlist.size();
            errortype="drop";
        }
        for(int i=0;i<files.length;i++)
        {
            // File not Found:

            if(files[i]==null)
            {
                if(++errorcount<=20)
                {
                    error=error+"NullPounterException when trying to add current entry\n";
                }
                continue;
            } else if(!files[i].exists())
            {
                if(++errorcount<=20)
                {
                    error=error+"Couldn't find file '"+files[i]+"'\n";
                }
                continue;
            // File found:
            } else if(files[i].getName().toLowerCase().endsWith(".list"))
            {
                loadFile(files[i].getAbsolutePath());
            }
            else
            {
                try
                {
                    org.jdom.output.XMLOutputter outputter=new org.jdom.output.XMLOutputter();
                    org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                    org.jdom.Document document=parser.build(files[i].getAbsolutePath());
                    if(!document.getRootElement().getName().toLowerCase().equals("test"))
                    {
                        if(++errorcount<=20)
                        {
                            error=error+"The outer node in the test file '"+files[i].getAbsolutePath()+"' isn't called test\n";
                        }
                        continue;
                    }
                    description=getFileDescription(document);
                    level=getFileLevel(document);
                }
                catch (org.jdom.input.JDOMParseException jdpe)
                {
                    //Partial XTT Test
                    if(jdpe.getPartialDocument() != null && jdpe.getPartialDocument().getRootElement().getName().toLowerCase().equals("test"))
                    {
                        if(++errorcount<=20)
                        {
                            error=error+jdpe.getSystemId() + " (" + jdpe.getLineNumber() + "):" + jdpe.getColumnNumber() + "\n" + jdpe.getMessage() + "\n";
                        }
                    }
                    else
                    {
                        if(++errorcount<=20)
                        {
                            error=error+"The file '"+files[i].getAbsolutePath()+"' has an XML error: "+jdpe.getMessage()+"\n";
                        }
                        continue;
                    }
                    
                }
                catch (Exception e)
                {
                    if(++errorcount<=20)
                    {
                        error=error+"The file '"+files[i].getAbsolutePath()+"' has an XML error: "+e.getMessage()+"\n";
                    }
                    continue;
                }

                Test newTest=new Test(files[i].getAbsolutePath(),"",description,level);
                testlist.add(dropPosition++,newTest);
                listtablemodel.fireTableDataChanged();
                if(isDrop)xttgui.setListStatus(xttgui.LIST_EDITED);

                //this.revalidate();
                //this.repaint();
            }
        }
        if(errorcount>20)
        {
            showError("LIST "+errortype+" Error",error+""+(errorcount-20)+" more errors...");
        } else if(errorcount>0)
        {
            showError("LIST "+errortype+" Error",error);
        }
    }

    private class Test extends XTTTest implements ActionListener
    {
        private JButton status=new JButton("SET");
        private JPopupMenu popup=null;
        private String testFileName="";
        private StringOutputStream log=new StringOutputStream();
        private String comments="";
        private String description="";
        private String level="";

        public JButton getButton(){return status;}
        public String getDescription(){return description;}
        public String getLevel(){return level;}
        
        private boolean isQueued = false;

        public Test(String filename, String comments, String description,String level)
        {
            super(filename);
            setFileName(filename);
            this.comments=comments;
            this.description=description;
            this.level=level;
            status.setPreferredSize(new Dimension(tableButtonwidth,16));//(int)status.getPreferredSize().getHeight()));
            status.setBackground(Color.GRAY);
            status.setOpaque(true);
            status.setHorizontalAlignment(status.CENTER);


            status.setActionCommand("CONFIG");
            status.addActionListener(this);

            popup = new JPopupMenu();

			JMenuItem menuItem = new JMenuItem("Run");
			menuItem.setActionCommand("RUN");
			menuItem.addActionListener(this);
			popup.add(menuItem);

			JMenu menu = new JMenu("Edit");

			menuItem = new JMenuItem("Test Launcher");
			menuItem.setActionCommand("EDITLAUNCHER");
			menuItem.addActionListener(this);
			menu.add(menuItem);

			menuItem = new JMenuItem("XML Editor");
			menuItem.setActionCommand("EDIT");
			menuItem.addActionListener(this);
			menu.add(menuItem);

			popup.add(menu);

            menuItem=new JMenuItem("Set");
            menuItem.setActionCommand("SET");
            menuItem.addActionListener(this);
            popup.add(menuItem);

            menuItem=new JMenuItem("Log");
            menuItem.setActionCommand("LOG");
            menuItem.addActionListener(this);
            popup.add(menuItem);

            menuItem=new JMenuItem("Remove");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }
        public void doQueue()
        {
            isQueued=true;
            this.setStatus(XTTProperties.UNATTEMPTED,"QUEUED");
        }
        public void unQueue()
        {
            if(isQueued)
            {
                this.setStatus(XTTProperties.UNATTEMPTED,"SET");
                isQueued=false;
            }
        }
        
        public void setStatus(int statusnum,String statustext)
        {
            //System.out.println(testFileName+" "+statustext);
            if(statusnum==XTTProperties.FAILED_WITH_CORE&&statustext==null)
            {
                status.setBackground(Color.RED);
                status.setText("CORE");
            } else if(statusnum>=XTTProperties.FAILED&&statustext==null)
            {
                status.setBackground(Color.RED);
                status.setText(XTTProperties.getStatusDescription(XTTProperties.FAILED));
            } else if(statusnum<=XTTProperties.PASSED&&statustext==null)
            {
                status.setBackground(Color.GREEN);
                status.setText(XTTProperties.getStatusDescription(XTTProperties.PASSED));
            } else if(statustext.equals("RUN"))
            {
                status.setBackground(Color.YELLOW);
                status.setText("CURRENT");
            } else if(statustext.equals("QUEUED"))
            {
                status.setBackground(Color.CYAN);
                status.setText("QUEUED");
            } else
            {
                status.setBackground(Color.GRAY);
                status.setText("SET");
            }
            status.repaint();
            listtablemodel.fireTableCellUpdated(testlist.indexOf(this),listtablemodel.BUTTON);
        }
        public void actionPerformed(ActionEvent e)
        {
            if(e.getActionCommand().equals("CONFIG"))
            {
                popup.show(status,0,0);
                popup.pack();
                popup.setPopupSize(status.getWidth(),popup.getComponent().getHeight());
            } else if(e.getActionCommand().equals("RUN"))
            {
                if(XTT.isTestRunning())
                {
                    showError("Run Error","Tests are already running, can't start until they finished!");
                    return;
                }
                xttgui.resetStatus();
                selectedtestlist=new Vector<Test>();
                Vector<XTTTest> list=new Vector<XTTTest>();
                this.resetTest();
                selectedtestlist.add(this);
                list.add(this);
                XTTProperties.setTestList(list);
                runTests();
            } else if(e.getActionCommand().equals("LOG"))
            {
                try
                {
                    LogViewer ed=new LogViewer(xttgui,getLog(),true);
                    ed.setTitle(shortFileName(getFileName())+" - "+ed.getTitle());
                } catch (java.lang.OutOfMemoryError ex)
                {
                    showError("Out of Memory","java.lang.OutOfMemoryError");
                }
            } else if(e.getActionCommand().equals("EDIT"))
            {
                TestEditor ed=new TestEditor(xttgui,getFileName(),this);
            } 
            else if(e.getActionCommand().equals("EDITLAUNCHER"))
            {
            	xttgui.runTestLauncher();
            	String testName ="";
        				try
        				{
        					if (TreeTestController.test!=null)
        					{
        						if (TreeTestController.test.isDirty())
        						{
        							try {
        								testName = TreeTestController.getTestFileName();
        							} catch (Exception e1) {
        								testName = TreeTestController.getTestName();
        							}
        							int ans = JOptionPane.showConfirmDialog(TreeTestController.getTestLauncherGui(),
        									"Do you want to save the changes you made \nin " + testName + " ?", "Save Confirmation",
        									JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        							if (ans == JOptionPane.YES_OPTION)
        							{
        								TreeTestController.test.save();
        							}
        							else if (ans == JOptionPane.NO_OPTION)
        								TreeTestController.test.setDirty(false);
        							else
        								return;
        						}
        					}
        						TreeTestController.openTest(new File (getFileName()));
        						TestLauncherGui.getFileTreePanel().removeSelectionListener();
        						TestLauncherGui.getFileTreePanel().expandToFile(TreeTestController.getTestFileNode());
        						TestLauncherGui.getFileTreePanel().addSelectionListener();
        						xttgui.getTestLauncher().setVisible(true);
        						
        					}
        				catch (Exception e1)
        				{
        					// TODO Auto-generated catch block
        					e1.printStackTrace();
        				}
            }
            else if(e.getActionCommand().equals("REMOVE"))
            {
                if(XTT.isTestRunning())
                {
                    showError("Run Error","Tests are running, can't remove until they finished!");
                    return;
                }
                testlist.remove(this);
                listtablemodel.fireTableDataChanged();
                xttgui.setListStatus(xttgui.LIST_EDITED);
            } else if(e.getActionCommand().equals("SET"))
            {
                if(XTT.isTestRunning())
                {
                    showError("Run Error","Tests are running, can't add until they finished!");
                    return;
                }
                try
                {
                    JFileChooser fc = new JFileChooser(".");
                    fc.setFileFilter(new XMLFileFilter());
                    //In response to a button click:
                    int returnVal = fc.showOpenDialog(xttgui);
                    if(returnVal==fc.APPROVE_OPTION)
                    {
                        File file = fc.getSelectedFile();
                        org.jdom.output.XMLOutputter outputter=new org.jdom.output.XMLOutputter();
                        org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
                        org.jdom.Document document=parser.build(file.getAbsolutePath());
                        if(!document.getRootElement().getName().toLowerCase().equals("test"))
                        {
                            String error="The outer node in the test file isn't called test";
                            showError("Test File Set Error",error);
                            return;
                        }
                        String oldfilename=this.testFileName;
                        setFileName(file.getAbsolutePath());
                        if(!oldfilename.equals(this.testFileName))
                        {
                            xttgui.setListStatus(xttgui.LIST_EDITED);
                        }
                        description=getFileDescription(document);

                    }
                } catch (Exception ex)
                {
                    showError("Test File Set Error",ex.getClass().getName()+"\n"+ex.getMessage());
                }
            }
        }

        public String toString()
        {
            return shortFileName(getFileName());
        }
        public String getFileName()
        {
            return this.testFileName;
        }
        public String getFolder()
        {
        	String path []=   this.testFileName.split("\\\\");
           return path[path.length-2];
        }
        public String getName()
        {
        	String path []=   this.testFileName.split("\\\\");
            return path[path.length-1].substring(0,path[path.length-1].indexOf(".xml"));
        }
        public String getComments()
        {
            return comments.toString();
        }

        public void setTestStatus(int status)
        {
            super.setTestStatus(status);
            this.setStatus(status,null);
        }
        public void setFileName(String filename)
        {
            super.setFileName(filename);
            this.testFileName=filename;
        }
        public void start()
        {
            isQueued=false;
            this.setStatus(XTTProperties.UNATTEMPTED,"RUN");
            setMemoryBar();
            setProgress();
            super.start();
        }

        public void end()
        {
            super.end();
            int numtest=0;
            if(selectedtestlist!=null)
            {
                numtest=selectedtestlist.size();
            } else
            {
                numtest=testlist.size();
            }
            xttgui.addTestStatus(this.getTestStatus(),numtest);
            if(this.getTestStatus()<=XTTProperties.PASSED)
            {
                this.setStatus(XTTProperties.PASSED,null);
            } else
            {
                this.setStatus(this.getTestStatus(),null);
            }
        }
        public void resetTest()
        {
            super.resetTest();
            this.setStatus(XTTProperties.PASSED,"");
            this.clearLog();
        }
        private void setProgress()
        {
            int currentposition=0;
            int currenttest=0;
            int numtest=0;
            if(selectedtestlist!=null)
            {
                numtest=selectedtestlist.size();
                currenttest=selectedtestlist.indexOf(this);
                currentposition=testlist.indexOf(this);
            } 
            else
            {
                numtest=testlist.size();
                currentposition=testlist.indexOf(this);
                currenttest=currentposition;
            }
            if(numtest>0)
            {
                int maxScroll=scrollport.getVerticalScrollBar().getMaximum();
                int visible=scrollport.getVerticalScrollBar().getVisibleAmount();
                int nextval=(currentposition+2)*maxScroll/testlist.size()-visible;
                int currentval=scrollport.getVerticalScrollBar().getValue();
                if(nextval<0)nextval=0;
                if(nextval>maxScroll)nextval=maxScroll;
                    scrollport.getVerticalScrollBar().setValue(nextval);
                    scrollport.revalidate();
                    scrollport.repaint();
            }
            progressBar.setMaximum(numtest);
            progressBar.setValue(currenttest);
            progressBar.setString(((int)(progressBar.getPercentComplete()*100.0))+"% - "+(currenttest+1)+"/"+numtest);
            progressBar.setIndeterminate(false);
        }
    }

    public String getFileDescription(org.jdom.Document document)
    {
        java.util.Iterator it=document.getRootElement().getChildren().iterator();
        org.jdom.Element el=null;
        while(it.hasNext())
        {
            el=(org.jdom.Element)it.next();
            if(el.getName().equalsIgnoreCase("description"))
            {
                //System.out.println(el.getText());
                return el.getText().split("\\r\\n|\\n")[0];
            }
        }
        return "";
        //*/
    }
    public String getFileLevel(org.jdom.Document document)
    {
        java.util.Iterator it=document.getRootElement().getChildren().iterator();
        org.jdom.Element el=null;
        while(it.hasNext())
        {
            el=(org.jdom.Element)it.next();
            if(el.getName().equalsIgnoreCase("testlevel"))
            {
                //System.out.println(el.getText());
                return el.getText().split("\\r\\n|\\n")[0];
            }
        }
        return "";
        //*/
    }
    private String shortFileName(String filename)
    {
        return ConvertLib.shortFileName(filename,currentFilePath);
    }

    public JComponent getMemoryBar()
    {
        setMemoryBar();
        return memory;
    }

    public void setMemoryBar()
    {
        memory.setOpaque(true);
        long free=(Runtime.getRuntime().freeMemory()+(Runtime.getRuntime().maxMemory()-Runtime.getRuntime().totalMemory()));
        String freemem="";
        if(free>1024*1024)
        {
            free=free/1024/1024;
            freemem=free+"MB";
        } else if (free>1024)
        {
            free=free/1024;
            freemem=free+"KB";
        } else
        {
            freemem=free+" B";
        }
        long max=Runtime.getRuntime().maxMemory()/1024/1024;
            if(free<10)
            {
                memory.setBackground(Color.RED);
            } else if(free<30)
            {
                memory.setBackground(Color.YELLOW);
            } else
            {
                memory.setBackground(Color.GREEN);
            }
        memory.setText(" MEM: "+freemem+" MAX: "+max+"MB ");
        memory.revalidate();
        memory.repaint();
    }


    private class LISTFileFilter extends javax.swing.filechooser.FileFilter
    {
        public boolean accept(java.io.File f)
        {
            if(f!=null)
            {
                if(f.isDirectory())return true;
                if(f.getName().toLowerCase().endsWith(".list"))return true;
            }
            return false;
        }
        public String getDescription()
        {
            return "LIST File Filter";
        }
    }

    public class ListTableModel extends AbstractTableModel
    {
        public final int BUTTON=0;
        public final int FOLDER=1;
        public final int FILENAME=2;
        public final int LEVEL=3;
        public final int PATH=4;
        public final int DESC=5;
        public int getColumnCount()
        {
            return 6;
        }
        public int getRowCount()
        {
            return testlist.size();
        }
        public Object getValueAt(int row, int col)
        {
            Test crnt=testlist.get(row);
            switch(col)
            {
                case BUTTON:
                    return crnt.getButton();
                case FOLDER:
                    return crnt.getFolder();
                case LEVEL:
                    return crnt.getLevel();
                case FILENAME:
                    return crnt.getName();
                case PATH:
                    return shortFileName(crnt.getFileName());
                case DESC:
                    return crnt.getDescription();
                default:
                    return null;
            }

        }
        public String getColumnName(int col)
        {
            switch(col)
            {
                case BUTTON:
                    return "Action";
                case FOLDER:
                    return "Feature";
                case FILENAME:
                    return "Name";    
                case LEVEL:
                    return "Level";     
                case PATH:
                    return "Path";     
                case DESC:
                    return "Description";
                default:
                    return "";
            }
        }

        public boolean isCellEditable(int row, int col)
        {
            if(col==BUTTON)return true;
            return false;
        }

    }

    private class ListTable extends JTable
    {
        public ListTable(ListTableModel m)
        {
            super(m);
        }
        public String getToolTipText(MouseEvent e)
        {
            String tip = null;
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);
            int realColumnIndex = convertColumnIndexToModel(colIndex);
            Test crnt=testlist.get(rowIndex);

            if(realColumnIndex==listtablemodel.BUTTON)
            {
                    tip = "Push for Menu";
            } else if(realColumnIndex==listtablemodel.FILENAME)
            {
                    tip=crnt.getFileName();
            } else if(realColumnIndex==listtablemodel.DESC)
            {
                    tip=crnt.getDescription();
                    if(tip.equals(""))
                    {
                        tip="Add description with <description> tag to test";
                    }
            } else
            {
                tip = super.getToolTipText(e);
            }
            return tip;
        }
        public void tableChanged(javax.swing.event.TableModelEvent e) 
        {
            this.editingStopped(null);
            super.tableChanged(e);
        }
    }

    private class TableButton extends AbstractCellEditor implements TableCellEditor,TableCellRenderer
    {

        public Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected, boolean hasFocus,int row, int column)
        {
            return testlist.get(row).getButton();
        }

        //Implement the one CellEditor method that AbstractCellEditor doesn't.
        public Object getCellEditorValue()
        {
            return new JButton();
        }

        //Implement the one method defined by TableCellEditor.
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            return testlist.get(row).getButton();
        }
    }

}
