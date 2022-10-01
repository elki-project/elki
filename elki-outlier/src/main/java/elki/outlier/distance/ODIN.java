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
package elki.outlier.distance;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection based on the in-degree of the kNN graph.
 * <p>
 * This is a curried version: instead of using a threshold T to obtain a binary
 * decision, we use the computed value as outlier score; normalized by k to
 * make the numbers more comparable across different parameterizations.
 * <p>
 * Reference:
 * <p>
 * V. Hautamäki and I. Kärkkäinen and P. Fränti<br>
 * Outlier detection using k-nearest neighbour graph<br>
 * Proc. 17th Int. Conf. Pattern Recognition (ICPR 2004)
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type
 */
@Title("ODIN: Outlier Detection Using k-Nearest Neighbour Graph")
@Reference(authors = "V. Hautamäki, I. Kärkkäinen, P. Fränti", //
    title = "Outlier detection using k-nearest neighbour graph", //
    booktitle = "Proc. 17th Int. Conf. Pattern Recognition (ICPR 2004)", //
    url = "https://doi.org/10.1109/ICPR.2004.1334558", //
    bibkey = "DBLP:conf/icpr/HautamakiKF04")
public class ODIN<O> implements OutlierAlgorithm {
  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of neighbors for kNN graph.
   */
  protected int kplus;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k k parameter
   */
  public ODIN(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // + query point
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the ODIN algorithm
   *
   * @param relation Relation to process.
   * @return ODIN outlier result.
   */
  public OutlierResult run(Relation<O> relation) {
    // Get the query functions:
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).kNNByDBID(kplus);

    // Get the objects to process, and a data storage for counting and output:
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB, 0.);

    double inc = 1. / (kplus - 1);
    DoubleMinMax minmax = new DoubleMinMax();
    // Process all objects
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      // Find the nearest neighbors (using an index, if available!)
      DBIDs neighbors = knnq.getKNN(iter, kplus);
      // For each neighbor, except ourselves, increase the in-degree:
      for(DBIDIter nei = neighbors.iter(); nei.valid(); nei.advance()) {
        if(DBIDUtil.equal(iter, nei)) {
          continue;
        }
        final double value = scores.doubleValue(nei) + inc;
        scores.put(nei, value);
        minmax.put(value);
      }
    }

    // Wrap the result and add metadata.
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., inc * (ids.size() - 1), 1);
    DoubleRelation rel = new MaterializedDoubleRelation("ODIN In-Degree", ids, scores);
    return new OutlierResult(meta, rel);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Number of nearest neighbors.
     */
    public static final OptionID K_ID = new OptionID("odin.k", "Number of neighbors to use for kNN graph.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Number of nearest neighbors to use.
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public ODIN<O> make() {
      return new ODIN<>(distance, k);
    }
  }
}
