package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.InternalParameterizationErrors;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler that doesn't set any parameters.
 * 
 * This is mostly useful for documentation purposes, listing all parameters
 * in a non-recursive way.
 * 
 * @author Erich Schubert
 */
public class UnParameterization implements Parameterization {  
  /**
   * Errors
   */
  java.util.Vector<ParameterException> errors = new java.util.Vector<ParameterException>();

  @Override
  public boolean hasUnusedParameters() {
    return false;
  }

  @Override
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    return false;
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  @Override
  public boolean hasErrors() {
    return errors.size() > 0;
  }

  @Override
  public boolean grab(Parameter<?, ?> opt) {
    return false;
  }

  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  @Override
  public boolean setValueForOption(Parameter<?, ?> opt) throws ParameterException {
    return false;
  }

  @Override
  public Parameterization descend(Object option) {
    return this;
  }
  
  @Override
  public <C> C tryInstantiate(Class<C> r, Class<?> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(r, c, this);
    }
    catch(Exception e) {
      reportError(new InternalParameterizationErrors("Error instantiating internal class: "+c.getName(), e));
      return null;
    }
  }

  @Override
  public <C> C tryInstantiate(Class<C> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(c, c, this);
    }
    catch(Exception e) {
      reportError(new InternalParameterizationErrors("Error instantiating internal class: "+c.getName(), e));
      return null;
    }
  }
}