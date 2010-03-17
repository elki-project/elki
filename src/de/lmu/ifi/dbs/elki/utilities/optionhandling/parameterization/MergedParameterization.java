package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * This configuration can be "rewound" to allow the same values to be consumed
 * multiple times, by different classes. This is used in best-effort
 * parameterization when some instances might not apply given the actual data,
 * e.g. in visualization classes.
 * 
 * @author Erich Schubert
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
  final private java.util.Vector<Pair<OptionID, Object>> used;

  /**
   * Constructor.
   * 
   * @param inner Child parameterization to wrap.
   */
  public MergedParameterization(Parameterization child) {
    super();
    this.inner = child;
    this.current = new ListParameterization();
    this.used = new java.util.Vector<Pair<OptionID, Object>>();
  }

  /**
   * Constructor for descending
   * 
   * @param inner Child parameterization to use.
   * @param current Current parameterization to re-used
   * @param used Used parameters list.
   */
  private MergedParameterization(Parameterization inner, ListParameterization current, Vector<Pair<OptionID, Object>> used) {
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
      for(Pair<OptionID, Object> pair : used) {
        current.addParameter(pair.first, pair.second);
      }
      used.removeAllElements();
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?, ?> opt) throws ParameterException {
    try {
      if(current.setValueForOption(opt)) {
        used.add(new Pair<OptionID, Object>(opt.getOptionID(), opt.getValue()));
        return true;
      }
    }
    catch(ParameterException e) {
      current.reportError(e);
    }
    if(inner.setValueForOption(opt)) {
      used.add(new Pair<OptionID, Object>(opt.getOptionID(), opt.getValue()));
      return true;
    }
    return false;
  }

  @Override
  public Parameterization descend(Parameter<?, ?> option) {
    // We should descend into current, too - but the API doesn't give us a
    // ListParameterization then!
    return new MergedParameterization(inner.descend(option), current, used);
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
  public boolean grab(Parameter<?, ?> opt) {
    try {
      if (setValueForOption(opt)) {
        return true;
      }
      // Try default value instead.
      if (opt.tryDefaultValue()) {
        return true;
      }
      // No value available.
      return false;
    }
    catch(ParameterException e) {
      reportError(e);
      return false;
    }
  }

  @Override
  public boolean hasUnusedParameters() {
    return inner.hasUnusedParameters();
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    // TODO: does checkConstraint work here reliably?
    return inner.checkConstraint(constraint);
  }
}