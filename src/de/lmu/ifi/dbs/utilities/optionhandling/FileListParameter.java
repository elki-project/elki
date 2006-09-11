package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FileListParameter extends ListParameter<File> implements
		ActionListener {

	private JTextField fileField;

	private final JFileChooser chooser;

	public FileListParameter(String name, String description) {
		super(name, description);
		chooser = new JFileChooser();
		inputField = createInputField();
	}

	private JComponent createInputField() {

		JPanel base = new JPanel();

		fileField = new JTextField();
		fileField.setColumns(30);
		fileField.setEditable(false);
		base.add(fileField);

		JButton label = new JButton("Load File");
		label.setActionCommand("fileLabel");
		label.addActionListener(this);
		base.add(label);
		return base;
	}


	@Override
	public Component getInputField() {
		return inputField;
	}

	@Override
	public String getValue() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < value.size(); i++) {
			buffer.append(value.get(i));
			if (i != value.size() - 1) {
				buffer.append(",");
			}
		}
		return buffer.toString();
	}

	@Override
	public boolean isSet() {

		return (value != null);
	}

	@Override
	public void setValue(String value) throws ParameterException {
		String[] files = SPLIT.split(value);
		if (files.length == 0) {
			// TODO
			throw new WrongParameterValueException("");
		}
		
		Vector<File> fileValue = new Vector<File>();
		for (String f : files) {
			File file = new File(f);
			try {
				if (!file.exists()) {
					// TODO
					throw new WrongParameterValueException("");
				}
			}
			// TODO
			catch (SecurityException e) {
				throw new WrongParameterValueException("");
			}
			fileValue.add(file);
		}
		this.value = fileValue;
	}

	@Override
	public void setValue() throws ParameterException {
		setValue(fileField.getText());
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("fileLabel")) {

			int returnValue = chooser.showOpenDialog(inputField);

			if (returnValue == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				updateFileField(file.getPath());
			}
		}
	}

	private void updateFileField(String cl) {

		String text = fileField.getText();
		if (text == null || text.equals("")) {
			fileField.setText(cl);
		} else {
			fileField.setText(text.concat("," + cl));
		}		
	}

	@Override
	public int getListSize() {
		return this.value.size();
	}

}
