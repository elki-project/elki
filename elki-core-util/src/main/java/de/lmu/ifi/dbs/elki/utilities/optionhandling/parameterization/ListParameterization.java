package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
      addParameter(pair.option, pair.value);
    }
  }

  /**
   * Add a flag to the parameter list
   * 
   * @param optionid Option ID
   */
  public void addFlag(OptionID optionid) {
    parameters.add(new ParameterPair(optionid, Flag.SET));
  }

  /**
   * Add a parameter to the parameter list
   * 
   * @param optionid Option ID
   * @param value Value
   */
  public void addParameter(OptionID optionid, Object value) {
    parameters.add(new ParameterPair(optionid, value));
  }

  /**
   * Convenience - add a Flag option directly.
   * 
   * @param flag Flag to add, if set
   */
  public void forwardOption(Flag flag) {
    if(flag.isDefined() && flag.getValue().booleanValue()) {
      addFlag(flag.getOptionID());
    }
  }

  /**
   * Convenience - add a Parameter for forwarding
   * 
   * @param param Parameter to add
   */
  public void forwardOption(Parameter<?> param) {
    if(param.isDefined()) {
      addParameter(param.getOptionID(), param.getValue());
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    Iterator<ParameterPair> iter = parameters.iterator();
    while(iter.hasNext()) {
      ParameterPair pair = iter.next();
      if(pair.option == opt.getOptionID()) {
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
    return (parameters.size() > 0);
  }

  /**
   * {@inheritDoc} Default implementation, for flat parameterizations.
   */
  @Override
  public Parameterization descend(Object option) {
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    for(ParameterPair pair : parameters) {
      buf.append('-').append(pair.option.toString()).append(' ');
      buf.append(pair.value.toString()).append(' ');
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
      params.add("-" + pair.option.toString());
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
   *
   * @apiviz.exclude
   */
  static final class ParameterPair {
    /**
     * Option key.
     */
    public OptionID option;

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
    public ParameterPair(OptionID key, Object value) {
      this.option = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return "-" + option.getName() + " " + value;
    }
  }
}
