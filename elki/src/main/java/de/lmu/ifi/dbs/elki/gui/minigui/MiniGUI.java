package de.lmu.ifi.dbs.elki.gui.minigui;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.awt.Dimension;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.gui.GUIUtil;
import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.gui.util.LogPanel;
import de.lmu.ifi.dbs.elki.gui.util.ParameterTable;
import de.lmu.ifi.dbs.elki.gui.util.ParametersModel;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.CLISmartHandler;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.workflow.LoggingStep;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Minimal GUI built around a table-based parameter editor.
 *
 * @author Erich Schubert
 *
 * @apiviz.composedOf SettingsComboboxModel
 * @apiviz.composedOf LoggingStep
 * @apiviz.owns ParameterTable
 * @apiviz.owns DynamicParameters
 */
@Alias({ "mini", "minigui" })
public class MiniGUI extends AbstractApplication {
  /**
   * Filename for saved settings.
   */
  public static final String SAVED_SETTINGS_FILENAME = "MiniGUI-saved-settings.txt";

  /**
   * Newline used in output.
   */
  public static final String NEWLINE = System.getProperty("line.separator");

  /**
   * ELKI logger for the GUI.
   */
  private static final Logging LOG = Logging.getLogger(MiniGUI.class);

  /**
   * Quit action, for mnemonics.
   */
  protected static final String ACTION_QUIT = "quit";

  /**
   * The frame
   */
  JFrame frame;

  /**
   * The main panel.
   */
  JPanel panel;

  /**
   * Logging output area.
   */
  protected LogPanel outputArea;

  /**
   * The parameter table.
   */
  protected ParameterTable parameterTable;

  /**
   * Parameter storage.
   */
  protected DynamicParameters parameters;

  /**
   * Settings storage.
   */
  protected SavedSettingsFile store = new SavedSettingsFile(SAVED_SETTINGS_FILENAME);

  /**
   * Combo box for saved settings.
   */
  protected JComboBox<String> savedCombo;

  /**
   * Model to link the combobox with.
   */
  protected SettingsComboboxModel savedSettingsModel;

  /**
   * The "run" button.
   */
  protected JButton runButton;

  /**
   * Application to configure / run.
   */
  private Class<? extends AbstractApplication> maincls = KDDCLIApplication.class;

