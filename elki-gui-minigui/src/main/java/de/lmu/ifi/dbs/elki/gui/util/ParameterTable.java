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
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.lmu.ifi.dbs.elki.gui.icons.StockIcon;
import de.lmu.ifi.dbs.elki.gui.util.ClassTree.ClassNode;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Class showing a table of ELKI parameters.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @composed - - - ParametersModel
 * @composed - - - ColorfulRenderer
 * @composed - - - DropdownEditor
 * @composed - - - FileNameEditor
 * @composed - - - ClassListEditor
 * @composed - - - AdjustingEditor
 * @assoc - - - Handler
 */
public class ParameterTable extends JTable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Color for parameters that are not optional and not yet specified.
   */
  static final Color COLOR_INCOMPLETE = new Color(0xFFCF9F);

  /**
   * Color for parameters with an invalid value.
   */
  static final Color COLOR_SYNTAX_ERROR = new Color(0xFFAFAF);

  /**
   * Color for optional parameters (with no default value)
   */
  static final Color COLOR_OPTIONAL = new Color(0xEFFFEF);

  /**
   * Color for parameters having a default value.
   */
  static final Color COLOR_DEFAULT_VALUE = new Color(0xEFEFEF);

  /**
   * Containing frame.
   */
  protected Frame frame;

  /**
   * The parameters we edit.
   */
  protected DynamicParameters parameters;

  /**
   * Constructor
   *
   * @param frame Containing frame
   * @param pm Parameter Model
   * @param parameters Parameter storage
   */
  public ParameterTable(Frame frame, ParametersModel pm, DynamicParameters parameters) {
    super(pm);
    this.frame = frame;
    this.parameters = parameters;
    this.setFillsViewportHeight(true);
    final ColorfulRenderer colorfulRenderer = new ColorfulRenderer();
    this.setDefaultRenderer(Parameter.class, colorfulRenderer);
    this.setDefaultRenderer(String.class, colorfulRenderer);
    final AdjustingEditor editor = new AdjustingEditor();
    this.setDefaultEditor(String.class, editor);
    this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    TableColumn col1 = this.getColumnModel().getColumn(0);
    final int ppi = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    col1.setPreferredWidth(2 * ppi);
    TableColumn col2 = this.getColumnModel().getColumn(1);
    col2.setPreferredWidth(8 * ppi);
    this.addKeyListener(new Handler());

    // Increase row height, to make editors usable.
    // FIXME: Heuristic hack. Any way to make this more reasonable?
    setRowHeight(getRowHeight() + (int) (ppi * 0.05));
  }

  /**
   * Internal key listener.
   *
   * @author Erich Schubert
   */
  protected class Handler implements KeyListener {
    @Override
    public void keyTyped(KeyEvent e) {
      // ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE //
            || e.getKeyCode() == KeyEvent.VK_ENTER //
            || e.getKeyCode() == KeyEvent.VK_DOWN //
            || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
          final ParameterTable parent = ParameterTable.this;
          if(!parent.isEditing()) {
            int leadRow = parent.getSelectionModel().getLeadSelectionIndex();
            int leadColumn = parent.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            parent.editCellAt(leadRow, leadColumn);
            Component editorComponent = getEditorComponent();
            // This is a hack, to make the content assist open immediately.
            if(editorComponent instanceof DispatchingPanel) {
              KeyListener[] l = ((DispatchingPanel) editorComponent).component.getKeyListeners();
              for(KeyListener li : l) {
                li.keyPressed(e);
              }
            }
          }
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      // ignore
    }
  }

  /**
   * Renderer for the table that colors the entries according to their bitmask.
   *
   * @author Erich Schubert
   */
  private class ColorfulRenderer extends DefaultTableCellRenderer {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public ColorfulRenderer() {
      super();
    }

    @Override
    public void setValue(Object value) {
      if(value instanceof String) {
        setText((String) value);
        setToolTipText(null);
        return;
      }
      if(value instanceof DynamicParameters.Node) {
        Parameter<?> o = ((DynamicParameters.Node) value).param;
        // Simulate a tree using indentation - there is no JTreeTable AFAICT
        StringBuilder buf = new StringBuilder();
        for(int i = 1; i < ((DynamicParameters.Node) value).depth; i++) {
          buf.append(' ');
        }
        buf.append(o.getOptionID().getName());
        setText(buf.toString());
        setToolTipText(o.getOptionID().getDescription());
        return;
      }
      setText("");
      setToolTipText(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if(!hasFocus) {
        if(row < parameters.size()) {
          int flags = parameters.getNode(row).flags;
          // TODO: don't hardcode black - maybe mix the other colors, too?
          c.setForeground(Color.BLACK);
          if((flags & DynamicParameters.BIT_INVALID) != 0) {
            c.setBackground(COLOR_SYNTAX_ERROR);
          }
          else if((flags & DynamicParameters.BIT_SYNTAX_ERROR) != 0) {
            c.setBackground(COLOR_SYNTAX_ERROR);
          }
          else if((flags & DynamicParameters.BIT_INCOMPLETE) != 0) {
            c.setBackground(COLOR_INCOMPLETE);
          }
          else if((flags & DynamicParameters.BIT_DEFAULT_VALUE) != 0) {
            c.setBackground(COLOR_DEFAULT_VALUE);
          }
          else if((flags & DynamicParameters.BIT_OPTIONAL) != 0) {
            c.setBackground(COLOR_OPTIONAL);
          }
          else {
            c.setBackground(null);
          }
        }
      }
      return c;
    }
  }

  /**
   * Editor using a Dropdown box to offer known values to choose from.
   *
   * @author Erich Schubert
   */
  private class DropdownEditor extends DefaultCellEditor implements KeyListener {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;

    /**
     * We need a panel to ensure focusing.
     */
    final JPanel panel;

    /**
     * Combo box to use
     */
    private final JComboBox<String> comboBox;

    /**
     * Constructor.
     *
     * @param comboBox Combo box we're going to use
     */
    public DropdownEditor(JComboBox<String> comboBox) {
      super(comboBox);
      this.comboBox = comboBox;
      panel = new DispatchingPanel((JComponent) comboBox.getEditor().getEditorComponent());
      panel.setLayout(new BorderLayout());
      panel.add(comboBox, BorderLayout.CENTER);
      comboBox.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    }

    @Override
    public void keyTyped(KeyEvent e) {
      // Ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE //
            || e.getKeyCode() == KeyEvent.VK_ENTER //
            || e.getKeyCode() == KeyEvent.VK_DOWN //
            || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
          if(!comboBox.isPopupVisible()) {
            comboBox.showPopup();
            e.consume();
          }
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      // Ignore
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      // remove old contents
      comboBox.removeAllItems();
      // Put the current value in first.
      Object val = table.getValueAt(row, column);
      if(val != null && val instanceof String) {
        String sval = (String) val;
        if(sval.equals(DynamicParameters.STRING_OPTIONAL)) {
          sval = "";
        }
        if(sval.startsWith(DynamicParameters.STRING_USE_DEFAULT)) {
          sval = "";
        }
        if(sval != "") {
          comboBox.addItem(sval);
          comboBox.setSelectedIndex(0);
        }
      }
      if(row < parameters.size()) {
        Parameter<?> option = parameters.getNode(row).param;
        // for Flag parameters.
        if(option instanceof Flag) {
          if(!Flag.SET.equals(val)) {
            comboBox.addItem(Flag.SET);
          }
          if(!Flag.NOT_SET.equals(val)) {
            comboBox.addItem(Flag.NOT_SET);
          }
        }
        // and for Enum parameters.
        else if(option instanceof EnumParameter<?>) {
          EnumParameter<?> ep = (EnumParameter<?>) option;
          for(String s : ep.getPossibleValues()) {
            if(ep.hasDefaultValue() && ep.getDefaultValueAsString().equals(s)) {
              comboBox.addItem(DynamicParameters.STRING_USE_DEFAULT + s);
            }
            else if(!s.equals(val)) {
              comboBox.addItem(s);
            }
          }
        }
        // No completion for others
      }
      return panel;
    }
  }

  /**
   * Editor for selecting input and output file and folders names
   *
   * @author Erich Schubert
   */
  private class FileNameEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, KeyListener {
    /**
     * Serial version number
     */
    private static final long serialVersionUID = 1L;

    /**
     * We need a panel to put our components on.
     */
    final JPanel panel;

    /**
     * Text field to store the name
     */
    final JTextField textfield = new JTextField();

    /**
     * The button to open the file selector
     */
    final JButton button = new JButton("...");

    /**
     * File selector mode.
     */
    int mode = FileDialog.LOAD;

    /**
     * Default path.
     */
    String defaultpath = (new File(".")).getAbsolutePath();

    /**
     * Constructor.
     */
    public FileNameEditor() {
      button.addActionListener(this);
      panel = new DispatchingPanel(textfield);
      panel.setLayout(new BorderLayout());
      panel.add(textfield, BorderLayout.CENTER);
      panel.add(button, BorderLayout.EAST);
      textfield.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      textfield.addKeyListener(this);
    }

    /**
     * Button callback to show the file selector
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      FileDialog fc = new FileDialog(frame);
      fc.setDirectory(defaultpath);
      fc.setMode(mode);
      final String curr = textfield.getText();
      if(curr != null && curr.length() > 0) {
        fc.setFile(curr);
      }
      fc.setVisible(true);
      String filename = fc.getFile();
      if(filename != null) {
        textfield.setText(new File(fc.getDirectory(), filename).getPath());
      }
      fc.setVisible(false);
      fc.dispose();
      textfield.requestFocus();
      fireEditingStopped();
    }

    @Override
    public void keyTyped(KeyEvent e) {
      // Ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE //
            || e.getKeyCode() == KeyEvent.VK_ENTER //
            || e.getKeyCode() == KeyEvent.VK_DOWN //
            || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
          e.consume();
          actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "assist"));
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      // Ignore
    }

    /**
     * Delegate getCellEditorValue to the text field.
     */
    @Override
    public Object getCellEditorValue() {
      return textfield.getText();
    }

    /**
     * Apply the Editor for a selected option.
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if(row < parameters.size()) {
        Parameter<?> option = parameters.getNode(row).param;
        if(option instanceof FileParameter) {
          FileParameter fp = (FileParameter) option;
          mode = FileParameter.FileType.INPUT_FILE.equals(fp.getFileType()) ? FileDialog.LOAD : FileDialog.SAVE;
          textfield.setText(fp.isDefined() ? fp.getValue().getPath() : "");
        }
      }
      textfield.requestFocus();
      return panel;
    }
  }

  /**
   * Editor for choosing classes.
   *
   * @author Erich Schubert
   * 
   * @composed - - - ClassTree
   */
  private class ClassListEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, KeyListener {
    /**
     * Serial version number
     */
    private static final long serialVersionUID = 1L;

    /**
     * We need a panel to put our components on.
     */
    final JPanel panel;

    /**
     * Text field to store the name
     */
    final JTextField textfield = new JTextField();

    /**
     * The button to open the file selector
     */
    final JButton button = new JButton("+");

    /**
     * The popup menu.
     */
    final TreePopup popup;

    /**
     * Tree model
     */
    private TreeModel model;

    /**
     * Parameter we are currently editing.
     */
    private Parameter<?> option;

    /**
     * Constructor.
     */
    public ClassListEditor() {
      textfield.addKeyListener(this);
      button.addActionListener(this);
      model = new DefaultTreeModel(new DefaultMutableTreeNode());
      popup = new TreePopup(model);
      popup.getTree().setRootVisible(false);
      popup.addActionListener(this);

      Icon classIcon = StockIcon.getStockIcon(StockIcon.GO_NEXT);
      Icon packageIcon = StockIcon.getStockIcon(StockIcon.PACKAGE);
      TreePopup.Renderer renderer = (TreePopup.Renderer) popup.getTree().getCellRenderer();
      renderer.setLeafIcon(classIcon);
      renderer.setFolderIcon(packageIcon);

      panel = new DispatchingPanel(textfield);

      panel.setLayout(new BorderLayout());
      panel.add(textfield, BorderLayout.CENTER);
      panel.add(button, BorderLayout.EAST);
      textfield.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    }

    /**
     * Callback to show the popup menu
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if(e.getSource() == button) {
        popup.show(panel);
        return;
      }
      if(e.getSource() == popup) {
        if(e.getActionCommand() == TreePopup.ACTION_CANCELED) {
          popup.setVisible(false);
          textfield.requestFocus();
          return;
        }
        TreePath path = popup.getTree().getSelectionPath();
        final Object comp = (path != null) ? path.getLastPathComponent() : null;
        if(comp instanceof ClassNode) {
          String newClass = ((ClassNode) comp).getClassName();
          if(newClass != null && !newClass.isEmpty()) {
            if(option instanceof ClassListParameter) {
              String val = textfield.getText();
              if(val.equals(DynamicParameters.STRING_OPTIONAL) //
                  || val.startsWith(DynamicParameters.STRING_USE_DEFAULT)) {
                val = "";
              }
              val = val.isEmpty() ? newClass : val + ClassListParameter.LIST_SEP + newClass;
              textfield.setText(val);
            }
            else {
              textfield.setText(newClass);
            }
            popup.setVisible(false);
            fireEditingStopped();
            textfield.requestFocus();
          }
        }
        return;
      }
      LoggingUtil.warning("Unrecognized action event in ClassListEditor: " + e);
    }

    /**
     * Delegate getCellEditorValue to the text field.
     */
    @Override
    public Object getCellEditorValue() {
      return textfield.getText();
    }

    @Override
    public void keyTyped(KeyEvent e) {
      // Ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
        if(e.getKeyCode() == KeyEvent.VK_SPACE //
            || e.getKeyCode() == KeyEvent.VK_ENTER //
            || e.getKeyCode() == KeyEvent.VK_DOWN //
            || e.getKeyCode() == KeyEvent.VK_KP_DOWN) {
          if(!popup.isVisible()) {
            popup.show(ClassListEditor.this.panel);
            e.consume();
          }
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      // Ignore
    }

    /**
     * Apply the Editor for a selected option.
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if(row < parameters.size()) {
        this.option = parameters.getNode(row).param;
        TreeNode root;
        // We can do dropdown choices for class parameters
        if(option instanceof ClassListParameter<?>) {
          ClassListParameter<?> cp = (ClassListParameter<?>) option;
          root = ClassTree.build(cp.getKnownImplementations(), cp.getRestrictionClass().getPackage().getName());
          button.setText("+");
        }
        else if(option instanceof ClassParameter<?>) {
          ClassParameter<?> cp = (ClassParameter<?>) option;
          root = ClassTree.build(cp.getKnownImplementations(), cp.getRestrictionClass().getPackage().getName());
          button.setText("v");
        }
        else {
          root = new DefaultMutableTreeNode();
        }
        if(option.isDefined()) {
          if(option.tookDefaultValue()) {
            textfield.setText(DynamicParameters.STRING_USE_DEFAULT + option.getDefaultValueAsString());
          }
          else {
            textfield.setText(option.getValueAsString());
          }
        }
        else {
          textfield.setText("");
        }
        popup.getTree().setModel(new DefaultTreeModel(root));
      }
      return panel;
    }
  }

  /**
   * This Editor will adjust to the type of the Option: Sometimes just a plain
   * text editor, sometimes a ComboBox to offer known choices, and sometime a
   * file selector dialog.
   *
   * TODO: class list parameters etc.
   *
   * @author Erich Schubert
   *
   */
  private class AdjustingEditor extends AbstractCellEditor implements TableCellEditor {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * The dropdown editor
     */
    private final DropdownEditor dropdownEditor;

    /**
     * The plain text cell editor
     */
    private final DefaultCellEditor plaintextEditor;

    /**
     * The class list editor
     */
    private final ClassListEditor classListEditor;

    /**
     * The file selector editor
     */
    private final FileNameEditor fileNameEditor;

    /**
     * We need to remember which editor we delegated to, so we know whom to ask
     * for the value entered.
     */
    private TableCellEditor activeEditor;

    /**
     * Constructor.
     */
    public AdjustingEditor() {
      final JComboBox<String> combobox = new JComboBox<>();
      combobox.setEditable(true);
      this.dropdownEditor = new DropdownEditor(combobox);
      JTextField tf = new JTextField();
      tf.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      this.plaintextEditor = new DefaultCellEditor(tf);
      this.classListEditor = new ClassListEditor();
      this.fileNameEditor = new FileNameEditor();
    }

    @Override
    public Object getCellEditorValue() {
      if(activeEditor == null) {
        return null;
      }
      return activeEditor.getCellEditorValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if(value instanceof String) {
        String s = (String) value;
        if(s.startsWith(DynamicParameters.STRING_USE_DEFAULT)) {
          value = s.substring(DynamicParameters.STRING_USE_DEFAULT.length());
        }
      }
      if(row < parameters.size()) {
        Parameter<?> option = parameters.getNode(row).param;
        if(option instanceof Flag) {
          activeEditor = dropdownEditor;
          return dropdownEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof ClassListParameter<?>) {
          activeEditor = classListEditor;
          return classListEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof ClassParameter<?>) {
          activeEditor = classListEditor;
          return classListEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof FileParameter) {
          activeEditor = fileNameEditor;
          return fileNameEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof EnumParameter<?>) {
          activeEditor = dropdownEditor;
          return dropdownEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
      }
      activeEditor = plaintextEditor;
      return plaintextEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
  }

  /**
   * This is a panel that will dispatch keystrokes to a particular component.
   *
   * This makes the tabular GUI much more user friendly.
   *
   * @author Erich Schubert
   */
  private class DispatchingPanel extends JPanel {
    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Component to dispatch to.
     */
    protected JComponent component;

    /**
     * Constructor.
     *
     * @param component Component to dispatch to.
     */
    public DispatchingPanel(JComponent component) {
      super();
      this.component = component;
      setRequestFocusEnabled(true);
    }

    @Override
    public void addNotify() {
      super.addNotify();
      component.requestFocus();
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
      InputMap map = component.getInputMap(condition);
      ActionMap am = component.getActionMap();

      if(map != null && am != null && isEnabled()) {
        Object binding = map.get(ks);
        Action action = (binding == null) ? null : am.get(binding);
        if(action != null) {
          return SwingUtilities.notifyAction(action, ks, e, component, e.getModifiers());
        }
      }
      return false;
    }
  };
}
