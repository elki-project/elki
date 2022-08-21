/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import elki.Algorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @param <O> object type
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Spatial Outlier Detection using Random Walk on Exhaustive Combination")
@Reference(authors = "X. Liu, C.-T. Lu, F. Chen", //
    title = "Spatial outlier detection: random walk based approaches", //
    booktitle = "Proc. SIGSPATIAL Int. Conf. Advances in Geographic Information Systems", //
    url = "https://doi.org/10.1145/1869790.1869841", //
    bibkey = "DBLP:conf/gis/LiuLC10")
public class CTLuRandomWalkEC<O> implements OutlierAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(CTLuRandomWalkEC.class);

  /**
   * Distance function used.
   */
  private Distance<? super O> distance;

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
   * @param distance Distance function
   * @param alpha Alpha parameter
   * @param c C parameter
   * @param k Number of neighbors
   */
  public CTLuRandomWalkEC(Distance<? super O> distance, double alpha, double c, int k) {
    super();
    this.distance = distance;
    this.alpha = alpha;
    this.c = c;
    this.k = k;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // FIXME: force relation 2 different from relation 1?
    return TypeUtil.array(distance.getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
  }

  /**
   * Run the algorithm.
   *
   * @param spatial Spatial neighborhood relation
   * @param relation Attribute value relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> spatial, Relation<? extends NumberVector> relation) {
    DistanceQuery<O> distFunc = new QueryBuilder<>(spatial, distance).distanceQuery();
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
          final double distance = distFunc.distance(id, n);
          heap.insert(distance, n);
          if(distance == 0) {
            LOG.warning("Zero distances are not supported - skipping: " + DBIDUtil.toString(id) + " " + DBIDUtil.toString(n));
            continue;
          }
          double diff = Math.abs(val - relation.get(n).doubleValue(0));
          double exp = FastMath.exp(FastMath.pow(diff, alpha));
          // Implementation note: not inverting exp worked a lot better.
          // Therefore we diverge from the article here.
          E[j][i] = exp / distance;
        }
        // Convert kNN Heap into DBID array (unordered)
        neighbors.put(id, heap.unorderedIterator().addTo(DBIDUtil.newArray(heap.size())));
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
      sum = sum != 0 ? sum : 1.0;
      for(int j = 0; j < E.length; j++) {
        E[j][i] *= -c / sum;
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
        similarityVectors.put(id, getCol(E, i));
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
        gmean *= angle(similarityVectors.get(id), similarityVectors.get(iter));
        cnt++;
      }
      final double score = cnt > 0 ? FastMath.pow(gmean, 1.0 / cnt) : 1;
      minmax.put(score);
      scores.putDouble(id, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("randomwalkec", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Parameterization class.
   *
   * @author Ahmed Hettab
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
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

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(ALPHA_ID, 0.5) //
          .grab(config, x -> alpha = x);
      new DoubleParameter(C_ID) //
          .grab(config, x -> c = x);
    }

    @Override
    public CTLuRandomWalkEC<O> make() {
      return new CTLuRandomWalkEC<>(distance, alpha, c, k);
    }
  }
}
