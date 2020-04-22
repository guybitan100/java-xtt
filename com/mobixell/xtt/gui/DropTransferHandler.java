package com.mobixell.xtt.gui;

import com.mobixell.xtt.XTT;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.TestList;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import java.awt.datatransfer.*;

import java.util.List;

public class DropTransferHandler extends TransferHandler implements Transferable
{
	private static final long serialVersionUID = 6715933315429380798L;
	List<File> data=null;
    int[] selectedRows=new int[0];
    
    public static final String tantau_sccsid = "@(#)$Id: DropTransferHandler.java,v 1.6 2008/02/19 09:11:32 rsoder Exp $";
    private TestList testlist=null;
    public DropTransferHandler(TestList testlist)
    {
        super();
        this.testlist=testlist;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////    Import Stuff   /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
    {
        if(XTT.isTestRunning())
        {
            return false;
        }
        for(int i=0;i<transferFlavors.length;i++)
        {
            if(transferFlavors[i].isFlavorJavaFileListType())//transferFlavors[i].getMimeType().equals("application/x-java-file-list; class=java.util.List"))
            {
                return true;
            }
        }
        return false;
    }
    public boolean importData(JComponent comp, Transferable t) 
    {
        try
        {
            if(testlist.checkDragSource(comp)&&selectedRows.length>0)
            {
                XTTProperties.printDebug("Moving test list with "+selectedRows.length+" entries");
                return testlist.moveEntries(selectedRows);
            } else if(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
            {
                java.util.List filelist=(java.util.List )t.getTransferData(DataFlavor.javaFileListFlavor);
                
                File[] o=(File[])filelist.toArray();
                XTTProperties.printDebug("Importing file list with "+o.length+" entries");
                testlist.addTests(o,true);
                return true;
            }
        } catch (UnsupportedFlavorException ufe)
        {
            XTTProperties.printException(ufe);
        } catch (IOException ufe)
        {
            XTTProperties.printException(ufe);
        }
        return false;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////    Export Stuff   /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected Transferable createTransferable(JComponent c)
    {
        data=testlist.getSelectedFileList();
        selectedRows=testlist.getSelectedRows();
        XTTProperties.printDebug("Exporting file list with "+data.size()+" entries");
        return (Transferable)this;
    }
    public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action) 
    {
        //System.out.println("exportAsDrag "+action+"\n"+comp+"\n"+e);
        super.exportAsDrag(comp,e,action);
    }
    
    public int getSourceActions(JComponent c) 
    {
        return TransferHandler.COPY;
        //return TransferHandler.COPY_OR_MOVE;
    }
    protected void exportDone(JComponent source, Transferable data, int action)
    {
        selectedRows=new int[0];
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////    Transferable Stuff   /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
    {
        if (flavor.equals(DataFlavor.javaFileListFlavor) )
        {
	        return (Object)data;
	    } else 
	    {
	        throw new UnsupportedFlavorException(flavor);
	    }   
	}
    public DataFlavor[] getTransferDataFlavors()
    {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor};
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return DataFlavor.javaFileListFlavor.equals(flavor);
    }
}
