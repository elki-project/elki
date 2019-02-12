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
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;

import de.lmu.ifi.dbs.elki.gui.multistep.MultiStepGUI;
import de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;

/**
 * Tab panel to manage saved settings.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - SavedSettingsTabPanel.SettingsComboboxModel
 */
public class SavedSettingsTabPanel extends JPanel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(SavedSettingsTabPanel.class);

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
  JComboBox<String> savedCombo;

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
      savedCombo = new JComboBox<>(savedSettingsModel);
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
      loadButton.addActionListener((e) -> {
        String key = savedSettingsModel.getSelectedItem();
        ArrayList<String> settings = store.get(key);
        SerializedParameterization config = new SerializedParameterization(settings);
        gui.setParameters(config);
        config.logUnusedParameters();
        config.clearErrors();
      });
      buttonPanel.add(loadButton);
      // button to save settings
      JButton saveButton = new JButton("Save");
      saveButton.setMnemonic(KeyEvent.VK_S);
      saveButton.addActionListener((e) -> {
        String key = savedSettingsModel.getSelectedItem();
        store.put(key, gui.serializeParameters());
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
   * @composed - - - de.lmu.ifi.dbs.elki.gui.util.SavedSettingsFile
   */
  class SettingsComboboxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
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
    public String getElementAt(int index) {
      return store.getElementAt(store.size() - 1 - index).first;
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
