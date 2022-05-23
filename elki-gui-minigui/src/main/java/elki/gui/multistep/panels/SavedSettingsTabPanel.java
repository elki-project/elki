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
package elki.gui.multistep.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;

import elki.gui.multistep.MultiStepGUI;
import elki.gui.util.SavedSettingsFile;
import elki.logging.Logging;
import elki.utilities.optionhandling.parameterization.SerializedParameterization;

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
      savedSettingsModel = new SettingsComboboxModel();
      savedCombo = new JComboBox<>(savedSettingsModel);
      savedCombo.setEditable(true);
      savedCombo.setSelectedItem("[Saved Settings]");
      savedCombo.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
      if(savedCombo.getRenderer() instanceof JComponent) {
        ((JComponent) savedCombo.getRenderer()).setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
      }
      if(savedCombo.getEditor().getEditorComponent() instanceof JComponent) {
        ((JComponent) savedCombo.getEditor().getEditorComponent()).setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      }

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
        ArrayList<String> settings = store.get(savedSettingsModel.getSelectedItem());
        if(settings != null) {
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
      saveButton.addActionListener((e) -> {
        try {
          store.put(savedSettingsModel.getSelectedItem(), gui.serializeParameters());
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
        try {
          store.remove(savedSettingsModel.getSelectedItem());
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
   * @composed - - - elki.gui.util.SavedSettingsFile
   */
  class SettingsComboboxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Selected entry
     */
    protected String selected = null;

    /**
     * Constructor
     */
    public SettingsComboboxModel() {
      super();
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
