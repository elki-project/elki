package de.lmu.ifi.dbs.elki.algorithm;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.APIViolationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * <p>
 * This class serves also as a model of implementing an algorithm within this
 * framework. Any Algorithm that makes use of these flags may extend this class.
 * </p>
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @apiviz.landmark
 * @apiviz.excludeSubtypes
 * 
 * @param <R> the result type
 */
public abstract class AbstractAlgorithm<R extends Result> implements Algorithm {
  /**
   * Constructor.
   */
  protected AbstractAlgorithm() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public R run(Database database) {
    final Object[] relations1;
    final Class<?>[] signature1;
    final Object[] relations2;
    final Class<?>[] signature2;
    // Build candidate method signatures
    {
      final TypeInformation[] inputs = getInputTypeRestriction();
      relations1 = new Object[inputs.length + 1];
      signature1 = new Class<?>[inputs.length + 1];
      relations2 = new Object[inputs.length];
      signature2 = new Class<?>[inputs.length];
      // First parameter is the database
      relations1[0] = database;
      signature1[0] = Database.class;
      // Other parameters are the bound relations
      for(int i = 0; i < inputs.length; i++) {
        // TODO: don't bind the same relation twice?
        // But sometimes this is wanted (e.g. using projected distances)
        relations1[i + 1] = database.getRelation(inputs[i]);
        signature1[i + 1] = Relation.class;
        relations2[i] = database.getRelation(inputs[i]);
        signature2[i] = Relation.class;
      }
    }

    // Find appropriate run method.
    try {
      Method runmethod1 = this.getClass().getMethod("run", signature1);
      return (R) runmethod1.invoke(this, relations1);
    }
    catch(NoSuchMethodException e) {
      // continue below.
    }
    catch(IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
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

    try {
      Method runmethod2 = this.getClass().getMethod("run", signature2);
      return (R) runmethod2.invoke(this, relations2);
    }
    catch(NoSuchMethodException e) {
      // continue below.
    }
    catch(IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
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
    throw new APIViolationException("No appropriate 'run' method found.");
  }

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  @Override
  public abstract TypeInformation[] getInputTypeRestriction();

  /**
   * Get the (STATIC) logger for this class.
   * 
   * @return the static logger
   */
  protected abstract Logging getLogger();

  /**
   * Make a default distance function configuration option.
   * 
   * @param <F> Distance function type
   * @param defaultDistanceFunction Default value
   * @param restriction Restriction class
   * @return Parameter object
   */
  public static <F extends DistanceFunction<?>> ObjectParameter<F> makeParameterDistanceFunction(Class<?> defaultDistanceFunction, Class<?> restriction) {
    return new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, restriction, defaultDistanceFunction);
  }
}