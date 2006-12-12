package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class ClassListEditor extends ParameterEditor {

	
	private JTextField textField;
	
	public ClassListEditor(Option option, JFrame owner) {
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
		textField.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent input) {

				try {
					((ClassListParameter) option).isValid(getValue());
				} catch (ParameterException e) {
					return false;
				}
				return true;
			}

			public boolean shouldYieldFocus(JComponent input) {
				boolean inputOK = verify(input);
				checkInput();
				return inputOK;

			}

			public void checkInput() {

				try {
					((ClassListParameter) option).isValid(getValue());
				} catch (ParameterException e) {

					KDDDialog.showParameterMessage(owner, e.getMessage(), e);

				}
			}
		});

		textField.setEditable(false);
		textField.setBackground(Color.white);
		JScrollPane scroller = new JScrollPane();
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroller.setViewportView(textField);
		inputField.add(scroller);

		JComboBox classSelector = new JComboBox();
		classSelector.setModel(new DefaultComboBoxModel(((ClassListParameter) option)
				.getRestrictionClasses()));

		classSelector.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox box = (JComboBox) e.getSource();
				String selClass = (String) box.getSelectedItem();
				textField.setText(selClass);
				setValue(textField.getText());
			}
		});

		inputField.add(classSelector);
		inputField.add(helpLabel);
		Dimension dim = inputField.getPreferredSize();
		dim.height = 50;
		inputField.setPreferredSize(dim);
	}

	@Override
	public boolean isValid() {
		try {

			option.isValid(getValue());
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
