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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * SLOM: a new measure for local spatial outliers
 * <p>
 * Reference:
 * <p>
 * S. Chawla, P. Sun<br>
 * SLOM: a new measure for local spatial outliers<br>
 * Knowledge and Information Systems 9(4)
 * <p>
 * This implementation works around some corner cases in SLOM, in particular
 * when an object has none or a single neighbor only (albeit the results will
 * still not be too useful then), which will result in divisions by zero.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 * 
 * @param <N> the type the spatial neighborhood is defined over
 * @param <O> the type of objects handled by the algorithm
 */
@Title("SLOM: a new measure for local spatial outliers")
@Description("Spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
@Reference(authors = "S. Chawla, P. Sun", //
    title = "SLOM: a new measure for local spatial outliers", //
    booktitle = "Knowledge and Information Systems 9(4)", //
    url = "https://doi.org/10.1007/s10115-005-0200-2", //
    bibkey = "DBLP:journals/kais/ChawlaS06")
public class SLOM<N, O> extends AbstractDistanceBasedSpatialOutlier<N, O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SLOM.class);

  /**
   * Constructor.
   * 
   * @param npred Neighborhood predicate
   * @param nonSpatialDistanceFunction Distance function to use on the
   *        non-spatial attributes
   */
  public SLOM(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<O> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  /**
   * @param database Database to process
   * @param spatial Spatial Relation to use.
   * @param relation Relation to use.
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> spatial, Relation<O> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(database, spatial);
    DistanceQuery<O> distFunc = getNonSpatialDistanceFunction().instantiate(relation);

    WritableDoubleDataStore modifiedDistance = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    // calculate D-Tilde
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double sum = 0;
      double maxDist = 0;
      int cnt = 0;

      final DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iditer, iter)) {
          continue;
        }
        double dist = distFunc.distance(iditer, iter);
        sum += dist;
        cnt++;
        maxDist = Math.max(maxDist, dist);
      }
      if(cnt > 1) {
        modifiedDistance.putDouble(iditer, ((sum - maxDist) / (cnt - 1)));
      }
      else {
        // Use regular distance when the d-tilde trick is undefined.
        // Note: this can be 0 when there were no neighbors.
        modifiedDistance.putDouble(iditer, maxDist);
      }
    }

    // Second step - compute actual SLOM values
    DoubleMinMax slomminmax = new DoubleMinMax();
    WritableDoubleDataStore sloms = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double sum = 0;
      int cnt = 0;

      final DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iditer, iter)) {
          continue;
        }
        sum += modifiedDistance.doubleValue(iter);
        cnt++;
      }
      double slom;
      if(cnt > 0) {
        // With and without the object itself:
        double avgPlus = (sum + modifiedDistance.doubleValue(iditer)) / (cnt + 1);
        double avg = sum / cnt;

        double beta = 0;
        for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
          final double dist = modifiedDistance.doubleValue(iter);
          if(dist > avgPlus) {
            beta += 1;
          }
          else if(dist < avgPlus) {
            beta -= 1;
          }
        }
        // Include object itself
        if(!neighbors.contains(iditer)) {
          final double dist = modifiedDistance.doubleValue(iditer);
          if(dist > avgPlus) {
            beta += 1;
          }
          else if(dist < avgPlus) {
            beta -= 1;
          }
        }
        beta = Math.abs(beta);
        // note: cnt == size of N(x), not N+(x)
        if(cnt > 1) {
          beta = Math.max(beta, 1.0) / (cnt - 1);
        }
        else {
          // Workaround insufficiency in SLOM paper - div by zero
          beta = 1.0;
        }
        beta = beta / (1 + avg);

        slom = beta * modifiedDistance.doubleValue(iditer);
      }
      else {
        // No neighbors to compare to - no score.
        slom = 0.0;
      }
      sloms.putDouble(iditer, slom);
      slomminmax.put(slom);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("SLOM", "slom-outlier", sloms, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(slomminmax.getMin(), slomminmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @hidden
   * 
   * @param <N> Neighborhood type
   * @param <O> Data Object type
   */
  public static class Parameterizer<N, O> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, O> {
    @Override
    protected SLOM<N, O> makeInstance() {
      return new SLOM<>(npredf, distanceFunction);
    }
  }
}
