package de.lmu.ifi.dbs.elki.gui.minigui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.gui.util.LogPane;
import de.lmu.ifi.dbs.elki.gui.util.ParameterTable;
import de.lmu.ifi.dbs.elki.gui.util.ParametersModel;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;

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
   * Filename for saved settings
   */
  public static final String SAVED_SETTINGS_FILENAME = "MiniGUI-saved-settings.txt";

  /**
   * Newline used in output.
   */
  public static final String NEWLINE = System.getProperty("line.separator");

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
   * Settings storage
   */
  protected SavedSettingsFile store = new SavedSettingsFile(SAVED_SETTINGS_FILENAME);

  /**
   * Combo box for saved settings
   */
  protected JComboBox savedCombo;

  /**
   * Model to link the combobox with
   */
  protected SettingsComboboxModel savedSettingsModel;

  /**
   * The "run" button.
   */
  private JButton runButton;

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

    // Combo box for saved settings
    savedSettingsModel = new SettingsComboboxModel(store);
    savedCombo = new JComboBox(savedSettingsModel);
    savedCombo.setEditable(true);
    savedCombo.setSelectedItem("[Saved Settings]");
    buttonPanel.add(savedCombo);

    // button to load settings
    JButton loadButton = new JButton("Load");
    loadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
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
    saveButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        String key = savedSettingsModel.getSelectedItem();
        store.put(key, parameters.serializeParameters());
        try {
          store.save();
        }
        catch(IOException e1) {
          logger.exception(e1);
        }
        savedSettingsModel.update();
      }
    });
    buttonPanel.add(saveButton);
    // button to remove saved settings
    JButton removeButton = new JButton("Remove");
    removeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        String key = savedSettingsModel.getSelectedItem();
        store.remove(key);
        try {
          store.save();
        }
        catch(IOException e1) {
          logger.exception(e1);
        }
        savedCombo.setSelectedItem("[Saved Settings]");
        savedSettingsModel.update();
      }
    });
    buttonPanel.add(removeButton);

    // button to launch the task
    runButton = new JButton("Run Task");
    runButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        Thread r = new Thread() {
          @Override
          public void run() {
            runTask();
          }
        };
        r.start();
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
    //ps.add("-algorithm XXX");
    doSetParameters(ps);

    // load saved settings (we wanted to have the logger first!)
    try {
      store.load();
      savedSettingsModel.update();
    }
    catch(FileNotFoundException e) {
      // Ignore - probably didn't save any settings yet.
    }
    catch(IOException e) {
      logger.exception(e);
    }

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
   */
  protected void doSetParameters(ArrayList<String> params) {
    SerializedParameterization config = new SerializedParameterization(params);
    TrackParameters track = new TrackParameters(config);
    new KDDTask<DatabaseObject>(track);
    config.logUnusedParameters();
    config.logAndClearReportedErrors();
    
    List<String> remainingParameters = config.getRemainingParameters();
    
    // update table:
    parameterTable.setEnabled(false);
    parameters.updateFromTrackParameters(track);
    // Add remaining parameters
    if (remainingParameters != null && !remainingParameters.isEmpty()) {
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

  /**
   * Do a full run of the KDDTask with the specified parameters.
   */
  protected void runTask() {
    parameterTable.setEnabled(false);
    ArrayList<String> params = parameters.serializeParameters();
    parameterTable.setEnabled(true);

    runButton.setEnabled(false);

    outputArea.clear();
    outputArea.publish("Running: " + FormatUtil.format(params, " ") + NEWLINE, Level.INFO);
    
    SerializedParameterization config = new SerializedParameterization(params);
    KDDTask<DatabaseObject> task = new KDDTask<DatabaseObject>(config);
    try {
      config.logUnusedParameters();
      config.failOnErrors();
      task.run();
    }
    catch(Exception e) {
      logger.exception(e);
    }

    runButton.setEnabled(true);
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
   * 
   * @param args command line parameters
   */
  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }

  /**
   * Class to interface between the saved settings list and a JComboBox
   * 
   * @author Erich Schubert
   */
  class SettingsComboboxModel extends AbstractListModel implements ComboBoxModel {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Settings storage
     */
    protected SavedSettingsFile store;

    /**
     * Selected entry
     */
    protected String selected = null;

    /**
     * Constructor
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
    public Object getElementAt(int index) {
      return store.getElementAt(index).first;
    }

    @Override
    public int getSize() {
      return store.size();
    }

    /**
     * Force an update
     */
    public void update() {
      fireContentsChanged(this, 0, getSize() + 1);
    }
  }
}