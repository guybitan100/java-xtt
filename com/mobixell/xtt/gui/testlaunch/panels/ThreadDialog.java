package com.mobixell.xtt.gui.testlaunch.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.mobixell.xtt.ModuleList;
import com.mobixell.xtt.gui.testlaunch.table.ThreadDataItem;
import com.mobixell.xtt.gui.testlaunch.table.ThreadTableModel;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.images.ImageCenter;

public class ThreadDialog extends JDialog implements ActionListener,MouseListener
{
	private static final long serialVersionUID = -5967383890282447522L;
	public static Vector<ThreadDataItem> vec_items = null;
	private JTable table = null;
	public ModuleList modules;
	private ThreadTableModel threadTableModel;
	private JButton bAddThread = null;
	private JButton bLinkThread = null;
	private JButton bClose = null;
	private JButton bRemoveThread = null;
	private JPanel pButtons = null;
	private JPanel ptable = null;
	public static ThreadDialog dialog = null;
	public static LinkedBlockingQueue<Integer> avalibeleThreadIds;
	public boolean isItemExist;
	public static boolean isInitVec;

	public ThreadDialog(JFrame parent)
	{
		super(parent);
		if (vec_items == null)
		{
			vec_items = new Vector<ThreadDataItem>();
			isInitVec = true;
		}
		else
		{
			isInitVec = false;
		}
		Image im = Toolkit.getDefaultToolkit().getImage(ImageCenter.getInstance().getImageUrl(ImageCenter.ICON_THREAD));
		this.setIconImage(im);
	}

	public static void showDialog(JFrame parent, boolean isItemExist) throws Exception
	{
		if (dialog == null)
		{
			if (vec_items == null) isInitVec = true;
			else isInitVec = false;
			
			dialog = new ThreadDialog(parent);
			dialog.isItemExist = isItemExist;
			dialog.init();
		}
		else
		{
			if (vec_items == null) isInitVec = true;
			else isInitVec = false;
			
			dialog.isItemExist = isItemExist;
			dialog.init();
		}

		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.isUsed ();
		dialog.setVisible(true);
	}
	
	public static void resetDialog()
	{
		avalibeleThreadIds = null;
		vec_items = null;
		dialog = null;
	}
	
	public void init()
	{
		setTitle("Thread Managment");
		avalibeleThreadIds = new LinkedBlockingQueue<Integer>();
		bAddThread = new JButton("Add New", ImageCenter.getInstance().getImage(ImageCenter.ICON_FIXTURE_RUNNING));
		bAddThread.setActionCommand("ADDTHREAD");
		bAddThread.addActionListener(this);

		bLinkThread = new JButton("Link To Step", ImageCenter.getInstance().getImage(ImageCenter.ICON_ARROW_RIGHT_YELLOW));
		bLinkThread.setActionCommand("ADDTOTHREAD");
		bLinkThread.addActionListener(this);

		if (isItemExist) bLinkThread.setEnabled(true);
		else bLinkThread.setEnabled(false);

		bClose = new JButton("Close", ImageCenter.getInstance().getImage(ImageCenter.ICON_EXIT));
		bClose.setActionCommand("CLOSE");
		bClose.addActionListener(this);

		

		bRemoveThread = new JButton("Remove", ImageCenter.getInstance().getImage(ImageCenter.ICON_CLEAR));
		bRemoveThread.setActionCommand("REMOVETHREAD");
		bRemoveThread.addActionListener(this);
		
		if (isInitVec) bRemoveThread.setEnabled(false);

		pButtons = new JPanel();

		pButtons.add(bLinkThread);
		pButtons.add(bAddThread);
		pButtons.add(bRemoveThread);
		pButtons.add(bClose);

		// Init in the first time

		if (ptable == null) 
			ptable = new JPanel(new BorderLayout());
		
		else ptable.removeAll();

		ptable.add(pButtons, BorderLayout.SOUTH);
		threadTableModel = new ThreadTableModel(vec_items);
		table = new JTable(threadTableModel);
		table.addMouseListener(this);
		table.getTableHeader().setReorderingAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		updateTable();

		if (isInitVec) initVec();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		scrollPane.getViewport().setPreferredSize(new Dimension(250, 215));

		ptable.add(scrollPane, BorderLayout.CENTER);
		setSelectionRow();

		add(ptable, BorderLayout.SOUTH);
		pack();
		this.setLocationRelativeTo(null); // center it
	}

