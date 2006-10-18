package de.lmu.ifi.dbs.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PropertyPanel extends JPanel {

	
	
	
	public PropertyPanel(){
		
		add(createChooseConnectionButton());
		add(new JLabel("Test"));
	}
	
	private JButton createChooseConnectionButton() {

		JButton chooseButton = new JButton("Choose");
		chooseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				// show JTree popup menu for selecting the database connecting
			}

		});

		return chooseButton;
	}
	
}
