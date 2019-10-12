/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.outlier.spatial;

import static elki.math.linearalgebra.VMath.*;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.*;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.math.statistics.distribution.NormalDistribution;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.pairs.Pair;

import net.jafama.FastMath;

/**
 * GLS-Backward Search is a statistical approach to detecting spatial outliers.
 * <p>
 * Implementation note: this is just the most basic version of this algorithm.
 * The spatial relation must be two dimensional, the set of spatial basis
 * functions is hard-coded (but trivial to enhance) to \(\{1,x,y,x^2,y^2,xy\}\),
 * and we assume the neighborhood is large enough for the simpler formulas to
 * work that make the optimization problem convex.
 * <p>
 * Reference:
 * <p>
 * F. Chen, C.-T. Lu, A. P. Boedihardjo<br>
 * GLS-SOD: A Generalized Local Statistical Approach for Spatial Outlier
 * Detection<br>
 * Proc. 16th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <V> Vector type to use for distances
 */
@Title("GLS-Backward Search")
@Reference(authors = "F. Chen, C.-T. Lu, A. P. Boedihardjo", //
    title = "GLS-SOD: A Generalized Local Statistical Approach for Spatial Outlier Detection", //
    booktitle = "Proc. 16th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/1835804.1835939", //
    bibkey = "DBLP:conf/kdd/ChenLB10")
public class CTLuGLSBackwardSearchAlgorithm<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<Distance<? super V>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CTLuGLSBackwardSearchAlgorithm.class);

  /**
   * Parameter Alpha - significance niveau
   */
  private double alpha;

  /**
   * Parameter k - neighborhood size
   */
  private int k;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k number of nearest neighbors to use
   * @param alpha Significance niveau
   */
  public CTLuGLSBackwardSearchAlgorithm(Distance<? super V> distance, int k, double alpha) {
    super(distance);
    this.alpha = alpha;
    this.k = k;
  }

  /**
   * Run the algorithm
   *
   * @param relationx Spatial relation
   * @param relationy Attribute relation
   * @return Algorithm result
   */
  public OutlierResult run(Relation<V> relationx, Relation<? extends NumberVector> relationy) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relationx.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax mm = new DoubleMinMax(0.0, 0.0);

    // Outlier detection loop
    {
      ModifiableDBIDs idview = DBIDUtil.newHashSet(relationx.getDBIDs());
      ProxyView<V> proxy = new ProxyView<>(idview, relationx);

      double phialpha = NormalDistribution.standardNormalQuantile(1.0 - alpha * .5);
      // Detect outliers while significant.
      while(true) {
        Pair<DBIDVar, Double> candidate = singleIteration(proxy, relationy);
        if(candidate.second < phialpha) {
          break;
        }
        scores.putDouble(candidate.first, candidate.second);
        if(!Double.isNaN(candidate.second)) {
          mm.put(candidate.second);
        }
        idview.remove(candidate.first);
      }

      // Remaining objects are inliers
      for(DBIDIter iter = idview.iter(); iter.valid(); iter.advance()) {
        scores.putDouble(iter, 0.0);
      }
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("GLSSODBackward", relationx.getDBIDs(), scores);
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
  private Pair<DBIDVar, Double> singleIteration(Relation<V> relationx, Relation<? extends NumberVector> relationy) {
    final int dim = RelationUtil.dimensionality(relationx);
    final int dimy = RelationUtil.dimensionality(relationy);
    assert (dim == 2);
    KNNQuery<V> knnQuery = new QueryBuilder<>(relationx, distance).kNNQuery(k + 1);

    // We need stable indexed DBIDs
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relationx.getDBIDs());
    // Sort, so we can do a binary search below.
    ids.sort();

    // init F,X,Z
    double[][] X = new double[ids.size()][6];
    double[][] F = new double[ids.size()][ids.size()];
    double[][] Y = new double[ids.size()][dimy];

    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        // Fill the data matrix
        {
          V vec = relationx.get(id);
          double la = vec.doubleValue(0);
          double lo = vec.doubleValue(1);
          X[i][0] = 1.0;
          X[i][1] = la;
          X[i][2] = lo;
          X[i][3] = la * lo;
          X[i][4] = la * la;
          X[i][5] = lo * lo;
        }

        {
          final NumberVector vecy = relationy.get(id);
          for(int d = 0; d < dimy; d++) {
            double idy = vecy.doubleValue(d);
            Y[i][d] = idy;
          }
        }

        // Fill the neighborhood matrix F:
        {
          KNNList neighbors = knnQuery.getKNNForDBID(id, k + 1);
          ModifiableDBIDs neighborhood = DBIDUtil.newArray(neighbors.size());
          for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(DBIDUtil.equal(id, neighbor)) {
              continue;
            }
            neighborhood.add(neighbor);
          }
          // Weight object itself positively.
          F[i][i] = 1.0;
          final int nweight = -1 / neighborhood.size();
          // We need to find the index positions of the neighbors,
          // unfortunately.
          for(DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
            int pos = ids.binarySearch(iter);
            assert (pos >= 0);
            F[pos][i] = nweight;
          }
        }
      }
    }
    // Estimate the parameter beta
    // Common term that we can save recomputing.
    double[][] common = times(transposeTimesTranspose(X, F), F);
    double[][] b = times(inverse(times(common, X)), times(common, Y));
    // Estimate sigma_0 and sigma:
    // sigma_sum_square = sigma_0*sigma_0 + sigma*sigma
    double[][] sigmaMat = times(F, minusEquals(times(X, b), times(F, Y)));
    final double sigma_sum_square = normF(sigmaMat) / (relationx.size() - 6 - 1);
    final double norm = 1 / FastMath.sqrt(sigma_sum_square);

    // calculate the absolute values of standard residuals
    double[][] E = timesEquals(times(F, minus(Y, times(X, b))), norm);

    DBIDVar worstid = DBIDUtil.newVar();
    double worstscore = Double.NEGATIVE_INFINITY;
    int i = 0;
    for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
      double err = squareSum(getRow(E, i));
      // double err = Math.abs(E.get(i, 0));
      if(err > worstscore) {
        worstscore = err;
        worstid.set(id);
      }
    }

    return new Pair<>(worstid, FastMath.sqrt(worstscore));
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Input vector type
   */
  public static class Par<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super V>> {
    /**
     * Holds the alpha value - significance niveau
     */
    public static final OptionID ALPHA_ID = new OptionID("glsbs.alpha", "Significance niveau");

    /**
     * Parameter to specify the k nearest neighbors
     */
    public static final OptionID K_ID = new OptionID("glsbs.k", "k nearest neighbors to use");

    /**
     * Parameter Alpha - significance niveau
     */
    private double alpha;

    /**
     * Parameter k - neighborhood size
     */
    private int k;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      getParameterAlpha(config);
      getParameterK(config);
    }

    @Override
    public CTLuGLSBackwardSearchAlgorithm<V> make() {
      return new CTLuGLSBackwardSearchAlgorithm<>(distance, k, alpha);
    }

    /**
     * Get the alpha parameter
     *
     * @param config Parameterization
     */
    protected void getParameterAlpha(Parameterization config) {
      new DoubleParameter(ALPHA_ID) //
          .grab(config, x -> alpha = x);
    }

    /**
     * Get the k parameter
     *
     * @param config Parameterization
     */
    protected void getParameterK(Parameterization config) {
      new IntParameter(K_ID) //
          .grab(config, x -> k = x);
    }
  }
}
