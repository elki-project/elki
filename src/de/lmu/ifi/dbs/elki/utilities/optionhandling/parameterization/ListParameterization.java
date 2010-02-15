package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
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
    if (flag.getValue()) {
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
  public boolean setValueForOption(@SuppressWarnings("unused") Object owner, Parameter<?,?> opt) throws ParameterException { 
    // TODO: just return false?
    if(!Parameter.class.isAssignableFrom(opt.getClass())) {
      throw new AbortException("Encountered a option " + opt.getName() + " that is neither a Parameter nor a Flag, and thus unsupported: " + opt.getClass().getName());
    }
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

  // FIXME: ERICH: INCOMPLETE TRANSITION: toString() here is NOT reliable!
  // This isn't really workable in a non-string context.
  // But it will suffice for transition purposes.
  @Deprecated
  public String[] asArray() {
    ArrayList<String> ret = new ArrayList<String>(2*parameters.size());
    for (Pair<OptionID, Object> pair : parameters) {
      ret.add(SerializedParameterization.OPTION_PREFIX + pair.first.getName());
      if (!(pair.second instanceof String)) {
        throw new RuntimeException("Deprecated functino asArray may only be used with string-serialized values!");
      }
      ret.add((String) pair.second);
    }
    return ret.toArray(new String[]{});
  }
}