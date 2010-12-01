package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Factory for RdKNN R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has RdKNNTree oneway - - produces
 * 
 * @param <O> Object type
 */
public class RdKNNTreeFactory<O extends NumberVector<O, ?>, D extends NumberDistance<D, N>, N extends Number> extends AbstractRStarTreeFactory<O, RdKNNTree<O, D, N>> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("rdknn.k", "positive integer specifying the maximal number k of reverse " + "k nearest neighbors to be supported.");

  /**
   * Parameter for k
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * The default distance function.
   */
  public static final Class<?> DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("rdknn.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter for distance function
   */
  private final ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, SpatialPrimitiveDistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialPrimitiveDistanceFunction<O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RdKNNTreeFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
    // logger.getWrappedLogger().setLevel(Level.OFF);

    // k_max
    if(config.grab(K_PARAM)) {
      k_max = K_PARAM.getValue();
    }
    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  @Override
  public RdKNNTree<O, D, N> instantiate(Database<O> database) {
    return new RdKNNTree<O, D, N>(database, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates, k_max, distanceFunction);
  }
}
