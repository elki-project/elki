package de.lmu.ifi.dbs.utilities.optionhandling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FileParameter extends Parameter<File> implements ActionListener {

	private JTextField fileField;

	private final JFileChooser chooser;

	public FileParameter(String name, String description, String directory) {
		super(name, description);
		chooser = new JFileChooser(directory);
		this.inputField = createInputField();
	}

	public FileParameter(String name, String description) {
		this(name, description,null);
	}

	private JComponent createInputField() {

		// TODO event. FormattedTextField mit eigenem FileFormatter
NumberFormat formatter;
		
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

	private void showFileChooser() {

	}

	public JComponent getInputField() {
		return inputField;
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("fileLabel")) {

			int returnValue = chooser.showOpenDialog(inputField);

			if (returnValue == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				System.out.println("file name: " + file.getName());
				fileField.setText(file.getName());
			}
		}
	}

	@Override
	public String getValue() {
		return value.getPath();
	}

	@Override
	public boolean isSet() {
		return (value != null);
	}


	@Override
	public void setValue(String value) throws ParameterException {
		File file = new File(value);
		try {
			if (!file.exists()) {
				throw new WrongParameterValueException("");
			}
		} 
		// TODO
		catch (SecurityException e) {
			throw new WrongParameterValueException("");
		}
		this.value = file;

	}

	@Override
	public void setValue() throws ParameterException {
		setValue(fileField.getText());		
	}

}