  /**
   * Constructor.
   */
  public MiniGUI() {
    super();
    // Create and set up the window.
    frame = new JFrame("ELKI MiniGUI");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    try {
      frame.setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
    }
    catch(Exception e) {
      // Ignore - icon not found is not fatal.
    }

    panel = new JPanel();
    panel.setOpaque(true); // content panes must be opaque
    panel.setLayout(new GridBagLayout());

    {
      // Button panel
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

      // Combo box for saved settings
      savedSettingsModel = new SettingsComboboxModel(store);
      savedCombo = new JComboBox<>(savedSettingsModel);
      savedCombo.setEditable(true);
      savedCombo.setSelectedItem("[Saved Settings]");
      buttonPanel.add(savedCombo);

      // button to load settings
      JButton loadButton = new JButton("Load");
      loadButton.setMnemonic(KeyEvent.VK_L);
      loadButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String key = savedSettingsModel.getSelectedItem();
          ArrayList<String> settings = store.get(key);
          if(settings != null) {
            outputArea.clear();
            outputArea.publish("Parameters: " + FormatUtil.format(settings, " ") + NEWLINE, Level.INFO);
            doSetParameters(settings);
          }
        }
      });
      buttonPanel.add(loadButton);
      // button to save settings
      JButton saveButton = new JButton("Save");
      saveButton.setMnemonic(KeyEvent.VK_S);
      saveButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String key = savedSettingsModel.getSelectedItem();
          // Stop editing the table.
          parameterTable.editCellAt(-1, -1);
          store.put(key, parameters.serializeParameters());
          try {
            store.save();
          }
          catch(IOException e1) {
            LOG.exception(e1);
          }
          savedSettingsModel.update();
        }
      });
      buttonPanel.add(saveButton);
      // button to remove saved settings
      JButton removeButton = new JButton("Remove");
      removeButton.setMnemonic(KeyEvent.VK_E);
      removeButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String key = savedSettingsModel.getSelectedItem();
          store.remove(key);
          try {
            store.save();
          }
          catch(IOException e1) {
            LOG.exception(e1);
          }
          savedCombo.setSelectedItem("[Saved Settings]");
          savedSettingsModel.update();
        }
      });
      buttonPanel.add(removeButton);

      // button to launch the task
      runButton = new JButton("Run Task");
      runButton.setMnemonic(KeyEvent.VK_R);
      runButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          startTask();
        }
      });
      buttonPanel.add(runButton);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.weightx = 1.0;
      constraints.weighty = 0.01;
      panel.add(buttonPanel, constraints);
    }

    {
      // Setup parameter storage and table model
      this.parameters = new DynamicParameters();
      ParametersModel parameterModel = new ParametersModel(parameters);
      parameterModel.addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
          // logger.debug("Change event.");
          updateParameterTable();
        }
      });

      // Create parameter table
      parameterTable = new ParameterTable(frame, parameterModel, parameters);
      // Create the scroll pane and add the table to it.
      JScrollPane scrollPane = new JScrollPane(parameterTable);

      // Add the scroll pane to this panel.
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 1;
      panel.add(scrollPane, constraints);
    }

    {
      // setup text output area
      outputArea = new LogPanel();

      // Create the scroll pane and add the table to it.
      JScrollPane outputPane = new JScrollPane(outputArea);
      outputPane.setPreferredSize(new Dimension(800, 400));

      // Add the output pane to the bottom
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridx = 0;
      constraints.gridy = 2;
      constraints.weightx = 1;
      constraints.weighty = 1;
      panel.add(outputPane, constraints);
    }

    // load saved settings (we wanted to have the logger first!)
    try {
      store.load();
      savedSettingsModel.update();
    }
    catch(FileNotFoundException e) {
      // Ignore - probably didn't save any settings yet.
    }
    catch(IOException e) {
      LOG.exception(e);
    }

    {
      KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK);
      panel.getInputMap().put(key, ACTION_QUIT);
    }
    {
      KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK);
      panel.getInputMap().put(key, ACTION_QUIT);
    }
    panel.getActionMap().put(ACTION_QUIT, new AbstractAction() {
      /**
       * Serial version
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        frame.dispose();
      }
    });

    // Finalize the frame.
    frame.setContentPane(panel);
    frame.pack();
  }

  /**
   * Serialize the parameter table and run setParameters().
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
   */
  protected void doSetParameters(List<String> params) {
    SerializedParameterization config = new SerializedParameterization(params);
    TrackParameters track = new TrackParameters(config);
    track.tryInstantiate(LoggingStep.class);
    track.tryInstantiate(maincls);
    config.logUnusedParameters();
    // config.logAndClearReportedErrors();
    final boolean hasErrors = (config.getErrors().size() > 0);
    if(hasErrors && params.size() > 0) {
      reportErrors(config);
    }
    runButton.setEnabled(!hasErrors);

    List<String> remainingParameters = config.getRemainingParameters();

    // update table:
    parameterTable.removeEditor();
    parameterTable.setEnabled(false);
    parameters.updateFromTrackParameters(track);
    // Add remaining parameters
    if(remainingParameters != null && !remainingParameters.isEmpty()) {
      DynamicParameters.RemainingOptions remo = new DynamicParameters.RemainingOptions();
      try {
        remo.setValue(FormatUtil.format(remainingParameters, " "));
      }
      catch(ParameterException e) {
        LOG.exception(e);
      }
      int bits = DynamicParameters.BIT_INVALID | DynamicParameters.BIT_SYNTAX_ERROR;
      parameters.addParameter(remo, remo.getValue(), bits, 0);
    }

    config.clearErrors();
    parameterTable.revalidate();
    parameterTable.setEnabled(true);
  }

  /**
   * Auto-load the last task from the history file.
   */
  protected void loadLatest() {
    int size = store.size();
    if(size > 0) {
      final Pair<String, ArrayList<String>> pair = store.getElementAt(size - 1);
      savedSettingsModel.setSelectedItem(pair.first);
      doSetParameters(pair.second);
    }
  }

  /**
   * Do a full run of the KDDTask with the specified parameters.
   */
  protected void startTask() {
    parameterTable.editCellAt(-1, -1);
    parameterTable.setEnabled(false);
    final ArrayList<String> params = parameters.serializeParameters();
    parameterTable.setEnabled(true);

    runButton.setEnabled(false);

    outputArea.clear();
    outputArea.publish("Running: " + FormatUtil.format(params, " ") + NEWLINE, Level.INFO);

    SwingWorker<Void, Void> r = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        SerializedParameterization config = new SerializedParameterization(params);
        config.tryInstantiate(LoggingStep.class);
        AbstractApplication task = config.tryInstantiate(maincls);
        try {
          config.logUnusedParameters();
          if(config.getErrors().size() == 0) {
            task.run();
          }
          else {
            reportErrors(config);
          }
          LOG.debug("Task completed successfully.");
        }
        catch(Throwable e) {
          LOG.exception("Task failed", e);
        }
        return null;
      }

      @Override
      protected void done() {
        super.done();
        runButton.setEnabled(true);
      }
    };
    r.execute();
  }

  /**
   * Report errors in a single error log record.
   *
   * @param config Parameterization
   */
  protected void reportErrors(SerializedParameterization config) {
    StringBuilder buf = new StringBuilder();
    buf.append("Task is not completely configured:" + NEWLINE + NEWLINE);
    for(ParameterException e : config.getErrors()) {
      if(e instanceof UnspecifiedParameterException) {
        buf.append("The parameter ");
        buf.append(((UnspecifiedParameterException) e).getParameterName());
        buf.append(" is required.").append(NEWLINE);
      }
      else {
        buf.append(e.getMessage() + NEWLINE);
      }
    }
    LOG.warning(buf.toString());
    config.clearErrors();
  }

  @Override
  public void run() throws UnableToComplyException {
    frame.setVisible(true);
    outputArea.becomeDefaultLogger();
  }

  /**
   * Main method that just spawns the UI.
   *
   * @param args command line parameters
   */
  public static void main(final String[] args) {
    GUIUtil.logUncaughtExceptions(LOG);
    GUIUtil.setLookAndFeel();
    OutputStep.setDefaultHandlerVisualizer();

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          final MiniGUI gui = new MiniGUI();
          gui.run();
          List<String> params = Collections.emptyList();
          if(args != null && args.length > 0) {
            params = new ArrayList<>(Arrays.asList(args));
            // TODO: it would be nicer to use the Parameterization API for this!
            if(params.size() > 0) {
              Class<? extends AbstractApplication> c = ELKIServiceRegistry.findImplementation(AbstractApplication.class, params.get(0));
              if(c != null) {
                gui.maincls = c;
                params.remove(0); // on success
              }
            }
            if(params.remove("-minigui.last")) {
              javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  gui.loadLatest();
                }
              });
            }
            if(params.remove("-minigui.autorun")) {
              javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  gui.startTask();
                }
              });
            }
          }
          gui.doSetParameters(params);
        }
        catch(Exception | Error e) {
          // Restore error handler, as the GUI is likely broken.
          LoggingConfiguration.replaceDefaultHandler(new CLISmartHandler());
          LOG.exception(e);
        }
      }
    });
  }

  /**
   * Class to interface between the saved settings list and a JComboBox.
   *
   * @author Erich Schubert
   *
   * @apiviz.composedOf de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile
   */
  class SettingsComboboxModel extends AbstractListModel<String>implements ComboBoxModel<String> {
    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Settings storage.
     */
    protected SavedSettingsFile store;

    /**
     * Selected entry.
     */
    protected String selected = null;

    /**
     * Constructor.
     *
     * @param store Store to access
     */
    public SettingsComboboxModel(SavedSettingsFile store) {
      super();
      this.store = store;
    }

    @Override
    public String getSelectedItem() {
      return selected;
    }

    @Override
    public void setSelectedItem(Object anItem) {
      if(anItem instanceof String) {
        selected = (String) anItem;
      }
    }

    @Override
    public String getElementAt(int index) {
      return store.getElementAt(store.size() - 1 - index).first;
    }

    @Override
    public int getSize() {
      return store.size();
    }

    /**
     * Force an update.
     */
    public void update() {
      fireContentsChanged(this, 0, getSize() + 1);
    }
  }
}
