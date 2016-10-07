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

import java.util.LinkedList;
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
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
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import gnu.trove.map.hash.TIntObjectHashMap;

public class MiniMaxNNChain<O> extends AbstractDistanceBasedAlgorithm<O, PointerPrototypeHierarchyRepresenatationResult> implements HierarchicalClusteringAlgorithm {

  private static final Logging LOG = Logging.getLogger(MiniMaxNNChain.class);
  private FiniteProgress progress;

  public MiniMaxNNChain(DistanceFunction<? super O> distanceFunction) {
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

    progress = log.isVerbose() ? new FiniteProgress("Running MiniMaxNNChain", size - 1, log) : null;

    double[] dists = new double[AGNES.triangleSize(size)];
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(AGNES.triangleSize(size));
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    // Initizalize lambda
    for(ix.seek(0); ix.valid(); ix.advance()) {
      pi.put(ix, ix);
      lambda.put(ix, Double.POSITIVE_INFINITY);
    }

    MiniMax.initializeMatrices(dists, prots, dq, ix, iy);

    nnChainCore(size, dists, prots, dq, ix, iy, pi, lambda, prototypes, clusters);
    
    LOG.ensureCompleted(progress);

    return new PointerPrototypeHierarchyRepresenatationResult(ids, pi, lambda, prototypes);
  }

  /**
   * Uses NNChain as in
   * "Modern hierarchical, agglomerative clustering algorithms" Daniel Müllner
   * 
   * @param size
   * @param distances
   * @param prots
   */
  private void nnChainCore(int size, double[] distances, ArrayModifiableDBIDs prots, DistanceQuery<O> dq, DBIDArrayIter ix, DBIDArrayIter iy, WritableDBIDDataStore pi, WritableDoubleDataStore lambda, WritableDBIDDataStore prototypes, TIntObjectHashMap<ModifiableDBIDs> clusters) {
    IntegerArray chain = new IntegerArray(size);
    int a = -1;
    int b = -1;
    int c, x, y;
    double minDist = Double.POSITIVE_INFINITY;
    double dist;
    int lastIndex;
    
    for(int k=0; k < size-1; k++) {
      if(chain.size() <= 3) {
       
        
        // Accessing two arbitrary not yet merged elements could be optimized to work in O(1) like in Müllner; 
        // however this usually does not have a huge impact (empirically just about 1/5000 of total performance) 
        for(ix.seek(0); ix.valid(); ix.advance()) {
          if(lambda.doubleValue(ix) == Double.POSITIVE_INFINITY) {
            a = ix.getOffset();
            break;
          }
        }
        
        chain.clear();
        chain.add(a);
        
        for(ix.seek(0); ix.valid(); ix.advance()) {
          if(lambda.doubleValue(ix) == Double.POSITIVE_INFINITY) {
            b = ix.getOffset();
            if(a == b) {
              continue;
            }
            else {
              break;
            }
          }
        }
      }
      else {
        
        lastIndex = chain.size-1;
        a = chain.get(lastIndex - 3);
        // Get the point that has been retained during the last merge.
        b = (chain.get(lastIndex - 2)<chain.get(lastIndex - 1))? chain.get(lastIndex - 2) : chain.get(lastIndex -1);
        chain.remove(lastIndex-2, 3);
      }
      do {
        c = b;
        minDist = getDistance(distances, b, a);
        for(int i = 0; i < size; i++) {
          if(i != a && lambda.doubleValue(ix.seek(i))==Double.POSITIVE_INFINITY) {
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
      while(!(chain.size() >= 3 && a == chain.get(chain.size -1 -2)));

      if(a < b) {
        x = b;
        y = a;
      }
      else {
        assert (a > b);
        x = a;
        y = b;
      }
      MiniMax.merge(size, distances, prots, ix, iy, pi, lambda, prototypes, clusters, dq, x, y);
      LOG.incrementProcessed(progress);
    }
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
