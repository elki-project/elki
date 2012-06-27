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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.Mean;
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
 * TODO: enforce using a distance matrix?
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
@Reference(title = "Clustering my means of Medoids", authors = "Kaufman, L. and Rousseeuw, P.J.", booktitle = "Statistical Data Analysis Based on the L_1–Norm and Related Methods")
public class PAMInitialMeans<V, D extends NumberDistance<D, ?>> implements KMeansInitialization<V>, KMedoidsInitialization<V> {
  /**
   * Constructor.
   */
  public PAMInitialMeans() {
    super();
  }

  @Override
  public List<V> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction) {
    // Get a distance query
    if(!(distanceFunction.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("PAM initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super V, D> distF = (PrimitiveDistanceFunction<? super V, D>) distanceFunction;
    final DistanceQuery<V, D> distQ = relation.getDatabase().getDistanceQuery(relation, distF);
    DBIDs medids = chooseInitialMedoids(k, distQ);
    List<V> medoids = new ArrayList<V>(k);
    for(DBIDIter iter = medids.iter(); iter.valid(); iter.advance()) {
      medoids.add(relation.get(iter));
    }
    return medoids;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DistanceQuery<? super V, ?> distQ2) {
    if(!(distQ2.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("PAM initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<? super V, D> distQ = (DistanceQuery<? super V, D>) distQ2;
    final DBIDs ids = distQ.getRelation().getDBIDs();

    ArrayModifiableDBIDs medids = DBIDUtil.newArray(k);
    double best = Double.POSITIVE_INFINITY;
    Mean mean = new Mean(); // Mean is numerically more stable than sum.
    WritableDoubleDataStore mindist = null;

    // First mean is chosen by having the smallest distance sum to all others.
    {
      DBID bestid = null;
      WritableDoubleDataStore bestd = null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        DBID id = iter.getDBID();
        WritableDoubleDataStore newd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
        mean.reset();
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          DBID other = iter2.getDBID();
          double d = distQ.distance(id, other).doubleValue();
          mean.put(d);
          newd.putDouble(other, d);
        }
        if(mean.getMean() < best) {
          best = mean.getMean();
          bestid = id;
          if(bestd != null) {
            bestd.destroy();
          }
          bestd = newd;
        }
        else {
          newd.destroy();
        }
      }
      medids.add(bestid);
      mindist = bestd;
    }
    assert (mindist != null);

    // Subsequent means optimize the full criterion.
    for(int i = 1; i < k; i++) {
      DBID bestid = null;
      WritableDoubleDataStore bestd = null;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        DBID id = iter.getDBID();
        if(medids.contains(id)) {
          continue;
        }
        WritableDoubleDataStore newd = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
        mean.reset();
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          DBID other = iter2.getDBID();
          double dn = distQ.distance(id, other).doubleValue();
          double v = Math.min(dn, mindist.doubleValue(other));
          mean.put(v);
          newd.put(other, v);
        }
        assert (mean.getCount() == ids.size());
        if(mean.getMean() < best) {
          best = mean.getMean();
          bestid = id;
          if(bestd != null) {
            bestd.destroy();
          }
          bestd = newd;
        }
        else {
          newd.destroy();
        }
      }
      if(bestid == null) {
        throw new AbortException("No median found that improves the criterion function?!?");
      }
      medids.add(bestid);
      mindist.destroy();
      mindist = bestd;
    }

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
  public static class Parameterizer<V, D extends NumberDistance<D, ?>> extends AbstractParameterizer {
    @Override
    protected PAMInitialMeans<V, D> makeInstance() {
      return new PAMInitialMeans<V, D>();
    }
  }
}