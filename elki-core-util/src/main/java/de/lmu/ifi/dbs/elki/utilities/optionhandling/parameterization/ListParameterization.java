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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.*;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler using a List and OptionIDs, for programmatic use.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class ListParameterization extends AbstractParameterization {
  /**
   * The actual parameters, for storage
   */
  List<ParameterPair> parameters;

  /**
   * Default constructor.
   */
  public ListParameterization() {
    super();
    this.parameters = new LinkedList<>();
  }

  /**
   * Constructor with an existing collection.
   *
   * @param dbParameters existing parameter collection
   */
  public ListParameterization(Collection<ParameterPair> dbParameters) {
    this();
    this.parameters = new LinkedList<>();
    for(ParameterPair pair : dbParameters) {
      if(pair.option instanceof OptionID) {
        addParameter((OptionID) pair.option, pair.value);
      }
      else if(pair.option instanceof String) {
        addParameter((String) pair.option, pair.value);
      }
      else {
        throw new IllegalStateException("The option field must only store OptionID or Strings.");
      }
    }
  }

  /**
   * Add a flag to the parameter list
   *
   * @param optionid Option ID
   * @return this, for chaining
   */
  public ListParameterization addFlag(OptionID optionid) {
    parameters.add(new ParameterPair(optionid, Flag.SET));
    return this;
  }

  /**
   * Add a flag to the parameter list
   *
   * @param optionid Option ID
   * @return this, for chaining
   */
  public ListParameterization addFlag(String optionid) {
    parameters.add(new ParameterPair(optionid, Flag.SET));
    return this;
  }

  /**
   * Add a parameter to the parameter list
   *
   * @param optionid Option ID
   * @param value Value
   * @return this, for chaining
   */
  public ListParameterization addParameter(OptionID optionid, Object value) {
    parameters.add(new ParameterPair(optionid, value));
    return this;
  }

  /**
   * Add a parameter to the parameter list
   *
   * @param pair Parameter pair
   * @return this, for chaining
   */
  protected ListParameterization addParameter(ParameterPair pair) {
    parameters.add(pair);
    return this;
  }

  /**
   * Add a parameter to the parameter list
   *
   * @param optionid Option ID
   * @param value Value
   * @return this, for chaining
   */
  public ListParameterization addParameter(String optionid, Object value) {
    optionid = optionid.startsWith(SerializedParameterization.OPTION_PREFIX) ? optionid.substring(SerializedParameterization.OPTION_PREFIX.length()) : optionid;
    parameters.add(new ParameterPair(optionid, value));
    return this;
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    Iterator<ParameterPair> iter = parameters.iterator();
    while(iter.hasNext()) {
      ParameterPair pair = iter.next();
      if(pair.option == opt.getOptionID() || (pair.option instanceof String && opt.getOptionID().getName().equals(pair.option))) {
        iter.remove();
        opt.setValue(pair.value);
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
  public List<ParameterPair> getRemainingParameters() {
    return parameters;
  }

  @Override
  public boolean hasUnusedParameters() {
    return !parameters.isEmpty();
  }

  @Override
  public ListParameterization descend(Object option) {
    return this;
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    for(ParameterPair pair : parameters) {
      buf.append(SerializedParameterization.OPTION_PREFIX).append(pair.option.toString()).append(' ')//
          .append(pair.value.toString()).append(' ');
    }
    return buf.toString();
  }

  /**
   * Serialize parameters.
   *
   * @return Array list of parameters
   */
  public ArrayList<String> serialize() {
    ArrayList<String> params = new ArrayList<>();
    for(ParameterPair pair : parameters) {
      params.add(SerializedParameterization.OPTION_PREFIX + pair.option.toString());
      if(pair.value instanceof String) {
        params.add((String) pair.value);
      }
      else if(pair.value instanceof Class) {
        params.add(((Class<?>) pair.value).getCanonicalName());
      }
      else { // Fallback:
        params.add(pair.value.toString());
      }
    }
    return params;
  }

  /**
   * Parameter pair, package-private.
   *
   * @author Erich Schubert
   */
  protected static final class ParameterPair {
    /**
     * Option key.
     */
    public Object option;

    /**
     * Option value.
     */
    public Object value;

    /**
     * Constructor.
     *
     * @param key Option key
     * @param value Option value
     */
    public ParameterPair(Object key, Object value) {
      this.option = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return SerializedParameterization.OPTION_PREFIX + option.toString() + " " + value;
    }
  }
}
