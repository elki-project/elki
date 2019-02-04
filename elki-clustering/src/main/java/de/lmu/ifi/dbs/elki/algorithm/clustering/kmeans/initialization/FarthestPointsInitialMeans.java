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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * K-Means initialization by repeatedly choosing the farthest point (by the
 * <em>minimum</em> distance to earlier points).
 *
 * Note: this is less random than other initializations, so running multiple
 * times will be more likely to return the same local minima.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type for kMedoids and kMedians
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.FarthestPointsInitialMeans")
public class FarthestPointsInitialMeans<O> extends AbstractKMeansInitialization implements KMedoidsInitialization<O> {
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
  public FarthestPointsInitialMeans(RandomFactory rnd, boolean dropfirst) {
    super(rnd);
    this.dropfirst = dropfirst;
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.POSITIVE_INFINITY);

    // Chose first mean
    double[][] means = new double[k][];

    DBIDRef first = DBIDUtil.randomSample(ids, rnd);
    NumberVector prevmean = relation.get(first);
    means[0] = prevmean.toArray();

    // Find farthest object each.
    DBIDVar best = DBIDUtil.newVar(first);
    for(int i = (dropfirst ? 0 : 1); i < k; i++) {
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        final double prev = store.doubleValue(it);
        if(prev != prev) {
          continue; // NaN: already chosen!
        }
        double val = Math.min(prev, distanceFunction.distance(prevmean, relation.get(it)));
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
    @SuppressWarnings("unchecked")
    final Relation<O> relation = (Relation<O>) distQ.getRelation();

    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.POSITIVE_INFINITY);

    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);

    DBIDRef first = DBIDUtil.randomSample(ids, rnd);
    DBIDVar prevmean = DBIDUtil.newVar(first);
    means.add(first);

    DBIDVar best = DBIDUtil.newVar(first);
    for(int i = (dropfirst ? 0 : 1); i < k; i++) {
      // Find farthest object:
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
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

    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractKMeansInitialization.Parameterizer {
    /**
     * Option ID to control the handling of the first object chosen.
     */
    public static final OptionID KEEPFIRST_ID = new OptionID("farthest.keepfirst", "Keep the first object chosen (which is chosen randomly) for the farthest points heuristic.");

    /**
     * Flag for discarding the first object chosen.
     */
    protected boolean keepfirst = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag dropfirstP = new Flag(KEEPFIRST_ID);
      if(config.grab(dropfirstP)) {
        keepfirst = dropfirstP.isTrue();
      }
    }

    @Override
    protected FarthestPointsInitialMeans<O> makeInstance() {
      return new FarthestPointsInitialMeans<>(rnd, !keepfirst);
    }
  }
}
