package experimentalcode.erich.minigui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Minimal GUI built around a table-based parameter editor.
 * 
 * @author Erich Schubert
 */
public class MiniGUI extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Newline used in output.
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * ELKI logger for the GUI
   */
  protected Logging logger = Logging.getLogger(MiniGUI.class);

  /**
   * Logging output area.
   */
  protected LogPane outputArea;

  /**
   * The parameter table
   */
  protected ParameterTable parameterTable;

  /**
   * Parameter storage
   */
  protected DynamicParameters parameters;

  /**
   * Constructor
   */
  public MiniGUI() {
    super();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    // Setup parameter storage and table model
    this.parameters = new DynamicParameters();
    ParametersModel parameterModel = new ParametersModel(parameters);
    parameterModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(@SuppressWarnings("unused") TableModelEvent e) {
        logger.debug("Change event.");
        updateParameterTable();
      }
    });

    // Create parameter table
    parameterTable = new ParameterTable(parameterModel, parameters);
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

  /**
   * Serialize the parameter table and run setParameters()
   */
  protected void updateParameterTable() {
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    outputArea.clear();
    outputArea.publish("Parameters: " + FormatUtil.format(params, " ") + NEWLINE, Level.INFO);
    doSetParameters(params);
    parameterTable.setEnabled(true);
  }

  /**
   * Do the actual setParameters invocation.
   * 
   * @param params Parameters
   * @return Collected options from KDDTask 
   */
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

  /**
   * Do a full run of the KDDTask with the specified parameters.
   */
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

  /**
   * Main method that just spawns the UI.
   * @param args
   */
  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}