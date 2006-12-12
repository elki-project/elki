package de.lmu.ifi.dbs.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.lmu.ifi.dbs.utilities.optionhandling.*;

public class CustomizerPanel extends JDialog {

	private JFrame owner;

	private Parameterizable editObject;

	private JComponent aboutPanel;

	private Vector<ParameterEditor> paramEditors;

	private String[] parameterValues;

	private boolean validParameters;

	public static final String FONT = "Tahoma";

	public static final int FONT_SIZE = 15;

	public CustomizerPanel(JFrame owner, Parameterizable obj) {

		super(owner, obj.getClass().getName(), true);
		this.owner = owner;
		this.editObject = obj;
		this.validParameters = false;

		// get parameter editors
		getParameterEditors(editObject.getPossibleOptions());

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

			parameters.setBorder(BorderFactory.createLineBorder(Color.darkGray));
			parameters.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
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
		pGbc.insets = new Insets(0,10,10,5);
		
		gbc.gridx = 1;

		gbc.gridy = gbc.gridy + 1;
		gbc.gridwidth = 2;
		// gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;

		buttonPanel.add(okButton(),pGbc);
		buttonPanel.add(cancelButton(),pGbc);
		base.add(buttonPanel, gbc);

		add(base);

		pack();
	
	}

	private void getParameterEditors(Option[] options) {

		paramEditors = new Vector<ParameterEditor>();
		parameterValues = new String[options.length];

		for (Option opt : options) {

			paramEditors.add(getProperEditor(opt));
		}
	}

	private JComponent aboutPanel() {

		JTextArea about = new JTextArea();
		about.setBorder(BorderFactory.createTitledBorder("About"));

		about.setEditable(false);
		about.setColumns(40);
		about.setLineWrap(true);
		about.setWrapStyleWord(true);
		// about.setText(((Parameterizable) editObject).description());
		// getDescription
		about.setText(editObject.getClass().getName());
		about.setBackground(getBackground());
		JPanel panel = new JPanel();
		panel.add(titleLabel());
		return panel;
		// return about;
	}

	private boolean checkParameters() {

		int i = 0;
		for (ParameterEditor edit : paramEditors) {

			if (!edit.isValid()) {
				return false;
			}
			parameterValues[i] = edit.getDisplayableValue();
			i++;
		}

		return true;
	}

	private JComponent okButton() {

		JButton ok = new JButton("OK");
		;
		ok.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (checkParameters()) {
					setVisible(false);
					validParameters = true;
					firePropertyChange("", null, null);
				}
			}
		});
		return ok;
	}

	private JComponent cancelButton() {
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		return cancel;
	}

	public boolean validParameters() {
		return validParameters;
	}

	private ParameterEditor getProperEditor(Option opt) {

		if (opt instanceof ClassParameter) {
			return new ClassEditor(opt, owner);
		}
		if (opt instanceof ClassListParameter) {
			return new ClassListEditor(opt, owner);
		}
		if (opt instanceof DoubleParameter) {
			return new DoubleEditor(opt, owner);
		}
		if (opt instanceof DoubleListParameter) {
			return new DoubleListEditor(opt, owner);
		}
		if (opt instanceof IntParameter) {
			return new IntegerEditor(opt, owner);
		}
		if (opt instanceof FileParameter) {
			return new FileEditor(opt, owner);
		}
		if (opt instanceof FileListParameter) {
			return new FileListEditor(opt, owner);
		}
		if (opt instanceof Flag) {
			return new FlagEditor(opt, owner);
		} else {
			return new StringEditor(opt, owner);
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
			doc.insertString(doc.getLength(), editObject.getClass().getName(), set);
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
				.createLineBorder(Color.DARK_GRAY), BorderFactory.createEmptyBorder(0, 10, 0, 10)));

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
		StringBuffer values = new StringBuffer();
		for(int i = 0; i < parameterValues.length; i++){
			values.append(parameterValues[i]);
			if(i != parameterValues.length-1){
				values.append(" ");
			}
		}

		return values.toString();
	}

	public void setVisible(boolean v){
		super.setVisible(v);
		pack();
		
	}
	
	
	
}
