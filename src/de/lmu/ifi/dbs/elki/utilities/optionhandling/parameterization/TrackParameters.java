package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Utility wrapper to track parameters for a configuration session.
 * 
 * All actual Parameterization operations are forwarded to the inner class.
 * 
 * @author Erich Schubert
 */
public class TrackParameters implements Parameterization {
  /**
   * Inner parameterization
   */
  Parameterization inner;
  
  /**
   * Tracking storage
   */
  java.util.Vector<Pair<Object, Parameter<?,?>>> options = new java.util.Vector<Pair<Object, Parameter<?,?>>>();

  /**
   * Constructor.
   * 
   * @param inner Inner parameterization to wrap.
   */
  public TrackParameters(Parameterization inner) {
    super();
    this.inner = inner;
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return inner.getErrors();
  }

  @Override
  public boolean grab(Object owner, Parameter<?, ?> opt) {
    boolean success = inner.grab(owner, opt);
    if (success) {
      options.add(new Pair<Object, Parameter<?,?>>(owner, opt));
    }
    return success;
  }

  @Override
  public boolean hasUnusedParameters() {
    return inner.hasUnusedParameters();
  }

  @Override
  public void reportError(ParameterException e) {
    inner.reportError(e);
  }

  @Override
  public boolean setValueForOption(Object owner, Parameter<?, ?> opt) throws ParameterException {
    return inner.setValueForOption(owner, opt);
  }
  
  /**
   * Get all seen parameters, set or unset, along with their owner objects.
   * 
   * @return Parameters seen
   */
  public Collection<Pair<Object, Parameter<?,?>>> getAllParameters() {
    return options;
  }

  /**
   * Get the tracked parameters that were actually set.
   * 
   * @return Parameters given
   */
  public Collection<Pair<OptionID, Object>> getGivenParameters() {
    java.util.Vector<Pair<OptionID, Object>> ret = new java.util.Vector<Pair<OptionID, Object>>();
    for (Pair<Object, Parameter<?,?>> pair : options) {
      if (pair.second.getGivenValue() != null) {
        ret.add(new Pair<OptionID, Object>(pair.second.getOptionID(), pair.second.getGivenValue()));
      }
    }
    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    return inner.checkConstraint(constraint);
  }
}