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
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class that handles the parameterization of a class.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - Parameterization
 * @has - - - de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter
 */
public abstract class AbstractParameterizer implements Parameterizer {
  /**
   * Constant for "fresh" state.
   */
  private static final int STATE_FRESH = 0;

  /**
   * Constant for "initializing" state.
   */
  private static final int STATE_INIT = 1;

  /**
   * Constant for "complete" state.
   */
  private static final int STATE_COMPLETE = 2;

  /**
   * Constant for "errors" state.
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
  protected abstract Object makeInstance();

  /**
   * Method to configure a class, then instantiate when the configuration step
   * was successful.
   * <p>
   * <b>Don't call this directly use unless you know what you are doing.<br>
   * Instead, use {@link Parameterization#tryInstantiate(Class)}!</b>
   * <p>
   * Otherwise, {@code null} will be returned, and the resulting errors can be
   * retrieved from the {@link Parameterization} parameter object. In general,
   * you should be checking the {@link Parameterization} object for errors
   * before accessing the returned value, since it may be {@code null}
   * unexpectedly otherwise.
   * 
   * @param config Parameterization
   * @return Instance or {@code null}
   */
  public final Object make(Parameterization config) {
    if(state != STATE_FRESH) {
      throw new AbortException("Parameterizers may only be used once!");
    }
    state = STATE_INIT;

    Object owner = this.getClass().getDeclaringClass();
    config = config.descend(owner == null ? this : owner);
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