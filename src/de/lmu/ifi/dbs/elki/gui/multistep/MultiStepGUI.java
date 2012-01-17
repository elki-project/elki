package de.lmu.ifi.dbs.elki.gui.multistep;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.AlgorithmTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.EvaluationTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.InputTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.LoggingTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.OutputTabPanel;
import de.lmu.ifi.dbs.elki.gui.multistep.panels.SavedSettingsTabPanel;
import de.lmu.ifi.dbs.elki.gui.util.LogPanel;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Experimenter-style multi step GUI.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf AlgorithmTabPanel
 * @apiviz.composedOf EvaluationTabPanel
 * @apiviz.composedOf InputTabPanel
 * @apiviz.composedOf LoggingTabPanel
 * @apiviz.composedOf OutputTabPanel
 * @apiviz.composedOf SavedSettingsTabPanel
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

  private SavedSettingsTabPanel setTab;

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
    SavedSettingsFile settings = new SavedSettingsFile(MiniGUI.SAVED_SETTINGS_FILENAME);
    try {
      settings.load();
    }
    catch(FileNotFoundException e) {
      logger.warning("Error loading saved settings.");
    }
    catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    inputTab = new InputTabPanel();
    algTab = new AlgorithmTabPanel(inputTab);
    evalTab = new EvaluationTabPanel(inputTab, algTab);
    outTab = new OutputTabPanel(inputTab, evalTab);
    logTab = new LoggingTabPanel();
    setTab = new SavedSettingsTabPanel(settings, this);
    panels.addTab("Input", inputTab);
    panels.addTab("Algorithm", algTab);
    panels.addTab("Evaluation", evalTab);
    panels.addTab("Output", outTab);
    panels.addTab("Logging", logTab);
    panels.addTab("Saved Settings", setTab);

    setParameters(new ListParameterization());
  }

  /**
   * Set the parameters.
   * 
   * @param config Parameterization
   */
  public void setParameters(Parameterization config) {
    // Clear errors after each step, so they don't consider themselves failed
    // because of earlier errors.
    logTab.setParameters(config);
    //config.clearErrors();
    inputTab.setParameters(config);
    //config.clearErrors();
    algTab.setParameters(config);
    //config.clearErrors();
    evalTab.setParameters(config);
    //config.clearErrors();
    outTab.setParameters(config);
    //config.clearErrors();
  }

  /**
   * Get the serialized parameters
   * 
   * @return Serialized parameters
   */
  public ArrayList<String> serializeParameters() {
    ListParameterization params = new ListParameterization();
    logTab.appendParameters(params);
    inputTab.appendParameters(params);
    algTab.appendParameters(params);
    evalTab.appendParameters(params);
    outTab.appendParameters(params);
    return params.serialize();
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
}