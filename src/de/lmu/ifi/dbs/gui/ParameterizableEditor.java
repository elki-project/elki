package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ParameterizableEditor extends ParameterEditor implements PopUpTreeListener,
		EditObjectChangeListener {

	public static final int VIEWPORT_WIDTH = 450;
	
	private JTextPane displayField;

	private JButton chooseButton;

	private JScrollPane pane;

	private JPanel scrollPanel;

	private PopUpTree treeMenu;

	private CustomizerPanel customizer;

	public ParameterizableEditor(ClassParameter<?> option, JFrame owner, ParameterChangeListener l){
		super(option, owner,l);
		
		customizer = new CustomizerPanel(owner, option.getRestrictionClass());
		customizer.addEditObjectChangeListener(this);

		if(((ClassParameter<?>)option).hasDefaultValue()){
			treeMenu = new PopUpTree(getPropertyFileInfo(option.getRestrictionClass()),((ClassParameter<?>)option).getDefaultValue());
		} else{
			treeMenu = new PopUpTree(getPropertyFileInfo(option.getRestrictionClass()), "");	
		}
		
		treeMenu.addPopUpTreeListener(this);
//		createInputField();
		
	}

	private void createChooseButton() {

		chooseButton = new JButton("Choose");
		chooseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				treeMenu.show(chooseButton, chooseButton.getLocation().x, chooseButton.getLocation().y);
			}

		});
		this.inputField.add(chooseButton);
	}

	private String[] getPropertyFileInfo(Class<?> classType) {

		// check if we got a property file
		if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(classType));
		}
		return new String[] {};

	}

	
	public boolean isOptional(){
		return ((ClassParameter<?>)this.option).isOptional();
	}
	
	@SuppressWarnings("serial")
	private void getDisplayField() {

		this.displayField = new JTextPane() {

			public Dimension getPreferredScrollableViewportSize() {
				Dimension dim = getPreferredSize();
				dim.width = VIEWPORT_WIDTH;

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

		this.displayField.setEditable(false);
		// set the right color
		this.displayField.setBackground(Color.white);

		this.pane = new JScrollPane();

		this.pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		this.pane.setViewportView(displayField);

		this.displayField.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

				if (customizer != null && customizer.getSelectedClass()!=null && ParameterizableEditor.this.value!=null) {
					customizer.setVisible(true,chooseButton.getLocationOnScreen());
				}
			}
		});

		scrollPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.VERTICAL;
		scrollPanel.add(pane, gbc);
		Dimension dim = scrollPanel.getPreferredSize();
		dim.height = 50;
		scrollPanel.setPreferredSize(dim);

		inputField.add(scrollPanel);
	}

	private void updateDisplayField(String fullClassName, String parameters) {

		// remove old text
		displayField.setText(null);

		String name = fullClassName;
		int index = name.lastIndexOf(".");
		if (index != -1) {
			name = name.substring(index + 1);
		}

//		String parameters = getDisplayableParameters(parametersToValue);

		StyledDocument doc = displayField.getStyledDocument();

		try {

			SimpleAttributeSet set = new SimpleAttributeSet();

			StyleConstants.setBold(set, true);

			doc.insertString(doc.getLength(), name, set);

			set = new SimpleAttributeSet();

			doc.insertString(doc.getLength(), " " + parameters, set);

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		displayField.setCaretPosition(0);
		scrollPanel.revalidate();

	}

	/**
	 * @see de.lmu.ifi.dbs.gui.PopUpTreeListener#selectedClassChanged(java.lang.String)
	 */
	public void selectedClassChanged(String selectedClass) {

		// TODO perhaps still do the test here!!
		// if(selectedClass.equals(this.selectedClass)){
		// // customizer.
		// }
		// else{
		customizer.setEditObjectClass(selectedClass,chooseButton.getLocationOnScreen());
		// }
	}

	@Override
	protected void createInputField() {

		inputField = new JPanel();

		createChooseButton();

		getDisplayField();

	}

//	@Override
//	public boolean isValid() {
//
//		// false, if display field is empty!
//		if (isEmpty()) {
//			KDDDialog.showMessage(this.owner, "No parameter value given for parameter \"" + option.getName() + "\"");
//			return false;
//		}
//		return true;
//	}

	public void editObjectChanged(String editObjectName, String parameters) {
		updateDisplayField(editObjectName, parameters);
		setValue(editObjectName, parameters);
	}

	private boolean isEmpty() {
		return (displayField.getText().equals("") || displayField.getText() == null);
	}

	private String getDisplayableParameters(String[] parametersToValue) {
		StringBuilder bob = new StringBuilder();
		int counter = 0;
		for (String n : parametersToValue) {
			// System.out.println(n);
			bob.append(n);
			if (counter != parametersToValue.length - 1) {
				bob.append(" ");
			}
			counter++;
		}
		return bob.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.lmu.ifi.dbs.gui.ParameterEditor#parameterToValue()
	 */
	public String[] parameterToValue() {
		String[] temp = getValue().split(" ");
		// System.out.println(option.getName());
		String[] paramToValue = new String[temp.length + 1];
		paramToValue[0] = "-" + option.getName();
		System.arraycopy(temp, 0, paramToValue, 1, temp.length);

		return paramToValue;
	}

	public void setValue(String value) {

		this.value = value;
//		this.firePropertyChangeEvent(new PropertyChangeEvent(this,option.getName(),"",value));
		this.fireParameterChangeEvent(new ParameterChangeEvent(this,option.getName(),"",value));
	}

	private void setValue(String editObjectName, String parameters) {
		try {
			Class.forName(editObjectName);
			option.setValue(editObjectName);
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			KDDDialog.showMessage(owner, e.getMessage());
		}
		if (parameters.isEmpty()) {
			setValue(editObjectName);
		} else {
			setValue(editObjectName + " " + parameters);
		}
	}
}
