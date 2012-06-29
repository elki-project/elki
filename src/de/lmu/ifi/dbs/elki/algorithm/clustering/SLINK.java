package de.lmu.ifi.dbs.elki.algorithm.clustering;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.HierarchyHashmapList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.ModifiableHierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Efficient implementation of the Single-Link Algorithm SLINK of R. Sibson.
 * <p>
 * Reference: R. Sibson: SLINK: An optimally efficient algorithm for the
 * single-link cluster method. <br>
 * In: The Computer Journal 16 (1973), No. 1, p. 30-34.
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject the algorithm is applied on
 * @param <D> the type of Distance used
 */
@Title("SLINK: Single Link Clustering")
@Description("Hierarchical clustering algorithm based on single-link connectivity.")
@Reference(authors = "R. Sibson", title = "SLINK: An optimally efficient algorithm for the single-link cluster method", booktitle = "The Computer Journal 16 (1973), No. 1, p. 30-34.", url = "http://dx.doi.org/10.1093/comjnl/16.1.30")
public class SLINK<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLINK.class);

  /**
   * The minimum number of clusters to extract
   */
  public static final OptionID SLINK_MINCLUSTERS_ID = OptionID.getOrCreateOptionID("slink.minclusters", "The maximum number of clusters to extract.");

  /**
   * The values of the function Pi of the pointer representation.
   */
  private WritableDataStore<DBID> pi;

  /**
   * The values of the function Lambda of the pointer representation.
   */
  private WritableDataStore<D> lambda;

  /**
   * Minimum number of clusters to extract
   */
  private Integer minclusters;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param minclusters Minimum clusters to extract. Can be null
   */
  public SLINK(DistanceFunction<? super O, D> distanceFunction, Integer minclusters) {
    super(distanceFunction);
    this.minclusters = minclusters;
  }

  /**
   * Performs the SLINK algorithm on the given database.
   */
  @SuppressWarnings("unchecked")
  public Result run(Database database, Relation<O> relation) {
    DistanceQuery<O, D> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    Class<D> distCls = (Class<D>) getDistanceFunction().getDistanceFactory().getClass();
    WritableRecordStore store = DataStoreUtil.makeRecordStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBID.class, distCls);
    pi = store.getStorage(0, DBID.class);
    lambda = store.getStorage(1, distCls);
    // Temporary storage for m.
    WritableDataStore<D> m = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, distCls);

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Clustering", relation.size(), logger) : null;
    // has to be an array for monotonicity reasons!
    ModifiableDBIDs processedIDs = DBIDUtil.newArray(relation.size());

    // apply the algorithm
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      step1(id);
      step2(id, processedIDs, distQuery, m);
      step3(id, processedIDs, m);
      step4(id, processedIDs);

      processedIDs.add(id);

      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }

    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    // We don't need m anymore.
    m.destroy();
    m = null;

    // build dendrogram
    BasicResult result = null;

    // Build clusters identified by their target object
    int minc = minclusters != null ? minclusters : relation.size();
    result = extractClusters(relation.getDBIDs(), pi, lambda, minc);

    result.addChildResult(new MaterializedRelation<DBID>("SLINK pi", "slink-order", TypeUtil.DBID, pi, processedIDs));
    result.addChildResult(new MaterializedRelation<D>("SLINK lambda", "slink-order", new SimpleTypeInformation<D>(distCls), lambda, processedIDs));
    result.addChildResult(new OrderingFromDataStore<D>("SLINK order", "slink-order", processedIDs, lambda));
    return result;
  }

  /**
   * First step: Initialize P(id) = id, L(id) = infinity.
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   */
  private void step1(DBID newID) {
    // P(n+1) = n+1:
    pi.put(newID, newID);
    // L(n+1) = infinity
    lambda.put(newID, getDistanceFunction().getDistanceFactory().infiniteDistance());
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param distFunc Distance function to use
   */
  private void step2(DBID newID, DBIDs processedIDs, DistanceQuery<O, D> distFunc, WritableDataStore<D> m) {
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      // M(i) = dist(i, n+1)
      m.put(id, distFunc.distance(id, newID));
    }
  }

  /**
   * Third step: Determine the values for P and L
   * 
   * @param newID the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   */
  private void step3(DBID newID, DBIDs processedIDs, WritableDataStore<D> m) {
    // for i = 1..n
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      D l_i = lambda.get(id);
      D m_i = m.get(id);
      DBID p_i = pi.get(id);
      D mp_i = m.get(p_i);

      // if L(i) >= M(i)
      if(l_i.compareTo(m_i) >= 0) {
        // M(P(i)) = min { M(P(i)), L(i) }
        m.put(p_i, DistanceUtil.min(mp_i, l_i));

        // L(i) = M(i)
        lambda.put(id, m_i);

        // P(i) = n+1;
        pi.put(id, newID);
      }
      else {
        // M(P(i)) = min { M(P(i)), M(i) }
        m.put(p_i, DistanceUtil.min(mp_i, m_i));
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   * 
   * @param newID the id of the current object
   * @param processedIDs the already processed ids
   */
  private void step4(DBID newID, DBIDs processedIDs) {
    // for i = 1..n
    for(DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      D l_i = lambda.get(id);
      D lp_i = lambda.get(pi.get(id));

      // if L(i) >= L(P(i))
      if(l_i.compareTo(lp_i) >= 0) {
        // P(i) = n+1
        pi.put(id, newID);
      }
    }
  }

  private DBID lastObjectInCluster(DBID id, D stopdist, final DataStore<DBID> pi, final DataStore<D> lambda) {
    if(stopdist == null) {
      return id;
    }

    DBID currentID = id;
    while(lambda.get(currentID).compareTo(stopdist) < 1) {
      currentID = pi.get(currentID);
    }
    return currentID;
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
  private Clustering<DendrogramModel<D>> extractClusters(DBIDs ids, final DataStore<DBID> pi, final DataStore<D> lambda, int minclusters) {
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), logger) : null;

    // stopdist
    D stopdist = null;
    // sort by lambda
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    order.sort(new CompareByLambda<D>(lambda));
    int index = ids.size() - minclusters - 1;
    while(index >= 0) {
      if(lambda.get(order.get(index)).equals(lambda.get(order.get(index + 1)))) {
        index--;
      }
      else {
        stopdist = lambda.get(order.get(index));
        break;
      }
    }

    // extract the child clusters
    Map<DBID, ModifiableDBIDs> cluster_ids = new HashMap<DBID, ModifiableDBIDs>();
    Map<DBID, D> cluster_distances = new HashMap<DBID, D>();
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      DBID id = it.getDBID();
      DBID lastObjectInCluster = lastObjectInCluster(id, stopdist, pi, lambda);
      ModifiableDBIDs cluster = cluster_ids.get(lastObjectInCluster);
      if(cluster == null) {
        cluster = DBIDUtil.newArray();
        cluster_ids.put(lastObjectInCluster, cluster);
      }
      cluster.add(id);

      D lambda_id = lambda.get(id);
      if(stopdist != null && lambda_id.compareTo(stopdist) <= 0 && (cluster_distances.get(lastObjectInCluster) == null || lambda_id.compareTo(cluster_distances.get(lastObjectInCluster)) > 0)) {
        cluster_distances.put(lastObjectInCluster, lambda_id);
      }

      // Decrement counter
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    // build hierarchy
    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<DendrogramModel<D>>("Single-Link-Dendrogram", "slink-dendrogram");
    ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier = new HierarchyHashmapList<Cluster<DendrogramModel<D>>>();
    Cluster<DendrogramModel<D>> root = root(cluster_ids, cluster_distances, pi, lambda, hier, progress);
    dendrogram.addCluster(root);

    return dendrogram;
  }

  private Cluster<DendrogramModel<D>> root(Map<DBID, ModifiableDBIDs> cluster_ids, Map<DBID, D> cluster_distances, final DataStore<DBID> pi, final DataStore<D> lambda, ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier, FiniteProgress progress) {
    if(cluster_ids.size() == 1) {
      DBID id = cluster_ids.keySet().iterator().next();
      String name = "cluster_" + id + "_" + cluster_distances.get(id);
      return new Cluster<DendrogramModel<D>>(name, cluster_ids.get(id), new DendrogramModel<D>(cluster_distances.get(id)), hier);
    }

    // sort leafs by lambda
    List<Pair<DBID, D>> leafs = new ArrayList<Pair<DBID, D>>(cluster_ids.size());
    for(DBID id : cluster_ids.keySet()) {
      leafs.add(new Pair<DBID, D>(id, lambda.get(id)));
    }

    Collections.sort(leafs, new Comparator<Pair<DBID, D>>() {
      @Override
      public int compare(Pair<DBID, D> o1, Pair<DBID, D> o2) {
        D k1 = lambda.get(o1.first);
        D k2 = lambda.get(o2.first);
        if(k1 == null && k2 == null) {
          return 0;
        }
        else if(k1 == null) {
          return -1;
        }
        else if(k2 == null) {
          return 1;
        }
        else {
          return k1.compareTo(k2);
        }
      }
    });

    // create nodes of the dendrogram
    Cluster<DendrogramModel<D>> parent = null;
    Map<DBID, Cluster<DendrogramModel<D>>> nodes = new HashMap<DBID, Cluster<DendrogramModel<D>>>();
    int nodeCount = 0;
    int clusterCount = 0;
    while(!leafs.isEmpty()) {
      // left child
      Pair<DBID, D> leaf = leafs.remove(0);
      DBID leftID = leaf.first;
      Cluster<DendrogramModel<D>> left = nodes.get(leftID);
      if(left == null) {
        // String name = "cluster_" + leftID + "_" +
        // cluster_distances.get(leftID);
        String name = "cluster_" + (++clusterCount);
        left = new Cluster<DendrogramModel<D>>(name, cluster_ids.get(leftID), new DendrogramModel<D>(cluster_distances.get(leftID)), hier);
        nodes.put(leftID, left);
      }
      // right child
      DBID rightID = pi.get(leftID);
      if(leftID.sameDBID(rightID)) {
        break;
      }
      Cluster<DendrogramModel<D>> right = nodes.get(rightID);
      if(right == null) {
        // String name = "cluster_" + rightID + "_" +
        // cluster_distances.get(rightID);
        String name = "cluster_" + (++clusterCount);
        right = new Cluster<DendrogramModel<D>>(name, cluster_ids.get(rightID), new DendrogramModel<D>(cluster_distances.get(rightID)), hier);
        nodes.put(rightID, right);
      }
      // parent
      // String name = "node" + (++nodeCount) + "_" + leaf.second;
      String name = "node_" + (++nodeCount);
      parent = createParent(name, lastAncestor(left, hier), lastAncestor(right, hier), leaf.second, hier);

      // Decrement counter
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    // root = parent
    return parent;
  }

  /**
   * Determines recursively the last ancestor of the specified cluster.
   * 
   * @param cluster the child
   * @param hier the cluster hierarchy
   * @return the (currently) last ancestor
   */
  private Cluster<DendrogramModel<D>> lastAncestor(Cluster<DendrogramModel<D>> cluster, ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier) {
    List<Cluster<DendrogramModel<D>>> parents = hier.getParents(cluster);
    if(parents.isEmpty()) {
      return cluster;
    }
    else {
      if(parents.size() > 1) {
        logger.warning("More than one parent in Single-Link dendrogram: " + cluster + " parents: " + parents);
        return null;
      }
      return lastAncestor(parents.get(0), hier);
    }
  }

  private Cluster<DendrogramModel<D>> createParent(String name, Cluster<DendrogramModel<D>> leftChild, Cluster<DendrogramModel<D>> rightChild, D distance, ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier) {
    // DBIDs ids = DBIDUtil.union(leftChild.getIDs(), rightChild.getIDs());
    Cluster<DendrogramModel<D>> parent = new Cluster<DendrogramModel<D>>(name, DBIDUtil.EMPTYDBIDS, new DendrogramModel<D>(distance), hier);

    hier.add(parent, leftChild);
    hier.add(parent, rightChild);

    return parent;
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
  @SuppressWarnings("unused")
  private Clustering<Model> extractClusters_erich(DBIDs ids, final DataStore<DBID> pi, final DataStore<D> lambda, int minclusters) {
    // extract a hierarchical clustering
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    // sort by lambda
    order.sort(new CompareByLambda<D>(lambda));
    D curdist = null;

    D stopdist = null;
    if(minclusters < ids.size()) {
      stopdist = lambda.get(order.get(ids.size() - minclusters));
    }

    ModifiableHierarchy<Cluster<Model>> hier = new HierarchyHashmapList<Cluster<Model>>();
    Map<DBID, Cluster<Model>> clusters = new HashMap<DBID, Cluster<Model>>();
    Map<DBID, ModifiableDBIDs> cids = new HashMap<DBID, ModifiableDBIDs>();

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), logger) : null;

    for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
      DBID dest = pi.get(it);
      D l = lambda.get(it);
      // logger.debugFine("DBID " + cur.toString() + " dist: " + l.toString());
      if(stopdist != null && stopdist.compareTo(l) > 0) {
        DBID cur = it.getDBID();
        ModifiableDBIDs curset = cids.remove(cur);
        ModifiableDBIDs destset = cids.get(dest);
        if(destset == null) {
          if(curset != null) {
            destset = curset;
          }
          else {
            destset = DBIDUtil.newHashSet();
            destset.add(cur);
          }
          destset.add(dest);
          cids.put(dest, destset);
        }
        else {
          if(curset != null) {
            destset.addDBIDs(curset);
          }
          else {
            destset.add(cur);
          }
        }
        curdist = l;
      }
      else {
        if(curdist == null || l.compareTo(curdist) > 0) {
          // New distance level reached. Post-process the current objects
          for(Entry<DBID, ModifiableDBIDs> ent : cids.entrySet()) {
            DBID key = ent.getKey();
            ModifiableDBIDs clusids = ent.getValue();
            // Make a new cluster
            String cname = "Cluster_" + key.toString() + "_" + curdist.toString();
            Cluster<Model> cluster = new Cluster<Model>(cname, clusids, ClusterModel.CLUSTER, hier);
            // Collect child clusters and clean up the cluster ids, keeping only
            // "new" objects.
            for(DBIDMIter iter = clusids.iter(); iter.valid(); iter.advance()) {
              Cluster<Model> chiclus = clusters.get(iter);
              if(chiclus != null) {
                hier.add(cluster, chiclus);
                clusters.remove(iter);
                iter.remove();
              }
            }
            clusters.put(key, cluster);
          }
          if(logger.isDebuggingFine()) {
            StringBuffer buf = new StringBuffer();
            buf.append("Number of clusters at depth ");
            buf.append((curdist != null ? curdist.toString() : "null"));
            buf.append(": ").append(clusters.size()).append(" ");
            buf.append("last-objects:");
            for(DBID id : clusters.keySet()) {
              buf.append(" ").append(id.toString());
            }
            logger.debugFine(buf.toString());
          }
          cids.clear();
          curdist = l;
        }
        // Add the current object to the destinations cluster
        {
          ModifiableDBIDs destset = cids.get(dest);
          if(destset == null) {
            destset = DBIDUtil.newHashSet();
            cids.put(dest, destset);
            destset.add(dest);
          }
          destset.add(it);
        }
      }
      // Decrement counter
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    // There should be one cluster remaining at infinite distance...
    if(clusters.size() != 1) {
      logger.warning("Single-link is expected to have a single cluster at the top level!");
      return null;
    }
    final Clustering<Model> clustering = new Clustering<Model>("Single-Link-Clustering", "slink-clustering");
    // FIXME: validate this is works correctly for a single-object dataset!
    for(Cluster<Model> cluster : clusters.values()) {
      clustering.addCluster(cluster);
    }
    return clustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
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
  private static final class CompareByLambda<D extends Distance<D>> implements Comparator<DBID> {
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
    public int compare(DBID id1, DBID id2) {
      D k1 = lambda.get(id1);
      D k2 = lambda.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return k1.compareTo(k2);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected Integer minclusters = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter minclustersP = new IntParameter(SLINK_MINCLUSTERS_ID, new GreaterEqualConstraint(1), true);
      if(config.grab(minclustersP)) {
        minclusters = minclustersP.getValue();
      }
    }

    @Override
    protected SLINK<O, D> makeInstance() {
      return new SLINK<O, D>(distanceFunction, minclusters);
    }
  }
}