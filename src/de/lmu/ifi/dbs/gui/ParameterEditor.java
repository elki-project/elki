package de.lmu.ifi.dbs.gui;

import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public abstract class ParameterEditor {

	protected JLabel nameLabel;

	protected JComponent inputField;

	protected Option<?> option;

	protected Window owner;

	protected String value;

	protected JButton helpLabel;

	public static final String PATH_HELP_ICON = "src\\de\\lmu\\ifi\\dbs\\gui\\images\\shapes018.gif";

	public ParameterEditor(Option<?> option, Window owner) {

		this.option = option;
		this.owner = owner;

		nameLabel = new JLabel(option.getName());
		nameLabel.setToolTipText(option.getName());

		helpLabel = new JButton();
		ImageIcon icon = new ImageIcon(PATH_HELP_ICON);
		icon.setImage(icon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH));
		helpLabel.setIcon(icon);
		helpLabel.setPressedIcon(new ImageIcon("src\\de\\lmu\\ifi\\dbs\\gui\\images\\afraid.gif"));
//		helpLabel.setRolloverEnabled(true);
//		helpLabel.setRolloverIcon(icon);
//		helpLabel.setRolloverSelectedIcon(icon);
		helpLabel.setBorderPainted(false);
		helpLabel.setContentAreaFilled(false);
		helpLabel.setRolloverEnabled(true);
		helpLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KDDDialog.showMessage(ParameterEditor.this.owner, ParameterEditor.this.option.getName() + ":\n" + ParameterEditor.this.option.getDescription());
			}
		});
	}

	protected abstract void createInputField();

	public abstract boolean isValid();
	
	
	public JComponent getInputField() {

		return inputField;
	}

	public JComponent getNameLabel() {
		return nameLabel;
	}

	public void setValue(String value) {
		this.value = value;
		
		try {
			option.setValue(value);
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		}
		// try {
		// if (isValid()) {
		// this.value = value;
		// }
		// } catch (ParameterException e) {
		// e.printStackTrace();
		// }
	}

	public String getValue() {
		return value;
	}

	public String[] parameterToValue() {
		
		String[] paramToValue = new String[2];
		paramToValue[0] = "-" + option.getName();
		paramToValue[1] = getValue();

		return paramToValue;
	}

	public String getDisplayableValue() {
		StringBuffer value = new StringBuffer();
		value.append("-");
		value.append(option.getName());
		value.append(" " + getValue());
		return value.toString();
	}

}
