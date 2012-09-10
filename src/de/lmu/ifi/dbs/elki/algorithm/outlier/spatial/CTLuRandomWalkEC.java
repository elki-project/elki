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
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Spatial outlier detection based on random walks.
 * 
 * Note: this method can only handle one-dimensional data, but could probably be
 * easily extended to higher dimensional data by using an distance function
 * instead of the absolute difference.
 * 
 * <p>
 * X. Liu and C.-T. Lu and F. Chen:<br>
 * Spatial outlier detection: random walk based approaches,<br>
 * in Proc. 18th SIGSPATIAL International Conference on Advances in Geographic
 * Information Systems, 2010
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector type
 * @param <D> Distance to use
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Spatial Outlier Detection using Random Walk on Exhaustive Combination")
@Reference(authors = "X. Liu and C.-T. Lu and F. Chen", title = "Spatial outlier detection: random walk based approaches", booktitle = "Proc. 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems, 2010", url = "http://dx.doi.org/10.1145/1869790.1869841")
public class CTLuRandomWalkEC<N, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<N, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(CTLuRandomWalkEC.class);

  /**
   * Parameter alpha: Attribute difference exponent
   */
  private double alpha;

  /**
   * Parameter c: damping factor
   */
  private double c;

  /**
   * Parameter k
   */
  private int k;

  /**
   * Constructor
   * 
   * @param distanceFunction Distance function
   * @param alpha Alpha parameter
   * @param c C parameter
   * @param k Number of neighbors
   */
  public CTLuRandomWalkEC(DistanceFunction<N, D> distanceFunction, double alpha, double c, int k) {
    super(distanceFunction);
    this.alpha = alpha;
    this.c = c;
    this.k = k;
  }

  /**
   * Run the algorithm
   * 
   * @param spatial Spatial neighborhood relation
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<N> spatial, Relation<? extends NumberVector<?>> relation) {
    DistanceQuery<N, D> distFunc = getDistanceFunction().instantiate(spatial);
    WritableDataStore<Vector> similarityVectors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);
    WritableDataStore<DBIDs> neighbors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, DBIDs.class);

    // Make a static IDs array for matrix column indexing
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());

    // construct the relation Matrix of the ec-graph
    Matrix E = new Matrix(ids.size(), ids.size());
    KNNHeap<D> heap = KNNUtil.newHeap(distFunc.getDistanceFactory(), k);
    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        final double val = relation.get(id).doubleValue(1);
        assert (heap.size() == 0);
        int j = 0;
        for(DBIDIter n = ids.iter(); n.valid(); n.advance(), j++) {
          if(i == j) {
            continue;
          }
          final double e;
          final D distance = distFunc.distance(id, n);
          heap.add(distance, n);
          double dist = distance.doubleValue();
          if(dist == 0) {
            LOG.warning("Zero distances are not supported - skipping: " + DBIDUtil.toString(id) + " " + DBIDUtil.toString(n));
            e = 0;
          }
          else {
            double diff = Math.abs(val - relation.get(n).doubleValue(1));
            double exp = Math.exp(Math.pow(diff, alpha));
            // Implementation note: not inverting exp worked a lot better.
            // Therefore we diverge from the article here.
            e = exp / dist;
          }
          E.set(j, i, e);
        }
        // Convert kNN Heap into DBID array
        ModifiableDBIDs nids = DBIDUtil.newArray(heap.size());
        while(heap.size() > 0) {
          nids.add(heap.poll());
        }
        neighbors.put(id, nids);
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
    {
      int i = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), i++) {
        // Note: matrix times ith unit vector = ith column
        Vector sim = E.getCol(i);
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
        double sim = MathUtil.angle(similarityVectors.get(id), similarityVectors.get(iter));
        gmean *= sim;
        cnt++;
      }
      final double score = Math.pow(gmean, 1.0 / cnt);
      minmax.put(score);
      scores.putDouble(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("randomwalkec", "RandomWalkEC", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
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
   * @apiviz.exclude
   * 
   * @param <N> Vector type
   * @param <D> Distance type
   */
  public static class Parameterizer<N, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<N, D> {
    /**
     * Parameter to specify the number of neighbors
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("randomwalkec.k", "Number of nearest neighbors to use.");

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
     * Parameter for kNN
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
     * Get the kNN parameter
     * 
     * @param config Parameterization
     */
    protected void configK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID, new GreaterEqualConstraint(1));
      if(config.grab(param)) {
        k = param.getValue();
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
    protected CTLuRandomWalkEC<N, D> makeInstance() {
      return new CTLuRandomWalkEC<N, D>(distanceFunction, alpha, c, k);
    }
  }
}