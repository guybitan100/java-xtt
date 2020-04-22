package com.mobixell.xtt.gui.testlaunch.panels;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.actionItems.AddTestToListAction;
import com.mobixell.xtt.gui.actionItems.CopyFileAction;
import com.mobixell.xtt.gui.actionItems.CopyFolderAction;
import com.mobixell.xtt.gui.actionItems.CreateFolderAction;
import com.mobixell.xtt.gui.actionItems.DeleteFileAction;
import com.mobixell.xtt.gui.actionItems.DeleteFolderAction;
import com.mobixell.xtt.gui.actionItems.PasteFileFolderAction;
import com.mobixell.xtt.gui.actionItems.RenameFileAction;
import com.mobixell.xtt.gui.main.TestList;
import com.mobixell.xtt.gui.testlaunch.SwingUtils;
import com.mobixell.xtt.gui.testlaunch.WaitDialog;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FileNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.FolderNode;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.TreeNodeRenderer;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode.NodeType;
import com.mobixell.xtt.images.ImageCenter;
/**
 * 
 * @version $Id: FileTree.java,v 1.9 20011/08/23 03:39:22 ian Exp $
 * @author Guy Bitan
 */
public class RunnerFileTreePanel extends JPanel implements KeyListener,MouseListener,CellEditorListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8273177178239311737L;
	/**
	 * 
	 */
	private JTree tree;
	public final  File testsLocation = new File(System.getProperty("user.dir")+"/tests/XMP");
	public  FileTreeSelectionListener treeSelectionListener ;
	private JPopupMenu popupMenu = new JPopupMenu();
	private  String strFileToCopyName = null;
	private  String strFileToCopyPath = null;
	private  String strFolderToCopyName =null;
	private  String strFolderToCopyPath =null;
	private  String prefix = null;
	public static TestList testList = null;
	public  File f1 = null;
	public  File f2 = null;
	/**
	 * Construct a FileTree
	 * 
	 * @throws UnsupportedLookAndFeelException
	 */
	public RunnerFileTreePanel(TestList testList) {
		this.testList=testList;
		 setLayout(new BorderLayout());
		    initTree();
			JScrollPane treeView=SwingUtils.getJScrollPaneWithWaterMark(ImageCenter.getInstance().getAwtImage(
	        		ImageCenter.ICON_TEST_TREE_BG), tree);
			// Lastly, put the JTree into a JScrollPane.
			JScrollPane scrollpane = new JScrollPane();
			scrollpane.getViewport().add(treeView);
			add(BorderLayout.CENTER, scrollpane);
	}
	public void removeSelectionListener()
	{
		tree.removeTreeSelectionListener(treeSelectionListener);
	}
	public void addSelectionListener()
	{
		tree.addTreeSelectionListener(treeSelectionListener);
		tree.requestFocus();
	}
	public void initTree()
	{
		tree = new JTree(new DefaultTreeModel(addNodes(null, testsLocation)));
		treeSelectionListener=new FileTreeSelectionListener();
		tree.addTreeSelectionListener(treeSelectionListener);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new TreeNodeRenderer());
		tree.addKeyListener(this);
		tree.addMouseListener(this);
		tree.setSelectionPath(getRootPath());
		tree.setCellEditor(new DefaultCellEditor(new JTextField("")));
		tree.getCellEditor().addCellEditorListener(this); 
	}

	public void refreshTree(boolean isSelect) {
		TreePath lastPath = tree.getSelectionPath();
		tree.setModel(new DefaultTreeModel(addNodes(null, testsLocation)));

		if (isSelect) {
			tree.scrollPathToVisible(lastPath);
			tree.setSelectionPath(lastPath);
		}
	}
	public TreePath getRootPath()
	{
		return new TreePath (tree.getModel().getRoot());
	}
	public  void expandToFile(FileNode fileNode) throws Exception
	{
		expandToFile(fileNode,getRootPath());
	}
	public  void expandToFile(FileNode fileNode, TreePath parent) throws Exception
	{
		// Traverse children
		AssetNode node = (AssetNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode n = (AssetNode) e.nextElement();
				if (n.getType().equals(NodeType.FOLDER))
				{
					TreePath path = parent.pathByAddingChild(n);
					expandToFile(fileNode, path);
				}
				if (n.getType().equals(NodeType.FILE))
				{
					if (((FileNode) n).getFileFullPath().equalsIgnoreCase(fileNode.getFileFullPath()))
					{
						tree.expandPath(parent);
						tree.setSelectionPath(new TreePath(n.getPath()));
						tree.scrollPathToVisible(new TreePath(n.getPath()));
						return;
					}
				}
			}
		}
	}
	public  void expandToFolder(FolderNode folderNode) throws Exception
	{
		expandToFolder(folderNode,getRootPath());
	}
	public  void expandToFolder(FolderNode folderNode,TreePath parent) throws Exception
	{
	
		if (folderNode==null || parent==null) return;
	
		if (folderNode.getFolderPath().equals(((FolderNode)parent.getLastPathComponent()).getFolderPath()))
		{
			tree.expandPath(parent);
			tree.setSelectionPath(parent);
			return;
		}
		
		// Traverse children
		AssetNode node = (AssetNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				AssetNode n = (AssetNode) e.nextElement();
				if (n.getType().equals(NodeType.FOLDER)) 
					if (((FolderNode) n).getFolderPath().equalsIgnoreCase(folderNode.getFolderPath()))
				{
					tree.expandPath(parent);
					tree.setSelectionPath(new TreePath(n.getPath()));
					tree.scrollPathToVisible(new TreePath(n.getPath()));
					return;
				}
				else
				{
					TreePath path = parent.pathByAddingChild(n);
					expandToFolder(folderNode,path);
				}
			}
		}
	}
	/** Add nodes from under "dir" into curTop. Highly recursive. 
	 * @throws Exception */
	 DefaultMutableTreeNode addNodes(FolderNode curTop, File dir)
	{
		String curPath = dir.getPath();
		FolderNode curDir = new FolderNode(dir.getName(), curPath);
		if (curTop != null)
		{ // should only be null at root
			curTop.add(curDir);
		}
		Vector<String> ol = new Vector<String>();
		String[] tmp = dir.list();
		for (int i = 0; i < tmp.length; i++)
			if (!tmp[i].startsWith(".svn"))
			{
				ol.addElement(tmp[i]);
			}
		Collections.sort(ol, String.CASE_INSENSITIVE_ORDER);
		File f;
		Vector<String> files = new Vector<String>();
		// Make two passes, one for Dirs and one for Files. This is #1.
		for (int i = 0; i < ol.size(); i++)
		{
			String thisObject = (String) ol.elementAt(i);
			String newPath;
			if (curPath.equals(".")) newPath = thisObject;
			else newPath = curPath + File.separator + thisObject;
			if ((f = new File(newPath)).isDirectory()) addNodes(curDir, f);
			else if ((f = new File(newPath)).isFile() && (f = new File(newPath)).getName().endsWith(".xml")||(f = new File(newPath)).getName().endsWith(".list")) 
			{
				files.addElement(thisObject);
			}
		}
		// Pass two: for files.
		for (int fnum = 0; fnum < files.size(); fnum++)
		{
				FileNode fileNode =new FileNode(files.elementAt(fnum), curPath + "\\" + files.elementAt(fnum), curPath);
				if (files.elementAt(fnum).endsWith(".list"))
				{
					fileNode.setFileType("list");
				}
				curDir.add(fileNode);
		}
		return curDir;
	}
	private void performAction(MouseEvent me) throws Exception
	{
		int x = me.getX();
		int y = me.getY();
		// save the path for future use
		TreePath clickedPath = tree.getPathForLocation(x, y);
		if (clickedPath == null) return;
		// save the selected node
		AssetNode currentNode = (AssetNode) clickedPath.getLastPathComponent();
		if (me.getButton() == MouseEvent.BUTTON3)
		{
			popupMenu.removeAll();
			if (tree.getSelectionCount() == 1)
			{
				if (currentNode.isType(NodeType.FILE)) 
				{
					popupMenu.add(AddTestToListAction.getInstance());
					if (strFileToCopyName != null || strFolderToCopyName != null)
					popupMenu.add(PasteFileFolderAction.getInstance(this));
					popupMenu.add(RenameFileAction.getInstance(this));
					popupMenu.add(CopyFileAction.getInstance(this));
					popupMenu.add(DeleteFileAction.getInstance(this));
					popupMenu.show(tree, x, y);
				}
				if (currentNode.isType(NodeType.FOLDER)) {
					if (strFileToCopyName != null || strFolderToCopyName != null)
					popupMenu.add(PasteFileFolderAction.getInstance(this));
					popupMenu.add(CopyFolderAction.getInstance(this));
					popupMenu.add(CreateFolderAction.getInstance(this));
					popupMenu.add(DeleteFolderAction.getInstance(this));
					if (strFileToCopyName != null)
					popupMenu.add(DeleteFolderAction.getInstance(this));
					popupMenu.show(tree, x, y);
				}
			}
			else
			{
				if (currentNode.isType(NodeType.FILE))
				{
					popupMenu.add(DeleteFileAction.getInstance(this));
					popupMenu.show(tree, x, y);
				}
			}
		}
		if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount()==2)
		{
			addTestToList();
		}
	}
	public String getSelectedPath(Boolean isFullPath)
	{
		try
		{
				if (((AssetNode) tree.getSelectionPath().getLastPathComponent()).getType() == NodeType.FOLDER)
				{
					return ((FolderNode) tree.getSelectionPath().getLastPathComponent()).getFolderPath();
				}
				else
				{
					if (isFullPath)
						return ((FileNode) tree.getSelectionPath().getLastPathComponent()).getFileFullPath();
					else
						return ((FileNode) tree.getSelectionPath().getLastPathComponent()).getFilePath();
			}
		} catch (NullPointerException e) {
			return null;
		}
	}
	public  AssetNode getSelectedNode()
	{
		try
		{
			if (tree.getSelectionPath() != null)
			{
				if (((AssetNode) tree.getSelectionPath().getLastPathComponent()).getType() == NodeType.FOLDER)
				{
					return ((FolderNode) tree.getSelectionPath().getLastPathComponent());
				}
				else
				{
					return ((FileNode) tree.getSelectionPath().getLastPathComponent());
				}
			}	
		}
		catch (NullPointerException e)
		{
			return null;
		}
		return null;
	}
	public void createFolder()
	{
		String folderName = (String) JOptionPane.showInputDialog(null,"Folder name","New Folder",JOptionPane.INFORMATION_MESSAGE,UIManager.getIcon("Tree.closedIcon"),null,null);
		
		if(folderName!="" && folderName!=null)
		
			createFolder(folderName);
		try
		{
			expandToFolder(new FolderNode(folderName,((FolderNode)getSelectedNode()).getFolderPath()+"\\"+folderName));
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public  void createFolder(String folderName)
	{
		String selectedPath;
		
		if (tree.getSelectionPath() != null)
		{
			if (((AssetNode) tree.getSelectionPath().getLastPathComponent()).getType() == NodeType.FOLDER)
			{
				selectedPath=((FolderNode) tree.getSelectionPath().getLastPathComponent()).getFolderPath();
			}
			else
			{
				selectedPath=((FolderNode) tree.getSelectionPath().getParentPath().getLastPathComponent()).getFolderPath();
			}
			File file = new File(selectedPath+"\\"+folderName+"\\");
			file.mkdir();
			refreshTree(true);
		}	
	}
	public  void renameFile(boolean isEditNode)
	{
		if (getSelectedNode().isType(NodeType.FILE))
		 {
			tree.setEditable(true);
			if (isEditNode)
			tree.startEditingAtPath(new TreePath(getSelectedNode().getPath()));
		 }
	}
	public  void copySourceFile()
	{
		prefix= "";
		strFolderToCopyName=null;
		strFolderToCopyPath=null;
		strFileToCopyName = ((FileNode) tree.getSelectionPath().getLastPathComponent()).getName();
		strFileToCopyPath = ((FileNode) tree.getSelectionPath().getLastPathComponent()).getFilePath();
	}
	public  void copySourceFolder()
	{
		prefix= "";
		strFileToCopyName = null;
		strFileToCopyPath=null;
		strFolderToCopyName = ((FolderNode) tree.getSelectionPath().getLastPathComponent()).getName();
		strFolderToCopyPath = ((FolderNode) tree.getSelectionPath().getLastPathComponent()).getFolderPath();
	}
	private  String getDestFileFolderPath()
	{
		AssetNode node = (AssetNode)tree.getSelectionPath().getLastPathComponent();
		String strDestFolderFilePath = null;
		prefix += "CopyOf";
		if (node.isType(NodeType.FOLDER))
		{
			if (strFileToCopyName!=null)
				strDestFolderFilePath = ((FolderNode) node).getFolderPath() + "\\" +prefix+strFileToCopyName;
			else
				strDestFolderFilePath = ((FolderNode) node).getFolderPath() + "\\" +prefix+strFolderToCopyName;	
		}
		if (node.isType(NodeType.FILE))
		{
			if (strFileToCopyName!=null)
			strDestFolderFilePath = ((FileNode) node).getFilePath() + "\\" +prefix+ strFileToCopyName;
			else
			strDestFolderFilePath = ((FileNode) node).getFilePath() + "\\" +prefix+ strFolderToCopyName;
		}
		return strDestFolderFilePath;
	}
	public  void pasteFileFolder()
	{
		tree.removeTreeSelectionListener(treeSelectionListener);	
		String strDestPath = null;
			strDestPath = getDestFileFolderPath();
			if (strFileToCopyPath!=null && strFileToCopyName!=null)
			{
				f1 = new File(strFileToCopyPath+"\\" +strFileToCopyName);
				f2 = new File(strDestPath);
				
				while (f2.exists())
				{
					strDestPath = getDestFileFolderPath();
					f2 = new File(strDestPath);
				}
				(new Thread() {
					public void run() {
						try {
							try {
								WaitDialog.launchWaitDialog("Copy To: "  + f2, null);
								pasteFile(f1, f2);
								refreshTree(false);
								expandToFile(new FileNode(f2.getName(), f2.getAbsolutePath(), f2.getPath()));
							} catch (Exception e1) {
								XTTProperties.printFail("Fail to copy file");
							}
						} finally {
							WaitDialog.endWaitDialog();
							tree.addTreeSelectionListener(treeSelectionListener);
						}
					}
				}).start();				
			}
			else
			{
				f1 = new File(strFolderToCopyPath);
				f2 = new File(strDestPath);
				
				while (f2.exists())
				{
					strDestPath = getDestFileFolderPath();
					f2 = new File(strDestPath);
				}
					(new Thread() {
						public void run() {
							try {
								try {
									WaitDialog.launchWaitDialog("Copy To: " + f2, null);
									pasteFolder(f1, f2);
									refreshTree(false);
									expandToFolder(new FolderNode(f2.getName(), f2.getPath()));
								} catch (Exception e1) {
									XTTProperties.printFail("Fail to copy folder");
								}
							} finally {
								WaitDialog.endWaitDialog();
								tree.addTreeSelectionListener(treeSelectionListener);
							}
						}
					}).start();		
			}
	}
	public  void pasteFolder(File src, File dest)throws IOException
	{
	
	    	if(src.isDirectory()){
	 
	    		//if directory not exists, create it
	    		if(!dest.exists()){
	    		   dest.mkdir();
	    		}
	 
	    		//list all the directory contents
	    		String files[] = src.list();
	 
	    		for (String file : files) {
	    		   //construct the src and dest file structure
	    		   File srcFile = new File(src, file);
	    		   File destFile = new File(dest, file);
	    		   //recursive copy
	    		   pasteFolder(srcFile,destFile);
	    		}
	 
	    	}else{
	    		//if file, then copy it
	    		//Use bytes stream to support all file types
	    		InputStream in = new FileInputStream(src);
	    	        OutputStream out = new FileOutputStream(dest); 
	 
	    	        byte[] buffer = new byte[1024];
	 
	    	        int length;
	    	        //copy the file content in bytes 
	    	        while ((length = in.read(buffer)) > 0){
	    	    	   out.write(buffer, 0, length);
	    	        }
	 
	    	        in.close();
	    	        out.close();
	    	}
	    	
	    }
	public  void pasteFile(File f1,File f2)
	{
		try
		{
			InputStream in = new FileInputStream(f1);
			OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}
		catch (FileNotFoundException ex)
		{
			XTTProperties.printDebug(ex.getMessage() + " in the specified directory.");
		}
		catch (IOException e)
		{
			XTTProperties.printDebug(e.getMessage());
		}
	}
	public  void deleteFileOrFolder()
	{
		AssetNode node = null;
		tree.removeTreeSelectionListener(treeSelectionListener);
		try
		{
			TreePath[] pathItems = tree.getSelectionPaths();
			for (int ind = 0; ind < pathItems.length; ind++)
			{
			node = (AssetNode) pathItems[ind].getLastPathComponent();
			if (node != null)
			{
				if (node.isType(NodeType.FILE))
				{
					String path = ((FileNode) node).getFileFullPath();
					File file = new File(path);
					if (file.exists())
					{
						Object[] options = { "Yes, delete", "No, cancel" };
						int n = JOptionPane.showOptionDialog(null, "Are you sure you want to delete this test?\n"
								+ file.getCanonicalPath(), "Confirme delete test", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
						if (n == JOptionPane.YES_OPTION)
						{
							WaitDialog.launchWaitDialog("delete file: " + file, null);
							file.delete();
							refreshTree(false);
							expandToFolder((FolderNode)(node.getParent()));
							WaitDialog.endWaitDialog();
						}
					}
				}
				else if  (node.isType(NodeType.FOLDER))
				{
					String path = ((FolderNode) node).getFolderPath();
					File dir = new File(path);
					if (dir.exists())
					{
						Object[] options = { "Yes, delete", "No, cancel" };
						int n = JOptionPane.showOptionDialog(null, "Are you sure you want to delete this folder?\n"
								+ dir.getCanonicalPath(), "Confirme delete folder", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
						if (n == JOptionPane.YES_OPTION)
						{
							WaitDialog.launchWaitDialog("delete folder: " + dir, null);
							deleteDir(dir);
							refreshTree(false);
							expandToFolder((FolderNode)(node.getParent()));
							WaitDialog.endWaitDialog();
						}
						}
					}
				}
			}
			tree.addTreeSelectionListener(treeSelectionListener);
		}
		catch (Exception ex)
		{
			showError("Delete Error", ex.getClass().getName() + "\n" + ex.getMessage());
		}
	}
	public  boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                        boolean success = deleteDir(new File(dir, children[i]));
                        if (!success) {
                                return false;
                        }
                }
        }
        return dir.delete();
}
	public void keyPressed(KeyEvent e)
	{	
		if (e.getKeyCode() == KeyEvent.VK_DELETE)
		{
				deleteFileOrFolder();
		}
		else if (e.getKeyCode() == KeyEvent.VK_F5)
		{
			refreshTree(true);
		}
		else if (e.getKeyCode() == KeyEvent.VK_F2)
		{
			renameFile(false);
		}
		else if ((e.getKeyCode() == KeyEvent.VK_C) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
			e.setKeyCode(44);
			if (getSelectedNode().isType(NodeType.FOLDER))
			{
				copySourceFolder();
			}
			else if (getSelectedNode().isType(NodeType.FILE))
			{
				copySourceFile();
			}
		}
		else if ((e.getKeyCode() == KeyEvent.VK_V)
				&& ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) 
		{
			pasteFileFolder();
		}
		
	}
	private void showError(String errortitle,String errortext)
	{
	        JOptionPane.showMessageDialog(null,
	        errortext,
	        errortitle,
	        JOptionPane.ERROR_MESSAGE);
	}
	
	class FileTreeSelectionListener implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent e)
		{
			
		}
	}
	public void mousePressed(MouseEvent e) {
		try {
			performAction(e);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void editingStopped(ChangeEvent e) 
	{
		
		if (getSelectedNode().isType(NodeType.FILE)) 
		{
			tree.removeTreeSelectionListener(treeSelectionListener);
			String newName = getSelectedNode().getUserObject().toString();

			if (!newName.endsWith(".xml")) 
			{
				showError("File Error","The file name shuld me XML file !");
				tree.getCellEditor().cancelCellEditing();
			} 
			else 
			{
               
				FileNode selectedNode = (FileNode)getSelectedNode();
				String newPath = selectedNode.getFilePath() + "\\" + newName;
				File oldFile = new File(selectedNode.getFileFullPath());
				File newFile = new File(newPath);
				if (newFile.exists())
				{
					newName = "CopyOf" + newName;
					newPath = selectedNode.getFilePath() + "\\" + newName;
					newFile = new File(selectedNode.getFilePath() + "\\" + newName);
				}	
					pasteFile(oldFile, newFile);
					oldFile.delete();
					selectedNode.setName(newName);
					selectedNode.setFullPath(newPath);
					DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();
					dtm.nodeChanged(selectedNode);
					dtm.reload();
					tree.setSelectionPath(new TreePath(selectedNode.getPath()));	
			}
			tree.addTreeSelectionListener(treeSelectionListener);
			refreshTree(true);
			
		}
	}

	public void addTestToList() 
	{
		if (getSelectedNode().isType(NodeType.FILE)) 
		{
			File file = new File(((FileNode) getSelectedNode()).getFileFullPath());
			
			if (file.exists()) 
			{
				if (file.getAbsolutePath().endsWith(".list")) 
				{
					testList.loadFile(file.getAbsolutePath());
				} 
				else
					testList.prepareAndAddTest(new File[] { file });
			}
		}
	}
    public void editingCanceled(ChangeEvent e)  {}  
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	public void mouseClicked(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
}