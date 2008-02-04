package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.optionhandling.FileListParameter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class FileListEditor extends TextFieldParameterEditor {

	public FileListEditor(FileListParameter option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@SuppressWarnings("serial")
	@Override
	protected void createInputField() {
		inputField = new JPanel();
		textField = new JTextField() {
			public void setText(String t) {
				String text = getText();
				if (text == null || text.equals("")) {
					super.setText(t);
				} else {
					super.setText(text.concat("," + t));

				}

				setCaretPosition(0);
				inputField.revalidate();

			}

			public Dimension getPreferredScrollableViewportSize() {
				Dimension dim = getPreferredSize();
				dim.width = 340;

				return dim;
			}

			public void setSize(Dimension d) {

				if (d.width < getParent().getSize().width) {
					d.width = getParent().getSize().width;

				}

				super.setSize(d);
			}

			public boolean getScrollableTracksViewportWidth() {

				return false;
			}
		};

		if (((FileListParameter) option).hasDefaultValue()) {
			List<File> defaultValue = ((FileListParameter) option).getDefaultValue();
			for (File f : defaultValue) {
				this.textField.setText(f.getPath());
			}
			setValue(this.textField.getText());
		}

		textField.setEditable(false);
		textField.setBackground(Color.white);
		JScrollPane scroller = new JScrollPane();
		scroller.setViewportView(textField);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		inputField.add(scroller);

		JButton label = new JButton("Load File");

		label.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int returnValue = chooser.showOpenDialog(inputField);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();

					if (!textField.getText().equals("")) {

						setValue(getValue() + "," + file.getPath());
					} else {

						setValue(file.getPath());
					}
					textField.setText(file.getName());
				}
			}
		});
		inputField.add(label);
		inputField.add(helpLabel);
		Dimension dim = inputField.getPreferredSize();
		dim.height = 50;
		inputField.setPreferredSize(dim);
	}

	@Override
	protected int getColumnNumber() {
		return StringEditor.COLUMN_NUMBER;
	}
}
