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
package elki.clustering.kmedoids.initialization;

import java.util.ArrayList;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
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
          // Swap mindist and newd:
          WritableDoubleDataStore temp = mindist;
          mindist = tempd;
          tempd = temp;
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      medids.add(bestid);
      cost[0] = best;
    }

    // For storing the clusters for refinement
    ArrayList<ModifiableDBIDs> clusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newArray((ids.size() / (i + 1)) + 10));
    }

    // Subsequent means optimize the full criterion.
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial centers", k, LOG) : null;
    LOG.incrementProcessed(prog); // First one was just chosen.
    for(int i = 1; i < k; i++) {
      double best = Double.POSITIVE_INFINITY;
      bestid.unset();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(medids.contains(iter)) {
          continue;
        }
        double sum = 0., v;
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          sum += v = MathUtil.min(distQ.distance(iter, iter2), mindist.doubleValue(iter2));
          tempd.put(iter2, v);
        }
        if(sum < best) {
          best = sum;
          bestid.set(iter);
          // Swap bestd and newd:
          WritableDoubleDataStore temp = bestd;
          bestd = tempd;
          tempd = temp;
        }
      }
      if(!bestid.isSet()) {
        throw new AbortException("No medoid found that improves the criterion function?!? Too many infinite distances.");
      }
      medids.add(bestid);
      // Swap bestd and mindist:
      WritableDoubleDataStore temp = bestd;
      bestd = mindist;
      mindist = temp;
      AlternateRefinement.assignToNearestCluster(medids, ids, distQ, clusters, cost);
      // Try to swap the medoid with a better cluster member:
      for(int j = 0; j < i; j++) {
        cost[j] = findMedoid(clusters.get(j), distQ, miter.seek(j), cost[j], temp, bestd, mindist);
      }
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
   * @param ci Cluster
   * @param distQ Distance query
   * @param miter Medoid iterator, pointing to the current medoid (modified)
   * @param bestm Prior cost, of the current assignment
   * @param temp Temporary storage for distances
   * @param temp2 Second temporary storage for distances
   * @param mindist Minimum distance (output)
   * @return New cost
   */
  private double findMedoid(ModifiableDBIDs ci, DistanceQuery<?> distQ, DBIDArrayMIter miter, double bestm, WritableDoubleDataStore temp, WritableDoubleDataStore temp2, WritableDoubleDataStore mindist) {
    boolean changed = false;
    for(DBIDIter iter = ci.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(miter, iter)) {
        continue;
      }
      double sum = 0;
      for(DBIDIter iter2 = ci.iter(); iter2.valid() && sum < bestm; iter2.advance()) {
        if(DBIDUtil.equal(iter, iter2)) {
          continue;
        }
        final double dist = distQ.distance(iter, iter2);
        sum += dist;
        temp.put(iter2, dist);
      }
      if(sum < bestm) {
        miter.setDBID(iter);
        bestm = sum;
        temp.put(iter, 0.);
        // Swap temporary storages:
        WritableDoubleDataStore temptemp = temp;
        temp2 = temp;
        temp = temptemp;
        changed = true;
      }
    }
    if(changed) {
      for(DBIDIter iter2 = ci.iter(); iter2.valid(); iter2.advance()) {
        mindist.put(iter2, temp2.doubleValue(iter2));
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
