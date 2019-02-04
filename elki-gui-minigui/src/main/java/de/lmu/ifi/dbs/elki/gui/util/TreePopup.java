/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.gui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;

/**
 * Popup menu that contains a JTree.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - Handler
 * @composed - - - Renderer
 */
public class TreePopup extends JPopupMenu {
  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Action string for confirmed operations (enter or click).
   */
  public static final String ACTION_SELECTED = "selected";

  /**
   * Action string for canceled operations (escape button pressed).
   */
  public static final String ACTION_CANCELED = "canceled";

  /**
   * Tree.
   */
  protected JTree tree;

  /**
   * Scroll pane, containing the tree.
   */
  protected JScrollPane scroller;

  /**
   * Tree model.
   */
  private TreeModel model;

  /**
   * Event handler
   */
  private Handler handler = new Handler();

  /**
   * Border of the popup.
   */
  private static Border TREE_BORDER = new LineBorder(Color.BLACK, 1);

  /**
   * Constructor with an empty tree model.
   * 
   * This needs to also add a root node, and therefore sets
   * {@code getTree().setRootVisible(false)}.
   */
  public TreePopup() {
    this(new DefaultTreeModel(new DefaultMutableTreeNode()));
    tree.setRootVisible(false);
  }

  /**
   * Constructor.
   * 
   * @param model Tree model
   */
  public TreePopup(TreeModel model) {
    super();
    this.setName("TreePopup.popup");
    this.model = model;

    // UI construction of the popup.
    tree = createTree();
    scroller = createScroller();
    configurePopup();
  }

  /**
   * Creates the JList used in the popup to display the items in the combo box
   * model. This method is called when the UI class is created.
   * 
   * @return a <code>JList</code> used to display the combo box items
   */
  protected JTree createTree() {
    JTree tree = new JTree(model);
    tree.setName("TreePopup.tree");
    tree.setFont(getFont());
    tree.setForeground(getForeground());
    tree.setBackground(getBackground());
    tree.setBorder(null);
    tree.setFocusable(true);
    tree.addMouseListener(handler);
    tree.addKeyListener(handler);
    tree.setCellRenderer(new Renderer());
    return tree;
  }

