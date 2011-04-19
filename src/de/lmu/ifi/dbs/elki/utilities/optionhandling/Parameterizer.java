package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Generic interface for a parameterizable factory.
 * 
 * @author Erich Schubert
 */
public interface Parameterizer {
  /**
   * Configure the class.
   * 
   * Note: the status is collected by the parameterization object, so that
   * multiple errors may arise and be reported in one run.
   * 
   * @param config Parameterization
   */
  public void configure(Parameterization config);
}
