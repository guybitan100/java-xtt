package com.mobixell.xtt.gui.testlaunch.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.mobixell.xtt.images.ImageCenter;


public class ParamHeaderRenderer extends JLabel implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		if (table != null) {
			JTableHeader header = table.getTableHeader();
			if (header != null) {
				setForeground(header.getForeground());
				setBackground(header.getBackground());
				setFont(header.getFont());
			}
		}

		setIcon(ImageCenter.getInstance().getImage(
				ImageCenter.ICON_TABLE_HEADER));

		setForeground(Color.white);

		switch (column) {
		case 0:
			setText("Use");
			break;
		case 1:
			setText("Group");
			break;
		case 2:
			setText("Parameter");
			break;
		case 3:
			setText("Value");
			break;
		case 4:
			setText("Name");
			break;
		default:
			break;
		}

		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		setHorizontalAlignment(JLabel.CENTER);
		
		return this;
	}

	public void paint(Graphics g) {
		Dimension size = this.getSize();
		g.drawImage(ImageCenter.getInstance().getAwtImage(
				ImageCenter.ICON_TABLE_HEADER), 0, 0, size.width, size.height,
				this);

		super.paint(g);
	}

}
