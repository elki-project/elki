package de.lmu.ifi.dbs.elki.algorithm;

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
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.pairs.CTriple;

/**
 * Algorithm to materialize all the distances in a data set.
 * 
 * The result can then be used with the DoubleDistanceParser and
 * MultipleFileInput to use cached distances.
 * 
 * Symmetry is assumed.
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("MaterializeDistances")
@Description("Materialize all distances in the data set to use as cached/precalculated data.")
public class MaterializeDistances<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<CTriple<DBID, DBID, Double>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MaterializeDistances.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction Parameterization
   */
  public MaterializeDistances(DistanceFunction<? super O, D> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Iterates over all points in the database.
   */
  public CollectionResult<CTriple<DBID, DBID, Double>> run(Database database, Relation<O> relation) {
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    final int size = relation.size();

    Collection<CTriple<DBID, DBID, Double>> r = new ArrayList<CTriple<DBID, DBID, Double>>(size * (size + 1) / 2);

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      for(DBIDIter iditer2 = relation.iterDBIDs(); iditer2.valid(); iditer2.advance()) {
        // skip inverted pairs
        if(iditer2.compareDBID(iditer) > 0) {
          continue;
        }
        double d = distFunc.distance(iditer, iditer2).doubleValue();
        r.add(new CTriple<DBID, DBID, Double>(iditer.getDBID(), iditer2.getDBID(), d));
      }
    }
    return new CollectionResult<CTriple<DBID, DBID, Double>>("Distance Matrix", "distance-matrix", r);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    @Override
    protected MaterializeDistances<O, D> makeInstance() {
      return new MaterializeDistances<O, D>(distanceFunction);
    }
  }
}