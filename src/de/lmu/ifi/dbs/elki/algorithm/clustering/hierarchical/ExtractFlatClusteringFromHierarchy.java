package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDistanceDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Extract a flat clustering from a full hierarchy, represented in pointer form.
 * 
 * @author Erich Schubert
 */
public class ExtractFlatClusteringFromHierarchy<D extends Distance<D>> implements ClusteringAlgorithm<Clustering<DendrogramModel<D>>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ExtractFlatClusteringFromHierarchy.class);

  /**
   * Minimum number of clusters to extract
   */
  private int minclusters = -1;

  /**
   * Clustering algorithm to run to obtain the hierarchy.
   */
  private HierarchicalClusteringAlgorithm<D> algorithm;

  /**
   * Constructor.
   * 
   * @param algorithm Algorithm to run
   * @param minclusters Minimum number of clusters
   */
  public ExtractFlatClusteringFromHierarchy(HierarchicalClusteringAlgorithm<D> algorithm, int minclusters) {
    super();
    this.algorithm = algorithm;
    this.minclusters = minclusters;
  }

  @Override
  public Clustering<DendrogramModel<D>> run(Database database) {
    PointerHierarchyRepresentationResult<D> pointerresult = algorithm.run(database);
    DBIDs ids = pointerresult.getDBIDs();
    DBIDDataStore pi = pointerresult.getParentStore();
    DataStore<D> lambda = pointerresult.getParentDistanceStore();

    Clustering<DendrogramModel<D>> result;
    if (lambda instanceof DoubleDistanceDataStore) {
      result = extractClustersDouble(ids, pi, (DoubleDistanceDataStore) lambda, minclusters);
    } else {
      result = extractClusters(ids, pi, lambda, minclusters);
    }
    result.addChildResult(pointerresult);

    return result;
  }

  /**
   * Extract all clusters from the pi-lambda-representation.
   * 
   * @param ids Object ids to process
   * @param pi Pi store
   * @param lambda Lambda store
   * @param minclusters Minimum number of clusters to extract
   * 
   * @return Hierarchical clustering
   */
  private Clustering<DendrogramModel<D>> extractClusters(DBIDs ids, final DBIDDataStore pi, final DataStore<D> lambda, int minclusters) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), LOG) : null;

    // Sort DBIDs by lambda. We need this for two things:
    // a) to determine the stop distance from "minclusters" parameter
    // b) to process arrows in decreasing / increasing order
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    order.sort(new CompareByLambda<>(lambda));

    // Stop distance:
    final D stopdist = (minclusters > 0) ? lambda.get(order.get(ids.size() - minclusters)) : null;

    // The initial pass is top-down.
    DBIDArrayIter it = order.iter();
    int split = (minclusters > 0) ? Math.max(ids.size() - minclusters, 0) : 0;
    // Tie handling: decrement split.
    if (stopdist != null) {
      while (split > 0) {
        it.seek(split - 1);
        if (stopdist.compareTo(lambda.get(it)) == 0) {
          split--;
          minclusters++;
        } else {
          break;
        }
      }
    }

    // Extract the child clusters
    int cnum = 0;
    int expcnum = Math.max(0, minclusters);
    WritableIntegerDataStore cluster_map = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, -1);
    ArrayList<ModifiableDBIDs> cluster_dbids = new ArrayList<>(expcnum);
    ArrayList<D> cluster_dist = new ArrayList<>(expcnum);
    ArrayModifiableDBIDs cluster_leads = DBIDUtil.newArray(expcnum);

    DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.
    // Go backwards on the lower part.
    for (it.seek(split - 1); it.valid(); it.retract()) {
      D dist = lambda.get(it); // Distance to successor
      pi.assignVar(it, succ); // succ = pi(it)
      int clusterid = cluster_map.intValue(succ);
      // Successor cluster has already been created:
      if (clusterid >= 0) {
        cluster_dbids.get(clusterid).add(it);
        cluster_map.putInt(it, clusterid);
        // Update distance to maximum encountered:
        if (cluster_dist.get(clusterid).compareTo(dist) < 0) {
          cluster_dist.set(clusterid, dist);
        }
      } else {
        // Need to start a new cluster:
        clusterid = cnum; // next cluster number.
        ModifiableDBIDs cids = DBIDUtil.newArray();
        // Add element and successor as initial members:
        cids.add(succ);
        cluster_map.putInt(succ, clusterid);
        cids.add(it);
        cluster_map.putInt(it, clusterid);
        // Store new cluster.
        cluster_dbids.add(cids);
        cluster_leads.add(succ);
        cluster_dist.add(dist);
        cnum++;
      }

      // Decrement counter
      if (progress != null) {
        progress.incrementProcessed(LOG);
      }
    }
    // Build a hierarchy out of these clusters.
    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<>("Single-Link-Dendrogram", "slink-dendrogram");
    Cluster<DendrogramModel<D>> root = null;
    ArrayList<Cluster<DendrogramModel<D>>> clusters = new ArrayList<>(ids.size() + expcnum - split);
    // Convert initial clusters to cluster objects
    {
      int i = 0;
      for (DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
        clusters.add(makeCluster(it2, cluster_dist.get(i), cluster_dbids.get(i)));
      }
      cluster_dist = null; // Invalidate
      cluster_dbids = null; // Invalidate
    }
    // Process the upper part, bottom-up.
    for (it.seek(split); it.valid(); it.advance()) {
      int clusterid = cluster_map.intValue(it);
      // The current cluster:
      final Cluster<DendrogramModel<D>> clus;
      if (clusterid >= 0) {
        clus = clusters.get(clusterid);
      } else {
        ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
        cids.add(it);
        clus = makeCluster(it, null, cids);
        // No need to store in clusters: cannot have another incoming pi
        // pointer!
      }
      // The successor to join:
      pi.assignVar(it, succ); // succ = pi(it)
      if (DBIDUtil.equal(it, succ)) {
        assert (root == null);
        root = clus;
      } else {
        // Parent cluster:
        int parentid = cluster_map.intValue(succ);
        D depth = lambda.get(it);
        // Parent cluster exists - merge as a new cluster:
        if (parentid >= 0) {
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS);
          dendrogram.addChildCluster(pclus, clusters.get(parentid));
          dendrogram.addChildCluster(pclus, clus);
          clusters.set(parentid, pclus); // Replace existing parent cluster
        } else {
          // Create a new, one-element, parent cluster.
          parentid = cnum;
          cnum++;
          ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
          cids.add(succ);
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, cids);
          dendrogram.addChildCluster(pclus, clus);
          assert (clusters.size() == parentid);
          clusters.add(pclus); // Remember parent cluster
          cluster_map.putInt(succ, parentid); // Reference
        }
      }

      // Decrement counter
      if (progress != null) {
        progress.incrementProcessed(LOG);
      }
    }

    if (progress != null) {
      progress.ensureCompleted(LOG);
    }
    // build hierarchy
    dendrogram.addToplevelCluster(root);

    return dendrogram;
  }

  /**
   * Extract all clusters from the pi-lambda-representation.
   * 
   * @param ids Object ids to process
   * @param pi Pi store
   * @param lambda Lambda store
   * @param minclusters Minimum number of clusters to extract
   * 
   * @return Hierarchical clustering
   */
  private Clustering<DendrogramModel<D>> extractClustersDouble(DBIDs ids, final DBIDDataStore pi, final DoubleDistanceDataStore lambda, int minclusters) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), LOG) : null;

    // Sort DBIDs by lambda. We need this for two things:
    // a) to determine the stop distance from "minclusters" parameter
    // b) to process arrows in decreasing / increasing order
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    order.sort(new CompareByDoubleLambda(lambda));

    // Stop distance:
    final double stopdist = (minclusters > 0) ? lambda.doubleValue(order.get(ids.size() - minclusters)) : Double.POSITIVE_INFINITY;

    // The initial pass is top-down.
    DBIDArrayIter it = order.iter();
    int split = (minclusters > 0) ? Math.max(ids.size() - minclusters, 0) : 0;
    // Tie handling: decrement split.
    if (minclusters > 0) {
      while (split > 0) {
        it.seek(split - 1);
        if (stopdist <= lambda.doubleValue(it)) {
          split--;
          minclusters++;
        } else {
          break;
        }
      }
    }

    // Extract the child clusters
    int cnum = 0;
    int expcnum = Math.max(0, minclusters);
    WritableIntegerDataStore cluster_map = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, -1);
    ArrayList<ModifiableDBIDs> cluster_dbids = new ArrayList<>(expcnum);
    TDoubleArrayList cluster_dist = new TDoubleArrayList(expcnum);
    ArrayModifiableDBIDs cluster_leads = DBIDUtil.newArray(expcnum);

    DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.
    // Go backwards on the lower part.
    for (it.seek(split - 1); it.valid(); it.retract()) {
      double dist = lambda.doubleValue(it); // Distance to successor
      pi.assignVar(it, succ); // succ = pi(it)
      int clusterid = cluster_map.intValue(succ);
      // Successor cluster has already been created:
      if (clusterid >= 0) {
        cluster_dbids.get(clusterid).add(it);
        cluster_map.putInt(it, clusterid);
        // Update distance to maximum encountered:
        if (cluster_dist.get(clusterid) < dist) {
          cluster_dist.set(clusterid, dist);
        }
      } else {
        // Need to start a new cluster:
        clusterid = cnum; // next cluster number.
        ModifiableDBIDs cids = DBIDUtil.newArray();
        // Add element and successor as initial members:
        cids.add(succ);
        cluster_map.putInt(succ, clusterid);
        cids.add(it);
        cluster_map.putInt(it, clusterid);
        // Store new cluster.
        cluster_dbids.add(cids);
        cluster_leads.add(succ);
        cluster_dist.add(dist);
        cnum++;
      }

      // Decrement counter
      if (progress != null) {
        progress.incrementProcessed(LOG);
      }
    }
    // Build a hierarchy out of these clusters.
    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<>("Single-Link-Dendrogram", "slink-dendrogram");
    Cluster<DendrogramModel<D>> root = null;
    ArrayList<Cluster<DendrogramModel<D>>> clusters = new ArrayList<>(ids.size() + expcnum - split);
    // Convert initial clusters to cluster objects
    {
      int i = 0;
      for (DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
        @SuppressWarnings("unchecked")
        D depth = (D) new DoubleDistance(cluster_dist.get(i));
        clusters.add(makeCluster(it2, depth, cluster_dbids.get(i)));
      }
      cluster_dist = null; // Invalidate
      cluster_dbids = null; // Invalidate
    }
    // Process the upper part, bottom-up.
    for (it.seek(split); it.valid(); it.advance()) {
      int clusterid = cluster_map.intValue(it);
      // The current cluster:
      final Cluster<DendrogramModel<D>> clus;
      if (clusterid >= 0) {
        clus = clusters.get(clusterid);
      } else {
        ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
        cids.add(it);
        clus = makeCluster(it, null, cids);
        // No need to store in clusters: cannot have another incoming pi
        // pointer!
      }
      // The successor to join:
      pi.assignVar(it, succ); // succ = pi(it)
      if (DBIDUtil.equal(it, succ)) {
        assert (root == null);
        root = clus;
      } else {
        // Parent cluster:
        int parentid = cluster_map.intValue(succ);
        @SuppressWarnings("unchecked")
        D depth = (D) new DoubleDistance(lambda.doubleValue(it));
        // Parent cluster exists - merge as a new cluster:
        if (parentid >= 0) {
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS);
          dendrogram.addChildCluster(pclus, clusters.get(parentid));
          dendrogram.addChildCluster(pclus, clus);
          clusters.set(parentid, pclus); // Replace existing parent cluster
        } else {
          // Create a new, one-element, parent cluster.
          parentid = cnum;
          cnum++;
          ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
          cids.add(succ);
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, cids);
          dendrogram.addChildCluster(pclus, clus);
          assert (clusters.size() == parentid);
          clusters.add(pclus); // Remember parent cluster
          cluster_map.putInt(succ, parentid); // Reference
        }
      }

      // Decrement counter
      if (progress != null) {
        progress.incrementProcessed(LOG);
      }
    }

    if (progress != null) {
      progress.ensureCompleted(LOG);
    }
    // build hierarchy
    dendrogram.addToplevelCluster(root);

    return dendrogram;
  }

  /**
   * Make the cluster for the given object
   * 
   * @param lead Leading object
   * @param depth Linkage depth
   * @param members Member objects
   * @return Cluster
   */
  private Cluster<DendrogramModel<D>> makeCluster(DBIDRef lead, D depth, DBIDs members) {
    final String name;
    if (members.size() == 0) {
      name = "merge_" + lead + "_" + depth;
    } else if (depth != null && depth.isInfiniteDistance()) {
      assert (members.contains(lead));
      name = "object_" + lead;
    } else if (depth != null) {
      name = "cluster_" + lead + "_" + depth;
    } else {
      // Complete data set only?
      name = "cluster_" + lead;
    }
    Cluster<DendrogramModel<D>> cluster = new Cluster<>(name, members, new DendrogramModel<>(depth));
    return cluster;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Order a DBID collection by the lambda value.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <D> Distance type
   */
  private static final class CompareByLambda<D extends Distance<D>> implements Comparator<DBIDRef> {
    /**
     * Lambda storage
     */
    private final DataStore<D> lambda;

    /**
     * Constructor.
     * 
     * @param lambda Lambda storage
     */
    protected CompareByLambda(DataStore<D> lambda) {
      this.lambda = lambda;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      D k1 = lambda.get(id1);
      D k2 = lambda.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return k1.compareTo(k2);
    }
  }

  /**
   * Order a DBID collection by the lambda value.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static final class CompareByDoubleLambda implements Comparator<DBIDRef> {
    /**
     * Lambda storage
     */
    private final DoubleDistanceDataStore lambda;

    /**
     * Constructor.
     * 
     * @param lambda Lambda storage
     */
    protected CompareByDoubleLambda(DoubleDistanceDataStore lambda) {
      this.lambda = lambda;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      double k1 = lambda.doubleValue(id1);
      double k2 = lambda.doubleValue(id2);
      return Double.compare(k1, k2);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<D extends Distance<D>> extends AbstractParameterizer {
    /**
     * The minimum number of clusters to extract
     */
    public static final OptionID MINCLUSTERS_ID = new OptionID("hierarchical.minclusters", "The minimum number of clusters to extract.");

    /**
     * Number of clusters to extract.
     */
    int minclusters = -1;

    /**
     * The hierarchical clustering algorithm to run.
     */
    HierarchicalClusteringAlgorithm<D> algorithm;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<HierarchicalClusteringAlgorithm<D>> algorithmP = new ObjectParameter<>(AlgorithmStep.Parameterizer.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class);
      if (config.grab(algorithmP)) {
        algorithm = algorithmP.instantiateClass(config);
      }

      IntParameter minclustersP = new IntParameter(MINCLUSTERS_ID);
      minclustersP.addConstraint(new GreaterEqualConstraint(1));
      if (config.grab(minclustersP)) {
        minclusters = minclustersP.intValue();
      }
    }

    @Override
    protected ExtractFlatClusteringFromHierarchy<D> makeInstance() {
      return new ExtractFlatClusteringFromHierarchy<>(algorithm, minclusters);
    }
  }
}
