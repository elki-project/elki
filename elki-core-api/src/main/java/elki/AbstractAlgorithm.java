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
package elki;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.utilities.exceptions.APIViolationException;
import elki.utilities.optionhandling.OptionID;

/**
 * This class serves also as a model of implementing an algorithm within this
 * framework. Any Algorithm that makes use of these flags may extend this class.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @opt operations
 *
 * @param <R> the result type
 */
public abstract class AbstractAlgorithm<R> implements Algorithm {
  /**
   * Constructor.
   */
  protected AbstractAlgorithm() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public R run(Database database) {
    // Build candidate method signatures
    TypeInformation[] inputs = getInputTypeRestriction();
    Object[] relations = new Object[inputs.length + 1];
    Class<?>[] signature = new Class<?>[inputs.length + 1];
    // First parameter is the database
    relations[0] = database;
    signature[0] = Database.class;
    // Other parameters are the bound relations
    for(int i = 1; i <= inputs.length; i++) {
      // TODO: don't bind the same relation twice?
      // But sometimes this is wanted (e.g. using projected distances)
      relations[i] = database.getRelation(inputs[i - 1]);
      signature[i] = Relation.class;
    }

    try {
      try {
        return (R) this.getClass().getMethod("run", signature).invoke(this, relations);
      }
      catch(NoSuchMethodException e) {
        // continue below.
      }

      // Try without the Database argument
      signature = Arrays.copyOfRange(signature, 1, signature.length);
      relations = Arrays.copyOfRange(relations, 1, relations.length);
      try {
        return (R) this.getClass().getMethod("run", signature).invoke(this, relations);
      }
      catch(NoSuchMethodException e) {
        // continue below.
      }
      throw new APIViolationException("No appropriate 'run' method found.");
    }
    catch(InvocationTargetException e) {
      final Throwable cause = e.getTargetException();
      if(cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if(cause instanceof Error) {
        throw (Error) cause;
      }
      throw new APIViolationException("Invoking the real 'run' method failed: " + cause.toString(), cause);
    }
    catch(IllegalArgumentException | IllegalAccessException
        | SecurityException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
    }
  }

  @Override
  public abstract TypeInformation[] getInputTypeRestriction();

  /**
   * Get the (STATIC) logger for this class.
   *
   * @return the static logger
   */
  protected abstract Logging getLogger();

  /**
   * Parameter to specify the algorithm to run.
   */
  public static final OptionID ALGORITHM_ID = new OptionID("algorithm", "Algorithm to run.");

  /**
   * OptionID for the distance function.
   */
  public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");
}
