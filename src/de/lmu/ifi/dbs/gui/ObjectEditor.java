package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;

public class ObjectEditor extends JPanel implements EditObjectChangeListener, PopUpTreeListener {

	private JTextPane displayField;

//	private CustomizerPanel custom;

	private JButton chooseButton;

	private JScrollPane pane;

	private JPanel scrollPanel;

	private PopUpTree treeMenu;
	
	private EditObject editObject;

	public ObjectEditor(Class type) {
//		this.classType = type;
		
		// create editObject
		editObject = new EditObject(type);
		editObject.addEditObjectListener(this);
		
		treeMenu = new PopUpTree(getPropertyFileInfo(type), "");
		treeMenu.addPopUpTreeListener(this);
		createChooseButton();

		getDisplayField();

	}	

//	private void setEditObject(Object obj) {
//
//		// TODO
//		// checken, ob value ein Object der gewuenschten Klasse ist (also z.B.
//		// Unterklasse von database connection...)
//		if (!classType.isAssignableFrom(obj.getClass())) {
//			System.err.println("setValue object not of correct type!");
//			return;
//		}
//
//		// event. EditorPane neu zeichnen!
//		this.editObject = obj;
//
//		respondToEditObject();
//	}

//	private void respondToEditObject() {
//		// TODO allgemein testen, ob Parameter gesetzt sind!
//		if (hasDefaultParameters()) {
//
//			updateDisplayField();
//		} else if (editObject instanceof Parameterizable) {
//			// show CustomizerPanel
//
//			custom = new CustomizerPanel((Parameterizable) editObject);
//			custom.addPropertyChangeListener(this);
//
//			custom.setVisible(true);
//		} else {
//			updateDisplayField();
//		}
//	}

	private void createChooseButton() {

		chooseButton = new JButton("Choose");
		chooseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				treeMenu.show(chooseButton, chooseButton.getLocation().x, chooseButton.getLocation().y);
			}

		});
		add(chooseButton);
	}

	private String[] getPropertyFileInfo(Class classType) {

		// check if we got a property file
		if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(classType));
		}
		return new String[] {};

	}

//	public void setSelectedClass(String className) {
//
//		// class is already set
//		if (editObject != null && editObject.getClass().getName().equals(className)) {
//			respondToEditObject();
//
//		} else {
//			try {
//
//				System.out.println("objectEditor: " + className);
//				setEditObject(Class.forName(className).newInstance());
//
//			} catch (InstantiationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//	}

	
	private void getDisplayField() {

		displayField = new JTextPane() {

			public Dimension getPreferredScrollableViewportSize() {
				Dimension dim = getPreferredSize();
				dim.width = 500;

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

		displayField.setEditable(false);
		// set the right color
		displayField.setBackground(Color.white);

		pane = new JScrollPane();

		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		pane.setViewportView(displayField);

		displayField.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

				// TODO editObject muss hier übernehmen!
//				if (custom != null) {
//					custom.setVisible(true);
//				}
			}
		});

		scrollPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.VERTICAL;
		scrollPanel.add(pane, gbc);
		Dimension dim = scrollPanel.getPreferredSize();
		dim.height = 50;
		scrollPanel.setPreferredSize(dim);

		add(scrollPanel);
	}

	private void updateDisplayField() {

		// remove old text

		displayField.setText(null);

		String name = editObject.getClassName();
		
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex != -1) {
			name = name.substring(dotIndex + 1);
		}

		StyledDocument doc = displayField.getStyledDocument();

		try {

			SimpleAttributeSet set = new SimpleAttributeSet();

			StyleConstants.setBold(set, true);

			doc.insertString(doc.getLength(), name, set);

			set = new SimpleAttributeSet();

			doc.insertString(doc.getLength(), " " + editObject.getParameters(), set);

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		displayField.setCaretPosition(0);
		scrollPanel.revalidate();

	}

//	public Object getValue() {
//
//		return editObject;
//	}

//	private boolean hasDefaultParameters() {
//
//		System.out.println((Parameterizable) editObject);
//		return false;
//	}

//	private String getObjectParameters() {
//		StringBuffer buffer = new StringBuffer();
//		for (Option opt : ((Parameterizable) editObject).getPossibleOptions()) {
//			buffer.append(" -");
//			buffer.append(opt.getName());
//			buffer.append(" ");
//			// buffer.append(opt.getValue());
//		}
//
//		return buffer.toString();
//	}

	public void propertyChange(PropertyChangeEvent evt) {
		updateDisplayField();

	}

	public String getEditObjectAsString() {

		if (editObject == null) {
			return null;
		}

		StringBuilder buffy = new StringBuilder();
		buffy.append(editObject.getClassName());
		buffy.append(" ");
		buffy.append(editObject.getParameters());
		return buffy.toString();

	}

	/**
	 * @see de.lmu.ifi.dbs.gui.PopUpTreeListener#selectedClassChanged(java.lang.String)
	 */
	public void selectedClassChanged(String selectedClass) {
		editObject.setEditObjectClass(selectedClass);
	}

	public void editObjectChanged() {
		updateDisplayField();
		
	}

}
