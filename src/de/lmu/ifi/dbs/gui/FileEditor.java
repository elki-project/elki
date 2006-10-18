package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class FileEditor extends ParameterEditor {

	private JTextField textField;

	public FileEditor(Option option, JFrame owner) {
		super(option, owner);
		createInputField();
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();
		JButton label = new JButton("Load File");
		label.setActionCommand("fileLabel");
		label.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int returnValue = chooser.showOpenDialog(inputField);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();

					textField.setText(file.getName());
					value = file.getPath();
				}
			}
		});
		inputField.add(label);

		textField = new JTextField();
		textField.setColumns(30);
		textField.setEditable(false);
		textField.setBackground(Color.white);

		inputField.add(textField);
		inputField.add(helpLabel);
	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(value);
		} catch (ParameterException e) {

			Border border = textField.getBorder();

			textField.setBorder(BorderFactory.createLineBorder(Color.red));
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			textField.setBorder(border);
			return false;

		}
		return true;
	}

}
