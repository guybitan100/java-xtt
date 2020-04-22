package com.mobixell.xtt.gui.testlaunch.panels;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jdom.JDOMException;
import com.mobixell.xtt.gui.JEditTextArea;
import com.mobixell.xtt.gui.XMLTokenMarker;
import com.mobixell.xtt.images.ImageCenter;

public class XmlDialog extends JDialog implements ActionListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	private static XmlDialog dialog = null;
	private String inputXml="";
	JEditTextArea ta;
	private JButton bClose = null;
	private JButton bReset = null;
	private JPanel pButtons = null;

	public XmlDialog(ParamDialog paramDialog, String inputXml)
	{
		super(paramDialog, true);
		this.inputXml = inputXml;
		initDialogComponents();
	}
	public XmlDialog(JFrame frame, String inputXml)
	{
		super(frame, true);
		this.inputXml = inputXml;
		initComponents();
	}
	public static String showDialog(ParamDialog paramDialog, String inputXml) throws JDOMException, IOException
	{
		dialog = new XmlDialog(paramDialog, inputXml);
		dialog.setVisible(true);
		return dialog.getXmlDialogText();
	}
	public static String showDialog(JFrame frame, String inputXml) throws JDOMException, IOException
	{
		dialog = new XmlDialog(frame, inputXml);
		dialog.setVisible(true);
		return dialog.getXmlDialogText();
	}
	private void initComponents()
	{
		setLayout(new BorderLayout());
		setTitle("XML Editor");	
		JPopupMenu popup = new JPopupMenu();

		JMenuItem menuItem = null;
		menuItem = new JMenuItem("Select All");
		menuItem.setActionCommand("ALL");
		menuItem.addActionListener(this);
		popup.add(menuItem);
		
		menuItem = new JMenuItem("Copy to Clipboard");
		menuItem.setActionCommand("COPY");
		menuItem.addActionListener(this);
		popup.add(menuItem);
		
		menuItem = new JMenuItem("Paste from Clipboard");
		menuItem.setActionCommand("PASTE");
		menuItem.addActionListener(this);
		popup.add(menuItem);

		ta = new JEditTextArea();
		ta.setTokenMarker(new XMLTokenMarker());
		ta.setCaretPosition(0);
		ta.scrollToCaret();
		ta.setText(inputXml);
		ta.setRightClickPopup(popup);
		 
		getContentPane().add(ta,BorderLayout.CENTER);
		Image im = Toolkit.getDefaultToolkit().getImage(
				ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_FILTER_TESTS_TREE));
		this.setIconImage(im);
		addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                ta.requestFocusInWindow();
            }
        });
		ta.setText(inputXml);
		ta.scrollTo(0,0);
		pack();
		setLocationRelativeTo(null); // center it
	}

	private void initDialogComponents()
	{
		setLayout(new BorderLayout());
		setTitle("XML Editor");
		bClose = new JButton("Set & Close", ImageCenter.getInstance().getImage(ImageCenter.ICON_SAVE));
		bClose.setActionCommand("CLOSE");
		bClose.addActionListener(this);

		bReset = new JButton("Reset", ImageCenter.getInstance().getImage(ImageCenter.ICON_CLEAR));
		bReset.setActionCommand("RESET");
		bReset.addActionListener(this);

		pButtons = new JPanel();
		pButtons.setPreferredSize(new Dimension(100, 40));
		pButtons.add(bClose);
		pButtons.add(bReset);
		
		initComponents();
		getContentPane().add(pButtons, BorderLayout.SOUTH);
	}

	public String getXmlDialogText() throws JDOMException, IOException
	{
		String text = dialog.ta.getText();
		if (text=="") text = "<xml>  </xml>";
		dialog.ta.setText(text);
		return text;
	}
	public void actionPerformed(ActionEvent e) 
	{
		String[] choicesYesNo = { "Yes", "No" };
		if(e.getActionCommand().equals("RESET"))
        {
			int option = JOptionPane.showOptionDialog(null, "Are you sure do you want to reset?", "Reset patemeters warning", 0, 1, null, choicesYesNo, choicesYesNo[0]);
	        if (option == 0)
	        {
	        ta.setText("");
	        }
        }
		else if(e.getActionCommand().equals("SAVE")) { setVisible(false);}
		else if(e.getActionCommand().equals("CLOSE")){setVisible(false);}
		else if(e.getActionCommand().equals("COPY"))
        {
            java.awt.datatransfer.StringSelection contents=new java.awt.datatransfer.StringSelection(ta.getSelectedText());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
        } else if(e.getActionCommand().equals("PASTE"))
        {
            ta.paste();
        }
        else if(e.getActionCommand().equals("ALL"))
        {
            ta.selectAll();
        }
	}
	 public void showError(String title,String errortext)
	    {
	        JOptionPane.showMessageDialog(this,
	            errortext,
	            title,
	            JOptionPane.ERROR_MESSAGE);
	    }
	 public void keyTyped(KeyEvent e){}
	 public void keyPressed(KeyEvent e){}
	 public void keyReleased(KeyEvent e){}
		
}