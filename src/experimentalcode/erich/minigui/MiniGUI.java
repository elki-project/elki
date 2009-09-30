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
import javax.swing.JTable;
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

    // Setup parameter storage and table model
    this.parameters = new DynamicParameters();
    ParametersModel parameterModel = new ParametersModel(parameters);
    parameterModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(@SuppressWarnings("unused") TableModelEvent e) {
        logger.debug("Change event.");
        runSetParameters();
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
}