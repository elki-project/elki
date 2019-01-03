/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Linear BUILD initialization for PAM (and k-means).
 * <p>
 * This is a O(nk) aproximation of the original PAM BUILD. For performance, it
 * uses an O(sqrt(n)) sample to achieve linear run time.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Peter J. Rousseeuw<br>
 * Faster k-Medoids Clustering:<br>
 * Improving the PAM, CLARA, and CLARANS Algorithms<br>
 * preprint, to appear
 *
 * @author Erich Schubert
 *
 * @param <O> Object type for KMedoids initialization
 */
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "preprint, to appear", //
    url = "https://arxiv.org/abs/1810.05691", //
    bibkey = "DBLP:journals/corr/abs-1810-05691")
public class LinearBUILDInitialMeans<O> implements KMeansInitialization, KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearBUILDInitialMeans.class);

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param rnd Random generator
   */
  public LinearBUILDInitialMeans(RandomFactory rnd) {
    super();
    this.rnd = rnd;
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    if(relation.size() < k) {
      throw new AbortException("Database has less than k objects.");
    }
    // Ugly cast; but better than code duplication.
    @SuppressWarnings("unchecked")
    Relation<O> rel = (Relation<O>) relation;
    // Get a distance query
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super O> distF = (PrimitiveDistanceFunction<? super O>) distanceFunction;
    final DistanceQuery<O> distQ = database.getDistanceQuery(rel, distF);
    DBIDs medids = chooseInitialMedoids(k, rel.getDBIDs(), distQ);
    double[][] medoids = new double[k][];
    DBIDIter iter = medids.iter();
    for(int i = 0; i < k; i++, iter.advance()) {
      medoids[i] = relation.get(iter).toArray();
    }
    return medoids;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    ArrayModifiableDBIDs medids = DBIDUtil.newArray(k);
    Random rand = rnd.getSingleThreadedRandom();
    // O(sqrt(n)) sample if k^2 < n.
    int ssize = Math.min(ids.size(), 10 + (int) Math.ceil(Math.sqrt(ids.size())));

    DBIDVar bestid = DBIDUtil.newVar();
    // We need three temporary storage arrays:
    WritableDoubleDataStore mindist, bestd, tempd;
    mindist = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    bestd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    tempd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    ArrayDBIDs sample = DBIDUtil.ensureArray(DBIDUtil.randomSample(ids, ssize, rand));
    DBIDArrayIter i = sample.iter(), j = sample.iter();
    // First mean is chosen by having the smallest distance sum to all others.
    {
      double best = Double.POSITIVE_INFINITY;
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial mean", sample.size(), LOG) : null;
      for(i.seek(0); i.valid(); i.advance()) {
        double sum = 0, d;
        for(j.seek(0); j.valid(); j.advance()) {
          sum += d = distQ.distance(i, j);
          tempd.putDouble(j, d);
        }
        if(sum < best) {
          best = sum;
          bestid.set(i);
          // Swap mindist and newd:
          WritableDoubleDataStore temp = mindist;
          mindist = tempd;
          tempd = temp;
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      medids.add(bestid);
    }

    // Subsequent means optimize the full criterion.
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial centers", k, LOG) : null;
    LOG.incrementProcessed(prog); // First one was just chosen.
    while(medids.size() < k) {
      double best = Double.POSITIVE_INFINITY;
      bestid.unset();
      for(i.seek(0); i.valid(); i.advance()) {
        if(medids.contains(i)) {
          continue;
        }
        double sum = 0., v;
        for(j.seek(0); j.valid(); j.advance()) {
          sum += v = MathUtil.min(distQ.distance(i, j), mindist.doubleValue(j));
          tempd.put(j, v);
        }
        if(sum < best) {
          best = sum;
          bestid.set(i);
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
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    mindist.destroy();
    bestd.destroy();
    tempd.destroy();
    return medids;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends AbstractKMeansInitialization.Parameterizer {
    @Override
    protected LinearBUILDInitialMeans<V> makeInstance() {
      return new LinearBUILDInitialMeans<>(rnd);
    }
  }
}
