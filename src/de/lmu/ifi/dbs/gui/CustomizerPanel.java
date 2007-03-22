package de.lmu.ifi.dbs.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.lmu.ifi.dbs.utilities.optionhandling.*;

public class CustomizerPanel extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1468920824680345029L;

	private JFrame owner;

	private Vector<ParameterEditor> paramEditors;

	private String[] parametersToValues;

	private boolean validParameters;

	public static final String FONT = "Tahoma";

	public static final int FONT_SIZE = 15;

	private ArrayList<EditObjectChangeListener> listener;

	private EditObject editObject;

	private String selectedClass;

	private JButton ok;

	private JButton cancel;

	public CustomizerPanel(Window owner) {
		super(owner);
		setModal(true);
		this.listener = new ArrayList<EditObjectChangeListener>();
	}

	public CustomizerPanel(Window owner, Class<?> type) {

		this(owner);
		editObject = new EditObject(type);
	}

	public CustomizerPanel(Window owner, Class<?> type, String selectedClass) {
		this(owner);
		editObject = new EditObject(type);
		editObject.setEditObjectClass(selectedClass);
		react();
	}

	private void react() {

		// check if editObject is parameterizable
		if (editObject.isParameterizable()) {

			createDisplayPanel();
			pack();
			ok.requestFocusInWindow();
			setVisible(true);
		}
		// TODO else
	}

	/**
	 * Creates the proper display panel (only if the edit object is
	 * Parameterizable).
	 * 
	 */
	private void createDisplayPanel() {

		// try to remove old display panel
		this.getContentPane().removeAll();

		// get parameter editors
		getParameterEditors(editObject.getOptions());

		JPanel base = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weighty = 0.8;
		gbc.weightx = 0.5;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(20, 10, 25, 10);

		base.add(titleLabel(), gbc);

		gbc.gridx = 3;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(5, 5, 5, 10);
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		base.add(getAboutButton(), gbc);

		// parameter editors
		if (paramEditors.size() != 0) {
			JPanel parameters = new JPanel(new GridBagLayout());
			GridBagConstraints pGbc = new GridBagConstraints();
			pGbc.gridy = 0;
			pGbc.weighty = 1.0;
			for (ParameterEditor edit : paramEditors) {

				// pGbc.gridy = pGbc.gridy + 1;

				// add title label
				pGbc.gridx = 0;
				pGbc.gridwidth = 1;
				pGbc.anchor = GridBagConstraints.WEST;
				pGbc.insets = new Insets(2, 15, 2, 15);
				parameters.add(edit.getNameLabel(), pGbc);

				// add inputfield
				pGbc.gridx = 1;
				pGbc.gridwidth = 3;
				// gbc.anchor = GridBagConstraints.EAST;
				parameters.add(edit.getInputField(), pGbc);
				pGbc.gridy = pGbc.gridy + 1;

			}

			gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.gridx = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.fill = GridBagConstraints.VERTICAL;
			// gbc.gridheight = pGbc.gridy;

			parameters
					.setBorder(BorderFactory.createLineBorder(Color.darkGray));
			parameters.setBorder(BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED));
			base.add(parameters, gbc);
		}
		// add buttons
		gbc.weighty = 0.8;
		gbc.ipadx = 5;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints pGbc = new GridBagConstraints();
		pGbc.ipadx = 3;
		pGbc.fill = GridBagConstraints.HORIZONTAL;
		pGbc.insets = new Insets(0, 10, 10, 5);

		gbc.gridx = 1;

		gbc.gridy = gbc.gridy + 1;
		gbc.gridwidth = 2;
		// gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;

		buttonPanel.add(okButton(), pGbc);
		buttonPanel.add(cancelButton(), pGbc);
		base.add(buttonPanel, gbc);

		add(base);
	}

	private void getParameterEditors(Option<?>[] options) {

		// TODO if options is null

		this.paramEditors = new Vector<ParameterEditor>();
		this.parametersToValues = new String[options.length];

		int counter = 0;
		for (Option<?> opt : options) {

			paramEditors.add(getProperEditor(opt));
			counter++;
		}
	}

	/**
	 * Returns true if all parameter editors hold valid parameter values, false
	 * otherwise.
	 * 
	 * @return true if all parameter values are valid, false otherwise.
	 */
	private boolean checkParameters() {

		if (paramEditors.isEmpty()) {
			parametersToValues = new String[] {};
			return true;
		}

		ArrayList<String> temp = new ArrayList<String>();
		for (ParameterEditor edit : paramEditors) {

			if (!edit.isValid()) {

				return false;
			}
			temp.addAll(Arrays.asList(edit.parameterToValue()));
		}

		// check global constraints
		try {
			editObject.checkGlobalConstraints();
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, "Global constraint error: "+e.getMessage(), e);
		}

		parametersToValues = temp.toArray(new String[] {});
		return true;
	}

	private JComponent okButton() {

		ok = new JButton("OK");
		// ok.setSelected(true);
		// getRootPane().setDefaultButton(ok);
		ok.requestFocusInWindow();
		ok.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (checkParameters()) {

					setVisible(false);
					validParameters = true;

					fireEditObjectChanged();

				}
				// else {
				// System.out.println("check parameters false!");
				// System.out.println(editObject.toString());
				// }
			}
		});
		return ok;
	}

	private JComponent cancelButton() {
		this.cancel = new JButton("Cancel");
		this.cancel.setSelected(false);
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		return cancel;
	}

	public boolean validParameters() {
		return validParameters;
	}

