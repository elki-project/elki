package experimentalcode.erich.minigui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class MiniGUI extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  private static final String NEWLINE = System.getProperty("line.separator");

  protected Logging logger = Logging.getLogger(MiniGUI.class);

  protected LogPane outputArea;

  protected JTable parameterTable;

  protected DynamicParameters parameters;

  public MiniGUI() {
    super();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    this.parameters = new DynamicParameters();

    ParametersModel parameterModel = new ParametersModel(parameters);
    parameterModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(@SuppressWarnings("unused") TableModelEvent e) {
        logger.debug("Change event.");
        runSetParameters();
      }
    });
    parameterTable = new JTable(parameterModel);
    parameterTable.setPreferredScrollableViewportSize(new Dimension(800, 400));
    parameterTable.setFillsViewportHeight(true);
    parameterTable.setDefaultRenderer(Option.class, new HighlightingRenderer(parameters));
    parameterTable.setDefaultRenderer(String.class, new HighlightingRenderer(parameters));
    final AdjustingEditor editor = new AdjustingEditor(parameters);
    parameterTable.setDefaultEditor(String.class, editor);

    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(parameterTable);

    // Add the scroll pane to this panel.
    add(scrollPane);

    // Button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

    // button to launch the task
    JButton runButton = new JButton("Run Task");
    runButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        runTask();
      }
    });
    buttonPanel.add(runButton);

    add(buttonPanel);

    // setup text output area
    outputArea = new LogPane();

    // Create the scroll pane and add the table to it.
    JScrollPane outputPane = new JScrollPane(outputArea);
    outputPane.setPreferredSize(new Dimension(800, 400));

    // Add the output pane to the bottom
    add(outputPane);

    // reconfigure logging
    outputArea.becomeDefaultLogger();

    // refresh Parameters
    ArrayList<String> ps = new ArrayList<String>();
    ps.add("-algorithm XXX");
    doSetParameters(ps);
  }

  protected void runSetParameters() {
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    parameterTable.setEnabled(true);
    outputArea.clear();
    outputArea.publish("Parameters: " + FormatUtil.format(params, " ") + NEWLINE, Level.INFO);
    doSetParameters(params);
  }

  private List<Pair<Parameterizable, Option<?>>> doSetParameters(ArrayList<String> params) {
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      if(params.size() > 0) {
        task.setParameters(params);
      }
    }
    catch(ParameterException e) {
      logger.error("Parameter Error: " + e.getMessage());
    }
    catch(Exception e) {
      logger.exception(e);
    }

    // Collect options
    ArrayList<Pair<Parameterizable, Option<?>>> options = task.collectOptions();

    // update table:
    parameterTable.setEnabled(false);
    parameters.updateFromOptions(options);
    parameterTable.revalidate();
    parameterTable.setEnabled(true);
    return options;
  }

  protected void runTask() {
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    parameterTable.setEnabled(true);
    outputArea.clear();
    outputArea.publish("Running: " + FormatUtil.format(params, " ") + NEWLINE, Level.INFO);
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      task.setParameters(params);
      task.run();
    }
    catch(ParameterException e) {
      outputArea.publish(e.getMessage(), Level.WARNING);
    }
    catch(Exception e) {
      logger.exception(e);
    }
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be
   * invoked from the event-dispatching thread.
   */
  protected static void createAndShowGUI() {
    // Create and set up the window.
    JFrame frame = new JFrame("ELKI MiniGUI");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // Create and set up the content pane.
    MiniGUI newContentPane = new MiniGUI();
    newContentPane.setOpaque(true); // content panes must be opaque
    frame.setContentPane(newContentPane);

    // Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
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
