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
package elki.clustering.kmeans.initialization;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.NumberVector;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.random.RandomFactory;

/**
 * K-Means initialization by repeatedly choosing the farthest point (by the
 * <em>minimum</em> distance to earlier points).
 * <p>
 * Note: this is less random than other initializations, so running multiple
 * times will be more likely to return the same local minima.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type for kMedoids and kMedians
 */
public class FarthestPoints<O> extends AbstractKMeansInitialization implements KMedoidsInitialization<O> {
  /**
   * Discard the first vector.
   */
  boolean dropfirst = true;

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   * @param dropfirst Flag to discard the first vector.
   */
  public FarthestPoints(RandomFactory rnd, boolean dropfirst) {
    super(rnd);
    this.dropfirst = dropfirst;
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.POSITIVE_INFINITY);

    double[][] means = new double[k][];

    // Chose first mean
    DBIDRef first = DBIDUtil.randomSample(ids, rnd);
    NumberVector prevmean = relation.get(first);
    if (!dropfirst) {
      means[0] = prevmean.toArray();
    }

    // Find farthest object each.
    DBIDVar best = DBIDUtil.newVar(first);
    for(int i = (dropfirst ? 0 : 1); i < k; i++) {
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        final double prev = store.doubleValue(it);
        if(prev != prev) {
          continue; // NaN: already chosen!
        }
        double val = Math.min(prev, distance.distance(prevmean, relation.get(it)));
        // Don't store distance to first mean, when it will be dropped below.
        if(i > 0) {
          store.putDouble(it, val);
        }
        if(val > maxdist) {
          maxdist = val;
          best.set(it);
        }
      }
      // Add new mean (and drop the initial mean when desired)
      store.putDouble(best, Double.NaN); // So it won't be chosen twice.
      prevmean = relation.get(best);
      means[i] = prevmean.toArray();
    }

    // Explicitly destroy temporary data.
    store.destroy();
    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.POSITIVE_INFINITY);
    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);

    DBIDVar prevmean = DBIDUtil.newVar(DBIDUtil.randomSample(ids, rnd));
    DBIDVar best = DBIDUtil.newVar(prevmean);
    means.add(prevmean);

    for(int i = dropfirst ? 0 : 1; i < k; i++) {
      // Find farthest object:
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        final double prev = store.doubleValue(it);
        if(prev != prev) {
          continue; // NaN: already chosen!
        }
        double val = Math.min(prev, distQ.distance(prevmean, it));
        // Don't store distance to first mean, when it will be dropped below.
        if(i > 0) {
          store.putDouble(it, val);
        }
        if(val > maxdist) {
          maxdist = val;
          best.set(it);
        }
      }
      // Add new mean:
      if(i == 0) {
        means.clear(); // Remove temporary first element.
      }
      store.putDouble(best, Double.NaN); // So it won't be chosen twice.
      prevmean.set(best);
      means.add(best);
    }

    store.destroy();
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractKMeansInitialization.Par {
    /**
     * Option ID to control the handling of the first object chosen.
     */
    public static final OptionID KEEPFIRST_ID = new OptionID("farthest.keepfirst", "Keep the first object chosen (which is chosen randomly) for the farthest points heuristic.");

    /**
     * Flag for discarding the first object chosen.
     */
    protected boolean keepfirst = false;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new Flag(KEEPFIRST_ID).grab(config, x -> keepfirst = x);
    }

    @Override
    public FarthestPoints<O> make() {
      return new FarthestPoints<>(rnd, !keepfirst);
    }
  }
}