  /**
   * Configure the popup display.
   */
  protected void configurePopup() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorderPainted(true);
    setBorder(TREE_BORDER);
    setOpaque(false);
    add(scroller);
    setDoubleBuffered(true);
    setFocusable(false);
  }

  /**
   * Creates the scroll pane which houses the scrollable tree.
   */
  protected JScrollPane createScroller() {
    JScrollPane sp = new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    sp.setHorizontalScrollBar(null);
    sp.setName("TreePopup.scrollPane");
    sp.setFocusable(false);
    sp.getVerticalScrollBar().setFocusable(false);
    sp.setBorder(null);
    return sp;
  }

  /**
   * Access the tree contained.
   * 
   * @return Tree
   */
  public JTree getTree() {
    return tree;
  }

  /**
   * Display the popup, attached to the given component.
   * 
   * @param parent Parent component
   */
  public void show(Component parent) {
    Dimension parentSize = parent.getSize();
    Insets insets = getInsets();

    // reduce the width of the scrollpane by the insets so that the popup
    // is the same width as the combo box.
    parentSize.setSize(parentSize.width - (insets.right + insets.left), 10 * parentSize.height);
    Dimension scrollSize = computePopupBounds(parent, 0, getBounds().height, parentSize.width, parentSize.height).getSize();

    scroller.setMaximumSize(scrollSize);
    scroller.setPreferredSize(scrollSize);
    scroller.setMinimumSize(scrollSize);

    super.show(parent, 0, parent.getHeight());
    tree.requestFocusInWindow();
  }

  protected Rectangle computePopupBounds(Component parent, int px, int py, int pw, int ph) {
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Rectangle screenBounds;

    // Calculate the desktop dimensions relative to the combo box.
    GraphicsConfiguration gc = parent.getGraphicsConfiguration();
    Point p = new Point();
    SwingUtilities.convertPointFromScreen(p, parent);
    if(gc != null) {
      Insets screenInsets = toolkit.getScreenInsets(gc);
      screenBounds = gc.getBounds();
      screenBounds.width -= (screenInsets.left + screenInsets.right);
      screenBounds.height -= (screenInsets.top + screenInsets.bottom);
      screenBounds.x += (p.x + screenInsets.left);
      screenBounds.y += (p.y + screenInsets.top);
    }
    else {
      screenBounds = new Rectangle(p, toolkit.getScreenSize());
    }

    Rectangle rect = new Rectangle(px, py, pw, ph);
    if(py + ph > screenBounds.y + screenBounds.height && ph < screenBounds.height) {
      rect.y = -rect.height;
    }
    return rect;
  }

  /**
   * Register an action listener.
   * 
   * @param listener Action listener
   */
  public void addActionListener(ActionListener listener) {
    listenerList.add(ActionListener.class, listener);
  }

  /**
   * Unregister an action listener.
   * 
   * @param listener Action listener
   */
  public void removeActionListener(ActionListener listener) {
    listenerList.remove(ActionListener.class, listener);
  }

  /**
   * Notify action listeners.
   * 
   * @param event the <code>ActionEvent</code> object
   */
  protected void fireActionPerformed(ActionEvent event) {
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == ActionListener.class) {
        ((ActionListener) listeners[i + 1]).actionPerformed(event);
      }
    }
  }

  /**
   * Tree cell render.
   * 
   * @author Erich Schubert
   */
  public class Renderer extends JPanel implements TreeCellRenderer {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Label to render
     */
    JLabel label;

    /**
     * Colors
     */
    private Color selbg, defbg, selfg, deffg;

    /**
     * Icons
     */
    private Icon leafIcon, folderIcon;

    /**
     * Constructor.
     */
    protected Renderer() {
      selbg = UIManager.getColor("Tree.selectionBackground");
      defbg = UIManager.getColor("Tree.textBackground");
      selfg = UIManager.getColor("Tree.selectionForeground");
      deffg = UIManager.getColor("Tree.textForeground");

      setLayout(new BorderLayout());
      add(label = new JLabel("This should never be rendered."));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      label.setText((String) ((DefaultMutableTreeNode) value).getUserObject());
      setForeground(selected ? selfg : deffg);
      setBackground(selected ? selbg : defbg);
      label.setIcon(leaf ? leafIcon : folderIcon);
      setPreferredSize(new Dimension(1000, label.getPreferredSize().height));
      return this;
    }

    /**
     * Set the leaf icon
     * 
     * @param leafIcon Leaf icon
     */
    public void setLeafIcon(Icon leafIcon) {
      this.leafIcon = leafIcon;
    }

    /**
     * Set the folder icon.
     * 
     * @param folderIcon Folder icon
     */
    public void setFolderIcon(Icon folderIcon) {
      this.folderIcon = folderIcon;
    }
  }

  /**
   * Event handler class.
   * 
   * @author Erich Schubert
   */
  protected class Handler implements MouseListener, KeyListener, FocusListener {
    @Override
    public void keyTyped(KeyEvent e) {
      if(e.getKeyChar() == '\n') {
        e.consume();
      }
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER) {
        fireActionPerformed(new ActionEvent(TreePopup.this, ActionEvent.ACTION_PERFORMED, ACTION_SELECTED, e.getWhen(), e.getModifiers()));
        e.consume();
        return;
      }
      if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        fireActionPerformed(new ActionEvent(TreePopup.this, ActionEvent.ACTION_PERFORMED, ACTION_CANCELED, e.getWhen(), e.getModifiers()));
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER) {
        e.consume();
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if(e.getButton() == MouseEvent.BUTTON1) {
        fireActionPerformed(new ActionEvent(TreePopup.this, ActionEvent.ACTION_PERFORMED, ACTION_SELECTED, e.getWhen(), e.getModifiers()));
      }
      // ignore
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // ignore
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // ignore
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // ignore
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // ignore
    }

    @Override
    public void focusGained(FocusEvent e) {
      // ignore
    }

    @Override
    public void focusLost(FocusEvent e) {
      fireActionPerformed(new ActionEvent(TreePopup.this, ActionEvent.ACTION_PERFORMED, ACTION_CANCELED));
    }
  }
}
