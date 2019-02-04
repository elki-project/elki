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
package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;

/**
 * Projection interface.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <IN> Input data type
 * @param <OUT> Output data type
 */
public interface Projection<IN, OUT> {
  /**
   * Initialize
   * 
   * @param in Data type to use for projecting.
   */
  void initialize(SimpleTypeInformation<? extends IN> in);

  /**
   * Project a single instance.
   * 
   * @param data Data to project
   * @return Projected data
   */
  OUT project(IN data);

  /**
   * Input type information.
   * 
   * @return Type restriction
   */
  TypeInformation getInputDataTypeInformation();

  /**
   * Output type restriction
   * 
   * @return Output type
   */
  SimpleTypeInformation<OUT> getOutputDataTypeInformation();
}
