package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

public class ObjectEditor extends JPanel {

	private Class classType;

	private Object editObject;

	private String title;

	public static final String PREFIX = "de.lmu.ifi.dbs";

	private DefaultMutableTreeNode root;

	private JTree tree;

	private JPopupMenu menu;

	private JTextPane displayField;

	private JFrame owner;

	private CustomizerPanel custom;

	private JButton chooseButton;

	private JScrollPane pane;

	private JPanel scrollPanel;

	public ObjectEditor(Class type, JFrame owner) {
		this.classType = type;
		this.owner = owner;

		getPropertyFileInfo();
		createChooseButton();

		getDisplayField();
	}

	public void setEditObject(Object obj) {

		// TODO
		// checken, ob value ein Object der gewünschten Klasse ist (also z.B.
		// Unterklasse von database connection...)
		if (!classType.isAssignableFrom(obj.getClass())) {
			System.err.println("setValue object not of correct type!");
			return;
		}

		// event. EditorPane neu zeichnen!
		this.editObject = obj;

		// TODO allgemein testen, ob Parameter gesetzt sind!
		if (hasDefaultParameters()) {
			// update display field
			updateDisplayField();
		} else if (editObject instanceof Parameterizable) {
			// show CustomizerPanel

			custom = new CustomizerPanel(owner, (Parameterizable)editObject);
			custom.setVisible(true);
			if (custom.validParameters()) {

				updateDisplayField();
			}
		} else {
			updateDisplayField();
		}

	}

	public void setClassType(Class type) {
		this.classType = type;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	private void createChooseButton() {

		chooseButton = new JButton("Choose");
		chooseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				maybeShowPopUp(createPopUpMenu());

				// show JTree according to classType
			}

		});
		add(chooseButton);
	}

	private String[] getPropertyFileInfo() {

		String className = classType.getName();

		// check if we got a property file
		if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName
					.getOrCreatePropertyName(classType));
		}
		return null;

	}

	private JPopupMenu createPopUpMenu() {

		JPanel treePane = new JPanel();

		root = new DefaultMutableTreeNode(PREFIX);
		tree = new JTree(createTree());

		// add SelectionListener
		tree.addTreeSelectionListener(new TreeSelectionListener() {

			public void valueChanged(TreeSelectionEvent e) {
				// System.out.println("value changed!");
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
						.getLastSelectedPathComponent();

				if (node.isLeaf()) {

					StringBuffer fullName = new StringBuffer();

					int level = node.getLevel();
					int i = 0;
					for (TreeNode n : node.getPath()) {

						fullName.append(((DefaultMutableTreeNode) n).getUserObject().toString());
						if (i != level) {
							fullName.append(".");
						}
						i++;

					}

					setSelectedClass(fullName.toString());
					menu.setVisible(false);
				}
			}
		});

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treePane.add(tree);
		treePane.setBackground(tree.getBackground());

		JScrollPane scroller = new JScrollPane(treePane);

		scroller.setPreferredSize(new Dimension(200, 150));

		menu = new JPopupMenu();
		menu.add(scroller);

		return menu;
	}

	private DefaultTreeModel createTree() {

		for (String obj : getPropertyFileInfo()) {

			// check if class is from a dbs-package
			if (obj.startsWith(PREFIX)) {

				// System.out.println("o:" + obj);

				int index = PREFIX.length() + 1;
				// System.out.println("index: " + index);

				String[] parts = obj.substring(index).split("\\.");

				// for (String p : parts) {
				// System.out.println("\t" + p);
				// }
				// analyse object
				DefaultMutableTreeNode parent = null;
				int parentIndex = -1;
				for (int i = 0; i < parts.length - 1; i++) {

					// System.out.println("checking "+parts[i]);
					DefaultMutableTreeNode node = checkRootPath(parts[i]);
					if (node != null) {
						parent = node;
						// don't forget to count the root!
						parentIndex = i + 1;

					} else {
						// System.out.println(parts[i] + " not found!");
					}

				}

				if (parentIndex == -1) {
					// System.out.println("parentIndex set to 0");
					parentIndex = 0;
					parent = root;
				}
				// else{
				// System.out.println(parent.getUserObject().toString());
				// System.out.println("parent index: "+parentIndex);
				// }

				DefaultMutableTreeNode child;
				while (parentIndex < parts.length - 1) {

					// System.out.println("add node "+parts[parentIndex]);
					child = new DefaultMutableTreeNode(parts[parentIndex]);
					parent.add(child);
					parent = child;

					parentIndex++;
				}
				parent.add(new DefaultMutableTreeNode(parts[parts.length - 1]));
			}
		}

		return new DefaultTreeModel(root);
	}

	private DefaultMutableTreeNode checkRootPath(String userObject) {

		for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements();) {
			DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
			// System.out.println("root child: "+current.toString());
			if (current.getUserObject().toString().equals(userObject)) {

				return current;
			}

		}
		return null;
	}

	private void maybeShowPopUp(JPopupMenu menu) {

		menu.show(chooseButton, chooseButton.getLocation().x, chooseButton.getLocation().y);
	}

	private void setSelectedClass(String className) {

		// class is already set
		// if (editObject != null &&
		// editObject.getClass().getName().equals(className)) {
		// return;
		// }

		try {
			// System.out.println("set edit object!");
			setEditObject(Class.forName(className).newInstance());

		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getDisplayField() {

		displayField = new JTextPane() {

			public Dimension getPreferredScrollableViewportSize() {
				Dimension dim = getPreferredSize();
				dim.width = 500;

				return dim;
			}

			public void setSize(Dimension d) {

				if (d.width < getParent().getSize().width) {
					d.width = getParent().getSize().width;

				}

				super.setSize(d);
			}

			public boolean getScrollableTracksViewportWidth() {

				return false;
			}
		};

		displayField.setEditable(false);
		// set the right color
		displayField.setBackground(Color.white);

		pane = new JScrollPane();

		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		pane.setViewportView(displayField);

		displayField.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {

				if (custom != null) {
					custom.setVisible(true);
				}
			}
		});

		scrollPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.VERTICAL;
		scrollPanel.add(pane, gbc);
		Dimension dim = scrollPanel.getPreferredSize();
		dim.height = 50;
		scrollPanel.setPreferredSize(dim);

		add(scrollPanel);
	}

	private void updateDisplayField() {

		// remove old text
		displayField.setText(null);

		String name = editObject.getClass().getName();
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex != -1) {
			name = name.substring(dotIndex + 1);
		}

		StyledDocument doc = displayField.getStyledDocument();

		try {

			SimpleAttributeSet set = new SimpleAttributeSet();

			StyleConstants.setBold(set, true);

			doc.insertString(doc.getLength(), name, set);

			set = new SimpleAttributeSet();

			doc.insertString(doc.getLength(), getObjectParameters(), set);

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scrollPanel.revalidate();

	}

	public Object getValue() {

		return editObject;
	}

	private boolean hasDefaultParameters() {

		return false;
	}

	private String getObjectParameters() {
		StringBuffer buffer = new StringBuffer();
		for (Option opt : ((Parameterizable) editObject).getPossibleOptions()) {
			buffer.append(" -");
			buffer.append(opt.getName());
			buffer.append(" ");
			buffer.append(opt.getValue());
		}

		return buffer.toString();
	}

}
