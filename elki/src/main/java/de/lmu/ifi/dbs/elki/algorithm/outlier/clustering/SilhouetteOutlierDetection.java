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
package de.lmu.ifi.dbs.elki.algorithm.outlier.clustering;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.clustering.internal.EvaluateSilhouette;
import de.lmu.ifi.dbs.elki.evaluation.clustering.internal.NoiseHandling;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Outlier detection by using the Silhouette Coefficients.
 * <p>
 * Silhouette values are computed as by Rousseeuw and then used as outlier
 * scores. To cite this outlier detection approach,
 * please cite the ELKI version you used (use the
 * <a href="https://elki-project.github.io/publications">ELKI publication
 * list</a>
 * for citation information and BibTeX templates).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - ClusteringAlgorithm
 *
 * @param <O> Object type
 */
@Reference(authors = "P. J. Rousseeuw", //
    title = "Silhouettes: A graphical aid to the interpretation and validation of cluster analysis", //
    booktitle = "Journal of Computational and Applied Mathematics, Volume 20", //
    url = "https://doi.org/10.1016/0377-0427(87)90125-7", //
    bibkey = "doi:10.1016/0377-04278790125-7")
public class SilhouetteOutlierDetection<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SilhouetteOutlierDetection.class);

  /**
   * Clustering algorithm to use
   */
  ClusteringAlgorithm<?> clusterer;

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param clusterer Clustering algorithm
   * @param noiseOption Noise handling option.
   */
  public SilhouetteOutlierDetection(DistanceFunction<? super O> distanceFunction, ClusteringAlgorithm<?> clusterer, NoiseHandling noiseOption) {
    super(distanceFunction);
    this.clusterer = clusterer;
    this.noiseOption = noiseOption;
  }

  @Override
  public OutlierResult run(Database database) {
    Relation<O> relation = database.getRelation(getDistanceFunction().getInputTypeRestriction());
    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());

    // TODO: improve ELKI api to ensure we're using the same DBIDs!
    Clustering<?> c = clusterer.run(database);

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();

    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
        case TREAT_NOISE_AS_SINGLETONS:
          // As suggested in Rousseeuw, we use 0 for singletons.
          for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
            scores.put(iter, 0.);
          }
          mm.put(0.);
          continue;
        case MERGE_NOISE:
          // Treat as cluster below
          break;
        }
      }
      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      double[] as = new double[ids.size()]; // temporary storage.
      DBIDArrayIter it1 = ids.iter(), it2 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        // a: In-cluster distances
        double a = as[it1.getOffset()]; // Already computed distances
        for(it2.seek(it1.getOffset() + 1); it2.valid(); it2.advance()) {
          final double dist = dq.distance(it1, it2);
          a += dist;
          as[it2.getOffset()] += dist;
        }
        a /= (ids.size() - 1);
        // b: other clusters:
        double min = Double.POSITIVE_INFINITY;
        for(Cluster<?> ocluster : clusters) {
          if(ocluster == /* yes, reference identity */cluster) {
            continue;
          }
          if(ocluster.isNoise()) {
            switch(noiseOption){
            case IGNORE_NOISE:
              continue;
            case MERGE_NOISE:
              // No special treatment
              break;
            case TREAT_NOISE_AS_SINGLETONS:
              // Treat noise cluster as singletons:
              for(DBIDIter it3 = ocluster.getIDs().iter(); it3.valid(); it3.advance()) {
                double dist = dq.distance(it1, it3);
                if(dist < min) {
                  min = dist;
                }
              }
              continue;
            }
          }
          final DBIDs oids = ocluster.getIDs();
          double b = 0.;
          for(DBIDIter it3 = oids.iter(); it3.valid(); it3.advance()) {
            b += dq.distance(it1, it3);
          }
          b /= oids.size();
          if(b < min) {
            min = b;
          }
        }
        final double score = (min - a) / Math.max(min, a);
        scores.put(it1, score);
        mm.put(score);
      }
    }
    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Silhouette Coefficients", "silhouette-outlier", scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), -1., 1., .5);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation dt = getDistanceFunction().getInputTypeRestriction();
    TypeInformation[] t = clusterer.getInputTypeRestriction();
    for(TypeInformation i : t) {
      if(dt.isAssignableFromType(i)) {
        return t;
      }
    }
    // Prepend distance type:
    TypeInformation[] t2 = new TypeInformation[t.length + 1];
    t2[0] = dt;
    System.arraycopy(t, 0, t2, 1, t.length);
    return t2;
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for choosing the clustering algorithm
     */
    public static final OptionID CLUSTERING_ID = new OptionID("silhouette.clustering", //
        "Clustering algorithm to use for the silhouette coefficients.");

    /**
     * Clustering algorithm to use
     */
    ClusteringAlgorithm<?> clusterer;

    /**
     * Noise handling
     */
    private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<ClusteringAlgorithm<?>> clusterP = new ObjectParameter<>(CLUSTERING_ID, ClusteringAlgorithm.class);
      if(config.grab(clusterP)) {
        clusterer = clusterP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<>(EvaluateSilhouette.Parameterizer.NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected SilhouetteOutlierDetection<O> makeInstance() {
      return new SilhouetteOutlierDetection<>(distanceFunction, clusterer, noiseOption);
    }
  }
}
