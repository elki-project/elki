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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization.ParameterPair;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * This configuration can be "rewound" to allow the same values to be consumed
 * multiple times, by different classes. This is used in best-effort
 * parameterization when some instances might not apply given the actual data,
 * e.g. in visualization classes.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
// TODO: Can we merge MergedParameterization and TrackParameters into one?
public class MergedParameterization implements Parameterization {
  /**
   * The parameterization we get the new values from.
   */
  final private Parameterization inner;

  /**
   * Parameters we used before, but have rewound
   */
  final private ListParameterization current;

  /**
   * Parameters to rewind.
   */
  final private List<ParameterPair> used;

  /**
   * Constructor.
   * 
   * @param child Child parameterization to wrap.
   */
  public MergedParameterization(Parameterization child) {
    super();
    this.inner = child;
    this.current = new ListParameterization();
    this.used = new ArrayList<>();
  }

  /**
   * Constructor for descending
   * 
   * @param inner Child parameterization to use.
   * @param current Current parameterization to re-used
   * @param used Used parameters list.
   */
  private MergedParameterization(Parameterization inner, ListParameterization current, List<ParameterPair> used) {
    super();
    this.inner = inner;
    this.current = current;
    this.used = used;
  }

  /**
   * Rewind the configuration to the initial situation
   */
  public void rewind() {
    synchronized(used) {
      for(ParameterPair pair : used) {
        current.addParameter(pair);
      }
      used.clear();
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    try {
      if(current.setValueForOption(opt)) {
        used.add(new ParameterPair(opt.getOptionID(), opt.getValue()));
        return true;
      }
    }
    catch(ParameterException e) {
      current.reportError(e);
    }
    if(inner.setValueForOption(opt)) {
      used.add(new ParameterPair(opt.getOptionID(), opt.getValue()));
      return true;
    }
    used.add(new ParameterPair(opt.getOptionID(), opt.getDefaultValue()));
    return false;
  }

  @Override
  public Parameterization descend(Object option) {
    return new MergedParameterization(inner.descend(option), current.descend(option), used);
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return current.getErrors();
  }

  @Override
  public void reportError(ParameterException e) {
    inner.reportError(e);
  }

  @Override
  public boolean hasUnusedParameters() {
    return inner.hasUnusedParameters();
  }
}
