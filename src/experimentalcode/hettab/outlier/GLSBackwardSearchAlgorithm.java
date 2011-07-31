package experimentalcode.hettab.outlier;

import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * FIXME: Documentation, Reference
 * 
 * GLSBackwardSearchAlgorithm provides the GLS-SOD Algorithm, an Algorithm to
 * detect Spatial Outlier
 * 
 * @author Ahmed Hettab
 * 
 * @param <V> DatabaseObject to use
 * @param <D> Distance function to use
 */
public class GLSBackwardSearchAlgorithm<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GLSBackwardSearchAlgorithm.class);

  /**
   * Holds the alpha value
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("glsbs.alpha", "the alpha parameter");

  /**
   * parameter x2
   */
  private double alpha;

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("glsbs.k", "k nearest neighbor");

  /**
   * The parameter k
   */
  private int k;

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID M_ID = OptionID.getOrCreateOptionID("glsbs.m", "the number of outliers to be detected");

  /**
   * The parameter k
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), new VectorFieldTypeInformation<NumberVector<?, ?>>(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation, Relation<? extends NumberVector<?, ?>> relationy) {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    DoubleMinMax mm = new DoubleMinMax(0.0, 0.0);

    // Outlier detection loop
    {
      ModifiableDBIDs idview = DBIDUtil.newHashSet(relation.getDBIDs());
      ProxyView<V> proxy = new ProxyView<V>(database, idview, relation);

      // Detect up to m outliers
      for(int numout = 0; numout < m; numout++) {
        Pair<DBID, Double> candidate = getCandidate(proxy, relationy);
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
   * 
   * @param database
   * @param outlier
   */
  // TODO test
  private Pair<DBID, Double> getCandidate(Relation<V> relation, Relation<? extends NumberVector<?, ?>> relationy) {
    final int dim = DatabaseUtil.dimensionality(relation);
    assert(dim == 2);
    assert(DatabaseUtil.dimensionality(relationy) == 1);
    KNNQuery<V, D> knnQuery = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k + 1);

    // We need stable indexed DBIDs
    ArrayDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    // Sort, so we can do a binary search below.
    Collections.sort(ids);

    // init F,X,Z
    Matrix X = new Matrix(ids.size(), 6);
    Matrix F = new Matrix(ids.size(), ids.size());
    Matrix Y = new Matrix(ids.size(), 1);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);

      // Fill the data matrix
      {
        V vec = relation.get(id);
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
        double idy = relationy.get(id).doubleValue(1);
        Y.set(i, 0, idy);
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
    final double sigma_sum_square = sigmaMat.normF() / (relation.size() - 6 - 1);
    final double norm = 1 / Math.sqrt(sigma_sum_square);

    // calculate the absolute values of standard residuals
    Matrix E = F.times(Y.minus(X.times(b))).timesEquals(norm);

    DBID worstid = null;
    double worstscore = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      double err = Math.abs(E.get(i, 0));
      if(err > worstscore) {
        worstscore = err;
        worstid = id;
      }
    }

    return new Pair<DBID, Double>(worstid, worstscore);
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @param <V> Input vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    private double alpha;

    private int k;

    private int m;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      getParameterAlpha(config);
      getParameterK(config);
      getParameterM(config);
    }

    @Override
    protected Object makeInstance() {
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