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
package elki.outlier.clustering;

import java.util.List;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection by using k-means clustering.
 * <p>
 * The scores are assigned by the objects distance to the nearest center.
 * <p>
 * We don't have a clear reference for this approach, but it seems to be a best
 * practise in some areas to remove objects that have the largest distance from
 * their center. If you need to cite this approach, please cite the ELKI version
 * you used (use the <a href="https://elki-project.github.io/publications">ELKI
 * publication list</a> for citation information and BibTeX templates).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - KMeans
 *
 * @param <O> Object type
 */
public class KMeansOutlierDetection<O extends NumberVector> implements OutlierAlgorithm {
  /**
   * Clustering algorithm to use
   */
  KMeans<O, ?> clusterer;

  /**
   * Constructor.
   *
   * @param clusterer Clustering algorithm
   */
  public KMeansOutlierDetection(KMeans<O, ?> clusterer) {
    super();
    this.clusterer = clusterer;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(clusterer.getDistance().getInputTypeRestriction());
  }

  /**
   * Run the outlier detection algorithm.
   *
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<O> relation) {
    Clustering<?> c = clusterer.run(relation);

    DistanceQuery<O> dq = new QueryBuilder<>(relation, clusterer.getDistance()).distanceQuery();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();

    @SuppressWarnings("unchecked")
    NumberVector.Factory<O> factory = (NumberVector.Factory<O>) RelationUtil.assumeVectorField(relation).getFactory();
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      // FIXME: use a primitive distance function on number vectors instead.
      O mean = factory.newNumberVector(ModelUtil.getPrototype(cluster.getModel(), relation));
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        double dist = dq.distance(mean, iter);
        scores.put(iter, dist);
        mm.put(dist);
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("KMeans outlier scores", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Parameterizer.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O extends NumberVector> implements Parameterizer {
    /**
     * Parameter for choosing the clustering algorithm.
     */
    public static final OptionID CLUSTERING_ID = new OptionID("kmeans.algorithm", //
        "Clustering algorithm to use for detecting outliers.");

    /**
     * Clustering algorithm to use
     */
    KMeans<O, ?> clusterer;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMeans<O, ?>>(CLUSTERING_ID, KMeans.class, LloydKMeans.class) //
          .grab(config, x -> clusterer = x);
    }

    @Override
    public KMeansOutlierDetection<O> make() {
      return new KMeansOutlierDetection<>(clusterer);
    }
  }
}
