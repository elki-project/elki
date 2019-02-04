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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Minimax Linkage clustering.
 * <p>
 * Reference:
 * <p>
 * S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado, P. C. Sham<br>
 * CLUSTAG: hierarchical clustering and graph methods for selecting tag SNPs<br>
 * Bioinformatics, 21 (8)
 * <p>
 * J. Bien and R. Tibshirani<br>
 * Hierarchical Clustering with Prototypes via Minimax Linkage<br>
 * Journal of the American Statistical Association 106(495)
 *
 * @author Julian Erhard
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - PointerPrototypeHierarchyRepresentationResult
 *
 * @param <O> Object type
 */
@Reference(authors = "S. I. Ao, K. Yip, M. Ng, D. Cheung, P.-Y. Fong, I. Melhado, P. C. Sham", //
    title = "CLUSTAG: hierarchical clustering and graph methods for selecting tag SNPs", //
    booktitle = "Bioinformatics, 21 (8)", //
    url = "https://doi.org/10.1093/bioinformatics/bti201", //
    bibkey = "DBLP:journals/bioinformatics/AoYNCFMS05")
@Reference(authors = "J. Bien, R. Tibshirani", //
    title = "Hierarchical Clustering with Prototypes via Minimax Linkage", //
    booktitle = "Journal of the American Statistical Association 106(495)", //
    url = "https://doi.org/10.1198/jasa.2011.tm10183", //
    bibkey = "doi:10.1198/jasa.2011.tm10183")
