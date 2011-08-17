package de.lmu.ifi.dbs.elki.algorithm.clustering;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractPrimitiveDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides the k-means algorithm.
 * <p>
 * Reference: J. MacQueen: Some Methods for Classification and Analysis of
 * Multivariate Observations. <br>
 * In 5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has MeanModel
 * 
 * @param <D> a type of {@link Distance} as returned by the used distance
 *        function
 * @param <V> a type of {@link NumberVector} as a suitable datatype for this
 *        algorithm
 */
@Title("K-Means")
@Description("Finds a partitioning into k clusters.")
@Reference(authors = "J. MacQueen", title = "Some Methods for Classification and Analysis of Multivariate Observations", booktitle = "5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297", url = "http://projecteuclid.org/euclid.bsmsp/1200512992")
public class KMeans<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm<V, D, Clustering<MeanModel<V>>> implements ClusteringAlgorithm<Clustering<MeanModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KMeans.class);

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("kmeans.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater or equal to 0, where 0 means no limit.
   */
  public static final OptionID MAXITER_ID = OptionID.getOrCreateOptionID("kmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("kmeans.seed", "The random number generator seed.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Holds the value of {@link #MAXITER_ID}.
   */
  private int maxiter;

  /**
   * Holds the value of {@link #SEED_ID}.
   */
  private Long seed;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param seed Random generator seed
   */
  public KMeans(PrimitiveDistanceFunction<? super V, D> distanceFunction, int k, int maxiter, Long seed) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.seed = seed;
  }

  /**
   * Run k-means
   * 
   * @param database Database
   * @param relation relation to use
   * @return result
   * @throws IllegalStateException
   */
  public Clustering<MeanModel<V>> run(Database database, Relation<V> relation) throws IllegalStateException {
    final Random random = (this.seed != null) ? new Random(this.seed) : new Random();
    if(relation.size() > 0) {
      final int dim = DatabaseUtil.dimensionality(relation);
      Pair<V, V> minmax = DatabaseUtil.computeMinMax(relation);
      List<V> means = new ArrayList<V>(k);
      List<V> oldMeans;
      if(logger.isVerbose()) {
        logger.verbose("initializing random vectors");
      }
      for(int i = 0; i < k; i++) {
        double[] r = MathUtil.randomDoubleArray(dim, random);
        // Rescale
        for (int d = 0; d < dim; d++) {
          r[d] = minmax.first.doubleValue(d + 1) + (minmax.second.doubleValue(d + 1) - minmax.first.doubleValue(d + 1)) * r[d];
        }
        // Instantiate
        V randomVector = minmax.first.newInstance(r);
        means.add(randomVector);
      }
      List<? extends ModifiableDBIDs> clusters;
      clusters = sort(means, relation);
      boolean changed = true;
      int iteration = 1;
      while(changed) {
        if(logger.isVerbose()) {
          logger.verbose("iteration " + iteration);
        }
        oldMeans = new ArrayList<V>(means);
        means = means(clusters, means, relation);
        clusters = sort(means, relation);
        changed = !means.equals(oldMeans);
        iteration++;

        if(maxiter > 0 && iteration > maxiter) {
          break;
        }
      }
      Clustering<MeanModel<V>> result = new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
      for(int i = 0; i < clusters.size(); i++) {
        DBIDs ids = clusters.get(i);
        MeanModel<V> model = new MeanModel<V>(means.get(i));
        result.addCluster(new Cluster<MeanModel<V>>(ids, model));
      }
      return result;
    }
    else {
      return new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
    }
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   * 
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected List<V> means(List<? extends ModifiableDBIDs> clusters, List<V> means, Relation<V> database) {
    List<V> newMeans = new ArrayList<V>(k);
    for(int i = 0; i < k; i++) {
      ModifiableDBIDs list = clusters.get(i);
      V mean = null;
      for(Iterator<DBID> clusterIter = list.iterator(); clusterIter.hasNext();) {
        if(mean == null) {
          mean = database.get(clusterIter.next());
        }
        else {
          mean = mean.plus(database.get(clusterIter.next()));
        }
      }
      if(list.size() > 0) {
        assert mean != null;
        mean = mean.multiplicate(1.0 / list.size());
      }
      else {
        mean = means.get(i);
      }
      newMeans.add(mean);
    }
    return newMeans;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means a list of k means
   * @param database the database to cluster
   * @return list of k clusters
   */
  protected List<? extends ModifiableDBIDs> sort(List<V> means, Relation<V> database) {
    List<ArrayModifiableDBIDs> clusters = new ArrayList<ArrayModifiableDBIDs>(k);
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newArray());
    }

    for(DBID id : database.iterDBIDs()) {
      List<D> distances = new ArrayList<D>(k);
      V fv = database.get(id);
      int minIndex = 0;
      for(int d = 0; d < k; d++) {
        distances.add(getDistanceFunction().distance(fv, means.get(d)));
        if(distances.get(d).compareTo(distances.get(minIndex)) < 0) {
          minIndex = d;
        }
      }
      clusters.get(minIndex).add(id);
    }
    for(ArrayModifiableDBIDs cluster : clusters) {
      Collections.sort(cluster);
    }
    return clusters;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<V, D> {
    protected int k;

    protected int maxiter;

    protected Long seed;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      IntParameter maxiterP = new IntParameter(MAXITER_ID, new GreaterEqualConstraint(0), 0);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }

      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected KMeans<V, D> makeInstance() {
      return new KMeans<V, D>(distanceFunction, k, maxiter, seed);
    }
  }
}