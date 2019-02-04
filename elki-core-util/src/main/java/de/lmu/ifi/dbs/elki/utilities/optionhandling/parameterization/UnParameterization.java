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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler that doesn't set any parameters.
 * 
 * This is mostly useful for documentation purposes, listing all parameters
 * in a non-recursive way.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class UnParameterization implements Parameterization {  
  /**
   * Errors
   */
  List<ParameterException> errors = new ArrayList<>();

  @Override
  public boolean hasUnusedParameters() {
    return false;
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  @Override
  public boolean grab(Parameter<?> opt) {
    return false;
  }

  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) {
    return false;
  }
}