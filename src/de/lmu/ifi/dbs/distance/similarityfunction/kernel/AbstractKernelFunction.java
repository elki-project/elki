package de.lmu.ifi.dbs.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractDistanceFunction;

import java.util.regex.Pattern;

/**
 * AbstractKernelFunction provides some methods valid for any extending
 * class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractKernelFunction<O extends DatabaseObject, D extends Distance> extends AbstractDistanceFunction<O, D> implements KernelFunction<O, D> {
  /**
   * Provides an abstract KernelFunction based on the given pattern.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractKernelFunction(final Pattern pattern) {
    super(pattern);
  }

  /**
   * Provides an abstract KernelFunction.
   * This constructor can be used if the required input pattern is
   * not yet known at instantiation time and will therefore be set later.
   */
  protected AbstractKernelFunction() {
    super();
  }

  /**
   * @see de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction#similarity(Integer, Integer)
   */
  public final D similarity(Integer id1, Integer id2) {
    return similarity(getDatabase().get(id1), getDatabase().get(id2));
  }

  /**
   * @see de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction#similarity(Integer, DatabaseObject)
   */
  public final D similarity(Integer id1, O o2) {
    return similarity(getDatabase().get(id1), o2);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction#isInfiniteSimilarity(Distance)
   */
  public final boolean isInfiniteSimilarity(D similarity) {
    return similarity.equals(infiniteSimilarity());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction#isNullSimilarity(Distance)
   */
  public final boolean isNullSimilarity(D similarity) {
    return similarity.equals(nullSimilarity());
  }

  /**
   * @see de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction#isUndefinedSimilarity(Distance)
   */
  public final boolean isUndefinedSimilarity(D similarity) {
    return similarity.equals(undefinedSimilarity());
  }

}
