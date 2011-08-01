package experimentalcode.hettab.outlier;

import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * GLS-Backward Search is a statistical approach to detecting spatial outliers.
 * 
 * <p>
 * F. Chen and C.-T. Lu and A. P. Boedihardjo: <br>
 * GLS-SOD: A Generalized Local Statistical Approach for Spatial Outlier
 * Detection <br>
 * In Proc. 16th ACM SIGKDD international conference on Knowledge discovery and
 * data mining, 2010
 * </p>
 * 
 * Implementation note: this is just the most basic version of this algorithm.
 * The spatial relation must be two dimensional, the set of spatial basis
 * functions is hard-coded (but trivial to enhance) to {1,x,y,x*x,y*y,x*y}, and
 * we assume the neighborhood is large enough for the simpler formulas to work
 * that make the optimization problem convex.
 * 
 * @author Ahmed Hettab
 * 
 * @param <V> Vector type to use for distances
 * @param <D> Distance function to use
 */
@Reference(authors = "F. Chen and C.-T. Lu and A. P. Boedihardjo", title = "GLS-SOD: A Generalized Local Statistical Approach for Spatial Outlier Detection", booktitle = "Proc. 16th ACM SIGKDD international conference on Knowledge discovery and data mining")
public class GLSBackwardSearchAlgorithm<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GLSBackwardSearchAlgorithm.class);

  /**
   * Parameter Alpha - significance niveau
   */
  private double alpha;

  /**
   * Parameter k - neighborhood size
   */
  private int k;

  /**
   * Parameter m - number of outliers to detect
   */
  private int m;

  /**
   * Constructor
   * 
   * @param alpha
   * @param k
   */
  public GLSBackwardSearchAlgorithm(DistanceFunction<V, D> distanceFunction, int k, double alpha, int m) {
    super(distanceFunction);
    this.alpha = alpha;
    this.k = k;
    this.m = m;
  }

  /**
   * Run the algorithm
   * 
   * @param relation Neighborhood relation
   * @param relationy Dataset relation
   * @return Algorithm result
   */
  public OutlierResult run(Relation<V> relation, Relation<? extends NumberVector<?, ?>> relationy) {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    DoubleMinMax mm = new DoubleMinMax(0.0, 0.0);

    // Outlier detection loop
    {
      ModifiableDBIDs idview = DBIDUtil.newHashSet(relation.getDBIDs());
      ProxyView<V> proxy = new ProxyView<V>(relation.getDatabase(), idview, relation);

      // Detect up to m outliers
      for(int numout = 0; numout < m; numout++) {
        Pair<DBID, Double> candidate = singleIteration(proxy, relationy);
        if(candidate.second < alpha) {
          break;
        }
        scores.put(candidate.first, candidate.second);
        mm.put(candidate.second);
        idview.remove(candidate.first);
        // sanity check, in case proxyview changes behaviour
        assert (proxy.size() + numout + 1 == relation.size());
      }

      // Remaining objects are inliers
      for(DBID id : idview) {
        scores.put(id, 0.0);
      }
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("GLSSODBackward", "GLSSODbackward-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Run a single iteration of the GLS-SOD modeling step
   * 
   * @param relationx Geo relation
   * @param relationy Attribute relation
   * @return Top outlier and associated score
   */
  private Pair<DBID, Double> singleIteration(Relation<V> relationx, Relation<? extends NumberVector<?, ?>> relationy) {
    final int dim = DatabaseUtil.dimensionality(relationx);
    final int dimy = DatabaseUtil.dimensionality(relationy);
    assert (dim == 2);
    KNNQuery<V, D> knnQuery = QueryUtil.getKNNQuery(relationx, getDistanceFunction(), k + 1);

    // We need stable indexed DBIDs
    ArrayDBIDs ids = DBIDUtil.newArray(relationx.getDBIDs());
    // Sort, so we can do a binary search below.
    Collections.sort(ids);

    // init F,X,Z
    Matrix X = new Matrix(ids.size(), 6);
    Matrix F = new Matrix(ids.size(), ids.size());
    Matrix Y = new Matrix(ids.size(), dimy);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);

      // Fill the data matrix
      {
        V vec = relationx.get(id);
        double la = vec.doubleValue(1);
        double lo = vec.doubleValue(2);
        X.set(i, 0, 1.0);
        X.set(i, 1, la);
        X.set(i, 2, lo);
        X.set(i, 3, la * lo);
        X.set(i, 4, la * la);
        X.set(i, 5, lo * lo);
      }

      {
        for(int d = 0; d < dimy; d++) {
          double idy = relationy.get(id).doubleValue(d + 1);
          Y.set(i, d, idy);
        }
      }

      // Fill the neighborhood matrix F:
      {
        List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k + 1);
        ModifiableDBIDs neighborhood = DBIDUtil.newArray(neighbors.size());
        for(DistanceResultPair<D> dpair : neighbors) {
          if(id.equals(dpair.getDBID())) {
            continue;
          }
          neighborhood.add(dpair.getDBID());
        }
        // Weight object itself positively.
        F.set(i, i, 1.0);
        final int nweight = -1 / neighborhood.size();
        // We need to find the index positions of the neighbors, unfortunately.
        for(DBID nid : neighborhood) {
          int pos = Collections.binarySearch(ids, nid);
          assert (pos >= 0);
          F.set(pos, i, nweight);
        }
      }
    }
    // Estimate the parameter beta
    // Common term that we can save recomputing.
    Matrix common = X.transposeTimesTranspose(F).times(F);
    Matrix b = common.times(X).inverse().times(common.times(Y));
    // Estimate sigma_0 and sigma:
    // sigma_sum_square = sigma_0*sigma_0 + sigma*sigma
    Matrix sigmaMat = F.times(X.times(b).minus(F.times(Y)));
    final double sigma_sum_square = sigmaMat.normF() / (relationx.size() - 6 - 1);
    final double norm = 1 / Math.sqrt(sigma_sum_square);

    // calculate the absolute values of standard residuals
    Matrix E = F.times(Y.minus(X.times(b))).timesEquals(norm);

    DBID worstid = null;
    double worstscore = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      double err = E.getRowVector(i).euclideanLength();
      // double err = Math.abs(E.get(i, 0));
      if(err > worstscore) {
        worstscore = err;
        worstid = id;
      }
    }

    return new Pair<DBID, Double>(worstid, worstscore);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> Input vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Holds the alpha value - significance niveau
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("glsbs.alpha", "Significance niveau");

    /**
     * Parameter to specify the k nearest neighbors
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("glsbs.k", "k nearest neighbors to use");

    /**
     * Parameter to specify the number of outliers to detect
     */
    public static final OptionID M_ID = OptionID.getOrCreateOptionID("glsbs.m", "The number of outliers to be detected");

    /**
     * Parameter Alpha - significance niveau
     */
    private double alpha;

    /**
     * Parameter k - neighborhood size
     */
    private int k;

    /**
     * Parameter m - number of outliers to detect
     */
    private int m;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      getParameterAlpha(config);
      getParameterK(config);
      getParameterM(config);
    }

    @Override
    protected GLSBackwardSearchAlgorithm<V, D> makeInstance() {
      return new GLSBackwardSearchAlgorithm<V, D>(distanceFunction, k, alpha, m);
    }

    /**
     * Get the alpha parameter
     * 
     * @param config Parameterization
     */
    protected void getParameterAlpha(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(ALPHA_ID);
      if(config.grab(param)) {
        alpha = param.getValue();
      }
    }

    /**
     * Get the k parameter
     * 
     * @param config Parameterization
     * @return k parameter
     */
    protected void getParameterK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID);
      if(config.grab(param)) {
        k = param.getValue();
      }
    }

    /**
     * Get the m parameter
     * 
     * @param config Parameterization
     * @return m parameter
     */
    protected void getParameterM(Parameterization config) {
      final IntParameter param = new IntParameter(M_ID);
      if(config.grab(param)) {
        m = param.getValue();
      }
    }
  }
}