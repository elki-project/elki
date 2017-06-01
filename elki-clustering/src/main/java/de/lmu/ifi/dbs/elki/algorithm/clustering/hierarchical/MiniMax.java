/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Minimax Linkage clustering.
 * 
 * Reference:
 * <p>
 * S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado and P. C.
 * Sham<br />
 * CLUSTAG: hierarchical clustering and graph methods for selecting tag
 * SNPs<br />
 * Bioinformatics, 21 (8)
 * </p>
 * <p>
 * J. Bien and R. Tibshirani<br >
 * Hierarchical Clustering with Prototypes via Minimax Linkage<br />
 * Journal of the American Statistical Association 106(495)
 * </p>
 * 
 * @author Julian Erhard
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Reference(authors = "S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado and P. C. Sham", //
    title = "CLUSTAG: hierarchical clustering and graph methods for selecting tag SNPs", //
    booktitle = "Bioinformatics, 21 (8)", //
    url = "http://dx.doi.org/10.1093/bioinformatics/bti201")
public class MiniMax<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class Logger.
   */
  private static final Logging LOG = Logging.getLogger(MiniMax.class);

  /**
   * Additional reference with detailed discussion.
   */
  @Reference(authors = "J. Bien and R. Tibshirani", //
      title = "Hierarchical Clustering with Prototypes via Minimax Linkage", //
      booktitle = "Journal of the American Statistical Association 106(495)", //
      url = "http://dx.doi.org/10.1198/jasa.2011.tm10183")
  public static final Void ADDITIONAL_REFERNECE = null;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use.
   */
  public MiniMax(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Run the algorithm on a database.
   * 
   * @param db Database
   * @param relation Relation to process.
   * @return Hierarchical result
   */
  public PointerPrototypeHierarchyRepresentationResult run(Database db, Relation<O> relation) {
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

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistanceFunction().isSquared());
    TIntObjectHashMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>(size);

    double[] dists = new double[AGNES.triangleSize(size)];
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(AGNES.triangleSize(size));
    DBIDArrayMIter protiter = prots.iter();
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    initializeMatrices(dists, prots, dq, ix, iy);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("MiniMax clustering", size - 1, LOG) : null;
    for(int i = 1; i < size; i++) {
      findMerge(size, dists, protiter, ix, iy, builder, clusters, dq);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    return (PointerPrototypeHierarchyRepresentationResult) builder.complete();
  }

  /**
   * Initializes the inter-cluster distance matrix of possible merges
   * 
   * @param distances The cluster distance matrix
   * @param dq The distance query
   * @param ix An iterator over the data base
   * @param iy Another iterator over the data base
   */
  protected static <O> void initializeMatrices(double[] distances, ArrayModifiableDBIDs prots, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy) {
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        distances[pos] = dq.distance(ix, iy);
        prots.add(iy);
        pos++;
      }
    }
    assert (prots.size() == pos);
  }

  /**
   * Find the best merge.
   * 
   * @param size Size
   * @param distances Distances array
   * @param prots Prototypes
   * @param ix Iterator (reused)
   * @param iy Iterator (reused)
   * @param builder Result builder
   * @param clusters Current clusters
   * @param dq Distance query
   */
  protected static void findMerge(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;

    for(int dx = 0; dx < size; dx++) {
      // Skip if object is already linked
      if(builder.isLinked(ix.seek(dx))) {
        continue;
      }

      for(int dy = 0; dy < dx; dy++) {
        // Skip if object is already linked
        if(builder.isLinked(iy.seek(dy))) {
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

    merge(size, distances, prots, ix, iy, builder, clusters, dq, x, y);
  }

  /**
   * Merges two clusters given by x, y, their points with smallest IDs, and y to
   * keep
   * 
   * @param size number of ids in the data set
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix iterator to reuse
   * @param iy iterator to reuse
   * @param builder Result builder
   * @param clusters the clusters
   * @param dq distance query of the data set
   * @param x first cluster to merge
   * @param y second cluster to merge
   */
  protected static void merge(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq, int x, int y) {
    assert (y < x);
    int offset = AGNES.triangleSize(x) + y;

    ix.seek(x);
    iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + distances[offset]);
    }

    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);

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
    builder.add(ix, distances[offset], iy, prots.seek(offset));

    updateMatrices(size, distances, prots, ix, iy, builder, clusters, dq, y);
  }

  /**
   * Update the entries of the matrices that contain a distance to c, the newly
   * merged cluster.
   * 
   * @param size number of ids in the data set
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix iterator to reuse
   * @param iy iterator to reuse
   * @param builder Result builder
   * @param clusters the clusters
   * @param dq distance query of the data set
   * @param c the cluster to update distances to
   */
  protected static <O> void updateMatrices(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int c) {
    // c is the new cluster.
    // Update entries (at (x,y) with x > y) in the matrix where x = c or y = c

    // Update entries at (c,y) with y < c
    ix.seek(c);
    for(iy.seek(0); iy.getOffset() < c; iy.advance()) {
      // Skip entry if already merged
      if(builder.isLinked(iy)) {
        continue;
      }
      updateEntry(distances, prots, ix, iy, clusters, dq, c, iy.getOffset());
    }

    // Update entries at (x,c) with x > c
    iy.seek(c);
    for(ix.seek(c + 1); ix.valid(); ix.advance()) {
      // Skip entry if already merged
      if(builder.isLinked(ix)) {
        continue;
      }
      updateEntry(distances, prots, ix, iy, clusters, dq, ix.getOffset(), c);
    }
  }

  /**
   * Update entry at x,y for distance matrix distances
   * 
   * @param distances distance matrix
   * @param prots calculated prototypes
   * @param ix iterator to reuse
   * @param iy iterator to reuse
   * @param clusters the clusters
   * @param dq distancequery on the data set
   * @param x index of cluster, x > y
   * @param y index of cluster, y < x
   * @param dimensions number of dimensions
   */
  protected static void updateEntry(double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq, int x, int y) {
    assert (y < x);
    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);

    DBIDVar prototype = DBIDUtil.newVar(ix.seek(x)); // Default prototype
    double minMaxDist = Double.POSITIVE_INFINITY;

    // Two "real" clusters:
    if(cx != null && cy != null) {
      minMaxDist = findPrototype(dq, cx, cy, prototype, minMaxDist);
      minMaxDist = findPrototype(dq, cy, cx, prototype, minMaxDist);
    }
    else if(cx != null) {
      // cy is singleton.
      minMaxDist = findPrototypeSingleton(dq, cx, iy.seek(y), prototype, minMaxDist);
    }
    else if(cy != null) {
      // cx is singleton.
      minMaxDist = findPrototypeSingleton(dq, cy, ix.seek(x), prototype, minMaxDist);
    }
    else {
      minMaxDist = dq.distance(ix.seek(x), iy.seek(y));
      prototype.set(ix);
    }

    final int offset = AGNES.triangleSize(x) + y;
    distances[offset] = minMaxDist;
    prots.seek(offset).setDBID(prototype);
  }

  /**
   * Find the prototypes.
   * 
   * @param dq Distance query
   * @param cx First set
   * @param cy Second set
   * @param prototype Prototype output variable
   * @param minMaxDist Previously best distance.
   * @return New best distance
   */
  private static double findPrototype(DistanceQuery<?> dq, DBIDs cx, DBIDs cy, DBIDVar prototype, double minMaxDist) {
    for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
      double maxDist = 0.;

      // Maximum distance of i to all elements in cy
      maxDist = findMax(dq, i, cy, maxDist, minMaxDist);
      if(maxDist >= minMaxDist) { // We already have an at least equally good
                                  // candidate.
        continue;
      }
      // Maximum distance of i to all elements in cx
      maxDist = findMax(dq, i, cx, maxDist, minMaxDist);

      // New best solution?
      if(maxDist < minMaxDist) {
        minMaxDist = maxDist;
        prototype.set(i);
      }
    }
    return minMaxDist;
  }

  /**
   * Find the prototypes.
   * 
   * @param dq Distance query
   * @param cx First set
   * @param cy Singleton object
   * @param prototype Prototype output variable
   * @param minMaxDist Previously best distance.
   * @return New best distance
   */
  private static double findPrototypeSingleton(DistanceQuery<?> dq, DBIDs cx, DBIDRef cy, DBIDVar prototype, double minMaxDist) {
    double maxDisty = 0.;
    for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
      // Maximum distance of i to the element in cy
      double maxDist = dq.distance(i, cy);
      // Maximum of distances from cy.
      maxDisty = (maxDist > maxDisty) ? maxDist : maxDisty;
      // We know a better solution already.
      if(maxDist >= minMaxDist) {
        continue;
      }
      // Maximum distance of i to all other elements in cx
      maxDist = findMax(dq, i, cx, maxDist, minMaxDist);

      if(maxDist < minMaxDist) {
        minMaxDist = maxDist;
        prototype.set(i);
      }
    }
    // Singleton point.
    if(maxDisty < minMaxDist) {
      minMaxDist = maxDisty;
      prototype.set(cy);
    }
    return minMaxDist;
  }

  /**
   * Find the maximum distance of one object to a set.
   *
   * @param dq Distance query
   * @param i Current object
   * @param cy Set of candidates
   * @param maxDist Known maximum to others
   * @param minMaxDist Early stopping threshold
   * @return Maximum distance
   */
  private static double findMax(DistanceQuery<?> dq, DBIDIter i, DBIDs cy, double maxDist, double minMaxDist) {
    for(DBIDIter j = cy.iter(); j.valid(); j.advance()) {
      double dist = dq.distance(i, j);
      if(dist > maxDist) {
        // Stop early, if we already know a better candidate.
        if(dist >= minMaxDist) {
          return dist;
        }
        maxDist = dist;
      }
    }
    return maxDist;
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
