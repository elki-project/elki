package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import gnu.trove.map.hash.TIntObjectHashMap;


public class MiniMax<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresenatationResult> implements HierarchicalClusteringAlgorithm {

  private static final Logging LOG = Logging.getLogger(MiniMax.class);

  public MiniMax(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

  public PointerPrototypeHierarchyRepresenatationResult run(Database db, Relation<O> relation) {
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction(), DatabaseQuery.HINT_OPTIMIZED_ONLY);
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    if(dq == null && ids instanceof DBIDRange) {
      LOG.verbose("Adding a distance matrix index to accelerate MiniMax.");
      PrecomputedDistanceMatrix<O> idx = new PrecomputedDistanceMatrix<O>(relation, getDistanceFunction());
      idx.initialize();
      dq = idx.getDistanceQuery(getDistanceFunction());
    }

    if(dq == null) {
      throw new AbortException("This implementation only supports StaticArrayDatabase.");
    }
    final int size = ids.size();

    WritableDBIDDataStore pi = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lambda = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.POSITIVE_INFINITY);
    WritableDBIDDataStore prototypes = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    TIntObjectHashMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>();
    final Logging log = getLogger();

    FiniteProgress progress = log.isVerbose() ? new FiniteProgress("Running MiniMax", size - 1, log) : null;

    double[] dists = new double[AGNES.triangleSize(size)];
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(AGNES.triangleSize(size));
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    // Initizalize lambda
    for(ix.seek(0); ix.valid(); ix.advance()) {
      pi.put(ix, ix);
      lambda.put(ix, Double.POSITIVE_INFINITY);
    }

    initializeMatrices(dists, prots, dq, ix, iy);

    for(int i = 1; i < size ; i++) {
      findMerge(size, dists, prots, ix, iy, pi, lambda, prototypes, clusters, dq);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);

