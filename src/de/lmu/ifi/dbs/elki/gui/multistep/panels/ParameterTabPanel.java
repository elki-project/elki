package de.lmu.ifi.dbs.elki.gui.multistep.panels;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.lmu.ifi.dbs.elki.gui.configurator.ConfiguratorPanel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observable;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observers;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Abstract panel, showing particular options.
 * 
 * @author Erich Schubert
 */
public abstract class ParameterTabPanel extends JPanel implements Observable<ParameterTabPanel>, ChangeListener {
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
   * Status to signal the step is ready to run
   */
  public static final String STATUS_READY = "ready to run";

  /**
   * Status to signal the step has been run completely.
   */
  public static final String STATUS_COMPLETE = "complete";

  /**
   * Status to signal the step has failed somehow
   */
  public static final String STATUS_FAILED = "failed";

  /**
   * ELKI logger for the GUI
   */
  protected static final Logging logger = Logging.getLogger(ParameterTabPanel.class);

  /**
   * The parameter table
   */
  private final ConfiguratorPanel parameterTable;

  /**
   * The "run" button.
   */
  private final JButton runButton;

  /**
   * The status text field
   */
  private final JLabel statusText;

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
      statusText = new JLabel();
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
      // Create parameter table
      parameterTable = new ConfiguratorPanel(); // new ParameterTable(parameterModel, parameters);
      parameterTable.addChangeListener(this);
      // Create the scroll pane and add the table to it.
      JScrollPane scrollPane = new JScrollPane(parameterTable);

      // Add the scroll pane to this panel.
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 1;
      constraints.anchor = GridBagConstraints.NORTH;
      add(scrollPane, constraints);
    }
  }

  /**
   * Serialize the parameter table and run setParameters()
   */
  protected void updateParameterTable() {
    parameterTable.setEnabled(false);
    ListParameterization config = new ListParameterization();
    parameterTable.appendParameters(config);
    setParameters(config);
    if(config.getErrors().size() > 0) {
      reportErrors(config);
    }
    config.clearErrors();
    parameterTable.setEnabled(true);
  }

  /**
   * Do the actual setParameters invocation.
   * 
   * @param config Parameterization
   */
  public void setParameters(Parameterization config) {
    TrackParameters track = new TrackParameters(config);
    configureStep(track);

    // update parameter table
    {
      parameterTable.setEnabled(false);
      
      parameterTable.clear();
      for (Pair<Object, Parameter<?,?>> pair : track.getAllParameters()) {
        parameterTable.addParameter(pair.first, pair.getSecond(), track);
      }
      //parameters.updateFromTrackParameters(track);

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
  protected void reportErrors(Parameterization config) {
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
    //config.clearErrors();
  }

  /**
   * Execute the task.
   */
  protected synchronized void execute() {
    ListParameterization config = new ListParameterization();
    parameterTable.appendParameters(config);
    runButton.setEnabled(false);
    try {
      configureStep(config);
      if (config.hasUnusedParameters()) {
        //List<Pair<OptionID, Object>> remainingParameters = config.getRemainingParameters();
        logger.warning("Unused parameters: "+"FIXME");
      }
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
    if(status == STATUS_READY || status == STATUS_COMPLETE) {
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

  @Override
  public void addObserver(Observer<? super ParameterTabPanel> o) {
    observers.add(o);
  }

  @Override
  public void removeObserver(Observer<? super ParameterTabPanel> o) {
    observers.remove(o);
  }
  
  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() == this.parameterTable) {
      //logger.warning("stateChanged!");
      updateParameterTable();
    }
  }
}