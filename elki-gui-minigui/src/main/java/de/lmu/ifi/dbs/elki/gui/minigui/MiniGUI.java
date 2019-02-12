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
package de.lmu.ifi.dbs.elki.gui.minigui;

import java.awt.Dimension;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.application.KDDCLIApplication;
import de.lmu.ifi.dbs.elki.gui.GUIUtil;
import de.lmu.ifi.dbs.elki.gui.util.*;
import de.lmu.ifi.dbs.elki.logging.CLISmartHandler;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
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
 * @since 0.3
 *
 * @composed - - - SettingsComboboxModel
 * @composed - - - LoggingStep
 * @composed - - - ParameterTable
 * @composed - - - DynamicParameters
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
   * Combo box for choosing the application to run.
   */
  protected JComboBox<String> appCombo;

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
   * Command line output field.
   */
  private JTextField commandLine;

  /**
   * Prefix for application package.
   */
  private String APP_PREFIX = AbstractApplication.class.getPackage().getName() + ".";

  /**
   * Constructor.
   */
  public MiniGUI() {
    super();
    // Create and set up the window.
    frame = new JFrame("ELKI MiniGUI Command Line Builder");
    Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    int ppi = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
    frame.setPreferredSize(new Dimension(Math.min(10 * ppi, screen.width), Math.min(10 * ppi, screen.height - 32)));
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

    setupAppChooser();
    setupParameterTable();
    setupLoadSaveButtons();
    setupCommandLine();
    setupLoggingArea();

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
   * Setup the application chooser.
   */
  private void setupAppChooser() {
    // Configurator to choose the main application
    appCombo = new JComboBox<>();
    for(Class<?> clz : ELKIServiceRegistry.findAllImplementations(AbstractApplication.class)) {
      String nam = clz.getCanonicalName();
      if(nam == null || clz.getCanonicalName().contains("GUI")) {
        continue;
      }
      if(nam.startsWith(APP_PREFIX)) {
        nam = nam.substring(APP_PREFIX.length());
      }
      appCombo.addItem(nam);
    }
    appCombo.setEditable(true);
    String sel = maincls.getCanonicalName();
    if(sel.startsWith(APP_PREFIX)) {
      sel = sel.substring(APP_PREFIX.length());
    }
    appCombo.setSelectedItem(sel);
    appCombo.addActionListener((e) -> {
      if("comboBoxChanged".equals(e.getActionCommand())) {
        Class<? extends AbstractApplication> clz = ELKIServiceRegistry.findImplementation(AbstractApplication.class, (String) appCombo.getSelectedItem());
        if(clz != null) {
          maincls = clz;
          updateParameterTable();
        }
        else {
          LOG.warning("Main class name not found.");
        }
      }
    });

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 1;
    constraints.weighty = .01;
    panel.add(appCombo, constraints);
  }

  /**
   * Setup the parameter table
   */
  private void setupParameterTable() {
    // Setup parameter storage and table model
    this.parameters = new DynamicParameters();
    ParametersModel parameterModel = new ParametersModel(parameters);
    parameterModel.addTableModelListener((e) -> updateParameterTable());

    // Create parameter table
    parameterTable = new ParameterTable(frame, parameterModel, parameters);
    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(parameterTable);

    // Add the scroll pane to this panel.
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 1;
    constraints.weighty = 1;
    panel.add(scrollPane, constraints);
  }

  /**
   * Create the load and save buttons.
   */
  private void setupLoadSaveButtons() {
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
    loadButton.addActionListener((e) -> {
      ArrayList<String> settings = store.get(savedSettingsModel.getSelectedItem());
      if(settings != null) {
        doSetParameters(settings);
      }
    });
    buttonPanel.add(loadButton);
    // button to save settings
    JButton saveButton = new JButton("Save");
    saveButton.setMnemonic(KeyEvent.VK_S);
    saveButton.addActionListener((e) -> {
      String key = savedSettingsModel.getSelectedItem();
      // Stop editing the table.
      parameterTable.editCellAt(-1, -1);
      ArrayList<String> list = new ArrayList<>(parameters.size() * 2 + 1);
      list.add(maincls.getCanonicalName());
      parameters.serializeParameters(list);
      store.put(key, list);
      try {
        store.save();
      }
      catch(IOException e1) {
        LOG.exception(e1);
      }
      savedSettingsModel.update();
    });
    buttonPanel.add(saveButton);
    // button to remove saved settings
    JButton removeButton = new JButton("Remove");
    removeButton.setMnemonic(KeyEvent.VK_E);
    removeButton.addActionListener((e) -> {
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
    });
    buttonPanel.add(removeButton);

    // button to launch the task
    runButton = new JButton("Run Task");
    runButton.setMnemonic(KeyEvent.VK_R);
    runButton.addActionListener((e) -> startTask());
    buttonPanel.add(runButton);

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.weightx = 1.0;
    constraints.weighty = 0.01;
    panel.add(buttonPanel, constraints);
  }

  /**
   * Setup command line field
   */
  private void setupCommandLine() {
    // setup text output area
    commandLine = new JTextField();
    commandLine.setEditable(false); // FIXME: Make editable!

    // Add the output pane to the bottom
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.weightx = 1;
    constraints.weighty = .01;
    panel.add(commandLine, constraints);
  }

  /**
   * Setup logging area
   */
  private void setupLoggingArea() {
    // setup text output area
    outputArea = new LogPanel();

    // Create the scroll pane and add the table to it.
    JScrollPane outputPane = new JScrollPane(outputArea);
    outputPane.setPreferredSize(new Dimension(800, 400));

    // Add the output pane to the bottom
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 4;
    constraints.weightx = 1;
    constraints.weighty = 1;
    panel.add(outputPane, constraints);
  }

  /**
   * Serialize the parameter table and run setParameters().
   */
  protected void updateParameterTable() {
    parameterTable.setEnabled(false);
    ArrayList<String> list = new ArrayList<String>(parameters.size() * 2 + 1);
    list.add(maincls.getCanonicalName());
    parameters.serializeParameters(list);
    doSetParameters(list);
    parameterTable.setEnabled(true);
  }

  /**
   * Do the actual setParameters invocation.
   *
   * @param params Parameters
   */
  protected void doSetParameters(List<String> params) {
    if(!params.isEmpty()) {
      String first = params.get(0);
      if(!first.startsWith("-")) {
        Class<? extends AbstractApplication> c = ELKIServiceRegistry.findImplementation(AbstractApplication.class, first);
        if(c != null) {
          maincls = c;
          params.remove(0);
        }
      }
    }
    outputArea.clear();
    SerializedParameterization config = new SerializedParameterization(params);
    TrackParameters track = new TrackParameters(config);
    track.tryInstantiate(LoggingStep.class);
    track.tryInstantiate(maincls);
    config.logUnusedParameters();
    // config.logAndClearReportedErrors();
    final boolean hasErrors = (config.getErrors().size() > 0);
    if(hasErrors && !params.isEmpty()) {
      reportErrors(config);
    }
    runButton.setEnabled(!hasErrors);

    List<String> remainingParameters = config.getRemainingParameters();

    String mainnam = maincls.getCanonicalName();
    if(mainnam.startsWith(APP_PREFIX)) {
      mainnam = mainnam.substring(APP_PREFIX.length());
    }
    commandLine.setText(format(mainnam, params));

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
   * Format objects to a command line.
   *
   * @param params Parameters to format (Strings, or list of strings)
   * @return Formatted string
   */
  private String format(Object... params) {
    StringBuilder buf = new StringBuilder();
    for(Object p : params) {
      if(p instanceof String) {
        formatTo(buf, (String) p);
      }
      else if(p instanceof List) {
        formatTo(buf, (List<?>) p);
      }
      else {
        LOG.warning("Incorrect object type: " + p.getClass());
      }
    }
    return buf.toString();
  }

  /**
   * Format a list of strings to a buffer.
   *
   * @param buf Output buffer
   * @param params List of strings
   */
  private void formatTo(StringBuilder buf, List<?> params) {
    for(Object p : params) {
      if(p instanceof String) {
        formatTo(buf, (String) p);
      }
      else {
        LOG.warning("Incorrect object type: " + p.getClass());
      }
    }
  }

  /**
   * Format a single string for the command line.
   *
   * @param buf Output buffer
   * @param s String
   */
  private void formatTo(StringBuilder buf, String s) {
    if(s == null || s.length() == 0) {
      return;
    }
    if(buf.length() > 0) {
      buf.append(' ');
    }
    // Test for escaping necessary
    int escape = 0;
    for(int i = 0, l = s.length(); i < l; i++) {
      char c = s.charAt(i);
      if(c == '\\') {
        escape |= 8;
      }
      else if(c <= ' ' || c >= 128 || c == '<' || c == '>' || c == '|' || c == '$') {
        escape |= 1;
      }
      else if(c == '"') {
        escape |= 2;
      }
      else if(c == '\'') {
        escape |= 4;
      }
    }
    if(escape == 0) {
      buf.append(s); // No escaping.
    }
    else if((escape & 10) == 0) {
      buf.append('"').append(s).append('"');
    }
    else if((escape & 12) == 0) {
      buf.append('\'').append(s).append('\'');
    }
    else { // Full escaping.
      buf.append('"');
      for(int i = 0, l = s.length(); i < l; i++) {
        char c = s.charAt(i);
        if(c == '"' || c == '\\' || c == '$') {
          buf.append('\\');
        }
        buf.append(c);
      }
      buf.append('"');
    }
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
    final ArrayList<String> params = new ArrayList<>(parameters.size() * 2);
    parameters.serializeParameters(params);
    parameterTable.setEnabled(true);

    runButton.setEnabled(false);

    outputArea.clear();
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
    StringBuilder buf = new StringBuilder(500).append("Task is not completely configured:").append(NEWLINE).append(NEWLINE);
    for(ParameterException e : config.getErrors()) {
      if(e instanceof UnspecifiedParameterException) {
        buf.append("The parameter ") //
            .append(((UnspecifiedParameterException) e).getParameterName()) //
            .append(" is required.").append(NEWLINE);
      }
      else {
        buf.append(e.getMessage()).append(NEWLINE);
      }
    }
    LOG.warning(buf.toString());
    config.clearErrors();
  }

  @Override
  public void run() {
    frame.setVisible(true);
    outputArea.becomeDefaultLogger();
  }

  /**
   * Main method that just spawns the UI.
   *
   * @param args command line parameters
   */
  public static void main(final String[] args) {
    // Detect the common problem of an incomplete class path:
    try {
      Class<?> clz = Thread.currentThread().getContextClassLoader().loadClass("de.lmu.ifi.dbs.elki.database.ids.DBIDUtil");
      clz.getMethod("newHashSet").invoke(null);
    }
    catch(ReflectiveOperationException e) {
      StringBuilder msg = new StringBuilder(500).append("Your Java class path is incomplete.\n");
      if(e.getCause() != null) {
        for(Throwable t = e.getCause(); t != null; t = t.getCause()) {
          msg.append(t.toString()).append('\n');
        }
      }
      else {
        msg.append(e.toString()).append('\n');
      }
      msg.append("Make sure you have all the required jars on the classpath.\nOn the home page, you can find a 'elki-bundle' which should include everything.");
      JOptionPane.showMessageDialog(null, msg, "ClassPath incomplete", JOptionPane.ERROR_MESSAGE);
      return;
    }
    // Detect the broken Ubuntu jAyatana hack;
    String toolopt = System.getenv("JAVA_TOOL_OPTION");
    if(toolopt != null && toolopt.indexOf("jayatana") >= 0) {
      String msg = "The Ubuntu JAyatana 'global menu support' hack is known to cause problems with many Java applications.\nPlease unset JAVA_TOOL_OPTION.";
      JOptionPane.showMessageDialog(null, msg, "Incompatible with JAyatana", JOptionPane.ERROR_MESSAGE);
      return;
    }
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
            if(!params.isEmpty()) {
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
   * @composed - - - de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile
   */
  class SettingsComboboxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
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
