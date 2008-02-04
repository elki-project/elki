package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class CustomizerPanel extends JDialog implements
		ParameterChangeListener, ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1468920824680345029L;

	/**
	 * The owner of this CustomizerPanel
	 */
	private JFrame owner;

	/**
	 * The font used for this CustomizerPanel
	 */
	public static final String FONT = "Tahoma";

	/**
	 * The font size used for this CustomizerPanel
	 */
	public static final int FONT_SIZE = 15;

	/**
	 * The EditObjectChangeListener of this CustomizerPanel
	 */
	private ArrayList<EditObjectChangeListener> listener;

	/**
	 * The edit object of this CustomizerPanel
	 */
	private EditObject editObject;

	private String selectedClass;

	private JButton ok;

	private JButton cancel;

	private String parameterValuesAsString;

	private ParameterPanel parameterPanel;

	public CustomizerPanel(Window owner) {
		super(owner);
		setModal(true);
		this.listener = new ArrayList<EditObjectChangeListener>();
	}

	public CustomizerPanel(Window owner, Class<?> type) {

		this(owner);
		editObject = new EditObject(type);
	}

	private void react(Point p) {

		// check if editObject is parameterizable
		if (editObject.isParameterizable()) {

			// getParameterEditors();
			parameterPanel = new ParameterPanel(editObject.getOptions(), this, this);
			update(false);
			ok.requestFocusInWindow();
			setVisible(true, p);
		}
		// TODO else
	}

	/**
	 * Creates the proper display panel (only if the edit object is
	 * Parameterizable).
	 * 
	 */
	private void createDisplayPanel(boolean showOptionalParameters) {

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

		// gbc.gridy = 1;
		// gbc.gridx = 1;
		// this.showOptionalParameters.setSelected(showOptionalParameters);
		// base.add(this.showOptionalParameters, gbc);

		// parameter editors
		if (editObject.getOptions().length != 0) {
			// if (paramEditors.size() != 0) {
			// JPanel parameters = new JPanel(new GridBagLayout());
			// GridBagConstraints p_gbc = new GridBagConstraints();
			// p_gbc.gridy = 0;
			// p_gbc.weighty = 1.0;
			//
			// if (this.showOptionalParameters != null) {
			// this.showOptionalParameters.setSelected(showOptionalParameters);
			// parameters.add(this.showOptionalParameters, p_gbc);
			// p_gbc.gridy = 2;
			// }
			// else{
			// p_gbc.gridy = 1;
			// }
			//			
			//
			// for (ParameterEditor e : this.paramEditors) {
			//
			// if (e.isOptional() && !showOptionalParameters) {
			// continue;
			// }
			// p_gbc.gridx = 0;
			// p_gbc.gridwidth = 1;
			// p_gbc.anchor = GridBagConstraints.WEST;
			// p_gbc.insets = new Insets(2, 15, 2, 15);
			// parameters.add(e.getNameLabel(), p_gbc);
			//
			// p_gbc.gridx = 1;
			// p_gbc.gridwidth = 3;
			// // gbc.anchor = GridBagConstraints.EAST;
			// parameters.add(e.getInputField(), p_gbc);
			// p_gbc.gridy = p_gbc.gridy + 1;
			//
			// }

			gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.gridx = 1;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.fill = GridBagConstraints.VERTICAL;
			// gbc.gridheight = pGbc.gridy;

			// parameters.setBorder(BorderFactory.createLineBorder(Color.darkGray));
			// parameters.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			// base.add(parameters, gbc);
			parameterPanel.update(showOptionalParameters);
			base.add(parameterPanel, gbc);
			// base.add(new ParameterPanel(editObject.getOptions(), this,this),
			// gbc);
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

	/**
	 * Returns true if all parameter editors hold valid parameter values, false
	 * otherwise.
	 * 
	 * @return true if all parameter values are valid, false otherwise.
	 */
	private boolean checkParameters() {

		try {
			if (editObject.isValid()) {
				this.parameterValuesAsString = editObject.parameterValuesAsString();
			}
			return true;
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
			return false;
		}
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
		// this.cancel.setSelected(false);
		this.cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// CustomizerPanel.this.selectedClass = null;
				setVisible(false);
			}
		});

		return cancel;
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
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), BorderFactory.createEmptyBorder(0, 10, 0, 10)));

		return panel;
	}

	private JComponent getAboutButton() {

		JButton about = new JButton("About");
		about.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// show About-Dialog
				KDDDialog.showAboutMessage(CustomizerPanel.this, editObject);
			}
		});
		return about;
	}

	public void setVisible(boolean v) {
		super.setVisible(v);
	}

	public void setVisible(boolean v, Point p) {
		if (KDDTaskFrame.CENTERED_LOCATION) {
			setLocationRelativeTo(null);
		} else {
			setLocation(p);
		}
		super.setVisible(v);
	}

	private void fireEditObjectChanged() {

		for (EditObjectChangeListener l : listener) {
			l.editObjectChanged(editObject.getClassName(), this.parameterValuesAsString);
		}
	}

	public void addEditObjectChangeListener(EditObjectChangeListener l) {
		listener.add(l);
	}

	public void setEditObjectClass(String selectedClass, Point p) {

		if (!selectedClass.equals(this.selectedClass)) {
			this.selectedClass = selectedClass;

			try {
				editObject.setEditObjectClass(selectedClass);
			} catch (InstantiationException e) {
				KDDDialog.showErrorMessage(owner, e.getMessage(), e);
			} catch (IllegalAccessException e) {
				KDDDialog.showErrorMessage(owner, e.getMessage(), e);
			} catch (ClassNotFoundException e) {
				KDDDialog.showErrorMessage(owner, e.getMessage(), e);
			} catch (UnableToComplyException e) {
				KDDDialog.showErrorMessage(owner, e.getMessage(), e);
			}

			react(p);
		} else {
			setVisible(true, p);
		}

	}

	public String getSelectedClass() {
		return selectedClass;
	}

	public void parameterChanged(ParameterChangeEvent evt) {
		System.out.println("ParameterChangeEvent, parameter "+evt.getParameterName()+" value: "+evt.getNewValue());
		try {
			if (evt.getSource() instanceof ParameterizableEditor) {
				this.editObject.setOptionValue((String) evt.getParameterName(), evt.getNewValue(), false);
			} else {
				this.editObject.setOptionValue((String) evt.getParameterName(), evt.getNewValue(), true);
			}
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		}
	}

	private void update(boolean showOptionalParameters) {
		this.getContentPane().removeAll();
		createDisplayPanel(showOptionalParameters);
		pack();
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			this.update(true);
		} else {
			this.update(false);
		}
	}
	
	public String[] parameterValuesToArray() {
		return editObject.parameterValuesToArray();
	}
}
