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
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Initialization method proposed by Park and Jun.
 * <p>
 * It is easy to imagine that this approach can become problematic, because it
 * does not take the distances between medoids into account. In the worst case,
 * it may choose k duplicates as initial centers, therefore we cannot recommend
 * this strategy, but it is provided for completeness.
 * <p>
 * Reference:
 * <p>
 * H.-S. Park, C.-H. Jun<br>
 * A simple and fast algorithm for K-medoids clustering<br>
 * Expert Systems with Applications 36(2)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type for KMedoids initialization
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "H.-S. Park, C.-H. Jun", //
    title = "A simple and fast algorithm for K-medoids clustering", //
    booktitle = "Expert Systems with Applications 36(2)", //
    url = "https://doi.org/10.1016/j.eswa.2008.01.039", //
    bibkey = "DBLP:journals/eswa/ParkJ09")
public class ParkInitialMeans<O> implements KMeansInitialization, KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ParkInitialMeans.class);

  /**
   * Constructor.
   */
  public ParkInitialMeans() {
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
    // Temporary storage arrays:
    WritableDoubleDataStore distsum = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    // First mean is chosen by having the smallest distance sum to all others.
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing distance sums", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double sum = 0;
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        sum += distQ.distance(iter, iter2);
      }
      distsum.putDouble(iter, sum);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // We use a heap to only store the best values.
    KNNHeap knn = DBIDUtil.newHeap(k);
    prog = LOG.isVerbose() ? new FiniteProgress("Computing element scores", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double sum = 0;
      for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
        sum += distQ.distance(iter, iter2) / distsum.doubleValue(iter2);
      }
      knn.insert(sum, iter);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    distsum.destroy();

    ArrayModifiableDBIDs medids = DBIDUtil.newArray(knn.toKNNList().subList(k));
    return medids;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends AbstractParameterizer {
    @Override
    protected ParkInitialMeans<V> makeInstance() {
      return new ParkInitialMeans<>();
    }
  }
}
