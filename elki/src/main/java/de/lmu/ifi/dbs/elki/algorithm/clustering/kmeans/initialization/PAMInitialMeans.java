package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * PAM initialization for k-means (and of course, PAM).
 *
 * Reference:
 * <p>
 * Clustering my means of Medoids<br />
 * Kaufman, L. and Rousseeuw, P.J.<br />
 * in: Statistical Data Analysis Based on the L_1–Norm and Related Methods
 * </p>
 *
 * @author Erich Schubert
 *
 * @param <O> Object type for KMedoids initialization
 */
@Reference(title = "Clustering my means of Medoids", //
authors = "Kaufman, L. and Rousseeuw, P.J.", //
booktitle = "Statistical Data Analysis Based on the L_1–Norm and Related Methods")
public class PAMInitialMeans<O> implements KMeansInitialization<NumberVector>, KMedoidsInitialization<O> {
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
  public <T extends NumberVector, V extends NumberVector> List<V> chooseInitialMeans(Database database, Relation<T> relation, int k, NumberVectorDistanceFunction<? super T> distanceFunction, NumberVector.Factory<V> factory) {
    // Ugly cast; but better than code duplication.
    @SuppressWarnings("unchecked")
    Relation<O> rel = (Relation<O>) relation;
    // Get a distance query
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super O> distF = (PrimitiveDistanceFunction<? super O>) distanceFunction;
    final DistanceQuery<O> distQ = database.getDistanceQuery(rel, distF);
    DBIDs medids = chooseInitialMedoids(k, rel.getDBIDs(), distQ);
    List<V> medoids = new ArrayList<>(k);
    for(DBIDIter iter = medids.iter(); iter.valid(); iter.advance()) {
      medoids.add(factory.newNumberVector(relation.get(iter)));
    }
    return medoids;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    ArrayModifiableDBIDs medids = DBIDUtil.newArray(k);
    DBIDVar bestid = DBIDUtil.newVar();
    WritableDoubleDataStore mindist = null;

    // First mean is chosen by having the smallest distance sum to all others.
    {
      double best = Double.POSITIVE_INFINITY;
      WritableDoubleDataStore newd = null;
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial mean", ids.size(), LOG) : null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(newd == null) {
          newd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
        }
        int sum = 0;
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          double d = distQ.distance(iter, iter2);
          sum += d;
          newd.putDouble(iter2, d);
        }
        if(sum < best) {
          best = sum;
          bestid.set(iter);
          if(mindist != null) {
            mindist.destroy();
          }
          mindist = newd;
          newd = null;
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      if(newd != null) {
        newd.destroy();
      }
      medids.add(bestid);
    }
    assert(mindist != null);

    // Subsequent means optimize the full criterion.
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Choosing initial centers", k, LOG) : null;
    LOG.incrementProcessed(prog); // First one was just chosen.
    for(int i = 1; i < k; i++) {
      double best = Double.POSITIVE_INFINITY;
      WritableDoubleDataStore bestd = null, newd = null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(medids.contains(iter)) {
          continue;
        }
        if(newd == null) {
          newd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
        }
        double sum = 0.;
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          double v = MathUtil.min(distQ.distance(iter, iter2), mindist.doubleValue(iter2));
          sum += v;
          newd.put(iter2, v);
        }
        if(sum < best) {
          best = sum;
          bestid.set(iter);
          if(bestd != null) {
            bestd.destroy();
          }
          bestd = newd;
          newd = null;
        }
      }
      if(bestd == null) {
        throw new AbortException("No median found that improves the criterion function?!? Too many infinite distances.");
      }
      medids.add(bestid);
      if(newd != null) {
        newd.destroy();
      }
      mindist.destroy();
      mindist = bestd;
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    mindist.destroy();
    return medids;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractParameterizer {
    @Override
    protected PAMInitialMeans<V> makeInstance() {
      return new PAMInitialMeans<>();
    }
  }
}