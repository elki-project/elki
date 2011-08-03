package de.lmu.ifi.dbs.elki.gui.multistep;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.AlgorithmTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.EvaluationTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.InputTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.LoggingTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.OutputTabPanel;
import de.lmu.ifi.dbs.elki.gui.util.LogPanel;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Experimenter-style multi step GUI.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf SettingsComboboxModel
 * @apiviz.composedOf AlgorithmTabPanel
 * @apiviz.composedOf EvaluationTabPanel
 * @apiviz.composedOf InputTabPanel
 * @apiviz.composedOf LoggingTabPanel
 * @apiviz.composedOf OutputTabPanel
 */
public class MultiStepGUI extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * ELKI logger for the GUI
   */
  protected static final Logging logger = Logging.getLogger(MultiStepGUI.class);

  /**
   * Logging output area.
   */
  protected LogPanel outputArea;

  private InputTabPanel inputTab;

  private AlgorithmTabPanel algTab;

  private EvaluationTabPanel evalTab;

  private OutputTabPanel outTab;

  private LoggingTabPanel logTab;

  /**
   * Constructor
   */
  public MultiStepGUI() {
    super();
    this.setLayout(new GridBagLayout());
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
      constraints.gridy = 1;
      constraints.weightx = 1;
      constraints.weighty = 1;
      add(outputPane, constraints);

      // reconfigure logging
      outputArea.becomeDefaultLogger();
    }
    {
      // setup tabbed panels
      JTabbedPane panels = new JTabbedPane();

      // Add the output pane to the bottom
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.BOTH;
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 1;
      add(panels, constraints);

      addPanels(panels);
    }
  }

  private void addPanels(JTabbedPane panels) {
    inputTab = new InputTabPanel();
    algTab = new AlgorithmTabPanel(inputTab);
    evalTab = new EvaluationTabPanel(inputTab, algTab);
    outTab = new OutputTabPanel(inputTab, evalTab);
    logTab = new LoggingTabPanel();
    panels.addTab("Input", inputTab);
    panels.addTab("Algorithm", algTab);
    panels.addTab("Evaluation", evalTab);
    panels.addTab("Output", outTab);
    panels.addTab("Logging", logTab);

    ListParameterization config = new ListParameterization();
    // Clear errors after each step, so they don't consider themselves failed
    // because of earlier errors.
    logTab.setParameters(config);
    config.clearErrors();
    inputTab.setParameters(config);
    config.clearErrors();
    algTab.setParameters(config);
    config.clearErrors();
    evalTab.setParameters(config);
    config.clearErrors();
    outTab.setParameters(config);
    config.clearErrors();
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be
   * invoked from the event-dispatching thread.
   */
  protected static void createAndShowGUI() {
    // Create and set up the window.
    JFrame frame = new JFrame("ELKI ExpGUI");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      // ignore
    }
    try {
      frame.setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
    }
    catch(Exception e) {
      // Ignore - icon not found is not fatal.
    }

    // Create and set up the content pane.
    MultiStepGUI newContentPane = new MultiStepGUI();
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
    OutputStep.setDefaultHandlerVisualizer();
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        createAndShowGUI();
      }
    });
  }

  /**
   * Class to interface between the saved settings list and a JComboBox
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile
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