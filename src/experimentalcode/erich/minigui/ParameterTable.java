package experimentalcode.erich.minigui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.BitSet;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;

public class ParameterTable extends JTable {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  public ParameterTable(ParametersModel pm, DynamicParameters parameters) {
    super(pm);
    this.setPreferredScrollableViewportSize(new Dimension(800, 400));
    this.setFillsViewportHeight(true);
    this.setDefaultRenderer(Option.class, new HighlightingRenderer(parameters));
    this.setDefaultRenderer(String.class, new HighlightingRenderer(parameters));
    final AdjustingEditor editor = new AdjustingEditor(parameters);
    this.setDefaultEditor(String.class, editor);
  }
  
  protected static class HighlightingRenderer extends DefaultTableCellRenderer {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;
  
    private DynamicParameters parameters;
  
    private static final Color COLOR_INCOMPLETE = new Color(0xFFFFAF);
  
    private static final Color COLOR_SYNTAX_ERROR = new Color(0xFFAFAF);
  
    private static final Color COLOR_OPTIONAL = new Color(0xAFFFAF);
  
    private static final Color COLOR_DEFAULT_VALUE = new Color(0xBFBFBF);
  
    public HighlightingRenderer(DynamicParameters parameters) {
      super();
      this.parameters = parameters;
    }
  
    @Override
    public void setValue(Object value) {
      if(value instanceof String) {
        setText((String) value);
        setToolTipText("");
        return;
      }
      if(value instanceof Option<?>) {
        Option<?> o = (Option<?>) value;
        setText(o.getOptionID().getName());
        setToolTipText(o.getOptionID().getDescription());
        return;
      }
      setText("");
      setToolTipText("");
    }
  
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if(row < parameters.size()) {
        BitSet flags = parameters.getFlags(row);
        if((flags.get(DynamicParameters.BIT_INVALID))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((flags.get(DynamicParameters.BIT_SYNTAX_ERROR))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((flags.get(DynamicParameters.BIT_NO_NAME_BUT_VALUE))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((flags.get(DynamicParameters.BIT_INCOMPLETE))) {
          c.setBackground(COLOR_INCOMPLETE);
        }
        else if((flags.get(DynamicParameters.BIT_OPTIONAL))) {
          c.setBackground(COLOR_OPTIONAL);
        }
        else if((flags.get(DynamicParameters.BIT_DEFAULT_VALUE))) {
          c.setBackground(COLOR_DEFAULT_VALUE);
        }
        else {
          c.setBackground(null);
        }
      }
      return c;
    }
  }

  protected static class DropdownEditor extends DefaultCellEditor {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;
  
    private DynamicParameters parameters;
  
    private final JComboBox comboBox;
  
    public DropdownEditor(DynamicParameters parameters, JComboBox comboBox) {
      super(comboBox);
      this.comboBox = comboBox;
      this.parameters = parameters;
    }
  
    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.DefaultCellEditor#getTableCellEditorComponent(javax.swing
     * .JTable, java.lang.Object, boolean, int, int)
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
      comboBox.removeAllItems();
      Object val = table.getValueAt(row, column);
      if(val != null) {
        comboBox.addItem(val);
        comboBox.setSelectedIndex(0);
      }
      if(row < parameters.size()) {
        Option<?> option = parameters.getOption(row);
        if(option instanceof ClassParameter<?>) {
          ClassParameter<?> cp = (ClassParameter<?>) option;
          if(cp.hasDefaultValue()) {
            comboBox.addItem(DynamicParameters.STRING_USE_DEFAULT);
          }
          else if(cp.isOptional()) {
            comboBox.addItem(DynamicParameters.STRING_OPTIONAL);
          }
          String prefix = cp.getRestrictionClass().getPackage().getName() + ".";
          for(Class<?> impl : cp.getKnownImplementations()) {
            String name = impl.getName();
            if(name.startsWith(prefix)) {
              comboBox.addItem(name.substring(prefix.length()));
            }
            else {
              comboBox.addItem(name);
            }
          }
        }
        else if(option instanceof Flag) {
          if(!Flag.SET.equals(val)) {
            comboBox.addItem(Flag.SET);
          }
          if(!Flag.NOT_SET.equals(val)) {
            comboBox.addItem(Flag.NOT_SET);
          }
        }
        else if(option instanceof Parameter<?, ?>) {
          // no completion.
        }
      }
      return c;
    }
  }

  protected static class FileNameEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
    /**
     * Serial version number
     */
    private static final long serialVersionUID = 1L;
  
    final JPanel panel = new JPanel();
  
    final JTextField textfield = new JTextField();
  
    final JButton button = new JButton("...");
  
    final JFileChooser fc = new JFileChooser();
  
    private DynamicParameters parameters;
  
    public FileNameEditor(DynamicParameters parameters) {
      this.parameters = parameters;
      button.addActionListener(this);
      panel.setLayout(new BorderLayout());
      panel.add(textfield, BorderLayout.CENTER);
      panel.add(button, BorderLayout.EAST);
    }
  
    public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
      int returnVal = fc.showOpenDialog(button);
  
      if(returnVal == JFileChooser.APPROVE_OPTION) {
        textfield.setText(fc.getSelectedFile().getPath());
      }
      else {
        // Do nothing on cancel.
      }
      fireEditingStopped();
    }
  
    public Object getCellEditorValue() {
      return textfield.getText();
    }
  
    public Component getTableCellEditorComponent(@SuppressWarnings("unused") JTable table, @SuppressWarnings("unused") Object value, @SuppressWarnings("unused") boolean isSelected, int row, @SuppressWarnings("unused") int column) {
      if(row < parameters.size()) {
        Option<?> option = parameters.getOption(row);
        if(option instanceof FileParameter) {
          FileParameter fp = (FileParameter) option;
          File f;
          try {
            f = fp.getValue();
          }
          catch(UnusedParameterException e) {
            f = null;
          }
          if(f != null) {
            String fn = f.getPath();
            textfield.setText(fn);
            fc.setSelectedFile(f);
          }
          else {
            textfield.setText("");
            fc.setSelectedFile(null);
          }
        }
      }
      return panel;
    }
  }

  protected static class AdjustingEditor extends AbstractCellEditor implements TableCellEditor {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;
  
    private final DropdownEditor dropdownEditor;
  
    private final DefaultCellEditor defaultEditor;
  
    private final FileNameEditor fileNameEditor;
  
    private TableCellEditor activeEditor;
  
    private final DynamicParameters parameters;
  
    public AdjustingEditor(DynamicParameters parameters) {
      final JComboBox combobox = new JComboBox();
      combobox.setEditable(true);
      this.parameters = parameters;
      this.dropdownEditor = new DropdownEditor(parameters, combobox);
      this.defaultEditor = new DefaultCellEditor(new JTextField());
      this.fileNameEditor = new FileNameEditor(parameters);
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
      if(row < parameters.size()) {
        Option<?> option = parameters.getOption(row);
        if(option instanceof Flag) {
          activeEditor = dropdownEditor;
          return dropdownEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof ClassParameter<?>) {
          activeEditor = dropdownEditor;
          return dropdownEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        if(option instanceof FileParameter) {
          activeEditor = fileNameEditor;
          return fileNameEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
      }
      activeEditor = defaultEditor;
      return defaultEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
  }
}
