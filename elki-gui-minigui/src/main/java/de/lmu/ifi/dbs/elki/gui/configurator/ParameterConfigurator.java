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

import javax.swing.event.ChangeListener;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Interface for different configuration assistants for the multistep GUI.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface ParameterConfigurator {
  /**
   * Add a parameter to the panel.
   * 
   * @param owner Owning ("parent") object
   * @param param Parameter
   * @param track Parameter tracker
   */
  void addParameter(Object owner, Parameter<?> param, TrackParameters track);

  /**
   * Add a change listener
   * 
   * @param listener Change listener
   */
  void addChangeListener(ChangeListener listener);

  /**
   * Remove a change listener
   * 
   * @param listener Change listener
   */
  void removeChangeListener(ChangeListener listener);

  /**
   * Append the parameters to a list.
   * 
   * @param params Parameter list (output)
   */
  void appendParameters(ListParameterization params);
}