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
package elki.clustering.kmedoids.initialization;

import java.util.Arrays;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Initialization method for k-medoids that combines the Greedy (PAM
 * {@link BUILD}) with "alternate" refinement steps. After choosing a new
 * medoid, all points are assigned to the nearest center, and the medoid of the
 * resulting partitions is used.
 * <p>
 * Reference:
 * <p>
 * M. Eugénia Captivo<br>
 * Fast primal and dual heuristics for the p-median location problem<br>
 * European Journal of Operational Research 52(1)
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type for KMedoids initialization
 */
@Reference(authors = "M. Eugénia Captivo", //
    title = "Fast primal and dual heuristics for the p-median location problem", //
    booktitle = "European Journal of Operational Research 52(1)", //
    url = "https://doi.org/10.1016/0377-2217(91)90336-T", //
    bibkey = "doi:10.1016/0377-2217(91)90336-T")
public class GreedyG<O> implements KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GreedyG.class);

  /**
   * Constructor.
   */
  public GreedyG() {
    super();
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    DBIDVar bestid = DBIDUtil.newVar();
    // We need three temporary storage arrays:
    WritableDoubleDataStore mindist, bestd, tempd;
    mindist = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    bestd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    tempd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableIntegerDataStore mina, besta, tempa;
    mina = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0);
    besta = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0);
    tempa = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0);
    // TODO: the code would become much nicer if we had a DoubleIntegerStorage,
    // and if we added "swap" or "copy" methods to the API.

    ArrayModifiableDBIDs medids = DBIDUtil.newArray(k);
    DBIDArrayMIter miter = medids.iter();
    double[] cost = new double[k];
    // First mean is chosen by having the smallest distance sum to all others.
    {
      double best = Double.POSITIVE_INFINITY;
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial mean", ids.size(), LOG) : null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double sum = 0, d;
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          sum += d = distQ.distance(iter, iter2);
          tempd.putDouble(iter2, d);
        }
        if(sum < best) {
          best = sum;
          bestid.set(iter);
          // Swap mindist and tempd
          WritableDoubleDataStore swap = mindist;
          mindist = tempd;
          tempd = swap;
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      medids.add(bestid);
      cost[0] = best;
    }

    ModifiableDBIDs tmpids = DBIDUtil.newArray(ids.size());
    boolean[] changed = new boolean[k];
    // Subsequent means optimize the full criterion.
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial centers", k, LOG) : null;
    LOG.incrementProcessed(prog); // First one was just chosen.
    for(int i = 1; i < k; i++) {
      double besttd = Double.POSITIVE_INFINITY, besti = 0;
      bestid.unset();
      for(DBIDIter cand = ids.iter(); cand.valid(); cand.advance()) {
        if(medids.contains(cand) || mindist.doubleValue(cand) == 0) {
          continue;
        }
        double td = 0, sum = 0.;
        for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
          final double mdist = mindist.doubleValue(it);
          final double dist = distQ.distance(cand, it);
          if(dist < mdist) {
            sum += dist;
            tempd.put(it, dist);
            tempa.put(it, i);
          }
          else {
            td += mdist;
            tempd.put(it, mdist);
            tempa.put(it, mina.intValue(it));
          }
        }
        td += sum;
        if(td < besttd) {
          besttd = td;
          besti = sum;
          bestid.set(cand);
          // Swap bestd and tempd, besta and tempa
          {
            WritableDoubleDataStore swap = bestd;
            bestd = tempd;
            tempd = swap;
          }
          {
            WritableIntegerDataStore swap = besta;
            besta = tempa;
            tempa = swap;
          }
        }
      }
      if(!bestid.isSet()) {
        throw new AbortException("No medoid found that improves the criterion function?!? Too many infinite distances.");
      }
      medids.add(bestid);
      Arrays.fill(changed, false);
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        int p = mina.intValue(it);
        if(p != besta.intValue(it)) {
          changed[p] = true;
        }
      }
      // Swap bestd and mindist, besta and preva
      {
        WritableDoubleDataStore swap = mindist;
        mindist = bestd;
        bestd = swap;
      }
      {
        WritableIntegerDataStore swap = mina;
        mina = besta;
        besta = swap;
      }
      // Try to swap the medoid with a better cluster member:
      for(int j = 0; j < i; j++) {
        if(changed[j]) {
          tmpids.clear();
          for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
            if(mina.intValue(it) == j) {
              tmpids.add(it);
            }
          }
          assert !tmpids.isEmpty();
          cost[j] = findMedoid(tmpids, distQ, j, miter.seek(j), cost[j], tempd, bestd, mindist);
        }
      }
      cost[i] = besti;
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    mindist.destroy();
    bestd.destroy();
    tempd.destroy();
    return medids;
  }

  /**
   * Find the best medoid of a given fixed set.
   * 
   * @param j Cluster number
   * @param ids Object ids
   * @param distQ Distance query
   * @param miter Medoid iterator, pointing to the current medoid (modified)
   * @param bestm Prior cost, of the current assignment
   * @param temp Temporary storage for distances
   * @param tempbest Second temporary storage for distances
   * @param mindist Minimum distance (output)
   * @return New cost
   */
  public static double findMedoid(DBIDs ids, DistanceQuery<?> distQ, int j, DBIDArrayMIter miter, double bestm, WritableDoubleDataStore temp, WritableDoubleDataStore tempbest, WritableDoubleDataStore mindist) {
    assert tempbest != temp;
    boolean changed = false;
    for(DBIDIter cand = ids.iter(); cand.valid(); cand.advance()) {
      double sum = 0, dist;
      for(DBIDIter it = ids.iter(); it.valid() && sum < bestm; it.advance()) {
        if(DBIDUtil.equal(cand, it)) {
          continue;
        }
        sum += dist = distQ.distance(cand, it);
        temp.put(it, dist);
      }
      if(sum < bestm) {
        miter.setDBID(cand);
        bestm = sum;
        temp.put(cand, 0.); // New medoid was skipped above
        // Swap temporary storages:
        {
          WritableDoubleDataStore swap = tempbest;
          tempbest = temp;
          temp = swap;
        }
        changed = true;
      }
    }
    if(changed) {
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        mindist.put(it, tempbest.doubleValue(it));
      }
    }
    return bestm;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V> implements Parameterizer {
    @Override
    public GreedyG<V> make() {
      return new GreedyG<>();
    }
  }
}
