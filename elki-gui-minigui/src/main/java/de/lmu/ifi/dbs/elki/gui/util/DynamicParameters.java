/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.gui.util;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Wrapper around a set of parameters for ELKI, that may not yet be complete or
 * correct.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @composed - - - de.lmu.ifi.dbs.elki.gui.util.DynamicParameters.RemainingOptions
 */
public class DynamicParameters {
  /**
   * Bit for an option that should be set
   */
  public static final int BIT_INCOMPLETE = 1;

  /**
   * Bit for an option with an invalid value
   */
  public static final int BIT_INVALID = 2;

  /**
   * Bit for an option containing an syntax error
   */
  public static final int BIT_SYNTAX_ERROR = 4;

  /**
   * Bit for an optional value
   */
  public static final int BIT_OPTIONAL = 8;

  /**
   * Bit for an option with a default value
   */
  public static final int BIT_DEFAULT_VALUE = 16;

  /**
   * Pseudo-value used in dropdowns for options that have a default value
   */
  public static final String STRING_USE_DEFAULT = "Default: ";

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
    protected Parameter<?> param;

    protected String value;

    protected int flags;

    protected int depth;

    /**
     * Constructor.
     * 
     * @param param Parameter
     * @param value Value
     * @param flags Flags
     * @param depth Depth (for tree representation)
     */
    public Node(Parameter<?> param, String value, int flags, int depth) {
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
    this.parameters = new ArrayList<>();
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
    for(TrackedParameter p : track.getAllParameters()) {
      Parameter<?> option = p.getParameter();
      String value = null;
      if(option.isDefined()) {
        if(option.tookDefaultValue()) {
          value = DynamicParameters.STRING_USE_DEFAULT + option.getDefaultValueAsString();
        }
        else {
          value = option.getValueAsString();
        }
      }
      if(value == null) {
        value = (option instanceof Flag) ? Flag.NOT_SET : "";
      }
      int bits = 0;
      if(option.isOptional()) {
        bits |= BIT_OPTIONAL;
      }
      if(option.hasDefaultValue() && option.tookDefaultValue()) {
        bits |= BIT_DEFAULT_VALUE;
      }
      if(value.length() <= 0) {
        if((bits & BIT_DEFAULT_VALUE) == 0 && (bits & BIT_OPTIONAL) == 0) {
          bits |= BIT_INCOMPLETE;
        }
      }
      else {
        try {
          if(!option.tookDefaultValue() && !option.isValid(value)) {
            bits |= BIT_INVALID;
          }
        }
        catch(ParameterException e) {
          bits |= BIT_INVALID;
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
      parameters.add(new Node(option, value, bits, depth));
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
  public synchronized void addParameter(Parameter<?> option, String value, int bits, int depth) {
    parameters.add(new Node(option, value, bits, depth));
  }

  /**
   * Serialize parameters into an array list to pass to setParameters()
   * 
   * @param p Output list (will not be emptied)
   */
  public synchronized void serializeParameters(ArrayList<String> p) {
    for(Node t : parameters) {
      if(t.param != null) {
        if(t.param instanceof RemainingOptions) {
          for(String str : t.value.split(" ")) {
            if(str.length() > 0) {
              p.add(str);
            }
          }
        }
        else if(t.param instanceof Flag) {
          if(Flag.SET.equals(t.value)) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.param.getOptionID().getName());
          }
        }
        else if(t.value != null && t.value.length() > 0) {
          if(!t.value.startsWith(STRING_USE_DEFAULT) && !STRING_OPTIONAL.equals(t.value)) {
            p.add(SerializedParameterization.OPTION_PREFIX + t.param.getOptionID().getName());
            p.add(t.value);
          }
        }
      }
    }
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
  protected static OptionID REMAINING_OPTIONS_ID = new OptionID("UNUSED", "Unrecognized options.");

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
      this.setOptional(true);
    }
  }
}
