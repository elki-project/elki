package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
  public Parameterization descend(@SuppressWarnings("unused") Object option) {
    return this;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Pair<OptionID, Object> pair : parameters) {
      buf.append("-").append(pair.getFirst().toString()).append(" ");
      buf.append(pair.getSecond().toString()).append(" ");
    }
    return buf.toString();
  }
}