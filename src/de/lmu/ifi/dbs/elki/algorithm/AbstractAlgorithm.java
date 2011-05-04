package de.lmu.ifi.dbs.elki.algorithm;

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
    final TypeInformation[] inputs = getInputTypeRestriction();
    final Object[] relations = new Object[inputs.length + 1];
    final Class<?>[] signature = new Class<?>[inputs.length + 1];
    // First parameter is the database
    relations[0] = database;
    signature[0] = Database.class;
    // Other parameters are the bound relations
    for(int i = 0; i < inputs.length; i++) {
      // FIXME: don't bind the same relation twice?
      relations[i + 1] = database.getRelation(inputs[i]);
      signature[i + 1] = Relation.class;
    }
    final Method runmethod;
    try {
      runmethod = this.getClass().getMethod("run", signature);
    }
    catch(Exception e) {
      throw new APIViolationException("Algorithm is missing a 'run' method matching its input signature.", e);
    }
    try {
      StringBuffer buf = new StringBuffer();
      for (Class<?> cls : signature) {
        buf.append(cls.toString()).append(",");
      }
      return (R) runmethod.invoke(this, relations);
    }
    catch(IllegalArgumentException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
    }
    catch(IllegalAccessException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
    }
    catch(InvocationTargetException e) {
      throw new APIViolationException("Invoking the real 'run' method failed.", e);
    }
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
  abstract protected Logging getLogger();

  /**
   * Make a default distance function configuration option
   * 
   * @param <F> Distance function type
   * @param defaultDistanceFunction Default value
   * @param restriction Restriction class
   * @return Parameter object
   */
  public static <F extends DistanceFunction<?, ?>> ObjectParameter<F> makeParameterDistanceFunction(Class<?> defaultDistanceFunction, Class<?> restriction) {
    return new ObjectParameter<F>(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, restriction, defaultDistanceFunction);
  }
}