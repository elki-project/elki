package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
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
 * @apiviz.stereotype factory
 * @apiviz.uses RdKNNTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 */
public class RdKNNTreeFactory<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractRStarTreeFactory<O, RdKNNTree<O, D>> {
  /**
   * Parameter for k
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("rdknn.k", "positive integer specifying the maximal number k of reverse " + "k nearest neighbors to be supported.");

  /**
   * The default distance function.
   */
  public static final Class<?> DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class;

  /**
   * Parameter for distance function
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("rdknn.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialPrimitiveDistanceFunction<O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter Bulk loading strategy
   * @param insertionCandidates
   * @param k_max
   * @param distanceFunction
   */
  public RdKNNTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, int insertionCandidates, int k_max, SpatialPrimitiveDistanceFunction<O, D> distanceFunction) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates);
    this.k_max = k_max;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public RdKNNTree<O, D> instantiate(Relation<O> relation) {
    PageFile<RdKNNNode<D>> pagefile = makePageFile(getNodeClass());
    return new RdKNNTree<O, D>(relation, pagefile, bulkSplitter, insertionCandidates, k_max, distanceFunction, distanceFunction.instantiate(relation));
  }

  protected Class<RdKNNNode<D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(RdKNNNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>, D extends NumberDistance<D, ?>> extends AbstractRStarTreeFactory.Parameterizer<O> {
    /**
     * Parameter k.
     */
    protected int k_max = 0;

    /**
     * The distance function.
     */
    protected SpatialPrimitiveDistanceFunction<O, D> distanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(k_maxP)) {
        k_max = k_maxP.getValue();
      }

      ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>> distanceFunctionP = new ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, SpatialPrimitiveDistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
    }

    @Override
    protected RdKNNTreeFactory<O, D> makeInstance() {
      return new RdKNNTreeFactory<O, D>(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates, k_max, distanceFunction);
    }
  }
}