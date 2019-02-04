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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Class containing an object, and the associated value.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class TrackedParameter {
  /**
   * Option ID
   */
  private Object owner;

  /**
   * Parameter value
   */
  private Parameter<?> parameter;

  /**
   * Constructor.
   *
   * @param owner Object owning the parameter value
   * @param parameter Parameter
   */
  public TrackedParameter(Object owner, Parameter<?> parameter) {
    this.owner = owner;
    this.parameter = parameter;
  }

  /**
   * Get the owner object.
   * 
   * @return Parameter owner
   */
  public Object getOwner() {
    return owner;
  }

  /**
   * Get the parameter observed.
   * 
   * @return Parameter
   */
  public Parameter<?> getParameter() {
    return parameter;
  }
}