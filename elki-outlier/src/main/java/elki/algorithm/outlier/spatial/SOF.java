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
package elki.algorithm.outlier.spatial;

import elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.distancefunction.PrimitiveDistanceFunction;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.Metadata;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * The Spatial Outlier Factor (SOF) is a spatial
 * {@link elki.algorithm.outlier.lof.LOF LOF} variation.
 * <p>
 * Since the "reachability distance" of LOF cannot be used canonically in the
 * bichromatic case, this part of LOF is dropped and the exact distance is used
 * instead.
 * <p>
 * Reference:
 * <p>
 * T. Huang, X. Qin<br>
 * Detecting outliers in spatial database<br>
 * Proc. 3rd International Conference on Image and Graphics
 * <p>
 * A LOF variation simplified with reachDist(o,p) == dist(o,p).
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <N> Neighborhood object type
 * @param <O> Attribute object type
 */
@Title("Spatial Outlier Factor")
@Reference(authors = "T. Huang, X. Qin", //
    title = "Detecting outliers in spatial database", //
    booktitle = "Proc. 3rd International Conference on Image and Graphics", //
    url = "https://doi.org/10.1109/ICIG.2004.53", //
    bibkey = "DBLP:conf/icig/HuangQ04")
public class SOF<N, O> extends AbstractDistanceBasedSpatialOutlier<N, O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SOF.class);

  /**
   * Constructor.
   * 
   * @param npred Neighborhood predicate
   * @param nonSpatialDistanceFunction Distance function on non-spatial
   *        attributes
   */
  public SOF(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<O> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(database, spatial);
    DistanceQuery<O> distFunc = getNonSpatialDistanceFunction().instantiate(relation);

    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax lofminmax = new DoubleMinMax();

    // Compute densities
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      double avg = 0;
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        avg += distFunc.distance(iditer, iter);
      }
      double lrd = 1 / (avg / neighbors.size());
      if(Double.isNaN(lrd)) {
        lrd = 0;
      }
      lrds.putDouble(iditer, lrd);
    }

    // Compute density quotients
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      double avg = 0;
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        avg += lrds.doubleValue(iter);
      }
      final double lrd = (avg / neighbors.size()) / lrds.doubleValue(iditer);
      if(!Double.isNaN(lrd)) {
        lofs.putDouble(iditer, lrd);
        lofminmax.put(lrd);
      }
      else {
        lofs.putDouble(iditer, 0.0);
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Spatial Outlier Factor", relation.getDBIDs(), lofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(or).addChild(npred);
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
   * @hidden
   * 
   * @param <N> Neighborhood type
   * @param <O> Attribute object type
   */
  public static class Parameterizer<N, O> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, O> {
    @Override
    protected SOF<N, O> makeInstance() {
      return new SOF<>(npredf, distanceFunction);
    }
  }
}
