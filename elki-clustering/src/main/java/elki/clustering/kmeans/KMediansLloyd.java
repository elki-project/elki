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
package elki.clustering.kmeans;

import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmedoids.PAM;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.data.model.MeanModel;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;

/**
 * k-medians clustering algorithm, but using Lloyd-style bulk iterations instead
 * of the more complicated approach suggested by Kaufman and Rousseeuw (see
 * {@link PAM} instead).
 * <p>
 * Reference:
 * <p>
 * Clustering via Concave Minimization<br>
 * P. S. Bradley, O. L. Mangasarian, W. N. Street<br>
 * Advances in Neural Information Processing Systems (NIPS'96)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @navassoc - - - MeanModel
 *
 * @param <V> vector datatype
 */
@Reference(title = "Clustering via Concave Minimization", //
    authors = "P. S. Bradley, O. L. Mangasarian, W. N. Street", //
    booktitle = "Advances in Neural Information Processing Systems", //
    url = "https://papers.nips.cc/paper/1260-clustering-via-concave-minimization", //
    bibkey = "DBLP:conf/nips/BradleyMS96")
public class KMediansLloyd<V extends NumberVector> extends AbstractKMeans<V, MeanModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMediansLloyd.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMediansLloyd(NumberVectorDistance<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<MeanModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistance(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildMediansResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration > 1) {
        means = medians(clusters, means, relation);
      }
      return assignToNearestCluster();
    }

    protected Clustering<MeanModel> buildMediansResult() {
      Clustering<MeanModel> result = new Clustering<>();
      Metadata.of(result).setLongName("k-Medians Clustering");
      for(int i = 0; i < clusters.size(); i++) {
        result.addToplevelCluster(new Cluster<>(clusters.get(i), new MeanModel(means[i])));
      }
      return result;
    }

    /**
     * Returns the median vectors of the given clusters in the given database.
     *
     * @param clusters the clusters to compute the means
     * @param medians the recent medians
     * @param relation the relation containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected double[][] medians(List<? extends DBIDs> clusters, double[][] medians, Relation<? extends NumberVector> relation) {
      final int dim = medians[0].length;
      final SortDBIDsBySingleDimension sorter = new SortDBIDsBySingleDimension(relation);
      double[][] newMedians = new double[k][];
      ArrayModifiableDBIDs list = DBIDUtil.newArray();
      DBIDArrayIter it = list.iter();
      for(int i = 0; i < k; i++) {
        DBIDs clu = clusters.get(i);
        if(clu.size() <= 0) {
          newMedians[i] = medians[i];
          continue;
        }
        list.clear();
        list.addDBIDs(clu);
        double[] mean = new double[dim];
        for(int d = 0; d < dim; d++) {
          sorter.setDimension(d);
          mean[d] = relation.get(it.seek(QuickSelectDBIDs.median(list, sorter))).doubleValue(d);
        }
        newMedians[i] = mean;
      }
      return newMedians;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    @Override
    public KMediansLloyd<V> make() {
      return new KMediansLloyd<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
