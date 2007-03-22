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

public abstract class ParameterEditor {

	protected JLabel nameLabel;
	
	protected JComponent inputField;
	
	protected Option<?> option;
	
	protected Window owner;
	
	private String value;
	
	protected JButton helpLabel;

	public static final String PATH_HELP_ICON = "src\\de\\lmu\\ifi\\dbs\\gui\\images\\shapes018.gif";
	
	public ParameterEditor(Option<?> option,Window owner){
		
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
	
	public void setValue(String value){
		this.value = value;
	}
	
	public String getValue(){
		return value;
	}
	
	public String[] parameterToValue(){
//		System.out.println(option.getName());
		String[] paramToValue = new String[2];
		paramToValue[0] = "-"+option.getName();
		if(!isValid()){
			paramToValue[1] = "";
		}else{
			paramToValue[1]=getValue();
		}
		
		return paramToValue;
	}
	
	public String getDisplayableValue(){
		StringBuffer value = new StringBuffer();
		value.append("-");
		value.append(option.getName());
		value.append(" "+getValue());
		return value.toString();
	}

}
