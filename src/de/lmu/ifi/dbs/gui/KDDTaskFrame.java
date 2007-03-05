package de.lmu.ifi.dbs.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;

public class KDDTaskFrame extends JFrame  {
	
	
	private KDDTask task;

	public KDDTaskFrame() {
		setTitle("KDD Task");

		task = new KDDTask();
		
		JPanel mainPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		// task name
		mainPanel.add(taskNamePanel(), gbc);
		// database connection
		gbc.gridy = 1;
		gbc.gridx = 1;
//		mainPanel.add(getDatabaseConnectionPanel(), gbc);

		 mainPanel.add(new DatabaseConnectionPanel(),gbc);
		// algorithm panel
		gbc.gridy = 2;
		mainPanel.add(new AlgorithmPanel(), gbc);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(new Dimension(300, 270));
		add(mainPanel);
		 pack();
		setVisible(true);
	}

	// task name panel
	private JComponent taskNamePanel() {

		JPanel taskName = new JPanel();

		// label
		taskName.add(new JLabel("Task Name"));

		JTextField nameField = new JTextField();
		// default name ist date_time
		nameField.setColumns(15);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.S");
		// System.out.println( formater.format(cal.getTime()) );
		nameField.setText(formater.format(cal.getTime()));
		nameField.setCaretPosition(0);

		taskName.add(nameField);

		return taskName;
	}

	

	

	private String[] getPropertyFileInfo(Class type) {

		// check if we got a property file
		if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName.getOrCreatePropertyName(type));
		}
		return new String[] {};

	}

	
	
	private class DatabaseConnectionPanel extends JPanel implements PopUpTreeListener{
		
		/**
		 * the popup-menu of this panel
		 */
		private PopUpTree menu;
		
		/**
		 * the select button
		 */
		private JButton select;
		
		/**
		 * the name label
		 */
		private JLabel nameLabel;
		
		/**
		 * the text field
		 */
		private JTextField dbField;
		
		/**
		 * the object editor of this panel
		 */
		private ObjectEditor editor;

		private EditObject editObject;
		
		private String dbClass;
		
		/**
		 * Constructs a swing-panel for the dababase connection selection
		 *
		 */
		public DatabaseConnectionPanel(){
			
			//gridbaylayout
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			
			// instantiate the popup-menu
			menu = new PopUpTree(getPropertyFileInfo(DatabaseConnection.class),FileBasedDatabaseConnection.class.getName());
			//don't forget to register as listener
			menu.addPopUpTreeListener(this);
			
			editObject = new EditObject(DatabaseConnection.class);
			editor = new ObjectEditor(DatabaseConnection.class);
			
			// name label
			nameLabel = new JLabel("Database Connection");
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(10,10,10,10);
			add(nameLabel,gbc);
			
			//select button
			createSelectButton();
			gbc.gridx = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.LINE_END;
			add(select, gbc);
		
			//text field
			createTextField();
			gbc.gridy = 2;
			gbc.gridwidth = 4;
			gbc.gridx = 0;
			gbc.insets = new Insets(5,10,10,10);
			gbc.anchor = GridBagConstraints.CENTER;
			add(dbField, gbc);
			
			
			//border
			setBorder(BorderFactory.createEtchedBorder());
		}
		
		private void createSelectButton(){
			select = new JButton("Select");
			
			
			select.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					menu.show(select, nameLabel.getLocation().x, select.getLocation().y);
				}
			});
		}
		
		private void createTextField(){
			dbField = new JTextField(20){
				
				public void setText(String text){
					if(text != null || !(text.equals(""))){
						super.setText(text);
						this.setToolTipText("Click for details");
						this.setCaretPosition(0);
					}
					else{
						super.setText("");
						
						setToolTipText("Select database connection");
					}
				}
			};
			dbField.setEditable(false);
			dbField.setBackground(Color.white);
			dbField.setToolTipText("Select database connection");
			
			
		}

		public void selectedClassChanged(String selectedClass) {

			editObject.setEditObjectClass(selectedClass);
//			editor.setSelectedClass(selectedClass);			
		}
	}
	
	private class AlgorithmPanel extends JPanel implements TreeSelectionListener{

		private JLabel nameLabel;
		
		private PopUpTree menu;
		
		private JButton select;
		
		private JTextField textField;
		
		public AlgorithmPanel(){
			
			//layout
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			
			//instantiate menu
			menu = new PopUpTree(getPropertyFileInfo(Algorithm.class),"");
			menu.addTreeSelectionListener(this);
			
			// name label
			nameLabel = new JLabel("Algorithm");
			nameLabel.setToolTipText("Select an algorithm to be performed");
			gbc.gridwidth = 2;
			gbc.insets = new Insets(10,10,10,10);
			add(nameLabel,gbc);
			
			//select button
			createSelectButton();
			gbc.gridx = 2;
			gbc.anchor = GridBagConstraints.LINE_END;
			add(select,gbc);
			
			// text field
			createTextField();
			gbc.insets = new Insets(5,10,10,10);
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth = 4;
			gbc.anchor = GridBagConstraints.CENTER;
			add(textField,gbc);
			
			setBorder(BorderFactory.createEtchedBorder());
		}
		
		
		public void valueChanged(TreeSelectionEvent e) {
			System.out.println(e.getPath());
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getNewLeadSelectionPath().getLastPathComponent();
			if(!node.isLeaf()){
				return;
			}
			
			textField.setText(node.getUserObject().toString());
			menu.setVisible(false);
			
			//show customizer Panel
		}
		
		private void createSelectButton(){
			select = new JButton("Select");
			select.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					menu.show(select, nameLabel.getLocation().x, select.getLocation().y);
				}
			});
			select.setEnabled(false);
		}
		
		private void createTextField(){
			textField = new JTextField(20){
				
				public void setText(String text){
					if(text != null || !(text.equals(""))){
						super.setText(text);
						this.setToolTipText("Click for details");
						this.setCaretPosition(0);
					}
					else{
						super.setText("");
						
						setToolTipText("Select algorithm");
					}
				}
			};
			textField.setEditable(false);
			textField.setBackground(Color.white);
			textField.setToolTipText("Select algorithm");
			
			
		}
		
	}

}
