package de.lmu.ifi.dbs.elki.gui.util;

import java.util.ArrayList;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Wrapper around a set of parameters for ELKI, that may not yet be complete or
 * correct.
 * 
 * @author Erich Schubert
 */
public class DynamicParameters {
  /**
   * Bit for an option that should be set
   */
  public static final int BIT_INCOMPLETE = 0;

  /**
   * Bit for an option with an invalid value
   */
  public static final int BIT_INVALID = 1;

  /**
   * Bit for an option containing an syntax error
   */
  public static final int BIT_SYNTAX_ERROR = 2;

  /**
   * Bit for an optional value
   */
  public static final int BIT_OPTIONAL = 3;

  /**
   * Bit for an option with a default value
   */
  public static final int BIT_DEFAULT_VALUE = 4;

  /**
   * Pseudo-value used in dropdowns for options that have a default value
   */
  public static final String STRING_USE_DEFAULT = "(use default)";

  /**
   * Pseudo-value used in options that are optional, to unset.
   */
  public static final String STRING_OPTIONAL = "(optional)";

  /**
   * Node in the option tree (well, actually list)
   * 
   * @author Erich Schubert
   */
  public class Node {
    protected Parameter<?, ?> param;

    protected String value;

    protected BitSet flags;

    protected int depth;

    /**
     * Constructor.
     * 
     * @param param Parameter
     * @param value Value
     * @param flags Flags
     * @param depth Depth (for tree representation)
     */
    public Node(Parameter<?, ?> param, String value, BitSet flags, int depth) {
      super();
      this.param = param;
      this.value = value;
      this.flags = flags;
      this.depth = depth;
    }
  }

  /**
   * Parameter storage
   */
  protected ArrayList<Node> parameters;

  /**
   * Constructor
   */
  public DynamicParameters() {
    super();
    this.parameters = new ArrayList<Node>();
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
   * Update the Parameter list from the collected options of an ELKI context
   * 
   * @param track Tracked Parameters
   */
  public synchronized void updateFromTrackParameters(TrackParameters track) {
    parameters.clear();
    for(Pair<Object, Parameter<?, ?>> p : track.getAllParameters()) {
      Parameter<?, ?> option = p.getSecond();
      String value = null;
      if(option.isDefined() && !option.tookDefaultValue()) {
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
      int depth = 0;
      {
        Object pos = track.getParent(option);
        while(pos != null) {
          pos = track.getParent(pos);
          depth += 1;
          if(depth > 10) {
            break;
          }
        }
      }
      Node t = new Node(option, value, bits, depth);
      parameters.add(t);
    }
  }

  /**
   * Add a single parameter to the list
   * 
   * @param option Option
   * @param value Value
   * @param bits Bits
   * @param depth Depth
   */
  public synchronized void addParameter(Parameter<?, ?> option, String value, BitSet bits, int depth) {
    Node t = new Node(option, value, bits, depth);
    parameters.add(t);
  }

  /**
   * Serialize parameters into an array list to pass to setParameters()
   * 
   * @return Array list of String parameters.
   */
  public synchronized ArrayList<String> serializeParameters() {
    ArrayList<String> p = new ArrayList<String>(2 * parameters.size());
    for(Node t : parameters) {
      if(t.param != null) {
        if(t.param instanceof RemainingOptions) {
          for (String str : t.value.split(" ")) {
            if (str.length() > 0) {
              p.add(str);
            }
          }
        }
        else if(t.param instanceof Flag) {
          if(t.value == Flag.SET) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.param.getOptionID().getName());
          }
        }
        else if(t.value != null && t.value.length() > 0) {
          if(t.value != STRING_USE_DEFAULT && t.value != STRING_OPTIONAL) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.param.getOptionID().getName());
            p.add(t.value);
          }
        }
      }
    }
    return p;
  }

  /**
   * Get the node in this nth row of the flattened tree.
   * 
   * @param rowIndex row index
   * @return tree node
   */
  public Node getNode(int rowIndex) {
    return this.parameters.get(rowIndex);
  }

  /**
   * OptionID for unrecognized options.
   */
  protected static OptionID REMAINING_OPTIONS_ID = OptionID.getOrCreateOptionID("UNUSED", "Unrecognized options.");

  /**
   * Dummy option class that represents unhandled options
   * 
   * @author Erich Schubert
   */
  public static class RemainingOptions extends StringParameter {
    /**
     * Constructor.
     */
    public RemainingOptions() {
      super(REMAINING_OPTIONS_ID);
      super.setOptional(true);
    }
  }
}