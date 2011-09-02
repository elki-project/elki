package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Global parameter constraint describing the dependency of a parameter (
 * {@link Parameter}) on a given flag ({@link Flag}). Depending on the status of
 * the flag the parameter is tested for keeping its constraints or not.
 * 
 * @author Steffi Wanka
 * @param <C> Constraint type
 * @param <S> Parameter type
 */
public class ParameterFlagGlobalConstraint<S, C extends S> implements GlobalParameterConstraint {
  /**
   * Parameter possibly to be checked.
   */
  private Parameter<S,C> param;

  /**
   * Flag the checking of the parameter constraints is dependent on.
   */
  private Flag flag;

  /**
   * Indicates at which status of the flag the parameter is to be checked.
   */
  private boolean flagConstraint;

  /**
   * List of parameter constraints.
   */
  private List<ParameterConstraint<S>> cons;

  /**
   * Constructs a global parameter constraint specifying that the testing of the
   * parameter given for keeping the parameter constraints given is dependent on
   * the status of the flag given.
   * 
   * @param p parameter possibly to be checked
   * @param c a list of parameter constraints, if the value is null, the parameter is just tested if it is set.
   * @param f flag controlling the checking of the parameter constraints
   * @param flagConstraint indicates at which status of the flag the parameter
   *        is to be checked
   */
  public ParameterFlagGlobalConstraint(Parameter<S, C> p, List<ParameterConstraint<S>> c, Flag f, boolean flagConstraint) {
    param = p;
    flag = f;
    this.flagConstraint = flagConstraint;
    cons = c;
  }

  /**
   * Checks the parameter for its parameter constraints dependent on the status
   * of the given flag. If a parameter constraint is breached a parameter
   * exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    // only check constraints of param if flag is set
    if(flagConstraint == flag.getValue()) {
      if(cons != null) {
        for(ParameterConstraint<? super C> c : cons) {
          c.test(param.getValue());
        }
      }
      else {
        if(!param.isDefined()) {
          throw new UnusedParameterException("Value of parameter " + param.getName() + " is not optional.");
        }
      }
    }
  }

  @Override
  public String getDescription() {
    StringBuffer description = new StringBuffer();
    if(flagConstraint) {
      description.append("If ").append(flag.getName());
      description.append(" is set, the following constraints for parameter ");
      description.append(param.getName()).append(" have to be fullfilled: ");
      if(cons != null) {
        for(int i = 0; i < cons.size(); i++) {
          ParameterConstraint<? super C> c = cons.get(i);
          if(i > 0) {
            description.append(", ");
          }
          description.append(c.getDescription(param.getName()));
        }
      }
      else {
        description.append(param.getName()+" must be set.");
      }
    }
    return description.toString();
  }
}