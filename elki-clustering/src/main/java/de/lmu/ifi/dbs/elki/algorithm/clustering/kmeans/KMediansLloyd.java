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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * k-medians clustering algorithm, but using Lloyd-style bulk iterations instead
 * of the more complicated approach suggested by Kaufman and Rousseeuw (see
 * {@link KMedoidsPAM} instead).
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
  public KMediansLloyd(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<MeanModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistanceFunction(), initialMeans(database, relation));
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
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
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
      Clustering<MeanModel> result = new Clustering<>("k-Medians Clustering", "kmedians-clustering");
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
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected KMediansLloyd<V> makeInstance() {
      return new KMediansLloyd<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
