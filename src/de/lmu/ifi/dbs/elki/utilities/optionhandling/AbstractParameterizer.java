package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class that handles the parameterization of a class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Parameterization
 * @apiviz.has Parameter
 * @apiviz.excludeSubtypes
 */
public abstract class AbstractParameterizer {
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
