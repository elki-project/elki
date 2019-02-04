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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Manage a parameterization serialized as String array, e.g. from command line.
 *
 * When building parameter lists, use {@link ListParameterization} where
 * possible.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class SerializedParameterization extends AbstractParameterization {
  /**
   * Prefix of option markers on the command line.
   *
   * The option markers are supposed to be given on the command line with
   * leading <code>-</code>.
   */
  public static final String OPTION_PREFIX = "-";

  /**
   * Parameter storage
   */
  LinkedList<String> parameters = null;

  /**
   * Constructor
   */
  public SerializedParameterization() {
    super();
    parameters = new LinkedList<>();
  }

  /**
   * Constructor
   *
   * @param args Parameters
   */
  public SerializedParameterization(String[] args) {
    this();
    for(String arg : args) {
      parameters.add(arg);
    }
  }

  /**
   * Constructor
   *
   * @param args Parameter list
   */
  public SerializedParameterization(List<String> args) {
    this();
    parameters.addAll(args);
  }

  /**
   * Return the yet unused parameters.
   *
   * @return Unused parameters.
   */
  public List<String> getRemainingParameters() {
    return parameters;
  }

  @Override
  public boolean hasUnusedParameters() {
    return !parameters.isEmpty();
  }

  /**
   * Log a warning if there were unused parameters.
   */
  public void logUnusedParameters() {
    if(hasUnusedParameters()) {
      LoggingUtil.warning("The following parameters were not processed: " + parameters);
    }
  }

  @Override
  public boolean setValueForOption(Parameter<?> opt) throws ParameterException {
    Iterator<String> piter = parameters.iterator();
    while(piter.hasNext()) {
      String cur = piter.next();
      if(!cur.startsWith(OPTION_PREFIX)) {
        continue;
        // throw new NoParameterValueException(cur + " is no parameter!");
      }

      if(opt.getOptionID().getName().regionMatches(0, cur, 1, Math.max(opt.getOptionID().getName().length(), cur.length() - 1))) {
        // Consume.
        piter.remove();
        // check if the option is a parameter or a flag
        if(opt instanceof Flag) {
          String set = Flag.SET;
          // The next element must be a parameter
          if(piter.hasNext()) {
            String next = piter.next();
            if(Flag.SET.equals(next)) {
              set = Flag.SET;
              piter.remove();
            }
            else if(Flag.NOT_SET.equals(next)) {
              set = Flag.NOT_SET;
              piter.remove();
            }
            else if(!next.startsWith(OPTION_PREFIX)) {
              throw new WrongParameterValueException(opt, next, "invalid value for a boolean flag.");
            }
            // We do not consume the next if it's not for us ...
          }
          // set the Flag
          opt.setValue(set);
          return true;
        }
        else {
          // Ensure there is a potential value for this parameter
          if(!piter.hasNext()) {
            throw new UnspecifiedParameterException(opt);
          }
          opt.setValue(piter.next());
          // Consume parameter
          piter.remove();
          // Success - return.
          return true;
        }
      }
    }
    return false;
  }
}
