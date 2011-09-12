package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler that only allows the use of default values.
 * 
 * @author Erich Schubert
 */
public class EmptyParameterization extends AbstractParameterization {  
  @Override
  public boolean hasUnusedParameters() {
    return false;
  }

  @Override
  public boolean setValueForOption(Parameter<?,?> opt) throws ParameterException {
    // Always return false, we don't have extra parameters,
    // This will cause {@link AbstractParameterization} to use the default values
    return false;
  }

  /** {@inheritDoc}
   * Default implementation, for flat parameterizations. 
   */
  @Override
  public Parameterization descend(Object option) {
    return this;
  }
}