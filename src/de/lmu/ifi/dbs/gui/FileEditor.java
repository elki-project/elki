package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;

public class FileEditor extends TextFieldParameterEditor {

	public FileEditor(FileParameter option, Window owner, ParameterChangeListener l) {
		super(option, owner,l);
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();
		
		textField = new JTextField();
		textField.setColumns(getColumnNumber());
		textField.setEditable(false);
		textField.setBackground(Color.white);
		
		if(((FileParameter)option).hasDefaultValue()){
			this.textField.setText(((FileParameter)option).getDefaultValue().getPath());
			setValue(textField.getText());
		}
		
		JButton label = new JButton("Load File");
		label.setActionCommand("fileLabel");
		label.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int returnValue = chooser.showOpenDialog(inputField);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();

					textField.setText(file.getName());
					setValue(file.getPath());
				}
			}
		});
		inputField.add(label);


		inputField.add(textField);
		inputField.add(helpLabel);
	}

	@Override
	protected int getColumnNumber() {
		return StringEditor.COLUMN_NUMBER;
	}
}
