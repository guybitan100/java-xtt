package com.mobixell.xtt.log;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.JEditTextArea;
import com.mobixell.xtt.gui.LOGTokenMarker;
import com.mobixell.xtt.gui.SyntaxStyle;
import com.mobixell.xtt.gui.SyntaxUtilities;
import com.mobixell.xtt.gui.TextAreaDefaults;
import com.mobixell.xtt.gui.Token;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.images.ImageCenter;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
/**
 * @author Roger Soder
 * @version $Id: LogViewer.java,v 1.3 2007/03/01 12:23:17 rsoder Exp $
 * @see com.mobixell.xtt.gui.main.XTTGui
 */
public class LogViewer extends JFrame implements WindowListener, ActionListener,KeyListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String tantau_sccsid = "@(#)$Id: LogViewer.java,v 1.3 2007/03/01 12:23:17 rsoder Exp $";
	private XTTGui xttgui = null;
	private JEditTextArea taLog = null;
	private String currentFileName = "";
	JLabel lineNumbetLabel;

	public LogViewer(XTTGui xttgui, String logtext,boolean isVisible)
	{
		Image im = Toolkit.getDefaultToolkit().getImage(
				ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_REPORTER));
		this.setIconImage(im);
		this.setTitle("Log Viewer");
		this.addWindowListener(this);
		this.xttgui = xttgui;
		this.setDefaultCloseOperation(LogViewer.DISPOSE_ON_CLOSE);
		JPopupMenu popup = new JPopupMenu();
		addKeyListener(this);
		JMenuItem menuItem = null;

		menuItem = new JMenuItem("Copy to Clipboard");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("COPY");
		menuItem.addActionListener(this);
		popup.add(menuItem);

		menuItem = new JMenuItem("Select all");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("SELECTALL");
		menuItem.addActionListener(this);
		popup.add(menuItem);

		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenu searchMenu = new JMenu("Search");
		menuBar.add(searchMenu);

		menuItem = new JMenuItem("Find");
		menuItem.setActionCommand("FIND");
		searchMenu.add(menuItem);
		menuItem.addActionListener(this);

		menuItem = new JMenuItem("Open");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.setActionCommand("OPEN");
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Select all");
		menuItem.setActionCommand("SELECTALL");
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Copy to Clipboard");
		menuItem.setActionCommand("COPY");
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Save as");
		menuItem.setActionCommand("SAVEAS");
		fileMenu.add(menuItem);
		menuItem.addActionListener(this);

		fileMenu.addSeparator();

		menuItem = new JMenuItem("Exit");
		menuItem.setActionCommand("EXIT");
		fileMenu.add(menuItem);
		menuItem.addActionListener(this);

		SyntaxStyle[] styles = SyntaxUtilities.getDefaultSyntaxStyles();
		styles[Token.KEYWORD1] = new SyntaxStyle(Color.red, false, true);
		styles[Token.KEYWORD2] = new SyntaxStyle(Color.orange, false, true);
		styles[Token.KEYWORD3] = new SyntaxStyle(new Color(0x009000), false, true);
		styles[Token.INVALID] = new SyntaxStyle(Color.magenta, false, true);

		TextAreaDefaults defaults = TextAreaDefaults.getDefaults();
		defaults.styles = styles;

		taLog = new JEditTextArea(defaults,this);
		taLog.setRightClickPopup(popup);
		taLog.setTokenMarker(new LOGTokenMarker());
		taLog.setEditable(false);
		taLog.setText(logtext);
		taLog.setCaretPosition(0);
		taLog.scrollToCaret();

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(taLog, BorderLayout.CENTER);
		this.pack();
		this.setLocationRelativeTo(this); // center it
		this.setVisible(isVisible);
	}
	private void loadFile(String filename)
	{
		try
		{
			File file = new File(filename);
			file.createNewFile();
			int size = (int) file.length();
			char[] content = new char[size];
			InputStreamReader reader = new InputStreamReader(new FileInputStream(file), XTTProperties.getCharSet());
			int numchars = reader.read(content, 0, size);
			taLog.setText(new String(content, 0, numchars));
			taLog.setCaretPosition(0);
			taLog.scrollToCaret();
			this.currentFileName = filename;
			this.setTitle(currentFileName + " - Log Viewer");
		}
		catch (Exception e)
		{
			xttgui.showConfigurationError(e.getMessage());
		}
	}
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("EXIT"))
		{
			this.dispose();
		}
		else if (e.getActionCommand().equals("FIND"))
		{
			taLog.search();
		}
		else if (e.getActionCommand().equals("SAVE"))
		{
			try
			{
				writeFile(taLog.getText());
			}
			catch (Exception ex)
			{
				String error = "Error:\n";
				showSaveError(error + ex);
			}
		}
		else if (e.getActionCommand().equals("SAVEAS"))
		{
			try
			{
				// Create a file chooser
				JFileChooser fc = new JFileChooser(".");
				// In response to a button click:
				int returnVal = fc.showSaveDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					File file = fc.getSelectedFile();
					file.createNewFile();
					this.currentFileName = file.getAbsolutePath();
					this.setTitle(currentFileName + " - Log Viewer");
					writeFile(taLog.getText());
					loadFile(file.getAbsolutePath());
				}
			}
			catch (Exception ex)
			{
				String error = "Error:\n";
				showSaveError(error + ex);
			}
		}
		else if (e.getActionCommand().equals("LOAD"))
		{
			JFileChooser fc = new JFileChooser(".");
			// In response to a button click:
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = fc.getSelectedFile();

				loadFile(file.getAbsolutePath());
			}
		}
		else if (e.getActionCommand().equals("COPY"))
		{
			java.awt.datatransfer.StringSelection contents = new java.awt.datatransfer.StringSelection(
					taLog.getSelectedText());
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
		}
		else if (e.getActionCommand().equals("SELECTALL"))
		{
			taLog.selectAll();
		}
		else if (e.getActionCommand().equals("OPEN"))
		{
			openFile();
		}
	}
	private void openFile()
	{
		try
		{
			JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
			// In response to a button click:
			int returnVal = fc.showOpenDialog(null);
			String str = "";
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				FileInputStream fstream = new FileInputStream(fc.getSelectedFile().getPath());
				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null)
				{
					// Print the content on the console
					str += strLine + "\n";
				}
				taLog.setText(str);
				// Close the input stream
				in.close();
			}
		}
		catch (Exception e)
		{
			showError("Test File Set Error", e.getMessage());
		}
	}
	private static void showError(String errortitle, String errortext)
	{
		JOptionPane.showMessageDialog(null, errortext, errortitle, JOptionPane.ERROR_MESSAGE);
	}
	private void showSaveError(String errortext)
	{
		JOptionPane.showMessageDialog(this, errortext + "\n\nLog File NOT saved!", "Log File error",
				JOptionPane.ERROR_MESSAGE);
	}
	private void writeFile(String text) throws Exception
	{
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(currentFileName));
		byte[] data = ConvertLib.createBytes(text);
		XTTProperties.printDebug("Writing file size: " + data.length + " bytes name: " + currentFileName);
		out.write(data, 0, data.length);
		out.flush();
		out.close();
	}
	private void endgame()
	{
		taLog.setText(" ");
		taLog.setText("");
		taLog.setText(" ");
		taLog.setText("");
		taLog.setText(" ");
		this.getContentPane().removeAll();
	}
	public void windowActivated(WindowEvent e)
	{
	}
	public void windowClosed(WindowEvent e)
	{
		endgame();
	}
	public void windowClosing(WindowEvent e)
	{
		endgame();
	}
	public void setMessage(String logtext)
	{
		taLog.setText(logtext);
	}
	public JEditTextArea getTaLog()
	{
		return taLog;
	}
	public void setTaLog(JEditTextArea taLog){this.taLog = taLog;}
	public void windowDeactivated(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowOpened(WindowEvent e){}
	public void keyPressed(KeyEvent e) {
		if ((e.getKeyCode() == KeyEvent.VK_F) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))
		{
			taLog.search();
		}
	}
	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
}