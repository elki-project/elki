package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class FileListEditor extends ParameterEditor {

	private JTextField textField;
	
	public FileListEditor(Option option, JFrame owner){
		super(option, owner);
		createInputField();
	}
	
	@Override
	protected void createInputField() {
		inputField = new JPanel();
		textField = new JTextField() {
			public void setText(String t) {
				String text = getText();
				if (text == null || text.equals("")) {
					setText(t);
				} else {
					setText(text.concat("," + t));
				}
			}
		};
		textField.setColumns(30);
		textField.setEditable(false);
		inputField.add(textField);

		JButton label = new JButton("Load File");
		label.setActionCommand("fileLabel");
		label.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int returnValue = chooser.showOpenDialog(inputField);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();

					if (! textField.getText().equals("")) {

						value = value + "," + file.getPath();
					} else {

						value = file.getPath();
					}
					 textField.setText(file.getName());
				}
			}
		});
		inputField.add(label);
		inputField.add(helpLabel);
	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(value);
		} catch (ParameterException e) {

			Border border = inputField.getBorder();

			inputField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			inputField.setBorder(border);
			return false;

		}
		return true;
	}

}
