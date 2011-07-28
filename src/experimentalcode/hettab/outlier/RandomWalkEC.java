package experimentalcode.hettab.outlier;

import java.util.Collection;

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
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
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
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import experimentalcode.hettab.neighborhood.DistanceBasedNeighborSetPredicate;

/**
 * <p>
 * Random Walk on Exhaustive Combination <br>
 * Liu, Xutong and Lu, Chang-Tien and Chen, Feng, <br>
 * Spatial outlier detection: random walk based approaches, <br>
 * in Proceedings of the 18th SIGSPATIAL International Conference on Advances in
 * Geographic Information Systems,2010
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector
 * @param<O> non Spatial Vector
 * @param <D> Distance to use
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Random Walk on Exhaustive Combination, which detect spatial Outlier")
@Reference(authors = "Liu, Xutong and Lu, Chang-Tien and Chen, Feng", title = "Spatial outlier detection: random walk based approaches", booktitle = "in Proceedings of the 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems,2010")
public class RandomWalkEC<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedNeighborhoodOutlier<N, D> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomWalkEC.class);

  /**
   * Parameter alpha
   */
  private double alpha;

  /**
   * parameter c
   */
  private double c;

  /**
   * Constructor
   * 
   * @param npredf
   * @param alpha
   * @param c
   */
  public RandomWalkEC(DistanceBasedNeighborSetPredicate.Factory<N, D> npredf, double alpha, double c) {
    super(npredf);
    this.alpha = alpha;
    this.c = c;
  }

  /**
   * Run the algorithm
   * 
   * @param spatial Spatial neighborhood relation
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<N> spatial, Relation<? extends NumberVector<?, ?>> relation) {
    final DistanceBasedNeighborSetPredicate<N, D> npred = getNeighborSetPredicateFactory().instantiate(spatial);
    PrimitiveDistanceFunction<N, D> distFunc = getNeighborSetPredicateFactory().getSpatialDistanceFunction();
    WritableDataStore<Vector> similarityVectors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);

    // Make a static IDs array for matrix column indexing
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // construct the relation Matrix of the ec-graph
    Matrix E = new Matrix(ids.size(), ids.size());
    for(int i = 0; i < ids.size(); i++) {
      final DBID id = ids.get(i);
      final double val = relation.get(id).doubleValue(1);
      for(int j = 0; j < ids.size(); j++) {
        final DBID n = ids.get(j);
        final double e;
        if(n.equals(id)) {
          e = 0;
        }
        else {
          double dist = distFunc.distance(spatial.get(id), spatial.get(n)).doubleValue();
          if(dist == 0) {
            e = 0;
          }
          else {
            double diff = Math.abs(val - relation.get(n).doubleValue(1));
            double exp = Math.exp(-Math.pow(diff, alpha));
            e = exp / dist;
          }
        }
        E.set(i, j, e);
      }
    }
    // normalize the adjacent Matrix
    // Sum based normalization - don't use E.normalizeColumns()
    // Which normalized to Euclidean length 1.0!
    // Also do the -c multiplication
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
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      Collection<DoubleObjPair<DBID>> neighbours = npred.getDistanceBasedNeighbors(id);
      double gmean = 1.0;
      int cnt = 0;
      for(DoubleObjPair<DBID> n : neighbours) {
        if(id.equals(n.second)) {
          continue;
        }
        double sim = cosineSimilarity(similarityVectors.get(id), similarityVectors.get(n.second));
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

  /**
   * Computes the cosine similarity for two given feature vectors.
   */
  private static final double cosineSimilarity(Vector v1, Vector v2) {
    return v1.scalarProduct(v2) / (v1.euclideanLength() * v2.euclideanLength());
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * 
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <V>
   * @param <D>
   */
  public static class Parameterizer<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedNeighborhoodOutlier.Parameterizer<N, D> {

    /**
     * Parameter to specify alpha
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("randomwalkec.alpha", "helps compute more accurate edge value");

    /**
     * Parameter to specify the c
     */
    public static final OptionID C_ID = OptionID.getOrCreateOptionID("randomwalkec.c", "the Parameter c");

    /**
     * 
     */
    double alpha = 0.5;

    /**
     * 
     */
    double c = 0.9;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configAlpha(config);
      configC(config);
    }

    /**
     * Get the alpha parameter
     * 
     * @param config Parameterization
     * @return alpha parameter
     */
    protected void configAlpha(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(ALPHA_ID);
      if(config.grab(param)) {
        alpha = param.getValue();
      }
    }

    /**
     * get the c parameter
     * 
     * @param config
     * @return
     */
    protected void configC(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(C_ID);
      if(config.grab(param)) {
        c = param.getValue();
      }
    }

    @Override
    protected RandomWalkEC<N, O, D> makeInstance() {
      return new RandomWalkEC<N, O, D>(npredf, alpha, c);
    }
  }
}