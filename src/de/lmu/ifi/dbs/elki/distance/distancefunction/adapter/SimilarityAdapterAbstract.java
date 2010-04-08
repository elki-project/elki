package de.lmu.ifi.dbs.elki.distance.distancefunction.adapter;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Adapter from a normalized similarity function to a distance function.
 * 
 * Note: The derived distance function will usually not satisfy the triangle
 * equation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> object class to process
 */
public abstract class SimilarityAdapterAbstract<O extends DatabaseObject> extends AbstractDistanceFunction<O, DoubleDistance> {
  /**
   * OptionID for {@link #SIMILARITY_FUNCTION_PARAM}
   */
  public static final OptionID SIMILARITY_FUNCTION_ID = OptionID.getOrCreateOptionID("adapter.similarityfunction", "Similarity function to derive the distance between database objects from.");

  /**
   * Parameter to specify the similarity function to derive the distance between
   * database objects from. Must extend
   * {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction}
   * .
   * <p>
   * Key: {@code -adapter.similarityfunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.similarityfunction.FractionalSharedNearestNeighborSimilarityFunction}
   * </p>
   */
  protected final ObjectParameter<NormalizedSimilarityFunction<O, DoubleDistance>> SIMILARITY_FUNCTION_PARAM = new ObjectParameter<NormalizedSimilarityFunction<O, DoubleDistance>>(SIMILARITY_FUNCTION_ID, NormalizedSimilarityFunction.class, FractionalSharedNearestNeighborSimilarityFunction.class);

  /**
   * Holds the similarity function.
   */
  protected NormalizedSimilarityFunction<O, DoubleDistance> similarityFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SimilarityAdapterAbstract(Parameterization config) {
    super(DoubleDistance.FACTORY);
    if(config.grab(SIMILARITY_FUNCTION_PARAM)) {
      similarityFunction = SIMILARITY_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Distance implementation
   */
  public abstract DoubleDistance distance(O v1, O v2);

  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    similarityFunction.setDatabase(database);
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return similarityFunction.getInputDatatype();
  }
}