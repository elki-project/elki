package de.lmu.ifi.dbs.gui;

import de.lmu.ifi.dbs.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.FileListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;

import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Abstract superclass for modeling parameter editors.
 * <p>
 * This class provides the basic structures to create a graphical user interface
 * for editing miscellaneous options of a parameterizable object.
 * </p>
 * 
 * @author Steffi Wanka
 * 
 */
public abstract class ParameterEditor {

	/**
	 * Displays the name of the option
	 */
	protected JLabel nameLabel;

	/**
	 * The component for displaying and entering the option value.
	 */
	protected JComponent inputField;

	/**
	 * The option to be presented by this parameter editor.
	 */
	protected Option<?> option;

	/**
	 * The window containing this parameter editor.
	 */
	protected Window owner;

	/**
	 * Represents the current option value.
	 */
	protected String value;

	/**
	 * Help button of this parameter editor.
	 */
	protected JButton helpLabel;

	/**
	 * The ParameterChangeListener of this parameter editor. This usually the
	 * window containing this parameter editor.
	 */
	protected ParameterChangeListener l;

	/**
	 * Symbol used for the help button.
	 */
	public static final String PATH_HELP_ICON = "src\\de\\lmu\\ifi\\dbs\\gui\\images\\shapes018.gif";

	/**
	 * Creates a new parameter editor for the given option with the given owner
	 * and the given ParameterChangeListener.
	 * <p>
	 * Sets up the name and help label and invokes {@link #createInputField()}.
	 * </p>
	 * 
	 * @param option
	 *            the option to be presented by this parameter editor
	 * @param owner
	 *            the component containing this parameter editor
	 * @param l
	 *            the ParameterChangeListener of this parameter editor
	 */
	public ParameterEditor(Option<?> option, Window owner, ParameterChangeListener l) {

		this.option = option;
		this.owner = owner;
		this.l = l;

		nameLabel = new JLabel(option.getName());
		nameLabel.setToolTipText(option.getName());

		helpLabel = new JButton();
		ImageIcon icon = new ImageIcon(PATH_HELP_ICON);
		icon.setImage(icon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH));
		helpLabel.setIcon(icon);
		helpLabel.setPressedIcon(new ImageIcon("src\\de\\lmu\\ifi\\dbs\\gui\\images\\afraid.gif"));
		helpLabel.setBorderPainted(false);
		helpLabel.setContentAreaFilled(false);
		helpLabel.setRolloverEnabled(true);
		helpLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				KDDDialog.showMessage(ParameterEditor.this.owner, ParameterEditor.this.option.getName() + ":\n" + ParameterEditor.this.option.getDescription());
			}
		});
		this.createInputField();
	}

	protected abstract void createInputField();

	public abstract boolean isOptional();

	public JComponent getInputField() {

		return inputField;
	}

	public JComponent getNameLabel() {
		return nameLabel;
	}

	public void setValue(String value) {

		try {
			option.setValue(value);
			this.value = value;
			this.fireParameterChangeEvent(new ParameterChangeEvent(this, option.getName(), "", value));
		} catch (ParameterException e) {
			KDDDialog.showParameterMessage(owner, e.getMessage(), e);
		}
	}

	public String getValue() {
		return value;
	}

	public static ParameterEditor createEditor(Option<?> p, JFrame owner, ParameterChangeListener l) {

		if (p instanceof ClassParameter<?>) {
			if (Parameterizable.class.isAssignableFrom(((ClassParameter<?>) p).getRestrictionClass())) {
				return new ParameterizableEditor((ClassParameter<?>) p, owner, l);
			}
			return new ClassEditor((ClassParameter<?>) p, owner, l);
		}

		if (p instanceof ClassListParameter) {
			return new ClassListEditor((ClassListParameter) p, owner, l);
		}

		if (p instanceof DoubleParameter) {
			return new DoubleEditor((DoubleParameter) p, owner, l);
		}

		if (p instanceof DoubleListParameter) {
			return new DoubleListEditor((DoubleListParameter) p, owner, l);
		}

		if (p instanceof FileParameter) {
			return new FileEditor((FileParameter) p, owner, l);
		}

		if (p instanceof FileListParameter) {
			return new FileListEditor((FileListParameter) p, owner, l);
		}

		if (p instanceof Flag) {
			return new FlagEditor((Flag) p, owner, l);
		}

		if (p instanceof IntParameter) {
			return new IntegerEditor((IntParameter) p, owner, l);
		}

		if (p instanceof StringParameter) {
			return new StringEditor((StringParameter) p, owner, l);
		}
		// TODO Fehlermeldung
		return null;
	}

	protected void fireParameterChangeEvent(ParameterChangeEvent evt) {
		this.l.parameterChanged(evt);
	}
}
