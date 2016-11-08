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
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
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
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * MiniMax hierarchical clustering using the NNchain algorithm.
 * 
 * Reference:
 * <p>
 * F. Murtagh<br />
 * A survey of recent advances in hierarchical clustering algorithms<br />
 * The Computer Journal 26(4)
 * </p>
 * 
 * @author Julian Erhard
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    title = "A survey of recent advances in hierarchical clustering algorithms", //
    booktitle = "The Computer Journal 26(4)", //
    url = "http://dx.doi.org/10.1093/comjnl/26.4.354")
public class MiniMaxNNChain<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  private static final Logging LOG = Logging.getLogger(MiniMaxNNChain.class);

  public MiniMaxNNChain(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
  }

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
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids);
    TIntObjectHashMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>();

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
  private void nnChainCore(int size, double[] distances, DBIDArrayMIter prots, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters) {
    IntegerArray chain = new IntegerArray(size + 1); // The maximum chain size =
                                                     // number of ids + 1
    int a = -1;
    int b = -1;
    int c, x, y;
    double minDist = Double.POSITIVE_INFINITY;
    double dist;
    int lastIndex;

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running MiniMaxNNChain", size - 1, LOG) : null;
    for(int k = 0; k < size - 1; k++) {
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

        for(ix.seek(0); ix.valid(); ix.advance()) {
          if(!builder.isLinked(ix)) {
            b = ix.getOffset();
            if(a != b) {
              break;
            }
          }
        }
      }
      else {
        lastIndex = chain.size - 1;
        a = chain.get(lastIndex - 3);
        // Get the point that has been retained during the last merge.
        b = (chain.get(lastIndex - 2) < chain.get(lastIndex - 1)) ? chain.get(lastIndex - 2) : chain.get(lastIndex - 1);
        chain.remove(lastIndex - 2, 3);
      }
      do {
        c = b;
        minDist = getDistance(distances, b, a);
        for(int i = 0; i < size; i++) {
          if(i != a && !builder.isLinked(ix.seek(i))) {
            dist = getDistance(distances, i, a);
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
      while(!(chain.size() >= 3 && a == chain.get(chain.size - 1 - 2)));

      if(a < b) {
        x = b;
        y = a;
      }
      else {
        x = a;
        y = b;
      }
      MiniMax.merge(size, distances, prots, ix, iy, builder, clusters, dq, x, y);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
  }

  protected static double getDistance(double[] distances, int x, int y) {
    if(x > y) {
      return distances[AGNES.triangleSize(x) + y];
    }
    else if(y > x) {
      return distances[AGNES.triangleSize(y) + x];
    }
    else {
      return 0;
    }
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
    protected MiniMaxNNChain<O> makeInstance() {
      return new MiniMaxNNChain<>(distanceFunction);
    }
  }
}
