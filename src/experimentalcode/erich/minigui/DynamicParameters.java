package experimentalcode.erich.minigui;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

public class DynamicParameters {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ParametersModel.class);  

  public static final int BIT_INCOMPLETE = 0;
  public static final int BIT_INVALID = 1;
  public static final int BIT_SYNTAX_ERROR = 2;
  public static final int BIT_NO_NAME_BUT_VALUE = 3;
  public static final int BIT_OPTIONAL = 4;
  public static final int BIT_DEFAULT_VALUE = 5;
  
  public static final String STRING_USE_DEFAULT = "(use default)";
  public static final String STRING_OPTIONAL = "(optional)";

  protected ArrayList<Triple<Option<?>, String, BitSet>> parameters;

  public DynamicParameters() {
    super();
    this.parameters = new ArrayList<Triple<Option<?>, String, BitSet>>();
  }

  public int size() {
    return this.parameters.size();
  }

  public Option<?> getOption(int rowIndex) {
    return this.parameters.get(rowIndex).first;
  }
  
  public String getValue(int rowIndex) {
    return this.parameters.get(rowIndex).second;
  }
  
  public BitSet getFlags(int rowIndex) {
    return this.parameters.get(rowIndex).third;
  }

  public void setValue(int rowIndex, String s) {
    Triple<Option<?>, String, BitSet> p;
    if(rowIndex < parameters.size()) {
      p = parameters.get(rowIndex);
    }
    else {
      BitSet flags = new BitSet();
      p = new Triple<Option<?>, String, BitSet>(null, "", flags);
      parameters.add(p);
    }
    BitSet flags = p.getThird();
    
    p.setSecond(s);

    if(p.getFirst() instanceof Flag) {
      if((!Flag.SET.equals(s)) && (!Flag.NOT_SET.equals(s))) {
        flags.set(DynamicParameters.BIT_SYNTAX_ERROR);
      }
      else {
        flags.clear(DynamicParameters.BIT_SYNTAX_ERROR);
      }
    }
  }

  public void updateFromOptions(ArrayList<Pair<Parameterizable, Option<?>>> options) {
    parameters.clear();
    for(Pair<Parameterizable, Option<?>> p : options) {
      Option<?> option = p.getSecond();
      String value = option.getGivenValue();
      if(value == null) {
        if(option instanceof Flag) {
          value = Flag.NOT_SET;
        }
        else {
          value = "";
        }
      }
      BitSet bits = new BitSet();
      if(option instanceof Parameter<?, ?>) {
        Parameter<?, ?> par = (Parameter<?, ?>) option;
        if(par.isOptional()) {
          bits.set(BIT_OPTIONAL);
        }
        if(par.hasDefaultValue() && par.tookDefaultValue()) {
          bits.set(BIT_DEFAULT_VALUE);
        }
      }
      else if(option instanceof Flag) {
        bits.set(BIT_OPTIONAL);
      }
      else {
        logger.warning("Option is neither Parameter nor Flag!");
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
      Triple<Option<?>, String, BitSet> t = new Triple<Option<?>, String, BitSet>(option, value, bits);
      parameters.add(t);
    }
  }
  
  public ArrayList<String> serializeParameters() {
    ArrayList<String> p = new ArrayList<String>(2 * parameters.size());
    for(Triple<Option<?>, String, BitSet> t : parameters) {
      if(t.getFirst() != null) {
        if(t.getFirst() instanceof Parameter<?, ?> && t.getSecond() != null && t.getSecond().length() > 0) {
          if(t.getSecond() != STRING_USE_DEFAULT && t.getSecond() != STRING_OPTIONAL) {
            p.add("-" + t.getFirst().getOptionID().getName());
            p.add(t.getSecond());
          }
        }
        else if(t.getFirst() instanceof Flag) {
          if(t.getSecond() == Flag.SET) {
            p.add("-" + t.getFirst().getOptionID().getName());
          }
        }
      }
    }
    return p;
  }
}
