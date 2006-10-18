package de.lmu.ifi.dbs.gui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import de.lmu.ifi.dbs.utilities.optionhandling.Option;

public abstract class ParameterEditor {

	protected JLabel nameLabel;
	
	protected JComponent inputField;
	
	protected Option option;
	
	protected JFrame owner;
	
	protected String value;
	
	protected JButton helpLabel;

	public static final String PATH_HELP_ICON = "de/lmu/ifi/dbs/gui/images/shapes018.gif";
	
	public ParameterEditor(Option option,JFrame owner){
		
		this.option = option;
		this.owner = owner;

		nameLabel = new JLabel(option.getName());
		nameLabel.setToolTipText(option.getName());		
		
		helpLabel = new JButton();
		ImageIcon icon = new ImageIcon(PATH_HELP_ICON);
		icon.setImage(icon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH));
		helpLabel.setIcon(icon);
		helpLabel.setBorderPainted(false);
		helpLabel.setContentAreaFilled(false);
		helpLabel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				KDDDialog.showMessage(ParameterEditor.this.owner, ParameterEditor.this.option.getName()+
						":\n"+ ParameterEditor.this.option.getDescription());
			}
		});
	}
	
	protected abstract void createInputField();
			
	
	public abstract boolean isValid();
	
	public JComponent getInputField(){
	
		return inputField;
	}
	
	public JComponent getNameLabel()
	{
		return nameLabel;
	}
}
