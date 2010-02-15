package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class TrackParameters implements Parameterization {
  Parameterization inner;
  
  java.util.Vector<Pair<Object, Parameter<?,?>>> options = new java.util.Vector<Pair<Object, Parameter<?,?>>>();

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
  
  public Collection<Pair<Object, Parameter<?,?>>> getParameters() {
    return options;
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    return inner.checkConstraint(constraint);
  }
}