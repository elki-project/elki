package de.lmu.ifi.dbs.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import de.lmu.ifi.dbs.database.connection.DatabaseConnection;

public class DBConnectionPane extends JPanel {

	private JButton select;
	
	private PopUpTree popUpTree;
	
	private JPopupMenu menu;
	
	public DBConnectionPane(){
		
//		setLayout(new Gr)
		JLabel title = new JLabel("Database Connection");
		add(title);
		
		createChooseButton();
//		menu = new PopUpTree(DatabaseConnection.class);
//		select = new JButton("Select");
//		add(select);
////		popUpTree.setLocation(select.getLocation().x, select.getLocation().y);
//		select.addActionListener(new ActionListener(){
//			public void actionPerformed(ActionEvent e){
////				popUpTree.show(select, select.getLocation().x, select.getLocation().y);	
//				maybeShowPopUp(new PopUpTree(DatabaseConnection.class));
//				System.out.println(select.getLocation());
//			}
//		});
	}
	private void createChooseButton() {

		select = new JButton("Choose");
		select.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				System.out.println("actionPerformed: "+select.getLocation());
				maybeShowPopUp();

				// show JTree according to classType
			}

		});
		add(select);
	}
	
	private void maybeShowPopUp() {

		menu.show(select, select.getLocationOnScreen().y, select.getLocationOnScreen().y);
		System.out.println(select.getLocation());
	}
	
}
