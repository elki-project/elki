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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JTextField;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Provide a configuration panel to input an arbitrary text parameter.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - Parameter
 */
// FIXME: update on focus loss?
// FIXME: restrictions for number input?
public class TextParameterConfigurator extends AbstractSingleParameterConfigurator<Parameter<?>> implements ActionListener {
  final JTextField value;

  public TextParameterConfigurator(Parameter<?> param, JComponent parent) {
    super(param, parent);

    // Input field
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    value = new JTextField();
    if(param.isDefined() && !param.tookDefaultValue()) {
      value.setText(param.getValueAsString());
    }
    value.setPreferredSize(new Dimension(400, value.getPreferredSize().height));
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
  public String getUserInput() {
    return value.getText();
  }
}
