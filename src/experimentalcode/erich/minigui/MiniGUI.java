package experimentalcode.erich.minigui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.MessageFormatter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

public class MiniGUI extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  public static final int BIT_INCOMPLETE = 0;

  public static final int BIT_INVALID = 1;

  public static final int BIT_SYNTAX_ERROR = 2;

  public static final int BIT_NO_NAME_BUT_VALUE = 3;

  public static final int BIT_OPTIONAL = 4;

  public static final int BIT_DEFAULT_VALUE = 5;

  static final String[] columns = { "Parameter", "Value" };

  private static final String NEWLINE = System.getProperty("line.separator");

  protected Logging logger = Logging.getLogger(MiniGUI.class);

  protected JTextArea outputArea;

  protected ArrayList<Triple<Option<?>, String, BitSet>> parameters;

  protected JTable parameterTable;

  public MiniGUI() {
    super();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    parameters = new ArrayList<Triple<Option<?>, String, BitSet>>();

    parameterTable = new JTable(new ParametersModel(parameters));
    parameterTable.setPreferredScrollableViewportSize(new Dimension(600, 200));
    parameterTable.setFillsViewportHeight(true);
    parameterTable.setDefaultRenderer(String.class, new HighlightingRenderer(parameters));
    parameterTable.setDefaultEditor(String.class, new SelfadjustingRenderer(parameters, new JComboBox()));

    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(parameterTable);

    // Add the scroll pane to this panel.
    add(scrollPane);

    // Button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

    // button to evaluate settings
    JButton setButton = new JButton("Test Settings");
    setButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        runSetParameters(false);
      }
    });
    buttonPanel.add(setButton);

    // button to evaluate settings
    JButton helpButton = new JButton("Request usage help");
    helpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        runSetParameters(true);
      }
    });
    buttonPanel.add(helpButton);

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
    outputArea = new JTextArea();

    // Create the scroll pane and add the table to it.
    JScrollPane outputPane = new JScrollPane(outputArea);
    outputPane.setPreferredSize(new Dimension(600, 200));

    // Add the output pane to the bottom
    add(outputPane);

    // reconfigure logging
    LogHandler.setReceiver(this);
    LoggingConfiguration.reconfigureLogging(MiniGUI.class.getPackage().getName(), "logging-minigui.properties");

    // refresh Parameters
    ArrayList<String> ps = new ArrayList<String>();
    ps.add("-algorithm XXX");
    doSetParameters(false, ps);
  }

  protected void runSetParameters(boolean help) {
    ArrayList<String> params = serializeParameters();
    if(!help) {
      outputArea.setText("Testing parameters: " + FormatUtil.format(params, " ") + NEWLINE);
    }
    else {
      outputArea.setText("");
    }
    List<Pair<Parameterizable, Option<?>>> options = doSetParameters(help, params);

    if(help) {
      StringBuffer buf = new StringBuffer();
      buf.append("Parameters:").append(NEWLINE);
      OptionUtil.formatForConsole(buf, 100, "   ", options);

      // TODO: global parameter constraints

      outputArea.append(buf.toString());
    }
  }

  private List<Pair<Parameterizable, Option<?>>> doSetParameters(boolean help, ArrayList<String> params) {
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      task.setParameters(params);
      if(!help) {
        outputArea.append("Test ok." + NEWLINE);
      }
    }
    catch(ParameterException e) {
      outputArea.append("Parameter Error: " + e.getMessage() + NEWLINE);
    }
    catch(Exception e) {
      logger.exception(e);
    }

    // Collect options
    ArrayList<Pair<Parameterizable, Option<?>>> options = task.collectOptions();

    // update table:
    parameterTable.setEnabled(false);
    parameters.clear();
    for(Pair<Parameterizable, Option<?>> p : options) {
      Option<?> option = p.getSecond();
      String value = option.getGivenValue();
      if(value == null) {
        if(option instanceof Flag) {
          value = Flag.NOT_SET;
        }
        else {
          value = "";
        }
      }
      BitSet bits = new BitSet();
      if(option instanceof Parameter<?, ?>) {
        Parameter<?, ?> par = (Parameter<?, ?>) option;
        if (par.isOptional()) {
          bits.set(BIT_OPTIONAL);
        }
        if (par.hasDefaultValue() && par.tookDefaultValue()) {
          bits.set(BIT_DEFAULT_VALUE);
        }
      }
      else if(option instanceof Flag) {
        bits.set(BIT_OPTIONAL);
      } else {
        logger.warning("Option is neither Parameter nor Flag!");
      }
      if(value == "") {
        if (!bits.get(BIT_DEFAULT_VALUE) && !bits.get(BIT_OPTIONAL)) {
          bits.set(BIT_INCOMPLETE);
        }
      }
      if(value != "") {
        try {
          if(!option.isValid(value)) {
            bits.set(BIT_INVALID);
          }
        }
        catch(ParameterException e) {
          bits.set(BIT_INVALID);
        }
      }
      Triple<Option<?>, String, BitSet> t = new Triple<Option<?>, String, BitSet>(option, value, bits);
      parameters.add(t);
    }
    parameterTable.revalidate();
    parameterTable.setEnabled(true);
    return options;
  }

  private ArrayList<String> serializeParameters() {
    parameterTable.setEnabled(false);
    ArrayList<String> p = new ArrayList<String>(2 * parameters.size());
    for(Triple<Option<?>, String, BitSet> t : parameters) {
      if(t.getFirst() != null) {
        if(t.getFirst() instanceof Parameter<?, ?> && t.getSecond() != null && t.getSecond().length() > 0) {
          p.add("-" + t.getFirst().getOptionID().getName());
          p.add(t.getSecond());
        }
        else if(t.getFirst() instanceof Flag) {
          if(t.getSecond() == Flag.SET) {
            p.add("-" + t.getFirst().getOptionID().getName());
          }
        }
      }
    }
    parameterTable.setEnabled(true);
    return p;
  }

  protected void runTask() {
    ArrayList<String> params = serializeParameters();
    outputArea.setText("Running: " + FormatUtil.format(params, " ") + NEWLINE);
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      task.setParameters(params);
      task.run();
    }
    catch(ParameterException e) {
      outputArea.setText(e.getMessage());
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

  class ParametersModel extends AbstractTableModel {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<Triple<Option<?>, String, BitSet>> parameters;

    public ParametersModel(ArrayList<Triple<Option<?>, String, BitSet>> parameters) {
      super();
      this.parameters = parameters;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public int getRowCount() {
      return parameters.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if(rowIndex < parameters.size()) {
        Triple<Option<?>, String, BitSet> p = parameters.get(rowIndex);
        if(columnIndex == 0) {
          String ret = p.getFirst().getOptionID().getName();
          if(ret == null) {
            ret = "";
          }
          return ret;
        }
        else if(columnIndex == 1) {
          String ret = p.getSecond();
          if(ret == null) {
            ret = "";
          }
          return ret;
        }
        return "";
      }
      else {
        return "";
      }
    }

    @Override
    public String getColumnName(int column) {
      return columns[column];
    }

    @Override
    public Class<?> getColumnClass(@SuppressWarnings("unused") int columnIndex) {
      return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return (columnIndex == 1) || (rowIndex > parameters.size());
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if(value instanceof String) {
        String s = (String) value;
        Triple<Option<?>, String, BitSet> p;
        if(rowIndex < parameters.size()) {
          p = parameters.get(rowIndex);
        }
        else {
          BitSet flags = new BitSet();
          // flags.set(BIT_INCOMPLETE);
          p = new Triple<Option<?>, String, BitSet>(null, "", flags);
          parameters.add(p);
        }
        BitSet flags = p.getThird();
        if(columnIndex == 0) {
          p.setFirst(null); // TODO: allow editing!
        }
        else if(columnIndex == 1) {
          p.setSecond(s);

          if(p.getFirst() instanceof Flag) {
            if((!Flag.SET.equals(s)) && (!Flag.NOT_SET.equals(s))) {
              flags.set(BIT_SYNTAX_ERROR);
            }
            else {
              flags.clear(BIT_SYNTAX_ERROR);
            }
          }
        }
        // set flag when we have a key but no value
        // flags.set(BIT_NO_NAME_BUT_VALUE, (p.getFirst().length() == 0 &&
        // p.getSecond().length() > 0));
        // no data at all?
        // if(p.getFirst() == null || p.getFirst() == "") {
        // if(p.getSecond() == null || p.getSecond() == "") {
        // flags.clear();
        // }
        // }
        p.setThird(flags);
      }
      else {
        logger.warning("Edited value is not a String!");
      }
    }
  }

  protected static class HighlightingRenderer extends DefaultTableCellRenderer {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<Triple<Option<?>, String, BitSet>> parameters;

    private static final Color COLOR_INCOMPLETE = new Color(0xAFAFFF);

    private static final Color COLOR_SYNTAX_ERROR = new Color(0xFFAFAF);

    private static final Color COLOR_OPTIONAL = new Color(0xAFFFAF);

    private static final Color COLOR_DEFAULT_VALUE = new Color(0xBFBFBF);

    public HighlightingRenderer(ArrayList<Triple<Option<?>, String, BitSet>> parameters) {
      super();
      this.parameters = parameters;
    }

    @Override
    public void setValue(Object value) {
      if(value instanceof String) {
        setText((value == null) ? "" : (String) value);
      }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if(row < parameters.size()) {
        Triple<Option<?>, String, BitSet> p = parameters.get(row);
        if((p.getThird().get(BIT_INVALID))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((p.getThird().get(BIT_SYNTAX_ERROR))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((p.getThird().get(BIT_NO_NAME_BUT_VALUE))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((p.getThird().get(BIT_INCOMPLETE))) {
          c.setBackground(COLOR_INCOMPLETE);
        }
        else if((p.getThird().get(BIT_OPTIONAL))) {
          c.setBackground(COLOR_OPTIONAL);
        }
        else if((p.getThird().get(BIT_DEFAULT_VALUE))) {
          c.setBackground(COLOR_DEFAULT_VALUE);
        }
        else {
          c.setBackground(null);
        }
      }
      return c;
    }
  }

  protected static class SelfadjustingRenderer extends DefaultCellEditor {
    /**
     * Serial Version
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<Triple<Option<?>, String, BitSet>> parameters;
    
    private final JComboBox comboBox;

    public SelfadjustingRenderer(ArrayList<Triple<Option<?>, String, BitSet>> parameters, JComboBox comboBox) {
      super(comboBox);
      this.comboBox = comboBox;
      this.parameters = parameters;
    }

    /* (non-Javadoc)
     * @see javax.swing.DefaultCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
      comboBox.removeAllItems();
      if(row < parameters.size()) {
        Triple<Option<?>, String, BitSet> p = parameters.get(row);
        if (p.getFirst() instanceof ClassParameter<?>) {
          ClassParameter<?> cp = (ClassParameter<?>) p.getFirst();
          String prefix = cp.getRestrictionClass().getPackage().getName() + ".";
          for (Class<?> impl : cp.getKnownImplementations()) {
            String name = impl.getName();
            if(name.startsWith(prefix)) {
              comboBox.addItem(name.substring(prefix.length()));
            }
            else {
              comboBox.addItem(name);
            }
          }
        } else if (p.getFirst() instanceof Flag) {
          comboBox.addItem(Flag.SET);
          comboBox.addItem(Flag.NOT_SET);
        } else if (p.getFirst() instanceof Parameter<?,?>) {
          // no completion.
        }
      }
      return c;
    }
  }

  public static class LogHandler extends Handler {
    private static MiniGUI logReceiver = null;

    /**
     * Formatter for regular messages (informational records)
     */
    private Formatter msgformat = new MessageFormatter();

    /**
     * Formatter for debugging messages
     */
    private Formatter debugformat = new SimpleFormatter();

    /**
     * Formatter for error messages
     */
    private Formatter errformat = new SimpleFormatter();

    @Override
    public void close() throws SecurityException {
      // Do nothing
    }

    @Override
    public void flush() {
      // Do nothing
    }

    @Override
    public void publish(LogRecord record) {
      // choose an appropriate formatter
      final Formatter fmt;
      // always format progress messages using the progress formatter.
      if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
        // format errors using the error formatter
        fmt = errformat;
      }
      else if(record.getLevel().intValue() <= Level.FINE.intValue()) {
        // format debug statements using the debug formatter.
        fmt = debugformat;
      }
      else {
        // default to the message formatter.
        fmt = msgformat;
      }
      // format
      final String m;
      try {
        m = fmt.format(record);
      }
      catch(Exception ex) {
        reportError(null, ex, ErrorManager.FORMAT_FAILURE);
        return;
      }
      if(logReceiver != null) {
        logReceiver.publish(m, record.getLevel());
      }
      else {
        // fall back to standard error.
        System.err.println(m);
      }
    }

    protected static void setReceiver(MiniGUI receiver) {
      logReceiver = receiver;
    }
  }

  /**
   * @param record log message.
   * @param level unused for now
   */
  protected void publish(String record, Level level) {
    outputArea.append(record);
  }
}
