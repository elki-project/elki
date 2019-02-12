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
package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.lmu.ifi.dbs.elki.gui.configurator.ConfiguratorPanel;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;

/**
 * Abstract panel, showing particular options.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - Status
 */
public abstract class ParameterTabPanel extends JPanel implements ChangeListener {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Status code enumeration
   * 
   * @author Erich Schubert
   */
  public enum Status {
    /** Status to signal the step has been configured properly. */
    STATUS_UNCONFIGURED {
      @Override
      public String toString() {
        return "unconfigured";
      }
    },

    /** Status to signal the step has been configured properly. */
    STATUS_CONFIGURED {
      @Override
      public String toString() {
        return "configured";
      }
    },

    /** Status to signal the step is ready to run */
    STATUS_READY {
      @Override
      public String toString() {
        return "ready to run";
      }
    },

    /** Status to signal the step has been run completely. */
    STATUS_COMPLETE {
      @Override
      public String toString() {
        return "complete";
      }
    },

    /** Status to signal the step has failed somehow */
    STATUS_FAILED {
      @Override
      public String toString() {
        return "failed";
      }
    }
  }

  /**
   * ELKI logger for the GUI
   */
  private static final Logging LOG = Logging.getLogger(ParameterTabPanel.class);

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
      runButton.addActionListener((e) -> new Thread(this::execute).start());
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
      parameterTable = new ConfiguratorPanel();
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
      for(TrackedParameter pair : track.getAllParameters()) {
        parameterTable.addParameter(pair.getOwner(), pair.getParameter(), track);
      }
      // parameters.updateFromTrackParameters(track);

      parameterTable.revalidate();
      parameterTable.setEnabled(true);
    }

    // Update status and notify observers
    updateStatus();
    firePanelUpdated();
  }

  /**
   * Collect parameters
   * 
   * @param params Parameter list to add to
   */
  public void appendParameters(ListParameterization params) {
    parameterTable.appendParameters(params);
  }

  /**
   * Report errors in a single error log record.
   * 
   * @param config Parameterization
   */
  protected void reportErrors(Parameterization config) {
    StringBuilder buf = new StringBuilder();
    for(ParameterException e : config.getErrors()) {
      if(e instanceof UnspecifiedParameterException) {
        continue;
      }
      buf.append(e.getMessage()).append(FormatUtil.NEWLINE);
    }
    if(buf.length() > 0) {
      LOG.warning("Configuration errors:" + FormatUtil.NEWLINE + FormatUtil.NEWLINE + buf.toString());
    }
    // config.clearErrors();
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
      if(config.hasUnusedParameters()) {
        // List<Pair<OptionID, Object>> remainingParameters =
        // config.getRemainingParameters();
        LOG.warning("Unused parameters: " + "FIXME");
      }
      if(config.getErrors().size() > 0) {
        reportErrors(config);
      }
      else {
        executeStep();
      }
    }
    catch(Exception e) {
      LOG.exception(e);
    }
    updateStatus();
    firePanelUpdated();
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
  protected abstract Status getStatus();

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
    Status status = getStatus();
    return Status.STATUS_READY.equals(status) || Status.STATUS_COMPLETE.equals(status);
  }

  /**
   * Test if this tab is complete
   * 
   * @return completeness status
   */
  public boolean isComplete() {
    return Status.STATUS_COMPLETE.equals(getStatus());
  }

  /**
   * Invoked to update the UI when the status could have changed.
   */
  protected void updateStatus() {
    statusText.setText(getStatus().toString());
    runButton.setEnabled(canRun());
  }

  protected void firePanelUpdated() {
    for(ParameterTabPanel p : listenerList.getListeners(ParameterTabPanel.class)) {
      p.panelUpdated(this);
    }
  }

  public void addPanelListener(ParameterTabPanel o) {
    listenerList.add(ParameterTabPanel.class, o);
  }

  public void removePanelListener(ParameterTabPanel o) {
    listenerList.remove(ParameterTabPanel.class, o);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() == this.parameterTable) {
      // logger.warning("stateChanged!");
      updateParameterTable();
    }
  }

  /**
   * Called when an observed panel changes.
   * 
   * @param o Observed panel
   */
  void panelUpdated(ParameterTabPanel o) {
    // Do nothing by default.
  };
}
