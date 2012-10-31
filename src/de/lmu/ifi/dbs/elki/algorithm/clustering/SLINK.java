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

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDistanceDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDistanceDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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

/**
 * Implementation of the efficient Single-Link Algorithm SLINK of R. Sibson.
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
  private static final Logging LOG = Logging.getLogger(SLINK.class);

  /**
   * Minimum number of clusters to extract
   */
  private int minclusters = -1;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param minclusters Minimum clusters to extract. Can be {@code -1}.
   */
  public SLINK(DistanceFunction<? super O, D> distanceFunction, int minclusters) {
    super(distanceFunction);
    this.minclusters = minclusters;
  }

  /**
   * Performs the SLINK algorithm on the given database.
   */
  public Result run(Database database, Relation<O> relation) {
    DistanceQuery<O, D> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    @SuppressWarnings("unchecked")
    Class<D> distCls = (Class<D>) getDistanceFunction().getDistanceFactory().getClass();
    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDataStore<D> lambda = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, distCls);
    // Temporary storage for m.
    WritableDataStore<D> m = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, distCls);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running SLINK", relation.size(), LOG) : null;
    // has to be an array for monotonicity reasons!
    ModifiableDBIDs processedIDs = DBIDUtil.newArray(relation.size());

    // Optimized code path for double distances
    if (getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction && lambda instanceof WritableDoubleDistanceDataStore && m instanceof WritableDoubleDistanceDataStore) {
      @SuppressWarnings("unchecked")
      PrimitiveDoubleDistanceFunction<? super O> dist = (PrimitiveDoubleDistanceFunction<? super O>) getDistanceFunction();
      WritableDoubleDistanceDataStore lambdad = (WritableDoubleDistanceDataStore) lambda;
      WritableDoubleDistanceDataStore md = (WritableDoubleDistanceDataStore) m;
      // apply the algorithm
      for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
        step1double(id, pi, lambdad);
        step2double(id, processedIDs, distQuery.getRelation(), dist, md);
        step3double(id, pi, lambdad, processedIDs, md);
        step4double(id, pi, lambdad, processedIDs);

        processedIDs.add(id);

        if (progress != null) {
          progress.incrementProcessed(LOG);
        }
      }
    } else {
      // apply the algorithm
      for (DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
        step1(id, pi, lambda);
        step2(id, processedIDs, distQuery, m);
        step3(id, pi, lambda, processedIDs, m);
        step4(id, pi, lambda, processedIDs);

        processedIDs.add(id);

        if (progress != null) {
          progress.incrementProcessed(LOG);
        }
      }
    }

    if (progress != null) {
      progress.ensureCompleted(LOG);
    }
    // We don't need m anymore.
    m.destroy();
    m = null;

    // Build dendrogam clusters identified by their target object
    if (LOG.isVerbose()) {
      LOG.verbose("Extracting clusters.");
    }
    final BasicResult result;
    if (lambda instanceof DoubleDistanceDataStore) {
      result = extractClustersDouble(relation.getDBIDs(), pi, (DoubleDistanceDataStore) lambda, minclusters);
    } else {
      result = extractClusters(relation.getDBIDs(), pi, lambda, minclusters);
    }

    result.addChildResult(new MaterializedRelation<DBID>("SLINK pi", "slink-order", TypeUtil.DBID, pi, processedIDs));
    result.addChildResult(new MaterializedRelation<D>("SLINK lambda", "slink-order", new SimpleTypeInformation<D>(distCls), lambda, processedIDs));
    result.addChildResult(new OrderingFromDataStore<D>("SLINK order", "slink-order", processedIDs, lambda));
    return result;
  }

  /**
   * First step: Initialize P(id) = id, L(id) = infinity.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   */
  private void step1(DBIDRef id, WritableDBIDDataStore pi, WritableDataStore<D> lambda) {
    // P(n+1) = n+1:
    pi.put(id, id);
    // L(n+1) = infinity
    lambda.put(id, getDistanceFunction().getDistanceFactory().infiniteDistance());
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param m Data store
   * @param distFunc Distance function to use
   */
  private void step2(DBIDRef id, DBIDs processedIDs, DistanceQuery<O, D> distFunc, WritableDataStore<D> m) {
    O newObj = distFunc.getRelation().get(id);
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      // M(i) = dist(i, n+1)
      m.put(it, distFunc.distance(it, newObj));
    }
  }

  /**
   * Third step: Determine the values for P and L
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   * @param m Data store
   */
  private void step3(DBIDRef id, WritableDBIDDataStore pi, WritableDataStore<D> lambda, DBIDs processedIDs, WritableDataStore<D> m) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      D l_i = lambda.get(it);
      D m_i = m.get(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
      D mp_i = m.get(p_i);

      // if L(i) >= M(i)
      if (l_i.compareTo(m_i) >= 0) {
        // M(P(i)) = min { M(P(i)), L(i) }
        m.put(p_i, DistanceUtil.min(mp_i, l_i));

        // L(i) = M(i)
        lambda.put(it, m_i);

        // P(i) = n+1;
        pi.put(it, id);
      } else {
        // M(P(i)) = min { M(P(i)), M(i) }
        m.put(p_i, DistanceUtil.min(mp_i, m_i));
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   * 
   * @param id the id of the current object
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   */
  private void step4(DBIDRef id, WritableDBIDDataStore pi, WritableDataStore<D> lambda, DBIDs processedIDs) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      D l_i = lambda.get(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
      D lp_i = lambda.get(p_i);

      // if L(i) >= L(P(i))
      if (l_i.compareTo(lp_i) >= 0) {
        // P(i) = n+1
        pi.put(it, id);
      }
    }
  }

  /**
   * First step: Initialize P(id) = id, L(id) = infinity.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   */
  private void step1double(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDistanceDataStore lambda) {
    // P(n+1) = n+1:
    pi.put(id, id);
    // L(n+1) = infinity
    lambda.putDouble(id, Double.POSITIVE_INFINITY);
  }

  /**
   * Second step: Determine the pairwise distances from all objects in the
   * pointer representation to the new object with the specified id.
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param processedIDs the already processed ids
   * @param m Data store
   * @param relation Data relation
   * @param distFunc Distance function to use
   */
  private void step2double(DBIDRef id, DBIDs processedIDs, Relation<? extends O> relation, PrimitiveDoubleDistanceFunction<? super O> distFunc, WritableDoubleDistanceDataStore m) {
    O newObj = relation.get(id);
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      // M(i) = dist(i, n+1)
      m.putDouble(it, distFunc.doubleDistance(relation.get(it), newObj));
    }
  }

  /**
   * Third step: Determine the values for P and L
   * 
   * @param id the id of the object to be inserted into the pointer
   *        representation
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   * @param m Data store
   */
  private void step3double(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDistanceDataStore lambda, DBIDs processedIDs, WritableDoubleDistanceDataStore m) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      double l_i = lambda.doubleValue(it);
      double m_i = m.doubleValue(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
      double mp_i = m.doubleValue(p_i);

      // if L(i) >= M(i)
      if (l_i >= m_i) {
        // M(P(i)) = min { M(P(i)), L(i) }
        m.putDouble(p_i, Math.min(mp_i, l_i));

        // L(i) = M(i)
        lambda.putDouble(it, m_i);

        // P(i) = n+1;
        pi.put(it, id);
      } else {
        // M(P(i)) = min { M(P(i)), M(i) }
        m.putDouble(p_i, Math.min(mp_i, m_i));
      }
    }
  }

  /**
   * Fourth step: Actualize the clusters if necessary
   * 
   * @param id the id of the current object
   * @param pi Pi data store
   * @param lambda Lambda data store
   * @param processedIDs the already processed ids
   */
  private void step4double(DBIDRef id, WritableDBIDDataStore pi, WritableDoubleDistanceDataStore lambda, DBIDs processedIDs) {
    DBIDVar p_i = DBIDUtil.newVar();
    // for i = 1..n
    for (DBIDIter it = processedIDs.iter(); it.valid(); it.advance()) {
      double l_i = lambda.doubleValue(it);
      pi.assignVar(it, p_i); // p_i = pi(it)
      double lp_i = lambda.doubleValue(p_i);

      // if L(i) >= L(P(i))
      if (l_i >= lp_i) {
        // P(i) = n+1
        pi.put(it, id);
      }
    }
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
    D nulldist = getDistanceFunction().getDistanceFactory().nullDistance();

    // Sort DBIDs by lambda. We need this for two things:
    // a) to determine the stop distance from "minclusters" parameter
    // b) to process arrows in decreasing / increasing order
    ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
    order.sort(new CompareByLambda<D>(lambda));

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
    ArrayList<ModifiableDBIDs> cluster_dbids = new ArrayList<ModifiableDBIDs>(expcnum);
    ArrayList<D> cluster_dist = new ArrayList<D>(expcnum);
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
    Cluster<DendrogramModel<D>> root = null;
    ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier = new HierarchyHashmapList<Cluster<DendrogramModel<D>>>();
    ArrayList<Cluster<DendrogramModel<D>>> clusters = new ArrayList<Cluster<DendrogramModel<D>>>(ids.size() + expcnum - split);
    // Convert initial clusters to cluster objects
    {
      int i = 0;
      for (DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
        clusters.add(makeCluster(it2, cluster_dist.get(i), cluster_dbids.get(i), hier));
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
        clus = makeCluster(it, nulldist, cids, hier);
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
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS, hier);
          hier.add(pclus, clusters.get(parentid));
          hier.add(pclus, clus);
          clusters.set(parentid, pclus); // Replace existing parent cluster
        } else {
          // Create a new, one-element, parent cluster.
          parentid = cnum;
          cnum++;
          ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
          cids.add(succ);
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, cids, hier);
          hier.add(pclus, clus);
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
    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<DendrogramModel<D>>("Single-Link-Dendrogram", "slink-dendrogram");
    dendrogram.addCluster(root);

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
    D nulldist = getDistanceFunction().getDistanceFactory().nullDistance();

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
    ArrayList<ModifiableDBIDs> cluster_dbids = new ArrayList<ModifiableDBIDs>(expcnum);
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
    Cluster<DendrogramModel<D>> root = null;
    ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier = new HierarchyHashmapList<Cluster<DendrogramModel<D>>>();
    ArrayList<Cluster<DendrogramModel<D>>> clusters = new ArrayList<Cluster<DendrogramModel<D>>>(ids.size() + expcnum - split);
    // Convert initial clusters to cluster objects
    {
      int i = 0;
      for (DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
        @SuppressWarnings("unchecked")
        D depth = (D) new DoubleDistance(cluster_dist.get(i));
        clusters.add(makeCluster(it2, depth, cluster_dbids.get(i), hier));
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
        clus = makeCluster(it, nulldist, cids, hier);
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
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS, hier);
          hier.add(pclus, clusters.get(parentid));
          hier.add(pclus, clus);
          clusters.set(parentid, pclus); // Replace existing parent cluster
        } else {
          // Create a new, one-element, parent cluster.
          parentid = cnum;
          cnum++;
          ArrayModifiableDBIDs cids = DBIDUtil.newArray(1);
          cids.add(succ);
          Cluster<DendrogramModel<D>> pclus = makeCluster(succ, depth, cids, hier);
          hier.add(pclus, clus);
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
    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<DendrogramModel<D>>("Single-Link-Dendrogram", "slink-dendrogram");
    dendrogram.addCluster(root);

    return dendrogram;
  }

  /**
   * Make the cluster for the given object
   * 
   * @param lead Leading object
   * @param depth Linkage depth
   * @param members Member objects
   * @param hier Cluster hierarchy
   * @return Cluster
   */
  private Cluster<DendrogramModel<D>> makeCluster(DBIDRef lead, D depth, DBIDs members, ModifiableHierarchy<Cluster<DendrogramModel<D>>> hier) {
    final String name;
    if (members.size() == 0) {
      name = "merge_" + lead + "_" + depth;
    } else if (depth.isInfiniteDistance()) {
      assert (members.contains(lead));
      name = "object_" + lead;
    } else {
      name = "cluster_" + lead + "_" + depth;
    }
    Cluster<DendrogramModel<D>> cluster = new Cluster<DendrogramModel<D>>(name, members, new DendrogramModel<D>(depth), hier);
    return cluster;
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
   * 
   * @param <D> Distance type
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
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * The minimum number of clusters to extract
     */
    public static final OptionID SLINK_MINCLUSTERS_ID = OptionID.getOrCreateOptionID("slink.minclusters", "The maximum number of clusters to extract.");

    protected int minclusters = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter minclustersP = new IntParameter(SLINK_MINCLUSTERS_ID);
      minclustersP.addConstraint(new GreaterEqualConstraint(1));
      minclustersP.setOptional(true);
      if (config.grab(minclustersP)) {
        minclusters = minclustersP.intValue();
      }
    }

    @Override
    protected SLINK<O, D> makeInstance() {
      return new SLINK<O, D>(distanceFunction, minclusters);
    }
  }
}
