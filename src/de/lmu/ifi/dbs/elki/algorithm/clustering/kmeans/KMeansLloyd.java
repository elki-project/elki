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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides the k-means algorithm, using Lloyd-style bulk iterations.
 * 
 * <p>
 * Reference:<br />
 * S. Lloyd<br/>
 * Least squares quantization in PCM<br/>
 * IEEE Transactions on Information Theory 28 (2)<br/>
 * previously published as Bell Telephone Laboratories Paper
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * 
 * @param <V> vector datatype
 * @param <D> distance value type
 */
@Title("K-Means")
@Description("Finds a partitioning into k clusters.")
@Reference(authors = "S. Lloyd", title = "Least squares quantization in PCM", booktitle = "IEEE Transactions on Information Theory 28 (2): 129–137.", url = "http://dx.doi.org/10.1109/TIT.1982.1056489")
public class KMeansLloyd<V extends NumberVector<?>, D extends Distance<D>> extends AbstractKMeans<V, D> implements ClusteringAlgorithm<Clustering<MeanModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansLloyd.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansLloyd(PrimitiveDistanceFunction<NumberVector<?>, D> distanceFunction, int k, int maxiter, KMeansInitialization<V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  /**
   * Run k-means.
   * 
   * @param database Database
   * @param relation relation to use
   * @return result
   */
  public Clustering<MeanModel<V>> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    List<? extends NumberVector<?>> means = initializer.chooseInitialMeans(relation, k, getDistanceFunction());
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<ModifiableDBIDs>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet(relation.size() / k));
    }

    for(int iteration = 0; maxiter <= 0 || iteration < maxiter; iteration++) {
      if(LOG.isVerbose()) {
        LOG.verbose("K-Means iteration " + (iteration + 1));
      }
      boolean changed = assignToNearestCluster(relation, means, clusters);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      // Recompute means.
      means = means(clusters, means, relation);
    }
    // Wrap result
    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    Clustering<MeanModel<V>> result = new Clustering<MeanModel<V>>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      MeanModel<V> model = new MeanModel<V>(factory.newNumberVector(means.get(i).getColumnVector().getArrayRef()));
      result.addCluster(new Cluster<MeanModel<V>>(clusters.get(i), model));
    }
    return result;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm.Parameterizer<NumberVector<?>, D> {
    /**
     * k Parameter.
     */
    protected int k;

    /**
     * Number of iterations.
     */
    protected int maxiter;

    /**
     * Initialization method.
     */
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
      return new KMeansLloyd<V, D>(distanceFunction, k, maxiter, initializer);
    }
  }
}
