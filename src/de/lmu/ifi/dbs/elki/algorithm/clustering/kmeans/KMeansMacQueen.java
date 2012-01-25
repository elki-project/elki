package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractPrimitiveDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides the k-means algorithm, using MacQueen style incremental updates.
 * 
 * <p>
 * Reference:<br />
 * J. MacQueen: Some Methods for Classification and Analysis of Multivariate
 * Observations. <br />
 * In 5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has MeanModel
 * 
 * @param <V> vector type to use
 * @param <D> distance function value type
 */
@Title("K-Means")
@Description("Finds a partitioning into k clusters.")
@Reference(authors = "J. MacQueen", title = "Some Methods for Classification and Analysis of Multivariate Observations", booktitle = "5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297", url = "http://projecteuclid.org/euclid.bsmsp/1200512992")
public class KMeansMacQueen<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractKMeans<V, D> implements ClusteringAlgorithm<Clustering<MeanModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KMeansMacQueen.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   */
  public KMeansMacQueen(PrimitiveDistanceFunction<NumberVector<?, ?>, D> distanceFunction, int k, int maxiter, KMeansInitialization<V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
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
    if(relation.size() <= 0) {
      return new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    List<Vector> means = initializer.chooseInitialMeans(relation, k, getDistanceFunction());
    // Initialize cluster and assign objects
    List<ModifiableDBIDs> clusters = new ArrayList<ModifiableDBIDs>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }
    assignToNearestCluster(relation, means, clusters);
    // Initial recomputation of the means.
    means = means(clusters, means, relation);

    // Raw distance function
    final PrimitiveDistanceFunction<? super NumberVector<?, ?>, D> df = getDistanceFunction();

    // Refine result
    for(int iteration = 0; maxiter <= 0 || iteration < maxiter; iteration++) {
      if(logger.isVerbose()) {
        logger.verbose("K-Means iteration " + (iteration + 1));
      }
      boolean changed = false;
      // Incremental update
      for(DBID id : relation.iterDBIDs()) {
        D mindist = df.getDistanceFactory().infiniteDistance();
        V fv = relation.get(id);
        int minIndex = 0;
        for(int i = 0; i < k; i++) {
          D dist = df.distance(fv, means.get(i));
          if(dist.compareTo(mindist) < 0) {
            minIndex = i;
            mindist = dist;
          }
        }
        // Update the cluster mean incrementally:
        for(int i = 0; i < k; i++) {
          ModifiableDBIDs ci = clusters.get(i);
          if(i == minIndex) {
            if(ci.add(id)) {
              incrementalUpdateMean(means.get(i), relation.get(id), ci.size(), +1);
              changed = true;
            }
          }
          else if(ci.remove(id)) {
            incrementalUpdateMean(means.get(i), relation.get(id), ci.size() + 1, -1);
            changed = true;
          }
        }
      }
      if(!changed) {
        break;
      }
    }
    final V factory = DatabaseUtil.assumeVectorField(relation).getFactory();
    Clustering<MeanModel<V>> result = new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      MeanModel<V> model = new MeanModel<V>(factory.newNumberVector(means.get(i).getArrayRef()));
      result.addCluster(new Cluster<MeanModel<V>>(ids, model));
    }
    return result;
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
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<NumberVector<?, ?>, D> {
    protected int k;

    protected int maxiter;

    protected KMeansInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<KMeansInitialization<V>>(INIT_ID, KMeansInitialization.class, RandomlyGeneratedInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      IntParameter maxiterP = new IntParameter(MAXITER_ID, new GreaterEqualConstraint(0), 0);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    @Override
    protected AbstractKMeans<V, D> makeInstance() {
      return new KMeansMacQueen<V, D>(distanceFunction, k, maxiter, initializer);
    }
  }
}