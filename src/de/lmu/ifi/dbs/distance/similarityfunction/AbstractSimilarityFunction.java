package de.lmu.ifi.dbs.distance.similarityfunction;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.distance.Distance;

import java.util.regex.Pattern;

/**
 * AbstractSimilarityFunction provides some methods valid for any extending
 * class.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractSimilarityFunction<O extends DatabaseObject, D extends Distance> extends AbstractMeasurementFunction<O, D> implements SimilarityFunction<O, D> {
  /**
   * Provides an abstract SimilarityFunction based on the given pattern.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractSimilarityFunction(Pattern pattern) {
    super(pattern);
  }

  /**
   * Provides an abstract SimilarityFunction.
   * This constructor can be used if the required input pattern is
   * not yet known at instantiation time and will therefore be set later.
   */
  protected AbstractSimilarityFunction() {
    super();
  }

  /**
   * @see SimilarityFunction#similarity(Integer, Integer)
   */
  public final D similarity(Integer id1, Integer id2) {
    return similarity(getDatabase().get(id1), getDatabase().get(id2));
  }

  /**
   * @see SimilarityFunction#similarity(Integer, DatabaseObject)
   */
  public final D similarity(Integer id1, O o2) {
    return similarity(getDatabase().get(id1), o2);
  }

  /**
   * @see SimilarityFunction#isInfiniteSimilarity(Distance)
   */
  public final boolean isInfiniteSimilarity(D similarity) {
    return similarity.equals(infiniteSimilarity());
  }

  /**
   * @see SimilarityFunction#isNullSimilarity(Distance)
   */
  public final boolean isNullSimilarity(D similarity) {
    return similarity.equals(nullSimilarity());
  }

  /**
   * @see SimilarityFunction#isUndefinedSimilarity(Distance)
   */
  public final boolean isUndefinedSimilarity(D similarity) {
    return similarity.equals(undefinedSimilarity());
  }
}
