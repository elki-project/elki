package de.lmu.ifi.dbs.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class KDDTabbedPane extends JTabbedPane {

	
	private DatabasePanel dbPanel;
	
	private AlgorithmPanel algPanel;
	
	
	public KDDTabbedPane(JFrame owner){
		
		// add the database panel
		dbPanel = new DatabasePanel(owner);
		addTab(dbPanel.getTitle(), null, dbPanel, dbPanel.getToolTipText());
		
		// add the algorithm panel
		algPanel = new AlgorithmPanel();
		addTab(algPanel.getTitle(),null, algPanel, algPanel.getToolTipText());
		
		//add the result panel
		
	}
}
