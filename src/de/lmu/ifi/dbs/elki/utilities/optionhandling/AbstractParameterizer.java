package de.lmu.ifi.dbs.elki.utilities.optionhandling;
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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class that handles the parameterization of a class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Parameterization
 * @apiviz.has de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter
 * @apiviz.excludeSubtypes
 */
public abstract class AbstractParameterizer implements Parameterizer {
  /**
   * Constant for "fresh" state
   */
  private static final int STATE_FRESH = 0;

  /**
   * Constant for "initializing" state
   */
  private static final int STATE_INIT = 1;

  /**
   * Constant for "complete" state
   */
  private static final int STATE_COMPLETE = 2;

  /**
   * Constant for "errors" state
   */
  private static final int STATE_ERRORS = -1;

  /**
   * Parameterization state.
   */
  private int state = STATE_FRESH;

  /**
   * Add all options.
   * 
   * <b>ALWAYS call super.makeOptions(config), unless you have a strong reason
   * to do otherwise!</b>
   * 
   * @param config Parameterization to add options to.
   */
  protected void makeOptions(Parameterization config) {
    // Nothing to do here.
  }

  // TODO: remove
  @Override
  public final void configure(Parameterization config) {
    makeOptions(config);
  }

  /**
   * Make an instance after successful configuration.
   * 
   * @return instance
   */
  abstract protected Object makeInstance();

  /**
   * The main parameterization wrapper.
   * 
   * Usually, you should use {@link Parameterization#tryInstantiate(Class)}
   * instead!
   * 
   * @param config Parameterization
   * @return Instance
   */
  public final Object make(Parameterization config) {
    if(state != STATE_FRESH) {
      throw new AbortException("Parameterizers may only be set up once!");
    }
    state = STATE_INIT;

    Object owner = this.getClass().getDeclaringClass();
    if(owner == null) {
      owner = this;
    }
    config = config.descend(owner);
    makeOptions(config);

    if(!config.hasErrors()) {
      state = STATE_COMPLETE;
      Object ret = makeInstance();
      if(ret == null) {
        throw new AbortException("makeInstance() returned null!", new Throwable());
      }
      return ret;
    }
    else {
      state = STATE_ERRORS;
      return null;
    }
  }
}