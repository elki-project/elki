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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * PAM initialization for k-means (and of course, for PAM).
 * <p>
 * Reference:
 * <p>
 * L. Kaufman, P. J. Rousseeuw<br>
 * Clustering by means of Medoids<br>
 * in: Statistical Data Analysis Based on the L1-Norm and Related Methods
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type for KMedoids initialization
 */
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Clustering by means of Medoids", //
    booktitle = "Statistical Data Analysis Based on the L1-Norm and Related Methods", //
    bibkey = "books/misc/KauRou87")
@Reference(authors = "L. Kaufman, P. J. Rousseeuw", //
    title = "Partitioning Around Medoids (Program PAM)", //
    booktitle = "Finding Groups in Data: An Introduction to Cluster Analysis", //
    url = "https://doi.org/10.1002/9780470316801.ch2", //
    bibkey = "doi:10.1002/9780470316801.ch2")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.PAMInitialMeans")
public class PAMInitialMeans<O> implements KMeansInitialization, KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PAMInitialMeans.class);

  /**
   * Constructor.
   */
  public PAMInitialMeans() {
    super();
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
    DBIDVar bestid = DBIDUtil.newVar();
    // We need three temporary storage arrays:
    WritableDoubleDataStore mindist, bestd, tempd;
    mindist = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    bestd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    tempd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

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
  public static class Parameterizer<V> extends AbstractParameterizer {
    @Override
    protected PAMInitialMeans<V> makeInstance() {
      return new PAMInitialMeans<>();
    }
  }
}
