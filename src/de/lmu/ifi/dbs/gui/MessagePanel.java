package de.lmu.ifi.dbs.gui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MessagePanel extends JPanel {

	private JTextArea messageField =  new JTextArea(1,10);
	
	public MessagePanel(){
		
		messageField.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(messageField);
		add(scrollPane);
	}
	
}
