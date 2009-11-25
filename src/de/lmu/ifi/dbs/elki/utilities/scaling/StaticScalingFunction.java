package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for Scaling functions that do NOT depend on analyzing the data set.
 * But will always map x to the same f(x) (given the same parameters when
 * implementing {@link Parameterizable}).
 * 
 * @author Erich Schubert
 * 
 */
public interface StaticScalingFunction extends ScalingFunction {
  // Empty marker interface
}
