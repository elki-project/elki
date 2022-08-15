/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.outlier.clustering;

import java.util.List;

import elki.clustering.kmeans.KMeansMinusMinus;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * k-means--: A Unified Approach to Clustering and Outlier Detection.
 * <p>
 * This implementation assigns the outlier label to all points that were
 * identified as noise according to the {@code KMeans--} algorithm.
 * <p>
 * Reference:
 * <p>
 * S. Chawla, A. Gionis<br>
 * k-means--: A Unified Approach to Clustering and Outlier Detection<br>
 * Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)
 *
 * @author Braulio V.S. Vinces (ELKIfication)
 *
 * @has - - - KMeans
 *
 * @param <V> vector datatype
 */
@Title("K-Means--")
@Reference(authors = "S. Chawla, A. Gionis", //
    title = "k-means--: A Unified Approach to Clustering and Outlier Detection", //
    booktitle = "Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)", //
    url = "https://doi.org/10.1137/1.9781611972832.21", //
    bibkey = "DBLP:conf/sdm/ChawlaG13")
public class KMeansMinusMinusOutlierDetection<V extends NumberVector> implements OutlierAlgorithm {

  /**
   * Clustering algorithm to use
   */
  KMeansMinusMinus<V> clusterer;

  /**
   * Constructor.
   *
   * @param clusterer Clustering algorithm
   */
  public KMeansMinusMinusOutlierDetection(KMeansMinusMinus<V> clusterer) {
    super();
    this.clusterer = clusterer;
  }

  /**
   * Run the outlier detection algorithm.
   *
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<V> relation) {
    Clustering<?> c = clusterer.run(relation);

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    final double outlier = 1.;
    final double inlier = 0.;
    DoubleMinMax mm = new DoubleMinMax(inlier, outlier);

    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      if(cluster.isNoise()) {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, 1.);
        }
      }
      else {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, 0.);
        }
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("KMeans-- outlier scores", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    // TODO:
    // Metadata.hierarchyOf(result).addChild(c);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(clusterer.getDistance().getInputTypeRestriction());
  }

  /**
   * Parameterizer.
   *
   * @author Braulio V.S. Vinces
   *
   * @param <V> Object type
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Clustering algorithm to use
     */
    KMeansMinusMinus<V> clusterer;

    @Override
    public void configure(Parameterization config) {
      Class<KMeansMinusMinus<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(KMeansMinusMinus.class);
      clusterer = config.tryInstantiate(cls);
    }

    @Override
    public KMeansMinusMinusOutlierDetection<V> make() {
      return new KMeansMinusMinusOutlierDetection<>(clusterer);
    }
  }
}
