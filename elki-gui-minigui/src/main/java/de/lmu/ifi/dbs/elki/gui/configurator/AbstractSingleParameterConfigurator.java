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

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Base class for MiniGUI input helpers
 * 
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <T> Parameter type
 */
public abstract class AbstractSingleParameterConfigurator<T extends Parameter<?>> extends AbstractParameterConfigurator<T> {
  /**
   * Label
   */
  final JLabel label;

  /**
   * Constructor.
   *
   * @param param Parameter
   * @param parent Parent edit control
   */
  public AbstractSingleParameterConfigurator(T param, JComponent parent) {
    super(param, parent);
    // Label
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    label = new JLabel(param.getOptionID().getName());
    label.setAlignmentX(0.0f);
    label.setToolTipText(param.getShortDescription());
    parent.add(label, constraints);
    // subclasses will add a second component to the row!
  }
}