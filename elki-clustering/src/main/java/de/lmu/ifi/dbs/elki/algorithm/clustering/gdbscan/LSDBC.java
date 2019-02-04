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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.jafama.FastMath;

/**
 * Locally Scaled Density Based Clustering.
 * <p>
 * This is a variant of DBSCAN which starts with the most dense point first,
 * then expands clusters until density has dropped below a threshold.
 * <p>
 * Reference:
 * <p>
 * E. Biçici, D. Yuret<br>
 * Locally Scaled Density Based Clustering<br>
 * Adaptive and Natural Computing Algorithms
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
@Title("LSDBC: Locally Scaled Density Based Clustering")
@Reference(authors = "E. Biçici, D. Yuret", //
    title = "Locally Scaled Density Based Clustering", //
    booktitle = "Adaptive and Natural Computing Algorithms", //
    url = "https://doi.org/10.1007/978-3-540-71618-1_82", //
    bibkey = "DBLP:conf/icannga/BiciciY07")
@Priority(Priority.IMPORTANT)
public class LSDBC<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Class logger.
   */
  private static Logging LOG = Logging.getLogger(LSDBC.class);

  /**
   * kNN parameter.
   */
  protected int k;

  /**
   * Alpha parameter.
   */
  protected double alpha;

  /**
   * Constants used internally.
   */
  protected static int UNPROCESSED = GeneralizedDBSCAN.Instance.UNPROCESSED, //
      NOISE = GeneralizedDBSCAN.Instance.NOISE;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param k Neighborhood size parameter
   * @param alpha Alpha parameter
   */
  public LSDBC(DistanceFunction<? super O> distanceFunction, int k, double alpha) {
    super(distanceFunction);
    this.k = k + 1; // Skip query point
    this.alpha = alpha;
  }

  /**
   * Run the LSDBC algorithm
   *
   * @param database Database to process
   * @param relation Data relation
   * @return Clustering result
   */
  public Clustering<Model> run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LSDBC", 3) : null;
    final int dim = RelationUtil.dimensionality(relation);
    final double factor = FastMath.pow(2., alpha / dim);

    final DBIDs ids = relation.getDBIDs();
    LOG.beginStep(stepprog, 1, "Materializing kNN neighborhoods");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistanceFunction(), k);

    LOG.beginStep(stepprog, 2, "Sorting by density");
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    fillDensities(knnq, ids, dens);
    ArrayModifiableDBIDs sids = DBIDUtil.newArray(ids);
    sids.sort(new DataStoreUtil.AscendingByDoubleDataStore(dens));

    LOG.beginStep(stepprog, 3, "Computing clusters");
    // Setup progress logging
    final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("LSDBC Clustering", ids.size(), LOG) : null;
    final IndefiniteProgress clusprogress = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters found", LOG) : null;
    // (Temporary) store the cluster ID assigned.
    final WritableIntegerDataStore clusterids = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, UNPROCESSED);
    // Note: these are not exact, as objects may be stolen from noise.
    final IntArrayList clustersizes = new IntArrayList();
    clustersizes.add(0); // Unprocessed dummy value.
    clustersizes.add(0); // Noise counter.

    // Implementation Note: using Integer objects should result in
    // reduced memory use in the HashMap!
    int clusterid = NOISE + 1;
    // Iterate over all objects in the database.
    for(DBIDIter id = sids.iter(); id.valid(); id.advance()) {
      // Skip already processed ids.
      if(clusterids.intValue(id) != UNPROCESSED) {
        continue;
      }
      // Evaluate Neighborhood predicate
      final KNNList neighbors = knnq.getKNNForDBID(id, k);
      // Evaluate Core-Point predicate:
      if(isLocalMaximum(neighbors.getKNNDistance(), neighbors, dens)) {
        double mindens = factor * neighbors.getKNNDistance();
        clusterids.putInt(id, clusterid);
        clustersizes.add(expandCluster(clusterid, clusterids, knnq, neighbors, mindens, progress));
        // start next cluster on next iteration.
        ++clusterid;
        if(clusprogress != null) {
          clusprogress.setProcessed(clusterid, LOG);
        }
      }
      else {
        // otherwise, it's a noise point
        clusterids.putInt(id, NOISE);
        clustersizes.set(NOISE, clustersizes.getInt(NOISE) + 1);
      }
      // We've completed this element
      LOG.incrementProcessed(progress);
    }
    // Finish progress logging.
    LOG.ensureCompleted(progress);
    LOG.setCompleted(clusprogress);

    LOG.setCompleted(stepprog);

    // Transform cluster ID mapping into a clustering result:
    ArrayList<ArrayModifiableDBIDs> clusterlists = new ArrayList<>(clusterid);
    // add storage containers for clusters
    for(int i = 0; i < clustersizes.size(); i++) {
      clusterlists.add(DBIDUtil.newArray(clustersizes.getInt(i)));
    }
    // do the actual inversion
    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      // Negative values are non-core points:
      int cid = clusterids.intValue(id);
      int cluster = Math.abs(cid);
      clusterlists.get(cluster).add(id);
    }
    clusterids.destroy();

    Clustering<Model> result = new Clustering<>("LSDBC", "lsdbc-clustering");
    for(int cid = NOISE; cid < clusterlists.size(); cid++) {
      boolean isNoise = (cid == NOISE);
      Cluster<Model> c;
      c = new Cluster<Model>(clusterlists.get(cid), isNoise, ClusterModel.CLUSTER);
      result.addToplevelCluster(c);
    }
    return result;
  }

  /**
   * Test if a point is a local density maximum.
   *
   * @param kdist k-distance of current
   * @param neighbors Neighbor points
   * @param kdists kNN distances
   * @return {@code true} when the point is a local maximum
   */
  private boolean isLocalMaximum(double kdist, DBIDs neighbors, WritableDoubleDataStore kdists) {
    for(DBIDIter it = neighbors.iter(); it.valid(); it.advance()) {
      if(kdists.doubleValue(it) < kdist) {
        return false;
      }
    }
    return true;
  }

  /**
   * Set-based expand cluster implementation.
   *
   * @param clusterid ID of the current cluster.
   * @param clusterids Current object to cluster mapping.
   * @param knnq kNNQuery
   * @param neighbors Neighbors acquired by initial getNeighbors call.
   * @param maxkdist Maximum k-distance
   * @param progress Progress logging
   *
   * @return cluster size
   */
  protected int expandCluster(final int clusterid, final WritableIntegerDataStore clusterids, final KNNQuery<O> knnq, final DBIDs neighbors, final double maxkdist, final FiniteProgress progress) {
    int clustersize = 1; // initial seed!
    final ArrayModifiableDBIDs activeSet = DBIDUtil.newArray();
    activeSet.addDBIDs(neighbors);
    // run expandCluster as long as this set is non-empty (non-recursive
    // implementation)
    DBIDVar id = DBIDUtil.newVar();
    while(!activeSet.isEmpty()) {
      activeSet.pop(id);
      // Assign object to cluster
      final int oldclus = clusterids.intValue(id);
      if(oldclus == NOISE) {
        clustersize += 1;
        // Non core point cluster member:
        clusterids.putInt(id, -clusterid);
      }
      else if(oldclus == UNPROCESSED) {
        clustersize += 1;
        // expandCluster again:
        // Evaluate Neighborhood predicate
        final KNNList newneighbors = knnq.getKNNForDBID(id, k);
        // Evaluate Core-Point predicate
        if(newneighbors.getKNNDistance() <= maxkdist) {
          activeSet.addDBIDs(newneighbors);
        }
        clusterids.putInt(id, clusterid);
        LOG.incrementProcessed(progress);
      }
    }
    return clustersize;
  }

  /**
   * Collect all densities into an array for sorting.
   *
   * @param knnq kNN query
   * @param ids DBIDs to process
   * @param dens Density storage
   */
  private void fillDensities(KNNQuery<O> knnq, DBIDs ids, WritableDoubleDataStore dens) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      dens.putDouble(iter, neighbors.getKNNDistance());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for neighborhood size.
     */
    public static final OptionID K_ID = new OptionID("lsdbc.k", "Neighborhood size (k)");

    /**
     * Parameter for the maximum density difference.
     */
    public static final OptionID ALPHA_ID = new OptionID("lsdbc.alpha", "Density difference factor");

    /**
     * kNN parameter.
     */
    protected int k;

    /**
     * Alpha parameter.
     */
    protected double alpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected LSDBC<O> makeInstance() {
      return new LSDBC<>(distanceFunction, k, alpha);
    }
  }
}
