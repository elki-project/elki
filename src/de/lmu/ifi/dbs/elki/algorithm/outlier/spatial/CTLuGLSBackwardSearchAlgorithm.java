package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
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
@Title("GLS-Backward Search")
@Reference(authors = "F. Chen and C.-T. Lu and A. P. Boedihardjo", title = "GLS-SOD: A Generalized Local Statistical Approach for Spatial Outlier Detection", booktitle = "Proc. 16th ACM SIGKDD international conference on Knowledge discovery and data mining", url = "http://dx.doi.org/10.1145/1835804.1835939")
public class CTLuGLSBackwardSearchAlgorithm<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, OutlierResult> implements OutlierAlgorithm {
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
   * @param distanceFunction Distance function
   * @param k number of nearest neighbors to use
   * @param alpha Significance niveau
   */
  public CTLuGLSBackwardSearchAlgorithm(DistanceFunction<V, D> distanceFunction, int k, double alpha) {
    super(distanceFunction);
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
  public OutlierResult run(Relation<V> relationx, Relation<? extends NumberVector<?>> relationy) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relationx.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax mm = new DoubleMinMax(0.0, 0.0);

    // Outlier detection loop
    {
      ModifiableDBIDs idview = DBIDUtil.newHashSet(relationx.getDBIDs());
      ProxyView<V> proxy = new ProxyView<V>(relationx.getDatabase(), idview, relationx);

      double phialpha = NormalDistribution.standardNormalQuantile(1.0 - alpha *.5);
      // Detect outliers while significant.
      while(true) {
        Pair<DBID, Double> candidate = singleIteration(proxy, relationy);
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

    Relation<Double> scoreResult = new MaterializedRelation<Double>("GLSSODBackward", "GLSSODbackward-outlier", TypeUtil.DOUBLE, scores, relationx.getDBIDs());
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
  private Pair<DBID, Double> singleIteration(Relation<V> relationx, Relation<? extends NumberVector<?>> relationy) {
    final int dim = RelationUtil.dimensionality(relationx);
    final int dimy = RelationUtil.dimensionality(relationy);
    assert (dim == 2);
    KNNQuery<V, D> knnQuery = QueryUtil.getKNNQuery(relationx, getDistanceFunction(), k + 1);

    // We need stable indexed DBIDs
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relationx.getDBIDs());
    // Sort, so we can do a binary search below.
    ids.sort();

    // init F,X,Z
    Matrix X = new Matrix(ids.size(), 6);
    Matrix F = new Matrix(ids.size(), ids.size());
    Matrix Y = new Matrix(ids.size(), dimy);

    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        // Fill the data matrix
        {
          V vec = relationx.get(id);
          double la = vec.doubleValue(0);
          double lo = vec.doubleValue(1);
          X.set(i, 0, 1.0);
          X.set(i, 1, la);
          X.set(i, 2, lo);
          X.set(i, 3, la * lo);
          X.set(i, 4, la * la);
          X.set(i, 5, lo * lo);
        }

        {
          final NumberVector<?> vecy = relationy.get(id);
          for(int d = 0; d < dimy; d++) {
            double idy = vecy.doubleValue(d);
            Y.set(i, d, idy);
          }
        }

        // Fill the neighborhood matrix F:
        {
          KNNResult<D> neighbors = knnQuery.getKNNForDBID(id, k + 1);
          ModifiableDBIDs neighborhood = DBIDUtil.newArray(neighbors.size());
          for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(DBIDUtil.equal(id, neighbor)) {
              continue;
            }
            neighborhood.add(neighbor);
          }
          // Weight object itself positively.
          F.set(i, i, 1.0);
          final int nweight = -1 / neighborhood.size();
          // We need to find the index positions of the neighbors,
          // unfortunately.
          for(DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
            int pos = ids.binarySearch(iter);
            assert (pos >= 0);
            F.set(pos, i, nweight);
          }
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
    int i = 0;
    for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
      double err = E.getRow(i).euclideanLength();
      // double err = Math.abs(E.get(i, 0));
      if(err > worstscore) {
        worstscore = err;
        worstid = DBIDUtil.deref(id);
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
    return LOG;
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
  public static class Parameterizer<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Holds the alpha value - significance niveau
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("glsbs.alpha", "Significance niveau");

    /**
     * Parameter to specify the k nearest neighbors
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("glsbs.k", "k nearest neighbors to use");

    /**
     * Parameter Alpha - significance niveau
     */
    private double alpha;

    /**
     * Parameter k - neighborhood size
     */
    private int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      getParameterAlpha(config);
      getParameterK(config);
    }

    @Override
    protected CTLuGLSBackwardSearchAlgorithm<V, D> makeInstance() {
      return new CTLuGLSBackwardSearchAlgorithm<V, D>(distanceFunction, k, alpha);
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
     */
    protected void getParameterK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID);
      if(config.grab(param)) {
        k = param.getValue();
      }
    }
  }
}