	public void setSelectionRow()
	{
		try
		{
			for (Enumeration<ThreadDataItem> tdiEnum = vec_items.elements(); tdiEnum.hasMoreElements();)
			{
				ThreadDataItem tdi = tdiEnum.nextElement();
				if (tdi.getStatus())
				{
					table.setRowSelectionInterval(0, tdi.id - 1);
					return;
				}
			}
			table.setRowSelectionInterval(0, 0);
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void updateTable()
	{
		table.getColumnModel().getColumn(ThreadTableModel.STATUS)
				.setCellEditor(table.getDefaultEditor((new Boolean(false)).getClass()));
		table.getColumnModel().getColumn(ThreadTableModel.THREADNAME)
				.setCellEditor(table.getDefaultEditor((new String("")).getClass()));
		table.getColumnModel().getColumn(ThreadTableModel.STATUS).setMaxWidth(45);
		table.getColumn("Status").setHeaderRenderer(new ThreadHeaderRenderer());
		table.getColumn("Status").setCellRenderer(new ThreadStatusCellRenderer());
		table.getColumn("Thread Name").setHeaderRenderer(new ThreadHeaderRenderer());
		table.setRowHeight(18);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		threadTableModel.fireTableDataChanged();
	}

	public void initVec()
	{
		vec_items.clear();
		for (int i = 0; i < 10; i++)
			addNewThread();
		updateTable();
	}

	public JTable gettable()
	{
		return this.table;
	}

	public ThreadTableModel getTableModel()
	{
		return this.threadTableModel;
	}

	public void actionPerformed(ActionEvent e)
	{

		if (e.getActionCommand().equals("ADDTHREAD"))
		{
			addNewThread();
		}
		if (e.getActionCommand().equals("ADDTOTHREAD"))
		{
			addToThread();
		}
		if (e.getActionCommand().equals("REMOVETHREAD"))
		{
			removeThread();
		}
		if (e.getActionCommand().equals("CLOSE"))
		{
			if (table.isEditing()) table.getCellEditor().stopCellEditing();
			setVisible(false);
		}
	}

	private void addNewThread()
	{
		if (table.isEditing()) table.getCellEditor().stopCellEditing();

		if (avalibeleThreadIds.size() > 0)
		{
			vec_items.add(new ThreadDataItem(avalibeleThreadIds.poll()));
		}
		else
		{
			avalibeleThreadIds.add(table.getRowCount() + 1);
			vec_items.add(new ThreadDataItem(avalibeleThreadIds.poll()));
		}
		threadTableModel.fireTableDataChanged();
		bRemoveThread.setEnabled(true);
		if (table.getRowCount() - 1 >= 0)
		{
			table.setRowSelectionInterval(0, table.getRowCount() - 1);
		}
	}

	private void addToThread()
	{
		if (table.getSelectedRow() != -1)
		{
			if (table.isEditing()) table.getCellEditor().stopCellEditing();
			try
			{
				if (table.getRowCount() > 0)
				{
					ThreadDataItem tdi = vec_items.get(table.getSelectedRow());
					tdi.setNode(TreeTestController.addToThread(tdi));
					tdi.setStatus(false);
					vec_items.set(table.getSelectedRow(), tdi);
					setVisible(false);
				}
			}
			catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else bLinkThread.setEnabled(false);
	}

	public static void updateThreadList(ThreadDataItem tdi)
	{
		int id = 0;
		try
		{
			if (vec_items == null)
			{
				vec_items = new Vector<ThreadDataItem>();
				isInitVec = false;
				id = vec_items.size();
				tdi.setId(id + 1);
				vec_items.add(id, tdi);
				tdi.setStatus(false);
			}
			else
			{
				id = vec_items.size();
				tdi.setId(id + 1);
				vec_items.add(id, tdi);
				tdi.setStatus(false);
			}
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void removeFromThread(String threadName)
	{
		try
		{
			for (Enumeration<ThreadDataItem> tdiEnum = vec_items.elements(); tdiEnum.hasMoreElements();)
			{
				ThreadDataItem tdi = tdiEnum.nextElement();
				if (tdi.getName().equalsIgnoreCase(threadName))
				{
					tdi.setNode(null);
					tdi.setStatus(true);
					break;
				}
			}
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	

	private void removeThread()
	{
		if (table.getSelectedRow() == vec_items.size())
		{
			if (table.getSelectedRow() == vec_items.size())
			{
				vec_items.remove(table.getSelectedRow() - 1);
				avalibeleThreadIds.add(vec_items.get(table.getSelectedRow() - 1).id);
			}
		}
		else
		{
			avalibeleThreadIds.add(vec_items.get(table.getSelectedRow()).id);
			vec_items.remove(table.getSelectedRow());
		}
		threadTableModel.fireTableDataChanged();
		if (table.getRowCount() - 1 >= 0)
		{
			table.setRowSelectionInterval(0, table.getRowCount() - 1);
		}
		else
		{
			avalibeleThreadIds.clear();
			bRemoveThread.setEnabled(false);
		}
	}

	class ThreadHeaderRenderer extends JLabel implements TableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{

			if (table != null)
			{
				JTableHeader header = table.getTableHeader();
				if (header != null)
				{
					setForeground(header.getForeground());
					setBackground(header.getBackground());
					setFont(header.getFont());
				}
			}

			setIcon(ImageCenter.getInstance().getImage(ImageCenter.ICON_TABLE_HEADER));

			setForeground(Color.white);

			switch (column)
			{
				case 0:
					setText("Status");
					break;
				case 1:
					setText("Thread Name");
					break;
				default:
					break;
			}

			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			setHorizontalAlignment(JLabel.CENTER);

			return this;
		}

		public void paint(Graphics g)
		{
			Dimension size = this.getSize();
			g.drawImage(ImageCenter.getInstance().getAwtImage(ImageCenter.ICON_TABLE_HEADER), 0, 0, size.width,
					size.height, this);
			super.paint(g);
		}
	}

	class ThreadStatusCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;
		ImageIcon OKAY_IMAGE = ImageCenter.getInstance().getImage(ImageCenter.ICON_REMOTEAGENT_NOTCONNECTED);
		ImageIcon INUSE_IMAGE = ImageCenter.getInstance().getImage(ImageCenter.ICON_REMOTEAGENT_OK);

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			JLabel label = (JLabel) super
					.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			label.setText("");
			label.setHorizontalAlignment(SwingConstants.CENTER);
			if (((Boolean) value) == true) label.setIcon(OKAY_IMAGE);
			if (((Boolean) value) == false) label.setIcon(INUSE_IMAGE);
			return label;
		}
	}

	
	public void isUsed ()
	{
		if (table.getRowCount() > 0)
		{
			ThreadDataItem tdi = vec_items.get(table.getSelectedRow());
			if (tdi.getStatus())
			{
			bRemoveThread.setEnabled(true);
			}
			else
			{
				bRemoveThread.setEnabled(false);
			}
		}
	}
	public void mouseClicked(MouseEvent e)
	{
		isUsed ();
		
	}

	@Override
	public void mousePressed(MouseEvent me)
	{
		
		
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
}
