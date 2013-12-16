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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * K-Means initialization by repeatedly choosing the farthest point.
 * 
 * Note: this is less random than other initializations, so running multiple
 * times will be more likely to return the same local minima.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <D> Distance type
 */
public class FarthestPointsInitialMeans<V, D extends NumberDistance<D, ?>> extends AbstractKMeansInitialization<V> implements KMedoidsInitialization<V> {
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
  public List<V> chooseInitialMeans(Database database, Relation<V> relation, int k, PrimitiveDistanceFunction<? super NumberVector<?>, ?> distanceFunction) {
    // Get a distance query
    if(!(distanceFunction.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("Farthest points K-Means initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    final PrimitiveDistanceFunction<? super V, D> distF = (PrimitiveDistanceFunction<? super V, D>) distanceFunction;
    DistanceQuery<V, D> distQ = database.getDistanceQuery(relation, distF);

    // Chose first mean
    List<V> means = new ArrayList<>(k);

    DBIDIter first = DBIDUtil.randomSample(relation.getDBIDs(), 1, rnd).iter();
    means.add(relation.get(first));

    DBIDVar best = DBIDUtil.newVar(first);
    for(int i = (dropfirst ? 0 : 1); i < k; i++) {
      // Find farthest object:
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        double dsum = 0.;
        for(V ex : means) {
          dsum += distQ.distance(ex, it).doubleValue();
        }
        if(dsum > maxdist) {
          maxdist = dsum;
          best.set(it);
        }
      }
      // Add new mean:
      if(k == 0) {
        means.clear(); // Remove temporary first element.
      }
      means.add(relation.get(best));
    }

    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DistanceQuery<? super V, ?> distQ2) {
    if(!(distQ2.getDistanceFactory() instanceof NumberDistance)) {
      throw new AbortException("Farthest points K-Means initialization can only be used with numerical distances.");
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<? super V, D> distQ = (DistanceQuery<? super V, D>) distQ2;
    final Relation<?> relation = distQ.getRelation();
    // Chose first mean
    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);

    DBIDIter first = DBIDUtil.randomSample(relation.getDBIDs(), 1, rnd).iter();
    means.add(first);

    DBIDVar best = DBIDUtil.newVar(first);
    for(int i = (dropfirst ? 0 : 1); i < k; i++) {
      // Find farthest object:
      double maxdist = Double.NEGATIVE_INFINITY;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        double dsum = 0.;
        for(DBIDIter ex = means.iter(); ex.valid(); ex.advance()) {
          dsum += distQ.distance(ex, it).doubleValue();
        }
        if(dsum > maxdist) {
          maxdist = dsum;
          best.set(it);
        }
      }
      // Add new mean:
      if(k == 0) {
        means.clear(); // Remove temporary first element.
      }
      means.add(best);
    }

    return means;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V, D extends NumberDistance<D, ?>> extends AbstractKMeansInitialization.Parameterizer<V> {
    /**
     * Option ID to control the handling of the first object chosen.
     */
    public static final OptionID DROPFIRST_ID = new OptionID("farthest.dropfirst", "Drop the first object chosen (which is chosen randomly) for the farthest points heuristic.");

    /**
     * Flag for discarding the first object chosen.
     */
    protected boolean dropfirst = true;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag dropfirstP = new Flag(DROPFIRST_ID);
      if(config.grab(dropfirstP)) {
        dropfirst = dropfirstP.isTrue();
      }
    }

    @Override
    protected FarthestPointsInitialMeans<V, D> makeInstance() {
      return new FarthestPointsInitialMeans<>(rnd, dropfirst);
    }
  }
}
