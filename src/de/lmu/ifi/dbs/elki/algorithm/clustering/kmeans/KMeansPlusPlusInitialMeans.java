package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * K-Means++ initialization for k-means.
 * 
 * Reference:
 * <p>
 * D. Arthur, S. Vassilvitskii<br />
 * k-means++: the advantages of careful seeding<br />
 * In: Proc. of the Eighteenth Annual ACM-SIAM Symposium on Discrete Algorithms,
 * SODA 2007
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
@Reference(authors = "D. Arthur, S. Vassilvitskii", title = "k-means++: the advantages of careful seeding", booktitle = "Proc. of the Eighteenth Annual ACM-SIAM Symposium on Discrete Algorithms, SODA 2007", url = "http://dx.doi.org/10.1145/1283383.1283494")
public class KMeansPlusPlusInitialMeans<V, D extends NumberDistance<D, ?>> extends AbstractKMeansInitialization<V> implements KMedoidsInitialization<V> {
  /**
   * Constructor.
   * 
   * @param rnd Random generator.
   */
  public KMeansPlusPlusInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public List<V> chooseInitialMeans(Database database, Relation<V> relation, int k, PrimitiveDistanceFunction<? super NumberVector<?>, ?> distanceFunction) {
    // Get a distance query
    if(!(distanceFunction.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("K-Means++ initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super V, D> distF = (PrimitiveDistanceFunction<? super V, D>) distanceFunction;
    DistanceQuery<V, D> distQ = database.getDistanceQuery(relation, distF);

    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);

    // Chose first mean
    List<V> means = new ArrayList<>(k);

    Random random = rnd.getSingleThreadedRandom();
    DBID first = DBIDUtil.deref(DBIDUtil.randomSample(ids, 1, random).iter());
    means.add(relation.get(first));

    // Initialize weights
    double weightsum = initialWeights(weights, ids, first, distQ);
    while(means.size() < k) {
      if(weightsum > Double.MAX_VALUE) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - too many data points, too large squared distances?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      DBIDIter it = ids.iter();
      for(; r > 0. && it.valid(); it.advance()) {
        double w = weights.doubleValue(it);
        if(w != w) {
          continue; // NaN: alrady chosen.
        }
        r -= w;
      }
      // Add new mean:
      final V newmean = relation.get(it);
      means.add(newmean);
      // Update weights:
      weights.putDouble(it, Double.NaN);
      // Choose optimized version for double distances, if applicable.
      if(distF instanceof PrimitiveDoubleDistanceFunction) {
        @SuppressWarnings("unchecked")
        PrimitiveDoubleDistanceFunction<V> ddist = (PrimitiveDoubleDistanceFunction<V>) distF;
        weightsum = updateWeights(weights, ids, newmean, ddist, relation);
      }
      else {
        weightsum = updateWeights(weights, ids, newmean, distQ);
      }
    }

    // Explicitly destroy temporary data.
    weights.destroy();

    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DistanceQuery<? super V, ?> distQ2) {
    if(!(distQ2.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("K-Means++ initialization initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<? super V, D> distQ = (DistanceQuery<? super V, D>) distQ2;
    @SuppressWarnings("unchecked")
    final Relation<V> rel = (Relation<V>) distQ.getRelation();

    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);

    DBIDs ids = rel.getDBIDs();
    WritableDoubleDataStore weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);

    Random random = rnd.getSingleThreadedRandom();
    DBIDRef first = DBIDUtil.randomSample(distQ.getRelation().getDBIDs(), 1, random).iter();
    means.add(first);

    // Initialize weights
    double weightsum = initialWeights(weights, ids, first, distQ);
    while(means.size() < k) {
      if(weightsum > Double.MAX_VALUE) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - too many data points, too large squared distances?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      DBIDIter it = ids.iter();
      for(; r > 0. && it.valid(); it.advance()) {
        double w = weights.doubleValue(it);
        if(w != w) {
          continue; // NaN: alrady chosen.
        }
        r -= w;
      }
      // Add new mean:
      means.add(it);
      // Update weights:
      weights.putDouble(it, Double.NaN);
      weightsum = updateWeights(weights, ids, rel.get(it), distQ);
    }

    return means;
  }

  /**
   * Initialize the weight list.
   * 
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  protected double initialWeights(WritableDoubleDataStore weights, DBIDs ids, DBIDRef latest, DistanceQuery<? super V, D> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      // Distance will usually already be squared
      double weight = distQ.distance(latest, it).doubleValue();
      weights.putDouble(it, weight);
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   * 
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  protected double updateWeights(WritableDoubleDataStore weights, DBIDs ids, V latest, DistanceQuery<? super V, D> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double weight = weights.doubleValue(it);
      if(weight != weight) {
        continue; // NaN: already chosen!
      }
      double newweight = distQ.distance(latest, it).doubleValue();
      if(newweight < weight) {
        weights.putDouble(it, newweight);
        weight = newweight;
      }
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   * 
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distF Distance function
   * @param rel Data relation
   * @return Weight sum
   */
  protected double updateWeights(WritableDoubleDataStore weights, DBIDs ids, V latest, PrimitiveDoubleDistanceFunction<V> distF, Relation<V> rel) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double weight = weights.doubleValue(it);
      if(weight != weight) {
        continue; // NaN: already chosen!
      }
      double newweight = distF.doubleDistance(latest, rel.get(it));
      if(newweight < weight) {
        weights.putDouble(it, newweight);
        weight = newweight;
      }
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V, D extends NumberDistance<D, ?>> extends AbstractKMeansInitialization.Parameterizer<V> {
    @Override
    protected KMeansPlusPlusInitialMeans<V, D> makeInstance() {
      return new KMeansPlusPlusInitialMeans<>(rnd);
    }
  }
}