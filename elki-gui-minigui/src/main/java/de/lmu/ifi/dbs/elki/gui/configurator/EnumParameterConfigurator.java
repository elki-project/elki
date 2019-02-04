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

import javax.swing.JComboBox;
import javax.swing.JComponent;

import de.lmu.ifi.dbs.elki.gui.util.DynamicParameters;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;

/**
 * Panel to configure EnumParameters by offering a dropdown to choose from.
 * 
 * TODO: offer radio buttons when just a few choices are available?
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - EnumParameter
 */
public class EnumParameterConfigurator extends AbstractSingleParameterConfigurator<EnumParameter<?>> implements ActionListener {
  final JComboBox<String> value;

  public EnumParameterConfigurator(EnumParameter<?> cp, JComponent parent) {
    super(cp, parent);
    // Input field
    {
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1.0;
      value = new JComboBox<>();
      value.setToolTipText(param.getShortDescription());
      value.setPrototypeDisplayValue(cp.getPossibleValues().iterator().next());
      parent.add(value, constraints);
      finishGridRow();
    }

    if(!param.tookDefaultValue() && param.isDefined()) {
      value.addItem(param.getValueAsString());
      value.setSelectedIndex(0);
    }

    // For parameters with a default value, offer using the default
    // For optional parameters, offer not specifying them.
    if(cp.hasDefaultValue()) {
      value.addItem(DynamicParameters.STRING_USE_DEFAULT + cp.getDefaultValueAsString());
    }
    else if(cp.isOptional()) {
      value.addItem(DynamicParameters.STRING_OPTIONAL);
    }
    // Offer the shorthand version of class names.
    for(String s : cp.getPossibleValues()) {
      value.addItem(s);
    }
    value.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if(e.getSource() == value) {
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("actionPerformed triggered by unknown source: " + e.getSource());
    }
  }

  @Override
  public String getUserInput() {
    String val = (String) value.getSelectedItem();
    if(val.startsWith(DynamicParameters.STRING_USE_DEFAULT)) {
      return null;
    }
    if(DynamicParameters.STRING_OPTIONAL.equals(val)) {
      return null;
    }
    return val;
  }
}