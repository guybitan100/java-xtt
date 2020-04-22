package com.mobixell.xtt.gui.testlaunch.table;

import java.util.Vector;
import javax.swing.table.AbstractTableModel;

import com.mobixell.xtt.gui.testlaunch.TestLauncherGui;
import com.mobixell.xtt.gui.testlaunch.panels.XmlDialog;
import com.mobixell.xtt.gui.testlaunch.table.ParamDataItem;

public class ParamTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = -4550063364356949659L;
	public static final int CHECKBOX_USE = 0;
	public static final int CHECKBOX_GROUP = 1;
	public static final int DROPDOWN = 2;
	public static final int VALUE = 3;
	public Vector<ParamDataItem> vec_items = new Vector<ParamDataItem>();

	public ParamTableModel(Vector<ParamDataItem> vec_items)
	{
		this.vec_items = vec_items;
	}

	public int getColumnCount()
	{
		return 4;
	}

	public int getRowCount()
	{
		return vec_items.size();
	}

	public Object getValueAt(int row, int col)
	{
		ParamDataItem crnt = vec_items.get(row);
		switch (col)
		{
			case CHECKBOX_USE:
				return crnt.use;
			case DROPDOWN:
				return crnt.type;
			case VALUE:
				return crnt.value;
			case CHECKBOX_GROUP:
				return crnt.group;
			default:
				return null;
		}
	}

	public Class<? extends Object> getColumnClass(int c)
	{
		return getValueAt(0, c).getClass();
	}

	public String getColumnName(int col)
	{
		switch (col)
		{
			case CHECKBOX_USE:
				return "Use";
			case DROPDOWN:
				return "Parameter";
			case VALUE:
				return "Value";
			case CHECKBOX_GROUP:
				return "Group";
			default:
				return "";
		}
	}

	// The table row/col events are here
	public void setValueAt(Object value, int row, int col)
	{
		ParamDataItem crnt = vec_items.get(row);
		switch (col)
		{
			case CHECKBOX_USE:
				crnt.use = (Boolean) value;
				// if use is not selected remove group
				if (crnt.use == false) crnt.group = false;
				break;
			case DROPDOWN:
				crnt.use = true;
				crnt.type = (String) value;

				if (crnt.type.equalsIgnoreCase("xml")) try
				{
					crnt.value = XmlDialog.showDialog(TestLauncherGui.parameterDialog, crnt.value);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*if (crnt.type.equalsIgnoreCase("file"))
				{
					JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
					fc.setDialogTitle("Open File Parameter");

					fc.setMultiSelectionEnabled(false);
					fc.setFileFilter(new FileFilter() {
						public boolean accept(File pathname)
						{
							return pathname.getName().toLowerCase().endsWith(".xml");
						}

						public String getDescription()
						{
							return "xml";
						}

					});
					fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					int returnVal = fc.showOpenDialog(fc);
					if (returnVal == JFileChooser.APPROVE_OPTION)
					{
						crnt.value = fc.getSelectedFile().getAbsoluteFile().getAbsolutePath();
					}
				}*/
				break;
			case VALUE:
				crnt.use = true;
				crnt.value = (String) value;
				break;
			// If group selected set also use
			case CHECKBOX_GROUP:
				crnt.group = (Boolean) value;
				if (crnt.use == false) crnt.use = (Boolean) value;
				break;
			default:
		}
		fireTableRowsUpdated(row, row);

	}

	public boolean isCellEditable(int row, int col)
	{
		ParamDataItem crnt = vec_items.get(row);

		switch (col)
		{
			case VALUE:
				crnt.use = true;
				if (crnt.type.equalsIgnoreCase("xml")) try
				{
					crnt.value = XmlDialog.showDialog(TestLauncherGui.parameterDialog, crnt.value);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			default:
		}

		fireTableRowsUpdated(row, row);
		return true;
	}
}
