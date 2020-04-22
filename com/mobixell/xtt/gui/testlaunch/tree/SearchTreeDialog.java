package com.mobixell.xtt.gui.testlaunch.tree;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdom.Document;
import com.mobixell.xtt.images.ImageCenter;



public class SearchTreeDialog extends JDialog implements WindowListener,KeyListener
{
	private static final long serialVersionUID = 1L;
	public Document document = null;
    private JButton findNextButton;
    private JButton findAllButton;
    private String findNextString = "Find Next";
    private String findAllString = "Find All";
    JTextField textArea;
    private JFrame parent;
    String strToFind="";
    private JPanel checkPanel;
    private JRadioButton upCheck ;
    private JRadioButton downCheck ;
    public static SearchTreeDialog dialog = null;
    TestMap test;
    TreePath[] sp =null;
    private static int nextIndex;
    
  public SearchTreeDialog (JFrame parent)
  {
		Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_SEARCH));
		this.setIconImage(im);
		
	    nextIndex=0;
	    addWindowListener(this);
	    this.test= TreeTestController.test;
		setTitle("Find");
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		addKeyListener(this);
		initComponents();
  }
  
  public static void showSearchTreeDialog(JFrame parent) throws Exception
	{
		if (dialog == null)
		{
			
		dialog = new SearchTreeDialog(parent);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		}
		dialog.setVisible(true);	
	}
  private void initComponents() 
  {
	  checkPanel = new JPanel(new BorderLayout());
	  checkPanel.setBorder(BorderFactory.createTitledBorder(""));
	  JPanel checkButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
	  JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
	  buttonsPanel.setBorder(BorderFactory.createTitledBorder(""));
	  ButtonGroup group = new ButtonGroup();
	  
	   upCheck=new JRadioButton("Up");
	   upCheck.setToolTipText("Search Up");
	   upCheck.setOpaque(false);

	   downCheck=new JRadioButton("Down");
	   downCheck.setToolTipText("Search Down");
	   downCheck.setOpaque(false);
	   downCheck.setSelected(true);
	   
	   group.add(upCheck);
	   group.add(downCheck);
	   
	   checkPanel.add(upCheck,BorderLayout.NORTH);
	   checkPanel.add(downCheck,BorderLayout.CENTER);
	   
	   findNextButton = new JButton(findNextString);
	   findAllButton = new JButton(findAllString);
	   
	   buttonsPanel.add(findNextButton);
	   buttonsPanel.add(findAllButton);
	   
	   findNextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				findElement();
			}
		});
	   findAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				findAllElements();
			}
		});
		JPanel panelAll = (JPanel) getContentPane();
		panelAll.setLayout(new BorderLayout());
		panelAll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		    textArea = new JTextField();
		    textArea.addKeyListener(this);
		    textArea.setColumns(20);
		    textArea.setEditable(true);
		    textArea.setDragEnabled(true);
		    textArea.setInheritsPopupMenu(true);
		    textArea.addKeyListener(this);
		    
			checkButtonsPanel.add(checkPanel);
			checkButtonsPanel.add(buttonsPanel);
			
			panelAll.add(textArea, BorderLayout.NORTH);
			panelAll.add(checkButtonsPanel, BorderLayout.CENTER);
			
			pack();
			setLocationRelativeTo(parent);
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
  
	public void findAllElements()
	{
		strToFind = textArea.getText();
		if (strToFind != null && strToFind != "")
		{
			nextIndex=0;		
			fillElements();

		if (sp == null)
		{
				JOptionPane.showMessageDialog(null, "Can't find the text: " + strToFind);
		}
		else
		{
			if (sp.length == 0)
			{
				JOptionPane.showMessageDialog(null, "Can't find the text: " + strToFind);
			}
			else
			{
				test.setSelectionPaths(sp);
				setVisible(false);
			}
		}
		}
	}

	public void findElement()
	{
		strToFind = textArea.getText();
		if (strToFind != null && !strToFind.equalsIgnoreCase(""))
		{
			fillElements();
		
		if (sp == null)
		{
			JOptionPane.showMessageDialog(null, "Can't find the text: " + strToFind);
		}
		else
		{
			if (sp.length == 0)
			{
				JOptionPane.showMessageDialog(null, "Can't find the text: " + strToFind);
			}
			else
			{
				if (downCheck.isSelected()) 
					if (nextIndex < sp.length) 
					{
						int index = nextIndex++;
						test.getTree().scrollPathToVisible(sp[index]);
						test.setSelectionPath(sp[index]);
					}
					else
						nextIndex =0;
				
				if (upCheck.isSelected()) 
					if (nextIndex > 0) 
					{
						int index = --nextIndex;
						test.getTree().scrollPathToVisible(sp[index]);
						test.setSelectionPath(sp[index]);
					}
			}
		}
		}
		else
			nextIndex=0;
	}

	public void fillElements()
	{
		if (strToFind != null && !strToFind.equalsIgnoreCase(""))
		{
			TreeNode root = (TreeNode)test.getModel().getRoot();
			ArrayList<TreePath> selectPaths = new ArrayList<TreePath>();
			find2(new TreePath(root), strToFind, 0, selectPaths);
			sp = new TreePath[selectPaths.toArray().length];

			for (int i = 0; i < selectPaths.toArray().length; i++)
				sp[i] = selectPaths.get(i);
		}
		else
			nextIndex=0;
 	} 
	private void find2(TreePath parent, String strToFind,int index,ArrayList<TreePath> selectPaths) {
				    TreeNode node = (TreeNode)parent.getLastPathComponent();
				    Object o = node;

				    // If by name, convert node to a string
				        o = o.toString();

				    // If equal, go down the branch
				    if (o.toString().toLowerCase().indexOf(strToFind.toLowerCase())>=0) {
				    	selectPaths.add(parent);
				        // If at end, return match
				        //if (depth == nodes.length-1) {
				            //return parent;
				        //}

				    }
				    else if (node.getChildCount() >= 0) 
				        {
				            for (Enumeration <?>e=node.children(); e.hasMoreElements(); ) {
				                TreeNode n = (TreeNode)e.nextElement();
				                TreePath path = parent.pathByAddingChild(n);
				                find2(path, strToFind,index,selectPaths);
				               
				            }
				        }

}
public void windowClosing(WindowEvent e)
{
	nextIndex=0;
	dispose();
	
}
public void keyPressed(KeyEvent e)
{
	if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
		findElement();
		}
}
public void windowClosed(WindowEvent e){}
public void windowIconified(WindowEvent e){}
public void windowDeiconified(WindowEvent e){}
public void windowActivated(WindowEvent e){}
public void windowDeactivated(WindowEvent e){}
public void keyTyped(KeyEvent e){}
public void keyReleased(KeyEvent e){}
public void windowOpened(WindowEvent e){}
}