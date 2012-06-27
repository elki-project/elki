package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;
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

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The Spatial Outlier Factor (SOF) is a spatial
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF LOF} variation.
 * 
 * Since the "reachability distance" of LOF cannot be used canonically in the
 * bichromatic case, this part of LOF is dropped and the exact distance is used
 * instead.
 * 
 * <p>
 * Huang, T., Qin, X.<br>
 * Detecting outliers in spatial database.<br>
 * In: Proc. 3rd International Conference on Image and Graphics,
 * Hong Kong, China.
 * </p>
 * 
 * A LOF variation simplified with reachDist(o,p) == dist(o,p).
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood object type
 * @param <O> Attribute object type
 * @param <D> Distance type
 */
@Title("Spatial Outlier Factor")
@Reference(authors = "Huang, T., Qin, X.", title = "Detecting outliers in spatial database", booktitle = "Proc. 3rd International Conference on Image and Graphics", url = "http://dx.doi.org/10.1109/ICIG.2004.53")
public class SOF<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier<N, O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SOF.class);

  /**
   * Constructor.
   * 
   * @param npred Neighborhood predicate
   * @param nonSpatialDistanceFunction Distance function on non-spatial
   *        attributes
   */
  public SOF(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The main run method
   * 
   * @param database Database to use (actually unused)
   * @param spatial Relation for neighborhood
   * @param relation Attributes to evaluate
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<N> spatial, Relation<O> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);
    DistanceQuery<O, D> distFunc = getNonSpatialDistanceFunction().instantiate(relation);

    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax lofminmax = new DoubleMinMax();

    // Compute densities
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double avg = 0;
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        avg += distFunc.distance(id, iter.getDBID()).doubleValue();
      }
      double lrd = 1 / (avg / neighbors.size());
      if (Double.isNaN(lrd)) {
        lrd = 0;
      }
      lrds.putDouble(id, lrd);
    }

    // Compute density quotients
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double avg = 0;
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        avg += lrds.doubleValue(iter.getDBID());
      }
      final double lrd = (avg / neighbors.size()) / lrds.doubleValue(id);
      if (!Double.isNaN(lrd)) {
        lofs.putDouble(id, lrd);
        lofminmax.put(lrd);
      } else {
        lofs.putDouble(id, 0.0);
      }
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<Double>("Spatial Outlier Factor", "sof-outlier", TypeUtil.DOUBLE, lofs, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood type
   * @param <O> Attribute object type
   * @param <D> Distance type
   */
  public static class Parameterizer<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, O, D> {
    @Override
    protected SOF<N, O, D> makeInstance() {
      return new SOF<N, O, D>(npredf, distanceFunction);
    }
  }
}