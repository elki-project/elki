package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Parameterization handler using a List and OptionIDs, for programmatic use.
 * 
 * @author Erich Schubert
 */
public class ListParameterization extends AbstractParameterization {
  /**
   * The actual parameters, for storage
   */
  LinkedList<Pair<OptionID, Object>> parameters = new LinkedList<Pair<OptionID, Object>>();

  /**
   * Default constructor.
   */
  public ListParameterization() {
    super();
  }
  
  /**
   * Constructor with an existing collection.
   * 
   * @param dbParameters existing parameter collection
   */
  public ListParameterization(Collection<Pair<OptionID, Object>> dbParameters) {
    this();
    for (Pair<OptionID, Object> pair : dbParameters) {
      addParameter(pair.first, pair.second);
    }
  }

  /**
   * Add a flag to the parameter list
   * 
   * @param optionid Option ID
   */
  public void addFlag(OptionID optionid) {
    parameters.add(new Pair<OptionID, Object>(optionid, Flag.SET));
  }

  /**
   * Add a parameter to the parameter list
   * 
   * @param optionid Option ID
   * @param value Value
   */
  public void addParameter(OptionID optionid, Object value) {
    parameters.add(new Pair<OptionID, Object>(optionid, value));
  }
  
  /**
   * Convenience - add a Flag option directly.
   * 
   * @param flag Flag to add, if set
   */
  public void forwardOption(Flag flag) {
    if (flag.isDefined() && flag.getValue()) {
      addFlag(flag.getOptionID());
    }
  }
  
  /**
   * Convenience - add a Parameter for forwarding
   * 
   * @param param Parameter to add
   */
  public void forwardOption(Parameter<?,?> param) {
    if (param.isDefined()) {
      addParameter(param.getOptionID(), param.getValue());
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?,?> opt) throws ParameterException { 
    Iterator<Pair<OptionID, Object>> iter = parameters.iterator();
    while(iter.hasNext()) {
      Pair<OptionID, Object> pair = iter.next();
      if(pair.first == opt.getOptionID()) {
        iter.remove();
        opt.setValue(pair.second);
        return true;
      }
    }
    return false;
  }

  /**
   * Return the yet unused parameters.
   * 
   * @return Unused parameters.
   */
  public List<Pair<OptionID, Object>> getRemainingParameters() {
    return parameters;
  }

  @Override
  public boolean hasUnusedParameters() {
    return (parameters.size() > 0);
  }

  /** {@inheritDoc}
   * Default implementation, for flat parameterizations. 
   */
  @Override
  public Parameterization descend(@SuppressWarnings("unused") Parameter<?, ?> option) {
    return this;
  }
}