    return new PointerPrototypeHierarchyRepresenatationResult(ids, pi, lambda, prototypes);
  }

  /** Initializes the inter-cluster distance matrix and the prototypes of possible merges
   * @param distances The cluster distance matrix
   * @param prots The array for the prototypes for merges
   * @param dq The distance query
   * @param ix An iterator over the data base
   * @param iy Another iterator over the data base
   */
  protected static <O> void initializeMatrices(double[] distances, ArrayModifiableDBIDs prots, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy) {
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        distances[pos] = dq.distance(ix, iy);
        prots.set(pos, ix);
        pos++;
      }
    }
  }

  protected static <O> void findMerge(int size, double[] distances, ArrayModifiableDBIDs prots, DBIDArrayIter ix, DBIDArrayIter iy, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDBIDDataStore prototypes, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;

    for(int dx = 0; dx < size; dx++) {

      // Skip if object is already linked
      if(lambda.doubleValue(ix.seek(dx)) < Double.POSITIVE_INFINITY) {
        continue;
      }

      for(int dy = 0; dy < dx; dy++) {
        // Skip if object is already linked
        if(lambda.doubleValue(ix.seek(dy)) < Double.POSITIVE_INFINITY) {
          continue;
        }

        int offset = AGNES.triangleSize(dx) + dy;
        if(distances[offset] < mindist) {
          mindist = distances[offset];
          x = dx;
          y = dy;
        }
      }
    }

    merge(size, distances, prots, ix, iy, pi, lambda, prototypes, clusters, dq, x, y);
  }

  /** Merges two clusters given by x, y, their points with smallest IDs,
   * and y to keep
   * 
   * @param size number of ids in the data set
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix iterator to reuse
   * @param iy iterator to reuse
   * @param pi parent data structure
   * @param lambda distance to parent data structure
   * @param prototypes prototype data store
   * @param clusters the clusters
   * @param dq  distance query of the data set
   * @param x first cluster to merge
   * @param y second cluster to merge
   */
  protected static <O> void merge(int size, double[] distances, ArrayModifiableDBIDs prots, DBIDArrayIter ix, DBIDArrayIter iy, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDBIDDataStore prototypes, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int x, int y) {
    int offset = AGNES.triangleSize(Math.max(x, y)) + Math.min(y, x);

    ix.seek(x);
    iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + distances[offset]);
    }

    ModifiableDBIDs cx = clusters.get(x);
    ModifiableDBIDs cy = clusters.get(y);

    // Keep y
    if(cy == null) {
      cy = DBIDUtil.newHashSet();
      cy.add(iy);
    }
    if(cx == null) {
      cy.add(ix);
    }
    else {
      cy.addDBIDs(cx);
      clusters.remove(x);
    }
    clusters.put(y, cy);

    // parent of x is set to y
    pi.put(ix, iy);

    lambda.put(ix, distances[offset]);

    prototypes.put(ix, prots.get(offset));

    updateMatrices(size, distances, prots, ix, iy, pi, lambda, prototypes, clusters, dq, y);
  }

  /** Update the entries of the matrices that contain a distance to c, the newly
   * merged cluster.
   * 
   * @param size number of ids in the data set
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix iterator to reuse
   * @param iy iterator to reuse
   * @param pi parent data structure
   * @param lambda distance to parent data structure
   * @param prototypes prototype data store
   * @param clusters the clusters
   * @param dq  distance query of the data set
   * @param c the cluster to update distances to
   */
  protected static <O> void updateMatrices(int size, double[] distances, ArrayModifiableDBIDs prots, DBIDArrayIter ix, DBIDArrayIter iy, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDBIDDataStore prototypes, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int c) {

    // c is the new cluster.
    // Update entries (at (x,y) with x > y) in the matrix where x = c or y = c

    // Update entries at (c,y) with y < c
    int x = c;
    int y = 0;
    ix.seek(x);
    for(; y < x; y++) {
      iy.seek(y);
      // Skip entry if already merged
      if(lambda.doubleValue(iy) < Double.POSITIVE_INFINITY) {
        continue;
      }
      updateEntry(distances, prots, ix, iy, pi, lambda, prototypes, clusters, dq, x, y);
    }

    // Update entries at (x,c) with x > c
    x = c + 1;
    y = c;
    iy.seek(y);
    for(; x < size; x++) {
      ix.seek(x);
      // Skip entry if already merged
      if(lambda.doubleValue(ix) < Double.POSITIVE_INFINITY) {
        continue;
      }
      updateEntry(distances, prots, ix, iy, pi, lambda, prototypes, clusters, dq, x, y);
    }
  }

  /** Update entry at x,y for distance matrix distances
   * 
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix  iterator to reuse
   * @param iy  iterator to reuse
   * @param pi  parent data structure
   * @param lambda  distance to parent data structure
   * @param prototypes  prototypes store data strucutre
   * @param clusters  the clusters
   * @param dq  distancequery on the data set
   * @param x index of cluster, x > y
   * @param y index of cluster, y < x
   * @param dimensions number of dimensions
   */
  protected static <O> void updateEntry(double[] distances, ArrayModifiableDBIDs prots, DBIDArrayIter ix, DBIDArrayIter iy, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDBIDDataStore prototypes, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int x, int y) {
    double minDist;
    double maxDist;
    double dist;
    
    assert( x > y);

    // Dummy initialization for the compiler
    DBID prototype = DBIDUtil.generateSingleDBID();
    ModifiableDBIDs cx;
    ModifiableDBIDs cy;

    minDist = Double.POSITIVE_INFINITY;

    cx = clusters.get(x);
    cy = clusters.get(y);

    if(cx == null) {
      cx = DBIDUtil.newHashSet();
      cx.add(ix);
    }
    if(cy == null) {
      cy = DBIDUtil.newHashSet();
      cy.add(iy);
    }

    // To find the prototype of cluster cx union cy, we first test all elements
    // of cluster cx
    findPrototype: for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {

      maxDist = Double.NEGATIVE_INFINITY;

      // Maximum distance of i to all elements in cx
      for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          // Optimization: continue with next i if this one cannot be a
          // prototype
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }
      // Maximum distance of i to all elements in cy
      for(DBIDIter j = cx.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }

      if(maxDist < minDist) {
        minDist = maxDist;
        prototype = DBIDUtil.deref(i);
      }
    }

    // To find the prototype of cluster cx union cy, we secondly test all
    // elements of
    // cluster cy
    findPrototype: for(DBIDIter i = cy.iter(); i.valid(); i.advance()) {

      maxDist = Double.NEGATIVE_INFINITY;

      // Maximum distance of i to all elements in cx
      for(DBIDIter j = cx.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }
      // Maximum distance of i to all elements in cy
      for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }

      if(maxDist < minDist) {
        minDist = maxDist;
        prototype = DBIDUtil.deref(i);
      }
    }

    distances[AGNES.triangleSize(x) + y] = minDist;
    prots.set(AGNES.triangleSize(x) + y, prototype);

  }
  
  /**
   * Return distance and prototype
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix  iterator to reuse
   * @param iy  iterator to reuse
   * @param pi  parent data structure
   * @param lambda  distance to parent data structure
   * @param prototypes  prototypes store data strucutre
   * @param clusters  the clusters
   * @param dq  distancequery on the data set
   * @param x index of cluster, x > y
   * @param y index of cluster, y < x
   * @param dimensions number of dimensions
   */
  public static <O> Pair<Double, DBID> getDistance(ArrayModifiableDBIDs prots, DBIDArrayIter ix, DBIDArrayIter iy, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int x, int y) {
    double minDist;
    double maxDist;
    double dist;
    
    assert( x > y);

    // Dummy initialization for the compiler
    DBID prototype = DBIDUtil.generateSingleDBID();
    ModifiableDBIDs cx;
    ModifiableDBIDs cy;

    minDist = Double.POSITIVE_INFINITY;

    cx = clusters.get(x);
    cy = clusters.get(y);
    ix.seek(x);
    iy.seek(y);

    if(cx == null) {
      cx = DBIDUtil.newHashSet();
      cx.add(ix);
    }
    if(cy == null) {
      cy = DBIDUtil.newHashSet();
      cy.add(iy);
    }

    // To find the prototype of cluster cx union cy, we first test all elements
    // of cluster cx
    findPrototype: for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {

      maxDist = Double.NEGATIVE_INFINITY;

      // Maximum distance of i to all elements in cx
      for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {

        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          // Optimization: continue with next i if this one cannot be a
          // prototype
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }
      // Maximum distance of i to all elements in cy
      for(DBIDIter j = cx.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }

      if(maxDist < minDist) {
        minDist = maxDist;
        prototype = DBIDUtil.deref(i);
      }
    }

    // To find the prototype of cluster cx union cy, we secondly test all
    // elements of
    // cluster cy
    findPrototype: for(DBIDIter i = cy.iter(); i.valid(); i.advance()) {

      maxDist = Double.NEGATIVE_INFINITY;

      // Maximum distance of i to all elements in cx
      for(DBIDIter j = cx.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }
      // Maximum distance of i to all elements in cy
      for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
        dist = dq.distance(i, j);
        if(dist > maxDist) {
          maxDist = dist;
          if(maxDist > minDist) {
            continue findPrototype;
          }
        }
      }

      if(maxDist < minDist) {
        minDist = maxDist;
        prototype = DBIDUtil.deref(i);
      }
    }

    return new Pair<Double, DBID>(minDist, prototype);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());

  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {

    @Override
    protected MiniMax<O> makeInstance() {
      return new MiniMax<>(distanceFunction);
    }

  }

}
