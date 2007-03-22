package de.lmu.ifi.dbs.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;

public class DatabasePanel extends JPanel {

	public static final String TITLE = "database";

	public static final String TOOL_TIP = "Choose a database connection, a parser, and a database";
	
	
	private JPanel inputPanel;
//	
	/**
	 * Panel showing the current database connection.
	 */
	private PropertyPanel connectionPanel;
	
	/**
	 * Panel showing the current parser.
	 */
	private PropertyPanel parserPanel;
	
	/**
	 * Panel showing the current database.
	 */
	private PropertyPanel databasePanel;
	
	private JFrame owner;

	public DatabasePanel(JFrame owner) {
		
		this.owner = owner;

		createDBConnectionPanel();
//		add(dbConnection, BorderLayout.CENTER);
		createAlgorithmPanel();

	}

	private void createDBConnectionPanel() {

//		dbConnection = new JPanel(new GridBagLayout());
		JPanel title = new JPanel();
		title.add(new JLabel("Database Connection"));

//		ObjectEditor editor = new ObjectEditor(DatabaseConnection.class);
//		editor.setTitle("database connection");
		
		connectionPanel = new PropertyPanel();
		
		JPanel conPanel = new JPanel(new BorderLayout());
		conPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		conPanel.add(title, BorderLayout.NORTH);
//		conPanel.add(editor, BorderLayout.CENTER);
		conPanel.setToolTipText(TOOL_TIP);
		
		add(conPanel);
		
		


	}
	
	
	private void createAlgorithmPanel(){
		
		JPanel title = new JPanel();
		title.add(new JLabel("Algorithm"));

//		ObjectEditor editor = new ObjectEditor(Algorithm.class);
//		editor.setTitle("algorithm");
		
		connectionPanel = new PropertyPanel();
		
		JPanel conPanel = new JPanel(new BorderLayout());
		conPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		conPanel.add(title, BorderLayout.NORTH);
//		conPanel.add(editor, BorderLayout.CENTER);
		conPanel.setToolTipText(TOOL_TIP);
		
		add(conPanel);
		
	}
	
	public String getTitle() {
		return TITLE;
	}
}
