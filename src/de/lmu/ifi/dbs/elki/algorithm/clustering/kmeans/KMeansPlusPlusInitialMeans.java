package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
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
   * @param seed Random seed.
   */
  public KMeansPlusPlusInitialMeans(Long seed) {
    super(seed);
  }

  @Override
  public List<V> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction) {
    // Get a distance query
    if(!(distanceFunction.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("K-Means++ initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super V, D> distF = (PrimitiveDistanceFunction<? super V, D>) distanceFunction;
    DistanceQuery<V, D> distQ = relation.getDatabase().getDistanceQuery(relation, distF);

    // Chose first mean
    List<V> means = new ArrayList<V>(k);

    Random random = (seed != null) ? new Random(seed) : new Random();
    DBID first = DBIDUtil.deref(DBIDUtil.randomSample(relation.getDBIDs(), 1, random.nextLong()).iter());
    means.add(relation.get(first));

    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Initialize weights
    double[] weights = new double[ids.size()];
    double weightsum = initialWeights(weights, ids, first, distQ);
    while(means.size() < k) {
      if(weightsum > Double.MAX_VALUE) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - too many data points, too large squared distances?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      int pos = 0;
      while(r > 0 && pos < weights.length) {
        r -= weights[pos];
        pos++;
      }
      // Add new mean:
      DBID newmean = ids.get(pos);
      means.add(relation.get(newmean));
      // Update weights:
      weights[pos] = 0.0;
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

    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DistanceQuery<? super V, ?> distQ2) {
    if(!(distQ2.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("PAM initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<? super V, D> distQ = (DistanceQuery<? super V, D>) distQ2;
    // Chose first mean
    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);

    Random random = (seed != null) ? new Random(seed) : new Random();
    DBID first = DBIDUtil.deref(DBIDUtil.randomSample(distQ.getRelation().getDBIDs(), 1, random.nextLong()).iter());
    means.add(first);

    ArrayDBIDs ids = DBIDUtil.ensureArray(distQ.getRelation().getDBIDs());
    // Initialize weights
    double[] weights = new double[ids.size()];
    double weightsum = initialWeights(weights, ids, first, distQ);
    while(means.size() < k) {
      if(weightsum > Double.MAX_VALUE) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - too many data points, too large squared distances?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean for k-means++ - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      int pos = 0;
      while(r > 0 && pos < weights.length) {
        r -= weights[pos];
        pos++;
      }
      // Add new mean:
      DBID newmean = ids.get(pos);
      means.add(newmean);
      // Update weights:
      weights[pos] = 0.0;
      weightsum = updateWeights(weights, ids, newmean, distQ);
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
  protected double initialWeights(double[] weights, ArrayDBIDs ids, DBID latest, DistanceQuery<? super V, D> distQ) {
    double weightsum = 0.0;
    DBIDIter it = ids.iter();
    for(int i = 0; i < weights.length; i++, it.advance()) {
      if(DBIDUtil.equal(latest, it)) {
        weights[i] = 0.0;
      }
      else {
        double d = distQ.distance(latest, it).doubleValue();
        weights[i] = d * d;
      }
      weightsum += weights[i];
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
  protected double updateWeights(double[] weights, ArrayDBIDs ids, DBID latest, DistanceQuery<? super V, D> distQ) {
    double weightsum = 0.0;
    DBIDIter it = ids.iter();
    for(int i = 0; i < weights.length; i++, it.advance()) {
      if(weights[i] > 0.0) {
        double d = distQ.distance(latest, it).doubleValue();
        weights[i] = Math.min(weights[i], d * d);
        weightsum += weights[i];
      }
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
   * @return Weight sum
   */
  protected double updateWeights(double[] weights, ArrayDBIDs ids, DBID latest, PrimitiveDoubleDistanceFunction<V> distF, Relation<V> rel) {
    final V lv = rel.get(latest);
    double weightsum = 0.0;
    DBIDIter it = ids.iter();
    for(int i = 0; i < weights.length; i++, it.advance()) {
      if(weights[i] > 0.0) {
        double d = distF.doubleDistance(lv, rel.get(it));
        weights[i] = Math.min(weights[i], d * d);
        weightsum += weights[i];
      }
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
      return new KMeansPlusPlusInitialMeans<V, D>(seed);
    }
  }
}