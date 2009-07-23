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
import javax.swing.JButton;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

public class MiniGUI extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  public static final int BIT_INCOMPLETE = 0;

  public static final int BIT_NO_DASH = 1;

  public static final int BIT_NO_NAME_BUT_VALUE = 2;

  static final String[] columns = { "Parameter", "Value" };

  private static final String NEWLINE = System.getProperty("line.separator");

  protected Logging logger = Logging.getLogger(MiniGUI.class);

  protected JTextArea outputArea;

  protected ArrayList<Triple<String, String, BitSet>> parameters;

  public MiniGUI() {
    super();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    parameters = new ArrayList<Triple<String, String, BitSet>>();

    parameters.add(new Triple<String, String, BitSet>("-algorithm", "clustering.DBSCAN", new BitSet()));
    parameters.add(new Triple<String, String, BitSet>("-dbscan.minpts", "10", new BitSet()));
    parameters.add(new Triple<String, String, BitSet>("-dbscan.epsilon", "0.3", new BitSet()));
    parameters.add(new Triple<String, String, BitSet>("-dbc.in", "data/testdata/unittests/hierarchical-3d2d1d.csv", new BitSet()));
    parameters.add(new Triple<String, String, BitSet>("-resulthandler", "experimentalcode.erich.ResultVisualizeScatterplot", new BitSet()));

    final JTable table = new JTable(new ParametersModel(parameters));
    table.setPreferredScrollableViewportSize(new Dimension(600, 200));
    table.setFillsViewportHeight(true);
    table.setDefaultRenderer(String.class, new HighlightingRenderer(parameters));

    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(table);

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
    JButton helpButton = new JButton("Help");
    helpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        runSetParameters(true);
      }
    });
    buttonPanel.add(helpButton);

    // button to launch the task
    JButton runButton = new JButton("Run");
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
  }

  protected void runSetParameters(boolean help) {
    ArrayList<String> params = serializeParameters();
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      task.setParameters(params);
    }
    catch(ParameterException e) {
      outputArea.setText(e.getMessage());
    }

    //LoggingUtil.warning("TEST!");

    if(help) {
      // Collect options
      List<Pair<Parameterizable, Option<?>>> options = new ArrayList<Pair<Parameterizable, Option<?>>>();
      task.collectOptions(options);
      StringBuffer buf = new StringBuffer();
      buf.append(NEWLINE).append("Parameters:").append(NEWLINE);
      OptionUtil.formatForConsole(buf, 100, "   ", options);

      // TODO: global parameter constraints

      outputArea.append(buf.toString());
    }
  }

  private ArrayList<String> serializeParameters() {
    ArrayList<String> p = new ArrayList<String>(2 * parameters.size());
    for(Triple<String, String, BitSet> t : parameters) {
      if(t.getFirst() != null && t.getFirst().length() > 0) {
        p.add(t.getFirst());
      }
      if(t.getSecond() != null && t.getSecond().length() > 0) {
        p.add(t.getSecond());
      }
    }
    return p;
  }

  protected void runTask() {
    ArrayList<String> params = serializeParameters();
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>();
    try {
      task.setParameters(params);
      task.run();
    }
    catch(ParameterException e) {
      outputArea.setText(e.getMessage());
    }
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be
   * invoked from the event-dispatching thread.
   */
  protected static void createAndShowGUI() {
    // Create and set up the window.
    JFrame frame = new JFrame("ELKI MiniGUI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

    private ArrayList<Triple<String, String, BitSet>> parameters;

    public ParametersModel(ArrayList<Triple<String, String, BitSet>> parameters) {
      super();
      this.parameters = parameters;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public int getRowCount() {
      return parameters.size() + 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if(rowIndex < parameters.size()) {
        Triple<String, String, BitSet> p = parameters.get(rowIndex);
        if(columnIndex == 0) {
          return p.getFirst();
        }
        else if(columnIndex == 1) {
          return p.getSecond();
        }
        return null;
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
    public boolean isCellEditable(@SuppressWarnings("unused") int rowIndex, @SuppressWarnings("unused") int columnIndex) {
      return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if(value instanceof String) {
        String s = (String) value;
        Triple<String, String, BitSet> p;
        if(rowIndex < parameters.size()) {
          p = parameters.get(rowIndex);
        }
        else {
          BitSet flags = new BitSet();
          // flags.set(BIT_INCOMPLETE);
          p = new Triple<String, String, BitSet>("", "", flags);
          parameters.add(p);
        }
        BitSet flags = p.getThird();
        if(columnIndex == 0) {
          p.setFirst(s);
          if(s.length() <= 0 || s.charAt(0) != '-') {
            flags.set(BIT_NO_DASH);
          }
          else {
            flags.clear(BIT_NO_DASH);
          }
        }
        else if(columnIndex == 1) {
          p.setSecond(s);
        }
        // set flag when we have a key but no value
        flags.set(BIT_NO_NAME_BUT_VALUE, (p.getFirst().length() == 0 && p.getSecond().length() > 0));
        // no data at all?
        if(p.getFirst() == null || p.getFirst() == "") {
          if(p.getSecond() == null || p.getSecond() == "") {
            flags.clear();
          }
        }
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

    private ArrayList<Triple<String, String, BitSet>> parameters;

    private static final Color COLOR_INCOMPLETE = new Color(0x7F7FFF);

    private static final Color COLOR_SYNTAX_ERROR = new Color(0xFF7F7F);

    public HighlightingRenderer(ArrayList<Triple<String, String, BitSet>> parameters) {
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
        Triple<String, String, BitSet> p = parameters.get(row);
        if((p.getThird().get(BIT_NO_DASH))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((p.getThird().get(BIT_NO_NAME_BUT_VALUE))) {
          c.setBackground(COLOR_SYNTAX_ERROR);
        }
        else if((p.getThird().get(BIT_INCOMPLETE))) {
          c.setBackground(COLOR_INCOMPLETE);
        }
        else {
          c.setBackground(null);
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
      } else {
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
