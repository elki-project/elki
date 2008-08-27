package de.lmu.ifi.dbs.elki.gui;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class ClassListEditor extends TextFieldParameterEditor {

	public ClassListEditor(ClassListParameter<?> option, JFrame owner, ParameterChangeListener l) {
		super(option, owner,l);
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

		addInputVerifier();

		if (((ClassListParameter<?>) option).hasDefaultValue()) {
			List<String> defaultValues = ((ClassListParameter<?>) option).getDefaultValue();
			for (String s : defaultValues) {
				this.textField.setText(s);
			}
			setValue(textField.getText());
		}

		textField.setEditable(false);
		textField.setBackground(Color.white);
		JScrollPane scroller = new JScrollPane();
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scroller.setViewportView(textField);
		inputField.add(scroller);

		JComboBox classSelector = new JComboBox();
		classSelector.setModel(new DefaultComboBoxModel(((ClassListParameter<?>) option).getRestrictionClasses()));

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
	protected int getColumnNumber() {
		return StringEditor.COLUMN_NUMBER;
	}

}
