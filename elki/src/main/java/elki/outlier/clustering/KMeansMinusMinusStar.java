/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2025
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
import java.util.Objects;

import elki.clustering.kmeans.KMeansMinusMinus;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * K-means--* outlier detection. This version uses the distance to the nearest
 * cluster center as outlier score, whereas the version in
 * {@link KMeansMinusMinusOutlierDetection} uses the binary labeling produced
 * from the classic {@link KMeansMinusMinus} algorithm.
 * <p>
 * Reference:
 * <p>
 * Braulio V. Sánchez Vinces, Erich Schubert, Arthur Zimek,
 * Robson L. F. Cordeiro.<br>
 * A comparative evaluation of clustering-based outlier detection<br>
 * Data Mining and Knowledge Discovery 39 (13), 2025.
 *
 * @author Braulio V.S. Vinces
 */
@Reference(authors = "Braulio V. Sánchez Vinces, Erich Schubert, Arthur Zimek, Robson L. F. Cordeiro", //
    title = "A comparative evaluation of clustering-based outlier detection", //
    booktitle = "Data Mining and Knowledge Discovery 39 (13)", //
    bibkey = "dblp:journals/datamine/VincesSZC25", //
    url = "https://doi.org/10.1007/s10618-024-01086-z")
public class KMeansMinusMinusStar<O extends NumberVector> implements OutlierAlgorithm {
  /**
   * Clustering algorithm to use
   */
  KMeansMinusMinus<O> clustering;

  /**
   * Constructor.
   *
   * @param clustering Clustering algorithm
   */
  public KMeansMinusMinusStar(KMeansMinusMinus<O> clustering) {
    super();
    this.clustering = clustering;
  }

  public OutlierResult run(Relation<O> relation) {
    Clustering<?> c = clustering.run(relation);
    DBIDs ids = relation.getDBIDs();

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();

    NumberVectorDistance<? super O> distfunc = clustering.getDistance();
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      if(cluster.isNoise()) {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          final O obj = relation.get(iter);
          double score = Double.POSITIVE_INFINITY;
          for(Cluster<?> c2 : clusters) {
            // avoid itself
            if(Objects.equals(cluster, c2)) {
              continue;
            }
            double dist = distfunc.distance(ModelUtil.getPrototype(c2.getModel(), relation), obj);
            score = Math.min(dist, score);
          }
          // distance to the nearest cluster's center:
          scores.put(iter, score);
          mm.put(score);
        }
      }
      else {
        NumberVector mean = ModelUtil.getPrototype(cluster.getModel(), relation);
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          // distance to the cluster's center
          double score = cluster.size() == 1 ? 0. : distfunc.distance(mean, relation.get(iter));
          scores.put(iter, score);
          mm.put(score);
        }
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("K-means--* outliers", ids, scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., 1., 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return clustering.getInputTypeRestriction();
  }

  /**
   * Parameterizer.
   *
   * @author Braulio V.S. Vinces
   */
  public static class Par<O extends NumberVector> implements Parameterizer {
    /**
     * Clustering algorithm to run.
     */
    KMeansMinusMinus<O> clustering;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Parameterization config) {
      ChainedParameterization list = new ChainedParameterization(new ListParameterization() //
          .addFlag(KMeansMinusMinus.Par.NOISE_FLAG_ID), config);
      list.errorsTo(config);
      clustering = list.tryInstantiate(KMeansMinusMinus.class);
    }

    @Override
    public KMeansMinusMinusStar<O> make() {
      return new KMeansMinusMinusStar<>(clustering);
    }
  }
}
