package de.lmu.ifi.dbs.elki.gui.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

/**
 * Wrapper around a set of parameters for ELKI, that may not yet be complete or correct.
 * 
 * @author Erich Schubert
 */
public class DynamicParameters {
  public static final int BIT_INCOMPLETE = 0;
  public static final int BIT_INVALID = 1;
  public static final int BIT_SYNTAX_ERROR = 2;
  public static final int BIT_OPTIONAL = 3;
  public static final int BIT_DEFAULT_VALUE = 4;
  
  public static final String STRING_USE_DEFAULT = "(use default)";
  public static final String STRING_OPTIONAL = "(optional)";

  /**
   * Parameter storage
   */
  protected ArrayList<Triple<Parameter<?,?>, String, BitSet>> parameters;

  /**
   * Constructor
   */
  public DynamicParameters() {
    super();
    this.parameters = new ArrayList<Triple<Parameter<?,?>, String, BitSet>>();
  }

  /**
   * Get the size
   * 
   * @return number of parameters
   */
  public int size() {
    return this.parameters.size();
  }

  /**
   * Get the Option for the given index
   * 
   * @param index
   * @return Option
   */
  public Parameter<?,?> getOption(int index) {
    return this.parameters.get(index).first;
  }
  
  /**
   * Get the value for the given index
   * 
   * @param index
   * @return Value as String
   */
  public String getValue(int index) {
    return this.parameters.get(index).second;
  }
  
  /**
   * Get the flags bit set for the given index
   * 
   * @param index
   * @return Flags bit set
   */
  public BitSet getFlags(int index) {
    return this.parameters.get(index).third;
  }

  /**
   * Set the value of the ith parameter
   * 
   * @param index Parameter index
   * @param value New value
   */
  public synchronized void setValue(int index, String value) {
    Triple<Parameter<?,?>, String, BitSet> p;
    if(index < parameters.size()) {
      p = parameters.get(index);
    }
    else {
      BitSet flags = new BitSet();
      p = new Triple<Parameter<?,?>, String, BitSet>(null, "", flags);
      parameters.add(p);
    }
    BitSet flags = p.getThird();
    
    p.setSecond(value);

    // Detect wrong values for flags.
    if(p.getFirst() instanceof Flag) {
      if((!Flag.SET.equals(value)) && (!Flag.NOT_SET.equals(value))) {
        flags.set(DynamicParameters.BIT_SYNTAX_ERROR);
      }
      else {
        flags.clear(DynamicParameters.BIT_SYNTAX_ERROR);
      }
    }
  }

  /**
   * Update the Parameter list from the collected options of an ELKI context
   * 
   * @param options Collected options
   */
  public synchronized void updateFromOptions(List<Pair<Object, Parameter<?,?>>> options) {
    parameters.clear();
    for(Pair<Object, Parameter<?,?>> p : options) {
      Parameter<?,?> option = p.getSecond();
      String value = null;
      if (option.isDefined() && !option.tookDefaultValue()) {
        value = option.getValueAsString();
      }
      if(value == null) {
        if(option instanceof Flag) {
          value = Flag.NOT_SET;
        }
        else {
          value = "";
        }
      }
      BitSet bits = new BitSet();
      if(option.isOptional()) {
        bits.set(BIT_OPTIONAL);
      }
      if(option.hasDefaultValue() && option.tookDefaultValue()) {
        bits.set(BIT_DEFAULT_VALUE);
      }
      if(value == "") {
        if(!bits.get(BIT_DEFAULT_VALUE) && !bits.get(BIT_OPTIONAL)) {
          bits.set(BIT_INCOMPLETE);
        }
      }
      if(value != "") {
        try {
          if(!option.isValid(value)) {
            bits.set(BIT_INVALID);
          }
        }
        catch(ParameterException e) {
          bits.set(BIT_INVALID);
        }
      }
      // SKIP these options, they should be moved out of KDDTask:
      if (option.getOptionID() == OptionID.HELP || option.getOptionID() == OptionID.HELP_LONG) {
        continue;
      }
      if (option.getOptionID() == OptionID.DESCRIPTION) {
        continue;
      }
      Triple<Parameter<?,?>, String, BitSet> t = new Triple<Parameter<?,?>, String, BitSet>(option, value, bits);
      parameters.add(t);
    }
  }
  
  /**
   * Serialize parameters into an array list to pass to setParameters()
   * 
   * @return Array list of String parameters.
   */
  public synchronized ArrayList<String> serializeParameters() {
    ArrayList<String> p = new ArrayList<String>(2 * parameters.size());
    for(Triple<Parameter<?,?>, String, BitSet> t : parameters) {
      if(t.getFirst() != null) {
        if(t.getSecond() != null && t.getSecond().length() > 0) {
          if(t.getSecond() != STRING_USE_DEFAULT && t.getSecond() != STRING_OPTIONAL) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.getFirst().getOptionID().getName());
            p.add(t.getSecond());
          }
        }
        else if(t.getFirst() instanceof Flag) {
          if(t.getSecond() == Flag.SET) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.getFirst().getOptionID().getName());
          }
        }
      }
    }
    return p;
  }
}