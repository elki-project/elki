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
package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.lmu.ifi.dbs.elki.gui.icons.StockIcon;
import de.lmu.ifi.dbs.elki.gui.util.ClassTree;
import de.lmu.ifi.dbs.elki.gui.util.ClassTree.ClassNode;
import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.gui.util.TreePopup;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Provide a configuration panel to choose a class with the help of a dropdown.
 * Additionally, the classes can in turn have additional parameters.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - ClassParameter
 */
public class ClassParameterConfigurator extends AbstractSingleParameterConfigurator<ClassParameter<?>> implements ActionListener, ChangeListener {
  /**
   * We need a panel to put our components on.
   */
  final JPanel panel;

  /**
   * Text field to store the name
   */
  final JTextField textfield;

  /**
   * The button to open the file selector
   */
  final JButton button;

  /**
   * The popup we use.
   */
  final TreePopup popup;

  /**
   * Configuration panel for child.
   */
  final ConfiguratorPanel child;

  /**
   * Constructor.
   * 
   * @param cp Class parameter
   * @param parent Parent component.
   */
  public ClassParameterConfigurator(ClassParameter<?> cp, JComponent parent) {
    super(cp, parent);
    textfield = new JTextField();
    textfield.setToolTipText(param.getShortDescription());
    if(cp.isDefined() && !cp.tookDefaultValue()) {
      textfield.setText(cp.getValueAsString());
    }
    textfield.setPreferredSize(new Dimension(400, textfield.getPreferredSize().height));
    if(!param.tookDefaultValue() && param.isDefined()) {
      textfield.setText(param.getValueAsString());
      // FIXME: pre-select the current / default value in tree!
    }

    // This is a hack, but the BasicArrowButton looks very odd on GTK.
    if(UIManager.getLookAndFeel().getName().indexOf("GTK") >= 0) {
      // This is not optimal either, but looks more consistent at least.
      button = new JButton(StockIcon.getStockIcon(StockIcon.EDIT_FIND));
    }
    else {
      button = new BasicArrowButton(BasicArrowButton.SOUTH);
    }
    button.setToolTipText(param.getShortDescription());
    button.addActionListener(this);

    TreeNode root = ClassTree.build(cp.getKnownImplementations(), cp.getRestrictionClass().getPackage().getName());

    popup = new TreePopup(new DefaultTreeModel(root));
    popup.getTree().setRootVisible(false);
    // popup.setPrototypeDisplayValue(cp.getRestrictionClass().getSimpleName());
    popup.addActionListener(this);

    Icon classIcon = StockIcon.getStockIcon(StockIcon.GO_NEXT);
    Icon packageIcon = StockIcon.getStockIcon(StockIcon.PACKAGE);
    TreePopup.Renderer renderer = (TreePopup.Renderer) popup.getTree().getCellRenderer();
    renderer.setLeafIcon(classIcon);
    renderer.setFolderIcon(packageIcon);
    
    // setup panel
    {
      panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(textfield, BorderLayout.CENTER);
      panel.add(button, BorderLayout.EAST);

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      parent.add(panel, constraints);
      finishGridRow();
    }

    // FIXME: re-add "none" option for optional parameters!
    // FIXME: re-highlight default value!

    // Child options
    {
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      constraints.insets = new Insets(0, 10, 0, 0);
      child = new ConfiguratorPanel();
      child.addChangeListener(this);
      parent.add(child, constraints);
    }
  }

  @Override
  public void addParameter(Object owner, Parameter<?> param, TrackParameters track) {
    child.addParameter(owner, param, track);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == button) {
      popup.show(panel);
      return;
    }
    if(e.getSource() == popup) {
      if (e.getActionCommand() == TreePopup.ACTION_CANCELED) {
        popup.setVisible(false);
        textfield.requestFocus();
        return;
      }
      TreePath path = popup.getTree().getSelectionPath();
      final Object comp = path != null ? path.getLastPathComponent() : null;
      if(comp instanceof ClassNode) {
        String newClass = ((ClassNode) comp).getClassName();
        if(newClass != null && !newClass.isEmpty()) {
          textfield.setText(newClass);
          popup.setVisible(false);
          fireValueChanged();
        }
      }
      return;
    }
    LoggingUtil.warning("actionPerformed triggered by unknown source: " + e.getSource());
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() == child) {
      fireValueChanged();
      return;
    }
    LoggingUtil.warning("stateChanged triggered by unknown source: " + e.getSource());
  }

  @Override
  public String getUserInput() {
    String val = textfield.getText();
    if(val.startsWith(DynamicParameters.STRING_USE_DEFAULT)) {
      return null;
    }
    if(DynamicParameters.STRING_OPTIONAL.equals(val)) {
      return null;
    }
    return val;
  }

  @Override
  public void appendParameters(ListParameterization params) {
    super.appendParameters(params);
    child.appendParameters(params);
  }
}