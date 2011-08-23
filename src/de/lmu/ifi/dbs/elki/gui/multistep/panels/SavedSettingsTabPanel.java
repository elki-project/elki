package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import de.lmu.ifi.dbs.elki.gui.multistep.MultiStepGUI;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;

/**
 * Tab panel to manage saved settings.
 * 
 * @author Erich Schubert
 */
public class SavedSettingsTabPanel extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Logger
   */
  protected static final Logging logger = Logging.getLogger(SavedSettingsTabPanel.class);

  /**
   * The settings file to display.
   */
  SavedSettingsFile store;

  /**
   * Settings combo box.
   */
  SettingsComboboxModel savedSettingsModel;

  /**
   * The combo box to use
   */
  JComboBox savedCombo;

  /**
   * The UI to set parameters on.
   * 
   * TODO: Use an Interface instead?
   */
  private MultiStepGUI gui;

  /**
   * Constructor.
   * 
   * @param store2 Settings store.
   * @param gui2 UI to use
   */
  public SavedSettingsTabPanel(SavedSettingsFile store2, MultiStepGUI gui2) {
    super();
    this.store = store2;
    this.gui = gui2;
    this.setLayout(new GridBagLayout());
    // Dropdown for saved settings
    {
      savedSettingsModel = new SettingsComboboxModel(store);
      savedCombo = new JComboBox(savedSettingsModel);
      savedCombo.setEditable(true);
      savedCombo.setSelectedItem("[Saved Settings]");
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1.0;
      constraints.weighty = 0.01;
      add(savedCombo, constraints);
    }
    // Button panel
    {
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      
      // button to load settings
      JButton loadButton = new JButton("Load");
      loadButton.setMnemonic(KeyEvent.VK_L);
      loadButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          String key = savedSettingsModel.getSelectedItem();
          ArrayList<String> settings = store.get(key);
          SerializedParameterization config = new SerializedParameterization(settings);
          gui.setParameters(config);
          config.logUnusedParameters();
          config.clearErrors();
        }
      });
      buttonPanel.add(loadButton);
      // button to save settings
      JButton saveButton = new JButton("Save");
      saveButton.setMnemonic(KeyEvent.VK_S);
      saveButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          String key = savedSettingsModel.getSelectedItem();
          store.put(key, gui.serializeParameters());
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
      removeButton.setMnemonic(KeyEvent.VK_E);
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
      
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.weightx = 1.0;
      constraints.weighty = 0.01;
      add(buttonPanel, constraints);
    }
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