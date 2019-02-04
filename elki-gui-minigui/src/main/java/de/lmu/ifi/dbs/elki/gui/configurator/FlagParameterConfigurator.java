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

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Provide a configuration panel to modify a boolean via a checkbox.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - Flag
 */
public class FlagParameterConfigurator extends AbstractParameterConfigurator<Flag> implements ActionListener {
  final JCheckBox value;

  public FlagParameterConfigurator(Flag param, JComponent parent) {
    super(param, parent);

    // Input field
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 2;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    value = new JCheckBox(param.getOptionID().getName());
    if(param.isDefined() && !param.tookDefaultValue()) {
      value.setSelected(param.isTrue());
    }
    value.setToolTipText(param.getShortDescription());
    parent.add(value, constraints);
    finishGridRow();
    
    value.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == value) {
      fireValueChanged();
    } else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: "+e.getSource());
    }
  }
  
  @Override
  public Boolean getUserInput() {
    return value.isSelected() ? Boolean.TRUE : null;
  }
}