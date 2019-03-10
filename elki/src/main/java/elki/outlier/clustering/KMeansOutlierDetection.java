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

import elki.AbstractAlgorithm;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.outlier.OutlierAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
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
public class KMeansOutlierDetection<O extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KMeansOutlierDetection.class);

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

  /**
   * Run the outlier detection algorithm.
   *
   * @param database Database
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    Distance<? super O> df = clusterer.getDistance();
    DistanceQuery<O> dq = database.getDistanceQuery(relation, df);

    // TODO: improve ELKI api to ensure we're using the same DBIDs!
    Clustering<?> c = clusterer.run(database, relation);

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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(clusterer.getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<KMeans<O, ?>> clusterP = new ObjectParameter<>(CLUSTERING_ID, KMeans.class, LloydKMeans.class);
      if(config.grab(clusterP)) {
        clusterer = clusterP.instantiateClass(config);
      }
    }

    @Override
    protected KMeansOutlierDetection<O> makeInstance() {
      return new KMeansOutlierDetection<>(clusterer);
    }
  }
}
