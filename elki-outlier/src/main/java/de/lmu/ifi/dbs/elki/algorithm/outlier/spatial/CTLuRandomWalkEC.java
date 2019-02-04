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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * Spatial outlier detection based on random walks.
 * <p>
 * Note: this method can only handle one-dimensional data, but could probably be
 * easily extended to higher dimensional data by using an distance function
 * instead of the absolute difference.
 * <p>
 * Reference:
 * <p>
 * X. Liu, C.-T. Lu, F. Chen<br>
 * Spatial outlier detection: random walk based approaches<br>
 * Proc. SIGSPATIAL Int. Conf. Advances in Geographic Information Systems
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <P> Spatial Vector type
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Spatial Outlier Detection using Random Walk on Exhaustive Combination")
@Reference(authors = "X. Liu, C.-T. Lu, F. Chen", //
    title = "Spatial outlier detection: random walk based approaches", //
    booktitle = "Proc. SIGSPATIAL Int. Conf. Advances in Geographic Information Systems", //
    url = "https://doi.org/10.1145/1869790.1869841", //
    bibkey = "DBLP:conf/gis/LiuLC10")
public class CTLuRandomWalkEC<P> extends AbstractDistanceBasedAlgorithm<P, OutlierResult> implements OutlierAlgorithm {
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(CTLuRandomWalkEC.class);

  /**
   * Parameter alpha: Attribute difference exponent.
   */
  private double alpha;

  /**
   * Parameter c: damping factor.
   */
  private double c;

  /**
   * Parameter k.
   */
  private int k;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param alpha Alpha parameter
   * @param c C parameter
   * @param k Number of neighbors
   */
  public CTLuRandomWalkEC(DistanceFunction<? super P> distanceFunction, double alpha, double c, int k) {
    super(distanceFunction);
    this.alpha = alpha;
    this.c = c;
    this.k = k;
  }

  /**
   * Run the algorithm.
   *
   * @param spatial Spatial neighborhood relation
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<P> spatial, Relation<? extends NumberVector> relation) {
    DistanceQuery<P> distFunc = getDistanceFunction().instantiate(spatial);
    WritableDataStore<double[]> similarityVectors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, double[].class);
    WritableDataStore<DBIDs> neighbors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, DBIDs.class);

    // Make a static IDs array for matrix column indexing
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // construct the relation Matrix of the ec-graph
    double[][] E = new double[ids.size()][ids.size()];
    KNNHeap heap = DBIDUtil.newHeap(k);
    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        final double val = relation.get(id).doubleValue(0);
        assert (heap.size() == 0);
        int j = 0;
        for(DBIDIter n = ids.iter(); n.valid(); n.advance(), j++) {
          if(i == j) {
            continue;
          }
          final double e;
          final double distance = distFunc.distance(id, n);
          heap.insert(distance, n);
          if(distance == 0) {
            LOG.warning("Zero distances are not supported - skipping: " + DBIDUtil.toString(id) + " " + DBIDUtil.toString(n));
            e = 0;
          }
          else {
            double diff = Math.abs(val - relation.get(n).doubleValue(0));
            double exp = FastMath.exp(FastMath.pow(diff, alpha));
            // Implementation note: not inverting exp worked a lot better.
            // Therefore we diverge from the article here.
            e = exp / distance;
          }
          E[j][i] = e;
        }
        // Convert kNN Heap into DBID array (unordered)
        ModifiableDBIDs nids = DBIDUtil.newArray(heap.size());
        for(DBIDIter it = heap.unorderedIterator(); it.valid(); it.advance()) {
          nids.add(it);
        }
        neighbors.put(id, nids);
      }
    }
    // normalize the adjacent Matrix
    // Sum based normalization - don't use E.normalizeColumns()
    // Which normalized to Euclidean length 1.0!
    // Also do the -c multiplication in this process.
    for(int i = 0; i < E[0].length; i++) {
      double sum = 0.0;
      for(int j = 0; j < E.length; j++) {
        sum += E[j][i];
      }
      if(sum == 0) {
        sum = 1.0;
      }
      for(int j = 0; j < E.length; j++) {
        E[j][i] = -c * E[j][i] / sum;
      }
    }
    // Add identity matrix. The diagonal should still be 0s, so this is trivial.
    assert (E.length == E[0].length);
    for(int col = 0; col < E[0].length; col++) {
      assert (E[col][col] == 0.0);
      E[col][col] = 1.0;
    }
    E = timesEquals(inverse(E), 1 - c);

    // Split the matrix into columns
    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        // Note: matrix times ith unit vector = ith column
        double[] sim = getCol(E, i);
        similarityVectors.put(id, sim);
      }
    }
    E = null;
    // compute the relevance scores between specified Object and its neighbors
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(spatial.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      double gmean = 1.0;
      int cnt = 0;
      for(DBIDIter iter = neighbors.get(id).iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(id, iter)) {
          continue;
        }
        double sim = angle(similarityVectors.get(id), similarityVectors.get(iter));
        gmean *= sim;
        cnt++;
      }
      final double score = FastMath.pow(gmean, 1.0 / cnt);
      minmax.put(score);
      scores.putDouble(id, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("randomwalkec", "RandomWalkEC", scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Ahmed Hettab
   *
   * @hidden
   *
   * @param <N> Vector type
   */
  public static class Parameterizer<N> extends AbstractDistanceBasedAlgorithm.Parameterizer<N> {
    /**
     * Parameter to specify the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("randomwalkec.k", "Number of nearest neighbors to use.");

    /**
     * Parameter to specify alpha.
     */
    public static final OptionID ALPHA_ID = new OptionID("randomwalkec.alpha", "Scaling exponent for value differences.");

    /**
     * Parameter to specify the c.
     */
    public static final OptionID C_ID = new OptionID("randomwalkec.c", "The damping parameter c.");

    /**
     * Parameter alpha: scaling.
     */
    double alpha = 0.5;

    /**
     * Parameter c: damping coefficient.
     */
    double c = 0.9;

    /**
     * Parameter for kNN.
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configK(config);
      configAlpha(config);
      configC(config);
    }

    /**
     * Get the kNN parameter.
     *
     * @param config Parameterization
     */
    protected void configK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(param)) {
        k = param.getValue();
      }
    }

    /**
     * Get the alpha parameter.
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
     * get the c parameter.
     *
     * @param config Parameterization
     */
    protected void configC(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(C_ID);
      if(config.grab(param)) {
        c = param.getValue();
      }
    }

    @Override
    protected CTLuRandomWalkEC<N> makeInstance() {
      return new CTLuRandomWalkEC<>(distanceFunction, alpha, c, k);
    }
  }
}
