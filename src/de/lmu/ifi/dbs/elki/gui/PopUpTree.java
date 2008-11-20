package de.lmu.ifi.dbs.elki.gui;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

// todo steffi comment all
public class PopUpTree extends JPopupMenu {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String[] treeData;

    protected JTree tree;

    private String title;

    public static final String PREFIX = "de.lmu.ifi.dbs.elki";

    private DefaultMutableTreeNode root;

    private String selectedClass;

    private Vector<PopUpTreeListener> treeListener;

    private String preSelectedLeaf;

    public PopUpTree(String[] data, String preSelectedLeaf) {

        treeData = data;

        this.preSelectedLeaf = preSelectedLeaf;

        root = new DefaultMutableTreeNode(title);

        treeListener = new Vector<PopUpTreeListener>();

        add(getTreePane());

        pack();

        setVisible(false);
    }

    @SuppressWarnings("unchecked")
    private Component getTreePane() {

        JPanel treePane = new JPanel();

        root = new DefaultMutableTreeNode(PREFIX);
        tree = new JTree(createTree());

        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {

                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForRow(selRow);

                    if (selPath == null) {
                        return;
                    }

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

                    // do only respond to mouse event if a leaf node was
                    // selected
                    if (node.isLeaf()) {

                        setVisible(false);
                        setSelectedClass(((LeafObject) node.getUserObject()).nodePath());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {

                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForRow(selRow);

                    if (selPath == null) {
                        return;
                    }

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

                    // do only respond to mouse event if a leaf node was
                    // selected
                    if (node.isLeaf()) {

                        setVisible(false);
                        setSelectedClass(((LeafObject) node.getUserObject()).nodePath());
                    }
                }
            }

        });

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // tree expand mode

        int row = -1;
        for (Enumeration<DefaultMutableTreeNode> e = root.breadthFirstEnumeration(); e.hasMoreElements();) {

            DefaultMutableTreeNode current = e.nextElement();


            if (current.isLeaf()) {

                TreeNode[] pathNodes = current.getPath();
                TreePath path = new TreePath(pathNodes);

                row = tree.getRowForPath(path);

                tree.makeVisible(path);

                if (((LeafObject) current.getUserObject()).nodePath().equals(preSelectedLeaf)) {
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                }
            }
        }


        tree.setRootVisible(false);

        treePane.add(tree);
        treePane.setBackground(tree.getBackground());

        JScrollPane scroller = new JScrollPane(treePane);

        scroller.setPreferredSize(new Dimension(430, 280));

        tree.scrollRowToVisible(row);
        return scroller;
    }

    /**
     * Create the tree's data model
     *
     * @return the tree's data model
     */
    private DefaultTreeModel createTree() {

        // instantiate root
        root = new DefaultMutableTreeNode("root");

        TreeMap<String, Vector<String>> classes = getClassTreeMaps(treeData);

        for (Map.Entry<String, Vector<String>> k : classes.entrySet()) {

            String path = k.getKey();

            Pattern splitPattern = Pattern.compile("[.]");

            String[] parts = splitPattern.split(path);

            // (possibly) build up path
            DefaultMutableTreeNode parent = root;
            int index = 0;

            for (int i = 0; i < parts.length; i++) {

                DefaultMutableTreeNode node = checkRootPath(parts[i]);

                if (node != null) {
                    parent = node;
                    index = i;
                }
                else {

                    for (int j = index; j < parts.length; j++) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(parts[j]);
                        parent.add(child);
                        parent = child;
                    }
                    break;
                }
            }


            for (String className : k.getValue()) {

                parent.add(new DefaultMutableTreeNode(new LeafObject(path, className)));
            }
        }

        return new DefaultTreeModel(root);

    }

    private DefaultMutableTreeNode checkRootPath(String nodeObject) {


        if (root.isLeaf()) {
            return null;
        }

        for (Enumeration<?> e = root.breadthFirstEnumeration(); e.hasMoreElements();) {
            DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();

            if (current.getUserObject().toString().equals(nodeObject)) {

                return current;
            }
        }
        return null;
    }

    public void addTreeSelectionListener(TreeSelectionListener l) {

        tree.addTreeSelectionListener(l);
    }

    public String getFullClassName(DefaultMutableTreeNode node) {

        if (node.isLeaf()) {
            return ((LeafObject) node.getUserObject()).nodePath();
        }
        StringBuilder buffer = new StringBuilder();
        int counter = 1;
        for (TreeNode n : node.getPath()) {
            if (((DefaultMutableTreeNode) n).isLeaf()) {
                continue;
            }
            buffer.append(n.toString());
            if (counter != node.getPath().length) {
                buffer.append(".");
            }
            counter++;
        }
        return buffer.toString();
    }

    protected void setSelectedClass(String newClass) {
        this.selectedClass = newClass;

        for (PopUpTreeListener l : treeListener) {
            l.selectedClassChanged(selectedClass);
        }
    }

    public void addPopUpTreeListener(PopUpTreeListener l) {
        treeListener.add(l);
    }


    private TreeMap<String, Vector<String>> getClassTreeMaps(String[] treeData) {

        TreeMap<String, Vector<String>> classes = new TreeMap<String, Vector<String>>();

        for (String obj : treeData) {

            int index = obj.lastIndexOf(".");

            String className = obj.substring(index + 1);
            String path = obj.substring(0, index + 1);

            boolean found = false;
            for (Map.Entry<String, Vector<String>> k : classes.entrySet()) {
                // got key already
                if (k.getKey().equals(path)) {
                    k.getValue().add(className);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Vector<String> names = new Vector<String>();
                names.add(className);
                classes.put(path, names);
            }
        }
        return classes;
    }

    // own user object class

    private class LeafObject {
        // node label
        private String nodeLabel;

        // complete path
        private String path;

        public LeafObject(String classPath, String nodeLabel) {
            this.path = classPath.concat(nodeLabel);
            this.nodeLabel = nodeLabel;
        }

        public String nodePath() {
            return path;
        }

        @Override
        public String toString() {
            return nodeLabel;
        }
    }
}
