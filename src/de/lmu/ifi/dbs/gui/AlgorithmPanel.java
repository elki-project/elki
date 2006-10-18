package de.lmu.ifi.dbs.gui;

import javax.swing.JPanel;


public class AlgorithmPanel extends JPanel {
	
	
	public static final String TITLE = "algorithms";
	
	public static final String TOOL_TIP = "Choose an algorithm";
	
	public AlgorithmPanel(){
		
		setToolTipText(TOOL_TIP);
	}
	
	public String getTitle(){
		return TITLE;
	}

}
