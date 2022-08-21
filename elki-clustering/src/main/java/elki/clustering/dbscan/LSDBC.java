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
package elki.clustering.dbscan;

import java.util.ArrayList;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.progress.StepProgress;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class LSDBC<O extends NumberVector> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LSDBC.class);

  /**
   * Number of neighbors (+ query point)
   */
  protected int kplus;

  /**
   * Alpha parameter.
   */
  protected double alpha;

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constants used internally.
   */
  protected static int UNPROCESSED = GeneralizedDBSCAN.Instance.UNPROCESSED, //
      NOISE = GeneralizedDBSCAN.Instance.NOISE;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param k Neighborhood size parameter
   * @param alpha Alpha parameter
   */
  public LSDBC(Distance<? super O> distance, int k, double alpha) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // Skip query point
    this.alpha = alpha;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the LSDBC algorithm
   *
   * @param relation Data relation
   * @return Clustering result
   */
  public Clustering<Model> run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LSDBC", 3) : null;
    final int dim = RelationUtil.dimensionality(relation);
    final double factor = FastMath.pow(2., alpha / dim);

    final DBIDs ids = relation.getDBIDs();
    LOG.beginStep(stepprog, 1, "Materializing kNN neighborhoods");
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(kplus);

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
      final KNNList neighbors = knnq.getKNN(id, kplus);
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
      clusterlists.get(Math.abs(clusterids.intValue(id))).add(id);
    }
    clusterids.destroy();

    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("LSDBC Clustering");
    for(int cid = NOISE; cid < clusterlists.size(); cid++) {
      result.addToplevelCluster(new Cluster<Model>(clusterlists.get(cid), (cid == NOISE), ClusterModel.CLUSTER));
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
   * @return cluster size
   */
  protected int expandCluster(int clusterid, WritableIntegerDataStore clusterids, KNNSearcher<DBIDRef> knnq, DBIDs neighbors, double maxkdist, FiniteProgress progress) {
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
        final KNNList newneighbors = knnq.getKNN(id, kplus);
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
  private void fillDensities(KNNSearcher<DBIDRef> knnq, DBIDs ids, WritableDoubleDataStore dens) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      dens.putDouble(iter, knnq.getKNN(iter, kplus).getKNNDistance());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par<O extends NumberVector> implements Parameterizer {
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

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(ALPHA_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> alpha = x);
    }

    @Override
    public LSDBC<O> make() {
      return new LSDBC<>(distance, k, alpha);
    }
  }
}