//	@SuppressWarnings("unchecked")
	private ParameterEditor getProperEditor(Option<?> opt) {

		if (opt instanceof ClassParameter) {

			for (String cl : ((ClassParameter<?>) opt).getRestrictionClasses()) {

				try {
					if (Class.forName(cl).newInstance() instanceof Parameterizable) {
						return new ObjectEditor(((ClassParameter<?>) opt)
								.getRestrictionClass(), (Option<String>)opt, this);
					}
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return new ClassEditor((Option<String>)opt, owner);
		}
		if (opt instanceof ClassListParameter) {
			return new ClassListEditor((Option<String>)opt, owner);
		}
		if (opt instanceof DoubleParameter) {
			return new DoubleEditor((Option<Double>)opt, owner);
		}
		if (opt instanceof DoubleListParameter) {
			return new DoubleListEditor((Option<Double>)opt, owner);
		}
		if (opt instanceof IntParameter) {
			return new IntegerEditor((Option<Integer>)opt, owner);
		}
		if (opt instanceof FileParameter) {
			return new FileEditor((Option<File>)opt, owner);
		}
		if (opt instanceof FileListParameter) {
			return new FileListEditor((Option<File>)opt, owner);
		}
		if (opt instanceof Flag) {
			return new FlagEditor((Option<Boolean>)opt, owner);
		} else {
			return new StringEditor((Option<String>)opt, owner);
		}
	}

	private JComponent titleLabel() {

		JTextPane titlePane = new JTextPane();
		StyledDocument doc = titlePane.getStyledDocument();

		SimpleAttributeSet set = new SimpleAttributeSet();

		StyleConstants.setBold(set, true);
		StyleConstants.setFontFamily(set, FONT);
		StyleConstants.setFontSize(set, FONT_SIZE);

		try {
			doc.insertString(doc.getLength(), editObject.getClassName(), set);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// titlePane.setBackground(getParent().getBackground());
		titlePane.setBackground(Color.LIGHT_GRAY);
		titlePane.setEditable(false);

		JPanel panel = new JPanel();
		panel.setBackground(Color.LIGHT_GRAY);
		panel.add(titlePane);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createLineBorder(Color.DARK_GRAY), BorderFactory
				.createEmptyBorder(0, 10, 0, 10)));

		return panel;
	}

	private JComponent getAboutButton() {

		JButton about = new JButton("About");
		about.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// show About-Dialog
				KDDDialog.showAboutMessage(owner, editObject);
			}
		});
		return about;
	}

	public String getParameterValuesAsString() {
		StringBuilder values = new StringBuilder();
		for (int i = 0; i < parametersToValues.length; i++) {
			values.append(parametersToValues[i]);
			if (i != parametersToValues.length - 1) {
				values.append(" ");
			}
		}

		return values.toString();
	}

	public void setVisible(boolean v) {
		// requestFocusInWindow();
		super.setVisible(v);
	}

	private void fireEditObjectChanged() {

		editObject.updateParameters(parametersToValues);

		for (EditObjectChangeListener l : listener) {
			// l.editObjectChanged(name, parameters);
			l.editObjectChanged(editObject.getClassName(), parametersToValues);
		}

	}

	public void addEditObjectChangeListener(EditObjectChangeListener l) {
		listener.add(l);
	}

	public void setEditObjectClass(String selectedClass) {

		if (!selectedClass.equals(this.selectedClass)) {
			this.selectedClass = selectedClass;
			editObject.setEditObjectClass(selectedClass);
			react();
		} else {
			setVisible(true);
		}

	}
}