public class MiniMax<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class Logger.
   */
  private static final Logging LOG = Logging.getLogger(MiniMax.class);

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
    DistanceQuery<O> dq = DatabaseUtil.precomputedDistanceQuery(db, relation, getDistanceFunction(), LOG);
    final DBIDs ids = relation.getDBIDs();
    final int size = ids.size();

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistanceFunction().isSquared());
    Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>(size);

    // Allocate working space:
    MatrixParadigm mat = new MatrixParadigm(ids);
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(MatrixParadigm.triangleSize(size));
    initializeMatrices(mat, prots, dq);

    DBIDArrayMIter protiter = prots.iter();
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("MiniMax clustering", size - 1, LOG) : null;
    DBIDArrayIter ix = mat.ix;
    for(int i = 1, end = size; i < size; i++) {
      end = AGNES.shrinkActiveSet(ix, builder, end, //
          findMerge(end, mat, protiter, builder, clusters, dq));
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    return (PointerPrototypeHierarchyRepresentationResult) builder.complete();
  }

  /**
   * Initializes the inter-cluster distance matrix of possible merges
   * 
   * @param mat Matrix
   * @param dq The distance query
   */
  protected static <O> void initializeMatrices(MatrixParadigm mat, ArrayModifiableDBIDs prots, DistanceQuery<O> dq) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
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
   * @param mat Matrix view
   * @param prots Prototypes
   * @param builder Result builder
   * @param clusters Current clusters
   * @param dq Distance query
   * @return x, for shrinking the working set.
   */
  protected static int findMerge(int end, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;

    for(int dx = 0; dx < end; dx++) {
      // Skip if object is already linked
      if(builder.isLinked(ix.seek(dx))) {
        continue;
      }
      final int xoffset = MatrixParadigm.triangleSize(dx);

      for(int dy = 0; dy < dx; dy++) {
        // Skip if object is already linked
        if(builder.isLinked(iy.seek(dy))) {
          continue;
        }

        double dist = distances[xoffset + dy];
        if(dist < mindist) {
          mindist = dist;
          x = dx;
          y = dy;
        }
      }
    }

    assert (y < x);
    merge(end, mat, prots, builder, clusters, dq, x, y);
    return x;
  }

  /**
   * Merges two clusters given by x, y, their points with smallest IDs, and y to
   * keep
   * 
   * @param size number of ids in the data set
   * @param mat distance matrix
   * @param prots calculated prototypes
   * @param builder Result builder
   * @param clusters the clusters
   * @param dq distance query of the data set
   * @param x first cluster to merge
   * @param y second cluster to merge
   */
  protected static void merge(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq, int x, int y) {
    assert (y < x);
    final DBIDArrayIter ix = mat.ix.seek(x), iy = mat.iy.seek(y);
    final double[] distances = mat.matrix;
    int offset = MatrixParadigm.triangleSize(x) + y;

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

    updateMatrices(size, mat, prots, builder, clusters, dq, y);
  }

  /**
   * Update the entries of the matrices that contain a distance to c, the newly
   * merged cluster.
   * 
   * @param size number of ids in the data set
   * @param mat matrix paradigm
   * @param prots calculated prototypes
   * @param builder Result builder
   * @param clusters the clusters
   * @param dq distance query of the data set
   * @param c the cluster to update distances to
   */
  protected static <O> void updateMatrices(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, int c) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    // c is the new cluster.
    // Update entries (at (x,y) with x > y) in the matrix where x = c or y = c

    // Update entries at (c,y) with y < c
    ix.seek(c);
    for(iy.seek(0); iy.getOffset() < c; iy.advance()) {
      // Skip entry if already merged
      if(builder.isLinked(iy)) {
        continue;
      }
      updateEntry(mat, prots, clusters, dq, c, iy.getOffset());
    }

    // Update entries at (x,c) with x > c
    iy.seek(c);
    for(ix.seek(c + 1); ix.valid(); ix.advance()) {
      // Skip entry if already merged
      if(builder.isLinked(ix)) {
        continue;
      }
      updateEntry(mat, prots, clusters, dq, ix.getOffset(), c);
    }
  }

  /**
   * Update entry at x,y for distance matrix distances
   * 
   * @param mat distance matrix
   * @param prots calculated prototypes
   * @param clusters the clusters
   * @param dq distance query on the data set
   * @param x index of cluster, {@code x > y}
   * @param y index of cluster, {@code y < x}
   */
  protected static void updateEntry(MatrixParadigm mat, DBIDArrayMIter prots, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<?> dq, int x, int y) {
    assert (y < x);
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);

    DBIDVar prototype = DBIDUtil.newVar(ix.seek(x)); // Default prototype
    double minMaxDist;
    // Two "real" clusters:
    if(cx != null && cy != null) {
      minMaxDist = findPrototype(dq, cx, cy, prototype, Double.POSITIVE_INFINITY);
      minMaxDist = findPrototype(dq, cy, cx, prototype, minMaxDist);
    }
    else if(cx != null) {
      // cy is singleton.
      minMaxDist = findPrototypeSingleton(dq, cx, iy.seek(y), prototype);
    }
    else if(cy != null) {
      // cx is singleton.
      minMaxDist = findPrototypeSingleton(dq, cy, ix.seek(x), prototype);
    }
    else {
      minMaxDist = dq.distance(ix.seek(x), iy.seek(y));
      prototype.set(ix);
    }

    final int offset = MatrixParadigm.triangleSize(x) + y;
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
      // Maximum distance of i to all elements in cy
      double maxDist = findMax(dq, i, cy, 0., minMaxDist);
      if(maxDist >= minMaxDist) {
        // We already have an at least equally good candidate.
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
   * @return New best distance
   */
  private static double findPrototypeSingleton(DistanceQuery<?> dq, DBIDs cx, DBIDRef cy, DBIDVar prototype) {
    double maxDisty = 0., minMaxDist = Double.POSITIVE_INFINITY;
    for(DBIDIter i = cx.iter(); i.valid(); i.advance()) {
      // Maximum distance of i to the element in cy
      double maxDist = dq.distance(i, cy);
      // Maximum of distances from cy.
      maxDisty = (maxDist > maxDisty) ? maxDist : maxDisty;
      if(maxDist >= minMaxDist) {
        // We know a better solution already.
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

  /**
   * Parameterization class.
   *
   * @author Julian Erhard
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    @Override
    protected MiniMax<O> makeInstance() {
      return new MiniMax<>(distanceFunction);
    }
  }
}
