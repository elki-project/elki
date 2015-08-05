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
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * MiniMax hierarchical clustering using the NNchain algorithm.
 * 
 * Reference:
 * <p>
 * F. Murtagh<br />
 * A survey of recent advances in hierarchical clustering algorithms<br />
 * The Computer Journal 26(4)
 * </p>
 * <p>
 * D. Müllner<br />
 * Modern hierarchical, agglomerative clustering algorithms<br />
 * arXiv preprint arXiv:1109.2378
 * </p>
 *
 * @author Julian Erhard
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    title = "A survey of recent advances in hierarchical clustering algorithms", //
    booktitle = "The Computer Journal 26(4)", //
    url = "http://dx.doi.org/10.1093/comjnl/26.4.354")
public class MiniMaxNNChain<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MiniMaxNNChain.class);

  /**
   * Additional literature:
   */
  @Reference(authors = "D. Müllner", //
      title = "Modern hierarchical, agglomerative clustering algorithms", //
      booktitle = "arXiv preprint arXiv:1109.2378", //
      url = "https://arxiv.org/abs/1109.2378")
  public static final Void ADDITIONAL_REFERNCE = null;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   */
  public MiniMaxNNChain(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Run the algorithm
   * 
   * @param db Database to run on
   * @param relation Data relation
   * @return Clustering result
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

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistanceFunction().isSquared());
    Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>();

    final int size = ids.size();
    double[] dists = new double[AGNES.triangleSize(size)];
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(AGNES.triangleSize(size));
    DBIDArrayMIter protiter = prots.iter();
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    MiniMax.initializeMatrices(dists, prots, dq, ix, iy);

    nnChainCore(size, dists, protiter, dq, ix, iy, builder, clusters);

    return (PointerPrototypeHierarchyRepresentationResult) builder.complete();
  }

  
  /**
   * Uses NNChain as in "Modern hierarchical, agglomerative clustering
   * algorithms" by Daniel Müllner
   * 
   * @param size number of ids in the data set
   * @param distances distance matrix
   * @param prots computed prototypes
   * @param dq distance query of the data set
   * @param ix iterator to reuse
   * @param iy another iterator to reuse
   * @param builder Result builder
   * @param clusters current clusters
   */
  private void nnChainCore(int size, double[] distances, DBIDArrayMIter prots, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters) {
    // The maximum chain size = number of ids + 1
    IntegerArray chain = new IntegerArray(size + 1);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running MiniMax-NNChain", size - 1, LOG) : null;
    for(int k = 0; k < size - 1; k++) {
      int a = -1, b = -1;
      if(chain.size() <= 3) {
        // Accessing two arbitrary not yet merged elements could be optimized to
        // work in O(1) like in Müllner;
        // however this usually does not have a huge impact (empirically just
        // about 1/5000 of total performance)
        for(ix.seek(0); ix.valid(); ix.advance()) {
          if(!builder.isLinked(ix)) {
            a = ix.getOffset();
            break;
          }
        }

        chain.clear();
        chain.add(a);

        for(ix.advance(); ix.valid(); ix.advance()) {
          if(!builder.isLinked(ix)) {
            b = ix.getOffset();
            assert(a != b);
            break;
          }
        }
      }
      else {
        // Chain is expected to look like (.... a, b, c, b) with b and c merged.
        int lastIndex = chain.size;
        int c = chain.get(lastIndex - 2);
        b = chain.get(lastIndex - 3);
        a = chain.get(lastIndex - 4);
        // Ensure we had a loop at the end:
        assert (chain.get(lastIndex - 1) == c || chain.get(lastIndex - 1) == b);
        // if c < b, then we merged b -> c, otherwise c -> b
        b = c < b ? c : b;
        // Cut the tail:
        chain.size -= 3;
      }
      // For ties, always prefer the second-last element b:
      double minDist = NNChain.getDistance(distances, a, b);
      do {
        int c = b;
        final int ta = NNChain.triangleSize(a);
        for(int i = 0; i < a; i++) {
          if(i != b && !builder.isLinked(ix.seek(i))) {
            double dist = distances[ta + i];
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }
        for(int i = a + 1; i < size; i++) {
          if(i != b && !builder.isLinked(ix.seek(i))) {
            double dist = distances[NNChain.triangleSize(i) + a];
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }

        b = a;
        a = c;

        chain.add(a);
      }
      while(chain.size() < 3 || a != chain.get(chain.size - 1 - 2));

      // We always merge the larger into the smaller index:
      if(a < b) {
        int tmp = a;
        a = b;
        b = tmp;
      }
      assert(minDist == NNChain.getDistance(distances, a, b));
      MiniMax.merge(size, distances, prots, ix, iy, builder, clusters, dq, a, b);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
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
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    @Override
    protected MiniMaxNNChain<O> makeInstance() {
      return new MiniMaxNNChain<>(distanceFunction);
    }
  }
}
