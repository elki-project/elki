package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Adapter from a normalized similarity function to a distance function.
 * 
 * Note: The derived distance function will usually not satisfy the triangle equations.
 *
 * @author Erich Schubert
 * @param <V> the type of FeatureVector to compute the distances of
 */
public abstract class SimilarityAdapterAbstract<V extends NumberVector<V,?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * OptionID for {@link #SIMILARITY_FUNCTION_PARAM}
   */
  public static final OptionID SIMILARITY_FUNCTION_ID = OptionID.getOrCreateOptionID(
      "adapter.similarityfunction",
      "Similarity function to derive the distance between database objects from."
  );

  /**
   * Parameter to specify the similarity function to derive the distance between database objects from.
   * Must extend {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction}.
   * <p>Key: {@code -adapter.similarityfunction} </p>
   * <p>Default value: {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction} </p>
   */
  protected final ClassParameter<NormalizedSimilarityFunction<V, DoubleDistance>> SIMILARITY_FUNCTION_PARAM =
      new ClassParameter<NormalizedSimilarityFunction<V, DoubleDistance>>(
          SIMILARITY_FUNCTION_ID,
          NormalizedSimilarityFunction.class,
          FractionalSharedNearestNeighborSimilarityFunction.class.getName());

  /**
   * Holds the similarity function.
   */
  protected NormalizedSimilarityFunction<V, DoubleDistance> similarityFunction;

  /**
   * Constructor
   */
  public SimilarityAdapterAbstract() {
    super();
    addOption(SIMILARITY_FUNCTION_PARAM);
  }

  /**
   * Distance implementation
   */
  public abstract DoubleDistance distance(V v1, V v2);

  @Override
  public String shortDescription() {
    return "SNN based distance for feature vectors.\n";
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    similarityFunction = SIMILARITY_FUNCTION_PARAM.instantiateClass();
    addParameterizable(similarityFunction);
    remainingParameters = similarityFunction.setParameters(remainingParameters);
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  @Override
  public void setDatabase(Database<V> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    similarityFunction.setDatabase(database, verbose, time);
  }
}
