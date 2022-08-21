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
package elki.outlier.clustering;

import java.util.List;

import elki.clustering.kmeans.ExponionKMeans;
import elki.clustering.kmeans.KMeans;
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
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SparseSquaredEuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection by using k-means clustering.
 * <p>
 * The scores are assigned by the objects distance to the nearest center.
 * <p>
 * We do not have a clear reference for this approach, but it seems to be a best
 * practice in some areas to remove objects that have the largest distance from
 * their center. This can for example be found mentioned in the book of Han,
 * Kamber and Pei, but our implementation goes beyond their approach when it
 * comes to handling singleton objects (that are a cluster of their own). To
 * cite this approach, please cite the ELKI version you used (use the
 * <a href="https://elki-project.github.io/publications">ELKI publication
 * list</a> for citation information and BibTeX templates).
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
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KMeansOutlierDetection.class);

  /**
   * Outlier scoring rule
   * 
   * @author Erich Schubert
   */
  public static enum Rule {
    /** Simple distance-based rule */
    DISTANCE,
    /** Distance with singletons */
    DISTANCE_SINGLETONS,
    /** Variance change */
    VARIANCE
  }

  /**
   * K-Means clustering algorithm to use
   */
  KMeans<O, ?> clusterer;

  /**
   * Outlier scoring rule
   */
  Rule rule;

  /**
   * Constructor.
   *
   * @param clusterer Clustering algorithm
   * @param rule Decision rule
   */
  public KMeansOutlierDetection(KMeans<O, ?> clusterer, Rule rule) {
    super();
    this.clusterer = clusterer;
    this.rule = rule;
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
    NumberVectorDistance<? super O> distfunc = clusterer.getDistance();
    if(rule == Rule.VARIANCE && !(distfunc instanceof SquaredEuclideanDistance || distfunc instanceof SparseSquaredEuclideanDistance)) {
      LOG.warning("K-means should be used with squared Euclidean distance only.");
    }
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();

    switch(rule){
    case DISTANCE:
      distanceScoring(c, relation, distfunc, scores, mm);
      break;
    case DISTANCE_SINGLETONS:
      singletonsScoring(c, relation, distfunc, scores, mm);
      break;
    case VARIANCE:
      varianceScoring(c, relation, distfunc, scores, mm);
      break;
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("KMeans outlier scores", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Simple distance-based scoring function.
   *
   * @param c Clustering
   * @param relation data relation
   * @param distfunc Distance function
   * @param scores Scores output
   * @param mm Minimum and maximum
   */
  private void distanceScoring(Clustering<?> c, Relation<O> relation, NumberVectorDistance<? super O> distfunc, WritableDoubleDataStore scores, DoubleMinMax mm) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      NumberVector mean = ModelUtil.getPrototype(cluster.getModel(), relation);
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        final O obj = relation.get(iter);
        double score = Double.NaN;
        // distance to the cluster's center:
        score = cluster.size() == 1 ? 0. : distfunc.distance(mean, obj);
        scores.put(iter, score);
        mm.put(score);
      }
    }
  }

  /**
   * Distance-based scoring that takes singletons into account.
   *
   * @param c Clustering
   * @param relation data relation
   * @param distfunc Distance function
   * @param scores Scores output
   * @param mm Minimum and maximum
   */
  private void singletonsScoring(Clustering<?> c, Relation<O> relation, NumberVectorDistance<? super O> distfunc, WritableDoubleDataStore scores, DoubleMinMax mm) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      NumberVector mean = ModelUtil.getPrototype(cluster.getModel(), relation);
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        final O obj = relation.get(iter);
        double score = Double.NaN;
        if(cluster.size() == 1 && clusters.size() > 1) {
          score = Double.POSITIVE_INFINITY;
          for(Cluster<?> c2 : clusters) {
            double dist = distfunc.distance(ModelUtil.getPrototype(c2.getModel(), relation), obj);
            score = dist < score ? dist : score;
          }
        }
        else {
          score = distfunc.distance(mean, obj);
        }
        scores.put(iter, score);
        mm.put(score);
      }
    }
  }

  /**
   * Variance-based scoring function.
   *
   * @param c Clustering
   * @param relation data relation
   * @param distfunc Distance function
   * @param scores Scores output
   * @param mm Minimum and maximum
   */
  private void varianceScoring(Clustering<?> c, Relation<O> relation, NumberVectorDistance<? super O> distfunc, WritableDoubleDataStore scores, DoubleMinMax mm) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      NumberVector mean = ModelUtil.getPrototype(cluster.getModel(), relation);
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        final O obj = relation.get(iter);
        double score = Double.NaN;
        if(cluster.size() == 1 && clusters.size() > 1) {
          score = Double.POSITIVE_INFINITY;
          for(Cluster<?> c2 : clusters) {
            double dist = distfunc.distance(ModelUtil.getPrototype(c2.getModel(), relation), obj);
            dist = dist * c2.size() / (c2.size() + 1);
            score = dist < score ? dist : score;
          }
        }
        else {
          score = distfunc.distance(mean, obj) * cluster.size() / (cluster.size() - 1);
        }
        scores.put(iter, score);
        mm.put(score);
      }
    }
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
     * Parameter for choosing the scoring rule.
     */
    public static final OptionID RULE_ID = new OptionID("kmeansod.scoring", //
        "Scoring rule for scoring outliers.");

    /**
     * Clustering algorithm to use
     */
    KMeans<O, ?> clusterer;

    /**
     * Outlier scorig rule
     */
    Rule rule;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMeans<O, ?>>(CLUSTERING_ID, KMeans.class, ExponionKMeans.class) //
          .grab(config, x -> clusterer = x);
      new EnumParameter<Rule>(RULE_ID, Rule.class, Rule.VARIANCE) //
          .grab(config, x -> rule = x);
    }

    @Override
    public KMeansOutlierDetection<O> make() {
      return new KMeansOutlierDetection<>(clusterer, rule);
    }
  }
}
