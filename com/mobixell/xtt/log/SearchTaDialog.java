package com.mobixell.xtt.log;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jdom.Document;

import com.mobixell.xtt.gui.ConfigurationEditor;
import com.mobixell.xtt.gui.JEditTextArea;


public class SearchTaDialog extends JDialog implements WindowListener,KeyListener
{
	private static final long serialVersionUID = 1L;
	public Document document = null;
    private JButton findNextButton;
    private static String findNextString = "Find Next";
    JTextField textArea;
    private LogViewer logparent;
    private ConfigurationEditor confparent;
    String strToFind="";
    String strTaLog ="";
    JEditTextArea editTa = null;
    int postionFirst = 0;
    int postionFound =0;
    
  public SearchTaDialog (LogViewer parent)
  {
	    super(parent);
	    this.addWindowListener(this);
	    this.logparent = parent;
		setTitle("Find");
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		initComponents();
		setVisible(true);
  }
  public SearchTaDialog (ConfigurationEditor parent)
  {
	    super(parent);
	    this.addWindowListener(this);
	    this.confparent = parent;
		setTitle("Find");
		setModalityType(ModalityType.DOCUMENT_MODAL);
		setResizable(false);
		initComponents();
		setVisible(true);
  }
  private void initComponents() {
	  
	   findNextButton = new JButton(findNextString);
	   if (logparent!=null)
		   initSearchLogPostions();
			else if (confparent!=null)
				initSearchConfPostions();
	  
	   JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
	   findNextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				if (logparent!=null)
				searchInLog();
				else if (confparent!=null)
					searchInConf();
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

		buttonsPanel.add(findNextButton);
		panelAll.add(textArea, BorderLayout.NORTH);
		panelAll.add(buttonsPanel, BorderLayout.CENTER);
		
		pack();
		setLocationRelativeTo(logparent);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
  
  public void searchInLog()
  {
	  strToFind = textArea.getText();
		if (strToFind != null  && strToFind !="") 
		{
		    postionFound =strTaLog.indexOf(strToFind, postionFirst);
		    
		    	if (postionFound>0)
		    	{
		    		postionFirst=postionFound + +strToFind.length();
		    		logparent.getTaLog().select(postionFound,postionFirst);
		    	}
		    	else
		    	{
		    		JOptionPane.showMessageDialog(logparent, "Can't find the text: " + strToFind);	
		    	}
			}
		    
		else 
			return;
  }
  public void searchInConf()
  {
	  strToFind = textArea.getText();
		if (strToFind != null  && strToFind !="") 
		{
		    postionFound =strTaLog.indexOf(strToFind, postionFirst);
		    
		    	if (postionFound>0)
		    	{
		    		postionFirst=postionFound + +strToFind.length();
		    		confparent.getTaConf().select(postionFound,postionFirst);
		    	}
		    	else
		    	{
		    		JOptionPane.showMessageDialog(confparent, "Can't find the text: " + strToFind);	
		    	}
			}
		    
		else 
			return;
  }
 public void initSearchLogPostions()
 {
	   editTa = logparent.getTaLog();
	   strTaLog = logparent.getTaLog().getText();
	   postionFirst =0;
	   postionFound=0;
 }
 
 public void initSearchConfPostions()
 {
	   editTa = confparent.getTaConf();
	   strTaLog = confparent.getTaConf().getText();
	   postionFirst =0;
	   postionFound=0;
 }
  public void actionPerformed(ActionEvent e) 
	{
	  if (logparent!=null)
			initSearchLogPostions();
			else if (confparent!=null)
				initSearchConfPostions();
	  
	  
		dispose();
	}

@Override
public void windowOpened(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void windowClosing(WindowEvent e)
{
	dispose();
}

@Override
public void windowClosed(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void windowIconified(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void windowDeiconified(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void windowActivated(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void windowDeactivated(WindowEvent e)
{
	// TODO Auto-generated method stub
	
}

@Override
public void keyTyped(KeyEvent e)
{
	
	
}

@Override
public void keyPressed(KeyEvent e)
{
	if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
		  if (logparent!=null)
			  searchInLog();
				else if (confparent!=null)
					searchInConf();
		
		}
}

@Override
public void keyReleased(KeyEvent e)
{
	// TODO Auto-generated method stub
	
}

}