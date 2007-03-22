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

import de.lmu.ifi.dbs.utilities.optionhandling.Option;

public class FileEditor extends TextFieldParameterEditor {

	private JTextField textField;

	public FileEditor(Option<File> option, Window owner) {
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
					setValue(file.getPath());
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

//	@Override
//	public boolean isValid() {
//		
//		if(getValue() == null && ((Parameter)option).isOptional()){
//			return true;
//		}
//		
//		try {
//
//			option.isValid(getValue());
//		} catch (ParameterException e) {
//
//			Border border = textField.getBorder();
//
//			textField.setBorder(BorderFactory.createLineBorder(Color.red));
//			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
//			textField.setBorder(border);
//			return false;
//
//		}
//		return true;
//	}

}
