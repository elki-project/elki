package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.gui.util.ParameterTable;
import de.lmu.ifi.dbs.elki.gui.util.ParametersModel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observable;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observers;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;

/**
 * Abstract panel, showing particular options.
 * 
 * @author Erich Schubert
 */
public abstract class ParameterTabPanel extends JPanel implements Observable<ParameterTabPanel> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Status to signal the step has been configured properly.
   */
  public static final String STATUS_UNCONFIGURED = "unconfigured";

  /**
   * Status to signal the step has been configured properly.
   */
  public static final String STATUS_CONFIGURED = "configured";

  /**
   * Status to signal the step has been run completely.
   */
  public static final String STATUS_COMPLETE = "complete";

  /**
   * ELKI logger for the GUI
   */
  protected Logging logger = Logging.getLogger(ParameterTabPanel.class);

  /**
   * The parameter table
   */
  private final ParameterTable parameterTable;

  /**
   * Parameter storage
   */
  private final DynamicParameters parameters;

  /**
   * The "run" button.
   */
  private final JButton runButton;

  /**
   * The status text field
   */
  private final JTextPane statusText;

  /**
   * Observers of this panel
   */
  protected final Observers<ParameterTabPanel> observers = new Observers<ParameterTabPanel>();

  /**
   * Input pane
   */
  public ParameterTabPanel() {
    super();
    this.setLayout(new GridBagLayout());
    {
      // Button panel
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

      // button to launch the task
      runButton = new JButton("Run Task");
      runButton.setMnemonic(KeyEvent.VK_R);
      runButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          Thread r = new Thread() {
            @Override
            public void run() {
              execute();
            }
          };
          r.start();
        }
      });
      buttonPanel.add(runButton);

      // Status text field
      statusText = new JTextPane();
      buttonPanel.add(statusText);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.weightx = 1.0;
      constraints.weighty = 0.01;
      add(buttonPanel, constraints);
    }

    {
      // Setup parameter storage and table model
      this.parameters = new DynamicParameters();
      ParametersModel parameterModel = new ParametersModel(parameters);
      parameterModel.addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(@SuppressWarnings("unused") TableModelEvent e) {
          // logger.debug("Change event.");
          updateParameterTable();
        }
      });

      // Create parameter table
      parameterTable = new ParameterTable(parameterModel, parameters);
      // Create the scroll pane and add the table to it.
      JScrollPane scrollPane = new JScrollPane(parameterTable);

      // Add the scroll pane to this panel.
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 1;
      add(scrollPane, constraints);
    }
  }

  /**
   * Serialize the parameter table and run setParameters()
   */
  protected void updateParameterTable() {
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    SerializedParameterization config = new SerializedParameterization(params);
    setParameters(config);
    parameterTable.setEnabled(true);
  }

  /**
   * Do the actual setParameters invocation.
   * 
   * @param config Parameterization
   */
  public void setParameters(SerializedParameterization config) {
    TrackParameters track = new TrackParameters(config);
    configureStep(track);
    config.logUnusedParameters();
    if(config.getErrors().size() > 0) {
      reportErrors(config);
    }
    List<String> remainingParameters = config.getRemainingParameters();

    // update parameter table
    {
      parameterTable.setEnabled(false);
      parameters.updateFromTrackParameters(track);
      // Add remaining parameters
      if(remainingParameters != null && !remainingParameters.isEmpty()) {
        DynamicParameters.RemainingOptions remo = new DynamicParameters.RemainingOptions();
        try {
          remo.setValue(FormatUtil.format(remainingParameters, " "));
        }
        catch(ParameterException e) {
          logger.exception(e);
        }
        BitSet bits = new BitSet();
        bits.set(DynamicParameters.BIT_INVALID);
        bits.set(DynamicParameters.BIT_SYNTAX_ERROR);
        parameters.addParameter(remo, remo.getValue(), bits, 0);
      }

      parameterTable.revalidate();
      parameterTable.setEnabled(true);
    }

    // Update status and notify observers
    updateStatus();
    observers.notifyObservers(this);
  }

  /**
   * Report errors in a single error log record.
   * 
   * @param config Parameterization
   */
  protected void reportErrors(SerializedParameterization config) {
    StringBuffer buf = new StringBuffer();
    for(ParameterException e : config.getErrors()) {
      if(e instanceof UnspecifiedParameterException) {
        continue;
      }
      buf.append(e.getMessage() + FormatUtil.NEWLINE);
    }
    if(buf.length() > 0) {
      logger.warning("Configuration errors:" + FormatUtil.NEWLINE + FormatUtil.NEWLINE + buf.toString());
    }
    config.clearErrors();
  }

  /**
   * Execute the task.
   */
  protected synchronized void execute() {
    SerializedParameterization config = serializeParameters();
    runButton.setEnabled(false);
    try {
      configureStep(config);
      config.logUnusedParameters();
      if(config.getErrors().size() > 0) {
        reportErrors(config);
      }
      else {
        executeStep();
      }
    }
    catch(Exception e) {
      logger.exception(e);
    }
    updateStatus();
    observers.notifyObservers(this);
  }

  /**
   * Configure this step with the given parameters.
   * 
   * @param config Configuration to use.
   */
  protected abstract void configureStep(Parameterization config);

  /**
   * Get the current status of this step.
   * 
   * @return current status
   */
  protected abstract String getStatus();

  /**
   * Execute the configured step.
   */
  protected abstract void executeStep();

  /**
   * Test if this tab is ready-to-run
   * 
   * @return can-run status
   */
  public boolean canRun() {
    String status = getStatus();
    if(status == STATUS_CONFIGURED || status == STATUS_COMPLETE) {
      return true;
    }
    return false;
  }

  /**
   * Test if this tab is complete
   * 
   * @return completeness status
   */
  public boolean isComplete() {
    String status = getStatus();
    if(status == STATUS_COMPLETE) {
      return true;
    }
    return false;
  }

  /**
   * Invoked to update the UI when the status could have changed.
   */
  protected void updateStatus() {
    statusText.setText(getStatus());
    runButton.setEnabled(canRun());
  }

  /**
   * Serialize the parameters to use in parameterization.
   * 
   * @return Parameters
   */
  private SerializedParameterization serializeParameters() {
    parameterTable.editCellAt(-1, -1);
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    parameterTable.setEnabled(true);
    return new SerializedParameterization(params);
  }

  @Override
  public void addObserver(Observer<? super ParameterTabPanel> o) {
    observers.add(o);
  }

  @Override
  public void removeObserver(Observer<? super ParameterTabPanel> o) {
    observers.remove(o);
  }
}