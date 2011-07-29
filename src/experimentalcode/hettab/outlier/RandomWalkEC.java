package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractDistanceBasedSpatialOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Spatial outlier detection based on random walks.
 * 
 * Note: this method can only handle one-dimensional data, but could probably be
 * easily extended to higher dimensional data by using an distance function
 * instead of the absolute difference.
 * 
 * <p>
 * X. Liu and C.-T. Lu and F. Chen, <br>
 * Random Walk on Exhaustive Combination <br>
 * Spatial outlier detection: random walk based approaches, <br>
 * in Proceedings of the 18th SIGSPATIAL International Conference on Advances in
 * Geographic Information Systems,2010
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N1> Spatial Vector type
 * @param <N2> Spatial Vector type
 * @param <D> Distance to use
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Spatial Outlier Detection using Random Walk on Exhaustive Combination")
@Reference(authors = "X. Liu and C.-T. Lu and F. Chen", title = "Spatial outlier detection: random walk based approaches", booktitle = "Proc. 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems, 2010")
public class RandomWalkEC<N1, N2, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier<N2, N1, D> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomWalkEC.class);

  /**
   * Parameter alpha: Attribute difference exponent
   */
  private double alpha;

  /**
   * Parameter c: damping factor
   */
  private double c;

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   * @param distanceFunction Distance function
   * @param alpha Alpha parameter
   * @param c C parameter
   */
  public RandomWalkEC(NeighborSetPredicate.Factory<N2> npredf, DistanceFunction<N1, D> distanceFunction, double alpha, double c) {
    super(npredf, distanceFunction);
    this.alpha = alpha;
    this.c = c;
  }

  /**
   * Run the algorithm
   * 
   * @param spatial1 Spatial neighborhood relation (for distance function)
   * @param spatial2 Spatial neighborhood relation (for neighborhood predicate)
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<N1> spatial1, Relation<N2> spatial2, Relation<? extends NumberVector<?, ?>> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial2);
    DistanceQuery<N1, D> distFunc = getNonSpatialDistanceFunction().instantiate(spatial1);
    WritableDataStore<Vector> similarityVectors = DataStoreUtil.makeStorage(spatial1.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);

    // Make a static IDs array for matrix column indexing
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    assert distFunc.getDistanceFunction().isSymmetric() : "The current implementation assumes a symmetric distance function!";
    // construct the relation Matrix of the ec-graph
    Matrix E = new Matrix(ids.size(), ids.size());
    for(int i = 0; i < ids.size(); i++) {
      final DBID id = ids.get(i);
      final double val = relation.get(id).doubleValue(1);
      for(int j = i + 1; j < ids.size(); j++) {
        final DBID n = ids.get(j);
        final double e;
        double dist = distFunc.distance(id, n).doubleValue();
        if(dist == 0) {
          logger.warning("Zero distances are not supported - skipping: " + id + " " + n);
          e = 0;
        }
        else {
          double diff = Math.abs(val - relation.get(n).doubleValue(1));
          double exp = Math.exp(Math.pow(diff, alpha));
          // Implementation note: not inverting exp worked a lot better.
          // Therefore we diverge from the article here.
          e = exp / dist;
        }
        // Exploit symmetry
        E.set(j, i, e);
        E.set(i, j, e);
      }
    }
    // normalize the adjacent Matrix
    // Sum based normalization - don't use E.normalizeColumns()
    // Which normalized to Euclidean length 1.0!
    // Also do the -c multiplication in this process.
    for(int i = 0; i < E.getColumnDimensionality(); i++) {
      double sum = 0.0;
      for(int j = 0; j < E.getRowDimensionality(); j++) {
        sum += E.get(j, i);
      }
      if(sum == 0) {
        sum = 1.0;
      }
      for(int j = 0; j < E.getRowDimensionality(); j++) {
        E.set(j, i, -c * E.get(j, i) / sum);
      }
    }
    // Add identity matrix. The diagonal should still be 0s, so this is trivial.
    assert (E.getRowDimensionality() == E.getColumnDimensionality());
    for(int col = 0; col < E.getColumnDimensionality(); col++) {
      assert (E.get(col, col) == 0.0);
      E.set(col, col, 1.0);
    }
    E = E.inverse().timesEquals(1 - c);

    // Split the matrix into columns
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      // Note: matrix times ith unit vector = ith column
      Vector sim = E.getColumnVector(i);
      similarityVectors.put(id, sim);
    }
    E = null;
    // compute the relevance scores between specified Object and its neighbors
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(spatial1.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      DBIDs neighbours = npred.getNeighborDBIDs(id);
      double gmean = 1.0;
      int cnt = 0;
      for(DBID n : neighbours) {
        if(id.equals(n)) {
          continue;
        }
        double sim = MathUtil.cosineSimilarity(similarityVectors.get(id), similarityVectors.get(n));
        gmean *= sim;
        cnt++;
      }
      final double score = Math.pow(gmean, 1.0 / cnt);
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("randomwalkec", "RandomWalkEC", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), getNonSpatialDistanceFunction().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N1> Vector type
   * @param <N2> Vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<N1, N2, D extends NumberDistance<D, ?>> extends AbstractNeighborhoodOutlier.Parameterizer<N2> {
    /**
     * Parameter to specify distance function
     */
    public static final OptionID DISTANCE_ID = OptionID.getOrCreateOptionID("randomwalkec.distance", "Distance function to use in computing the connectivity graph.");

    /**
     * Parameter to specify alpha
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("randomwalkec.alpha", "Scaling exponent for value differences.");

    /**
     * Parameter to specify the c
     */
    public static final OptionID C_ID = OptionID.getOrCreateOptionID("randomwalkec.c", "The damping parameter c.");

    /**
     * Parameter alpha: scaling
     */
    double alpha = 0.5;

    /**
     * Parameter c: damping coefficient
     */
    double c = 0.9;

    /**
     * Distance function to use
     */
    DistanceFunction<N1, D> distFunc;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configDistance(config);
      configAlpha(config);
      configC(config);
    }

    /**
     * Get the distance function parameter
     * 
     * @param config Parameterization
     */
    protected void configDistance(Parameterization config) {
      final ObjectParameter<DistanceFunction<N1, D>> param = new ObjectParameter<DistanceFunction<N1, D>>(DISTANCE_ID, DistanceFunction.class);
      if(config.grab(param)) {
        distFunc = param.instantiateClass(config);
      }
    }

    /**
     * Get the alpha parameter
     * 
     * @param config Parameterization
     */
    protected void configAlpha(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(ALPHA_ID, 0.5);
      if(config.grab(param)) {
        alpha = param.getValue();
      }
    }

    /**
     * get the c parameter
     * 
     * @param config
     */
    protected void configC(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(C_ID);
      if(config.grab(param)) {
        c = param.getValue();
      }
    }

    @Override
    protected RandomWalkEC<N1, N2, D> makeInstance() {
      return new RandomWalkEC<N1, N2, D>(npredf, distFunc, alpha, c);
    }
  }
}