package de.lmu.ifi.dbs.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class PopUpTree extends JPopupMenu {

	private String[] classes;

	private JTree tree;

	private String title;

	public static final String PREFIX = "de.lmu.ifi.dbs.database.connection";

	public static final String SUBSTRING = "connection";

	private DefaultMutableTreeNode root;

	public PopUpTree(String[] classNames, String title) {

		classes = classNames;

		root = new DefaultMutableTreeNode(title);
		
		add(getTreePane());
		
		pack();
		
		setVisible(true);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	
	private Component getTreePane(){
		
		JPanel treePane = new JPanel();
		
		JTree tree = new JTree(createTree());
		treePane.add(tree);
		treePane.setBackground(tree.getBackground());
		
		JScrollPane scroller = new JScrollPane(treePane);
		
		scroller.setPreferredSize(new Dimension(200,150));
		
		
		return scroller;
	}
	
	private DefaultTreeModel createTree() {

		for (String obj : classes) {

			// check if class is from a dbs-package
			if (obj.startsWith(PREFIX)) {

				System.out.println("o:" + obj);

				int index = obj.indexOf(SUBSTRING);

				String[] parts = obj.substring(index).split("\\.");

				for (String p : parts) {
					System.out.println("\t"+p);
				}
				// analyse object
				DefaultMutableTreeNode parent = null;
				int parentIndex = -1;
				for (int i = 0; i < parts.length - 1; i++) {

					System.out.println("checking "+parts[i]);
					DefaultMutableTreeNode node = checkRootPath(parts[i]);
					if (node == null) {
						System.out.println(parts[i] +" not found!");
						break;
					}
					parent = node;
					parentIndex = i;
				}

				if (parentIndex == parts.length - 2) {
					parent.add(new DefaultMutableTreeNode(
							parts[parts.length - 1]));
				}

				else {

					if (parentIndex == -1) {
						System.out.println("parentIndex set to 0");
						parentIndex = 0;
						parent = root;
					}

					
					DefaultMutableTreeNode child;
					while (parentIndex < parts.length - 1) {

						System.out.println("add node "+parts[parentIndex]);
						child = new DefaultMutableTreeNode(parts[parentIndex]);
						parent.add(child);
						parent = child;

						parentIndex++;
					}
					parent.add(new DefaultMutableTreeNode(parts[parts.length - 1]));
				}
			}
		}

		return new DefaultTreeModel(root);
	}

	private DefaultMutableTreeNode checkRootPath(String userObject) {

		for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements();) {
			DefaultMutableTreeNode current = (DefaultMutableTreeNode) e
					.nextElement();
			System.out.println("root child: "+current.toString());
			if (current.getUserObject().toString().equals(userObject)) {

				return current;
			}

		}
		return null;
	}

}
